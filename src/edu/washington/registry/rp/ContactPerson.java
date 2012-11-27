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


package edu.washington.iam.registry.rp;

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

import edu.washington.iam.tools.XMLHelper;

import edu.washington.iam.registry.exception.RelyingPartyException;

public class ContactPerson implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());


    private String type;
    private String company;
    private String surName;
    private String givenName;
    private String email;
    private String phone;
      
    // create from document element 
    public ContactPerson (Element ele) throws RelyingPartyException {
       type = ele.getAttribute("contactType");
       if (type==null || !(type.equals("technical") || type.equals("administrative") || type.equals("support") ||
               type.equals("billing") || type.equals("other"))) throw new RelyingPartyException("invalid contact type");
       company = null;
       surName = null;
       givenName = null;
       email = null;
       phone = null;
     
       NodeList chl = ele.getChildNodes();
       for (int i=0; i<chl.getLength(); i++) {
           if (chl.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
           Element ch = (Element)chl.item(i);
           String name = ch.getNodeName();
           if (XMLHelper.matches(name,"Company")) company = ch.getTextContent();
           if (XMLHelper.matches(name,"SurName")) surName = ch.getTextContent();
           if (XMLHelper.matches(name,"GivenName")) givenName = ch.getTextContent();
           if (XMLHelper.matches(name,"EmailAddress")) email = ch.getTextContent();
           if (XMLHelper.matches(name,"TelephoneNumber")) phone = ch.getTextContent();
       }
    }

    // create from string
    public ContactPerson (String t) {
       type = t;
       company = null;
       surName = null;
       givenName = null;
       email = null;
       phone = null;
    }


    public void writeXml(BufferedWriter xout) throws IOException {
       xout.write("  <ContactPerson contactType=\"" + type + "\">\n");
       if (company != null) xout.write("   <Company>" + company + "</Company>\n");
       if (givenName != null) xout.write("   <GivenName>" + givenName + "</GivenName>\n");
       if (surName != null) xout.write("   <SurName>" + surName + "</SurName>\n");
       if (email != null) xout.write("   <EmailAddress>" + email + "</EmailAddress>\n");
       if (phone != null) xout.write("   <TelephoneNumber>" + phone + "</TelephoneNumber>\n");
       xout.write("  </ContactPerson>\n");
    }

    public void setType(String v) {
       type = v;
    }
    public String getType() {
       return (type);
    }

    public void setCompany(String v) {
       company = v;
    }
    public String getCompany() {
       return (company);
    }

    public void setSurName(String v) {
       surName = v;
    }
    public String getSurName() {
       return (surName);
    }

    public void setGivenName(String v) {
       givenName = v;
    }
    public String getGivenName() {
       return (givenName);
    }

    public void setEmail(String v) {
       email = v;
    }
    public String getEmail() {
       return (email);
    }

    public void setPhone(String v) {
       phone = v;
    }
    public String getPhone() {
       return (phone);
    }
}

