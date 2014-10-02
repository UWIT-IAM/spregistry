package edu.washington.iam.registry.rp;

import edu.washington.iam.registry.exception.RelyingPartyException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

    private List<String> fakeEntityIds = Arrays.asList("testsp1", "testsp2", "testsp3", "testsp4",
            "https://dbmetadatatest.s.uw.edu/shibboleth",
            "https://searchdbmetadatatest.s.uw.edu",
            "https://searchdbmetadatatest2.s.uw.edu",
            "https://searchdbmetadatatest3.s.uw.edu");

    @Before
    public void setupInitialData() {
        setupWithRPs(fakeEntityIds);
    }

    @After
    public void teardown(){
        teardownRPs(fakeEntityIds);
    }

    @Test
    public void testGetRelyingPartyIds() throws Exception {
        List<String> ids = dao.getRelyingPartyIds();
        Assert.assertTrue(ids.size() > 2);
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

    @Test
    public void testAddSelectRelyingParties(){
        List<RelyingParty> relyingParties = dao.addSelectRelyingParties("searchdbmetadatatest");
        Assert.assertEquals(3, relyingParties.size());
    }

    @Test
    public void testAddSelectRelyingPartiesNoDeleted(){
        template.update("update metadata set status = 0 where entity_id = ?",
                new Object[] {"https://searchdbmetadatatest2.s.uw.edu"});
        List<RelyingParty> relyingParties = dao.addSelectRelyingParties("searchdbmetadatatest");
        Assert.assertEquals(2, relyingParties.size());
    }

    private void setupWithRPs(List<String> entityIds){
        String groupId = "uwrp";
        for (String entityId : entityIds) {

            template.update("insert into metadata (group_id, entity_id, xml, status, update_time) " +
                    "values (?, ?, ?, 1, now())",
                    new Object[]{groupId, entityId, fakeRelyingPartyXml(entityId)});
        }
    }

    private void teardownRPs(List<String> entityIds){
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
        namedTemplate.update("delete from metadata where entity_id in (:ids)",
                new MapSqlParameterSource().addValue("ids", entityIds));
    }

    private String fakeRelyingPartyXml(String entityId){
        return "<EntityDescriptor entityID=\"{entityId}\">\n" +
                "  <SPSSODescriptor  protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol urn:oasis:names:tc:SAML:1.0:protocol\">\n" +
                // no keydescriptor in the dao test, test elsewhere
                // "   <KeyDescriptor>...</KeyDescriptor>\n" +*/
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
                "   <GivenName>J F</GivenName>\n" +
                "   <EmailAddress>jf@example.com</EmailAddress>\n" +
                "  </ContactPerson>\n" +
                "  <ContactPerson contactType=\"administrative\">\n" +
                "   <GivenName>J F</GivenName>\n" +
                "   <EmailAddress>jf@example.com</EmailAddress>\n" +
                "  </ContactPerson>\n" +
                "  <ContactPerson contactType=\"support\">\n" +
                "   <GivenName>Super Bob</GivenName>\n" +
                "   <EmailAddress>bob@spud.edu</EmailAddress>\n" +
                "  </ContactPerson>\n" +
                " </EntityDescriptor>"
                        .replace("{entityId}", entityId);
    }
}
