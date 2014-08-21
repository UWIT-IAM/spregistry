package edu.washington.iam.registry.filter;

import java.util.List;

public interface FilterPolicyDAO {

    List<AttributeFilterPolicy> getFilterPolicies(FilterPolicyGroup filterPolicyGroup);
    // returns filter policy for a given rp or null if none exist
    AttributeFilterPolicy getFilterPolicy(FilterPolicyGroup filterPolicyGroup,
                                          String rpid);
    // add new or update existing filterPolicies
    void updateFilterPolicies(FilterPolicyGroup filterPolicyGroup,
                              List<AttributeFilterPolicy> attributeFilterPolicies);
    int removeRelyingParty(FilterPolicyGroup filterPolicyGroup,
                           String entityId);
    void addAttributeRule(FilterPolicyGroup filterPolicyGroup,
                             String entityId,
                             Attribute attribute,
                             String type,
                             String value);
    void removeAttributeRule(FilterPolicyGroup filterPolicyGroup,
                            String entityId,
                            Attribute attribute,
                            String type,
                            String value);
}
