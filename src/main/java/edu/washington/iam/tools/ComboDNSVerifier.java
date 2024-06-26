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

package edu.washington.iam.tools;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComboDNSVerifier implements DNSVerifier {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private DNSVerifier netVerifier = null;
  private DNSVerifier gwsVerifier = null;

  /**
   * Test if a user has ownership of a domain
   *
   * @param id user's uwnetid
   * @param domain to test
   * @param return list of owners (can be null)
   */
  public boolean isOwner(String entity, String id, List<String> owners) throws DNSVerifyException {
    log.debug("combo verify: owner for " + entity);
    // maybe strip junk from entity
    String dns = entity;
    if (dns.startsWith("http://")) dns = dns.substring(7);
    if (dns.startsWith("https://")) dns = dns.substring(8);
    int i = dns.indexOf("/");
    if (i > 0) dns = dns.substring(0, i);
    i = dns.indexOf(":");
    if (i > 0) dns = dns.substring(0, i);
    i = dns.indexOf("?");
    if (i > 0) dns = dns.substring(0, i);

    log.debug("looking for owner (" + id + ") in " + dns);

    boolean isNetOwner = netVerifier.isOwner(dns, id, owners);
    if (isNetOwner && owners == null) return true;
    log.debug("combo verify: gws for " + dns);
    boolean isGwsOwner = gwsVerifier.isOwner(dns, id, owners);
    if (isNetOwner || isGwsOwner) return true;
    return false;
  }

  public boolean isOwner(String dns, String id) throws DNSVerifyException {
    return isOwner(dns, id, null);
  }

  public void setNetVerifier(DNSVerifier v) {
    netVerifier = v;
  }

  public void setGwsVerifier(DNSVerifier v) {
    gwsVerifier = v;
  }

  public void init() {}
}
