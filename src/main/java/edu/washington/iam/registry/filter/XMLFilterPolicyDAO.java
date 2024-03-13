package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.FilterPolicyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    for (XMLFilterPolicyGroup xmlFilterPolicyGroup : filterPolicyGroups) {
      filterPolicyGroupList.add(xmlFilterPolicyGroup.toFilterPolicyGroup());
    }
    return filterPolicyGroupList;
  }

  @Override
  public FilterPolicyGroup getFilterPolicyGroup(String pgid) {
    for (XMLFilterPolicyGroup filterPolicyGroup : filterPolicyGroups)
      if (filterPolicyGroup.getId().equals(pgid)) return filterPolicyGroup.toFilterPolicyGroup();
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
  public void updateFilterPolicies(
      FilterPolicyGroup filterPolicyGroup,
      List<AttributeFilterPolicy> attributeFilterPolicies,
      String updatedBy)
      throws FilterPolicyException {
    // updates against AttributeFilterPolicy mean that existing afps are already updated in memory
    // this means that it's updated the moment we do writePolicyGroup()
    XMLFilterPolicyGroup xmlFilterPolicyGroup = this.getXMLFilterPolicyGroup(filterPolicyGroup);
    for (AttributeFilterPolicy attributeFilterPolicy : attributeFilterPolicies) {
      if (xmlFilterPolicyGroup.getFilterPolicy(attributeFilterPolicy.getEntityId()) == null) {
        xmlFilterPolicyGroup.getFilterPolicies().add(attributeFilterPolicy);
      }
      // else already updated in memory
    }
    xmlFilterPolicyGroup.writePolicyGroup();
  }

  @Override
  public int removeRelyingParty(
      FilterPolicyGroup filterPolicyGroup, String entityId, String updatedBy)
      throws FilterPolicyException {
    return this.getXMLFilterPolicyGroup(filterPolicyGroup).removeFilterPolicy(entityId);
  }

  private XMLFilterPolicyGroup getXMLFilterPolicyGroup(FilterPolicyGroup filterPolicyGroup) {
    for (XMLFilterPolicyGroup xmlFilterPolicyGroup : filterPolicyGroups) {
      if (xmlFilterPolicyGroup.getId().equals(filterPolicyGroup.getId())) {
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
    for (int p = 0; p < policyGroupSources.size(); p++) {
      try {
        XMLFilterPolicyGroup pg = new XMLFilterPolicyGroup(policyGroupSources.get(p));
        filterPolicyGroups.add(pg);
      } catch (FilterPolicyException e) {
        log.error("could not load policy group ");
      }
    }
  }

  @PostConstruct
  private void init() {
    loadPolicyGroups();
  }
}
