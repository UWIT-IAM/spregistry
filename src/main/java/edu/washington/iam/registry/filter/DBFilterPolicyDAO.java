package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.AttributeNotFoundException;
import edu.washington.iam.registry.exception.FilterPolicyException;
import edu.washington.iam.registry.exception.NoPermissionException;
import edu.washington.iam.tools.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class DBFilterPolicyDAO implements FilterPolicyDAO {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private JdbcTemplate template;

    private List<FilterPolicyGroup> filterPolicyGroups;
    private FilterPolicyGetter filterPolicyGetter = new FilterPolicyGetter();

    //private Map<String, List<AttributeFilterPolicy>> attributeFilterListMap = new HashMap<>();

    @Override
    public List<FilterPolicyGroup> getFilterPolicyGroups() {
        if(filterPolicyGroups == null){
            filterPolicyGroups = template.query("select * from filter_group where status = 1",
                    new RowMapper<FilterPolicyGroup>() {
                        @Override
                        public FilterPolicyGroup mapRow(ResultSet resultSet, int i) throws SQLException {
                            FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
                            filterPolicyGroup.setId(resultSet.getString("id"));
                            filterPolicyGroup.setEditable(resultSet.getInt("edit_mode") == 1);
                            filterPolicyGroup.setDescription(filterPolicyGroup.getId());
                            return filterPolicyGroup;
                        }
                    });
        }
        return filterPolicyGroups;
    }

    @Override
    public FilterPolicyGroup getFilterPolicyGroup(String id) {
        for(FilterPolicyGroup filterPolicyGroup : this.getFilterPolicyGroups()){
            if(filterPolicyGroup.getId().equals(id))
                return  filterPolicyGroup;
        }
        return null;
    }

    private class AttributeFilterPolicyEntries {
        private List<AttributeFilterPolicy> attributeFilterPolicies;

        public Timestamp getLastFetchTime() {
            return lastFetchTime;
        }

        public void setLastFetchTime(Timestamp lastFetchTime) {
            this.lastFetchTime = lastFetchTime;
        }

        public List<AttributeFilterPolicy> getAttributeFilterPolicies() {
            return attributeFilterPolicies;
        }

        public void setAttributeFilterPolicies(List<AttributeFilterPolicy> attributeFilterPolicies) {
            this.attributeFilterPolicies = attributeFilterPolicies;
        }

        private Timestamp lastFetchTime;
    }

    private class FilterPolicyGetter {
        //private final Logger log = LoggerFactory.getLogger(getClass());

        private Map<String, AttributeFilterPolicyEntries> attributeFiltersMap = new HashMap<>();

        public List<AttributeFilterPolicy> getFilterPolicies(final FilterPolicyGroup filterPolicyGroup)
        {
            if(attributeFiltersMap.containsKey(filterPolicyGroup.getId())){
                log.debug("checking filter table for updates to " + filterPolicyGroup.getId());
                Timestamp lastUpdateTime =
                        template.queryForObject("select max(update_time) from filter where group_id = ?",
                            new Object[]{filterPolicyGroup.getId()},
                            Timestamp.class);
                if(lastUpdateTime.after(attributeFiltersMap.get(filterPolicyGroup.getId()).getLastFetchTime())){
                    log.info("attribute filter policy has been updated, rebuilding for " + filterPolicyGroup.getId());
                    attributeFiltersMap.remove(filterPolicyGroup.getId());
                }
                else {
                    return attributeFiltersMap.get(filterPolicyGroup.getId()).getAttributeFilterPolicies();
                }
            }
            Timestamp fetchTime = new Timestamp(new Date().getTime());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            List<AttributeFilterPolicy> tmpAttributeFilterPolicies =
                    template.query("select * from filter where group_id = ? and status = 1",
                            new Object[] {filterPolicyGroup.getId()},
                            new RowMapper<AttributeFilterPolicy>() {
                                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

                                @Override
                                public AttributeFilterPolicy mapRow(ResultSet resultSet, int i) throws SQLException {
                                    Document document;
                                    String entityId = resultSet.getString("entity_id");
                                    String groupId = resultSet.getString("group_id");
                                    try {
                                        DocumentBuilder builder = dbf.newDocumentBuilder();
                                        document = builder.parse(resultSet.getAsciiStream("xml"));

                                    }
                                    catch(Exception e){
                                        return null;
                                    }

                                    AttributeFilterPolicy attributeFilterPolicy =
                                            attributeFilterPolicyFromElement(
                                                    document.getDocumentElement(),
                                                    filterPolicyGroup);

                                    if(attributeFilterPolicy == null){
                                        log.info(String.format("unparseable attribute filter for entity: %s in group: %s",
                                                entityId, groupId));
                                    }

                                    return attributeFilterPolicy;
                                }
                            });

            // TODO: figure if this next block should come out. Only if we're confident there could never be nulls
            List<AttributeFilterPolicy> attributeFilterPolicies = new ArrayList<>();
            for(AttributeFilterPolicy attributeFilterPolicy : tmpAttributeFilterPolicies){
                if(attributeFilterPolicy != null)
                    attributeFilterPolicies.add(attributeFilterPolicy);
            }

            log.info("got the following attributeFilterPolicies: " + attributeFilterPolicies.size());
            AttributeFilterPolicyEntries newEntry = new AttributeFilterPolicyEntries();
            newEntry.setLastFetchTime(fetchTime);
            newEntry.setAttributeFilterPolicies(attributeFilterPolicies);
            attributeFiltersMap.put(filterPolicyGroup.getId(), newEntry);
            return attributeFilterPolicies;
        }
    }

    @Override
    public List<AttributeFilterPolicy> getFilterPolicies(FilterPolicyGroup filterPolicyGroup) {
        return filterPolicyGetter.getFilterPolicies(filterPolicyGroup);
    }

    @Override
    public AttributeFilterPolicy getFilterPolicy(FilterPolicyGroup filterPolicyGroup, String rpid) {
        for(AttributeFilterPolicy attributeFilterPolicy : getFilterPolicies(filterPolicyGroup)){
            if(attributeFilterPolicy.matches(rpid))
                return attributeFilterPolicy;
        }
        return null;
    }

    @Override
    public void updateFilterPolicies(FilterPolicyGroup filterPolicyGroup,
                                     List<AttributeFilterPolicy> attributeFilterPolicies)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {
        Map<String, AttributeFilterPolicy> afpMap = new HashMap<>();
        for(AttributeFilterPolicy attributeFilterPolicy : attributeFilterPolicies){
            afpMap.put(attributeFilterPolicy.getEntityId(), attributeFilterPolicy);
        }

        NamedParameterJdbcTemplate npTemplate = new NamedParameterJdbcTemplate(template);
        List<String> entityIdsToUpdate = npTemplate.queryForList(
                "select entity_id from filter where group_id = :groupId and entity_id in (:ids) and status = 1"
                ,new MapSqlParameterSource()
                .addValue("groupId", filterPolicyGroup.getId())
                .addValue("ids", afpMap.keySet())
                , String.class
                );
        List<String> entityIdsToAdd = new ArrayList<>(afpMap.keySet());
        entityIdsToAdd.removeAll(entityIdsToUpdate);

        for(String addEntityId : entityIdsToAdd){
            createFilterPolicy(filterPolicyGroup, afpMap.get(addEntityId));
        }
        for(String updateEntityId : entityIdsToUpdate){
            updateFilterPolicy(filterPolicyGroup, afpMap.get(updateEntityId));
        }

    }

    public void updateFilterPolicy(FilterPolicyGroup filterPolicyGroup,
                                   AttributeFilterPolicy attributeFilterPolicy) throws FilterPolicyException {
        log.info(String.format("updating %s for %s", attributeFilterPolicy.getEntityId(), filterPolicyGroup.getId()));
        try {
            StringWriter sw = new StringWriter();
            BufferedWriter xout = new BufferedWriter(sw);
            attributeFilterPolicy.writeXml(xout);
            xout.close();
            template.update("update filter set xml = ?, status = 1, update_time = now() where entity_id = ? and group_id = ? ",
                    new Object[]{
                            sw.toString(),
                            attributeFilterPolicy.getEntityId(),
                            filterPolicyGroup.getId()}
            );
        } catch (Exception e) {
            log.info("update trouble: " + e.getMessage());
            throw(new FilterPolicyException(e));
        }
    }

    public void createFilterPolicy(FilterPolicyGroup filterPolicyGroup,
                                   AttributeFilterPolicy attributeFilterPolicy) throws FilterPolicyException {
        log.info(String.format("creating %s for %s", attributeFilterPolicy.getEntityId(), filterPolicyGroup.getId()));
        try {
            StringWriter sw = new StringWriter();
            BufferedWriter xout = new BufferedWriter(sw);
            attributeFilterPolicy.writeXml(xout);
            xout.close();
            template.update("insert into filter (group_id, entity_id, xml, status, update_time) values (?, ?, ?, 1, now())",
                    new Object[]{
                            filterPolicyGroup.getId(),
                            attributeFilterPolicy.getEntityId(),
                            sw.toString()}
            );
        } catch (Exception e) {
            log.info("create trouble: " + e.getMessage());
            throw(new FilterPolicyException(e));
        }
    }

    @Override
    public int removeRelyingParty(FilterPolicyGroup filterPolicyGroup, String entityId)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {
        log.info("removing fp for " + entityId);
        template.update("update filter set status = 0, update_time = now() where group_id = ? and entity_id = ?",
                filterPolicyGroup.getId(),
                entityId);
        // TODO: DB error handling
        // one way to clear the cache
        //attributeFilterListMap.remove(filterPolicyGroup.getId());
        return 200;
    }

    public AttributeFilterPolicy attributeFilterPolicyFromElement(Element topElement,
                                                                  FilterPolicyGroup filterPolicyGroup) {
        AttributeFilterPolicy attributeFilterPolicy = null;
        // scan requirement rules
        for (Element childElement : XMLHelper.getChildElements(topElement)) {
            String name = childElement.getNodeName();

            if (XMLHelper.matches(name, "PolicyRequirementRule")) {
                String type = childElement.getAttribute("xsi:type");
                if (type.equals("basic:AttributeRequesterString"))
                    attributeFilterPolicy = addOrUpdatePolicy(
                            attributeFilterPolicy,
                            childElement,
                            topElement,
                            filterPolicyGroup);
                else if (type.equals("saml:AttributeRequesterEntityAttributeExactMatch"))
                    attributeFilterPolicy = addOrUpdateSamlPolicy(
                            attributeFilterPolicy,
                            childElement,
                            topElement,
                            filterPolicyGroup);
                else if (type.equals("basic:OR")) {
                    // scan rules
                    for (Element orElement : XMLHelper.getChildElements(childElement)) {
                        name = orElement.getNodeName();

                        if (XMLHelper.matches(name,"Rule")) {
                            attributeFilterPolicy = addOrUpdatePolicy(
                                    attributeFilterPolicy,
                                    orElement,
                                    topElement,
                                    filterPolicyGroup);
                        }
                    }
                }
            }
        }
        return attributeFilterPolicy;
    }

    private AttributeFilterPolicy addOrUpdatePolicy(AttributeFilterPolicy existingAttributeFilterPolicy,
                                   Element childElement,
                                   Element topElement,
                                   FilterPolicyGroup filterPolicyGroup
    ) {

        AttributeFilterPolicy attributeFilterPolicy = null;
        String type = childElement.getAttribute("xsi:type");
        String value = childElement.getAttribute("value");
        if (value.length()==0) value = childElement.getAttribute("regex");
        try {
            if(existingAttributeFilterPolicy != null){
                attributeFilterPolicy = existingAttributeFilterPolicy;
                attributeFilterPolicy.addAttributeRules(topElement, filterPolicyGroup.isEditable(), filterPolicyGroup.getId() );
            }
            else
                attributeFilterPolicy = new AttributeFilterPolicy(
                    type,
                    value,
                    topElement,
                    filterPolicyGroup.isEditable(),
                    filterPolicyGroup);
        } catch (FilterPolicyException ex) {
            log.error("load of attribute failed: " + ex);

        }
        return attributeFilterPolicy;
    }

    private AttributeFilterPolicy addOrUpdateSamlPolicy(AttributeFilterPolicy existingAttributeFilterPolicy,
                                       Element childElement,
                                       Element topElement,
                                       FilterPolicyGroup filterPolicyGroup) {
        AttributeFilterPolicy attributeFilterPolicy = null;
        String type = childElement.getAttribute("xsi:type");
        String name = childElement.getAttribute("attributeName");
        if (!name.equals("http://macedir.org/entity-category")) {
            log.error("saml policy not category");
            return existingAttributeFilterPolicy;
        }
        String value = childElement.getAttribute("attributeValue");
        try {
            if (existingAttributeFilterPolicy != null){
                attributeFilterPolicy = existingAttributeFilterPolicy;
                attributeFilterPolicy.addAttributeRules(topElement,
                        filterPolicyGroup.isEditable(),
                        filterPolicyGroup.getId());
            }
            else attributeFilterPolicy = new AttributeFilterPolicy(
                    type,
                    value,
                    topElement,
                    filterPolicyGroup.isEditable(),
                    filterPolicyGroup);
        } catch (FilterPolicyException ex) {
            log.error("load of attribute failed: " + ex);
        }
        return attributeFilterPolicy;
    }


}
