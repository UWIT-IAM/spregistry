package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.AttributeNotFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class AttributeDAOXMLTest {
    @Autowired
    private AttributeDAOXML dao;

    @Test
    public void testGetAttributes() throws Exception {
        List<Attribute> attributeList = dao.getAttributes();
        Assert.assertEquals(28, attributeList.size());

    }

    @Test
    public void testGetAttribute() throws Exception {
        Attribute attribute = dao.getAttribute("surname");
        Assert.assertNotNull(attribute);
        Assert.assertEquals("surname", attribute.getId());
        Assert.assertEquals("u_weblogin_admins", attribute.getAuthorizingGroup());
        Assert.assertEquals("Last name (PDS: sn)", attribute.getDescription());
        Assert.assertNotNull(dao.getAttribute("uwNetID"));
    }

    @Test
    public void testGetAttributeNotFound() throws Exception {
        boolean exceptionCaught = false;
        try {
            Attribute attribute = dao.getAttribute("foo");
        }
        catch (AttributeNotFoundException e){
            exceptionCaught = true;
        }
        Assert.assertTrue(exceptionCaught);
    }
}
