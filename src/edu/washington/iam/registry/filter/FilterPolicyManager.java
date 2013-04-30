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

import org.w3c.dom.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.washington.iam.registry.exception.FilterPolicyException;
import edu.washington.iam.registry.exception.AttributeNotFoundException;
import edu.washington.iam.registry.exception.NoPermissionException;

public interface FilterPolicyManager extends Serializable {

   public List<Attribute> getAttributes();
   public List<Attribute> getAttributes(String userid);
   public List<AttributeFilterPolicy> getFilterPolicies(String rpid);

    public int removeRelyingParty(String entityId, String pgid)
           throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;

    public void addAttributeRule(String pgid, String entityId, String attributeId, String attrType, String attributeValue, String user)
           throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;
    public void removeAttributeRule(String pgid, String entityId, String attributeId, String attrType, String value, String user)
           throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;

    public List<FilterPolicyGroup> getFilterPolicyGroups();
    public void updateRelyingParty(String pgid, Document doc, String remoteUser)
             throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;
}
