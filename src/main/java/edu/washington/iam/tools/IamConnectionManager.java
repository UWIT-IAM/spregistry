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

package edu.washington.iam.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

import java.net.URL;
import java.net.MalformedURLException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.io.FileInputStream;
import java.io.IOException;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;


import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;


/**
 * Build a connection manager from PEM ca, cert, and key files
 */

public class IamConnectionManager {

   private String caCertificateFile;
   private String certificateFile;
   private String keyFile;

   private SSLSocketFactory socketFactory;
   private TrustManager[] trustManagers;
   private KeyManager[] keyManagers;
   private KeyStore keyStore;
   private KeyStore trustStore;
   private SchemeRegistry schemeRegistry;
   private ThreadSafeClientConnManager connectionManager;

   private static Logger log = LoggerFactory.getLogger(IamConnectionManager.class);
   
   public IamConnectionManager(String cafile, String certfile, String keyfile) {
      log.debug("create connection manager");
      caCertificateFile = cafile; 
      certificateFile = certfile; 
      keyFile = keyfile; 
      String protocol = "https";
      int port = 443;

      initManagers();

      try {
         SSLContext ctx = SSLContext.getInstance("TLS");
         ctx.init(keyManagers, trustManagers, null);
         socketFactory = new SSLSocketFactory(ctx);
         // this needed because we address idps by hostname
         socketFactory.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
         Scheme scheme = new Scheme(protocol, socketFactory, port);
         schemeRegistry = new SchemeRegistry();
         schemeRegistry.register(scheme);

         log.debug("create conn mgr");
         connectionManager = new ThreadSafeClientConnManager(new BasicHttpParams(),schemeRegistry);

      } catch (Exception e) {
         log.error("sf error: " + e);
      }
   }

   
   public SSLSocketFactory getSocketFactory() {
       log.debug("sr get sock factory");
       return socketFactory;
   }
   public SchemeRegistry getSchemeRegistry() {
       log.debug("sr get scheme reg");
       return schemeRegistry;
   }
   public ClientConnectionManager getConnectionManager() {
       return (ClientConnectionManager) connectionManager;
   }
      
   protected void __initSocketFactory() {
       log.debug("sr sock factory init");

       TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
           public X509Certificate[] getAcceptedIssuers() {
               return null;
           }

           public void checkClientTrusted(X509Certificate[] certs, String authType) {
               return;
           }

           public void checkServerTrusted(X509Certificate[] certs, String authType) {
            log.debug("Trusting server cert");
               return;
           }
       }};

       try {
           SSLContext sc = SSLContext.getInstance("SSL");
           // sc.init(keyManagers, trustManagers, new java.security.SecureRandom());
           sc.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
           // socketFactory = sc.getSocketFactory();
       } catch (Exception e) {
           log.error("mango initSF error: " + e);
       }
   }

   
   protected void initManagers() {

        try {
           /* trust managers */
           if (caCertificateFile != null) {
              KeyStore trustStore;
              int cn = 0;

              // at this point the logger isn't usually running yet
              log.info("Setting x509 trust from " + caCertificateFile);
              System.out.println("Setting x509 trust from " + caCertificateFile);

              TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
              CertificateFactory cf = CertificateFactory.getInstance("X.509");
              FileInputStream in = new FileInputStream(caCertificateFile);
              Collection certs = cf.generateCertificates(in);

              trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
              trustStore.load(null, null);

              Iterator cit = certs.iterator();
              while (cit.hasNext()) {
                 X509Certificate cert = (X509Certificate) cit.next();
                 log.info(" adding " + cert.getSubjectX500Principal().toString());
                 System.out.println(" adding " + cert.getSubjectX500Principal().toString());
                 trustStore.setCertificateEntry("CACERT" + cn, cert);
                 cn += 1;
              }
              tmf.init(trustStore);
              trustManagers = tmf.getTrustManagers();
           } else {  // no verification
              trustManagers = new TrustManager[] { new X509TrustManager() {
                 public X509Certificate[] getAcceptedIssuers() {
                     return null;
                 }

                 public void checkClientTrusted(X509Certificate[] certs, String authType) {
                     return;
                 }

                 public void checkServerTrusted(X509Certificate[] certs, String authType) {
                     return;
                 }
             }};
           }

           /* key manager */
           if (certificateFile != null && keyFile != null) {
               KeyStore keyStore;
               KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
               keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
               keyStore.load(null, null);

               FileInputStream in = new FileInputStream(certificateFile);
               CertificateFactory cf = CertificateFactory.getInstance("X.509");
               X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
               PKCS1 pkcs = new PKCS1();
               log.info("reading key file: " + keyFile);
               PrivateKey key = pkcs.readKey(keyFile);

               X509Certificate[] chain = new X509Certificate[1];
               chain[0] = cert;
               keyStore.setKeyEntry("CERT", (Key) key, "pw".toCharArray(), chain);
               kmf.init(keyStore, "pw".toCharArray());
               keyManagers = kmf.getKeyManagers();
           }
        } catch (IOException e) {
           log.error("error reading cert or key error: " + e);
        } catch (KeyStoreException e) {
           log.error("keystore error: " + e);
        } catch (NoSuchAlgorithmException e) {
           log.error("sf error: " + e);
        } catch (CertificateException e) {
           log.error("sf error: " + e);
        } catch (UnrecoverableKeyException e) {
           log.error("sf error: " + e);
        }

   }

   
}
