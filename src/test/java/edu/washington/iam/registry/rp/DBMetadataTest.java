package edu.washington.iam.registry.rp;

import edu.washington.iam.registry.exception.RelyingPartyException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-db-context.xml")
public class DBMetadataTest {
    @Autowired
    private DBMetadata dao;
    @Autowired
    private JdbcTemplate template;

    @Before
    public void setupInitialData() {
        // TODO: Consider a better way than inserting and deleting for every test
        String groupId = "uwrp";
        List<String> fakeEntityIds = Arrays.asList("testsp1", "testsp2", "testsp3", "testsp4",
                "https://dbmetadatatest.s.uw.edu/shibboleth");
        for (String fakeId : fakeEntityIds) {
            template.update("insert into metadata (group_id, entity_id, xml, status, update_time) " +
                    "values (?, ?, ?, 1, now())",
                    new Object[]{groupId, fakeId, fakeRelyingPartyXml(fakeId)});
        }
    }

    @After
    public void teardown(){
        // TODO: never run this against an actual DB
        template.update("delete from metadata");
    }

    @Test
    public void testGetRelyingPartyIds() throws Exception {
        List<String> ids = dao.getRelyingPartyIds();
        Assert.assertTrue(ids.size() > 1);
    }

    @Test
    public void testGetRelyingPartyById() throws Exception {
        RelyingParty relyingParty = dao.getRelyingPartyById("https://dbmetadatatest.s.uw.edu/shibboleth");
        Assert.assertNotNull(relyingParty);
    }

    @Test
    public void testGetRelyingPartyByIdNoParty() throws Exception {
        boolean caughtException = false;
        try {
            RelyingParty relyingParty = dao.getRelyingPartyById("doublefoobar");
        }
        catch (RelyingPartyException e){
            caughtException = true;
        }
        Assert.assertTrue(caughtException);
    }

    @Test
    public void testGetRelyingPartyByIdBigTest() throws Exception {
        boolean atLeastOnce = false;
        for(String rpId : dao.getRelyingPartyIds()){
            atLeastOnce = true;
            RelyingParty relyingParty = dao.getRelyingPartyById(rpId);
            Assert.assertNotNull(relyingParty);
        }
        Assert.assertTrue(atLeastOnce);
    }

    private String fakeRelyingPartyXml(String entityId){
        // TODO: figure if this is the place to test KeyDescriptor, whose cert needs to match entityId
        return "<EntityDescriptor entityID=\"{entityId}\">\n" +
                "  <SPSSODescriptor  protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol urn:oasis:names:tc:SAML:1.0:protocol\">\n" +
                /*"   <KeyDescriptor>\n" +
                "    <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "     <ds:KeyName>urizen4.cac.washington.edu</ds:KeyName>\n" +
                "     <ds:X509Data><ds:X509Certificate>MIIDFTCCAf2gAwIBAgIJAO8YvxX3KWN8MA0GCSqGSIb3DQEBBQUAMCUxIzAhBgNV\n" +
                "BAMTGnVyaXplbjQuY2FjLndhc2hpbmd0b24uZWR1MB4XDTExMDUwOTE4MzcyNFoX\n" +
                "DTIxMDUwNjE4MzcyNFowJTEjMCEGA1UEAxMadXJpemVuNC5jYWMud2FzaGluZ3Rv\n" +
                "bi5lZHUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCkxqVvraqnCTtK\n" +
                "W/cubeko2YsETD4sAvOh+049PmgD08eiJQdw+9z9bdI0phTl1xIshBHKHEAvMoZl\n" +
                "U9bZKrbSTiREI1Q+OQ137FQ1TShH29bHdS/Ib7Uh23HlLFNaS+Jr6Fk4Fbuhw5qb\n" +
                "ZKKb7hCtZYh0jjheAdV1E9BNics+te7K8zDAAV8ZAEW/NNi0xEKp040o208rpXmP\n" +
                "JR578Dyu3QQagRCIwIQtvf1xlsdEWaJ+uGB26n5S2fDqW2dmtTVhwp+MgIBxhJ4D\n" +
                "WRe0+m+DKFLkrtF2DEBnQ9yuC4kb3xzIl73kAfxtq4V7DxZ8L9wLYQJ8rGl8Vj4z\n" +
                "bwIca2v3AgMBAAGjSDBGMCUGA1UdEQQeMByCGnVyaXplbjQuY2FjLndhc2hpbmd0\n" +
                "b24uZWR1MB0GA1UdDgQWBBTm7AR5cODCkdjGuptICtDDh9+sPTANBgkqhkiG9w0B\n" +
                "AQUFAAOCAQEAb32LB2BLqY97jC5bRx2IxXDS29oYupOWbXmahot9OevldUvimRIr\n" +
                "6jJkeiT75YCRpdpax1D6tYbbjoZBz+/vyOQ5jicqPqfWuFPrqejfThNyhaJnHL6B\n" +
                "UhCBHXE4gxvThdEy0jHbljXRtw+pcjD735ECmfainUg2etswfRIgQioC+YWu0k5I\n" +
                "gj+99MoIEmQsEnFQOxAZDLOqreCOAeSN2mzGTlP3iUnREDXp+2QdKTNRkJnChnCL\n" +
                "GimnXQ1vIb+hBlnaALukHNMOTqg3n9RRUItzowVi9y1K9frRuFou3E0A70hD1xCm\n" +
                "jpYNeB/Tb9Ku37L5sA3ticoaL4KfpScpMw==</ds:X509Certificate></ds:X509Data>\n" +
                "    </ds:KeyInfo>\n" +
                "   </KeyDescriptor>\n" +*/
                "   <AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://urizen4.cac.washington.edu/Shibboleth.sso/SAML2/POST\" index=\"0\"/>\n" +
                "   <AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign\" Location=\"https://urizen4.cac.washington.edu/Shibboleth.sso/SAML2/POST-SimpleSign\" index=\"1\"/>\n" +
                "   <AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:PAOS\" Location=\"https://urizen4.cac.washington.edu/Shibboleth.sso/SAML2/ECP\" index=\"3\"/>\n" +
                "   <AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://urizen4.cac.washington.edu/stepup/Shibboleth.sso/SAML2/POST\" index=\"6\"/>\n" +
                "  </SPSSODescriptor>\n" +
                "  <Organization>\n" +
                "   <OrganizationName xml:lang=\"en\">AIE-IAM urizen4 test and development</OrganizationName>\n" +
                "   <OrganizationDisplayName xml:lang=\"en\">Urizen4 est and dev system for AIE-IAM</OrganizationDisplayName>\n" +
                "   <OrganizationURL xml:lang=\"en\">https://urizen4.cac.washington.edu/</OrganizationURL>\n" +
                "  </Organization>\n" +
                "  <ContactPerson contactType=\"technical\">\n" +
                "   <GivenName>Jim Fox</GivenName>\n" +
                "   <EmailAddress>fox@uw.edu</EmailAddress>\n" +
                "  </ContactPerson>\n" +
                "  <ContactPerson contactType=\"administrative\">\n" +
                "   <GivenName>Jim Fox</GivenName>\n" +
                "   <EmailAddress>fox@washington.edu</EmailAddress>\n" +
                "  </ContactPerson>\n" +
                "  <ContactPerson contactType=\"support\">\n" +
                "   <GivenName>Super Bob</GivenName>\n" +
                "   <EmailAddress>bob@spud.edu</EmailAddress>\n" +
                "  </ContactPerson>\n" +
                " </EntityDescriptor>"
                        .replace("{entityId}", entityId);
    }
}
