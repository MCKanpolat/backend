package org.col.api;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.col.api.model.*;
import org.col.api.search.NameUsageWrapper;
import org.col.api.vocab.*;
import org.col.common.tax.AuthorshipNormalizer;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.nameparser.api.*;

/**
 * utility class to metrics new test instances to be used in tests.
 */
public class TestEntityGenerator {
  private final static AuthorshipNormalizer ANORMALIZER = AuthorshipNormalizer.createWithAuthormap();
  private final static Random RND = new Random();
  private final static RandomInstance random = new RandomInstance();
  private static final Splitter SPACE_SPLITTER = Splitter.on(" ").trimResults();
  private static final AtomicInteger ID_GEN = new AtomicInteger(10000);

  public final static ColUser USER_USER = new ColUser();
  public final static ColUser USER_EDITOR = new ColUser();
  public final static ColUser USER_ADMIN = new ColUser();
  
  /**
   * Corresponds exactly to dataset record inserted via apple.sql or tree.sql with key=11
   */
  public final static Dataset DATASET11 = setUser(new Dataset());
  /**
   * Corresponds exactly to dataset record inserted via apple.sql with key=12
   */
  public final static Dataset DATASET12 = setUser(new Dataset());
  /**
   * Corresponds exactly to 1st name record inserted via apple.sql
   */
  public final static Name NAME1 = setUser( new Name());
  /**
   * Corresponds exactly to 2nd name record inserted via apple.sql
   */
  public final static Name NAME2 = setUser( new Name());
  /**
   * Corresponds exactly to 3rd name record inserted via apple.sql
   */
  public final static Name NAME3 = setUser( new Name());
  /**
   * Corresponds exactly to 4th name record inserted via apple.sql
   */
  public final static Name NAME4 = setUser( new Name());
  /**
   * Corresponds exactly to 1st taxon record inserted via apple.sql
   */
  public final static Taxon TAXON1 = setUser( new Taxon());
  /**
   * Corresponds exactly to 2nd taxon record inserted via apple.sql
   */
  public final static Taxon TAXON2 = setUser( new Taxon());
  /**
   * Corresponds exactly to 1st taxon record inserted via apple.sql
   */
  public final static Synonym SYN1 = setUser( new Synonym());
  /**
   * Corresponds exactly to 2nd taxon record inserted via apple.sql
   */
  public final static Synonym SYN2 = setUser( new Synonym());
  /**
   * Corresponds exactly to 1st reference record inserted via apple.sql
   */
  public final static Reference REF1 = setUser( new Reference());
  /**
   * Corresponds exactly to 2nd reference record inserted via apple.sql
   */
  public final static Reference REF2 = setUser( new Reference());
  public final static Reference REF3 = setUser( new Reference());
  
  public final static int VERBATIM_KEY1 = 1;
  public final static int VERBATIM_KEY2 = 2;
  public final static int VERBATIM_KEY3 = 3;
  public final static int VERBATIM_KEY4 = 4;
  public final static int VERBATIM_KEY5 = 5;
  
