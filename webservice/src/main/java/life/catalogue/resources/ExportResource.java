package life.catalogue.resources;

import io.dropwizard.auth.Auth;
import life.catalogue.WsServerConfig;
import life.catalogue.api.exception.NotFoundException;
import life.catalogue.api.model.User;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.common.io.Resources;
import life.catalogue.dao.DatasetImportDao;
import life.catalogue.dao.DatasetInfoCache;
import life.catalogue.db.mapper.DatasetMapper;
import life.catalogue.db.mapper.NameUsageMapper;
import life.catalogue.db.tree.JsonTreePrinter;
import life.catalogue.db.tree.TextTreePrinter;
import life.catalogue.dw.auth.Roles;
import life.catalogue.dw.jersey.MoreMediaTypes;
import life.catalogue.exporter.AcExporter;
import life.catalogue.exporter.HtmlExporter;
import life.catalogue.exporter.HtmlExporterSimple;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.gbif.nameparser.api.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

/**
 * Stream dataset exports to the user.
 * If existing it uses preprepared files from the filesystem.
 * For yet non existing files we should generate and store them for later reuse.
 * If no format is given the original source is returned.
 *
 * Managed datasets can change data continously and we will need to:
 *  a) never store any version and dynamically recreate them each time
 *  b) keep a "dirty" flag that indicates the currently stored archive is not valid anymore because data has changed.
 *     Any edit would have to raise the dirty flag which therefore must be kept in memory and only persisted if it has changed.
 *     Creating an export would remove the flag - we will need a flag for each supported output format.
 *
 * Formats currently supported for the entire dataset and which are archived for reuse:
 *  - ColDP
 *  - ColDP simple (single TSV file)
 *  - DwCA
 *  - DwC simple (single TSV file)
 *  - TextTree
 *
 *  Single file formats for dynamic exports using some filter (e.g. rank, taxonID, etc)
 *  - ColDP simple (single TSV file)
 *  - DwC simple (single TSV file)
 *  - TextTree
 */
@Path("/dataset/{key}/export")
@Produces(MediaType.APPLICATION_JSON)
public class ExportResource {
  private final DatasetImportDao diDao;
  private final SqlSessionFactory factory;
  private final WsServerConfig cfg;

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(ExportResource.class);
  private final AcExporter exporter;

  public ExportResource(SqlSessionFactory factory, AcExporter exporter, DatasetImportDao diDao, WsServerConfig cfg) {
    this.factory = factory;
    this.exporter = exporter;
    this.diDao = diDao;
    this.cfg = cfg;
  }

  @POST
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public boolean export(@PathParam("key") int datasetKey, @Auth User user) {
    return exportAC(datasetKey, user);
  }

  private boolean exportAC(int key, User user) {
    try {
      exporter.export(key);
      return true;

    } catch (Throwable e) {
      LOG.error("Error exporting dataset {}", key, e);
    }
    return false;
  }


  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response original(@PathParam("key") int key) {
    File source = cfg.normalizer.source(key);
    if (source.exists()) {
      StreamingOutput stream = os -> {
        InputStream in = new FileInputStream(source);
        IOUtils.copy(in, os);
        os.flush();
      };
      return Response.ok(stream).build();
    }
    throw new NotFoundException(key, "original archive for dataset " + key + " not found");
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response textTree(@PathParam("key") int key,
                           @PathParam("id") String taxonID,
                           @QueryParam("rank") Set<Rank> ranks,
                           @Context SqlSession session) {
    StreamingOutput stream;
    final Integer projectKey;
    Integer attempt;
    // a release?
    if (DatasetInfoCache.CACHE.origin(key) == DatasetOrigin.RELEASED) {
      attempt = DatasetInfoCache.CACHE.importAttempt(key);
      projectKey = DatasetInfoCache.CACHE.sourceProject(key);
    } else {
      attempt = session.getMapper(DatasetMapper.class).lastImportAttempt(key);
      projectKey = key;
    }

    if (attempt != null && taxonID == null && (ranks == null || ranks.isEmpty())) {
      // stream from pre-generated file
      stream = os -> {
        InputStream in = new FileInputStream(diDao.getFileMetricsDao().treeFile(projectKey, attempt));
        IOUtils.copy(in, os);
        os.flush();
      };

    } else {
      stream = os -> {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));
        TextTreePrinter printer = TextTreePrinter.dataset(key, taxonID, ranks, factory, writer);
        printer.print();
        if (printer.getCounter() == 0) {
          writer.write("--NONE--");
        }
        writer.flush();
      };
    }
    return Response.ok(stream).build();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response textTreeAll(@PathParam("key") int key,
                              @QueryParam("rank") Set<Rank> ranks,
                              @Context SqlSession session) {
    return textTree(key,null,ranks,session);
  }
                           
  @GET
  @Path("{id}")
  @Produces(MediaType.TEXT_HTML)
  public Response html(@PathParam("key") int key,
                       @PathParam("id") String taxonID,
                       @QueryParam("rank") Set<Rank> ranks,
                       @QueryParam("full") boolean full) {
    StreamingOutput stream;
    stream = os -> {
      Writer writer = new BufferedWriter(new OutputStreamWriter(os));
      if (full) {
        HtmlExporter exporter = HtmlExporter.subtree(key, taxonID, ranks, cfg, factory, writer);
        exporter.print();
      } else {
        HtmlExporterSimple exporter = HtmlExporterSimple.subtree(key, taxonID, ranks, cfg, factory, writer);
        exporter.print();
      }
      writer.flush();
    };
    return Response.ok(stream).build();
  }

  @GET
  @Path("css")
  @Produces(MoreMediaTypes.TEXT_CSS)
  public Response htmlCss() {
    StreamingOutput stream = os -> {
      InputStream in = Resources.stream("exporter/html/catalogue.css");
      IOUtils.copy(in, os);
      os.flush();
    };
    return Response.ok(stream).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public Object simpleName(@PathParam("key") int key,
                           @PathParam("id") String taxonID,
                           @QueryParam("rank") Set<Rank> ranks,
                           @QueryParam("synonyms") boolean includeSynonyms,
                           @QueryParam("nested") boolean nested,
                           @Context SqlSession session) {
    if (nested) {
      StreamingOutput stream;
      stream = os -> {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));
        JsonTreePrinter.dataset(key, taxonID, ranks, factory, writer).print();
        writer.flush();
      };
      return Response.ok(stream).build();

    } else {
      // spot lowest rank
      Rank lowestRank = null;
      if (!ranks.isEmpty()) {
        LinkedList<Rank> rs = new LinkedList<>(ranks);
        Collections.sort(rs);
        lowestRank = rs.getLast();
      }
      return session.getMapper(NameUsageMapper.class).processTreeSimple(key, null, taxonID, null, lowestRank, includeSynonyms);
    }
  }

}
