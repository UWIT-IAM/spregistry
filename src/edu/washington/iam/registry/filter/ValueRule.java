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

public class ValueRule implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String id;
    private String type;
    private String value;
    private List<Rule> rules;
    private boolean permit;

    // create from document element
    public ValueRule (Element ele, boolean pf) throws FilterPolicyException {

       rules = new Vector();
       type = ele.getAttribute("xsi:type");
       if (type==null) throw new FilterPolicyException("No type attribute");
       // log.debug("create from doc: " + type);

       permit = pf;

       // add value as first rule
       if (type.equals("basic:AttributeValueString")) {
          value = ele.getAttribute("value");
          if (value.length()>0) {
              log.debug("adding value as rule: " + value);
              rules.add(new Rule(type, value));
          }
       } else if (type.equals("basic:AttributeValueRegex")) {
          value = ele.getAttribute("regex");
          if (value.length()>0) {
              log.debug("adding value as rule: " + value);
              rules.add(new Rule(type, value));
          }
       }

       NodeList nl1 = ele.getChildNodes();
       for (int i=0; i<nl1.getLength(); i++) {
           if (nl1.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
           Element e1 = (Element)nl1.item(i);
           String name = e1.getNodeName();
           // log.info("rp ele: " + name);

           if (XMLHelper.matches(name,"Rule")) {
              rules.add(new Rule(e1));
           }
       }
    }


    // create from string element
    public ValueRule (String type, String value, boolean pf) throws FilterPolicyException {

       rules = new Vector();
       this.type = type;
       log.debug("create from string: " + type);

       permit = pf;
       this.value = value;
    }

    // equals
    public boolean equals(String type, String value) {
       if (this.type.equals(type) && this.value.equals(value)) return true;
       return false;
    }


    public void writeXml(BufferedWriter xout) throws IOException {
       String pd = "PermitValueRule";
       if (!permit) pd = "DenyValueRule";
       if (rules.size()==0) {
          xout.write("    <" + pd + " xsi:type=\"" + type + "\"/>\n");
       } else if (rules.size()==1) {
          String valueStr = "value";
          if (rules.get(0).getType().equals("basic:AttributeValueRegex")) valueStr = "regex";
          xout.write("    <" + pd + " xsi:type=\"" + rules.get(0).getType() + "\" " + valueStr + "=\"" 
                     + XMLHelper.safeXml(rules.get(0).getValue()) + "\"/>\n");
       } else {
          xout.write("    <" + pd + " xsi:type=\"" + type + "\">\n");
          for (int i=0; i<rules.size(); i++) rules.get(i).writeXml(xout);
          xout.write("    </" + pd + ">\n");
       }
    }

    public void setId(String v) {
       id = v;
    }
    public String getId() {
       return (id);
    }

    public void setType(String v) {
       type = v;
    }
    public String getType() {
       return (type);
    }

    public void setRules(List<Rule> v) {
       rules = v;
    }
    public List<Rule> getRules() {
       return (rules);
    }


}

