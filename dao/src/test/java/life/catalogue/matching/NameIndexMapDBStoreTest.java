package life.catalogue.matching;

import life.catalogue.api.TestEntityGenerator;
import life.catalogue.api.model.IndexName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class NameIndexMapDBStoreTest {
  AtomicInteger keyGen = new AtomicInteger();
  File dbf;
  DBMaker.Maker maker;
  NameIndexMapDBStore db;

  @Before
  public void init() throws Exception {
    dbf = File.createTempFile("colNidxStore",".db");
    dbf.delete();
    maker = DBMaker.fileDB(dbf).fileMmapEnableIfSupported();
    db = new NameIndexMapDBStore(maker, dbf);
    db.start();
  }

  @After
  public void cleanup() throws Exception {
    db.stop();
    dbf.delete();
  }

  @Test
  public void size() throws Exception {
    try {
      assertEquals(0, db.count());

      addNameList("a", 1);
      assertEquals(1, db.count());

      addNameList("b", 2); // 2,3
      assertEquals(3, db.count());

      addNameList("c", 3); // 4,5,6
      assertEquals(6, db.count());
  
      addNameList("a", 3); // 7,8,9
      assertEquals(9, db.count());

      // add the same id, this should not increase the size
      addName("a", 1);
      assertEquals(9, db.count());

      // now shutdown and reopen
      db.stop();
      db = new NameIndexMapDBStore(maker, dbf);
      db.start();

      assertEquals(9, db.count());

    } finally {
      dbf.delete();
    }
  }

  private void addName(String key, int id) {
    IndexName n = new IndexName(TestEntityGenerator.newName());
    n.setKey(id);
    db.add(key, n);
  }

  private void addNameList(String key, int size) {
    for (int idx = 0; idx<size; idx++) {
      IndexName n = new IndexName(TestEntityGenerator.newName());
      n.setKey(keyGen.incrementAndGet());
      db.add(key, n);
    }
  }

  @Test
  public void close() {
  }
}