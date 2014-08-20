package edu.washington.iam.registry.filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class XMLFilterPolicyManagerTest {
    @Autowired
    private XMLFilterPolicyManager filterPolicyManager;

    @Test
    public void testGetAttributes() throws Exception {
        List<Attribute> attributes = filterPolicyManager.getAttributes();
        Assert.notNull(attributes);
        Assert.notEmpty(attributes);

    }
}
