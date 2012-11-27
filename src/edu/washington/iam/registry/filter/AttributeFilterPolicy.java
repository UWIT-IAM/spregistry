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

import java.io.Serializable;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.List;
import java.util.Vector;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import edu.washington.iam.tools.XMLHelper;

import edu.washington.iam.registry.exception.FilterPolicyException;

public class AttributeFilterPolicy implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String entityId;
    private boolean regex;
    private boolean editable;
    private List<AttributeRule> attributeRules;
    private String policyGroupId;

    // create from document element ( partly parsed )
    public AttributeFilterPolicy (String type, String name, Element ele, boolean edit, String pgid) throws FilterPolicyException {

       editable = edit;
       attributeRules = new Vector();
       entityId = name;
       policyGroupId = pgid;
       if (type.equals("basic:AttributeRequesterString")) regex = false;
       else if (type.equals("basic:AttributeRequesterRegex")) regex = true;
       else throw new FilterPolicyException("cant use type " + type);

       log.debug("create filter policy for " + entityId + " regex? " + regex);
       addAttributeRules(ele, edit, pgid);
    }

    // create from strings 
    public AttributeFilterPolicy (String pgid, String rpid) {
       editable = false;
       attributeRules = new Vector();
       policyGroupId = pgid;
       entityId = rpid;
       regex = false;
    }

    // add rules
    public void addAttributeRules(Element ele, boolean edit, String pgid) {
       NodeList nl1 = ele.getChildNodes();
       for (int i=0; i<nl1.getLength(); i++) {
           if (nl1.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
           Element e1 = (Element)nl1.item(i);
           String name = e1.getNodeName();
           // log.info("rp ele: " + name);

         try {
           if (XMLHelper.matches(name,"AttributeRule")) {
              // log.debug("have attribute rule");
              attributeRules.add(new AttributeRule(e1));
           }
         } catch (FilterPolicyException e) {
            log.error("issue with attribute: " + e);
         }
       }
    }

    // replace rules
    public void replaceAttributeRule(String id, Element rule) throws FilterPolicyException {
       removeAttributeRule(id);
       attributeRules.add(new AttributeRule(rule));
    }

    // remove an attribute from this policy
    public void removeAttributeRule(String id) {
       for (int i=0; i<attributeRules.size(); i++) {
          if (attributeRules.get(i).getId().equals(id)) {
             attributeRules.remove(i);
             break;
          }
       }
    }


    // add an attribute to this policy
    public void addAttribute(String id, String type, String value) {
      try {
       for (int i=0; i<attributeRules.size(); i++) {
          if (attributeRules.get(i).equals(id)) {
             attributeRules.get(i).addValue(type, value);
             return;
          }
       }
       attributeRules.add(new AttributeRule(id, type, value));
      } catch (FilterPolicyException e) {
         log.error("except: " + e);
      }
    }

    // remove an attribute from this policy
    public void removeAttribute(String id, String type, String value) {
       for (int i=0; i<attributeRules.size(); i++) {
          if (attributeRules.get(i).equals(id)) {
             attributeRules.get(i).removeValue(type, value);
             return;
          }
       }
       // throw exception
    }

    // see if this policy applies to the rp
    public boolean matches(String rpid) {
       if (regex) return rpid.matches(entityId);
       return rpid.equals(entityId);
    }

    // write xml doc
    public void writeXml(BufferedWriter xout) throws IOException {

       // skip if no rules
       if (attributeRules.size()==0) {
           log.debug("no rules for " + entityId);
           return;
       }
       String pid = entityId.replaceAll("[^a-zA-Z0-9]","_");
       xout.write(" <AttributeFilterPolicy id=\"" + pid + "\">\n" + 
         "  <PolicyRequirementRule xsi:type=\"basic:AttributeRequesterString\" value=\"" + entityId + "\"/>\n");
       for (int i=0; i<attributeRules.size(); i++) attributeRules.get(i).writeXml(xout);
       xout.write(" </AttributeFilterPolicy>\n\n");
    }

    public void setEntityId(String v) {
       entityId = v;
    }
    public String getEntityId() {
       return (entityId);
    }

    public void setEditable(boolean v) {
       editable = v;
    }
    public boolean isEditable() {
       return (editable);
    }

    public void setAttributeRules(List<AttributeRule> v) {
       attributeRules = v;
    }
    public List<AttributeRule> getAttributeRules() {
       return (attributeRules);
    }

}

