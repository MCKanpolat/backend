package life.catalogue.exporter;

import com.google.common.io.Files;
import freemarker.template.*;
import life.catalogue.WsServerConfig;
import life.catalogue.api.model.Dataset;
import life.catalogue.api.model.Page;
import life.catalogue.api.search.DatasetSearchRequest;
import life.catalogue.common.io.CompressionUtil;
import life.catalogue.common.io.UTF8IoUtils;
import life.catalogue.db.mapper.DatasetMapper;
import life.catalogue.img.ImgConfig;
import life.catalogue.postgres.PgCopyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.postgresql.jdbc.PgConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcExporter {
  private static final Logger LOG = LoggerFactory.getLogger(AcExporter.class);
  private static final String EXPORT_SQL = "/exporter/ac-export.sql";
  private static final String COPY_WITH = "CSV HEADER NULL '\\N' DELIMITER E'\\t' QUOTE E'\\f' ENCODING 'UTF8' ";
  private static final Pattern COPY_START = Pattern.compile("^\\s*COPY\\s*\\(");
  private static final Pattern COPY_END   = Pattern.compile("^\\s*\\)\\s*TO\\s*'(.+)'");
  private static final Pattern VAR_DATASET_KEY = Pattern.compile("\\{\\{datasetKey}}", Pattern.CASE_INSENSITIVE);
  private final WsServerConfig cfg;
  private final SqlSessionFactory factory;
  // we only allow a single export to run at a time
  private static boolean LOCK = false;

  public AcExporter(WsServerConfig cfg, SqlSessionFactory factory) {
    this.cfg = cfg;
    this.factory = factory;
  }
  
  /**
   * @return final archive
   */
  public File export(int datasetKey) throws IOException, SQLException, IllegalStateException {
    File expDir = new File(cfg.normalizer.scratchDir(datasetKey), "export");
    if (!aquireLock()) {
      throw new IllegalStateException("There is a running export already");
    }

    try {
      LOG.info("Export catalogue {} to {}", datasetKey, expDir.getAbsolutePath());
      // create csv files
      PgConnection c = cfg.db.connect();
      c.setAutoCommit(false);
      try {
        InputStream sql = AcExporter.class.getResourceAsStream(EXPORT_SQL);
        executeAcExportSql(datasetKey, c, new BufferedReader(new InputStreamReader(sql, StandardCharsets.UTF_8)), expDir);

      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);

      } finally {
        dropSchema(datasetKey, c);
        c.commit();
        c.close();
      }
      // include images
      exportLogos(datasetKey, expDir);
  
      // export citation.ini
      exportCitations(datasetKey, expDir);
      
      // zip up archive in exports directory
      File arch = new File(cfg.exportDir, datasetKey+"-ac-export.zip");
      LOG.info("Zip up archive and move to exports dir");
      if (arch.exists()) {
        LOG.debug("Remove previous export file {}", arch.getAbsolutePath());
      }
      LOG.info("Creating final export archive {}", arch.getAbsolutePath());
      CompressionUtil.zipDir(expDir, arch, true);
      return arch;
      
    } finally {
      LOG.info("Remove temp export directory {}", expDir.getAbsolutePath());
      FileUtils.deleteQuietly(expDir);
      LOG.info("Export completed");
      releaseLock();
    }
  }

  private static String exportSchema(int datasetKey){
    return "exp"+datasetKey;
  }

  private static void dropSchema(int datasetKey, Connection con) throws SQLException {
    final String schema = exportSchema(datasetKey);
    LOG.info("Remove export schema {} with all tables & sequences from postgres", schema);
    try (Statement stmnt = con.createStatement()) {
      stmnt.execute(String.format("DROP SCHEMA IF EXISTS %s CASCADE", schema));
      con.commit();
    }
  }

  private static synchronized boolean aquireLock(){
    if (!LOCK) {
      LOCK = true;
      return true;
    }
    return false;
  }

  private static synchronized void releaseLock(){
    LOCK = false;
  }

  private void exportCitations(int catalogueKey, File dir) throws IOException {
    LOG.info("Export credits");
    File cf = new File(dir, "credits.ini");
  
    Map<String, Object> data = new HashMap<>();
  
    try (SqlSession session = factory.openSession(true)) {
      DatasetMapper dm = session.getMapper(DatasetMapper.class);
      Dataset d = dm.get(catalogueKey);
      if (d.getReleased()==null) {
        // use today as default release date if missing
        d.setReleased(LocalDate.now());
      }
      data.put("d", d);
      
      Template temp = FmUtil.FMK.getTemplate("credits.ftl");
      Writer out = UTF8IoUtils.writerFromFile(cf);
      temp.process(data, out);
    } catch (TemplateException e) {
      LOG.error("Failed to write credits", e);
      throw new RuntimeException(e);
    }
  }
  
  private void exportLogos(int projectKey, File expDir) throws IOException {
    LOG.info("Export logos for sources of project " + projectKey);
    File logoDir = new File(expDir, "logos");
    logoDir.mkdir();
  
    int counter = 0;
    try (SqlSession session = factory.openSession(true)) {
      DatasetMapper dm = session.getMapper(DatasetMapper.class);
      DatasetSearchRequest req = new DatasetSearchRequest();
      req.setContributesTo(projectKey);
      List<Dataset> resp = dm.search(req, null, new Page(0,1000));
      LOG.info("Found " +resp.size()+ " source datasets of project " + projectKey);
      for (Dataset d : resp) {
        Path p = cfg.img.datasetLogo(d.getKey(), ImgConfig.Scale.MEDIUM);
        if (java.nio.file.Files.exists(p)) {
          File img =  new File(logoDir, (d.getKey()-1000) + ".png");
          Files.copy(p.toFile(), img);
          
          p = cfg.img.datasetLogo(d.getKey(), ImgConfig.Scale.SMALL);
          img =  new File(logoDir, (d.getKey()-1000) + "-sm.png");
          Files.copy(p.toFile(), img);
          counter++;
          
        } else {
          LOG.warn("Missing logo for dataset {}: {}", d.getKey(), d.getTitle());
          LOG.info("Missing logo for dataset " + d.getTitle());
        }
      }
    }
    LOG.info(counter + " logos exported");
  }
  
  /**
   * @return directory with all CSV dump files
   */
  private void executeAcExportSql(int datasetKey, PgConnection con, BufferedReader sql, File csvDir) throws IOException, SQLException {
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = sql.readLine()) != null) {
      Matcher m = COPY_END.matcher(line);
      if (COPY_START.matcher(line).find()) {
        executeSql(con, sb.toString());
        sb = new StringBuilder();
      
      } else if (m.find()) {
        // copy to file
        File f = new File(csvDir, m.group(1).trim());
        Files.createParentDirs(f);
        LOG.info("Exporting " + f.getName());
        LOG.info("Exporting {}", f.getAbsolutePath());
        PgCopyUtils.dump(con, sb.toString(), f, COPY_WITH);
        sb = new StringBuilder();
      
      } else {
        if (sb.length() > 0) {
          sb.append("\n");
        }
        // substitute datasetKey variable
        sb.append(VAR_DATASET_KEY.matcher(line).replaceAll(String.valueOf(datasetKey)));
      }
    }
    if (sb.length() > 0) {
      executeSql(con, sb.toString());
    }
    con.commit();
  }
  
  private void executeSql(PgConnection con, String sql) throws SQLException, IOException {
    try (Statement stmnt = con.createStatement()) {
      if (sql.startsWith("--")) {
        if (sql.contains("\n")) {
          LOG.info(StringUtils.capitalize(sql.substring(3, sql.indexOf('\n'))));
        }
      } else if (sql.contains(" ")){
        LOG.info("Execute " + sql.substring(0, sql.indexOf(' ')) + " SQL");
      }
      stmnt.execute(sql);
      con.commit();
    }
  }
}
