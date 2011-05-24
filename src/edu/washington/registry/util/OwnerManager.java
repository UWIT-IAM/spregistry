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

public interface OwnerManager {

    class DomainOwner {
        public String domain;
        public List<String> owners;
        public long mtime;
        public DomainOwner(String n) {
           domain = n;
           owners = new Vector();
           mtime = 0;
        }
    }

    public boolean isDomainOwner(String id, String entity);

    public void init();

}
