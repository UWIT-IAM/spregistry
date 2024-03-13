package edu.washington.iam.registry.rp;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class RelyingPartyManagerImplTest {
  @Autowired private RelyingPartyManagerImpl rpManager;

  @org.junit.Before
  public void setUp() throws Exception {}

  @org.junit.After
  public void tearDown() throws Exception {}

  @Test
  public void testGenRelyingPartyByName() throws Exception {
    RelyingParty rp = rpManager.genRelyingPartyByName("https://www.example.com", "www.example.com");
    Assert.assertEquals("https://www.example.com", rp.getEntityId());
  }

  @Test
  public void testGetMetadataIds() throws Exception {
    Assert.assertTrue(rpManager.getMetadataIds().contains("UW"));
  }
}
