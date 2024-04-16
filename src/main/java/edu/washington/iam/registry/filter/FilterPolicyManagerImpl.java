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

package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.AttributeNotFoundException;
import edu.washington.iam.registry.exception.FilterPolicyException;
import edu.washington.iam.registry.rp.RelyingParty;
import edu.washington.iam.tools.XMLHelper;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FilterPolicyManagerImpl implements FilterPolicyManager {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private FilterPolicyDAO filterPolicyDAO;

  @Autowired private AttributeDAO attributeDAO;

  @Override
  public AttributeFilterPolicy getFilterPolicy(FilterPolicyGroup filterPolicyGroup, String rpid) {
    log.debug(
        String.format(
            "getting filter policy: pgid: %s; rpid: %s",
            filterPolicyGroup != null ? filterPolicyGroup.getId() : "null", rpid));
    return filterPolicyDAO.getFilterPolicy(filterPolicyGroup, rpid);
  }

  @Override
  public List<AttributeFilterPolicy> getFilterPolicies(RelyingParty rp) {
    log.debug("looking for fps for " + rp.getEntityId());
    List<AttributeFilterPolicy> list = new Vector();

    for (FilterPolicyGroup filterPolicyGroup : filterPolicyDAO.getFilterPolicyGroups()) {
      List<AttributeFilterPolicy> attributeFilterPolicies =
          filterPolicyDAO.getFilterPolicies(filterPolicyGroup);
      for (AttributeFilterPolicy attributeFilterPolicy : attributeFilterPolicies) {
        if (attributeFilterPolicy.matches(rp)) {
          list.add(attributeFilterPolicy);
        }
      }
    }

    log.info("fp search found " + list.size());
    return list;
  }

  @Override
  public List<Attribute> getAttributes() {
    return attributeDAO.getAttributes();
  }

  @Override
  public List<Attribute> getAttributes(RelyingParty rp) {
    List<Attribute> ret = new Vector();
    log.debug("getting editable attributes for " + rp.getEntityId());
    List<AttributeFilterPolicy> fps = this.getFilterPolicies(rp);
    int matches = 0;
    for (Attribute attribute : attributeDAO.getAttributes()) {
      Attribute attr = new Attribute(attribute);
      for (AttributeFilterPolicy afp : fps) {
        for (AttributeRule attributeRule : afp.getAttributeRules()) {
          if (attributeRule.getId().equals(attr.getId())) {
            // attributefilterpolicy is set so we can see attribute source with mouseover text
            // on attribute page.
            attr.setAttributeFilterPolicy(afp);
            attr.setAttributeRule(attributeRule);
            matches++;
          }
        }
      }
      ret.add(attr);
    }
    log.debug("from " + attributeDAO.getAttributes().size() + ", found " + matches + " matches");
    return ret;
  }

  /*
   * Update policies from an API PUT.
   * simplified document
   */
  @Override
  public void updateRelyingParty(String pgid, Document doc, String updatedBy)
      throws FilterPolicyException {
    // we have received an XML document (doc) containing the new requested new attribute state
    // "pgid" is the request policygroup as passed from spreg UI, "doc" contains the actual entityid
    // within the XML payload
    log.info("rp update attr doc for " + pgid);

    FilterPolicyGroup policyGroup = filterPolicyDAO.getFilterPolicyGroup(pgid);
    if (policyGroup == null) throw new FilterPolicyException("policy group not found");
    if (!policyGroup.isEditable()) throw new FilterPolicyException("policy group not editable");

    // process each policy ( will be only one requirement rule )
    List<AttributeFilterPolicy> attributeFilterPolicies = new ArrayList<>();
    for (Element policy :
        XMLHelper.getElementsByName(doc.getDocumentElement(), "AttributeFilterPolicy")) {
      Element reqRule = XMLHelper.getElementByName(policy, "PolicyRequirementRule");
      if (reqRule == null) throw new FilterPolicyException("invalid post");

      // type assumed
      String rpid = reqRule.getAttribute("value");
      log.debug("attr update, pol=" + pgid + ", rp=" + rpid);
      AttributeFilterPolicy afp = filterPolicyDAO.getFilterPolicy(policyGroup, rpid);
      if (afp == null) {
        afp = new AttributeFilterPolicy(policyGroup, rpid);
      }

      for (Element attributeRule : XMLHelper.getElementsByName(policy, "AttributeRule")) {
        String attributeId = attributeRule.getAttribute("attributeID");
        String act = attributeRule.getAttribute("action");
        Attribute attribute;
        try {
          attribute = attributeDAO.getAttribute(attributeId);
        } catch (AttributeNotFoundException e) {
          throw new FilterPolicyException(String.format("attribute not found: %s", attributeId), e);
        }

        log.debug(".." + act + " " + attributeId);

        if (act.equals("replace")) afp.replaceAttributeRule(attributeId, attributeRule);
        else if (act.equals("remove")) afp.removeAttributeRule(attributeId);
        else throw new FilterPolicyException("unknown action");
      }
      // create list of filter policies (based on XML doc) to send to updatefilterpolicy method
      attributeFilterPolicies.add(afp);
    }

    filterPolicyDAO.updateFilterPolicies(policyGroup, attributeFilterPolicies, updatedBy);
    // save the new doc
    // policyGroup.writePolicyGroup();
  }

  @Override
  public int removeEditableRelyingParty(String entityId, String updatedBy)
      throws FilterPolicyException {
    int status = 200;
    for (FilterPolicyGroup filterPolicyGroup : this.getFilterPolicyGroups()) {
      if (filterPolicyGroup.isEditable()) {
        log.info(
            String.format("Removing %s from policy group %s", entityId, filterPolicyGroup.getId()));
        status = filterPolicyDAO.removeRelyingParty(filterPolicyGroup, entityId, updatedBy);
      }
    }
    return status;
  }

  @Override
  public int removeRelyingParty(String entityId, String pgid, String updatedBy)
      throws FilterPolicyException {

    return filterPolicyDAO.removeRelyingParty(
        filterPolicyDAO.getFilterPolicyGroup(pgid), entityId, updatedBy);
  }

  @Override
  public FilterPolicyGroup getPolicyGroup(String pgid) {
    return filterPolicyDAO.getFilterPolicyGroup(pgid);
  }

  @Override
  public List<FilterPolicyGroup> getFilterPolicyGroups() {
    return filterPolicyDAO.getFilterPolicyGroups();
  }
}
