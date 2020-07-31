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

/*TODO mattjm 2017-11-13 this class can be deleted soon--no non-test JAVA code reaches it (some tests and javascript
still do)*/


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

public class ProxyIdp implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String idp;
    private String clientId;
    private String clientSecret;

    private String safePy(String in) {
       return in.replaceAll("\"","\\\"");
    }

    // TODO: Our new constructor doesn't check for 'invalid' characters. Sort out if these are really invalid
    // check for any bad chars
    private void isOK(String s) throws ProxyException {
       if (s.indexOf('<')>=0 || s.indexOf('>')>=0 || s.indexOf('"')>=0 || s.indexOf('\'')>=0 ) throw new ProxyException("invalid characters");
    }

    // create from document element
    public ProxyIdp(){}
    public ProxyIdp (Element ele) throws ProxyException {

       idp = ele.getAttribute("idp");
       clientId = ele.getAttribute("clientId");
       clientSecret = ele.getAttribute("clientSecret");
       isOK(clientId);
       isOK(clientSecret);
       log.debug("create from doc: " + clientId);
    }

    // write xml doc
    public void writeXml(BufferedWriter xout) throws IOException {
        xout.write("<ProxyIdp idp=\"" + XMLHelper.safeXml(idp) 
                   + "\" clientId=\"" + XMLHelper.safeXml(clientId) 
                   + "\" clientSecret=\"" + XMLHelper.safeXml(clientSecret) + "\"/>\n");
    }

    // write py doc
    public void writePy(BufferedWriter xout) throws IOException {
       xout.write("\"" + idp + "\": {\"key\": \"" + safePy(clientId) + "\", \"secret\": \"" + safePy(clientSecret) + "\"},\n");
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

