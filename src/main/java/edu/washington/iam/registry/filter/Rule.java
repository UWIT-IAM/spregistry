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
import java.io.IOException;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class Rule implements Serializable {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private String id;
  private String type;
  private String value;

  // create from document element
  public Rule(Element ele) throws FilterPolicyException {

    type = ele.getAttribute("xsi:type");
    if (type == null) throw new FilterPolicyException("No type attribute");
    // log.debug("create from doc: " + type);

    if (type.equals("basic:AttributeValueString")) {
      value = ele.getAttribute("value");
    } else if (type.equals("basic:AttributeValueRegex")) {
      value = ele.getAttribute("regex");
    } else {
      throw new FilterPolicyException("unknown rule requirement rules not editable");
    }
  }

  // create from strings
  public Rule(String t, String v) throws FilterPolicyException {
    type = t;
    if (type == null) throw new FilterPolicyException("No type attribute");
    value = v;
  }

  public void writeXml(BufferedWriter xout) throws IOException {
    String valueStr = "value";
    if (type.equals("basic:AttributeValueRegex")) valueStr = "regex";
    xout.write(
        "     <basic:Rule xsi:type=\""
            + type
            + "\" "
            + valueStr
            + "=\""
            + XMLHelper.safeXml(value)
            + "\"/>\n");
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

  public boolean isString() {
    return type.equals("basic:AttributeValueString");
  }

  public boolean isRegex() {
    return type.equals("basic:AttributeValueRegex");
  }
}
