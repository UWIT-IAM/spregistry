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

import java.util.List;
import java.util.Vector;

public class ProxyManagerDB implements ProxyManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    private JdbcTemplate template;

    public List<Proxy> getProxys() {
        return null;
    }

    public Proxy getProxy(String entityId) {
        return template.query("SELECT * FROM proxy "
                    + "WHERE entity_id = 'https://jpf.cac.washington.edu/shibboleth' ;",
                new ResultSetExtractor<Proxy>() {
                    @Override
                    public Proxy extractData(ResultSet rs) throws SQLException, DataAccessException {
                        Proxy p = null;
                        List<ProxyIdp> pIdps = new Vector<ProxyIdp>();
                        while(rs.next()){
                            if(p == null){
                                p = new Proxy();
                                p.setEntityId(rs.getString("entity_id"));
                            }
                            ProxyIdp pIdp = new ProxyIdp();
                            pIdp.setIdp(rs.getString("social_provider"));
                            pIdp.setClientSecret(rs.getString("social_secret"));
                            pIdp.setClientId(rs.getString("social_key"));
                            pIdps.add(pIdp);
                        }
                        p.setProxyIdps(pIdps);
                        return p;
                    }
                });
    }

    public int removeRelyingParty(String rpid) {
        return 0;
    }

    public void updateProxy(String id, Document doc, String remoteUser) throws ProxyException, NoPermissionException {

    }
}
