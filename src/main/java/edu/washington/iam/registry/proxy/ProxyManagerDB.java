package edu.washington.iam.registry.proxy;

import edu.washington.iam.registry.exception.NoPermissionException;
import edu.washington.iam.registry.exception.ProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.w3c.dom.Document;

import java.sql.*;
//import org.postgresql.jdbc3.Jdbc3PoolingDataSource;

import java.util.*;

public class ProxyManagerDB implements ProxyManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private JdbcTemplate template;
    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }


    public List<Proxy> getProxys() {
        return template.query("select * from proxy",
                new ProxyMapper());
    }

    public Proxy getProxy(String entityId) {
        List<Proxy> proxies = template.query("select * from proxy where entity_id = ?",
                new Object[] {entityId},
                new ProxyMapper());
        if(proxies.size() != 0)
            return proxies.get(0);
        else
            return null;
    }

    public int removeRelyingParty(String rpid) {
        return 0;
    }

    public void updateProxy(String id, Document doc, String remoteUser) throws ProxyException, NoPermissionException {

    }

    private static final class ProxyMapper implements ResultSetExtractor<List<Proxy>> {
        @Override
        public List<Proxy> extractData(ResultSet rs) throws SQLException, DataAccessException{
            Map<String, List<ProxyIdp>> proxyIdpsMap = new HashMap<String, List<ProxyIdp>>();
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
                    List<ProxyIdp> pIdps = new ArrayList<ProxyIdp>();
                    pIdps.add(pIdp);
                    proxyIdpsMap.put(entityId, pIdps);
                }
            }
            List<Proxy> proxyList = new ArrayList<Proxy>();
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
