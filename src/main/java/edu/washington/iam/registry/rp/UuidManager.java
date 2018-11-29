package edu.washington.iam.registry.rp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

/*
* This class manages UUIDs.
*
* FOr UW SPs, the table of record for UUID is metadata.  For InCommon SPs, the table of record is incommon_sp.
* 
* */
public class UuidManager {

    private JdbcTemplate template;
    public void setTemplate(JdbcTemplate m) { template = m; }


    private final Logger log = LoggerFactory.getLogger(getClass());

    /*Feed an entityid to getUuid and it will get the UUID if one exists, or return
     * a new one if one isn't found.  Works for InCommon (no entry in metadata table) and UW SPs.*/
    public UUID getUuid(String entityId){

        //look in metadata table
        List<UUID> uuid = template.queryForList(
                "select uuid from metadata where entity_id = ? and end_time is null",
                UUID.class,
                entityId);
        if (uuid.size() > 0) {
            return uuid.get(0);
        }
        //the above should hit most of the time...for InCommon SPs it will take some extra work
        else {
            //look in the special InCommon EntityID mapping table
            List<UUID> uuidIc = template.queryForList(
                    "select uuid from incommon_sp where entity_id = ? and active is true",
                    UUID.class,
                    entityId);
            if (uuidIc.size() > 0) {
                return uuidIc.get(0);
            }
            else {
                //we have never done anything in the database with this SP, so we have to create a UUID for it
                UUID newUuid = UUID.randomUUID();
                template.update(
                        "insert into incommon_sp (uuid, entity_id, active) values " +
                                "(? ,?, true)",
                        newUuid, entityId);
                log.debug("added new incommon SP mapping for " + entityId);
                return newUuid;

            }
        }


        }

        /*Given an entity ID, returns whether or not that entityID has a UUID entry in the
         * incommon_sp table */
        public Boolean hasIncUuid(String entityId){

            List<UUID> uuidIc = template.queryForList(
                    "select uuid from incommon_sp where entity_id = ? and active is true",
                    UUID.class,
                    entityId);
            if (uuidIc.size() > 0) {
                return true;
            } else { return false; }

        }





}
