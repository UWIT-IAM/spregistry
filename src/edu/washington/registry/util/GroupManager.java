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


package edu.washington.registry.util;

import java.util.List;
import java.util.Vector;

public interface GroupManager {

    class GMGroup {
        public String name;
        public List<String> members;
        public GMGroup(String n) {
           name = n;
           members = new Vector();
        }
    }

    // add this group to our list
    public void getGroup(String name);
    
    // test membership
    public boolean isMember(String groupName, String user);

}

