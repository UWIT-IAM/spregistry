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

public class AttributeRule implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String id;
    private List<ValueRule> valueRules;

    private boolean editable = false;  // used by controller-velocity

    // create from document element
    public AttributeRule (Element ele) throws FilterPolicyException {

       valueRules = new Vector();
       id = ele.getAttribute("attributeID");
       if (id==null) throw new FilterPolicyException("No attributeId attribute");
       // log.debug("create atr rule from doc: " + id);


       NodeList nl1 = ele.getChildNodes();
       for (int i=0; i<nl1.getLength(); i++) {
           if (nl1.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
           Element e1 = (Element)nl1.item(i);
           String name = e1.getNodeName();
           // log.info("rp ele: " + name);

           if (XMLHelper.matches(name,"PermitValueRule")) {
              valueRules.add(new ValueRule(e1, true));
           }
           if (XMLHelper.matches(name,"DenyValueRule")) {
              valueRules.add(new ValueRule(e1, false));
           }
       }
    }

    // create from name and value
    public AttributeRule (String id, String type, String value) throws FilterPolicyException {

       valueRules = new Vector();
       this.id = id;
       log.debug("create atr rule from value: " + id);

       valueRules.add(new ValueRule(type, value, true));
    }

    // add a value
    public void addValue(String type, String value) throws FilterPolicyException {
       valueRules.add(new ValueRule(type, value, true));
    }

    // remove a value
    public void removeValue(String type, String value) {
       for (int i=0;i<valueRules.size(); i++) {
          if (valueRules.get(i).equals(type, value)) {
             valueRules.remove(i);
             return;
          }
       }
    }

    public boolean equals(String id) {
       if (this.id.equals(id)) return true;
       return false;
    }

    // write
    public void writeXml(BufferedWriter xout) throws IOException {
       if (valueRules.size()>0) {
           xout.write("  <AttributeRule attributeID=\"" + XMLHelper.safeXml(id) + "\">\n");
          for (int i=0; i<valueRules.size(); i++) valueRules.get(i).writeXml(xout);
          xout.write("  </AttributeRule>\n");
       }
    }

    public void setId(String v) {
       id = v;
    }
    public String getId() {
       return (id);
    }

    public void setValueRules(List<ValueRule> v) {
       valueRules = v;
    }
    public List<ValueRule> getValueRules() {
       return (valueRules);
    }

    public void setEditable(boolean v) {
       editable = v;
    }
    public boolean isEditable() {
       return editable;
    }

}

