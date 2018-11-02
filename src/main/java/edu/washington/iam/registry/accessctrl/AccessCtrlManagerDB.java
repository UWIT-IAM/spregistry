package edu.washington.iam.registry.accessctrl;

import edu.washington.iam.registry.exception.AccessCtrlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.washington.iam.tools.IdpHelper;

public class AccessCtrlManagerDB implements AccessCtrlManager {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private JdbcTemplate template;

    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    private IdpHelper idpHelper = null;
    public void setIdpHelper(IdpHelper v) {
        idpHelper = v;
    }


    public List<AccessCtrl> getAccessCtrlHistory(String entityId) throws AccessCtrlException {
        List<AccessCtrl> AccessCtrlHistory = null;
        try {
            AccessCtrlHistory = template.query(
                    "select * from access_ctrl where end_time is not null and entity_id = ?",
                    new Object[] {entityId},
                    new AccessCtrlMapper());
            return AccessCtrlHistory;
        }
        catch (Exception e){
            String errorMsg = String.format("error getting access control history: %s", entityId);
            log.debug(errorMsg);
            throw new AccessCtrlException(errorMsg);
        }

    }

    public AccessCtrl getAccessCtrl(String entityId) {
        log.debug("looking for access control for " + entityId);
        AccessCtrl accessCtrl = null;

        List<AccessCtrl> accessCtrlList = template.query("select * from access_control where entity_id = ? and end_time is null",
                new Object[] {entityId},
                new AccessCtrlMapper());
        if(accessCtrlList.size() != 0){
            accessCtrl = accessCtrlList.get(0);
        }

        return accessCtrl;
    }

    public void updateAccessCtrl(AccessCtrl accessCtrl, String updatedBy) throws AccessCtrlException {
        log.debug("looking to update access control for " + accessCtrl.getEntityId());

        try {
            List<UUID> uuid = template.queryForList(
                    "select uuid from metadata where entity_id = ? and end_time is null",
                    UUID.class,
                    accessCtrl.getEntityId());
                    accessCtrl.setUuid(uuid.get(0));
            log.info("attempting access control update for " + accessCtrl.getEntityId());
            //recycle "delete" method to mark current record inactive
            removeAccessCtrl(accessCtrl.getEntityId(), updatedBy);
            log.info("Marked current access control record (if any) inactive--adding new one next");
            // add new active record
            log.info(Integer.toString(template.update(
                    "insert into access_control (uuid, entity_id, end_time, start_time, updated_by, auto_2fa," +
                            "auto_2fa_group, conditional, conditional_group) values " +
                            "(? ,?, ?,  now(), ?, ?, ?, ?, ?)",
                    accessCtrl.getUuid(), accessCtrl.getEntityId(), null, updatedBy, accessCtrl.getAuto2FAInternal(),
                    accessCtrl.getGroupAuto2FA(), accessCtrl.getConditional(), accessCtrl.getConditionalGroup())));
            log.debug("updated existing access control for " + accessCtrl.getEntityId());

            if (idpHelper!=null) idpHelper.notifyIdps("accessctrl");
        } catch (Exception e) {
            log.info("update access control trouble: " + e.getMessage());
            // just eat it - don't know the repercussions
        }




    }

    public int removeAccessCtrl(String entityId, String updatedBy) throws AccessCtrlException {
        log.debug("looking to delete access control for " + entityId);

        List<Integer> ids = template.queryForList(
                "select id from access_control where entity_id = ? and end_time is null",
                Integer.class, entityId);
        if (ids.size() == 1 && ids.get(0) != null) {
            template.update("update access_control set end_time = now(), updated_by = ? where id = ?", updatedBy, ids.get(0));
            log.info("updated (delete) access control for %s", entityId);
            if (idpHelper!=null) idpHelper.notifyIdps("accessctrl");
            return 200;
        }
        else if (ids.size() == 0) {
            log.info(String.format("No access control found for %s", entityId));
            return 500;
        }
        else{
            throw new AccessCtrlException("more than one active access control record found!!  No update performed.");
            //TODO what about a return code?
        }
    }


    private static final class AccessCtrlMapper implements ResultSetExtractor<List<AccessCtrl>> {
        @Override
        public List<AccessCtrl> extractData(ResultSet rs) throws SQLException, DataAccessException{
            List<AccessCtrl> accessCtrlList = new ArrayList<>();
                while (rs.next()) {
                    AccessCtrl accessCtrlItem = new AccessCtrl();
                    accessCtrlItem.setEntityId(rs.getString("entity_id"));
                    accessCtrlItem.setUuid((UUID) rs.getObject("uuid"));
                    accessCtrlItem.setAuto2FAInternal(rs.getBoolean("auto_2fa"));
                    accessCtrlItem.setConditional(rs.getBoolean("conditional"));
                    accessCtrlItem.setConditionalGroup(rs.getString("conditional_group"));
                    accessCtrlItem.setGroupAuto2FA(rs.getString("auto_2fa_group"));
                    accessCtrlList.add(accessCtrlItem);
                }
            return accessCtrlList;
        }
    }
}
