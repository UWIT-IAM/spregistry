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

import com.sun.mail.smtp.SMTPSenderFailedException;
import java.util.List;
import java.util.Vector;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;

// local interface to java mail sender

public class IamMailSender {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private JavaMailSender mailSender;

  public void setMailSender(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  private String replyTo = "iam-support@uw.edu";

  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  private String from = "SP Registry <iam-support@uw.edu>";

  public void setFrom(String from) {
    this.from = from;
  }

  // create a standard message with the headers
  private MimeMessage genMimeMessage(IamMailMessage msg) {
    MimeMessage mime = mailSender.createMimeMessage();
    try {
      mime.setRecipients(RecipientType.TO, InternetAddress.parse(msg.getTo()));
      mime.setSubject(msg.makeSubstitutions(msg.getSubject()));
      mime.setReplyTo(InternetAddress.parse(replyTo));
      mime.setFrom(new InternetAddress(msg.getFrom()));
      mime.addHeader("X-Auto-Response-Suppress", "NDR, OOF, AutoReply");
      mime.addHeader("Precedence", "Special-Delivery, never-bounce");
      mime.setText(msg.makeSubstitutions(msg.getText()));
    } catch (MessagingException e) {
      log.error("iam mail build fails: " + e);
    }
    return mime;
  }

  // send mail
  public void send(IamMailMessage msg) {
    MimeMessage mime = genMimeMessage(msg);
    mailSender.send(mime);
  }

  // send mail with owner cc
  public void sendWithOwnerCc(IamMailMessage msg, DNSVerifier verifier, List<String> cns) {

    MimeMessage mime = genMimeMessage(msg);
    try {
      List<String> owners = new Vector();
      for (int i = 0; i < cns.size(); i++) verifier.isOwner(cns.get(i), null, owners);
      Address[] oAddrs = new Address[owners.size()];
      for (int i = 0; i < owners.size(); i++) {
        oAddrs[i] = new InternetAddress(owners.get(i) + "@uw.edu");
        // log.debug(" cc to: " + owners.get(i));
      }
      mime.setRecipients(RecipientType.CC, oAddrs);
      mailSender.send(mime);
    } catch (DNSVerifyException ex) {
      log.error("checking dns: " + ex.getMessage());
    } catch (SMTPSenderFailedException e) {
      log.error("cannot send email: " + e);
    } catch (MessagingException e) {
      log.error("iam mail failure: " + e);
    }
  }
}
