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

package edu.washington.registry.filter;

import java.util.List;
import java.util.Vector;
import java.util.Collections;
import java.util.Properties;
import java.io.File;
import java.net.URI;
import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import edu.washington.registry.util.XMLHelper;
import edu.washington.registry.util.GroupManager;
import edu.washington.registry.exception.FilterPolicyException;
import edu.washington.registry.exception.AttributeException;
import edu.washington.registry.exception.AttributeNotFoundException;
import edu.washington.registry.exception.NoPermissionException;

public class XMLFilterPolicyManager implements FilterPolicyManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<FilterPolicyGroup> filterPolicyGroups;
    private GroupManager groupManager;

    private List<Attribute> attributes;
    private String attributeUri;

    private List<Properties> policyGroupSources;
    private String tempUri = "file:/tmp/fp.xml";

    public List<AttributeFilterPolicy> getFilterPolicies(String rpid) {
       log.debug("looking for fps for " + rpid);
       List<AttributeFilterPolicy> list = new Vector();

       for (int g=0; g<filterPolicyGroups.size(); g++) {
          List<AttributeFilterPolicy> fps = filterPolicyGroups.get(g).getFilterPolicies();
          for (int p=0; p<fps.size(); p++) {
             AttributeFilterPolicy fp = fps.get(p);
             if (fp.matches(rpid)) {
                log.debug("  adding " + fp.getEntityId());
                list.add(fp);
             }
          }
       }
       // Collections.sort(list, new FilterPolicyComparator());
       log.info("fp search found "+list.size());
       return list;
    }

    public List<Attribute> getAttributes(String user) {
       List<Attribute> ret = new Vector();
       log.debug("getting editable attributes for " + user);
       for (int i=0; i<attributes.size(); i++) {
          if (userCanEdit(attributes.get(i), user)) ret.add(attributes.get(i));
       }
       log.debug("from " + attributes.size() + ", found " + ret.size());
       return ret;
    }

    /*
     * Update policies from an API PUT. 
     * simplified document
     */
    public void updateRelyingParty(String pgid, Document doc, String remoteUser)
             throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {

       log.info("rp update attr doc for " + pgid);

       FilterPolicyGroup policyGroup = getPolicyGroup(pgid);
       if (policyGroup==null) throw new FilterPolicyException("policy group not found");
       if (!policyGroup.isEditable()) throw new FilterPolicyException("policy group not editable");
       
       // process each policy ( will be only one requirement rule )
       List<Element> pols = XMLHelper.getElementsByName(doc.getDocumentElement(), "AttributeFilterPolicy");
       for (int i=0; i<pols.size(); i++) {
          Element pol = pols.get(i);
          Element reqRule = XMLHelper.getElementByName(pol, "PolicyRequirementRule");
          if (reqRule==null) throw new FilterPolicyException("invalid post");

          // type assumed
          String rpid = reqRule.getAttribute("value");
          log.debug("attr update, pol=" + pgid + ", rp=" + rpid);
          AttributeFilterPolicy afp = policyGroup.getFilterPolicy(rpid);
          if (afp==null) {
              afp = new AttributeFilterPolicy(pgid, rpid);
              policyGroup.getFilterPolicies().add(afp);
          }

          List<Element> attrs = XMLHelper.getElementsByName(pol, "AttributeRule");
          for (int j=0; j<attrs.size(); j++) {
             Element attrEle = attrs.get(j);
             String attributeId = attrEle.getAttribute("attributeID");
             String act = attrEle.getAttribute("action");
             Attribute attribute = getAttribute(attributeId);
             if (!userCanEdit(attribute, remoteUser)) throw new NoPermissionException();

             log.debug(".." + act + " " + attributeId);

             if (act.equals("replace")) afp.replaceAttributeRule(attributeId, attrEle);
             else if (act.equals("remove")) afp.removeAttributeRule(attributeId);
             else throw new FilterPolicyException("unknown action");
          }
       }
 
       // save the new doc
       policyGroup.writePolicyGroup();
    }

    public void addAttributeRule(String policyGroupId, String entityId, String attributeId, String type, String value, String remoteUser)
           throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {
       Attribute attribute = getAttribute(attributeId);
       if (!userCanEdit(attribute, remoteUser)) throw new NoPermissionException();
       FilterPolicyGroup pg = getPolicyGroup(policyGroupId);
       pg.addAttribute(entityId, attributeId, type, value);
    }

    public void removeAttributeRule(String pgid, String entityId, String attributeId, String type, String value, String remoteUser)
         throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {
       Attribute attribute = getAttribute(attributeId);
       if (!userCanEdit(attribute, remoteUser)) throw new NoPermissionException();
       FilterPolicyGroup pg = getPolicyGroup(pgid);
       pg.removeAttribute(entityId, attributeId, type, value);
    }

    private FilterPolicyGroup getPolicyGroup(String pgid) {
       for (int g=0; g<filterPolicyGroups.size(); g++) if (filterPolicyGroups.get(g).getId().equals(pgid)) return filterPolicyGroups.get(g);
       return null;
    }

    private void loadAttributes() {
       attributes = new Vector();
       DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
       builderFactory.setNamespaceAware(false);
       Document doc;

       try {
          DocumentBuilder builder = builderFactory.newDocumentBuilder();
          doc = builder.parse (attributeUri);
       } catch (Exception e) {
          log.error("parse issue: " + e);
          return;
       }

       List<Element> list = XMLHelper.getElementsByName(doc.getDocumentElement(), "Attribute");
       log.info("found " + list.size());

       for (int i=0; i<list.size(); i++) {
          Element fpe = list.get(i);
          try {
             attributes.add(new Attribute(fpe));
          } catch (AttributeException e) {
             log.error("load of element failed: " + e);
          }
       }

       for (int i=0; i<attributes.size(); i++) {
           groupManager.getGroup(attributes.get(i).getAuthorizingGroup());
       }
    }

    private void loadPolicyGroups() {
       filterPolicyGroups = new Vector();
       DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
       builderFactory.setNamespaceAware(true);

       // load policyGroups from each source
       for (int p=0; p<policyGroupSources.size(); p++) {
          try {
             FilterPolicyGroup pg = new FilterPolicyGroup(policyGroupSources.get(p));
             filterPolicyGroups.add(pg);
          } catch (FilterPolicyException e) {
             log.error("could not load policy group ");
          }
       }

    }

    // find an attribute
    public Attribute getAttribute(String id) throws AttributeNotFoundException {
       for (int i=0; i<attributes.size(); i++) {
          if (attributes.get(i).getId().equals(id)) return attributes.get(i);
       }
       throw new AttributeNotFoundException();
    }
     
    // can user edit
    public boolean userCanEdit(Attribute attribute, String user) {
       if (groupManager.isMember(attribute.getAuthorizingGroup(), user)) return true;
       return false;
    }

    public void setPolicyGroupSources(List<Properties> v) {
       policyGroupSources = v;
    }

    public List<FilterPolicyGroup> getFilterPolicyGroups() {
       return filterPolicyGroups;
    }

    public List<Attribute> getAttributes() {
       return attributes;
    }

    public void setAttributeUri(String v) {
       attributeUri = v;
    }
    public void setTempUri(String v) {
       tempUri = v;
    }

    public void setGroupManager(GroupManager v) {
       groupManager = v;
    }

    public void init() {
       loadAttributes();
       loadPolicyGroups();
    }

}
