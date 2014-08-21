package edu.washington.iam.registry.filter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class XMLFilterPolicyDAOTest {
    @Autowired
    private XMLFilterPolicyDAO dao;

    @Test
    public void testGetFilterPolicyGroups() throws Exception {
        List<FilterPolicyGroup> filterPolicyGroups = dao.getFilterPolicyGroups();
        Assert.assertEquals(1, filterPolicyGroups.size());
    }
}
