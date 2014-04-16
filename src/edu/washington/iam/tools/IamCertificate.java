/* ========================================================================
 * Copyright (c) 2013 The University of Washington
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
import java.util.Date;
import java.util.Vector;

/* Generic certificate */

public class IamCertificate {
   public int id;            // my id
   public String cn;           // CN from cert
   public String dn;           // DN from cert
   public List<String> names;  // cn (1st)  + altnames
   public Date issued;
   public Date expires;
   public String pemRequest;
   public String pemCert;
   public String remHash;
   public String dnC;  // country code
   public String dnST;  // state 
   public int keySize;
   public String issuerDn;
   public String snStr;  // sn as string

   public IamCertificate() {
      names = new Vector();
      cn = "";
      dn = "";
      expires = null;
   }
   public IamCertificate(String pem) throws IamCertificateException {
      names = new Vector();
      cn = "";
      dn = "";
      expires = null;
      pemCert = pem;
      if (!pem.startsWith("-----")) pemCert = "-----BEGIN CERTIFICATE-----\n" + pem + "\n-----END CERTIFICATE-----";
      IamCertificateHelper.parseCert(this);
   }

   public int getId() {
      return id;
   }
   public String getCn() {
      return cn;
   }
   public String getDn() {
      return dn;
   }
   public String getCleanDn() {
      if (dn!=null) return dn.replaceAll("<","").replaceAll(">","").replaceAll("&","");
      return null;
   }
  
   public String getPemCert() {
      return pemCert;
   }
   public Date getIssued() {
      return issued;
   }
   public Date getExpires() {
      return expires;
   }
   public int getKeySize() {
      return keySize;
   }
   public String getIssuerDn() {
      return issuerDn;
   }
   public String getSnStr() {
      return snStr;
   }

}

