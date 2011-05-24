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
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.washington.registry.ws.RelyingPartyController;


/* crude way to use the netact domain ownership db */

public class DNSOwnerManager implements OwnerManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ReentrantReadWriteLock locker = new ReentrantReadWriteLock();

    private String dnsApp = "/data/local/bin/domain-owners.pl";
    private String ownerGroupBase = "u_";

    private List<DomainOwner> domainOwners = new Vector<DomainOwner>();
    private long refresh = 600; // seconds

    /**
     * Test if a user has ownership of a domain
     *
     * @param id user's uwnetid
     * @param entity entity id to test 
     */

    public boolean isDomainOwner(String id, String entity) {

       // maybe strip junk from entity
       String domain = entity;
       if (domain.startsWith("http://")) domain = domain.substring(7);
       if (domain.startsWith("https://")) domain = domain.substring(8);
       int i = domain.indexOf("/");
       if (i>0) domain = domain.substring(0,i);
       i = domain.indexOf(":");
       if (i>0) domain = domain.substring(0,i);

       log.debug("looking for owner (" + id + ") in " + domain);

       DomainOwner dom = getDomainOwner(domain);
       for (i=0; i<dom.owners.size(); i++ ) if (dom.owners.get(i).equals(id)) return true;
       log.debug("not yet");
       if (RelyingPartyController.getGroupManager().isMember(ownerGroupBase + "_" + domain, id)) return true;
       log.debug("nope");
       return false;
    }
       
    private DomainOwner getDomainOwner(String domain) {
  
       log.debug(".. looking for " + domain + " in cache");

       locker.readLock().lock();
       for (int i=0; i<domainOwners.size(); i++) {
          if (domainOwners.get(i).domain.equals(domain)) {
             DomainOwner dom = domainOwners.get(i);
             locker.readLock().unlock();
             if ( (dom.mtime + refresh) < (System.currentTimeMillis()/1000) ) { 
                log.debug("dns " + domain + " needs owner refresh");
                getOwners(dom);
             }
             return dom;
          }
       }
       locker.readLock().unlock();
       log.debug(".. not found..  adding");
       DomainOwner dom = new DomainOwner(domain);
       getOwners(dom);
       locker.writeLock().lock();
       domainOwners.add(dom);
       locker.writeLock().unlock();
       return dom;
   }

   private void getOwners(DomainOwner dom) {
      List<String> owners = new Vector<String>();
      try {
          String cmd =  dnsApp + " " + dom.domain;
          Runtime rt = Runtime.getRuntime();
          Process pc = rt.exec(cmd);
          InputStreamReader isr = new InputStreamReader( pc.getInputStream() );
          BufferedReader br = new BufferedReader(isr);
          String line;
          while ((line = br.readLine()) != null) {
             log.debug("netact says: " + line);
             if (line.equals("NONE")) log.debug("netact says none");
             owners.add(line);
          }
          dom.owners = owners;

       } catch (IOException e) {
          log.debug("netact error: " + e); 
       }
       dom.mtime = System.currentTimeMillis()/1000;
    }

    public void init() {
       log.debug("om init");
    }

    public void setOwnerGroupBase(String v) {
       ownerGroupBase = v;
    }


}

