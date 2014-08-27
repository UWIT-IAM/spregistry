package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.AttributeNotFoundException;

import java.util.List;

public interface AttributeDAO {
    List<Attribute> getAttributes();
    Attribute getAttribute(String id) throws AttributeNotFoundException;
}
