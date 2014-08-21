package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.AttributeNotFoundException;
import edu.washington.iam.registry.exception.FilterPolicyException;
import edu.washington.iam.registry.exception.NoPermissionException;

import java.util.List;

public interface FilterPolicyDAO {

    List<FilterPolicyGroup> getFilterPolicyGroups();
    FilterPolicyGroup getFilterPolicyGroup(String id);

    List<AttributeFilterPolicy> getFilterPolicies(FilterPolicyGroup filterPolicyGroup);
    // returns filter policy for a given rp or null if none exist
    AttributeFilterPolicy getFilterPolicy(FilterPolicyGroup filterPolicyGroup,
                                          String rpid);
    // add new or update existing filterPolicies
    void updateFilterPolicies(FilterPolicyGroup filterPolicyGroup,
                              List<AttributeFilterPolicy> attributeFilterPolicies)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;
    int removeRelyingParty(FilterPolicyGroup filterPolicyGroup,
                           String entityId)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;
    // slated for demolition
    void addAttributeRule(FilterPolicyGroup filterPolicyGroup,
                             String entityId,
                             Attribute attribute,
                             String type,
                             String value)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;
    // deathwatch
    void removeAttributeRule(FilterPolicyGroup filterPolicyGroup,
                            String entityId,
                            Attribute attribute,
                            String type,
                            String value)
            throws FilterPolicyException, AttributeNotFoundException, NoPermissionException;
}