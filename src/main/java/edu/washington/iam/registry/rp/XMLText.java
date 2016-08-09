package edu.washington.iam.registry.rp;

public final class XMLText {

    public static String xmlStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<EntitiesDescriptor Name=\"urn:washington.edu:rpedit\"\n" +
      "    xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n" +
      "    xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
      "    xmlns:shibmd=\"urn:mace:shibboleth:metadata:1.0\"\n" +
      "    xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"\n" +
      "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
    public static String xmlEnd = "</EntitiesDescriptor>";
    public static String xmlNotice = "\n  <!-- DO NOT EDIT: This is a binary, created by sp-registry -->\n\n";

}
