package edu.washington.iam.registry.rp;


import edu.washington.iam.registry.exception.RelyingPartyException;
import edu.washington.iam.tools.XMLHelper;
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
import java.util.*;
import java.sql.Timestamp;

import edu.washington.iam.tools.IdpHelper;

public class DBMetadata implements MetadataDAO {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private String id;
    private String groupId;
    private boolean editable;
    private IdpHelper idpHelper = null;
    public void setIdpHelper(IdpHelper v) {
        idpHelper = v;
    }

    @Autowired
    private JdbcTemplate template;


    @Override
    public List<RelyingParty> getRelyingParties() {
        List<RelyingParty> rps = template.query(
                "select * from metadata where end_time is null and group_id = ?",
                new Object[] {groupId},
                new RelyingPartyMapper());
        return rps;
    }


    @Override
    public List<RelyingParty> getRelyingPartyHistoryById(String id) throws RelyingPartyException {
        // get metadata
        List<RelyingParty> rps = null;
        try {
            rps = template.query(
                    "select * from metadata where group_id = ? and entity_id = ?" +
                    "order by start_time ASC",
                    new Object[]{groupId, id},
                    new RelyingPartyMapper());
            return rps;
        }
        catch (Exception e){
            String errorMsg = String.format("error getting rp: %s, rps size = %s", id, rps.size());
            log.debug(errorMsg);
            throw new RelyingPartyException(errorMsg);
        }


    }


    @Override
    public RelyingParty getRelyingPartyById(String id) throws RelyingPartyException {
        List<RelyingParty> rps = template.query(
                "select * from metadata where end_time is null and group_id = ? and entity_id = ?",
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
    public List<String> searchRelyingPartyIds(String searchStr){
        String sql;
        List<String> results;
        if(searchStr != null) {
            results = template.queryForList(
                    "select entity_id from metadata where end_time is null and group_id = ? and entity_id like ?",
                    new Object[]{groupId, '%' + searchStr + '%'},
                    String.class);
        }
        else {
            results = template.queryForList(
                    "select entity_id from metadata where end_time is null and group_id = ?",
                    new Object[]{groupId},
                    String.class);
        }
        return results;
    }

    @Override
    public void updateRelyingParty(RelyingParty relyingParty) {
        log.info(String.format("updating metadata for rp %s in %s", relyingParty.getEntityId(), groupId));
        try {
            String xml = XMLHelper.serializeXmlToString(relyingParty);
            List<Integer> existingIds = template.queryForList(
                    "select id from metadata where group_id = ? and entity_id = ? and end_time is null",
                    Integer.class,
                    groupId, relyingParty.getEntityId());
            if (existingIds.size() == 0) {
                // no active records so we add an active record
                template.update(
                        "insert into metadata (uuid, group_id, entity_id, xml, end_time, start_time) values " +
                                "(? ,?, ?, ?, ?, now())",
                        genUUID(), groupId, relyingParty.getEntityId(), xml, null);
                        log.debug("added new rp " + relyingParty.getEntityId());
            } else if (existingIds.size() == 1) {
                // active record exists so mark last one inactive
                template.update("update metadata set end_time = now() where id = ?", existingIds.get(0));
                // add new active record
                log.info(Integer.toString(template.update(
                        "insert into metadata (uuid, group_id, entity_id, xml, end_time, start_time) values " +
                                "(?, ?, ?, ?, ?, now())",
                        relyingParty.getUuid(), groupId, relyingParty.getEntityId(), xml, null)));
                log.debug("updated existing rp " + relyingParty.getEntityId());
            } else {
                throw new RelyingPartyException("more than one active metadata record found!!  No update performed.  ");
            }
            if (idpHelper!=null) idpHelper.notifyIdps("metadata");
        } catch (Exception e) {
            log.info("update metadata trouble: " + e.getMessage());
            // just eat it - don't know the repercussions
        }
    }


    @Override
    public void removeRelyingParty(String rpid) {
        try {
            log.info(String.format("looking to remove metadata for rp %s in %s", rpid, groupId));
            List<Integer> rpIds = template.queryForList(
                    "select id from metadata where group_id = ? and entity_id = ? and end_time is null",
                    Integer.class,
                    groupId, rpid);
            if (rpIds.size() == 1 && rpIds.get(0) != null) {
                log.info(Integer.toString(template.update("update metadata set end_time = now() where id = ?",
                        rpIds.get(0))));
                log.info(String.format("updated (delete) %s", rpid));
            }
            else if (rpIds.size() == 0) {
                log.info(String.format("No rp found for %s", rpid));
            }
            else {
                throw new RelyingPartyException("more than one active metadata record found!!  No update performed.  ");
            }


        }
        catch (Exception e) {
            log.info("remove metadata trouble: " + e.getMessage());
        }
    }
    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void cleanup() {

    }

    public void setGroupId(String groupId) { this.groupId = groupId; }
    public void setId(String id) { this.id = id; }
    public void setEditable(boolean editable) { this.editable = editable; }
    private String genUUID() { return UUID.randomUUID().toString(); }


    private class RelyingPartyMapper implements RowMapper<RelyingParty> {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        @Override
        public RelyingParty mapRow(ResultSet resultSet, int i) throws SQLException {

            Document document;
            RelyingParty relyingParty;
            String groupId = resultSet.getString("group_id");
            String entityId = resultSet.getString("entity_id");
            String startTime = resultSet.getString("start_time");
            String endTime = resultSet.getString("end_time");
            String updatedBy = resultSet.getString("updated_by");
            String uuid = resultSet.getString("uuid");

            try {
                DocumentBuilder builder = dbf.newDocumentBuilder();
                document = builder.parse(resultSet.getAsciiStream("xml"));

            } catch (Exception e) {
                return null;
            }

            try {
                relyingParty = new RelyingParty(document.getDocumentElement(), id, true, updatedBy, startTime, endTime, uuid);
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
