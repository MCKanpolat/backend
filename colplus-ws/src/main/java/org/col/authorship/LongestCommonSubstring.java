package org.col.authorship;

/**
 * **********************************************************************
 * Computes the longest  common substring of 2 given strings.
 * Assumes that the character '\1' does not appear in either text.
 * Perhaps, search for a character that does not appear in either text
 * (and make sure SuffixArray.java doesn't choose the same one).
 * % java LongestCommonSubstring tale.txt mobydick.txt
 * ' seemed on the point of being '
 * ***********************************************************************
 */


public class LongestCommonSubstring {

  public static String lcs(String text1, String text2) {
    int N1 = text1.length();

    // concatenate two string with intervening '\1'
    String text = text1 + '\1' + text2;
    int N = text.length();

    // compute suffix array of concatenated text
    SuffixArray suffix = new SuffixArray(text);

    // search for longest common substring
    String lcs = "";
    for (int i = 1; i < N; i++) {

      // adjacent suffixes both from first text string
      if (suffix.index(i) < N1 && suffix.index(i - 1) < N1) continue;

      // adjacent suffixes both from secondt text string
      if (suffix.index(i) > N1 && suffix.index(i - 1) > N1) continue;

      // check if adjacent suffixes longer common substring
      int length = suffix.lcp(i);
      if (length > lcs.length()) {
        lcs = text.substring(suffix.index(i), suffix.index(i) + length);
      }
    }
    return lcs;
  }
}