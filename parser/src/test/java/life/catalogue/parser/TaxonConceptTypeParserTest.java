package life.catalogue.parser;

import com.google.common.collect.Lists;
import life.catalogue.api.vocab.TaxonConceptRelType;
import org.junit.Test;

import java.util.List;

public class TaxonConceptTypeParserTest extends ParserTestBase<TaxonConceptRelType> {

  public TaxonConceptTypeParserTest() {
    super(TaxonConceptTypeParser.PARSER);
  }

  @Test
  public void parse() throws Exception {
    assertParse(TaxonConceptRelType.EQUALS, "equals");
    assertParse(TaxonConceptRelType.EQUALS, "Congruent");
    assertParse(TaxonConceptRelType.EQUALS, "EQ");
  }

  @Override
  List<String> unparsableValues() {
    return Lists.newArrayList("a", "3d");
  }
}
