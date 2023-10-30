/* ========================================================================
 * Copyright (c) 2012 The University of Washington
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

package edu.washington.iam.registry.accessctrl;

import edu.washington.iam.registry.exception.AccessCtrlException;
import java.io.Serializable;
import java.util.List;

public interface AccessCtrlManager extends Serializable {
  public AccessCtrl getAccessCtrl(String entityId);

  public void updateAccessCtrl(AccessCtrl accessCtrl, String updatedBy) throws AccessCtrlException;

  public List<AccessCtrl> getAccessCtrlHistory(String entityId) throws AccessCtrlException;

  public int removeAccessCtrl(String entityId, String updatedBy) throws AccessCtrlException;
}
