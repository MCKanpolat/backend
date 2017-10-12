package org.col.api;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

/**
 * TODO: deal with ex-authors
 */
public class Authorship {
  private static final Joiner AUTHORSHIP_JOINER = Joiner.on(", ").skipNulls();

  /**
   * list of basionym authors.
   */
  private List<String> originalAuthors = Lists.newArrayList();

  /**
   * Year of original name publication
   */
  private String originalYear;

  /**
   * list of authors excluding ex- authors
   */
  private List<String> combinationAuthors = Lists.newArrayList();

  /**
   * The year this combination was first published, usually the same as the publishedIn reference.
   * It is used for sorting names and ought to be populated even for botanical names which do not use it in the complete authorship string.
   */
  private String combinationYear;

  public List<String> getOriginalAuthors() {
    return originalAuthors;
  }

  public void setOriginalAuthors(List<String> originalAuthors) {
    this.originalAuthors = originalAuthors;
  }

  public String getOriginalYear() {
    return originalYear;
  }

  public void setOriginalYear(String originalYear) {
    this.originalYear = originalYear;
  }

  public List<String> getCombinationAuthors() {
    return combinationAuthors;
  }

  public void setCombinationAuthors(List<String> combinationAuthors) {
    this.combinationAuthors = combinationAuthors;
  }

  public String getCombinationYear() {
    return combinationYear;
  }

  public void setCombinationYear(String combinationYear) {
    this.combinationYear = combinationYear;
  }

  public boolean isEmpty() {
    return combinationAuthors.isEmpty() && combinationYear == null && originalAuthors.isEmpty() && originalYear == null;
  }

  /**
   * @return true if original year or authors exist
   */
  public boolean hasOriginal() {
    return !originalAuthors.isEmpty() || originalYear != null;
  }

  /**
   * @return true if original year or authors exist
   */
  public boolean hasCombination() {
    return !combinationAuthors.isEmpty() || combinationYear != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Authorship that = (Authorship) o;
    return Objects.equals(originalAuthors, that.originalAuthors) &&
        Objects.equals(originalYear, that.originalYear) &&
        Objects.equals(combinationAuthors, that.combinationAuthors) &&
        Objects.equals(combinationYear, that.combinationYear);
  }

  @Override
  public int hashCode() {
    return Objects.hash(originalAuthors, originalYear, combinationAuthors, combinationYear);
  }

  private String joinAuthors(List<String> authors) {
    //TODO: use & for last author join
    // TODO: offer option to abbreviate with et al.
    return AUTHORSHIP_JOINER.join(authors);
  }

  /**
   * @return the full authorship string as used in scientific names
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (hasOriginal()) {
      sb.append("(");
      sb.append(joinAuthors(originalAuthors));
      if (originalYear != null && !originalAuthors.isEmpty()) {
        sb.append(", ");
        sb.append(originalYear);
      }
      sb.append(") ");
    }
    if (hasCombination()) {
      sb.append(joinAuthors(combinationAuthors));
      if (combinationYear != null && !combinationAuthors.isEmpty()) {
        sb.append(", ");
        sb.append(combinationYear);
      }
    }
    return sb.toString();
  }
}
