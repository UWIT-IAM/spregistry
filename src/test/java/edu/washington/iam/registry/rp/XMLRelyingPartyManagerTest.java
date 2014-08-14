package edu.washington.iam.registry.rp;

import org.junit.Test;
import org.junit.Assert;

public class XMLRelyingPartyManagerTest {
    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGenRelyingPartyByName() throws Exception {
        XMLRelyingPartyManager rpm = new XMLRelyingPartyManager();
        RelyingParty rp = rpm.genRelyingPartyByName("https://www.example.com", "www.example.com");
        Assert.assertEquals("https://www.example.com", rp.getEntityId());
    }
}
