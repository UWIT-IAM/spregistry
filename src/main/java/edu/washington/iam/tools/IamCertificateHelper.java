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

import java.io.IOException;
import java.io.StringReader;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IamCertificateHelper {

  private static final Logger log = LoggerFactory.getLogger(IamCertificateHelper.class);

  /* extract some info from the submitted CSR */

  public static int parseCsr(IamCertificate cert) throws IamCertificateException {

    try {
      PEMParser pRd = new PEMParser(new StringReader(cert.pemRequest));
      PKCS10CertificationRequest request = (PKCS10CertificationRequest) pRd.readObject();
      if (request == null) throw new IamCertificateException("invalid CSR (request)");
      CertificationRequestInfo info = request.getCertificationRequestInfo();
      if (info == null) throw new IamCertificateException("invalid CSR (info)");

      X509Name dn = X509Name.getInstance(info.getSubject());
      if (dn == null) throw new IamCertificateException("invalid CSR (dn)");
      log.debug("dn=" + dn.toString());
      cert.dn = dn.toString();
      try {
        List cns = dn.getValues(X509Name.CN);
        cert.cn = (String) (cns.get(0));
        log.debug("cn=" + cert.cn);
        cert.names.add(cert.cn); // first entry for names is always cn
        cns = dn.getValues(X509Name.C);
        cert.dnC = (String) (cns.get(0));
        cns = dn.getValues(X509Name.ST);
        cert.dnST = (String) (cns.get(0));
      } catch (Exception e) {
        log.debug("get cn error: " + e);
        throw new IamCertificateException("invalid CSR");
      }

      // see if we've got alt names (in extensions)

      ASN1Set attrs = info.getAttributes();
      if (attrs != null) {
        for (int a = 0; a < attrs.size(); a++) {
          Attribute attr = Attribute.getInstance(attrs.getObjectAt(a));
          if (attr.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {

            // is the extension
            X509Extensions extensions =
                X509Extensions.getInstance(attr.getAttrValues().getObjectAt(0));

            // get the subAltName extension
            ASN1ObjectIdentifier sanoid =
                new ASN1ObjectIdentifier(X509Extensions.SubjectAlternativeName.getId());
            X509Extension xext = extensions.getExtension(sanoid);
            if (xext != null) {
              log.debug("processing altname extensions");
              ASN1Object asn1 = X509Extension.convertValueToObject(xext);
              Enumeration dit = DERSequence.getInstance(asn1).getObjects();
              while (dit.hasMoreElements()) {
                GeneralName gn = GeneralName.getInstance(dit.nextElement());
                log.debug("altname tag=" + gn.getTagNo());
                log.debug("altname name=" + gn.getName().toString());
                if (gn.getTagNo() == GeneralName.dNSName) cert.names.add(gn.getName().toString());
              }
            }
          }
        }
      }

      // check key size
      PublicKey pk = request.getPublicKey();
      log.debug("key alg = " + pk.getAlgorithm());
      log.debug("key fmt = " + pk.getFormat());
      if (pk.getAlgorithm().equals("RSA")) {
        RSAPublicKey rpk = (RSAPublicKey) pk;
        cert.keySize = rpk.getModulus().bitLength();
        log.debug("key size = " + cert.keySize);
      }

    } catch (IOException e) {
      log.debug("ioerror: " + e);
      throw new IamCertificateException("invalid CSR " + e.getMessage());
    } catch (Exception e) {
      log.debug("excp: " + e);
      throw new IamCertificateException("invalid CSR");
    }
    return 1;
  }

  /* extract some info from the Cert */

  public static int parseCert(IamCertificate cert) throws IamCertificateException {

    try {
      // log.debug("parse certt: " + cert.pemCert);
      PEMParser pRd = new PEMParser(new StringReader(cert.pemCert));
      X509CertificateHolder holder = (X509CertificateHolder) pRd.readObject();
      X509Certificate x509 = new JcaX509CertificateConverter().getCertificate(holder);

      if (x509 == null) {
        log.info("bad cert");
        throw new IamCertificateException("invalid cert PEM");
      }
      cert.snStr = x509.getSerialNumber().toString();
      cert.issued = x509.getNotBefore();
      cert.expires = x509.getNotAfter();
      // log.debug("pem expires = " + cert.expires);

      X500Principal prin = x509.getIssuerX500Principal();
      cert.issuerDn = prin.toString();
      // log.debug("issuer = " + cert.issuerDn);

      prin = x509.getSubjectX500Principal();
      cert.dn = prin.toString();
      // log.debug("principal = " + cert.dn);

      // see if we've got alt names (in extensions)

      try {
        Collection<List<?>> ans = x509.getSubjectAlternativeNames();

        if (ans != null) {
          // log.debug("ans size = " + ans.size());
          Iterator it = ans.iterator();
          while (it.hasNext()) {
            List an = (List) it.next();
            if (an.size() == 2) {
              // log.debug("an0="+an.get(0).toString() + " an1=" + an.get(1).toString());
              if (an.get(0) instanceof Integer && an.get(1) instanceof String) {
                if ((Integer) an.get(0) == 2) cert.names.add((String) an.get(1));
              }
            }
          }
        }
        if (cert.cn.equals("") && cert.names.size() > 0) cert.cn = cert.names.get(0);
      } catch (CertificateParsingException e) {
        log.debug("parse error on alt names: " + e);
      }

      // check for expired
      /***
       * try {
       * x509.checkValidity();
       * } catch (CertificateExpiredException e) {
       * cert.status = IamCertificate.CERT_STATUS_EXPIRED;
       * } catch (CertificateNotYetValidException e) {
       * log.debug("not yet valid?");
       * }
       ***/

      // get the key size
      PublicKey pk = x509.getPublicKey();
      if (pk.getAlgorithm().equals("RSA")) {
        RSAPublicKey rpk = (RSAPublicKey) pk;
        cert.keySize = rpk.getModulus().bitLength();
        // log.debug("pub key size = " + cert.keySize);
      }

      return 1;

    } catch (IOException e) {
      log.info("ioerror: " + e);
      throw new IamCertificateException("invalid cert: ioerror");
    } catch (Exception ex) {
      log.info("excp: " + ex);
      throw new IamCertificateException("invalid cert: excep");
    }
  }
}
