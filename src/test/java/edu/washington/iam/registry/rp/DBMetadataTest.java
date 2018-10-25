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

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;


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
    public void testSearchRelyingPartyIdsNullSearch() throws Exception {
        List<String> ids = dao.searchRelyingPartyIds(null);
        Assert.assertTrue(ids.size() > 5);
    }

    @Test
    public void testSearchRelyingPartyIds() throws Exception {
        List<String> ids = dao.searchRelyingPartyIds("searchdbmetadatatest");
        Assert.assertEquals(3, ids.size());
    }

    @Test
    public void testSearchRelyingPartyIdsNoDeleted() throws Exception {
        template.update("update metadata set end_time = '2001-01-01' where entity_id = ?",
                "https://searchdbmetadatatest2.s.uw.edu");
        List<String> relyingParties = dao.searchRelyingPartyIds("searchdbmetadatatest");
        Assert.assertEquals(2, relyingParties.size());
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
            dao.getRelyingPartyById("doublefoobar");
        }
        catch (RelyingPartyException e){
            caughtException = true;
        }
        Assert.assertTrue(caughtException);
    }

    @Test
    public void testGetRelyingPartyByIdBigTest() throws Exception {
        boolean atLeastOnce = false;
        for(String rpId : dao.searchRelyingPartyIds(null)){
            atLeastOnce = true;
            RelyingParty relyingParty = dao.getRelyingPartyById(rpId);
            Assert.assertNotNull(relyingParty);
        }
        Assert.assertTrue(atLeastOnce);
    }

    @Test
    public void testRemoveRelyingParty(){
        dao.removeRelyingParty(fakeEntityIds.get(0), "testuser");
        List<String> ids = dao.searchRelyingPartyIds(null);
        Assert.assertEquals(fakeEntityIds.size() - 1, ids.size());
    }

    @Test
    public void testUpdateRelyingPartyNewRP() throws Exception {
        String entityId = "https://updaterelyingpartynewrp.s.uw.edu";
        RelyingParty relyingParty = fakeRelyingParty(entityId);

        dao.updateRelyingParty(relyingParty, "testuser");

        List<String> ids = dao.searchRelyingPartyIds(null);
        Assert.assertEquals(fakeEntityIds.size() + 1, ids.size());
        Assert.assertTrue(String.format("list of entity ids contains %s", entityId), ids.contains(entityId));
    }

    @Test
    public void testUpdateRelyingPartyExistingRP() throws Exception {
        Timestamp preUpdateTime = new Timestamp(new Date().getTime());
        Thread.sleep(500);
        Assert.assertTrue(getTimestampForRP(fakeEntityIds.get(0)).before(preUpdateTime));
        int preUpdateSize = dao.searchRelyingPartyIds(null).size();

        dao.updateRelyingParty(fakeRelyingParty(fakeEntityIds.get(0)), "testuser");

        Assert.assertTrue(String.format("update time for %s has changed", fakeEntityIds.get(0)),
                getTimestampForRP(fakeEntityIds.get(0)).after(preUpdateTime));
        Assert.assertEquals(preUpdateSize, dao.searchRelyingPartyIds(null).size());
    }

    @Test
    public void testUpdateRelyingPartyDeletedRP() throws Exception {
        template.update("update metadata set end_time = '2001-01-01' where entity_id = ? ", fakeEntityIds.get(0));
        Timestamp preUpdateTime = new Timestamp(new Date().getTime());
        Thread.sleep(500);
        int preUpdateSize = dao.searchRelyingPartyIds(null).size();

        dao.updateRelyingParty(fakeRelyingParty(fakeEntityIds.get(0)), "testuser");

        Assert.assertTrue(String.format("update time for %s has changed", fakeEntityIds.get(0)),
                getTimestampForRP(fakeEntityIds.get(0)).after(preUpdateTime));
        Assert.assertEquals(preUpdateSize + 1, dao.searchRelyingPartyIds(null).size());
    }

    private Timestamp getTimestampForRP(String entityId){
        return template.queryForObject("select start_time from metadata where entity_id = ? and end_time is null",
                Timestamp.class,
                entityId);
    }

    private UUID genUUID() { return UUID.randomUUID(); }

    private void setupWithRPs(List<String> entityIds){
        String groupId = "uwrp";
        for (String entityId : entityIds) {

            template.update("insert into metadata (uuid, group_id, entity_id, xml, end_time, start_time) " +
                    "values (?, ?, ?, ?, ?, now())",
                     genUUID(), groupId, entityId, fakeRelyingPartyXml(entityId), null);
        }
    }

    private void teardownRPs(List<String> entityIds){
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
        namedTemplate.update("delete from metadata where entity_id in (:ids)",
                new MapSqlParameterSource().addValue("ids", entityIds));
    }

    private RelyingParty fakeRelyingParty(String entityId) throws Exception {
        RelyingParty relyingParty;
        String relyingPartyXml = fakeRelyingPartyXml(entityId);

        relyingParty = new RelyingParty(
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(relyingPartyXml.getBytes()))
                        .getDocumentElement(),
                "uwrp", true, "mattjm", "2001-01-01", null, genUUID());

        return relyingParty;
    }

    private String fakeRelyingPartyXml(String entityId){
        String xml = ("<EntityDescriptor entityID=\"{entityId}\">\n" +
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
                " </EntityDescriptor>")
                        .replace("{entityId}", entityId);
        return  xml;
    }
}
