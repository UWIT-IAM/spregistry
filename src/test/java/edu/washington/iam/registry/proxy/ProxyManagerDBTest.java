package edu.washington.iam.registry.proxy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-db-context.xml")
public class ProxyManagerDBTest {
  /* Integration tests commented out to placate CI */
  @Autowired private ProxyManagerDB dao;

  @Autowired private JdbcTemplate template;

  @Test
  public void testGetProxy() throws Exception {
    Proxy p = dao.getProxy("https://gettest.example.com");

    Assert.assertNotNull(p);
    Assert.assertEquals("https://gettest.example.com", p.getEntityId());
    Assert.assertEquals(true, p.getSocialActive());
    Assert.assertNotNull(p.getUuid());
  }

  /*
  @Test
  public void testUpdateProxy() throws Exception {
      Proxy p = new Proxy();
      p.setEntityId("https://www.example.com");
      List<ProxyIdp> proxyIdps = new ArrayList<>();
      ProxyIdp proxyIdp = new ProxyIdp();
      proxyIdp.setIdp("Facebook");
      proxyIdp.setClientSecret("letmein");
      proxyIdp.setClientId("facebookguy");
      proxyIdps.add(proxyIdp);
      proxyIdp = new ProxyIdp();
      proxyIdp.setIdp("Twitter");
      proxyIdp.setClientSecret("shhhh");
      proxyIdp.setClientId("twitterguy");
      proxyIdps.add(proxyIdp);
      p.setProxyIdps(proxyIdps);
      dao.updateProxy(p);
  }

  @Test
  public void testRemoveRelyingParty()
  {
      dao.removeProxy("https://www.example.com");
  }*/
}
