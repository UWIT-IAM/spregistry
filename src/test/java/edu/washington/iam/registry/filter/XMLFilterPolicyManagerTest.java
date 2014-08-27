package edu.washington.iam.registry.filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Assert;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class XMLFilterPolicyManagerTest {
    @Autowired
    private XMLFilterPolicyManager filterPolicyManager;

    @Test
    public void testGetAttributes() throws Exception {
        List<Attribute> attributes = filterPolicyManager.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertTrue(attributes.size() > 0);

    }

    @Test
    public void testGetFilterPolicy() throws  Exception {
        FilterPolicyGroup filterPolicyGroup = new FilterPolicyGroup();
        filterPolicyGroup.setId("UW");
        AttributeFilterPolicy attributeFilterPolicy =
                filterPolicyManager.getFilterPolicy(filterPolicyGroup, "https://diafine3.cac.washington.edu/shibboleth");
        Assert.assertNotNull(attributeFilterPolicy);
        Assert.assertEquals(8, attributeFilterPolicy.getAttributeRules().size());
        attributeFilterPolicy = filterPolicyManager.getFilterPolicy(filterPolicyGroup,
                "https://jpf.cac.washington.edu/shibboleth");
        Assert.assertNull(attributeFilterPolicy);
    }
}
