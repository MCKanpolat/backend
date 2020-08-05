package life.catalogue.resources;

import life.catalogue.api.model.DatasetImport;
import life.catalogue.api.model.Page;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.api.vocab.ImportState;
import life.catalogue.dao.DatasetImportDao;
import life.catalogue.dao.DatasetInfoCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

@Path("/dataset/{key}/import")
@SuppressWarnings("static-method")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DatasetImportResource {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(DatasetImportResource.class);
  private final DatasetImportDao diDao;

  public DatasetImportResource(DatasetImportDao diDao) {
    this.diDao = diDao;
  }


  @GET
  public List<DatasetImport> getImports(@PathParam("key") int key,
                                        @QueryParam("state") List<ImportState> states,
                                        @QueryParam("limit") @DefaultValue("1") int limit) {
    // a release? use mother project in that case
    if (DatasetInfoCache.CACHE.origin(key) == DatasetOrigin.RELEASED) {
      Integer projectKey = DatasetInfoCache.CACHE.sourceProject(key);
      Integer attempt = DatasetInfoCache.CACHE.importAttempt(key);
      return List.of(diDao.getAttempt(projectKey, attempt));

    } else {
      return diDao.list(key, states, new Page(0, limit)).getResult();
    }
  }
  
  @GET
  @Path("{attempt}")
  public DatasetImport getImportAttempt(@PathParam("key") int key,
                                        @PathParam("attempt") int attempt) {
    return diDao.getAttempt(key, attempt);
  }
  
  @GET
  @Path("{attempt}/tree")
  @Produces({MediaType.TEXT_PLAIN})
  public Stream<String> getImportAttemptTree(@PathParam("key") int key,
                                     @PathParam("attempt") int attempt) throws IOException {
    return diDao.getFileMetricsDao().getTree(key, attempt);
  }
  
  @GET
  @Path("{attempt}/names")
  @Produces({MediaType.TEXT_PLAIN})
  public Stream<String> getImportAttemptNames(@PathParam("key") int key,
                                              @PathParam("attempt") int attempt) {
    return diDao.getFileMetricsDao().getNames(key, attempt);
  }

  @GET
  @Path("{attempt}/ids")
  @Produces({MediaType.TEXT_PLAIN})
  public Stream<String> getImportAttemptNameIds(@PathParam("key") int key,
                                                @PathParam("attempt") int attempt) {
    return diDao.getFileMetricsDao().getNameIds(key, attempt);
  }

}
