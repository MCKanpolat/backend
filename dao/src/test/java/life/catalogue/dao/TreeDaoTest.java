package life.catalogue.dao;

import life.catalogue.api.model.*;
import life.catalogue.db.PgSetupRule;
import life.catalogue.db.TestDataRule;
import life.catalogue.db.TxtTreeDataRule;
import org.gbif.nameparser.api.Rank;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TreeDaoTest extends DaoTestBase {
  static final int catKey = 10;
  static final int TRILOBITA = 100;
  static final int MAMMALIA = 101;

  static final Page PAGE = new Page(0, 100);

  @ClassRule
  public static TxtTreeDataRule treeRule = new TxtTreeDataRule(Map.of(
    catKey, TxtTreeDataRule.TreeData.ANIMALIA,
    TRILOBITA, TxtTreeDataRule.TreeData.TRILOBITA,
    MAMMALIA, TxtTreeDataRule.TreeData.MAMMALIA
  ));

  @BeforeClass
  public static void initSector() {
    System.out.println("Setup sectors & decisions");
  }

  public TreeDaoTest() {
    super(TestDataRule.empty());
  }

  TreeDao dao = new TreeDao(PgSetupRule.getSqlSessionFactory());

  @Test
  public void root() {
    for (TreeNode.Type type : TreeNode.types()) {
      ResultPage<TreeNode> root = noSectors(dao.root(catKey, catKey, type, PAGE));
      assertEquals(1, root.size());
      assertEquals("1", root.getResult().get(0).getId());
      assertEquals("Animalia", root.getResult().get(0).getName());

      root = noSectors(dao.root(TRILOBITA, catKey, type, PAGE));
      assertEquals(1, root.size());
      assertEquals("1", root.getResult().get(0).getId());
      assertEquals("Trilobita", root.getResult().get(0).getName());
    }
  }

  @Test
  public void parents() {
    for (boolean placeholder : List.of(false, true)) {
      for (TreeNode.Type type : TreeNode.types()) {
        List<TreeNode> parents = valid(dao.parents(DSID.key(TRILOBITA, "10"), catKey, placeholder, type));
        assertEquals(6, parents.size());
        assertEquals(Rank.SPECIES, parents.get(0).getRank());
        assertEquals(Rank.GENUS, parents.get(1).getRank());
      }
    }

    // expect placeholders
    for (TreeNode.Type type : TreeNode.types()) {
      List<TreeNode> parents = valid(dao.parents(DSID.key(TRILOBITA, "61"), catKey, true, type));
      assertEquals(5, parents.size());
      assertEquals("Amechilus palaora", parents.get(0).getName());
      assertEquals(Rank.SPECIES, parents.get(0).getRank());
      assertEquals(Rank.GENUS, parents.get(1).getRank());
      assertPlaceholder(parents.get(2), Rank.FAMILY);
      assertPlaceholder(parents.get(3), Rank.ORDER);
      assertEquals(Rank.CLASS, parents.get(4).getRank());
      assertEquals("Trilobita", parents.get(4).getName());
    }

    // test calling a placeholder id
    List<TreeNode> parents = valid(dao.parents(DSID.key(TRILOBITA, RankID.buildID("2", Rank.FAMILY)), catKey, true, null));
  }

  @Test
  public void children() {
  }

  private static void assertPlaceholder(TreeNode n, Rank rank) {
    assertTrue(n instanceof TreeNode.PlaceholderNode);
    assertEquals(rank, n.getRank());
  }

  private static ResultPage<TreeNode> noSectors(ResultPage<TreeNode> nodes) {
    noSectors(nodes.getResult());
    return nodes;
  }

  private static List<TreeNode> noSectors(List<TreeNode> nodes) {
    valid(nodes);
    for (TreeNode n : nodes) {
      assertNull(n.getSectorKey());
    }
    return nodes;
  }

  private static List<TreeNode> valid(List<TreeNode> nodes) {
    for (TreeNode n : nodes) {
      assertNotNull(n.getId());
      assertNotNull(n.getDatasetKey());
      assertNotNull(n.getName());
    }
    return nodes;
  }

  private static ResultPage<TreeNode> valid(ResultPage<TreeNode> nodes) {
    valid(nodes.getResult());
    return nodes;
  }

  static SimpleName nameref(String id) {
    SimpleName nr = new SimpleName();
    nr.setId(id);
    return nr;
  }
}