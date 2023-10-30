package edu.washington.iam.registry.rp;

public class RelyingPartyEntry {

  public String getMetadataId() {
    return metadataId;
  }

  public void setMetadataId(String metadataId) {
    this.metadataId = metadataId;
  }

  public String getRelyingPartyId() {
    return relyingPartyId;
  }

  public void setRelyingPartyId(String relyingPartyId) {
    this.relyingPartyId = relyingPartyId;
  }

  private String relyingPartyId;
  private String metadataId;
}
