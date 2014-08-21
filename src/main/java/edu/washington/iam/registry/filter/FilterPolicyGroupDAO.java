package edu.washington.iam.registry.filter;

import java.util.List;

public interface FilterPolicyGroupDAO {
    List<FilterPolicyGroup> getFilterPolicyGroups();
    FilterPolicyGroup getFilterPolicyGroup(String id);
}
