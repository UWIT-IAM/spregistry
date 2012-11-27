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

import java.util.List;
import java.util.Vector;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import edu.washington.iam.registry.exception.AttributeException;
import edu.washington.iam.tools.XMLHelper;

import edu.washington.iam.registry.exception.FilterPolicyException;

public class Attribute implements Serializable  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String id;
    private String name;
    private String description;
    private boolean ferpa;
    private boolean hippa;
    private String authorizingGroup;
    private boolean editable = false;

    // create from document element
    public Attribute (Element ele) throws AttributeException {

       id = ele.getAttribute("id");
       if (id==null) throw new AttributeException("No id for attribute");
       name = ele.getAttribute("name");
       description = ele.getAttribute("description");

       log.debug("create from doc: " + id);

       // get authorized users
       authorizingGroup = ele.getAttribute("authorizingGroup");

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
    public String getAuthorizingGroup() {
       return authorizingGroup;
    }

    public void setEditable(boolean v) {
       editable = v;
    }
    public boolean isEditable() {
       return editable;
    }


}

