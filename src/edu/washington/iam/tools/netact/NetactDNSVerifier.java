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


package edu.washington.iam.tools.netact;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
    
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
 
import edu.washington.iam.tools.DNSVerifier;
import edu.washington.iam.tools.DNSVerifyException;
import edu.washington.iam.tools.WebClient;
import edu.washington.iam.tools.XMLHelper;


public class NetactDNSVerifier implements DNSVerifier {

    private final Logger log =   LoggerFactory.getLogger(getClass());

    /* Netact soap service provides ownership list */

    private static String netactUrl = "https://netman.cac.washington.edu/sslr/mod_soap/Net/Contacts/soap_dish/index.cgi";
    private static String netactAction = "https://netman.cac.washington.edu/Net/Contacts#get_uwnetids_from_hostname";
    private static boolean initialized = false;

    private static String soapBody = "<get_uwnetids_from_hostname xmlns=\"https://netman.cac.washington.edu/Net/Contacts\">" +
       "<c-gensym3 xsi:type=\"xsd:string\">HOST</c-gensym3></get_uwnetids_from_hostname>";

    private WebClient webClient;

    /**
     * Test if a user has ownership of a domain
     *
     * @param id user's uwnetid
     * @param domain to test
     * @param return list of owners (can be null)
     */

    public boolean isOwner(String dns, String id, List<String> owners) throws DNSVerifyException  {

       boolean isOwner = false;
       if (id==null) id = "";
       log.debug("looking for owner (" + id + ") in " + dns);

       try {
          // format and make the request
          String body = soapBody.replaceFirst("HOST", dns);
          Element resp = webClient.doSoapRequest(netactUrl, netactAction, body);
          Element r1 = XMLHelper.getElementByName(resp, "get_uwnetids_from_hostnameResponse");
          Element r2 = XMLHelper.getElementByName(r1, "Array");
          NodeList nl = r2.getChildNodes();
          for (int i=0;i<nl.getLength(); i++) {
             Node n = nl.item(i);
             if (n.getNodeName().equals("item")) {
                if (n.getTextContent().equals(id)) {
                   if (owners==null) return true;  // done
                   isOwner = true;
                }
                if (owners!=null && !owners.contains(n.getTextContent())) owners.add(n.getTextContent());
             }
          }
       } catch (Exception e) {
          log.debug("netact dns lookup error: " + e);
          throw new DNSVerifyException(e.getMessage() + " : " + e.getCause());
       }
       
       // do substrings too
       dns = dns.replaceFirst("[^\\.]+\\.", "");
       // log.debug("do substrings: " + dns);
       int p = dns.indexOf(".");
       if (p>0) { 
          if (isOwner(dns, id, owners)) {
             if (owners==null) return true;  // done
             isOwner = true;
          }
       }
       return isOwner;
    }

    public boolean isOwner(String dns, String id) throws DNSVerifyException  {
        return isOwner(dns, id, null);
    } 

    public void setWebClient(WebClient v) {
       webClient = v;
    }

    public void init() {
    }
}
 
