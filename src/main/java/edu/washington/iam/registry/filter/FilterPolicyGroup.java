package edu.washington.iam.registry.filter;

public class FilterPolicyGroup {
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  private String id;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  private String description;

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  private boolean editable;
}
