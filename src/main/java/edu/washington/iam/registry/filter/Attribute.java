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

import edu.washington.iam.registry.exception.AttributeException;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class Attribute implements Serializable {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private String id;
  private String name;
  private String description;
  private boolean ferpa;
  private boolean hippa;
  private String authorizingGroup;
  private String type;
  // reqHidden = hidden on attribute request page
  private boolean reqHidden = false;
  private boolean editable = false;
  AttributeFilterPolicy attributeFilterPolicy;
  AttributeRule attributeRule;

  // create from document element
  public Attribute(Element ele) throws AttributeException {

    id = ele.getAttribute("id");
    if (id == null) throw new AttributeException("No id for attribute");
    name = ele.getAttribute("name");
    description = ele.getAttribute("description");
    type = ele.getAttribute("type");
    reqHidden = ele.getAttribute("reqHidden").equals("true");

    log.debug("create from doc: " + id);

    // get authorized users
    authorizingGroup = ele.getAttribute("authorizingGroup");
  }

  // create from another attribute
  public Attribute(Attribute src) {

    id = src.getId();
    name = src.getName();
    description = src.getDescription();
    type = src.getType();
    editable = src.isEditable();
    reqHidden = src.isReqHidden();
  }

  public void setId(String v) {
    id = v;
  }

  public String getId() {
    return (id);
  }

  public String getName() {
    return (name);
  }

  public String getDescription() {
    return description;
  }

  public String getType() {
    return type;
  }

  public String getAuthorizingGroup() {
    return authorizingGroup;
  }

  public AttributeFilterPolicy getAttributeFilterPolicy() {
    return attributeFilterPolicy;
  }

  public void setAttributeFilterPolicy(AttributeFilterPolicy v) {
    attributeFilterPolicy = v;
  }

  public AttributeRule getAttributeRule() {
    return attributeRule;
  }

  public void setAttributeRule(AttributeRule v) {
    attributeRule = v;
  }

  public void setEditable(boolean v) {
    editable = v;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setReqHidden(boolean v) {
    reqHidden = v;
  }

  public boolean isReqHidden() {
    return reqHidden;
  }
}
