package edu.washington.iam.registry.rp;

import java.io.Serializable;
import java.util.List;

import org.w3c.dom.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.washington.iam.registry.exception.RelyingPartyException;

public interface RelyingPartyManager extends Serializable {

   // public List<RelyingParty> getRelyingParties();
   public List<String> getRelyingPartyIds();
   public List<RelyingParty> getRelyingParties(String sel, String type);

   public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException;
   public RelyingParty getRelyingPartyById(String id, String mdid) throws RelyingPartyException;

   public int updateRelyingParty(Document doc, String mdId) throws RelyingPartyException;
   public int removeRelyingParty(String id, String mdId);

   public RelyingParty genRelyingPartyByLookup(String dns) throws RelyingPartyException;
   public RelyingParty genRelyingPartyByName(String entityId, String dns);
   public RelyingParty genRelyingPartyByCopy(String dns, String entityId);
   
   public RelyingParty updateRelyingPartyMD(RelyingParty rp, RelyingParty rrp);

   public List<Metadata> getMetadata();
   public Metadata getMetadataById(String mdid);


   public void init();
   public void cleanup();

}