  static {
    USER_ADMIN.setKey(91);
    USER_ADMIN.setUsername("'admin'");
    USER_ADMIN.setFirstname("Stan");
    USER_ADMIN.setLastname("Sterling");
    USER_ADMIN.setEmail("stan@mailinator.com");
    USER_ADMIN.getRoles().add(ColUser.Role.ADMIN);

    USER_EDITOR.setKey(92);
    USER_EDITOR.setUsername("editor");
    USER_EDITOR.setFirstname("Yuri");
    USER_EDITOR.setLastname("Roskov");
    USER_EDITOR.setEmail("yuri@mailinator.com");
    USER_EDITOR.getRoles().add(ColUser.Role.USER);
    USER_EDITOR.getRoles().add(ColUser.Role.EDITOR);

    USER_USER.setKey(93);
    USER_USER.setUsername("'user'");
    USER_USER.setFirstname("Frank");
    USER_USER.setLastname("Müller");
    USER_USER.setEmail("frank@mailinator.com");
    USER_USER.getRoles().add(ColUser.Role.USER);

    DATASET11.setKey(11);
    DATASET12.setKey(12);

    
    REF1.setId("ref-1");
    REF1.setDatasetKey(DATASET11.getKey());
    REF1.setCreatedBy(Users.DB_INIT);
    REF1.setModifiedBy(Users.DB_INIT);
    
    REF2.setId("ref-1b");
    REF2.setDatasetKey(DATASET11.getKey());
    REF2.setCreatedBy(Users.DB_INIT);
    REF2.setModifiedBy(Users.DB_INIT);
    
    REF3.setId("ref-2");
    REF3.setDatasetKey(DATASET12.getKey());
    REF3.setCreatedBy(Users.DB_INIT);
    REF3.setModifiedBy(Users.DB_INIT);
    
    NAME1.setId("name-1");
    NAME1.setHomotypicNameId(NAME1.getId());
    NAME1.setDatasetKey(DATASET11.getKey());
    NAME1.setVerbatimKey(VERBATIM_KEY5);
    NAME1.setGenus("Malus");
    NAME1.setSpecificEpithet("sylvestris");
    NAME1.setRank(Rank.SPECIES);
    NAME1.setOrigin(Origin.SOURCE);
    NAME1.setType(NameType.SCIENTIFIC);
    NAME1.updateNameCache();
    NAME1.setPublishedInId(REF1.getId());
    NAME1.setPublishedInPage("712");
    NAME1.setCreatedBy(Users.DB_INIT);
    NAME1.setModifiedBy(Users.DB_INIT);
    
    NAME2.setId("name-2");
    NAME2.setHomotypicNameId(NAME2.getId());
    NAME2.setDatasetKey(DATASET11.getKey());
    NAME2.setGenus("Larus");
    NAME2.setSpecificEpithet("fuscus");
    NAME2.setRank(Rank.SPECIES);
    NAME2.setOrigin(Origin.SOURCE);
    NAME2.setType(NameType.SCIENTIFIC);
    NAME2.updateNameCache();
    NAME2.setPublishedInId(null);
    NAME2.setPublishedInPage(null);
    NAME2.setCreatedBy(Users.DB_INIT);
    NAME2.setModifiedBy(Users.DB_INIT);
    
    NAME3.setId("name-3");
    NAME3.setHomotypicNameId(NAME2.getId());
    NAME3.setDatasetKey(DATASET11.getKey());
    NAME3.setGenus("Larus");
    NAME3.setSpecificEpithet("fusca");
    NAME3.setRank(Rank.SPECIES);
    NAME3.setOrigin(Origin.SOURCE);
    NAME3.setType(NameType.SCIENTIFIC);
    NAME3.updateNameCache();
    NAME3.setPublishedInId(null);
    NAME3.setPublishedInPage(null);
    NAME3.setCreatedBy(Users.DB_INIT);
    NAME3.setModifiedBy(Users.DB_INIT);
    
    NAME4.setId("name-4");
    NAME4.setHomotypicNameId(NAME4.getId());
    NAME4.setDatasetKey(DATASET11.getKey());
    NAME4.setGenus("Larus");
    NAME4.setSpecificEpithet("erfundus");
    NAME4.setRank(Rank.SPECIES);
    NAME4.setOrigin(Origin.SOURCE);
    NAME4.setType(NameType.SCIENTIFIC);
    NAME4.updateNameCache();
    NAME4.setPublishedInId(null);
    NAME4.setPublishedInPage(null);
    NAME4.setCreatedBy(Users.DB_INIT);
    NAME4.setModifiedBy(Users.DB_INIT);
    
    TAXON1.setId("root-1");
    TAXON1.setDatasetKey(DATASET11.getKey());
    TAXON1.setVerbatimKey(VERBATIM_KEY1);
    TAXON1.setName(NAME1);
    TAXON1.setStatus(TaxonomicStatus.ACCEPTED);
    TAXON1.setOrigin(Origin.SOURCE);
    TAXON1.setCreatedBy(Users.DB_INIT);
    TAXON1.setModifiedBy(Users.DB_INIT);
    
    TAXON2.setId("root-2");
    TAXON2.setDatasetKey(DATASET11.getKey());
    TAXON2.setVerbatimKey(VERBATIM_KEY5);
    TAXON2.setName(NAME2);
    TAXON2.setStatus(TaxonomicStatus.ACCEPTED);
    TAXON2.setOrigin(Origin.SOURCE);
    TAXON2.setCreatedBy(Users.DB_INIT);
    TAXON2.setModifiedBy(Users.DB_INIT);
  
    SYN1.setId("s1");
    SYN1.setName(NAME3);
    SYN1.setAccepted(TAXON2);
    SYN1.setStatus(TaxonomicStatus.SYNONYM);
    SYN1.setCreatedBy(Users.DB_INIT);
    SYN1.setModifiedBy(Users.DB_INIT);
  
    SYN2.setId("s2");
    SYN2.setName(NAME4);
    SYN2.setAccepted(TAXON2);
    SYN2.setStatus(TaxonomicStatus.SYNONYM);
    SYN2.setAccordingTo("John Smith");
    SYN2.setVerbatimKey(133);
    SYN2.setCreatedBy(Users.DB_INIT);
    SYN2.setModifiedBy(Users.DB_INIT);
  }

