/* ========================================================================
 * Copyright (c) 2012 The University of Washington
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

package edu.washington.iam.registry.proxy;

import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.washington.iam.registry.ws.RelyingPartyController;
import edu.washington.iam.tools.XMLHelper;
import edu.washington.iam.registry.exception.ProxyException;
import edu.washington.iam.registry.exception.NoPermissionException;

import edu.washington.iam.registry.rp.RelyingPartyManager;
import edu.washington.iam.registry.rp.RelyingParty;
import edu.washington.iam.registry.exception.RelyingPartyException;

public class XMLProxyManager implements ProxyManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ReentrantReadWriteLock locker = new ReentrantReadWriteLock();

    private List<Proxy> proxys;
    private String proxyUri;
    private String sourceName;
    private String proxyMdUri;
    private String pyUri = "file:/data/local/etc/proxy/sp-secrets";
    private int proxyRefresh = 0;  // seconds

    private List<Properties> policyGroupSources;
    private String tempUri = "file:/tmp/sp-proxy.xml";
    private String tempPyUri = "file:/tmp/sp-proxy.py";

    Thread reloader = null;

    private long modifyTime = 0;

    public void refreshProxyIfNeeded() {
       log.debug("prox reloader checking...");
       File f = new File(sourceName);
       if (f.lastModified()>modifyTime) {
          log.debug("reloading proxys from  " + proxyUri);
          locker.writeLock().lock();
          try {
             loadProxys();
          } catch (Exception e) {
             log.error("reload errro: " + e);
          }
          locker.writeLock().unlock();
          log.debug("reload completed, time now " + modifyTime);
       }
    }

    private void loadProxys() {
       DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
       builderFactory.setNamespaceAware(false);
       Document doc;
       proxys = new Vector();

       try {
          DocumentBuilder builder = builderFactory.newDocumentBuilder();
          doc = builder.parse (proxyUri);
       } catch (Exception e) {
          log.error("parse issue: " + e);
          return;
       }

       // update the timestamp
       File f = new File(sourceName);
       modifyTime = f.lastModified();
       log.debug("filter load " + f.getName() + ": time = " + modifyTime);

       List<Element> list = XMLHelper.getElementsByName(doc.getDocumentElement(), "Proxy");
       log.info("found " + list.size());

       for (int i=0; i<list.size(); i++) {
          Element pxe = list.get(i);
          try {
             proxys.add(new Proxy(pxe));
          } catch (ProxyException e) {
             log.error("load of element failed: " + e);
          }
       }
    }

    public List<Proxy> getProxys() {
       List<Proxy> ret = new Vector();
       for (int p=0; p<proxys.size(); p++) ret.add(proxys.get(p));
       return ret;
    }

    public Proxy getProxy(String rpid) {
       log.debug("looking for proxy for " + rpid);
       for (int p=0; p<proxys.size(); p++) {
          Proxy px = proxys.get(p);
          if (px.getEntityId().equals(rpid)) return px;
       }
       return null;
    }

    public int removeRelyingParty(String rpid) {
       log.debug("looking to delete proxy for " + rpid);
       refreshProxyIfNeeded();
       for (int p=0; p<proxys.size(); p++) {
          Proxy px = proxys.get(p);
          if (px.getEntityId().equals(rpid)) {
             proxys.remove(p);
          }
       }
       // save changes
       writeProxyFiles();

       return 200;
    }

    /*
     * Update proxy from an API PUT. 
     */
    public void updateProxy(Proxy newproxy){
       log.info("proxy update " + newproxy.getEntityId());

       refreshProxyIfNeeded();
       // replace the entry
       boolean np = true;
       for (int p=0; p<proxys.size(); p++) {
          if (proxys.get(p).getEntityId().equals(newproxy.getEntityId())) {
             proxys.set(p, newproxy);
             np = false;
          }
       }
       if (np) proxys.add(newproxy);

       // save the new docs
       writeProxyFiles();
    }


    class ProxyReloader extends Thread {
        
        public void run() {
           log.debug("proxy reloader running: interval = " + proxyRefresh);

           while (true) {
              refreshProxyIfNeeded();
              try {
                 if (isInterrupted()) {
                    log.info("interrupted during processing");
                    break;
                 }
                 Thread.sleep(proxyRefresh * 1000);
              } catch (InterruptedException e) {
                 log.info("sleep interrupted");
                 break;
              }
           }
        }

    }

  static String xmlStart = "<Proxies>\n";
  static String xmlEnd = "</Proxies>\n";

  public int writeProxys() {

      log.debug("saving proxy xml");
      try {
         URI xUri = new URI(tempUri);
         File xfile = new File(xUri);
         FileWriter xstream = new FileWriter(xfile);
         BufferedWriter xout = new BufferedWriter(xstream);

         // write header
         xout.write(xmlStart);

         // write policies
         for (int i=0; i<proxys.size();i++) {
            proxys.get(i).writeXml(xout);
         }
   
         // write trailer
         xout.write(xmlEnd);
         xout.close();
      } catch (IOException e) {
         log.error("write io error: " + e);
         return 1;
      } catch (URISyntaxException e) {
         log.error("bad uri error: " + e);
         return 1;
      }

      // move the temp file to live
      try {
         File live = new File(new URI(proxyUri));
         File temp = new File(new URI(tempUri));
         temp.renameTo(live);
       } catch (Exception e) {
          log.error("rename: " + e);
          return 1;
       }
       return 0;
   }

  public int writePyProxys() {
  
      log.debug("saving proxy py");
      try {
         URI xUri = new URI(tempPyUri);
         File xfile = new File(xUri);
         FileWriter xstream = new FileWriter(xfile);
         BufferedWriter xout = new BufferedWriter(xstream);

         // write header
         xout.write("{\n");

         // write policies
         for (int i=0; i<proxys.size();i++) {
            proxys.get(i).writePy(xout);
         }

         // write trailer
         xout.write("}");
         xout.close();
      } catch (IOException e) {
         log.error("write io error: " + e);
         return 1;
      } catch (URISyntaxException e) {
         log.error("bad uri error: " + e);
         return 1;
      }

      // move the temp file to live
      try {
         File live = new File(new URI(pyUri));
         File temp = new File(new URI(tempPyUri));
         temp.renameTo(live);
       } catch (Exception e) {
          log.error("rename: " + e);
          return 1;
       }
       return 0;
   }


  public int writeProxyMd() {

      log.debug("saving proxy md");
      RelyingPartyManager rpManager = RelyingPartyController.getRelyingPartyManager();
      try {
         URI xUri = new URI(proxyMdUri);
         File xfile = new File(xUri);
         FileWriter xstream = new FileWriter(xfile);
         BufferedWriter xout = new BufferedWriter(xstream);

         // write header
         xout.write(proxyMdXmlStart);

         // write policies
         for (int i=0; i<proxys.size();i++) {
            String pid = proxys.get(i).getEntityId();
            log.debug("looking for " + pid);
            for (String metadataId : rpManager.getMetadataIds()) {
               try {
                  RelyingParty rp = rpManager.getRelyingPartyById(pid, metadataId);
                  log.debug("found in " + metadataId);
                  rp.writeXml(xout);
               } catch (RelyingPartyException e) {
               }
            }
         }
   
         // write trailer
         xout.write(proxyMdXmlEnd);
         xout.close();
      } catch (IOException e) {
         log.error("write io error: " + e);
         return 1;
      } catch (URISyntaxException e) {
         log.error("bad uri error: " + e);
         return 1;
      }

      return 0;
   }

   // These are direct copies from the metadata class, which is no longer accessible (except through rpManager).
   // Since XMLProxyManager should be switched off in place of DB access, this whole class should no longer be
   // reached. I've added this just to stay compatible until we're comfortable enough with the DB version
   // that we feel we can delete this entirely.
   private String proxyMdXmlStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
           "<EntitiesDescriptor Name=\"urn:washington.edu:rpedit\"\n" +
           "    xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n" +
           "    xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
           "    xmlns:shibmd=\"urn:mace:shibboleth:metadata:1.0\"\n" +
           "    xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"\n" +
           "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
   private String proxyMdXmlEnd = "</EntitiesDescriptor>";

   private void writeProxyFiles() {
       writeProxys();
       writeProxyMd();
       writePyProxys();
   }
       
    public void setProxyUri(String v) {
       proxyUri = v;
       sourceName = proxyUri.replaceFirst("file:","");
    }
    public void setProxyMdUri(String v) {
       proxyMdUri = v;
    }
    public void setTempUri(String v) {
       tempUri = v;
    }
    public void setPyUri(String v) {
       pyUri = v;
    }
    public void setTempPyUri(String v) {
       tempPyUri = v;
    }

    public void setProxyRefresh(int i) {
       proxyRefresh = i;
    }

    public void init() {
       loadProxys();

       // start proxy list refresher
       if (proxyRefresh>0) {
          reloader = new Thread(new ProxyReloader());
          reloader.start();
       }
       
    }

    public void cleanup() {
        if (reloader != null) reloader.interrupt();
    }

}
