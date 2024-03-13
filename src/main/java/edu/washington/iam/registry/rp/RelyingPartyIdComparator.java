package edu.washington.iam.registry.rp;

import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Comparator for relying parties.  Sort by reverse dns */

public class RelyingPartyIdComparator implements Comparator<String> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public int compare(String id1, String id2) {
    if (id1.startsWith("https://")) id1 = id1.substring(8);
    if (id1.startsWith("http://")) id1 = id1.substring(7);
    if (id2.startsWith("https://")) id2 = id2.substring(8);
    if (id2.startsWith("http://")) id2 = id2.substring(7);
    return id1.compareTo(id2);
  }
}