  /*
   * Creates a VernacularName using the specified vernacular name, belonging to the specified taxon and dataset DATASET11.
   */
  public static VernacularName newVernacularName(String name) {
    VernacularName vn = new VernacularName();
    vn.setName(name);
    vn.setLatin(name);
    vn.setLanguage(Language.ENGLISH);
    vn.setCountry(Country.UNITED_KINGDOM);
    return vn;
  }

  public static VernacularName newVernacularName(String name, Language lang) {
    VernacularName vn = new VernacularName();
    vn.setName(name);
    vn.setLatin(name);
    vn.setLanguage(lang);
    return vn;
  }

  /*
   * Creates a new taxon with a generated id
   */
  public static Taxon newTaxon() {
    return newTaxon(DATASET11.getKey(), "t" + ID_GEN.getAndIncrement());
  }

  /*
   * Creates a new taxon with the specified id, belonging to dataset DATASET11.
   */
  public static Taxon newTaxon(String id) {
    return newTaxon(DATASET11.getKey(), id);
  }

  /*
   * Creates a new taxon with the specified id, belonging to the specified dataset.
   */
  public static Taxon newTaxon(int datasetKey, String id) {
    Taxon t = setUserDate(new Taxon());
    t.setStatus(TaxonomicStatus.ACCEPTED);
    t.setAccordingTo("Foo");
    t.setAccordingToDate(LocalDate.of(2010, 11, 24));
    t.setDatasetKey(datasetKey);
    t.setWebpage(URI.create("http://foo.com"));
    t.setFossil(true);
    t.setId(id);
    t.setLifezones(EnumSet.of(Lifezone.BRACKISH, Lifezone.FRESHWATER, Lifezone.TERRESTRIAL));
    t.setName(NAME1);
    t.setOrigin(Origin.SOURCE);
    t.setParentId(TAXON1.getId());
    t.setRecent(true);
    t.setRemarks("Foo == Bar");
    t.setChildCount(0);
    t.setSpeciesEstimate(81);
    t.setSpeciesEstimateReferenceId(REF1.getId());
    return t;
  }

  public static CslData newCslData() {
    return (CslData) new RandomInstance().create(CslData.class, CslName.class, CslDate.class);
  }
  
  public static Synonym newSynonym(Taxon accepted) {
    Name n = newName("n-" + ID_GEN.getAndIncrement());
    return newSynonym(n, accepted.getId());
  }
  public static Synonym newSynonym(Name name, String acceptedID) {
    return newSynonym(TaxonomicStatus.SYNONYM, NAME4, acceptedID);
  }
  public static Synonym newSynonym(TaxonomicStatus status, Name name, String acceptedID) {
    Synonym s = setUserDate(new Synonym());
    s.setDatasetKey(name.getDatasetKey());
    s.setId("syn" + ID_GEN.getAndIncrement());
    s.setName(name);
    s.setAccordingTo("non Döring 1999");
    s.setStatus(status);
    s.setParentId(acceptedID);
    s.setOrigin(Origin.SOURCE);
    return s;
  }

  /*
   * Creates a new name with the specified id, belonging to the specified dataset.
   */
  public static Name newName(String id) {
    return newName(id, RandomUtils.randomSpecies());
  }

  /*
   * Creates a new name with the specified id, belonging to the specified dataset.
   */
  public static Name newName(String id, String scientificName) {
    return newName(DATASET11.getKey(), id, scientificName);
  }

  /*
   * Creates a new name with the specified id, belonging to the specified dataset.
   */
  public static Name newName(int datasetKey, String id, String scientificName) {
    return newName(datasetKey, id, scientificName, Rank.SPECIES);
  }

