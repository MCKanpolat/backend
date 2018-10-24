package org.col.es.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TermsQuery extends AbstractQuery {

  private final Map<String, Collection<?>> terms;

  public TermsQuery(String field, Collection<?> values) {
    terms = new HashMap<>();
    terms.put(field, values);
  }

}