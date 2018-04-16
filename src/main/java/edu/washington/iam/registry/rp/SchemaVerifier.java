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


package edu.washington.iam.registry.rp;

import java.util.List;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/* Use saml xmlsectool to test metadata validity */

public class SchemaVerifier {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String verifyCommand = null;
    public void setVerifyCommand(String v) {
        verifyCommand = v;
    }

    /**
     * Test if a RelyingParty is schema valid
     *
     * @param relying party 
     */

    public boolean testSchemaValid(RelyingParty rp) {

       boolean isValid = false;
       log.debug("testing schema validity of " + rp.getEntityId());

       try {
          File tmp = File.createTempFile("md_", ".xml", new File("/tmp"));
          tmp.deleteOnExit();
          FileWriter xstream = new FileWriter(tmp);
          BufferedWriter xout = new BufferedWriter(xstream);

          xout.write(XMLText.xmlStart);
          rp.writeXml(xout);
          xout.write(XMLText.xmlEnd);
          xout.close();

          String cmd = verifyCommand + " " + tmp.getAbsolutePath();
          Runtime rt = Runtime.getRuntime();
          Process pc = rt.exec(cmd);
          InputStreamReader isr = new InputStreamReader( pc.getInputStream() );
          BufferedReader br = new BufferedReader(isr);
          String line;
          while ((line = br.readLine()) != null) {
            log.debug("xmlsectool says: " + line);
            if (line.contains("XML document is schema valid")) return true;
          }
       } catch (IOException e) {
          log.error("xmlsectool error: " + e); 
       }
       return false;
        //return true; //for testing  on Windows

    }

}

