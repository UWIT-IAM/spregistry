/* ========================================================================
 * Copyright (c) 2009 The University of Washington
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Organization implements Serializable {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private String name;
  private String displayName;
  private String url;

  // create from document element
  public Organization(Element ele) throws RelyingPartyException {
    name = null;
    displayName = null;
    url = null;
    NodeList chl = ele.getChildNodes();
    for (int i = 0; i < chl.getLength(); i++) {
      if (chl.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
      Element ch = (Element) chl.item(i);
      String nn = ch.getNodeName();
      if (XMLHelper.matches(nn, "OrganizationName")) name = ch.getTextContent();
      if (XMLHelper.matches(nn, "OrganizationDisplayName")) displayName = ch.getTextContent();
      if (XMLHelper.matches(nn, "OrganizationURL")) url = ch.getTextContent();
    }
    // if (name==null) throw new RelyingPartyException("missing org name");
    // if (displayName==null) throw new RelyingPartyException("missing org displayName");
    // if (url==null) throw new RelyingPartyException("missing org url");
  }

  // create from strings
  public Organization(String n, String d, String u) {
    name = n;
    displayName = d;
    url = u;
  }

  public void writeXml(BufferedWriter xout) throws IOException {
    xout.write("  <Organization>\n");
    if (name != null)
      xout.write(
          "   <OrganizationName xml:lang=\"en\">"
              + XMLHelper.safeXml(name)
              + "</OrganizationName>\n");
    if (displayName != null)
      xout.write(
          "   <OrganizationDisplayName xml:lang=\"en\">"
              + XMLHelper.safeXml(displayName)
              + "</OrganizationDisplayName>\n");
    if (url != null)
      xout.write(
          "   <OrganizationURL xml:lang=\"en\">" + XMLHelper.safeXml(url) + "</OrganizationURL>\n");
    xout.write("  </Organization>\n");
  }

  public void setName(String v) {
    name = v;
  }

  public String getName() {
    return (name);
  }

  public void setDisplayName(String v) {
    displayName = v;
  }

  public String getDisplayName() {
    return (displayName);
  }

  public void setUrl(String v) {
    url = v;
  }

  public String getUrl() {
    return (url);
  }
}
