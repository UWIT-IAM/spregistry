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

package edu.washington.iam.tools.gws;

import edu.washington.iam.tools.Group;
import edu.washington.iam.tools.GroupManager;
import edu.washington.iam.tools.WebClient;
import edu.washington.iam.tools.XMLHelper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class GWSGroupManager implements GroupManager {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private WebClient webClient;
  private String gwsBase = null;

  /**
   * Get a group
   *
   */
  public Group getGroup(String name) {

    Group group = new Group(name);
    log.debug("looking for gws group: " + name);

    try {
      String url = gwsBase + name + "/effective_member";
      Element resp = webClient.doRestGet(url);
      Element grpE = XMLHelper.getElementByName(resp, "group");
      Element mbrsE = XMLHelper.getElementByName(grpE, "members");
      List<Element> mbrs = XMLHelper.getElementsByName(mbrsE, "member");
      log.debug("get  " + mbrs.size() + " group members");
      for (int i = 0; i < mbrs.size(); i++) {
        String mbr = mbrs.get(i).getTextContent();
        log.debug("mbr: " + mbr);
        group.members.add(mbr);
      }

    } catch (Exception e) {
      log.debug("gws lookup error: " + e);
      return null;
    }
    return group;
  }

  public void setWebClient(WebClient v) {
    webClient = v;
  }

  public void setGwsBase(String v) {
    gwsBase = v;
  }

  public void init() {}
}
