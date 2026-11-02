package vn.edu.utc.hotel_booking.modules.ai.demo.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Connection;
import java.time.Instant;
import java.util.function.Function;

/** Separate lazy pool, restricted role and one read-only repeatable-read snapshot per replay. */
public final class PostgresDemoDataProvider implements DemoDataProvider {
    private final HikariDataSource pool;
    // A trusted version oracle validates DB bytes; all response data still comes from the DB.
    private final FixtureDemoDataProvider versionOracle = new FixtureDemoDataProvider();
    public PostgresDemoDataProvider(String url,String user,String password) {
        if(url==null || !url.matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]{1,5}/(?:hotel_booking_db|hotel_acceptance_db|m1_analytics_test)")
                || user==null || !user.matches("m1_demo_reader_[a-f0-9]{10}") || password==null || password.isBlank()) throw FixtureDemoDataProvider.unavailable();
        var config=new HikariConfig(); config.setJdbcUrl(url); config.setUsername(user); config.setPassword(password);
        config.setMaximumPoolSize(2); config.setMinimumIdle(0); config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(5000); config.setReadOnly(true); config.setPoolName("m1-demo-readonly");
        config.addDataSourceProperty("connectTimeout","5"); config.addDataSourceProperty("socketTimeout","5");
        pool=new HikariDataSource(config);
    }
    private static void requireAdmin() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a->"ROLE_CHAIN_ADMIN".equals(a.getAuthority()))) throw new AccessDeniedException("Demo requires chain administrator");
    }
    private <T> T read(String key,String hash,Instant reference,Function<com.fasterxml.jackson.databind.JsonNode,T> action) {
        requireAdmin();
        try(var connection=pool.getConnection()) {
            connection.setReadOnly(true); connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); connection.setAutoCommit(false);
            try {
                try(var guard=connection.createStatement()) {
                    guard.setQueryTimeout(5);
                    try(var check=guard.executeQuery("SELECT NOT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolinherit), NOT has_schema_privilege(current_user,'m1_demo','CREATE') AND NOT EXISTS (SELECT 1 FROM information_schema.tables t WHERE (t.table_schema='public' AND has_table_privilege(current_user,quote_ident(t.table_schema)||'.'||quote_ident(t.table_name),'SELECT,INSERT,UPDATE,DELETE')) OR (t.table_schema='m1_demo' AND has_table_privilege(current_user,quote_ident(t.table_schema)||'.'||quote_ident(t.table_name),'INSERT,UPDATE,DELETE'))) FROM pg_roles WHERE rolname=current_user")) {
                        if(!check.next() || !check.getBoolean(1) || !check.getBoolean(2)) throw FixtureDemoDataProvider.unavailable();
                    }
                }
                try(var statement=connection.prepareStatement("SELECT version,data_mode,reference_instant,source_sha256,state,content_digest,encode(sha256(convert_to(m1_demo.dataset_payload(dataset_key)::text,'UTF8')),'hex'),m1_demo.dataset_payload(dataset_key)::text FROM m1_demo.datasets WHERE dataset_key=?")) {
                    statement.setQueryTimeout(5); statement.setString(1,key);
                    try(var result=statement.executeQuery()) {
                        if(!result.next() || result.getInt(1)!=1 || !"SYNTHETIC".equals(result.getString(2))
                                || !reference.equals(result.getTimestamp(3).toInstant()) || !hash.equals(result.getString(4)) || !"READY".equals(result.getString(5))
                                || !result.getString(6).equals(result.getString(7))) throw FixtureDemoDataProvider.unavailable();
                        String raw=result.getString(8);
                        if(raw==null || raw.length()>1_000_000) throw FixtureDemoDataProvider.unavailable();
                        var payload=new ObjectMapper().readTree(raw);
                        if(!payload.equals(versionOracle.expectedPayload(key))) throw FixtureDemoDataProvider.unavailable();
                        requireAdmin(); T value=action.apply(payload); requireAdmin(); connection.commit(); return value;
                    }
                }
            } catch(Exception e) { connection.rollback(); if(e instanceof AccessDeniedException denied) throw denied; throw FixtureDemoDataProvider.unavailable(); }
        } catch(AccessDeniedException e) { throw e; } catch(Exception e) { throw FixtureDemoDataProvider.unavailable(); }
    }
    public <T> T occupancy(String scenario,Function<OccupancyData,T> action) {
        return read("occupancy-v1",FixtureDemoDataProvider.OCCUPANCY_HASH,FixtureDemoDataProvider.OCCUPANCY_REFERENCE,node->action.apply(FixtureDemoDataProvider.occupancy(node,scenario)));
    }
    public <T> T group(String id,Function<GroupData,T> action) {
        return read("group-v1",FixtureDemoDataProvider.GROUP_HASH,FixtureDemoDataProvider.GROUP_REFERENCE,node->action.apply(FixtureDemoDataProvider.group(node,id)));
    }
    @Override public void close() { pool.close(); }
}
