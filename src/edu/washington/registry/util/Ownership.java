/* ========================================================================
 * Copyright (c) 2011 The University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */


package edu.washington.registry.util;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/* crude way to use the netact domain ownership db */

public final class Ownership {

    private static final Logger log = LoggerFactory.getLogger(Ownership.class);

    private static String dnsApp = "/data/local/bin/domain-owners.pl";

    private Ownership() {}

    /**
     * Test if a user has ownership of a domain
     *
     * @param id user's uwnetid
     * @param entity entity id to test 
     */

    public static boolean isDomainOwner(String id, String entity) {


       // maybe strip junk from entity
       String domain = entity;
       if (domain.startsWith("http://")) domain = domain.substring(7);
       if (domain.startsWith("https://")) domain = domain.substring(8);
       int i = domain.indexOf("/");
       if (i>0) domain = domain.substring(0,i);
       i = domain.indexOf(":");
       if (i>0) domain = domain.substring(0,i);

       log.debug("looking for owner (" + id + ") in " + domain);

       try {
          String cmd =  dnsApp + " " + domain;
          Runtime rt = Runtime.getRuntime();
          Process pc = rt.exec(cmd);
          InputStreamReader isr = new InputStreamReader( pc.getInputStream() );
          BufferedReader br = new BufferedReader(isr);
          String line;
          while ((line = br.readLine()) != null) {
             log.debug("netact says: " + line);
             if (line.equals("NONE")) return false;
             if (line.equals(id)) return true;
          }
       } catch (IOException e) {
          log.debug("netact error: " + e); 
       }
       return false;
    }

}

