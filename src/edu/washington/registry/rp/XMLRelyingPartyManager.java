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

package edu.washington.registry.rp;

import java.util.List;
import java.util.Vector;
import java.util.Collections;
import java.util.Properties;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import java.security.SecureRandom;
import java.security.Security;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import edu.washington.registry.util.XMLHelper;
import edu.washington.registry.exception.RelyingPartyException;

public class XMLRelyingPartyManager implements RelyingPartyManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<RelyingParty> relyingParties;
    private List<Metadata> metadata;

    private List<Properties> metadataSources;

    static {
       // org.apache.xml.security.Init.init();
       Security.addProvider(new BouncyCastleProvider());
    }

    public void init() {
       loadMetadata();
    }

    // get selected list of rps
    public List<RelyingParty> getRelyingParties(String sel, String mdid) {
       log.debug("rp search: " + sel + ", md=" + mdid);
       List<RelyingParty> list = new Vector();

       for (int i=0;i<metadata.size();i++) {
          if (mdid!=null && !mdid.equals(metadata.get(i).getId())) continue;
          metadata.get(i).addSelectRelyingParties(sel, list);
       }

       Collections.sort(list, new RelyingPartyComparator());
       log.info("rp search found "+list.size());
       return list;
    }

    // get rp by id
    public RelyingParty getRelyingPartyById(String id, String mdid) throws RelyingPartyException {
       log.debug("rp search: " + id + ", md=" + mdid);
       if (mdid==null) return getRelyingPartyById(id);
       Metadata md = getMetadataById(mdid);
       if (md != null) return md.getRelyingPartyById(id);
       throw new RelyingPartyException("not found");
    }

    public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException {
       log.debug("rp search: " + id);
       for (int i=0;i<metadata.size();i++) {
          try {
             return metadata.get(i).getRelyingPartyById(id);
          } catch (RelyingPartyException e) {
             // log.debug("not that one");
          }
       }
       throw new RelyingPartyException("not found");
    }
    
     private HostnameVerifier hostv = new HostnameVerifier() {
        public boolean verify(String urlhost, SSLSession session) {
            log.info("verify host: "+urlhost+" vs. "+session.getPeerHost());
            return true;
        }
     };

    // create RP by copy of another
    public RelyingParty genRelyingPartyByCopy(String dns, String id) {
       try {
          RelyingParty rp = getRelyingPartyById(id);
          return rp.replicate(dns);
       } catch (RelyingPartyException e) {
             // log.debug("not that one");
       }
       return null;
    }
    

    // create RP by dns lookup 
    public RelyingParty genRelyingPartyByLookup(String dns) throws RelyingPartyException {
       
       RelyingParty rp = null;
       String url = "https://" + dns + "/Shibboleth.sso/Metadata";

       log.info("getrpmd: genRelyingPartyByLookup: " + url);

       // install the all trusting trust manager
       try {
          TrustManager[] managers = { new TrustAnyX509TrustManager() };
          SSLContext sc = SSLContext.getInstance("SSL");
          sc.init(null, managers, new SecureRandom());
          HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
          HttpsURLConnection.setDefaultHostnameVerifier(hostv);
       } catch (Exception e) {
          log.error("trust install: " + e);
          throw new RelyingPartyException("trust insall fails: " + e);
       }
        
       try {
           DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = builderFactory.newDocumentBuilder();
           Document doc = builder.parse (url);
 
           Element ele = doc.getDocumentElement();

           log.info("spm: tagname: " + ele.getTagName());
       
           if (XMLHelper.getElementByName(ele, "SPSSODescriptor")==null) {
                throw new RelyingPartyException("no spsso in document");
           }
           rp = new RelyingParty(ele, "lookup", true);
       } catch (ParserConfigurationException e) {
           throw new RelyingPartyException("parser: " + e);
       } catch (SAXException e) {
           throw new RelyingPartyException("SAXException: " + e);
       } catch (IOException e) {
           throw new RelyingPartyException("IOException: " + e);
       }
       return rp;
    }

    // create RP with defaults
    public RelyingParty genRelyingPartyByDefault(String dns) {
         return new RelyingParty(dns);
    }

    public List<String> getRelyingPartyIds() {
       log.info("spm: getRelyingPartyIds");
       Vector<String> list = new Vector();
       for (int m=0; m<metadata.size(); m++) {
          List<RelyingParty> rps = metadata.get(m).getRelyingParties();
          for (int i=0; i<rps.size(); i++) {
             list.add(rps.get(i).getEntityId());
          }
       }
       Collections.sort(list);
       return list;
    }


    /* load metadata from the xml metadata file */

    protected void loadMetadata() {

       metadata = new Vector();
       relyingParties = new Vector();
       DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
       builderFactory.setNamespaceAware(true);

       // load metadata from each source
       for (int m=0; m<metadataSources.size(); m++) {
          try {
             Metadata md = new Metadata(metadataSources.get(m));
             metadata.add(md);
          } catch (RelyingPartyException e) {
             log.error("could not load metadata: " + e);
          }
       }
    }

    /* load relyingParty from posted xml */

    public int updateRelyingParty(Document indoc, String mdid) throws RelyingPartyException {

       int status = 200;
       log.info("rp update doc, source=" + mdid);
      
       Metadata md = getMetadataById(mdid);
       if (!md.isEditable()) return 403;

       md.updateRelyingParty(indoc);

       md.writeMetadata();
       return (status);

    }


    /* delete relyingParty */

    public int deleteRelyingParty(String id, String mdid) {

       log.info("rp delete doc");
      
       Metadata md = getMetadataById(mdid);
       if (!md.isEditable()) return 403;

       md.removeRelyingParty(id);

       md.writeMetadata();

       return (200);
    }


    /* update relyingParty from fresh copy */

    public RelyingParty updateRelyingPartyMD(RelyingParty rp, RelyingParty frp) {

       int status = 200;
       log.info("rp update rp");
      
       // later
       return rp;

    }

    private Metadata getMetadataById(String mdid) {
       for (int i=0; i<metadata.size(); i++) if (metadata.get(i).getId().equals(mdid)) return metadata.get(i);
       return null;
    }
        

    public List<Metadata> getMetadata() {
       return (metadata);
    }

    public void setMetadataSources(List<Properties> v) {
       metadataSources = v;
    }
    public List<Properties> getMetadataSources() {
       return metadataSources;
    }

}
