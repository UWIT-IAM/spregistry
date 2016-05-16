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

import java.util.*;
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

import edu.washington.iam.tools.XMLHelper;
import edu.washington.iam.registry.exception.RelyingPartyException;

public class RelyingPartyManagerImpl implements RelyingPartyManager {

    private final Logger log = LoggerFactory.getLogger(getClass());


    public void setMetadataDAOs(Map<String, MetadataDAO> metadataDAOs) {
        this.metadataDAOs = metadataDAOs;
    }

    private Map<String, MetadataDAO> metadataDAOs;

    private SchemaVerifier schemaVerifier = null;
    public void setSchemaVerifier(SchemaVerifier v) {
        this.schemaVerifier = v;
    }
    
    static {
       Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void init() {
       //loadMetadata();
    }
    @Override
    public void cleanup() {
        for(MetadataDAO metadataDAO : metadataDAOs.values()){
            metadataDAO.cleanup();
        }
    }

    @Override
    public List<RelyingPartyEntry> searchRelyingPartyIds(String searchStr, String metadataId){
        log.debug("rp search: " + searchStr + ", md=" + metadataId);
        List<RelyingPartyEntry> list = new ArrayList<>();
        Map<String, List<String>> idsMap = new HashMap<>();
        if(metadataId == null){
            for(String mdid : metadataDAOs.keySet()){
                idsMap.put(mdid, metadataDAOs.get(mdid).searchRelyingPartyIds(searchStr));
            }
        }
        else if(metadataDAOs.containsKey(metadataId)){
            idsMap.put(metadataId, metadataDAOs.get(metadataId).searchRelyingPartyIds(searchStr));
        }

        for(String mdid : idsMap.keySet()){
            for(String entityId : idsMap.get(mdid)) {
                RelyingPartyEntry rpEntry = new RelyingPartyEntry();
                rpEntry.setRelyingPartyId(entityId);
                rpEntry.setMetadataId(mdid);
                list.add(rpEntry);
            }
        }

        Collections.sort(list, new RelyingPartyEntryComparator());
        return list;
    }

    // get rp by id
    @Override
    public RelyingParty getRelyingPartyById(String id, String mdid) throws RelyingPartyException {
        log.debug("rp search: " + id + ", md=" + mdid);
        if (mdid==null)
            return this.getRelyingPartyById(id);
        return metadataDAOs.get(mdid).getRelyingPartyById(id);
    }

    @Override
    public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException {
        log.debug("rp search: " + id);
        for (MetadataDAO metadataDAO : metadataDAOs.values()){
            try {
                return metadataDAO.getRelyingPartyById(id);
            }
            catch (RelyingPartyException e){

            }
        }
        throw new RelyingPartyException("not found");
    }
    
     private HostnameVerifier hostv = new HostnameVerifier() {
        @Override
        public boolean verify(String urlhost, SSLSession session) {
            log.info("verify host: "+urlhost+" vs. "+session.getPeerHost());
            return true;
        }
     };

    // create RP by copy of another
    @Override
    public RelyingParty genRelyingPartyByCopy(String dns, String id) {
       try {
          RelyingParty rp = this.getRelyingPartyById(id);
          return rp.replicate(dns);
       } catch (RelyingPartyException e) {
             // log.debug("not that one");
       }
       return null;
    }
    

    // create RP by dns lookup
    @Override
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
       } catch (Exception e) {
           throw new RelyingPartyException("Exception: " + e);
       }
       return rp;
    }

    // create RP with defaults
    @Override
    public RelyingParty genRelyingPartyByName(String entityId, String dns) {
         return new RelyingParty(entityId, dns);
    }

    @Override
    public List<String> getMetadataIds() {
        return new ArrayList<>(metadataDAOs.keySet());
    }

    public int updateRelyingParty(RelyingParty relyingParty, String mdid) throws RelyingPartyException {
        int status = 200;
        log.info(String.format("rp update doc, source=%s; rpid=%s", mdid, relyingParty.getEntityId()));
        // do a final verification of the new metadata
        if (schemaVerifier!=null && ! schemaVerifier.testSchemaValid(relyingParty)) throw new RelyingPartyException("schema verify fails");

        metadataDAOs.get(mdid).updateRelyingParty(relyingParty);
        return (status);
    }


    /* delete relyingParty */
    @Override
    public int removeRelyingParty(String id, String mdid) {
        log.info("rp delete doc " + id);

        metadataDAOs.get(mdid).removeRelyingParty(id);
        return (200);
    }

    @Override
    public boolean isMetadataEditable(String mdid){
        return (metadataDAOs.containsKey(mdid) && metadataDAOs.get(mdid).isEditable());
    }

}
