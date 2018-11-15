package org.col.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.col.api.vocab.Language;

public class Description implements Referenced, VerbatimEntity, IntKey {
  @JsonIgnore
  private Integer key;
  private Integer verbatimKey;
  private String category;
  private String description;
  private Language language;
  private String referenceId;
  
  @Override
  public Integer getKey() {
    return key;
  }
  
  @Override
  public void setKey(Integer key) {
    this.key = key;
  }
  
  @Override
  public Integer getVerbatimKey() {
    return verbatimKey;
  }
  
  @Override
  public void setVerbatimKey(Integer verbatimKey) {
    this.verbatimKey = verbatimKey;
  }
  
  public String getCategory() {
    return category;
  }
  
  public void setCategory(String category) {
    this.category = category;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Language getLanguage() {
    return language;
  }
  
  public void setLanguage(Language language) {
    this.language = language;
  }
  
  @Override
  public String getReferenceId() {
    return referenceId;
  }
  
  @Override
  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Description that = (Description) o;
    return Objects.equals(key, that.key) &&
        Objects.equals(verbatimKey, that.verbatimKey) &&
        Objects.equals(category, that.category) &&
        Objects.equals(description, that.description) &&
        language == that.language &&
        Objects.equals(referenceId, that.referenceId);
  }
  
  @Override
  public int hashCode() {
    
    return Objects.hash(key, verbatimKey, category, description, language, referenceId);
  }
}