/* ========================================================================
 * Copyright (c) 2016 The University of Washington
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
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.entity.StringEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
    
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
 
import edu.washington.iam.tools.WebClient;

// google-gson
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;


public class IdpHelper {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static String[] idpHosts;
    private static String refreshUrl;
    private WebClient webClient;

    /**
     * Notify IdPs of metadata or filter update
     *
     */

    public boolean notifyIdps(String type) {

       int status = 0;
       List<NameValuePair> data = new ArrayList<NameValuePair>();
       data.add(new BasicNameValuePair("type", type));

       log.debug("notify IdPs of update");

       try {
          for ( String host : idpHosts ) {
             String url = String.format(refreshUrl, host);
             status = webClient.simpleRestPut(url, data);
             log.debug(String.format("got: %d", status));
          }

       } catch (Exception e) {
          log.debug("idp notify error: " + e);
       }
       
       return status==200;
    }

    public void setWebClient(WebClient v) {
       webClient = v;
    }
    public void setIdpHosts(String[] v) {
       idpHosts = v;
    }
    public void setRefreshUrl(String v) {
       refreshUrl = v;
    }

    public void init() {
    }
}
 
