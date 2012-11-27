package edu.washington.iam.registry.rp;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/* Comparator for relying parties.  Sort by reverse dns */

public class RelyingPartyComparator implements Comparator  {

    private final Logger log = LoggerFactory.getLogger(getClass());


    public int compare(Object rp1, Object rp2) {
      
        String id1 = ((RelyingParty)rp1).getEntityId();
        if (id1.startsWith("https://")) id1 = id1.substring(8);
        if (id1.startsWith("http://")) id1 = id1.substring(7);
        String id2 = ((RelyingParty)rp2).getEntityId();
        if (id2.startsWith("https://")) id2 = id2.substring(8);
        if (id2.startsWith("http://")) id2 = id2.substring(7);
        RelyingPartyIdComparator c = new RelyingPartyIdComparator();
// log.info("compare " + id1 + " to " + id2);
        return (c.compare(id1, id2));
   }
               
   public boolean equals(Object rp1, Object rp2) {
        String id1 = ((RelyingParty)rp1).getEntityId();
        String id2 = ((RelyingParty)rp2).getEntityId();
        return (id1.equals(id2));
   }

}

 

