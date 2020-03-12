package life.catalogue.db.mapper;

import life.catalogue.api.RandomUtils;
import life.catalogue.api.TestEntityGenerator;
import life.catalogue.api.model.Page;
import life.catalogue.api.model.Sector;
import life.catalogue.api.model.SectorImport;
import life.catalogue.api.search.SectorSearchRequest;
import life.catalogue.api.vocab.Datasets;
import life.catalogue.api.vocab.EntityType;
import life.catalogue.api.vocab.Users;
import life.catalogue.db.MybatisTestUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static life.catalogue.api.TestEntityGenerator.DATASET11;
import static org.junit.Assert.*;

public class SectorMapperTest extends CRUDTestBase<Integer, Sector, SectorMapper> {
  
  private static final int targetDatasetKey = Datasets.DRAFT_COL;
  private static final int subjectDatasetKey = DATASET11.getKey();
  private Sector s1;
  private Sector s2;

  public SectorMapperTest() {
    super(SectorMapper.class);
  }
  
  private void add2Sectors() {
    // create a few draft taxa to attach sectors to
    MybatisTestUtils.populateDraftTree(session());

    s1 = createTestEntity(targetDatasetKey);
    s1.getSubject().setId(TestEntityGenerator.TAXON1.getId());
    s1.getTarget().setId("t4");
    mapper().create(s1);

    s2 = createTestEntity(targetDatasetKey);
    mapper().create(s2);
    commit();
  }

  private void addImport(Sector s, SectorImport.State state, LocalDateTime finished) {
    SectorImport si = SectorImportMapperTest.create(state, s);
    si.setFinished(finished);
    si.setCreatedBy(Users.TESTER);
    mapper(SectorImportMapper.class).create(si);

    if (state == SectorImport.State.FINISHED) {
      mapper().updateLastSync(s.getKey(), si.getAttempt());
    }
  }

  @Test
  public void getBySubject() {
    add2Sectors();
    assertNotNull(mapper().getBySubject(targetDatasetKey, subjectDatasetKey, TestEntityGenerator.TAXON1.getId()));
    assertNull(mapper().getBySubject(targetDatasetKey, subjectDatasetKey +1, TestEntityGenerator.TAXON1.getId()));
    assertNull(mapper().getBySubject(targetDatasetKey, subjectDatasetKey, TestEntityGenerator.TAXON1.getId()+"dfrtgzh"));
  }
  
  @Test
  public void listByTarget() {
    add2Sectors();
    assertEquals(1, mapper().listByTarget(targetDatasetKey,"t4").size());
    assertEquals(0, mapper().listByTarget(targetDatasetKey,"t32134").size());
  }

  @Test
  public void list() {
    add2Sectors();
    assertEquals(2, mapper().listByDataset(targetDatasetKey,subjectDatasetKey).size());
    assertEquals(0, mapper().listByDataset(targetDatasetKey,-432).size());
  }

  @Test
  public void broken() {
    add2Sectors();
  
    SectorSearchRequest req = SectorSearchRequest.byDataset(targetDatasetKey,subjectDatasetKey);
    req.setBroken(true);
    assertEquals(1, mapper().search(req, new Page()).size());
  
    req.setSubjectDatasetKey(543432);
    assertEquals(0, mapper().search(req, new Page()).size());
  }

  @Test
  public void search() {
    add2Sectors();


    addImport(s1, SectorImport.State.FINISHED, LocalDateTime.of(2019, 12, 24, 12, 0, 0));
    addImport(s1, SectorImport.State.FINISHED, LocalDateTime.of(2020, 1, 10, 12, 0, 0));
    addImport(s1, SectorImport.State.FAILED, LocalDateTime.of(2020, 2, 11, 12, 0, 0));

    addImport(s2, SectorImport.State.FAILED, LocalDateTime.of(2018, 1, 10, 12, 0, 0));
    addImport(s2, SectorImport.State.FINISHED, LocalDateTime.of(2020, 1, 21, 12, 0, 0));
    commit();

    SectorSearchRequest req = SectorSearchRequest.byCatalogue(targetDatasetKey);
    req.setLastSync(LocalDate.of(2020, 1, 1));
    assertEquals(0, mapper().search(req, new Page()).size());

    req.setLastSync(LocalDate.of(2020, 1, 15));
    assertEquals(1, mapper().search(req, new Page()).size());

    req.setLastSync(LocalDate.of(2020, 2, 1));
    assertEquals(2, mapper().search(req, new Page()).size());

    req.setLastSync(LocalDate.of(2022, 3, 1));
    assertEquals(2, mapper().search(req, new Page()).size());

    req.setLastSync(LocalDate.of(2019, 1, 1));
    assertEquals(0, mapper().search(req, new Page()).size());
  }
  
  @Test
  public void listTargetDatasetKeys() {
    assertEquals(0, mapper().listTargetDatasetKeys().size());
    add2Sectors();
    assertEquals(1, mapper().listTargetDatasetKeys().size());
  }
  
  @Override
  Sector createTestEntity(int dkey) {
    return create();
  }
  
  public static Sector create() {
    Sector d = new Sector();
    d.setDatasetKey(Datasets.DRAFT_COL);
    d.setSubjectDatasetKey(subjectDatasetKey);
    d.setMode(Sector.Mode.ATTACH);
    d.setCode(NomCode.ZOOLOGICAL);
    d.setSubject(TestEntityGenerator.newSimpleName());
    d.setTarget(TestEntityGenerator.newSimpleNameWithoutStatusParent());
    d.setRanks(Set.copyOf(Rank.LINNEAN_RANKS));
    d.setEntities(Set.of(EntityType.NAME, EntityType.NAME_USAGE, EntityType.NAME_RELATION));
    d.setNote(RandomUtils.randomUnicodeString(1024));
    d.setCreatedBy(TestEntityGenerator.USER_EDITOR.getKey());
    d.setModifiedBy(TestEntityGenerator.USER_EDITOR.getKey());
    return d;
  }
  
  @Override
  Sector removeDbCreatedProps(Sector s) {
    // remove newly set property
    s.setCreated(null);
    s.setModified(null);
    return s;
  }
  
  @Override
  void updateTestObj(Sector s) {
    s.setNote("not my thing");
  }
  
  @Test(expected = PersistenceException.class)
  public void unique() throws Exception {
    Sector d1 = create();
    mapper().create(d1);
    commit();
    
    d1.setKey(null);
    mapper().create(d1);
    commit();
  }
  
  @Test
  public void process(){
    // processing
    DecisionMapperTest.CountHandler handler = new DecisionMapperTest.CountHandler();
    mapper().processDataset(Datasets.DRAFT_COL).forEach(handler);
    assertEquals(0, handler.counter.size());
  }
}