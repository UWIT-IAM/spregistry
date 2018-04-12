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
        template.update("insert into proxy (entity_id, status, update_time) "
                    + "values (?, ?, now())",
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
                proxy.setSocialActive(rs.getBoolean("status"));
                proxyList.add(proxy);
            }

            return proxyList;
        }
    }
}
