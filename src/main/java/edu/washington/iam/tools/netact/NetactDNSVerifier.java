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


package edu.washington.iam.tools.netact;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.washington.iam.tools.DNSVerifier;
import edu.washington.iam.tools.DNSVerifyException;
import edu.washington.iam.tools.WebClient;

// google-gson
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;


public class NetactDNSVerifier implements DNSVerifier {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /* Netact rest service provides ownership list.
       It is undocumented.  Seems that we have to check for host and domain whether
       or not the target is a host or a domain.
       */

    private static String hostUrl = null;
    private static String domainUrl = null;
    private static String certUrl = null;
    private WebClient webClient;

    /**
     * Test if a user (netid) has ownership of a domain
     *
     * @param id user's uwnetid
     * @param dns domain to test
     * @param owners list of owners (can be null)
     */

    public boolean isOwner(String dns, String id, List<String> owners) throws DNSVerifyException  {

        boolean isOwner = false;
        if (id==null) id = "";
        log.debug("looking for owner (" + id + ") in " + dns);

        try {
            String[] urls = { hostUrl, domainUrl };
            for ( String url : urls ) {
                String respString = webClient.simpleRestGet(url + dns);
                log.debug("got: " + respString);

                JsonParser parser = new JsonParser();
                JsonElement ele = parser.parse(respString);
                if (ele.isJsonObject()) {
                    JsonObject resp = ele.getAsJsonObject();
                    if (resp.get("netids").isJsonArray()) {
                        JsonArray ids = resp.getAsJsonArray("netids");
                        for (int i = 0; i < ids.size(); i++) {
                            JsonPrimitive oidu = ids.get(i).getAsJsonPrimitive();
                            if (oidu==null) continue;
                            String oid = oidu.getAsString();
                            if (oid.equals(id)) {
                                if (owners==null) return true;  // done
                                isOwner = true;
                            }
                            if (owners!=null && !owners.contains(oid)) owners.add(oid);
                        }
                    }
                }

            }


        } catch (Exception e) {
            log.debug("netact dns lookup error: " + e);
            throw new DNSVerifyException(e.getMessage() + " : " + e.getCause());
        }

        // do substrings too
        dns = dns.replaceFirst("[^\\.]+\\.", "");
        // log.debug("do substrings: " + dns);
        int p = dns.indexOf(".");
        if (p>0) {
            if (isOwner(dns, id, owners)) {
                if (owners==null) return true;  // done
                isOwner = true;
            }
        }
        return isOwner;
    }

    //check if a certificate is authorized to administer a domain
    public boolean isCertOwner(String dns, String id, List<String> owners) throws DNSVerifyException  {

        boolean isOwner = false;
        if (id==null) id = "";
        log.debug("looking for owner (" + id + ") in " + dns);

        try {


            String respString = webClient.simpleRestGet(certUrl + dns);
            log.debug("got: " + respString);

            JsonParser parser = new JsonParser();
            JsonElement ele = parser.parse(respString);
            if (ele.isJsonObject()) {
                JsonObject resp = ele.getAsJsonObject();
                if (resp.get("certificates").isJsonArray()) {
                    JsonArray certlist = resp.getAsJsonArray("certificates");
                    for (int i = 0; i < certlist.size(); i++) {
                        JsonPrimitive certcn = certlist.get(i).getAsJsonPrimitive();
                        if (certcn==null) continue;
                        String oid = certcn.getAsString();
                        if (oid.equals(id)) {
                            if (owners==null) return true;  // done
                            isOwner = true;
                        }
                        if (owners!=null && !owners.contains(oid)) owners.add(oid);
                    }
                } else {
                    throw new DNSVerifyException("DNS contacts service did not return certificate array");
                }
            }

        }

        catch (Exception e) {
            log.debug("netact dns lookup error: " + e);
            throw new DNSVerifyException(e.getMessage() + " : " + e.getCause());
        }

        // do substrings too
        dns = dns.replaceFirst("[^\\.]+\\.", "");
        // log.debug("do substrings: " + dns);
        int p = dns.indexOf(".");
        if (p>0) {
            if (isCertOwner(dns, id, owners)) {
                if (owners==null) return true;  // done
                isOwner = true;
            }
        }
        return isOwner;
    }

    public boolean isOwner(String dns, String id) throws DNSVerifyException  {
        return isOwner(dns, id, null);
    }

    public void setWebClient(WebClient v) {
        webClient = v;
    }
    public void setHostUrl(String v) {
        hostUrl = v;
    }
    public void setDomainUrl(String v) {
        domainUrl = v;
    }
    public void setCertUrl(String v) {
        certUrl = v;
    }

    public void init() {
    }
}
 
