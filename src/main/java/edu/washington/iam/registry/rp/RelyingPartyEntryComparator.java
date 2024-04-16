package edu.washington.iam.registry.rp;

import java.util.Comparator;

public class RelyingPartyEntryComparator implements Comparator<RelyingPartyEntry> {
  @Override
  public int compare(RelyingPartyEntry rpe1, RelyingPartyEntry rpe2) {
    int entityIdCompare =
        new RelyingPartyIdComparator().compare(rpe1.getRelyingPartyId(), rpe2.getRelyingPartyId());
    if (entityIdCompare == 0) {
      return rpe1.getMetadataId().compareTo(rpe2.getMetadataId());
    }
    return entityIdCompare;
  }
}
