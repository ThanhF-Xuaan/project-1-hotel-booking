package vn.edu.utc.hotel_booking.modules.operation.service.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Deployment must explicitly apply the maintenance migration before starting this application version. */
@Component
public class ActiveStaySchemaVerifier implements InitializingBean {
    private final JdbcTemplate jdbc;
    public ActiveStaySchemaVerifier(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void afterPropertiesSet() {
        Boolean ready=jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM public.databasechangelog WHERE id='20261103-160000-active-stay-dedup')
              AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='stay_guests'
                  AND column_name='active_dedup_key' AND data_type='bytea')
              AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='stay_guests'
                  AND column_name='booking_room_id' AND is_nullable='YES')
              AND EXISTS(SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('public.uq_stay_guest_active_dedup')
                  AND indisvalid AND indisunique)
              AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid='public.stay_guests'::regclass
                  AND tgname='trg_m1_active_stay_dedup' AND tgenabled='O')
              AND to_regprocedure('public.m1_active_stay_key(smallint,bigint,bigint,integer,text,text,timestamptz,timestamptz,boolean)') IS NOT NULL
            """,Boolean.class);
        if(!Boolean.TRUE.equals(ready)) throw new IllegalStateException("Required maintenance migration m1-active-stay-dedup has not been verified");
    }
}
