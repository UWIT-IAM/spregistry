package edu.washington.iam.registry.proxy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/applicationContext.xml")
public class ProxyManagerDBTest {
    @Autowired
    private ProxyManagerDB dao;

    @Test
    public void testGetProxy() throws Exception {
        Proxy p = dao.getProxy("https://jpf.cac.washington.edu/shibboleth");
        Assert.assertNotNull(p);
    }
}
