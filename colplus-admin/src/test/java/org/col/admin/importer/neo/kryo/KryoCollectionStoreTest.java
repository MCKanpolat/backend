package org.col.admin.importer.neo.kryo;

import java.io.File;

import com.esotericsoftware.kryo.pool.KryoPool;
import org.col.api.model.Page;
import org.col.api.model.Reference;
import org.gbif.utils.text.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class KryoCollectionStoreTest {

  @Test
  public void storeReferences() throws Exception {

    KryoPool pool = new KryoPool.Builder(new NeoKryoFactory())
        .softReferences()
        .build();

    try (KryoCollectionStore<Page> store = new KryoCollectionStore(Page.class, File.createTempFile("kryo-",".bin"), pool)) {

      for (int i = 0; i < 100; i++) {
        Page r = buildPage();
        r.setOffset(i);
        store.add(r);
      }

      int counter = 0;
      for (Page r : store) {
        System.out.println(r);
        assertNotNull(r);
        assertEquals(counter, r.getOffset());
        counter++;
      }

      assertEquals(counter, 100);
    }
  }

  private Page buildPage() {
    return new Page(10, 12);
  }

  private Reference buildRef() {
    Reference r = new Reference();
    r.getCsl().setTitle("Harry Belafonte");
    r.setYear(1989);
    r.setId(StringUtils.randomString(12));
    return r;
  }

}