package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** DDL/preflight rehearsals are fully rolled back, on the exclusive synthetic sandbox only. */
class ActiveStayMigrationPostgresIT extends OperationPostgresSupport {
    final String migration;
    ActiveStayMigrationPostgresIT() throws Exception {
        migration=Files.readString(Path.of("src/main/resources/db/sqlFile/incremental/2026/20261103-160000-active-stay-dedup.sql"));
    }
    void removeTechnicalDdl(Connection connection) throws SQLException {
        try(var statement=connection.createStatement()) {
            statement.execute("DROP TRIGGER trg_m1_active_stay_dedup ON public.stay_guests;DROP INDEX public.uq_stay_guest_active_dedup;ALTER TABLE public.stay_guests DROP COLUMN active_dedup_key;DROP FUNCTION public.m1_set_active_stay_key();DROP FUNCTION public.m1_active_stay_key(smallint,bigint,bigint,integer,text,text,timestamptz,timestamptz,boolean);DROP FUNCTION public.m1_normalize_stay_token(text)");
        }
    }
    void insert(Connection connection,short hotel,String document) throws SQLException {
        try(var query=connection.prepareStatement("INSERT INTO stay_guests(hotel_id,booking_room_id,room_number,full_name,document_type,document_number,guest_type,nationality,check_in_time,is_deleted) VALUES(?,1,'101','SYNTHETIC_PREFLIGHT','CCCD',?,'ADULT','Vietnam',CURRENT_TIMESTAMP,false)")) {
            query.setShort(1,hotel);query.setString(2,document);query.executeUpdate();
        }
    }
    @Test void duplicatePreflightAbortsWholeMigrationWithoutExposingIdentity() throws Exception {
        int baseline=jdbc.queryForObject("SELECT count(*) FROM stay_guests",Integer.class);
        String document="SYNTHETIC_PRIVATE_"+UUID.randomUUID().toString().substring(0,8);
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {removeTechnicalDdl(connection);insert(connection,(short)1,document);insert(connection,(short)1,document);
                try(var statement=connection.createStatement()) {
                    assertThatThrownBy(()->statement.execute(migration)).isInstanceOfSatisfying(SQLException.class,e->{
                        assertThat(e.getSQLState()).isEqualTo("23505");assertThat(e.getMessage()).contains("M1_ACTIVE_STAY_DUPLICATES","ids=").doesNotContain(document);
                    });
                }
            }finally{connection.rollback();}
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stay_guests",Integer.class)).isEqualTo(baseline);
        assertThat(jdbc.queryForObject("SELECT to_regclass('public.uq_stay_guest_active_dedup') IS NOT NULL",Boolean.class)).isTrue();
    }
    @Test void invalidActiveLinkPreflightAbortsAndPreservesOriginalSchema() throws Exception {
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {removeTechnicalDdl(connection);insert(connection,(short)2,"SYNTHETIC_BAD_LINK");
                try(var statement=connection.createStatement()) {
                    assertThatThrownBy(()->statement.execute(migration)).isInstanceOfSatisfying(SQLException.class,e->{
                        assertThat(e.getSQLState()).isEqualTo("23514");assertThat(e.getMessage()).contains("M1_ACTIVE_STAY_INVALID_LINK id=").doesNotContain("SYNTHETIC_BAD_LINK");
                    });
                }
            }finally{connection.rollback();}
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stay_guests WHERE full_name='SYNTHETIC_PREFLIGHT'",Integer.class)).isZero();
    }
    String scalar(Connection connection,String sql) throws SQLException {
        try(var statement=connection.createStatement();var result=statement.executeQuery(sql)){result.next();return result.getString(1);}
    }
    @Test void cleanBackfillPreservesExportedRowsTimestampsAndQueueMarkers() throws Exception {
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try(var statement=connection.createStatement()){statement.execute("UPDATE lodging_queues SET status='EXPORTED',batch_reference='SYNTHETIC_BATCH',exported_at=CURRENT_TIMESTAMP WHERE id=1");}
                String guests=scalar(connection,"SELECT jsonb_agg(to_jsonb(g)-'active_dedup_key' ORDER BY id)::text FROM stay_guests g");
                String queues=scalar(connection,"SELECT jsonb_agg(to_jsonb(q) ORDER BY id)::text FROM lodging_queues q");
                removeTechnicalDdl(connection);
                try(var statement=connection.createStatement()){statement.execute(migration);}
                assertThat(scalar(connection,"SELECT jsonb_agg(to_jsonb(g)-'active_dedup_key' ORDER BY id)::text FROM stay_guests g")).isEqualTo(guests);
                assertThat(scalar(connection,"SELECT jsonb_agg(to_jsonb(q) ORDER BY id)::text FROM lodging_queues q")).isEqualTo(queues);
                assertThat(scalar(connection,"SELECT octet_length(active_dedup_key) FROM stay_guests WHERE id=1")).isEqualTo("32");
            }finally{connection.rollback();}
        }
    }
    @Test void deploymentPreflightRequiresValidIndexAndMigrationVersion() throws Exception {
        new vn.edu.utc.hotel_booking.modules.operation.service.support.ActiveStaySchemaVerifier(jdbc).afterPropertiesSet();
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try(var statement=connection.createStatement()){statement.execute("DROP INDEX public.uq_stay_guest_active_dedup");}
                var scoped=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
                assertThatThrownBy(()->new vn.edu.utc.hotel_booking.modules.operation.service.support.ActiveStaySchemaVerifier(scoped).afterPropertiesSet())
                        .isInstanceOf(IllegalStateException.class).hasMessage("Required maintenance migration m1-active-stay-dedup has not been verified");
            }finally{connection.rollback();}
        }
    }

}
