package edu.washington.iam.registry.rp;

import edu.washington.iam.registry.exception.RelyingPartyException;

import java.util.List;

public interface MetadataDAO {
    public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException;
    public List<String> searchRelyingPartyIds(String searchStr);
    public void updateRelyingParty(RelyingParty rp);
    public void removeRelyingParty(String rpid);
    public boolean isEditable();
    public void cleanup();
}
