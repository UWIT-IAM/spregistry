package edu.washington.iam.registry.filter;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-db-context.xml")
public class DBFilterPolicyDAOTest {
    @Autowired
    private DBFilterPolicyDAO dao;

    @Autowired
    private JdbcTemplate template;

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
            template.update("insert into filter (group_id, entity_id, xml, status, update_time) " +
                    "values (?, ?, ?, 1, now())",
                    new Object[] {filterPolicyGroup.getId(), fakeId, fakeAttributeFilterPolicyXml(fakeId)});
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
            template.update("insert into filter (group_id, entity_id, xml, status, update_time) " +
                    "values (?, ?, ?, 1, now())",
                    new Object[] {filterPolicyGroup.getId(), fakeId, fakeAttributeFilterPolicyXml(fakeId)});
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

        dao.createFilterPolicy(filterPolicyGroup, attributeFilterPolicy);

        qResults = template.queryForList("select update_time from filter where entity_id = ?"
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
        template.update("insert into filter (group_id, entity_id, xml, status, update_time) values (?, ?, ?, 1, now())",
                new Object[] {
                        filterPolicyGroup.getId(),
                        entityId,
                        fakeAttributeFilterPolicyXml(entityId)
                });
        List<Timestamp> qResults = template.queryForList("select update_time from filter where entity_id = ?"
                , new Object[]{entityId}
                , Timestamp.class);
        Assert.assertEquals(1, qResults.size());
        Timestamp preUpdateTime = qResults.get(0);

        dao.updateFilterPolicy(filterPolicyGroup, attributeFilterPolicy);

        qResults = template.queryForList("select update_time from filter where entity_id = ?"
                , new Object[] {entityId}
                , Timestamp.class);
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
        List<Timestamp> qResults = namedTemplate.queryForList("select update_time from filter where entity_id in (:ids)"
                , new MapSqlParameterSource().addValue("ids", entityIds)
                , Timestamp.class);
        Assert.assertEquals(2, qResults.size());
        Timestamp preUpdateTime = new Timestamp(new Date().getTime());


        List<AttributeFilterPolicy> updatePolicies = new ArrayList<>();
        updatePolicies.add(fakeAttributeFilterPolicy(filterPolicyGroup, updateEntityId));
        updatePolicies.add(fakeAttributeFilterPolicy(filterPolicyGroup, createEntityId));

        dao.updateFilterPolicies(filterPolicyGroup, updatePolicies);

        qResults = namedTemplate.queryForList("select update_time from filter where entity_id in (:ids)"
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
        template.update("insert into filter (group_id, entity_id, xml, status, update_time) values (?, ?, ?, 1, now())",
                new Object[] {
                        filterPolicyGroup.getId(),
                        entityId,
                        fakeAttributeFilterPolicyXml(entityId)
                });
    }
    private FilterPolicyGroup fakeFilterPolicyGroup() {
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("uwrp");
        return filterPolicyGroup;
    }

    private AttributeFilterPolicy fakeAttributeFilterPolicy (FilterPolicyGroup filterPolicyGroup, String entityId)
        throws Exception {
        String filterPolicyXml = fakeAttributeFilterPolicyXml(entityId);
        AttributeFilterPolicy afp = dao.attributeFilterPolicyFromElement(
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(filterPolicyXml.getBytes()))
                        .getDocumentElement(),
                filterPolicyGroup);
        return afp;
    }

    private String fakeAttributeFilterPolicyXml(String entityId){
        return String.format(
                "<AttributeFilterPolicy id=\"%s\">                                                                                                                                            \n" +
                        "  <PolicyRequirementRule xsi:type=\"basic:AttributeRequesterString\" value=\"%s\"/>                                                                                             \n" +
                        "  <AttributeRule attributeID=\"assurance\">                                                                                                                                                                          \n" +
                        "    <PermitValueRule xsi:type=\"basic:ANY\"/>                                                                                                                                                                        \n" +
                        "  </AttributeRule>                                                                                                                                                                                                 \n" +
                        "  <AttributeRule attributeID=\"cn\">                                                                                                                                                                                 \n" +
                        "    <PermitValueRule xsi:type=\"basic:ANY\"/>                                                                                                                                                                        \n" +
                        "  </AttributeRule>                                                                                                                                                                                                 \n" +
                        " </AttributeFilterPolicy>",
                entityId.replaceAll("[:/.]", "_"),
                entityId);
    }
}