  public static Name newName(int datasetKey, String id, String scientificName, Rank rank) {
    Name n = setUserDate(new Name());
    n.setId(id);
    n.setNameIndexId(RandomUtils.randomLatinString(10));
    n.setHomotypicNameId(id);
    n.setDatasetKey(datasetKey);
    n.setCombinationAuthorship(createAuthorship());
    if (RND.nextBoolean()) {
      n.setBasionymAuthorship(createAuthorship());
    }
    if (RND.nextInt(10) == 1) {
      n.setSanctioningAuthor("Fr.");
    }
    List<String> tokens = SPACE_SPLITTER.splitToList(scientificName);
    if (tokens.size() == 1) {
      n.setUninomial(tokens.get(0));
    } else {
      n.setGenus(tokens.get(0));
      n.setInfragenericEpithet("Igen");
      n.setSpecificEpithet(tokens.get(1));
      if (tokens.size() > 2) {
        n.setInfraspecificEpithet(tokens.get(2));
      }
    }
    n.setCandidatus(true);
    n.setCultivarEpithet("Red Rose");
    n.setStrain("ACTT 675213");
    n.setWebpage(URI.create("http://gbif.org"));
    n.setNotho(NamePart.SPECIFIC);
    n.setFossil(true);
    n.setRank(rank);
    n.setOrigin(Origin.SOURCE);
    n.setType(NameType.SCIENTIFIC);
    n.setCode(NomCode.BOTANICAL);
    n.setNomStatus(NomStatus.ACCEPTABLE);
    n.updateNameCache();
    n.setAuthorshipNormalized(ANORMALIZER.normalizeName(n));
    n.addRemark("my first note");
    n.addRemark("my second note");
    return n;
  }

  /**
   * Creates a new name instance with an id generated by the static id generator from this class which will not overlap
   */
  public static Name newName() {
    return newName("n" + ID_GEN.getAndIncrement());
  }

  public static List<Name> newNames(int size) {
    List<Name> names = Lists.newArrayList();
    while (size-- > 0) {
      names.add(newName());
    }
    return names;
  }

  public static Synonymy newSynonymy() {
    Synonymy s = new Synonymy();
    s.addHeterotypicGroup(newNames(1 + RND.nextInt(3)));
    while (RND.nextBoolean() || RND.nextBoolean()) {
      s.addHeterotypicGroup(newNames(1 + RND.nextInt(6)));
    }
    return s;
  }

  public static Reference newReference() {
    return newReference(RandomUtils.randomLatinString(25));
  }

  public static Reference newReference(String title) {
    Reference r = setUserDate(new Reference());
    r.setId("r" + ID_GEN.getAndIncrement());
    r.setDatasetKey(TestEntityGenerator.DATASET11.getKey());
    CslData csl = new CslData();
    r.setCsl(csl);
    csl.setType(CSLRefType.ARTICLE_JOURNAL);
    csl.setTitle(title);
    csl.setContainerTitle("Nature");
    csl.setVolume("556");
    csl.setAbstrct("a very long article you should read");
    CslName author1 = new CslName();
    author1.setGiven("John");
    author1.setFamily("Smith");
    author1.setLiteral("John Smith");
    CslName author2 = new CslName();
    author2.setGiven("Betty");
    author2.setFamily("Jones");
    author2.setLiteral("Jones, Betty");
    csl.setAuthor(new CslName[] {author1, author2});
    CslDate date = new CslDate();
    date.setDateParts(new int[][] {{2014, 8, 12}});
    date.setLiteral("2014-8-12");
    csl.setAccessed(date);
    csl.setCategories(new String[] {"A", "B", "C"});
    return r;
  }

  public static Authorship createAuthorship() {
    Authorship a = new Authorship();
    while (a.getAuthors().size() < 2 || RND.nextBoolean()) {
      a.getAuthors().add(RandomUtils.randomAuthor());
    }
    a.setYear(RandomUtils.randomSpeciesYear());
    return a;
  }

  public static CslData createCsl() {
    CslData csl = (CslData) random.create(CslData.class, CslName.class, CslDate.class);
    csl.getOriginalDate().setDateParts(new int[][] {{1752, 4, 4}, {1752, 8, 4}});
    csl.getSubmitted().setDateParts(new int[][] {{1850, 6, 12}});
    csl.setURL("http://gbif.org");
    csl.setDOI("10.1093/database/baw125");
    csl.setISSN("1758-0463");
    return csl;
  }

  public static VerbatimRecord createVerbatim() {
    VerbatimRecord rec = new VerbatimRecord(11, "myFile.txt", DwcTerm.Taxon);
    rec.setDatasetKey(TestEntityGenerator.DATASET11.getKey());
    for (Term t : DwcTerm.values()) {
      rec.put(t, RandomUtils.randomLatinString(1 + RND.nextInt(23)).toLowerCase());
    }
    for (Term t : DcTerm.values()) {
      rec.put(t, RandomUtils.randomUnicodeString(1 + RND.nextInt(77)));
    }
    for (Term t : GbifTerm.values()) {
      rec.put(t, RandomUtils.randomLatinString(1 + RND.nextInt(8)));
    }
    rec.put(UnknownTerm.build("http://col.plus/terms/punk"),
        RandomUtils.randomLatinString(500 + RND.nextInt(2000)));
    rec.addIssue(Issue.ACCEPTED_NAME_MISSING);
    rec.addIssue(Issue.POTENTIAL_VARIANT);
    return rec;
  }

