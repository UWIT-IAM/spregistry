/* ========================================================================
 * Copyright (c) 2010-2011 The University of Washington
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

package edu.washington.iam.registry.rp;

import java.util.List;
import java.util.Vector;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.lang.InterruptedException;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.washington.iam.tools.XMLHelper;
import edu.washington.iam.registry.exception.RelyingPartyException;

public class Metadata {
 
   private final Logger log = LoggerFactory.getLogger(getClass());
   private final ReentrantReadWriteLock locker = new ReentrantReadWriteLock();

   private String id;
   private String description;
   private boolean editable;
   private String uri;
   private String sourceName;
   private String tempUri;
   private int refreshInterval = 0;
   private List<RelyingParty> relyingParties;

   Thread reloader = null;

    private String xmlStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<EntitiesDescriptor Name=\"urn:washington.edu:rpedit\"\n" +
      "    xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n" +
      "    xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
      "    xmlns:shibmd=\"urn:mace:shibboleth:metadata:1.0\"\n" +
      "    xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"\n" +
      "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
    private String xmlEnd = "</EntitiesDescriptor>";
    private String xmlNotice = "\n  <!-- DO NOT EDIT: This is a binary, created by sp-registry -->\n\n";

    private long modifyTime = 0;

    private void refreshMetadataIfNeeded() {
       log.debug("reloader checking...");
       File f = new File(sourceName);
       if (modifyTime==0) {
          modifyTime = f.lastModified();
          log.debug("init " + f.getName() + ": last mod = " + modifyTime);
       } else {
          if (f.lastModified()>modifyTime) {
             // reload the metadata
             log.debug("reloading metadata for " + id + " from  " + uri);
             locker.writeLock().lock();
             try {
                relyingParties = new Vector();
                loadMetadata();
             } catch (Exception e) {
                log.error("reload errro: " + e);
             }
             locker.writeLock().unlock();
             modifyTime = f.lastModified();
             log.debug("reload completed, time now " + modifyTime);
          }
       }
    }

    class MetadataReloader extends Thread {
        

        public void run() {
           log.debug("reloader running: interval = " + refreshInterval);
           
           // loop on checking the source

           while (true) {
              refreshMetadataIfNeeded();
              try {
                 if (isInterrupted()) {
                    log.info("interrupted during processing");
                    break;
                 }
                 Thread.sleep(refreshInterval * 1000);
              } catch (InterruptedException e) {
                 log.info("sleep interrupted");
                 break;
              }
           }
        }
       
    }

   // create from properties

   public Metadata(Properties prop) throws RelyingPartyException {
       id = prop.getProperty("id");
       description = prop.getProperty("description");
       uri = prop.getProperty("uri");
       sourceName = uri.replaceFirst("file:","");
       tempUri = prop.getProperty("tempUri");
       String v = prop.getProperty("editable");
       if (v.equalsIgnoreCase("true")) editable = true;
       else editable = false;
       v = prop.getProperty("refresh");
       try {
          if (v!=null) refreshInterval = Integer.parseInt(v); // seconds
       } catch (NumberFormatException e) {
          log.error("invalid refresh arg " + v);
       }
       relyingParties = new Vector();
       loadMetadata();

       if (refreshInterval>0) {
          reloader = new Thread(new MetadataReloader());
          reloader.start();
       }
   }

   // load metadata from the url
   private void loadMetadata() throws RelyingPartyException {
      log.info("load relyingParties for " + id + " from " + uri);
      Document doc;
      try {
         DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
         builderFactory.setNamespaceAware(true);
         DocumentBuilder builder = builderFactory.newDocumentBuilder();
         doc = builder.parse (uri);
       } catch (Exception e) {
         log.error("parse issue: " + e);
         throw new RelyingPartyException("bad xml");
       }

       List<Element> list = XMLHelper.getElementsByName(doc.getDocumentElement(), "EntityDescriptor");
       log.info("found " + list.size());

       for (int i=0; i<list.size(); i++) {
          Element rpe = list.get(i);
          if (XMLHelper.getElementByName(rpe, "SPSSODescriptor")==null) continue;
          try {
             RelyingParty rp = new RelyingParty(rpe, this);
             relyingParties.add(rp);
          } catch (RelyingPartyException e) {
             log.error("load of element failed: " + e);
          }

       }
   }

   // load a single rp (from posted xml)
   public void updateRelyingParty(Document doc) throws RelyingPartyException {
      if (!editable) throw new RelyingPartyException("not editable");
      RelyingParty rp = new RelyingParty(doc.getDocumentElement(), this);

      refreshMetadataIfNeeded();
      locker.readLock().lock();
      // remove any existing one
      for (int i=0; i<relyingParties.size(); i++) {
         RelyingParty r = relyingParties.get(i);
         if (r.getEntityId().equals(rp.getEntityId())) {
            relyingParties.remove(i);
            break;
         }
      }

      // add the new
      relyingParties.add(rp);
      locker.readLock().unlock();
      writeMetadata();
   }

   // remove a single rp 
   public void removeRelyingParty(String id) {
      if (!editable) return;
      refreshMetadataIfNeeded();
      locker.readLock().lock();
      for (int i=0; i<relyingParties.size(); i++) {
         RelyingParty r = relyingParties.get(i);
         if (r.getEntityId().equals(id)) {
            relyingParties.remove(i);
            break;
         }
      }
      locker.readLock().unlock();
      writeMetadata();
   }

   // get rp by id
   public RelyingParty getRelyingPartyById(String rpid) throws RelyingPartyException {
      log.debug("md " + id + " looking for " + rpid);
      RelyingParty ret = null;
      locker.readLock().lock();
      for (int i=0; i<relyingParties.size(); i++) {
          if (relyingParties.get(i).getEntityId().equals(rpid)) ret = relyingParties.get(i);
      }
      locker.readLock().unlock();
      if (ret != null) return ret;
      log.debug(" ..nope");
      throw new RelyingPartyException("not found");
   }

   // select rps by match
   public int addSelectRelyingParties(String sel, List<RelyingParty> list) {
      int nrp = 0;
      locker.readLock().lock();
      for (int i=0; i<relyingParties.size(); i++) {
         RelyingParty rp = relyingParties.get(i);
         if (sel!=null && !rp.getEntityId().matches(".*" + sel + ".*")) continue;
         list.add(rp);
         nrp += 1;
      }
      locker.readLock().unlock();
      return nrp;
   }

   // write the metadata
    public int writeMetadata() {

      locker.readLock().lock();
      try {
         URI xUri = new URI(tempUri);
         File xfile = new File(xUri);
         FileWriter xstream = new FileWriter(xfile);
         BufferedWriter xout = new BufferedWriter(xstream);

         // write header
         xout.write(xmlStart);
         xout.write(xmlNotice);

         // write rps
         for (int i=0; i<relyingParties.size();i++) {
            RelyingParty rp = relyingParties.get(i);
            rp.writeXml(xout);
            xout.write("\n");
         }

         // write trailer
         xout.write(xmlEnd);
         xout.close();
      } catch (IOException e) {
         log.error("write io error: " + e);
         locker.readLock().unlock();
         return 1;
      } catch (URISyntaxException e) {
         log.error("bad uri error: " + e);
         locker.readLock().unlock();
         return 1;
      } catch (Exception e) {
         log.error("write error: " + e);
         locker.readLock().unlock();
         return 1;
      }

      // move the temp file to live
      try {
         File live = new File(new URI(uri));
         File temp = new File(new URI(tempUri));
         temp.renameTo(live);
      } catch (Exception e) {
         log.info("rename: " + e);
      }

      locker.readLock().unlock();
      return 0;
   }

   public String getId() {
      return (id);
   }
   public String getDescription() {
      return (description);
   }
   public boolean isEditable() {
      return editable;
   }
   public List<RelyingParty> getRelyingParties() {
      return relyingParties;
   }

    public void setTempUri(String v) {
       tempUri = v;
    }
    public String getTempUri() {
       return tempUri;
    }
   public String getXmlStart() {
      return (xmlStart);
   }
   public String getXmlEnd() {
      return (xmlEnd);
   }
   public String getXmlNotice() {
      return (xmlNotice);
   }

    public void cleanup() {
       log.info("Metadata got signal to cleanup");
       if (reloader!=null) reloader.interrupt();
    }

}

