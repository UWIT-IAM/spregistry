/* ========================================================================
 * Copyright (c) 2009 The University of Washington
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

import java.io.Serializable;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import edu.washington.registry.util.XMLHelper;

import edu.washington.registry.exception.RelyingPartyException;

// small subset of all possible keydescriptors
public class KeyDescriptor implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());


    private String use;
    private String keyName;
    private String certificate;
      
    // create from document element  (KeyDescriptor)
    public KeyDescriptor (Element ele) throws RelyingPartyException {

       use = ele.getAttribute("use");

       Element ki = XMLHelper.getElementByName(ele, "KeyInfo");
       keyName = null;
       certificate = null;
       if (ki==null) throw new RelyingPartyException("missing keyinfo");

        Element kn = XMLHelper.getElementByName(ki, "KeyName");
        Element x5 = XMLHelper.getElementByName(ki, "X509Data");

        if (kn==null && x5==null) throw new RelyingPartyException("invlaid keyinfo");
       
        if (kn!=null) keyName = kn.getTextContent();
        if (x5!=null) {
            Element crt = XMLHelper.getElementByName(x5, "X509Certificate");
            if (crt!=null) setCertificate(crt.getTextContent());
        }
    }

    // create from dns default
    public KeyDescriptor (String dns) {
       keyName = dns;
       certificate = null;
    }


/**
    public Element toDOM(Document doc) {
        Element kd = doc.createElement("KeyDescriptor");
    // for now, drop the key use
    //     if (use!=null) kd.setAttribute("use", use);
        Element ki = doc.createElement("ds:KeyInfo");
        if (keyName != null) {
           Element e = doc.createElement("ds:KeyName");
           e.appendChild(doc.createTextNode(keyName));
           ki.appendChild(e);
        }
        if (certificate != null) {
           Element e = doc.createElement("ds:X509Data");
           Element c = doc.createElement("ds:X509Certificate");
           c.appendChild(doc.createTextNode(certificate));
           e.appendChild(c);
           ki.appendChild(e);
        }
        kd.appendChild(ki);
       return kd;
    }
 **/

    public void writeXml(BufferedWriter xout) throws IOException {
       xout.write("   <KeyDescriptor>\n");
       xout.write("    <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n");
       if (keyName!=null) xout.write("     <ds:KeyName>" + keyName + "</ds:KeyName>\n");
       if (certificate!=null) xout.write("     <ds:X509Data><ds:X509Certificate>" + certificate + "</ds:X509Certificate></ds:X509Data>\n");
       xout.write("    </ds:KeyInfo>\n");
       xout.write("   </KeyDescriptor>\n");
    }

    // check for duplicate descriptor (ignore 'use')
    public boolean isDuplicate(KeyDescriptor test) {
       if (keyName!=null && (test.getKeyName()==null || !test.getKeyName().equals(keyName))) return false;
       if (keyName==null && test.getKeyName()!=null) return false;
       if (certificate!=null && (test.getCertificate()==null || !test.getCertificate().equals(certificate))) return false;
       if (certificate==null && test.getCertificate()!=null) return false;
       return true;
    }

    public void setUse(String v) {
       use = v;
    }
    public String getUse() {
       return (use);
    }

    public void setKeyName(String v) {
       keyName = v;
    }
    public String getKeyName() {
       return (keyName);
    }

    public void setCertificate(String v) {
       certificate = v.replaceAll("\\s*-----BEGIN CERTIFICATE-----\\s*","").replaceAll("\\s*-----END CERTIFICATE-----\\s*","");
    }
    public String getCertificate() {
       return (certificate);
    }

}

