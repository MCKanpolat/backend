package org.col.api.model;

import org.gbif.dwc.terms.AcefTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TermRecordTest {
  private TermRecord v;

  @Before
  public void init() {
    v = new TermRecord(11, "myFile.txt", DwcTerm.Taxon);
    initRecord(v);
  }

  private static void initRecord(TermRecord rec) {
    rec.put(DwcTerm.scientificName, "Abies alba");
    rec.put(DwcTerm.scientificNameAuthorship, "D&ouml;ring & M&#246;glich");
    rec.put(DwcTerm.nameAccordingTo, "D\\u00f6ring &amp; M\\366glich");
    rec.put(DwcTerm.namePublishedIn, "D\\u{00f6}ring &amp; M\\366glich");
    rec.put(AcefTerm.Title, "A new species of <i>Neamia</i> (Perciformes: Apogonidae) from the West Pacific Ocean.");
    // from is-6157 http://api.col.plus/taxon/7/info
    rec.put(DwcTerm.vernacularName, "&#75;&#101;&#105;&#104;&#228;&#115;&#108;&#117;&#117;&#104;&#97;&#117;&#107;&#105;");
  }

  @Test
  public void getTerm() throws Exception {
    assertEquals("Abies alba", v.get(DwcTerm.scientificName));
    assertEquals("Abies alba", v.getRaw(DwcTerm.scientificName));
    assertFalse(v.isUnescaped());

    assertEquals("D&ouml;ring & M&#246;glich", v.getRaw(DwcTerm.scientificNameAuthorship));
    assertEquals("Döring & Möglich", v.get(DwcTerm.scientificNameAuthorship));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("Döring & Möglich", v.get(DwcTerm.nameAccordingTo));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("Döring & Möglich", v.get(DwcTerm.namePublishedIn));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("A new species of Neamia (Perciformes: Apogonidae) from the West Pacific Ocean.", v.get(AcefTerm.Title));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("Keihäsluuhauki", v.get(DwcTerm.vernacularName));
    assertTrue(v.isUnescaped());
  }

  @Test
  public void getFirst() throws Exception {
    assertEquals("Abies alba", v.getFirst(GbifTerm.canonicalName, DwcTerm.scientificName));
    assertFalse(v.isUnescaped());

    assertEquals("Döring & Möglich", v.getFirst(GbifTerm.canonicalName, DwcTerm.scientificNameAuthorship));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("Döring & Möglich", v.getFirst(GbifTerm.canonicalName, DwcTerm.nameAccordingTo));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("Döring & Möglich", v.getFirst(GbifTerm.canonicalName, DwcTerm.namePublishedIn));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("A new species of Neamia (Perciformes: Apogonidae) from the West Pacific Ocean.", v.getFirst(GbifTerm.canonicalName, AcefTerm.Title));
    assertTrue(v.isUnescaped());

    init();
    assertFalse(v.isUnescaped());
    assertEquals("Keihäsluuhauki", v.getFirst(GbifTerm.canonicalName, DwcTerm.vernacularName));
    assertTrue(v.isUnescaped());
  }

}