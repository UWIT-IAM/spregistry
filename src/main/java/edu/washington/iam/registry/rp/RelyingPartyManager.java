package edu.washington.iam.registry.rp;

import edu.washington.iam.registry.exception.RelyingPartyException;
import java.io.Serializable;
import java.util.List;

public interface RelyingPartyManager extends Serializable {

  public List<RelyingParty> getRelyingParties();

  public List<RelyingParty> getRelyingParties(String search, String admin);

  public List<RelyingParty> getRelyingPartyHistoryById(String id) throws RelyingPartyException;

  public List<String> getMetadataIds();

  public List<RelyingPartyEntry> searchRelyingPartyIds(String searchStr, String metadataId);

  public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException;

  public RelyingParty getRelyingPartyById(String id, String mdid) throws RelyingPartyException;

  public int updateRelyingParty(RelyingParty relyingParty, String mdId, String remoteUser)
      throws RelyingPartyException;

  public int removeRelyingParty(String id, String mdId, String remoteUser);

  public RelyingParty genRelyingPartyByLookup(String dns) throws RelyingPartyException;

  public RelyingParty genRelyingPartyByName(String entityId, String dns);

  public RelyingParty genRelyingPartyByCopy(String dns, String entityId);

  public boolean isMetadataEditable(String mdid);

  public void init();

  public void cleanup();
}
