package edu.washington.iam.registry.rp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

public class UuidManager {

    @Autowired
    JdbcTemplate template;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public UUID GetUuid (String entityId){

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
            if (uuid.size() > 0) {
                return uuid.get(0);
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



}
