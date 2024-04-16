/* ========================================================================
 * Copyright (c) 2009-2011 The University of Washington
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

package edu.washington.iam.registry.rp;

import edu.washington.iam.registry.exception.RelyingPartyException;
import edu.washington.iam.tools.XMLHelper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class AssertionConsumerService implements Serializable {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private String binding;
  private String location;
  private String index;

  // create from document element
  public AssertionConsumerService(Element ele) throws RelyingPartyException {
    binding = ele.getAttribute("Binding");
    location = ele.getAttribute("Location");
    index = ele.getAttribute("index");
    if (binding == null || location == null || index == null)
      throw new RelyingPartyException("missing ACS attributes");
    if (!binding.startsWith("urn:")) throw new RelyingPartyException("invalid ACS binding");
    if (!location.startsWith("http")) throw new RelyingPartyException("invalid ACS location");
    try {
      int i = Integer.parseInt(index);
    } catch (NumberFormatException e) {
      throw new RelyingPartyException("invalid acs index");
    }
  }

  // create from inputs
  public AssertionConsumerService(int i, String b, String l) {
    binding = b;
    location = l;
    index = "" + i;
  }

  /**
   * public Element toDOM(Document doc) {
   * Element acs = doc.createElement("AssertionConsumerService");
   * acs.setAttribute("Binding", binding);
   * acs.setAttribute("Location", location);
   * acs.setAttribute("index", index);
   * return acs;
   * }
   **/
  public void writeXml(BufferedWriter xout) throws IOException {
    xout.write(
        "   <AssertionConsumerService Binding=\""
            + XMLHelper.safeXml(binding)
            + "\" Location=\""
            + XMLHelper.safeXml(location)
            + "\" index=\""
            + XMLHelper.safeXml(index)
            + "\"/>\n");
  }

  public void setBinding(String v) {
    binding = v;
  }

  public String getBinding() {
    return (binding);
  }

  public void setLocation(String v) {
    location = v;
  }

  public String getLocation() {
    return (location);
  }

  public void setIndex(String v) {
    index = v;
  }

  public String getIndex() {
    return (index);
  }
}
