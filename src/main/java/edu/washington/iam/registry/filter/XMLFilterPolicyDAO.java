package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.AttributeNotFoundException;
import edu.washington.iam.registry.exception.FilterPolicyException;
import edu.washington.iam.registry.exception.NoPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class XMLFilterPolicyDAO implements FilterPolicyDAO {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<XMLFilterPolicyGroup> filterPolicyGroups;

    public void setPolicyGroupSources(List<Properties> policyGroupSources) {
        this.policyGroupSources = policyGroupSources;
    }

    List<Properties> policyGroupSources;

    @Override
    public List<FilterPolicyGroup> getFilterPolicyGroups() {
        List<FilterPolicyGroup> filterPolicyGroupList = new ArrayList<>();
        for(XMLFilterPolicyGroup xmlFilterPolicyGroup : filterPolicyGroups)
        {
            filterPolicyGroupList.add(xmlFilterPolicyGroup.toFilterPolicyGroup());
        }
        return filterPolicyGroupList;
    }

    @Override
    public FilterPolicyGroup getFilterPolicyGroup(String pgid) {
        for (XMLFilterPolicyGroup filterPolicyGroup : filterPolicyGroups)
            if (filterPolicyGroup.getId().equals(pgid))
                return filterPolicyGroup.toFilterPolicyGroup();
        return null;
    }

    @Override
    public List<AttributeFilterPolicy> getFilterPolicies(FilterPolicyGroup filterPolicyGroup) {
        return getXMLFilterPolicyGroup(filterPolicyGroup).getFilterPolicies();
    }

    @Override
    public AttributeFilterPolicy getFilterPolicy(FilterPolicyGroup filterPolicyGroup, String rpid) {
        XMLFilterPolicyGroup xmlFilterPolicyGroup = this.getXMLFilterPolicyGroup(filterPolicyGroup);
        xmlFilterPolicyGroup.refreshPolicyIfNeeded();
        return xmlFilterPolicyGroup.getFilterPolicy(rpid);
    }

    @Override
    public void updateFilterPolicies(FilterPolicyGroup filterPolicyGroup,
                                     List<AttributeFilterPolicy> attributeFilterPolicies)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException
    {
        XMLFilterPolicyGroup xmlFilterPolicyGroup = this.getXMLFilterPolicyGroup(filterPolicyGroup);
        for(AttributeFilterPolicy attributeFilterPolicy : attributeFilterPolicies){
            if(xmlFilterPolicyGroup.getFilterPolicy(attributeFilterPolicy.getEntityId()) == null){
                xmlFilterPolicyGroup.getFilterPolicies().add(attributeFilterPolicy);
            }
            // else already updated in memory
        }
        xmlFilterPolicyGroup.writePolicyGroup();
    }

    @Override
    public int removeRelyingParty(FilterPolicyGroup filterPolicyGroup, String entityId)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {
        return this.getXMLFilterPolicyGroup(filterPolicyGroup).removeFilterPolicy(entityId);
    }

    @Override
    public void addAttributeRule(FilterPolicyGroup filterPolicyGroup, String entityId, Attribute attribute,
                                 String type, String value)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {
        this.getXMLFilterPolicyGroup(filterPolicyGroup).addAttribute(entityId, attribute.getId(), type, value);
    }

    @Override
    public void removeAttributeRule(FilterPolicyGroup filterPolicyGroup, String entityId, Attribute attribute,
                                    String type, String value)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException {
        this.getXMLFilterPolicyGroup(filterPolicyGroup).removeAttribute(entityId, attribute.getId(), type, value);
    }

    private XMLFilterPolicyGroup getXMLFilterPolicyGroup(FilterPolicyGroup filterPolicyGroup){
        for(XMLFilterPolicyGroup xmlFilterPolicyGroup : filterPolicyGroups){
            if(xmlFilterPolicyGroup.getId().equals(filterPolicyGroup.getId())){
                return xmlFilterPolicyGroup;
            }
        }
        log.info("The unthinkable has happened");
        return null;
    }

    private void loadPolicyGroups() {
        filterPolicyGroups = new Vector();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);

        // load policyGroups from each source
        for (int p=0; p<policyGroupSources.size(); p++) {
            try {
                XMLFilterPolicyGroup pg = new XMLFilterPolicyGroup(policyGroupSources.get(p));
                filterPolicyGroups.add(pg);
            } catch (FilterPolicyException e) {
                log.error("could not load policy group ");
            }
        }

    }

    @PostConstruct
    private void init(){
        loadPolicyGroups();
    }
}
