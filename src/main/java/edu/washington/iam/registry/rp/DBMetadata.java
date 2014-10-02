package edu.washington.iam.registry.rp;

import edu.washington.iam.registry.exception.RelyingPartyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBMetadata implements MetadataDAO {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private String id;
    private String groupId;
    private boolean editable;

    @Autowired
    private JdbcTemplate template;

    @Override
    public List<RelyingParty> addSelectRelyingParties(String sel) {
        List<RelyingParty> rps = template.query(
                "select * from metadata where status = 1 and group_id = ? and entity_id like ?",
                new Object[] { groupId, '%' + sel + '%'},
                new RelyingPartyMapper()
        );
        List<RelyingParty> rpsNoNulls = new ArrayList<>();
        for(RelyingParty relyingParty : rps){
            if(relyingParty != null){
                rpsNoNulls.add(relyingParty);
            }
        }
        return rpsNoNulls;
    }

    @Override
    public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException {
        List<RelyingParty> rps = template.query(
                "select * from metadata where status = 1 and group_id = ? and entity_id = ?",
                new Object[] { groupId, id},
                new RelyingPartyMapper());
        if(rps.size() == 1 && rps.get(0) != null){
            return rps.get(0);
        }
        else {
            String errorMsg = String.format("error getting rp: %s, rps size = %s", id, rps.size());
            log.debug(errorMsg);
            throw new RelyingPartyException(errorMsg);
        }
    }

    @Override
    public List<String> getRelyingPartyIds() {
        List<String> ids = template.queryForList("select entity_id from metadata where status = 1 and group_id = ?",
                String.class,
                new Object[]{groupId});
        return ids;
    }

    @Override
    public void updateRelyingParty(RelyingParty rp) {

    }

    @Override
    public void removeRelyingParty(String rpid) {
        template.update("update metadata set status = 0, update_time = now() where entity_id = ?",
                new Object[] {rpid});
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void cleanup() {

    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    public void setId(String id) { this.id = id; }
    public void setEditable(boolean editable) { this.editable = editable; }

    private class RelyingPartyMapper implements RowMapper<RelyingParty> {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        @Override
        public RelyingParty mapRow(ResultSet resultSet, int i) throws SQLException {

            Document document;
            RelyingParty relyingParty;
            String groupId = resultSet.getString("group_id");
            String entityId = resultSet.getString("entity_id");

            try {
                DocumentBuilder builder = dbf.newDocumentBuilder();
                document = builder.parse(resultSet.getAsciiStream("xml"));

            } catch (Exception e) {
                return null;
            }

            try {
                relyingParty = new RelyingParty(document.getDocumentElement(), id, true);
            } catch (RelyingPartyException ex) {
                log.debug(String.format("exception for new Relying Party group_id: %s entity_id: %s message: %s",
                        groupId, entityId, ex.getMessage()));
                relyingParty = null;
            }

            if (relyingParty == null) {
                log.info(String.format("unparseable attribute filter for entity: %s in group: %s",
                        entityId, groupId));
            }

            return relyingParty;
        }
    }
}
