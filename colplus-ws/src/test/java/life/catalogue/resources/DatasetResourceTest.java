package life.catalogue.resources;

import java.time.LocalDate;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import life.catalogue.api.model.Dataset;
import life.catalogue.api.model.Page;
import life.catalogue.api.model.ResultPage;
import life.catalogue.api.search.DatasetSearchRequest;
import life.catalogue.api.vocab.DataFormat;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.api.vocab.DatasetType;
import life.catalogue.api.vocab.Frequency;
import life.catalogue.db.mapper.TestDataRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static life.catalogue.api.TestEntityGenerator.nullifyUserDate;
import static life.catalogue.dw.ApiUtils.*;
import static org.junit.Assert.*;

public class DatasetResourceTest extends ResourceTestBase {
  
  static GenericType<ResultPage<Dataset>> RESULT_PAGE = new GenericType<ResultPage<Dataset>>() {};
  
  @Rule
  public TestDataRule testDataRule = TestDataRule.datasets(RULE.getSqlSessionFactory());

  public DatasetResourceTest() {
    super("/dataset");
  }
  
  @Test
  public void list() {
    Page page = new Page(0,10);
    ResultPage<Dataset> resp = applyPage(base, page).request().get(RESULT_PAGE);
    
    assertEquals(10, resp.size());
    assertTrue(resp.getTotal() > 200);
    for (Dataset d : resp) {
      assertNotNull(d);
    }
  
    DatasetSearchRequest req = DatasetSearchRequest.byQuery("Catalogue");
    req.setSortBy(DatasetSearchRequest.SortBy.TITLE);
    resp = applySearch(base, req, page).request().get(RESULT_PAGE);
  
    assertEquals(10, resp.size());
    assertEquals("A World Catalogue of Centipedes (Chilopoda) for the Web", resp.getResult().get(0).getTitle());
  
    req.setFormat(DataFormat.DWCA);
    resp = applySearch(base, req, page).request().get(RESULT_PAGE);
  
    assertEquals(5, resp.size());
    assertEquals("Catalogue of Afrotropical Bees", resp.getResult().get(0).getTitle());
  }
  
  @Test
  public void create() {
    Dataset d = new Dataset();
    d.setTitle("s3s3derftg");
    d.setType(DatasetType.OTHER);
    d.setOrigin(DatasetOrigin.UPLOADED);
    d.setContact("me");
    d.setReleased(LocalDate.now());
    d.setImportFrequency(Frequency.MONTHLY);
    Integer key = editorCreds(base).post(json(d), Integer.class);
    d.setKey(key);
    
    Dataset d2 = base.path(key.toString()).request().get(Dataset.class);
    
    assertEquals(nullifyUserDate(d2), nullifyUserDate(d));
  }
  
  @Test
  public void get() {
    Dataset d = base.path("1008").request().get(Dataset.class);
    assertNotNull(d);
    assertNull(d.getDeleted());
    assertEquals("The Reptile Database", d.getTitle());
  }
  
  @Test
  @Ignore
  public void update() {
  }
  
  @Test
  public void delete() {
    Response resp = editorCreds(base.path("2035")).delete();
    assertEquals(204, resp.getStatus());
  
    Dataset d = base.path("2035").request().get(Dataset.class);
    assertNotNull(d.getDeleted());
  }
  
  
  
  @Test
  @Ignore
  public void getImports() {
  }
  
  @Test
  @Ignore
  public void logo() {
  }
  
  @Test
  @Ignore
  public void uploadLogo() {
  }
  
  @Test
  @Ignore
  public void deleteLogo() {
  }
}