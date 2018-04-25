package org.col.db.mapper;

import com.google.common.base.Splitter;
import org.col.api.RandomUtils;
import org.col.api.TestEntityGenerator;
import org.col.api.model.Dataset;
import org.col.api.model.Name;
import org.col.api.model.Synonym;
import org.col.api.model.Taxon;
import org.col.api.vocab.TaxonomicStatus;
import org.col.db.dao.NameDao;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class SynonymMapperTest extends MapperTestBase<SynonymMapper> {

  private static final Splitter SPACE_SPLITTER = Splitter.on(" ").trimResults();

  private NameDao nameDao;
  private SynonymMapper synonymMapper;
  private TaxonMapper taxonMapper;

  public SynonymMapperTest() {
    super(SynonymMapper.class);
  }

  @Before
  public void initMappers() {
    nameDao = new NameDao(initMybatisRule.getSqlSession());
    synonymMapper = initMybatisRule.getMapper(SynonymMapper.class);
    taxonMapper = initMybatisRule.getMapper(TaxonMapper.class);
  }


  @Test
  public void roundtrip() throws Exception {
    Name n = TestEntityGenerator.newName();
    nameDao.create(n);

    Name an = TestEntityGenerator.newName();
    nameDao.create(an);
    Taxon t = TestEntityGenerator.newTaxon(an.getDatasetKey(), RandomUtils.randomString(25));
    t.setName(an);
    taxonMapper.create(t);

    Synonym s1 = TestEntityGenerator.newSynonym(TaxonomicStatus.SYNONYM, n, t);
    synonymMapper.create(s1);
    commit();

    List<Synonym> syns = synonymMapper.listByName(s1.getName().getKey());
    assertEquals(1, syns.size());
    Synonym s2 = syns.get(0);

    assertEquals(s1.getName(), s2.getName());
    assertEquals(s1.getAccepted().getName(), s2.getAccepted().getName());
    assertEquals(s1.getAccepted(), s2.getAccepted());
    assertEquals(s1, s2);
  }

  @Test
  public void synonyms() throws Exception {
    final int accKey = TestEntityGenerator.TAXON1.getKey();
    final int datasetKey = TestEntityGenerator.TAXON1.getDatasetKey();

    List<Synonym> synonyms = synonymMapper.listByTaxon(accKey);
    assertTrue(synonyms.isEmpty());
    assertEquals(0, synonyms.size());

    // homotypic 1
    Name syn1 = TestEntityGenerator.newName("syn1");
    nameDao.create(syn1);

    // homotypic 2
    Name syn2bas = TestEntityGenerator.newName("syn2bas");
    nameDao.create(syn2bas);

    Name syn21 = TestEntityGenerator.newName("syn2.1");
    syn21.setHomotypicNameKey(syn2bas.getKey());
    nameDao.create(syn21);

    Name syn22 = TestEntityGenerator.newName("syn2.2");
    syn22.setHomotypicNameKey(syn2bas.getKey());
    nameDao.create(syn22);

    // homotypic 3
    Name syn3bas = TestEntityGenerator.newName("syn3bas");
    nameDao.create(syn3bas);

    Name syn31 = TestEntityGenerator.newName("syn3.1");
    syn31.setHomotypicNameKey(syn3bas.getKey());
    nameDao.create(syn31);

    commit();

    // no synonym links added yet, expect empty synonymy even though basionym links
    // exist!
    synonyms = synonymMapper.listByTaxon(accKey);
    assertTrue(synonyms.isEmpty());
    assertEquals(0, synonyms.size());

    // now add a few listByTaxon
    synonymMapper.create(NameDao.toSynonym(datasetKey, accKey, syn1.getKey()));
    commit();
    synonyms = synonymMapper.listByTaxon(accKey);
    assertFalse(synonyms.isEmpty());
    assertEquals(1, synonyms.size());

    synonymMapper.create(NameDao.toSynonym(datasetKey, accKey, syn2bas.getKey()));
    synonymMapper.create(NameDao.toSynonym(datasetKey, accKey, syn21.getKey()));
    synonymMapper.create(NameDao.toSynonym(datasetKey, accKey, syn22.getKey()));
    synonymMapper.create(NameDao.toSynonym(datasetKey, accKey, syn3bas.getKey()));
    synonymMapper.create(NameDao.toSynonym(datasetKey, accKey, syn31.getKey()));

    synonyms = synonymMapper.listByTaxon(accKey);
    assertEquals(6, synonyms.size());
    assertEquals(2, synonymMapper.listByTaxon(TestEntityGenerator.TAXON2.getKey()).size());


    // now also add a misapplied name with the same name
    Synonym mis = new Synonym();
    mis.setName(syn21);
    mis.setAccepted(TestEntityGenerator.TAXON2);
    mis.setStatus(TaxonomicStatus.MISAPPLIED);
    mis.setAccordingTo("auct. Döring");
    synonymMapper.create(mis);
    commit();

    assertEquals(6, synonymMapper.listByTaxon(accKey).size());
    assertEquals(3, synonymMapper.listByTaxon(TestEntityGenerator.TAXON2.getKey()).size());
  }

}
