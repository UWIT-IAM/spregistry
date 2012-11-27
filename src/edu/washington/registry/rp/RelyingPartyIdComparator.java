package edu.washington.iam.registry.rp;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.commons.lang.ArrayUtils;


/* Comparator for relying parties.  Sort by reverse dns */

public class RelyingPartyIdComparator implements Comparator  {

    private final Logger log = LoggerFactory.getLogger(getClass());


    public int compare(Object rp1, Object rp2) {
      
        String id1 = (String)rp1;
        String id2 = (String)rp2;

        if (id1.startsWith("https://")) id1 = id1.substring(8);
        if (id1.startsWith("http://")) id1 = id1.substring(7);
        if (id2.startsWith("https://")) id2 = id2.substring(8);
        if (id2.startsWith("http://")) id2 = id2.substring(7);
        return id1.compareTo(id2);
   

/*** if doind dns reverse compare 
        if (!(id1.startsWith("http")&&id2.startsWith("http"))) return id1.compareTo(id2);
        String[] p1 = id1.split("/");
        String[] p2 = id2.split("/");
        // log.info(" p1("+p1.length+") "+p1[2]);

        String[] r1 = p1[2].split("\\.");
        String[] r2 = p2[2].split("\\.");

        // log.info(" r1("+r1.length+"), r2("+r2.length+")");

        ArrayUtils.reverse(r1);
        ArrayUtils.reverse(r2);

        // for (int i=0; i<r1.length; i++) log.info("r_" + i + ": " + r1[i]);
 
        for (int i=0;;i++) {
           if (i<r1.length && i<r2.length) {
              // log.info("  r1("+r1[i]+") r2("+r2[i]+")");
              int v = r1[i].compareTo(r2[i]);
              if (v!=0) return (v);
           } else {
              if (r1.length==r2.length) return p1[2].compareTo(p2[2]);
              if (r1.length<r2.length) return (-1);
              return (1);
           }
        }
 ***/

   }
               
     

   public boolean equals(Object rp1, Object rp2) {
        String id1 = (String)rp1;
        String id2 = (String)rp2;
        return id1.equals(id2);
   }

}

 

