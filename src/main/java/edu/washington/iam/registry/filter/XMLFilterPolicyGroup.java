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

package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.FilterPolicyException;
import edu.washington.iam.tools.XMLHelper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLFilterPolicyGroup {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ReentrantReadWriteLock locker = new ReentrantReadWriteLock();

  private String id;
  private String description;
  private Document doc;
  private boolean editable;
  private String uri;
  private String sourceName;
  private String tempUri;
  private int refreshInterval = 0;
  private List<AttributeFilterPolicy> filterPolicies;

  private String xmlStart =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<AttributeFilterPolicyGroup id=\"ServerRegPolicy\"\n"
          + "  xmlns=\"urn:mace:shibboleth:2.0:afp\"\n"
          + "  xmlns:basic=\"urn:mace:shibboleth:2.0:afp:mf:basic\"\n"
          + "  xmlns:saml=\"urn:mace:shibboleth:2.0:afp:mf:saml\"\n"
          + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
          + "  xsi:schemaLocation=\"urn:mace:shibboleth:2.0:afp classpath:/schema/shibboleth-2.0-afp.xsd\n"
          + "    urn:mace:shibboleth:2.0:afp:mf:basic classpath:/schema/shibboleth-2.0-afp-mf-basic.xsd\n"
          + "    urn:mace:shibboleth:2.0:afp:mf:saml classpath:/schema/shibboleth-2.0-afp-mf-saml.xsd\">\n";

  private String xmlEnd = "</AttributeFilterPolicyGroup>";
  private String xmlNotice =
      "\n  <!-- DO NOT EDIT: This is a binary, created by sp-registry -->\n\n";

  private long modifyTime = 0;

  Thread reloader = null;

  public void refreshPolicyIfNeeded() {
    log.debug("fp reloader checking...");
    File f = new File(sourceName);
    if (f.lastModified() > modifyTime) {
      log.debug("reloading policy for " + id + " from  " + uri);
      locker.writeLock().lock();
      try {
        loadPolicyGroup();
      } catch (Exception e) {
        log.error("reload errro: " + e);
      }
      locker.writeLock().unlock();
      log.debug("reload completed, time now " + modifyTime);
    }
  }

  // thread to sometimes reload the policies
  class PolicyReloader extends Thread {

    public void run() {
      log.debug("policy reloader running: interval = " + refreshInterval);

      // loop on checking the source

      while (true) {
        refreshPolicyIfNeeded();
        try {
          if (isInterrupted()) {
            log.info("interrupted during processing");
            break;
          }
          Thread.sleep(refreshInterval * 1000);
        } catch (InterruptedException e) {
          log.info("sleep interrupted");
          break;
        }
      }
    }
  }

  public XMLFilterPolicyGroup(Properties prop) throws FilterPolicyException {
    id = prop.getProperty("id");
    description = prop.getProperty("description");
    uri = prop.getProperty("uri");
    sourceName = uri.replaceFirst("file:", "");
    tempUri = prop.getProperty("tempUri");
    String v = prop.getProperty("editable");
    if (v.equalsIgnoreCase("true")) editable = true;
    else editable = false;
    v = prop.getProperty("refresh");
    try {
      if (v != null) refreshInterval = Integer.parseInt(v); // seconds
    } catch (NumberFormatException e) {
      log.error("invalid refresh arg " + v);
    }
    loadPolicyGroup();
    if (refreshInterval > 0) {
      reloader = new Thread(new PolicyReloader());
      reloader.start();
    }
  }

  /*
   * Load policies.  This code allows us to input more complex documents
   * than we produce. e.g. multiple requirement rules, split requirement rules.
   */
  private void loadPolicyGroup() throws FilterPolicyException {
    log.info("load policy group for " + id + " from " + uri);
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(true);
    filterPolicies = new Vector();

    if (uri != null) {
      try {
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        doc = builder.parse(uri);
      } catch (Exception ex) {
        log.error("parse issue: " + ex);
        throw new FilterPolicyException("parse error");
      }

      // update the timestamp
      File f = new File(sourceName);
      modifyTime = f.lastModified();
      log.debug("filter load " + f.getName() + ": time = " + modifyTime);

      List<Element> list =
          XMLHelper.getElementsByName(doc.getDocumentElement(), "AttributeFilterPolicy");
      log.info("found " + list.size());

      for (int i = 0; i < list.size(); i++) {
        Element fpe = list.get(i);
        String afpid = fpe.getAttribute("id");
        // scan requirement rules
        NodeList nl1 = fpe.getChildNodes();
        for (int j = 0; j < nl1.getLength(); j++) {
          if (nl1.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
          Element e1 = (Element) nl1.item(j);
          String name = e1.getNodeName();
          // log.info("rp ele: " + name);

          if (XMLHelper.matches(name, "PolicyRequirementRule")) {
            // log.debug("have requirement rule");
            String type = e1.getAttribute("xsi:type");
            if (type.equals("basic:AttributeRequesterString")) addOrUpdatePolicy(e1, fpe);
            else if (type.equals("saml:AttributeRequesterEntityAttributeExactMatch"))
              addOrUpdateSamlPolicy(e1, fpe);
            else if (type.equals("basic:OR")) {
              // scan rules
              NodeList nl2 = e1.getChildNodes();
              for (int k = 0; k < nl2.getLength(); k++) {
                if (nl2.item(k).getNodeType() != Node.ELEMENT_NODE) continue;
                Element e2 = (Element) nl2.item(k);
                name = e2.getNodeName();
                // log.info("rp ele: " + name);

                if (XMLHelper.matches(name, "Rule")) {
                  addOrUpdatePolicy(e2, fpe);
                }
              }
            }
          }
        }
      }
      log.info(
          String.format(
              "load policy group for %s from %s yields %d results",
              id, uri, filterPolicies.size()));
    }
  }

  private void addOrUpdatePolicy(Element rr, Element ele) {

    String type = rr.getAttribute("xsi:type");
    String value = rr.getAttribute("value");
    if (value.length() == 0) value = rr.getAttribute("regex");
    AttributeFilterPolicy afp = getFilterPolicy(value);
    try {
      if (afp != null) afp.addAttributeRules(ele, editable, id);
      else
        filterPolicies.add(
            new AttributeFilterPolicy(type, value, ele, editable, this.toFilterPolicyGroup()));
    } catch (FilterPolicyException ex) {
      log.error("load of attribute failed: " + ex);
    }
  }

  private void addOrUpdateSamlPolicy(Element rr, Element ele) {

    String type = rr.getAttribute("xsi:type");
    String name = rr.getAttribute("attributeName");
    if (!name.equals("http://macedir.org/entity-category")) {
      log.error("saml policy not category");
      return;
    }
    String value = rr.getAttribute("attributeValue");
    AttributeFilterPolicy afp = getFilterPolicy(value);
    try {
      if (afp != null) afp.addAttributeRules(ele, editable, id);
      else
        filterPolicies.add(
            new AttributeFilterPolicy(type, value, ele, editable, this.toFilterPolicyGroup()));
    } catch (FilterPolicyException ex) {
      log.error("load of attribute failed: " + ex);
    }
  }

  public FilterPolicyGroup toFilterPolicyGroup() {
    FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
    filterPolicyGroup.setDescription(this.getDescription());
    filterPolicyGroup.setId(this.getId());
    filterPolicyGroup.setEditable(this.editable);
    return filterPolicyGroup;
  }

  /*
   * find a policy for an entity.
   */
  public AttributeFilterPolicy getFilterPolicy(String rpid) {
    // log.debug("looking for fp for " + rpid + " in " + id);
    refreshPolicyIfNeeded();
    for (int g = 0; g < filterPolicies.size(); g++) {
      AttributeFilterPolicy fp = filterPolicies.get(g);
      if (fp.matches(rpid)) {
        // log.debug("  found: " + fp.getEntityId());
        return fp;
      }
    }
    return null;
  }

  /*
   * remove a policy for an entity.
   */
  public int removeFilterPolicy(String rpid) throws FilterPolicyException {
    log.debug("removing fp for " + rpid);
    if (!editable) throw new FilterPolicyException("not editable");
    refreshPolicyIfNeeded();
    for (int i = 0; i < filterPolicies.size(); i++) {
      if (filterPolicies.get(i).getEntityId().equals(rpid)) {
        filterPolicies.remove(i);
        break;
      }
    }
    if (writePolicyGroup() == 0) return 200;
    log.error("write policy failed: " + rpid);

    return 500;
  }

  /*
   * add an attribute
   */
  public void addAttribute(String entityId, String attributeId, String type, String value)
      throws FilterPolicyException {
    if (!editable) throw new FilterPolicyException("not editable");
    refreshPolicyIfNeeded();
    AttributeFilterPolicy afp = getFilterPolicy(entityId);
    if (afp == null) throw new FilterPolicyException("not found");
    afp.addAttribute(attributeId, type, value);
  }

  /*
   * remove an attribute
   */
  public void removeAttribute(String entityId, String attributeId, String type, String value)
      throws FilterPolicyException {
    if (!editable) throw new FilterPolicyException("not editable");
    refreshPolicyIfNeeded();
    AttributeFilterPolicy afp = getFilterPolicy(entityId);
    if (afp == null) throw new FilterPolicyException("not found");
    afp.removeAttribute(attributeId, type, value);
  }

  /*
   * Write xml
   */
  public int writePolicyGroup() {

    try {
      URI xUri = new URI(tempUri);
      File xfile = new File(xUri);
      FileWriter xstream = new FileWriter(xfile);
      BufferedWriter xout = new BufferedWriter(xstream);

      // write header
      xout.write(xmlStart);
      xout.write(xmlNotice);

      // write policies
      for (int i = 0; i < filterPolicies.size(); i++) filterPolicies.get(i).writeXml(xout);

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
      File live = new File(new URI(uri));
      File temp = new File(new URI(tempUri));
      temp.renameTo(live);
    } catch (Exception e) {
      log.error("rename: " + e);
      return 1;
    }
    return 0;
  }

  public String getId() {
    return (id);
  }

  public String getUri() {
    return (uri);
  }

  public String getDescription() {
    return (description);
  }

  public List<AttributeFilterPolicy> getFilterPolicies() {
    return filterPolicies;
  }

  public boolean isEditable() {
    return editable;
  }
}
