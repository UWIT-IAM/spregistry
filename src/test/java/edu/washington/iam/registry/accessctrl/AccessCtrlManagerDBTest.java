package edu.washington.iam.registry.accessctrl;

import edu.washington.iam.registry.accessctrl.AccessCtrlManagerDB;
import edu.washington.iam.registry.accessctrl.AccessCtrl;
import edu.washington.iam.registry.exception.AccessCtrlException;
import edu.washington.iam.registry.rp.RelyingParty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Arrays;
import java.util.List;
import edu.washington.iam.registry.exception.RelyingPartyException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-db-context.xml")
public class AccessCtrlManagerDBTest {

    @Autowired
    private AccessCtrlManagerDB dao;
    @Autowired
    private JdbcTemplate template;

    private String remoteUser = "mattjm";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<String> fakeEntityIds = Arrays.asList(
            "https://accessctrltest1.s.uw.edu/shibboleth",
            "https://accessctrltest2.s.uw.edu/shibboleth",
            "https://accessctrltest3.s.uw.edu/shibboleth",
            "https://accessctrltest4.s.uw.edu/shibboleth");

    @Before
    public void setupInitialData() {
        setupWithRPs(fakeEntityIds);
    }

    @After
    public void teardown(){
        teardownRPs(fakeEntityIds);
    }


    //should be null if we've never set one before
    @Test
    public void testInitialAccessCtrl(){

        template.update("delete from access_control where entity_id = ?",
                fakeEntityIds.get(0));

        AccessCtrl myCtrl = new AccessCtrl();
        myCtrl.setEntityId("phony");
        myCtrl = dao.getAccessCtrl(fakeEntityIds.get(0));
        //make sure phony access control object is overwritten with null one returned from get method
        Assert.assertFalse(myCtrl.getAuto2FA());
        Assert.assertFalse(myCtrl.getCond2FA());
        Assert.assertFalse(myCtrl.getConditional());

    }

    @Test
    public void testEnableStuff(){
        String entityId = fakeEntityIds.get(0);
        AccessCtrl myCtrl = new AccessCtrl();

        //clear out any previous records
        template.update("delete from filter where entity_id = ?",
                entityId);

        //get UUID from metadata table
        List<UUID> uuid = template.queryForList("select uuid from metadata where entity_id = ? and end_time is null",
                UUID.class,
                entityId);
        //set up test object
        myCtrl.setUuid(uuid.get(0));
        try {
            myCtrl.setAuto2FA(true);
            myCtrl.setEntityId(entityId);
        } catch (AccessCtrlException e) {}
        //use test object to update access control for this entity id
        try {
            dao.updateAccessCtrl(myCtrl, remoteUser);
        } catch (AccessCtrlException e) {
            log.info("test enable 2fa threw exception");
        }

        //new empty access control object
        AccessCtrl myCtrlAfter = new AccessCtrl();
        //get access control data for this entity id
        myCtrlAfter = dao.getAccessCtrl(entityId);
        //verify expected state
        Assert.assertTrue(myCtrlAfter.getAuto2FA());
        Assert.assertTrue(!myCtrlAfter.getConditional());
        Assert.assertTrue(myCtrlAfter.getConditionalGroup() == "");
        //get ALL DB entries (current and former)
        List<Integer> ids = template.queryForList("select id from access_control where entity_id = ?",
                Integer.class,
                entityId);
        //should just be 1
        Assert.assertTrue(ids.size() == 1);

        //now enable conditional access
        //set group name and properties on object (changing access control object from above)
        String myGroup = "uw_iam_my_group";
        myCtrl.setConditional(true);
        myCtrl.setConditionalGroup(myGroup);
        //do the update using appropriate method
        try {
            dao.updateAccessCtrl(myCtrl, remoteUser);
        } catch (AccessCtrlException e) {
            log.info("test enable conditional access threw exception");
        }
        //reset retrieval object
        myCtrlAfter = new AccessCtrl();
        myCtrlAfter = dao.getAccessCtrl(entityId);
        //verify expected state
        Assert.assertTrue(myCtrlAfter.getAuto2FA());
        Assert.assertTrue(myCtrlAfter.getConditional());
        Assert.assertTrue(myCtrlAfter.getConditionalGroup() == myGroup);
        //get ALL DB entries (current and former)
        ids = template.queryForList("select id from access_control where entity_id = ?",
                Integer.class,
                entityId);
        //should be 2
        Assert.assertTrue(ids.size() == 2);


    }


    @Test
    public void testDisableAllStuff() {
        String entityId = fakeEntityIds.get(0);
        AccessCtrl myCtrl = new AccessCtrl();

        //start by creating a new DB entry (we don't care if there are others already)
        //get UUID from metadata table
        List<UUID> uuid = template.queryForList("select uuid from metadata where entity_id = ? and end_time is null",
                UUID.class,
                entityId);
        //set up test object
        myCtrl.setUuid(uuid.get(0));
        try {
            myCtrl.setAuto2FA(true);
        } catch (AccessCtrlException e) {}
        myCtrl.setEntityId(entityId);
        //use test object to update access control for this entity id
        try {
            dao.updateAccessCtrl(myCtrl, remoteUser);
        } catch (AccessCtrlException e) {
            log.info("test enable 2fa threw exception");
        }
        //new empty access control object
        AccessCtrl myCtrlAfter = new AccessCtrl();
        //get access control data for this entity id
        myCtrlAfter = dao.getAccessCtrl(entityId);
        //verify expected state
        Assert.assertTrue(myCtrlAfter.getAuto2FA());
        Assert.assertTrue(!myCtrlAfter.getConditional());
        Assert.assertTrue(myCtrlAfter.getConditionalGroup() == "");


        //now call the remove method...
        Integer returnCode = 0;
        try {
            returnCode = dao.removeAccessCtrl(entityId, remoteUser);
        } catch (AccessCtrlException e) {log.info("test delete access control threw exception");}
        Assert.assertTrue(returnCode == 200);

        List<Integer> ids = template.queryForList("select id from access_control where entity_id = ? and end_time is null",
                Integer.class,
                entityId);
        //should be none
        Assert.assertTrue(ids.size() == 0);

    }





















//TEST SETUP AND TEARDOWN METHODS

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
