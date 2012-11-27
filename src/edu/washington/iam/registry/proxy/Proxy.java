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
    private String idp;
    private String clientId;
    private String clientSecret;

    private String safePy(String in) {
       return in.replaceAll("\"","\\\"");
    }

    // create from document element
    public Proxy (Element ele) throws ProxyException {

       entityId = ele.getAttribute("entityId");
       idp = ele.getAttribute("idp");
       clientId = ele.getAttribute("clientId");
       clientSecret = ele.getAttribute("clientSecret");
       log.debug("create from doc: " + clientId);
    }

    // write xml doc
    public void writeXml(BufferedWriter xout) throws IOException {
       xout.write("<Proxy entityId=\"" + entityId + "\" idp=\"" + idp + "\" clientId=\"" + clientId + "\" clientSecret=\"" + clientSecret + "\"/>\n");
    }

    // write py doc
    public void writePy(BufferedWriter xout) throws IOException {
       xout.write("\"" + entityId + "\": {\n \"Google\": {\"key\": \"" + safePy(clientId) + "\", \"secret\": \"" + safePy(clientSecret) + "\"},\n },\n");
    }

    public void setEntityId(String v) {
       entityId = v;
    }
    public String getEntityId() {
       return (entityId);
    }
    public void setIdp(String v) {
       idp = v;
    }
    public String getIdp() {
       return (idp);
    }
    public void setClientId(String v) {
       clientId = v;
    }
    public String getClientId() {
       return (clientId);
    }
    public void setClientSecret(String v) {
       clientSecret = v;
    }
    public String getClientSecret() {
       return (clientSecret);
    }

}