  public static SimpleName newNameRef() {
    return newNameRef(RandomUtils.randomLatinString(5));
  }

  public static SimpleName newNameRef(String id) {
    SimpleName n = new SimpleName();
    n.setId(id);
    n.setName(RandomUtils.randomSpecies());
    n.setAuthorship(RandomUtils.randomAuthorship().toString());
    n.setRank(Rank.SPECIES);
    return n;
  }

  public static NameUsageWrapper newNameUsageTaxonWrapper() {
    NameUsageWrapper nuw = new NameUsageWrapper();
    nuw.setUsage(TAXON1);
    EnumSet<Issue> issues = EnumSet.of(Issue.ACCEPTED_NAME_MISSING, Issue.POTENTIAL_VARIANT,
        Issue.DISTRIBUTION_AREA_INVALID);
    nuw.setIssues(issues);
    nuw.setVernacularNames(
        Arrays.asList(newVernacularName("zeemeeuw", Language.DUTCH), newVernacularName("seagull")));
    return nuw;
  }

  public static NameUsageWrapper newNameUsageSynonymWrapper() {
    NameUsageWrapper nuw = new NameUsageWrapper();
    nuw.setUsage(SYN2);
    EnumSet<Issue> issues = EnumSet.of(Issue.ACCEPTED_NAME_MISSING, Issue.POTENTIAL_VARIANT,
        Issue.DISTRIBUTION_AREA_INVALID);
    nuw.setIssues(issues);
    return nuw;
  }

  public static NameUsageWrapper newNameUsageBareNameWrapper() {
    NameUsageWrapper nuw = new NameUsageWrapper();
    BareName bn = new BareName();
    bn.setName(NAME4);
    nuw.setUsage(bn);
    EnumSet<Issue> issues = EnumSet.of(Issue.ID_NOT_UNIQUE);
    nuw.setIssues(issues);
    return nuw;
  }
  
  public static <T extends UserManaged> List<T> nullifyDate(List<T> managed) {
    for (T m : managed) {
      nullifyDate(m);
    }
    return managed;
  }
  
  public static Taxon nullifyDate(Taxon taxon) {
    nullifyDate((UserManaged) taxon);
    nullifyDate(taxon.getName());
    return taxon;
  }
  
  public static Synonym nullifyDate(Synonym syn) {
    nullifyDate((UserManaged) syn);
    nullifyDate(syn.getName());
    return syn;
  }
  
  public static <T extends UserManaged> T nullifyDate(T managed) {
    managed.setCreated(null);
    managed.setModified(null);
    return managed;
  }
  
  public static <T extends UserManaged> void nullifyDate(Collection<T> managed) {
    managed.forEach(TestEntityGenerator::nullifyDate);
  }

  public static Taxon nullifyUserDate(Taxon taxon) {
    if (taxon != null) {
      nullifyUserDate((UserManaged) taxon);
      nullifyUserDate(taxon.getName());
    }
    return taxon;
  }
  
  public static Synonym nullifyUserDate(Synonym syn) {
    nullifyUserDate((UserManaged) syn);
    nullifyUserDate(syn.getName());
    nullifyUserDate(syn.getAccepted());
    return syn;
  }

  public static <T extends UserManaged> T nullifyUserDate(T managed) {
    if (managed != null) {
      managed.setCreated(null);
      managed.setCreatedBy(null);
      managed.setModified(null);
      managed.setModifiedBy(null);
    }
    return managed;
  }
  
  public static <T extends UserManaged> void nullifyUserDate(Collection<T> managed) {
    managed.forEach(TestEntityGenerator::nullifyUserDate);
  }
  
  public static <T extends UserManaged> T setUser(T managed) {
    managed.applyUser(Users.DB_INIT);
    return managed;
  }

  public static <T extends UserManaged> T setUserDate(T managed) {
    return setUserDate(managed, Users.DB_INIT);
  }
  
  public static <T extends UserManaged> T setUserDate(T managed, Integer userKey) {
    return setUserDate(managed, userKey, LocalDateTime.now());
  }
  
  public static <T extends UserManaged> T setUserDate(T managed, Integer userKey, LocalDateTime dateTime) {
    managed.setCreated(dateTime);
    managed.setCreatedBy(userKey);
    managed.setModified(dateTime);
    managed.setModifiedBy(userKey);
    return managed;
  }
}
