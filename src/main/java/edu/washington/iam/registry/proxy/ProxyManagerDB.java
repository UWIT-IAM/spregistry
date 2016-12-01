package edu.washington.iam.registry.proxy;

import edu.washington.iam.registry.exception.ProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProxyManagerDB implements ProxyManager {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private JdbcTemplate template;

    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

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

    public int removeProxy(String rpid) throws ProxyException {
        log.debug("looking to delete proxy for " + rpid);

        List<Integer> rpIds = template.queryForList(
                "select id from proxy where entity_id = ? and end_time is null",
                Integer.class, rpid);
        if (rpIds.size() == 1 && rpIds.get(0) != null) {
            template.update("update proxy set end_time = now() where id = ?", rpIds.get(0));
            log.debug("updated (delete) proxy for %s", rpid);
            return 200;
        }
        else if (rpIds.size() == 0) {
            log.info(String.format("No proxy found for %s", rpid));
            return 500;
        }
        else{
            throw new ProxyException("more than one active proxy record found!!  No update performed.");
            //TODO what about a return code?
        }
    }

    //add or update a proxy
    public void updateProxy(Proxy proxy) throws ProxyException {
        // TODO: Right now we just delete all proxy data and reinsert it. A smarter implementation would check
        //       for differences
        List<String> uuid = template.queryForList("select uuid from metadata where entity_id = ? and end_time is null",
        String.class,
        proxy.getEntityId());
        proxy.setUuid(uuid.get(0));
        log.info("attempting proxy update " + proxy.getEntityId());
        //recycle "delete" method to mark current record inactive
        removeProxy(proxy.getEntityId());
        log.info("Marked current proxy record inactive--adding new one next");
            template.update("insert into proxy (uuid, entity_id, status, start_time, end_time) "
                    + "values (?, ?, ?, now(), null)",
                    proxy.getUuid(),
                    proxy.getEntityId(),
                    (proxy.getSocialActive()) ? 1 : 0);

    }

    private static final class ProxyMapper implements ResultSetExtractor<List<Proxy>> {
        @Override
        public List<Proxy> extractData(ResultSet rs) throws SQLException, DataAccessException{
            List<Proxy> proxyList = new ArrayList<>();
            while(rs.next()){
                Proxy proxy = new Proxy();
                proxy.setEntityId(rs.getString("entity_id"));
                proxy.setUuid(rs.getString("uuid"));
                proxy.setSocialActive(rs.getBoolean("status"));
                proxyList.add(proxy);
            }

            return proxyList;
        }
    }
}
