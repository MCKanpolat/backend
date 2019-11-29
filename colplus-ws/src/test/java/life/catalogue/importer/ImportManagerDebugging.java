package life.catalogue.importer;

import java.net.URI;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Files;
import io.dropwizard.client.HttpClientBuilder;
import life.catalogue.importer.ImportManager;
import life.catalogue.importer.ImportRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.ibatis.session.SqlSession;
import life.catalogue.WsServerConfig;
import life.catalogue.api.TestEntityGenerator;
import life.catalogue.api.model.Dataset;
import life.catalogue.api.vocab.DataFormat;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.api.vocab.DatasetType;
import life.catalogue.api.vocab.Users;
import life.catalogue.common.tax.AuthorshipNormalizer;
import life.catalogue.dao.TreeRepoRule;
import life.catalogue.db.PgSetupRule;
import life.catalogue.db.mapper.DatasetMapper;
import life.catalogue.es.name.index.NameUsageIndexService;
import life.catalogue.img.ImageServiceFS;
import life.catalogue.matching.NameIndexFactory;
import org.elasticsearch.client.RestClient;
import org.junit.*;

@Ignore("manual import debugging")
public class ImportManagerDebugging {
  static final AuthorshipNormalizer aNormalizer = AuthorshipNormalizer.createWithAuthormap();

  ImportManager importManager;
  CloseableHttpClient hc;
  RestClient esClient;
  
  @ClassRule
  public static PgSetupRule pgSetupRule = new PgSetupRule();
  
  @Rule
  public final TreeRepoRule treeRepoRule = new TreeRepoRule();
  
  private static WsServerConfig provideConfig() {
    WsServerConfig cfg = new WsServerConfig();
    cfg.gbif.syncFrequency = 0;
    cfg.importer.continousImportPolling = 0;
    cfg.importer.threads = 3;
    cfg.normalizer.archiveDir = Files.createTempDir();
    cfg.normalizer.scratchDir = Files.createTempDir();
    cfg.db.host = "localhost";
    cfg.db.database = "colplus";
    cfg.db.user = "postgres";
    cfg.db.password = "postgres";
    cfg.es.hosts = "localhost";
    cfg.es.ports = "9200";
    
    return cfg;
  }
  
  @Before
  public void init() throws Exception {
    MetricRegistry metrics = new MetricRegistry();
    
    final WsServerConfig cfg = provideConfig();
    //new InitDbCmd().execute(cfg);
    
    hc = new HttpClientBuilder(metrics).using(cfg.client).build("local");
    importManager = new ImportManager(cfg, metrics, hc, PgSetupRule.getSqlSessionFactory(), aNormalizer,
        NameIndexFactory.passThru(), NameUsageIndexService.passThru(), new ImageServiceFS(cfg.img));
    importManager.start();
  }
  
  @After
  public void shutdown() throws Exception {
    importManager.stop();
    hc.close();
    esClient.close();
  }
  
  /**
   * Try with 3 small parallel datasets
   */
  @Test
  public void debugParallel() throws Exception {
    importManager.submit(new ImportRequest(1000, Users.IMPORTER));
    importManager.submit(new ImportRequest(1006, Users.IMPORTER));
    importManager.submit(new ImportRequest(1007, Users.IMPORTER));
    
    Thread.sleep(1000);
    while (importManager.hasRunning()) {
      Thread.sleep(1000);
    }
  }
  
  @Test
  public void debugImport() throws Exception {
    Dataset d = create(DataFormat.COLDP, "https://sfg.taxonworks.org/downloads/15/download_file", "TW Test");
    // do it again as key 1 is problematic
    d = create(DataFormat.COLDP, "https://sfg.taxonworks.org/downloads/15/download_file", "TW Test");
    System.out.println("Submitting " + d);
    importManager.submit(new ImportRequest(d.getKey(), Users.IMPORTER));
    Thread.sleep(1000);
    while (importManager.hasRunning()) {
      Thread.sleep(1000);
    }
    System.out.println("Done");
  }
  
  private Dataset create(DataFormat format, String url, String title) {
    Dataset d = new Dataset();
    d.setType(DatasetType.OTHER);
    d.setTitle(title);
    d.setOrigin(DatasetOrigin.EXTERNAL);
    d.setDataFormat(format);
    d.setDataAccess(URI.create(url));
    TestEntityGenerator.setUser(d);
    
    try (SqlSession s = PgSetupRule.getSqlSessionFactory().openSession()) {
      s.getMapper(DatasetMapper.class).create(d);
      s.commit();
    }
    
    return d;
  }
}