/* ========================================================================
 * Copyright (c) 2012-2013 The University of Washington
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

import java.io.Serializable;

import java.util.List;
import java.util.Vector;
import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import edu.washington.iam.tools.XMLHelper;

import edu.washington.iam.registry.exception.ProxyException;

public class Proxy implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String entityId;
    private List<ProxyIdp> idps;

    private String safePy(String in) {
       return in.replaceAll("\"","\\\"");
    }

    // create from document element
    public Proxy (){}

    public Proxy (Element ele) throws ProxyException {

       entityId = ele.getAttribute("entityId");
       idps = new Vector();
       NodeList nl1 = ele.getChildNodes();
       for (int i=0; i<nl1.getLength(); i++) {
           if (nl1.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
           Element e1 = (Element)nl1.item(i);
           String name = e1.getNodeName();
           if (XMLHelper.matches(name,"ProxyIdp")) {
              idps.add(new ProxyIdp(e1));
           }
       }
       log.debug("create from doc: " + entityId);
    }

    // write xml doc
    public void writeXml(BufferedWriter xout) throws IOException {
       if (idps.size()==0) return;
       xout.write("<Proxy entityId=\"" + XMLHelper.safeXml(entityId) + "\">\n");
       for (int i=0; i<idps.size(); i++) idps.get(i).writeXml(xout);
       xout.write("</Proxy>\n");
    }

    // write py doc
    public void writePy(BufferedWriter xout) throws IOException {
       if (idps.size()==0) return;
       xout.write("\"" + entityId + "\": {\n");
       for (int i=0; i<idps.size(); i++) idps.get(i).writePy(xout);
       xout.write(" },\n");
    }

    public void setEntityId(String v) {
       entityId = v;
    }
    public String getEntityId() {
       return (entityId);
    }
    public void setProxyIdps(List<ProxyIdp> v) {
       idps = v;
    }
    public List<ProxyIdp> getProxyIdps() {
       return (idps);
    }
    public ProxyIdp getProxyIdp(String idp) {
       for (int i=0; i<idps.size(); i++) if (idps.get(i).getIdp().equals(idp)) return idps.get(i);
       return null;
    }
}

