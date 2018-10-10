/* ========================================================================
 * Copyright (c) 2009 The University of Washington
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

import java.io.BufferedWriter;
import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.Arrays;

import edu.washington.iam.tools.XMLSerializable;

import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import edu.washington.iam.tools.XMLHelper;

import edu.washington.iam.registry.exception.RelyingPartyException;

import org.javers.core.*;
import org.javers.core.diff.Diff;
import org.javers.core.metamodel.object.*;

import edu.washington.iam.registry.rp.HistoryItem;
import edu.washington.iam.registry.rp.HistoryItem.*;

import javax.xml.bind.ValidationEvent;
import java.util.Optional;

import static org.javers.core.diff.ListCompareAlgorithm.LEVENSHTEIN_DISTANCE;

//decorator for javers compare functions
@TypeName("RelyingParty")
public class RelyingParty implements XMLSerializable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String uuid;
    @Id //decorator for javers
    private String entityId;
    private String startTime;
    private String endTime;
    private String updatedBy;
    private String metadataId;
    private boolean editable;
    private String protocolSupportEnumerationsUnsplit;
    private List<String> protocolSupportEnumerations;
    // List<Extension> extensions;
    private List<KeyDescriptor> keyDescriptors;
    private List<String> nameIDFormats;
    private List<AssertionConsumerService> assertionConsumerServices;
    private Organization organization;
    private List<ContactPerson> contactPersons;

    private String authnRequestsSigned;
    private List<ManageNameIDService> manageNameIDServices;

    private String entityCategory;

    // initialize
    private void localInit () {
       metadataId = "";

       updatedBy = "";
       startTime = "";
       endTime = "";
       uuid = "";
       editable = false;
       // extensions = new Vector();
       keyDescriptors = new Vector();
       nameIDFormats = new Vector();
       assertionConsumerServices = new Vector();
       organization = null;
       contactPersons = new Vector();
       manageNameIDServices = new Vector();
    }

    // create from document element
//    public RelyingParty (Element ele, Metadata md) throws RelyingPartyException {
//       this(ele, md.getId(), md.isEditable());
//    }

    public RelyingParty (Element ele, String mdid, boolean edit) throws RelyingPartyException {

        this(ele, mdid, edit, "", "", "", "");

    }

    // create from document element

    public RelyingParty (Element ele, String mdid, boolean edit, String updatedBy, String startTime, String endTime,
                         String uuid)
            throws RelyingPartyException {

       localInit();
       this.entityId = ele.getAttribute("entityID");
       if (entityId==null) throw new RelyingPartyException("No entity id attribute");
       // log.debug("create from doc: " + entityId);

       this.metadataId = mdid;
       this.editable = edit;
       this.updatedBy = updatedBy;
       this.startTime = startTime;
       this.endTime = endTime;
       this.uuid = uuid;

       NodeList nl1 = ele.getChildNodes();
       for (int i=0; i<nl1.getLength(); i++) {
           if (nl1.item(i).getNodeType()!=Node.ELEMENT_NODE) continue;
           Element e1 = (Element)nl1.item(i);
           String name = e1.getNodeName();
           // log.debug("rp ele: " + name);

           if (XMLHelper.matches(name,"SPSSODescriptor")) {
              authnRequestsSigned = ele.getAttribute("AuthnRequestsSigned");
              protocolSupportEnumerationsUnsplit = e1.getAttribute("protocolSupportEnumeration");
              protocolSupportEnumerations = Arrays.asList(protocolSupportEnumerationsUnsplit.split(" "));
       /***
              for (int j=0; j<protocolSupportEnumerations.size(); j++) {
                 String psev = protocolSupportEnumerations.get(j);
                 if (!(psev.equals("urn:mace:shibboleth:1.0") ||
                       psev.equals("urn:oasis:names:tc:SAML:1.0:protocol") ||
                       psev.equals("urn:oasis:names:tc:SAML:1.1:protocol") ||
                       psev.equals("urn:oasis:names:tc:SAML:2.0:protocol"))) throw new RelyingPartyException("bad protocol " + psev);
              }
       ***/

              NodeList nl2 = e1.getChildNodes();
              for (int j=0; j<nl2.getLength(); j++) {
                 if (nl2.item(j).getNodeType()!=Node.ELEMENT_NODE) continue;
                 Element e2 = (Element)nl2.item(j);
                 String name2 = e2.getNodeName();
                 // log.info("sso ele: " + name2);
       //        if (XMLHelper.matches(name2,"Extension")) extensions.add(new Extension(e2));
                 // ignore 'duplicate' keyinfo  (ignores 'use')
                 if (XMLHelper.matches(name2,"KeyDescriptor")) {
                    KeyDescriptor kd = new KeyDescriptor(e2);
                    boolean notdup = true;
                    for (int k=0; k<keyDescriptors.size(); k++) {
                       if (kd.isDuplicate(keyDescriptors.get(k))) notdup = false;
                    }
                    if (notdup) keyDescriptors.add(new KeyDescriptor(e2));
                 }
                 if (XMLHelper.matches(name2,"NameIDFormat")) nameIDFormats.add(e2.getTextContent());
                 if (XMLHelper.matches(name2,"AssertionConsumerService")) assertionConsumerServices.add(new AssertionConsumerService(e2));
                 if (XMLHelper.matches(name2,"ManageNameIDService")) manageNameIDServices.add(new ManageNameIDService(e2));
              }
           }
           if (XMLHelper.matches(name,"Organization")) organization = new Organization(e1);
           if (XMLHelper.matches(name,"ContactPerson")) contactPersons.add(new ContactPerson(e1));

           // we're just looking for the category
         try {
           if (XMLHelper.matches(name,"Extensions")) {
              NodeList nl2 = e1.getChildNodes();
              for (int j=0; j<nl2.getLength(); j++) {
                 if (nl2.item(j).getNodeType()!=Node.ELEMENT_NODE) continue;
                 Element e2 = (Element)nl2.item(j);
                 String name2 = e2.getNodeName();
                 // log.debug("ext name2: " + name2);
                 if (XMLHelper.matches(name2,"EntityAttributes")) {
                    NodeList nl3 = e2.getChildNodes();
                    for (int k=0; k<nl3.getLength(); k++) {
                       if (nl3.item(k).getNodeType()!=Node.ELEMENT_NODE) continue;
                       Element e3 = (Element)nl3.item(k);
                       String name3 = e3.getNodeName();
                       // log.debug("ext name3: " + name3);
                       if (XMLHelper.matches(name3,"Attribute")) {
                          String aname = e3.getAttribute("Name");
                          if (!aname.equals("http://macedir.org/entity-category")) continue;
                          NodeList nl4 = e3.getChildNodes();
                          for (int l=0; l<nl4.getLength(); l++) {
                             if (nl4.item(l).getNodeType()!=Node.ELEMENT_NODE) continue;
                             Element e4 = (Element)nl4.item(k);
                             String name4 = e4.getNodeName();
                             // log.debug("ext name4: " + name4);
                             if (!XMLHelper.matches(name4,"AttributeValue")) continue;
                             entityCategory = e4.getTextContent();
                             // log.debug("cat: " + entityCategory);
                          }
                       }
                    }
                 }
              }
           }
         } catch (NullPointerException e) {
            entityCategory = null;
         }
       }
    }

    // create default rp
    public RelyingParty (String id, String dns) {
       localInit();
       entityId = id;
       editable = true;

       protocolSupportEnumerationsUnsplit = "urn:oasis:names:tc:SAML:2.0:protocol";
       protocolSupportEnumerations = Arrays.asList(protocolSupportEnumerationsUnsplit.split(" "));

       keyDescriptors.add(new KeyDescriptor(dns));
       nameIDFormats.add("urn:mace:shibboleth:1.0:nameIdentifier");

       assertionConsumerServices.add(new AssertionConsumerService(1,
           "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST", "https://" + dns + "/Shibboleth.sso/SAML2/POST"));
       organization = new Organization("", "", "");
       contactPersons.add(new ContactPerson("administrative"));
    }

    @Override
    public void writeXml(BufferedWriter xout) throws IOException {
        xout.write(" <EntityDescriptor entityID=\"" + XMLHelper.safeXml(entityId) + "\">\n");
        String ars = "";
        if (authnRequestsSigned.length()>0) ars = " AuthnRequestsSigned=\"" + XMLHelper.safeXml(authnRequestsSigned) + "\"";
        xout.write("  <SPSSODescriptor " 
                   + ars 
                   + " protocolSupportEnumeration=\"" + XMLHelper.safeXml(protocolSupportEnumerationsUnsplit) + "\">\n");

       for (int i=0; i<keyDescriptors.size(); i++) {
          keyDescriptors.get(i).writeXml(xout);
       }
 
       for (int i=0; i<nameIDFormats.size(); i++) {
           xout.write("   <NameIDFormat>" + XMLHelper.safeXml(nameIDFormats.get(i)) + "</NameIDFormat>\n");
       }

/***  don't know if this goes before or after the nameidformats
       for (int i=0; i<manageNameIDServices.size(); i++) {
          manageNameIDServices.get(i).writeXml(xout);
       }
 ***/
       for (int i=0; i<assertionConsumerServices.size(); i++) {
          assertionConsumerServices.get(i).writeXml(xout);
       }
       xout.write("  </SPSSODescriptor>\n");

       if (organization!=null) organization.writeXml(xout);
       else log.info("no org for " + entityId);
       for (int i=0; i<contactPersons.size(); i++) {
          contactPersons.get(i).writeXml(xout);
       }
       xout.write(" </EntityDescriptor>\n");
    }

    public HistoryItem RpCompare(RelyingParty obj){

        HistoryItem historyItems;

        Javers javers = JaversBuilder.javers()
                .withListCompareAlgorithm(LEVENSHTEIN_DISTANCE)
                .build();

        //take a diff
        Diff diff = javers.compare(this, obj);
       String foo = javers.getJsonConverter().toJson(diff).toString();
        //get the date
        ValueChange effectiveDate = (ValueChange)diff.getPropertyChanges("startTime").get(0);
        //create new history item using date
        historyItems = new HistoryItem(effectiveDate.getRight().toString(), obj.getUpdatedBy());
        //now iterate over all changes and put into history item (ignore start and end times now)
        try {
            //select out ValueChange objects only
            int objDupIndex = -1;  //prevent duplicates
            Object objDupType = null;  //prevent duplicates
            List<ValueChange> myValueChanges = diff.getChangesByType(ValueChange.class);
            for (ValueChange change : myValueChanges) {
                //changed value
                Object obj1 = change.getAffectedObject();  //returns changed object
                //we don't care about these fields
                if (change.getPropertyName().equalsIgnoreCase("startTime") ||
                        change.getPropertyName().equalsIgnoreCase("endTime") ||
                        change.getPropertyName().equalsIgnoreCase("uuid") ||
                        change.getPropertyName().equalsIgnoreCase("updatedBy")) continue;
                //if object type is RelyingParty then this change is a single valued field, not a list of fields of a different object type (e.g. contactPersons).
                if (obj1 instanceof RelyingParty) {
                    String propertyName = change.getPropertyName().toString();
                    String left = change.getLeft().toString();
                    String right = change.getRight().toString();
                    historyItems.AddChangeItem(propertyName, left, right);

                } else {  //else should catch any "object" type fields of RelyingParty
                    String propertyName = change.getPropertyName().toString();
                    //string containing index of affected object
                    GlobalId globalId = change.getAffectedGlobalId();
                    ValueObjectId valueId = (ValueObjectId) globalId;
                    String[] idList = valueId.getFragment().split("/");
                    int objIndex = Integer.parseInt(idList[1]);
                    //since we grab the entire object when we detect one change,
                    //don't bother tracking additional changes
                    if (objIndex == objDupIndex && obj1.equals(objDupType)) {
                        continue;
                    }
                    objDupIndex = objIndex;  //keep track of this instance index
                    objDupType = obj1;  //keep track of this object so we don't duplicate it
                    //easy to get the change TO value from the change object
                    Object right = change.getAffectedObject().get();
                    //some nutty reflection to get the original value
                    Class leftCls = this.getClass();
                    Field leftField = leftCls.getDeclaredField(idList[0]);
                    Object left = ((Vector) leftField.get(this)).get(objIndex);

                    //idList[0] is the name of the property in RelyingParty Object
                    //note the object type name from left and right are different from idlist[0].  The latter is the name of
                    //the list of the former objects in RelyingParty Object.
                    historyItems.AddChangeItem(idList[0], left, right);


                }
            }
            List<NewObject> myNewObjects = diff.getChangesByType(NewObject.class);
            for (Change change : myNewObjects) {
                Object obj2 = change.getAffectedObject().get();
                String[] classNameSplit = obj2.getClass().toString().split("\\.");
                String className = classNameSplit[classNameSplit.length - 1];
                if (className.equalsIgnoreCase("startTime") ||
                        className.equalsIgnoreCase("endTime") ||
                        className.equalsIgnoreCase("uuid") ||
                        className.equalsIgnoreCase("loggerRemoteView") ||
                        className.equalsIgnoreCase("logger") ||
                        className.equalsIgnoreCase("updatedBy")) continue;
                //if object type is RelyingParty then this change is a single valued field, not a list of fields of a different object type (e.g. contactPersons).
                if (obj2 instanceof RelyingParty) {
                    historyItems.AddNewItem(className, obj2);
                } else {  //else should catch any "object" type fields of RelyingParty
                    String propertyName = className;
                    //string containing index of affected object
                    GlobalId globalId = change.getAffectedGlobalId();
                    ValueObjectId valueId = (ValueObjectId) globalId;
                    String[] idList = valueId.getFragment().split("/");
                    int objIndex = Integer.parseInt(idList[1]);
                    //since we grab the entire object when we detect one change,
                    //don't bother tracking additional changes
                    if (objIndex == objDupIndex && obj2.equals(objDupType)) {
                        continue;
                    }
                    objDupIndex = objIndex;  //keep track of this instance index
                    objDupType = obj2;  //keep track of this object so we don't duplicate it
                    //idList[0] is the name of the property in RelyingParty Object
                    //note the object type (classname above) is different from idlist[0].  The latter is the name of
                    //the list of the former objects in RelyingParty Object.
                    historyItems.AddNewItem(idList[0], obj2);


                }

            }
            List<ObjectRemoved> myRemovedObjects = diff.getChangesByType(ObjectRemoved.class);
            for (Change change : myRemovedObjects) {
                Object obj3 = change.getAffectedObject().get();
                String[] classNameSplit = obj3.getClass().toString().split("\\.");
                String className = classNameSplit[classNameSplit.length - 1];
                if (className.equalsIgnoreCase("startTime") ||
                        className.equalsIgnoreCase("endTime") ||
                        className.equalsIgnoreCase("uuid") ||
                        className.equalsIgnoreCase("loggerRemoteView") ||
                        className.equalsIgnoreCase("logger") ||
                        className.equalsIgnoreCase("updatedBy")) continue;
                //if object type is RelyingParty then this change is a single valued field, not a list of fields of a different object type (e.g. contactPersons).
                if (obj3 instanceof RelyingParty) {
                    historyItems.AddDeleteItem(className, change.toString()); //will that work?
                } else {  //else should catch any "object" type fields of RelyingParty
                    String propertyName = className;
                    //string containing index of affected object
                    GlobalId globalId = change.getAffectedGlobalId();
                    ValueObjectId valueId = (ValueObjectId) globalId;
                    String[] idList = valueId.getFragment().split("/");
                    int objIndex = Integer.parseInt(idList[1]);
                    //since we grab the entire object when we detect one change,
                    //don't bother tracking additional changes
                    if (objIndex == objDupIndex && obj3.equals(objDupType)) {
                        continue;
                    }
                    objDupIndex = objIndex;  //keep track of this instance index
                    objDupType = obj3;  //keep track of this object so we don't duplicate it
                    //some nutty reflection to get the original value
                    Class leftCls = this.getClass();
                    Field leftField = leftCls.getDeclaredField(idList[0]);
                    Object left = ((Vector) leftField.get(this)).get(objIndex);
                    //idList[0] is the name of the property in RelyingParty Object
                    //note the object type (classname above) is different from idlist[0].  The latter is the name of
                    //the list of the former objects in RelyingParty Object.
                    historyItems.AddDeleteItem(idList[0], left);
                }

            }
        }
        catch (Exception e) {
            Exception ee = e;
        }

        return historyItems;
    }

    public RelyingParty replicate(String dns) {
         return null;
    }

    public void setEntityId(String v) {
       entityId = v;
    }
    public String getEntityId() {
       return (entityId);
    }

    public void setMetadataId(String v) {
       metadataId = v;
    }
    public String getMetadataId() {
       return (metadataId);
    }

    public void setStartTime(String v) {
        startTime = v;
    }
    public String getStartTime() {
        return (startTime);
    }
    public void setEndTime(String v) {
        endTime = v;
    }
    public String getEndTime() {
        return (endTime);
    }
    public void setUuid(String v) {
        uuid = v;
    }
    public String getUuid() {
        return (uuid);
    }
    public void setUpdatedBy(String v) {
        updatedBy = v;
    }
    public String getUpdatedBy() {
        return (updatedBy);
    }


    public void setEditable(boolean v) {
       editable = v;
    }
    public boolean getEditable() {
       return (editable);
    }

    public List<String> getProtocolSupportEnumerations() {
       return (protocolSupportEnumerations);
    }
    public void setKeyDescriptors(List<KeyDescriptor> v) {
       keyDescriptors = v;
    }
    public List<KeyDescriptor> getKeyDescriptors() {
       return (keyDescriptors);
    }

    public void setNameIDFormats(List<String> v) {
       nameIDFormats = v;
    }
    public List<String> getNameIDFormats() {
       return (nameIDFormats);
    }

    public void setAssertionConsumerServices(List<AssertionConsumerService> v) {
       assertionConsumerServices = v;
    }
    public List<AssertionConsumerService> getAssertionConsumerServices() {
       return (assertionConsumerServices);
    }
    public String getAuthnRequestsSigned(){
        return authnRequestsSigned;
    }
    public void setAuthnRequestsSigned(String v){
        authnRequestsSigned = v;
    }

    public void setOrganization(Organization v) {
       organization = v;
    }
    public Organization getOrganization() {
       return organization;
    }

    public void setContactPersons(List<ContactPerson> v) {
       contactPersons = v;
    }
    public List<ContactPerson> getContactPersons() {
       return (contactPersons);
    }

    public String getEntityCategory() {
        return (entityCategory);
    }

}

