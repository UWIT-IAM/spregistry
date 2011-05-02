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


package edu.washington.registry.filter;

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

import edu.washington.registry.util.XMLHelper;

import edu.washington.registry.exception.FilterPolicyException;

public class Rule implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String id;
    private String type;
    private String value;

    // create from document element
    public Rule (Element ele) throws FilterPolicyException {

       type = ele.getAttribute("xsi:type");
       if (type==null) throw new FilterPolicyException("No type attribute");
       log.debug("create from doc: " + type);

       if (type.equals("basic:AttributeValueString")) {
          value = ele.getAttribute("value");
       } else {
          throw new FilterPolicyException("unknown rule requirement rules not editable");
       }
    }

    // create from strings
    public Rule (String t, String v) throws FilterPolicyException {
       type = t;
       if (type==null) throw new FilterPolicyException("No type attribute");
       value = v;
    }

    public void writeXml(BufferedWriter xout) throws IOException {
       xout.write("     <basic:Rule xsi:type=\"basic:AttributeValueString\" value=\"" + value + "\"/>\n");
    }

    public void setType(String v) {
       type = v;
    }
    public String getType() {
       return (type);
    }

    public void setValue(String v) {
       value = v;
    }
    public String getValue() {
       return (value);
    }

}

