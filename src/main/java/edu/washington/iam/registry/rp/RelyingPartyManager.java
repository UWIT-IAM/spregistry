package edu.washington.iam.registry.rp;

import java.io.Serializable;
import java.util.List;

import org.w3c.dom.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.washington.iam.registry.exception.RelyingPartyException;

public interface RelyingPartyManager extends Serializable {

   public List<RelyingParty> getRelyingParties();
   public List<String> getMetadataIds();
   public List<RelyingPartyEntry> searchRelyingPartyIds(String searchStr, String metadataId);
   public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException;
   public RelyingParty getRelyingPartyById(String id, String mdid) throws RelyingPartyException;

   public int updateRelyingParty(RelyingParty relyingParty, String mdId) throws RelyingPartyException;
   public int removeRelyingParty(String id, String mdId);

   public RelyingParty genRelyingPartyByLookup(String dns) throws RelyingPartyException;
   public RelyingParty genRelyingPartyByName(String entityId, String dns);
   public RelyingParty genRelyingPartyByCopy(String dns, String entityId);

   public boolean isMetadataEditable(String mdid);

   public void init();
   public void cleanup();

}
