package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.rp.RelyingParty;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-db-context.xml")
public class DBFilterPolicyDAOTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DBFilterPolicyDAO dao;

    @Autowired
    private JdbcTemplate template;

    private String remoteUser  = "mattjm";


    private List<String> fakeEntityIds = Arrays.asList(
            "https://testupdate.s.uw.edu/shibboleth",
            "https://testcreate.s.uw.edu/shibboleth",
            "https://testnoupdate.s.uw.edu/shibboleth");

    @Before
    public void setupInitialData() {
        setupWithRPs(fakeEntityIds);
    }

    @After
    public void teardown(){
        teardownRPs(fakeEntityIds);
    }

    @Test
    public void testGetFilterPolicyGroups() throws Exception {
        List<FilterPolicyGroup> filterPolicyGroups = dao.getFilterPolicyGroups();
        Assert.assertTrue(filterPolicyGroups.size() > 0);
    }

    @Test
    public void testGetFilterPolicyGroup() throws Exception {
        FilterPolicyGroup filterPolicyGroup = dao.getFilterPolicyGroup("uwrp");
        Assert.assertNotNull(filterPolicyGroup);
    }

    @Test
    public void testGetFilterPolicies() throws  Exception {
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("uwrp");
        List<String> fakeEntityIds = Arrays.asList("testsp1", "testsp2", "testsp3", "testsp4");
        for(String fakeId : fakeEntityIds){
            template.update("insert into filter (uuid, group_id, entity_id, xml, end_time, start_time) " +
                    "values (?, ?, ?, ?, null, now())",
                    genUUID(), filterPolicyGroup.getId(), fakeId, fakeAttributeFilterPolicyXml(fakeId));
        }

        List<AttributeFilterPolicy> afps = dao.getFilterPolicies(filterPolicyGroup);
        Assert.assertEquals(fakeEntityIds.size(), afps.size());
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
        namedTemplate.update("delete from filter where entity_id in (:entityIds)",
                new MapSqlParameterSource().addValue("entityIds", fakeEntityIds));
    }

    @Test
    public void testGetFilterPoliciesCaching() throws  Exception {
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("uwrp");
        List<String> fakeEntityIds = Arrays.asList("testsp1", "testsp2", "testsp3", "testsp4");
        for(String fakeId : fakeEntityIds){
            template.update("insert into filter (uuid, group_id, entity_id, xml, end_time, start_time) " +
                            "values (?, ?, ?, ?, null, now())",
                    genUUID(), filterPolicyGroup.getId(), fakeId, fakeAttributeFilterPolicyXml(fakeId));
        }

        List<AttributeFilterPolicy> afps = dao.getFilterPolicies(filterPolicyGroup);
        Assert.assertEquals(fakeEntityIds.size(), afps.size());
        // get it again to test that update check works
        afps = dao.getFilterPolicies(filterPolicyGroup);
        Assert.assertEquals(fakeEntityIds.size(), afps.size());

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
        namedTemplate.update("delete from filter where entity_id in (:entityIds)",
                new MapSqlParameterSource().addValue("entityIds", fakeEntityIds));
    }

    @Test
    public void testAttributeFilterPolicyFromElement() throws Exception {
        String inFilterPolicyXml = fakeAttributeFilterPolicyXml("https://example.com/shibboleth");
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("uwrp");
        AttributeFilterPolicy afp = dao.attributeFilterPolicyFromElement(
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(inFilterPolicyXml.getBytes()))
                        .getDocumentElement(),
                filterPolicyGroup);

        StringWriter sw = new StringWriter();
        BufferedWriter xout = new BufferedWriter(sw);
        afp.writeXml(xout);
        xout.close();

        Assert.assertEquals(inFilterPolicyXml.replaceAll("\\s+", ""),
                sw.toString().replaceAll("\\s+", ""));

    }

    @Test
    public void testAttributeFilterPolicyParseComplexRegex() throws Exception {
        String inFilterPolicyXml = "<AttributeFilterPolicy id=\"releaseStuffToUW\"> " +
                "<PolicyRequirementRule xsi:type=\"basic:AttributeRequesterRegex\" " +
                " regex=\"^https://[^/]*\\.(uw|washington)\\.edu(/.*)?$\" /> " +
                "<AttributeRule attributeID=\"uwNetID\"> <PermitValueRule xsi:type=\"basic:ANY\" /> </AttributeRule> " +
                "<AttributeRule attributeID=\"ePPN\"> <PermitValueRule xsi:type=\"basic:ANY\" /> </AttributeRule> " +
                "<AttributeRule attributeID=\"affiliation\"> <PermitValueRule xsi:type=\"basic:ANY\"/> </AttributeRule> " +
                "<AttributeRule attributeID=\"scopedAffiliation\"> <PermitValueRule xsi:type=\"basic:ANY\"/> </AttributeRule> " +
                "</AttributeFilterPolicy>";
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("uwcore");
        Element afpElement = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(inFilterPolicyXml.getBytes()))
                .getDocumentElement();
        AttributeFilterPolicy afp = dao.attributeFilterPolicyFromElement(
                afpElement,
                filterPolicyGroup);

        Assert.assertNotNull(afp);
        Assert.assertTrue(afp.matches("https://foo.washington.edu/shibboleth"));
        Assert.assertTrue(afp.matches("https://foo.uw.edu/shibboleth"));
        Assert.assertTrue(afp.matches("https://foo.washington.edu"));
        Assert.assertTrue(afp.matches("https://foo.uw.edu"));
        Assert.assertFalse(afp.matches("https://foo.uw.edu.haxxorz.ru/shibboleth"));
        Assert.assertFalse(afp.matches("https://haxxorz.ru/foo.washington.edu/shibboleth"));
        Assert.assertFalse(afp.matches("https://foo.notwashington.edu/shibboleth"));

        Assert.assertEquals(4, afp.getAttributeRules().size());
    }

    @Test
    @Ignore("The possibility of a null AttributeFilterPolicy is understood, consider removing this test")
    public void testAttributeFilterPolicyFromElementRuleAndNotParses() throws  Exception {
        String inFilterPolicyXml = "<AttributeFilterPolicy id=\"releaseTransientIdToAnyone\"> " +
                " <PolicyRequirementRule xsi:type=\"basic:AND\">" +
                "  <basic:Rule xsi:type=\"basic:NOT\"> " +
                "   <basic:Rule xsi:type=\"basic:AttributeRequesterString\" value=\"google.com\" /> " +
                "  </basic:Rule> " +
                "  <basic:Rule xsi:type=\"basic:NOT\"> " +
                "   <basic:Rule xsi:type=\"basic:AttributeRequesterString\" value=\"https://hmcpark.t2hosted.com/cmn/auth.aspx\" /> " +
                "  </basic:Rule> <basic:Rule xsi:type=\"basic:NOT\">" +
                "  <basic:Rule xsi:type=\"basic:AttributeRequesterString\" value=\"http://www.instructure.com/saml2\" /> </basic:Rule> " +
                " </PolicyRequirementRule> " +
                " <AttributeRule attributeID=\"transientId\"> " +
                "  <PermitValueRule xsi:type=\"basic:ANY\" /> " +
                " </AttributeRule> " +
                "</AttributeFilterPolicy>";
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("uwcore");
        Element afpElement = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(inFilterPolicyXml.getBytes()))
                .getDocumentElement();
        AttributeFilterPolicy afp = dao.attributeFilterPolicyFromElement(
                afpElement,
                filterPolicyGroup);
        Assert.assertNotNull(afp);
    }

    @Test
    public void testCreateFilterPolicy() throws Exception {
        String entityId = "https://testcreate.s.uw.edu/shibboleth";
        FilterPolicyGroup filterPolicyGroup = fakeFilterPolicyGroup();
        AttributeFilterPolicy attributeFilterPolicy = fakeAttributeFilterPolicy(filterPolicyGroup, entityId);

        template.update("delete from filter where entity_id = ?", entityId);
        List<Timestamp> qResults = template.queryForList("select entity_id from filter where entity_id = ?"
                , new Object[]{entityId}
                , Timestamp.class);
        // check that there's nothing in there already
        Assert.assertEquals(0, qResults.size());
        Timestamp preUpdateTime = new Timestamp(new Date().getTime());
        Thread.sleep(500);
        attributeFilterPolicy.setUuid(genUUID());
        dao.createFilterPolicy(filterPolicyGroup, attributeFilterPolicy, remoteUser);

        qResults = template.queryForList("select start_time from filter where entity_id = ?"
                , new Object[] {entityId}
                , Timestamp.class);
        Assert.assertEquals(1, qResults.size());
        Assert.assertTrue(qResults.get(0).after(preUpdateTime));

        template.update("delete from filter where entity_id = ?", entityId);
    }

    @Test
    public void testCreateFilterPolicyPreviouslyDeleted() throws Exception {
        //make sure an old end_time is null status doesn't break "updating" the status again
        String entityId = "https://testcreate.s.uw.edu/shibboleth";
        FilterPolicyGroup filterPolicyGroup = fakeFilterPolicyGroup();
        AttributeFilterPolicy attributeFilterPolicy = fakeAttributeFilterPolicy(filterPolicyGroup, entityId);

        template.update("delete from filter where entity_id = ?", entityId);
        template.update("insert into filter (uuid, group_id, entity_id, xml, end_time, start_time) values (?, ?, ?, ?, now(), now())",
                genUUID(), filterPolicyGroup.getId(), entityId,  fakeAttributeFilterPolicyXml(entityId));
        List<Timestamp> qResults = template.queryForList("select start_time from filter where entity_id = ? and end_time is null"
                , new Object[]{entityId}
                , Timestamp.class);
        Assert.assertEquals(0, qResults.size());
        Timestamp preUpdateTime = new Timestamp(new Date().getTime());
        Thread.sleep(500);
        dao.updateFilterPolicies(filterPolicyGroup, Arrays.asList(attributeFilterPolicy), remoteUser);

        qResults = template.queryForList("select start_time from filter where entity_id = ? and end_time is null"
                , new Object[] {entityId}
                , Timestamp.class);
        Assert.assertEquals(1, qResults.size());
        Assert.assertTrue(qResults.get(0).after(preUpdateTime));

        template.update("delete from filter where entity_id = ?", entityId);
    }

    @Test
    public void testUpdateFilterPolicy() throws Exception {
        String entityId = "https://testupdate.s.uw.edu/shibboleth";
        FilterPolicyGroup filterPolicyGroup = fakeFilterPolicyGroup();
        AttributeFilterPolicy attributeFilterPolicy = fakeAttributeFilterPolicy(filterPolicyGroup, entityId);

        template.update("delete from filter where entity_id = ?", entityId);
        template.update("insert into filter (group_id, entity_id, xml, start_time, end_time, uuid) values (?, ?, ?, now(), ?, ?)",
                filterPolicyGroup.getId(), entityId, fakeAttributeFilterPolicyXml(entityId), null, genUUID());
        List<Timestamp> qResults = template.queryForList("select start_time from filter where entity_id = ? and end_time is null"
                , new Object[]{entityId}
                , Timestamp.class);
        Assert.assertEquals(1, qResults.size());
        Timestamp preUpdateTime = qResults.get(0);
        //it's too fast!
        Thread.sleep(500);
        dao.updateFilterPolicy(filterPolicyGroup, attributeFilterPolicy, remoteUser);

        qResults = template.queryForList("select start_time from filter where entity_id = ? and end_time is null"
                , new Object[] {entityId}
                , Timestamp.class);
        log.info("before: " + preUpdateTime.toString() + " after: " + qResults.get(0).toString());
        Assert.assertEquals(1, qResults.size());
        Assert.assertTrue(qResults.get(0).after(preUpdateTime));
        template.update("delete from filter where entity_id = ?", entityId);
    }

    @Test
    public void testUpdateFilterPolicies() throws Exception {
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
        String updateEntityId = "https://testupdate.s.uw.edu/shibboleth";
        String createEntityId = "https://testcreate.s.uw.edu/shibboleth";
        String noUpdateEntityId = "https://testnoupdate.s.uw.edu/shibboleth";
        List<String> entityIds = Arrays.asList(updateEntityId, createEntityId, noUpdateEntityId);
        FilterPolicyGroup filterPolicyGroup = fakeFilterPolicyGroup();
        namedTemplate.update("delete from filter where entity_id in (:ids)",
                new MapSqlParameterSource().addValue("ids", entityIds));
        insertFakeData(filterPolicyGroup, updateEntityId);
        insertFakeData(filterPolicyGroup, noUpdateEntityId);
        List<Timestamp> qResults = namedTemplate.queryForList("select start_time from filter where entity_id in (:ids)"
                , new MapSqlParameterSource().addValue("ids", entityIds)
                , Timestamp.class);
        Assert.assertEquals(2, qResults.size());
        Timestamp preUpdateTime = new Timestamp(new Date().getTime());
        Thread.sleep(500);

        List<AttributeFilterPolicy> updatePolicies = new ArrayList<>();
        updatePolicies.add(fakeAttributeFilterPolicy(filterPolicyGroup, updateEntityId));
        updatePolicies.add(fakeAttributeFilterPolicy(filterPolicyGroup, createEntityId));

        dao.updateFilterPolicies(filterPolicyGroup, updatePolicies, remoteUser);

        qResults = namedTemplate.queryForList("select start_time from filter where end_time is null and entity_id in (:ids)"
                , new MapSqlParameterSource().addValue("ids", entityIds)
                , Timestamp.class);
        Assert.assertEquals(3, qResults.size());
        int updateCount = 0;
        for(Timestamp result : qResults){
            if(result.after(preUpdateTime))
                updateCount++;
        }
        Assert.assertEquals(2, updateCount);

        //clean up
        namedTemplate.update("delete from filter where entity_id in (:ids)",
                new MapSqlParameterSource().addValue("ids", entityIds));
    }

    private void insertFakeData(FilterPolicyGroup filterPolicyGroup, String entityId) throws Exception {
        List<UUID> uuid = template.queryForList("select uuid from metadata where entity_id = ? and end_time is null",
                UUID.class,
                entityId);
        template.update("insert into filter (uuid, group_id, entity_id, xml, end_time, start_time) values (?, ?, ?, ?, null, now())",
                uuid.get(0), filterPolicyGroup.getId(), entityId, fakeAttributeFilterPolicyXml(entityId));
    }
    private FilterPolicyGroup fakeFilterPolicyGroup() {
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("uwrp");
        return filterPolicyGroup;
    }

    private AttributeFilterPolicy fakeAttributeFilterPolicy (FilterPolicyGroup filterPolicyGroup, String entityId)
        throws Exception {
        String filterPolicyXml = fakeAttributeFilterPolicyXml(entityId);
        return dao.attributeFilterPolicyFromElement(
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(filterPolicyXml.getBytes()))
                        .getDocumentElement(),
                filterPolicyGroup);
    }

    private String fakeAttributeFilterPolicyXml(String entityId){
        return String.format(
                "<AttributeFilterPolicy id=\"%s\">\n" +
                        "  <PolicyRequirementRule xsi:type=\"basic:AttributeRequesterString\" value=\"%s\"/>\n" +
                        "  <AttributeRule attributeID=\"assurance\">\n" +
                        "    <PermitValueRule xsi:type=\"basic:ANY\"/>\n" +
                        "  </AttributeRule>\n" +
                        "  <AttributeRule attributeID=\"cn\">\n" +
                        "    <PermitValueRule xsi:type=\"basic:ANY\"/>\n" +
                        "  </AttributeRule>\n" +
                        " </AttributeFilterPolicy>",
                entityId.replaceAll("[:/.]", "_"),
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

        // All tests expect filter to be empty at start.
        template.update("delete from filter");
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
