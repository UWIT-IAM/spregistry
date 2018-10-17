package edu.washington.iam.registry.accessctrl;

import edu.washington.iam.registry.exception.AccessCtrlException;
import edu.washington.iam.registry.exception.ProxyException;
import edu.washington.iam.registry.proxy.Proxy;
import edu.washington.iam.registry.proxy.ProxyManager;
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
        log.debug("looking for proxy for " + entityId);
        AccessCtrl accessCtrl = null;

        List<AccessCtrl> accessCtrlList = template.query("select * from proxy where entity_id = ? and end_time is null",
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
            List<String> uuid = template.queryForList(
                    "select uuid from metadata where entity_id = ? and end_time is null",
                    String.class,
                    accessCtrl.getEntityId());
                    accessCtrl.setUuid(uuid.get(0));
            log.info("attempting access control update for " + accessCtrl.getEntityId());
            //recycle "delete" method to mark current record inactive
            removeAccessCtrl(accessCtrl.getEntityId(), updatedBy);
            log.info("Marked current proxy record (if any) inactive--adding new one next");
            // add new active record
            log.info(Integer.toString(template.update(
                    "insert into access_control (uuid, entity_id, end_time, start_time, updated_by, auto2fa," +
                            "conditional, conditional_group) values " +
                            "(? ,?, ?,  now(), ?, ?, ?, ?)",
                    accessCtrl.getUuid(), accessCtrl.getEntityId(), null, updatedBy, accessCtrl.getAuto2FA(),
                    accessCtrl.getConditional(), accessCtrl.getConditionalGroup())));
            log.debug("updated existing access control for " + accessCtrl.getEntityId());

            if (idpHelper!=null) idpHelper.notifyIdps("metadata");
        } catch (Exception e) {
            log.info("update access control trouble: " + e.getMessage());
            // just eat it - don't know the repercussions
        }




    }

    public int removeAccessCtrl(String entityId, String updatedBy) throws ProxyException {
        log.debug("looking to delete proxy for " + entityId);

        List<Integer> entityIds = template.queryForList(
                "select id from access_control where entity_id = ? and end_time is null",
                Integer.class, entityId);
        if (entityIds.size() == 1 && entityIds.get(0) != null) {
            template.update("update access_control set end_time = now(), updated_by = ? where id = ?", entityIds.get(0), updatedBy);
            log.debug("updated (delete) access control for %s", entityId);
            return 200;
        }
        else if (entityIds.size() == 0) {
            log.info(String.format("No access control found for %s", entityId));
            return 500;
        }
        else{
            throw new ProxyException("more than one active access control record found!!  No update performed.");
            //TODO what about a return code?
        }
    }
    

    private String genUUID() { return UUID.randomUUID().toString(); }

    private static final class AccessCtrlMapper implements ResultSetExtractor<List<AccessCtrl>> {
        @Override
        public List<AccessCtrl> extractData(ResultSet rs) throws SQLException, DataAccessException{
            List<AccessCtrl> accessCtrlList = new ArrayList<>();
            while(rs.next()){
                AccessCtrl accessCtrlItem = new AccessCtrl();
                accessCtrlItem.setEntityId(rs.getString("entity_id"));
                accessCtrlItem.setUuid(rs.getString("uuid"));
                accessCtrlItem.setAuto2FA(rs.getBoolean("auto2fa"));
                accessCtrlItem.setConditional(rs.getBoolean("conditional"));
                accessCtrlItem.setConditionalGroup(rs.getString("conditional_group"));
                accessCtrlList.add(accessCtrlItem);
            }

            return accessCtrlList;
        }
    }
}
