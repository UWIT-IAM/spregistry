package edu.washington.iam.registry.rp;

import edu.washington.iam.registry.exception.RelyingPartyException;

import java.util.List;

public interface MetadataDAO {
    public List<RelyingParty> getRelyingParties();
    public List<RelyingParty> getRelyingPartyHistoryById(String id) throws RelyingPartyException;
    public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException;
    public List<String> searchRelyingPartyIds(String searchStr);
    public List<RelyingParty> getRelyingPartiesById(String searchStr);
    public List<RelyingParty> getRelyingPartiesByAdmin(String admin);
    public void updateRelyingParty(RelyingParty rp, String updatedBy);
    public void removeRelyingParty(String rpid, String updatedBy);
    public boolean isEditable();
    public void cleanup();
}
