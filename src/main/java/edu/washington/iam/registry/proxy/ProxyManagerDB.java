package edu.washington.iam.registry.proxy;

import edu.washington.iam.registry.exception.ProxyException;
import edu.washington.iam.registry.rp.UuidManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProxyManagerDB implements ProxyManager {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private JdbcTemplate template;

    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    @Autowired
    private UuidManager uuidManager;

    public List<Proxy> getProxys() {
        log.debug("getting the list of proxies");
        return template.query("select * from proxy where end_time is null",
                new ProxyMapper());
    }

    public List<Proxy> getProxyHistory(String entityId) throws ProxyException {
        List<Proxy> proxys = null;
        try {
            proxys = template.query(
                    "select * from proxy where end_time is not null and entity_id = ?",
                    new Object[] {entityId},
                    new ProxyMapper());
            return proxys;
        }
        catch (Exception e){
            String errorMsg = String.format("error getting proxy history: %s", entityId);
            log.debug(errorMsg);
            throw new ProxyException(errorMsg);
        }

    }

    public Proxy getProxy(String entityId) {
        log.debug("looking for proxy for " + entityId);
        Proxy proxy = null;

        List<Proxy> proxies = template.query("select * from proxy where entity_id = ? and end_time is null",
                new Object[] {entityId},
                new ProxyMapper());
        if(proxies.size() != 0){
            proxy = proxies.get(0);
        }

        return proxy;
    }

    public int removeProxy(String rpid, String updatedBy) throws ProxyException {
        log.debug("looking to delete proxy for " + rpid);

        List<Integer> rpIds = template.queryForList(
                "select id from proxy where entity_id = ? and end_time is null",
                Integer.class, rpid);
        if (rpIds.size() == 1 && rpIds.get(0) != null) {
            template.update("update proxy set end_time = now(), updated_by = ?, status = ? where _id = ?", updatedBy, 0, rpIds.get(0));
            log.debug("updated (delete) proxy for %s", rpid);
            return 200;
        }
        else if (rpIds.size() == 0) {
            //there is no record with end_time = null if social gateway wasn't enabled
            log.info(String.format("No proxy found for %s (usually not an error--was inactive)", rpid));
            //if there are no records with end_time = null then there are no active records to remove
            //and everything is fine.  mattjm 2018-10-23
            return 200;
        }
        else{
            throw new ProxyException("more than one active proxy record found!!  No update performed.");
            //TODO what about a return code?
        }
    }

    //add or update a proxy
    public void updateProxy(Proxy proxy, String updatedBy) throws ProxyException {

        proxy.setUuid(uuidManager.getUuid(proxy.getEntityId()));
        log.info("attempting proxy update " + proxy.getEntityId());
        //recycle "delete" method to mark current record inactive
        removeProxy(proxy.getEntityId(), updatedBy);
        //only add a record with end_time=null if social gateway should be active.
        if (proxy.getSocialActive()) {
            log.info("Marked current proxy record inactive--adding new one next");
            template.update("insert into proxy (uuid, entity_id, start_time, end_time, updated_by, status) "
                            + "values (?, ?, now(), null, ?, 1)",
                    proxy.getUuid(),
                    proxy.getEntityId(),
                    updatedBy);

        }
    }

    private static final class ProxyMapper implements ResultSetExtractor<List<Proxy>> {
        @Override
        public List<Proxy> extractData(ResultSet rs) throws SQLException, DataAccessException{
            List<Proxy> proxyList = new ArrayList<>();
            while(rs.next()){
                Proxy proxy = new Proxy();
                proxy.setEntityId(rs.getString("entity_id"));
                proxy.setUuid((UUID)rs.getObject("uuid"));
                proxy.setSocialActive(true);
                proxy.setUpdatedBy(rs.getString("updated_by"));
                proxy.setStartTime(rs.getString("start_time"));
                proxy.setEndTime(rs.getString("end_time"));
                proxyList.add(proxy);
            }

            return proxyList;
        }
    }
}
