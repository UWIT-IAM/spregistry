package edu.washington.iam.registry.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyManagerDB implements ProxyManager {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private JdbcTemplate template;

    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public List<Proxy> getProxys() {
        log.debug("getting the list of proxies");
        return template.query("select * from proxy",
                new ProxyMapper());
    }

    public Proxy getProxy(String entityId) {
        log.debug("looking for proxy for " + entityId);
        Proxy proxy = null;

        List<Proxy> proxies = template.query("select * from proxy where entity_id = ?",
                new Object[] {entityId},
                new ProxyMapper());
        if(proxies.size() != 0){
            proxy = proxies.get(0);
        }

        return proxy;
    }

    public int removeRelyingParty(String rpid) {
        log.debug("looking to delete proxy for " + rpid);
        template.update("delete from proxy where entity_id = ?", rpid);
        return 200;
    }

    public void updateProxy(Proxy proxy) {
        // TODO: Right now we just delete all proxy data and reinsert it. A smarter implementation would check
        //       for differences
        log.info("proxy update " + proxy.getEntityId());
        template.update("delete from proxy where entity_id = ?", proxy.getEntityId());
        for(ProxyIdp proxyIdp : proxy.getProxyIdps()){
            template.update("insert into proxy (entity_id, social_provider, social_key, social_secret, status, update_time) "
                    + "values (?, ?, ?, ?, ?, now())",
                    proxy.getEntityId(),
                    proxyIdp.getIdp(),
                    proxyIdp.getClientId(),
                    proxyIdp.getClientSecret(),
                    1);
        }
    }

    private static final class ProxyMapper implements ResultSetExtractor<List<Proxy>> {
        @Override
        public List<Proxy> extractData(ResultSet rs) throws SQLException, DataAccessException{
            Map<String, List<ProxyIdp>> proxyIdpsMap = new HashMap<>();
            while(rs.next()){
                String entityId = rs.getString("entity_id");
                ProxyIdp pIdp = new ProxyIdp();
                pIdp.setIdp(rs.getString("social_provider"));
                pIdp.setClientSecret(rs.getString("social_secret"));
                pIdp.setClientId(rs.getString("social_key"));
                if(proxyIdpsMap.containsKey(entityId)){
                    proxyIdpsMap.get(entityId).add(pIdp);
                }
                else{
                    List<ProxyIdp> pIdps = new ArrayList<>();
                    pIdps.add(pIdp);
                    proxyIdpsMap.put(entityId, pIdps);
                }
            }
            List<Proxy> proxyList = new ArrayList<>();
            for(Map.Entry<String, List<ProxyIdp>> entry : proxyIdpsMap.entrySet()){
                Proxy proxy = new Proxy();
                proxy.setEntityId(entry.getKey());
                proxy.setProxyIdps(entry.getValue());
                proxyList.add(proxy);
            }
            return proxyList;
        }
    }
}
