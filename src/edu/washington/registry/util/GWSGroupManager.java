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


package edu.washington.registry.util;

import java.io.Serializable;
import java.io.InputStream;

import java.util.List;
import java.util.Vector;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.BasicHttpParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import edu.washington.registry.exception.AttributeException;
import edu.washington.registry.util.XMLHelper;

public class GWSGroupManager implements GroupManager  {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // GWS connection params
    private String urlBase = "https://iam-ws.u.washington.edu:7443/";
    private String certFile = null;
    private String keyFile = null;
    private String caFile = null;
    private String memberXPath;
    private ClientConnectionManager connectionManager;
    private boolean initialized = false;
    private DocumentBuilder documentBuilder;
    XPathExpression memberXpathExpression;

    private List<GMGroup> groups;


    // add this group to our list
    public void getGroup(String name) {
       log.debug("getGroup for " + name);
       if (name==null || name.length()==0) return;
       for (int i=0;i<groups.size();i++) {
          if (groups.get(i).name.equals(name)) return;
       }
       GMGroup group = new GMGroup(name);
       getMembers(group);
       groups.add(group);
    }

    // test membership
    public boolean isMember(String groupName, String user) {
       for (int i=0;i<groups.size();i++) {
          GMGroup grp = groups.get(i);
          if (grp.name.equals(groupName)) {
             for (int j=0;j<grp.members.size();j++) if (grp.members.get(j).equals(user)) return true;
             return false;
          }
       }
       return false;
    }
    
    private void getMembers(GMGroup group) {
       group.members = new Vector();
       log.debug("gettting members for " + group.name);
       DefaultHttpClient httpclient = new DefaultHttpClient((ClientConnectionManager)connectionManager, new BasicHttpParams());
       try {

          log.debug(" url is " + urlBase + group.name + "/member");

          HttpGet httpget = new HttpGet(urlBase + group.name + "/member");

          HttpResponse response = httpclient.execute(httpget);
          HttpEntity entity = response.getEntity();

          // null is error - should get something
          if (entity == null) {
             throw new AttributeException("httpclient get exception");
          }


          // parse response text
          Document doc = documentBuilder.parse(entity.getContent());

          Object result = memberXpathExpression.evaluate(doc, XPathConstants.NODESET);
          NodeList nodes = (NodeList) result;
          log.debug("got " + nodes.getLength() + " matches to the xpath");

          for (int j = 0; j < nodes.getLength(); j++) {
              String mbr = (String)nodes.item(j).getTextContent();
              log.debug("add mbr " + mbr);
              group.members.add(mbr);
          }

/**

          List<Element> list =  XMLHelper.getElementsByClass(doc.getDocumentElement(), "member");
          log.debug("found " + list.size() + " members"); 
          for (int i=0; i<list.size(); i++) {
             Element ele = list.get(i);
             // try {
                group.members.add(ele.getTextContent());
             // }
          }
 **/
       } catch (Exception e) {
          log.error("exception " + e);
       }
    }

    // initialize

    public void init() {
       log.debug("gws client init");

       groups = new Vector();

       // init the doc system
       try {
          DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
          domFactory.setNamespaceAware(false);
          domFactory.setValidating(false);
          String feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
          domFactory.setFeature(feature, false);
          documentBuilder = domFactory.newDocumentBuilder();
    
       } catch (ParserConfigurationException e) {
          log.error("javax.xml.parsers.ParserConfigurationException: " + e);
       }

       // init xpath
       try {
          XPath xpath = XPathFactory.newInstance().newXPath();
          memberXpathExpression = xpath.compile(memberXPath);
       } catch (XPathExpressionException e) {
          log.error("xpath expr: " + e);
       }
    
       // init SSL
       // System.setProperty( "javax.net.debug", "ssl");

       try {
         if (caFile!=null && certFile!=null && keyFile!=null) {
            log.info("using the socketfactory: ca=" + caFile + ", cert=" + certFile + ", key=" + keyFile);
            SRConnectionManager srcm = new SRConnectionManager(urlBase, caFile, certFile, keyFile);
            connectionManager = srcm.getConnectionManager();
/**
         } else {
            log.info("using default socketfactory");
            socketFactory = new SSLSocketFactory();
 **/
         }

         initialized = true;

       } catch (Exception e) {
          log.error(" " + e);
       }
       log.debug("gws client initialize done");
    }

    public void setCertFile(String v) {
       certFile = v;
    }
    public void setKeyFile(String v) {
       keyFile = v;
    }
    public void setCaFile(String v) {
       caFile = v;
    }
    public void setUrlBase(String v) {
       urlBase = v;
    }
    public void setMemberXPath(String v) {
       memberXPath = v;
    }


}

