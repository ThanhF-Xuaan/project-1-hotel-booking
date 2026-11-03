package vn.edu.utc.hotel_booking.modules.ai.demo.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.demo.forecast.OccupancyDemoService;
import vn.edu.utc.hotel_booking.modules.ai.demo.group.GroupDemoService;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

/** Explicit opt-in on a separate disposable DB. Never skipped when selected. */
class PostgresDemoDataProviderIT {
    private String env(String key) { String v=System.getenv(key);assertThat(v).as(key+" required").isNotBlank();return v; }
    private String url() { String v=env("M1_DEMO_DB_URL");assertThat(v).endsWith("/m1_analytics_test");return v; }
    private PostgresDemoDataProvider provider() { admin();return new PostgresDemoDataProvider(url(),env("M1_DEMO_DB_USER"),env("M1_DEMO_DB_PASSWORD")); }
    private void admin() { SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("synthetic",null,"ROLE_CHAIN_ADMIN")); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void write(String sql) throws Exception {
        try(var c=DriverManager.getConnection(url(),env("M1_DEMO_IT_USER"),env("M1_DEMO_IT_PASSWORD"));var s=c.createStatement()){s.execute(sql);}
    }
    @Test void dbAndFixtureOutputsMatchAllNineScenarios() {
        try(var db=provider()) {
            var fixture=new FixtureDemoDataProvider();
            for(String scenario:new String[]{"HOTEL_A","HOTEL_B"})
                assertThat(new OccupancyDemoService(db).replay(scenario)).usingRecursiveComparison().isEqualTo(new OccupancyDemoService(fixture).replay(scenario));
            for(String scenario:new String[]{"FIT_AND_TIE","QUOTA_ONE","MIDDLE_NIGHT_LIMIT","ZERO_QUOTA","QUOTA_CONTROL","NO_TRANSFER_BLOCK","NO_TRANSFER_ROOM_TYPE"})
                assertThat(new GroupDemoService(db).replay(scenario)).usingRecursiveComparison().isEqualTo(new GroupDemoService(fixture).replay(scenario));
        }
    }
    @Test void readonlyRoleCannotWriteEvenWithClientReadonlyDisabledOrReadCore() throws Exception {
        write("CREATE TABLE IF NOT EXISTS public.analytics_core_sentinel(id int)");
        try(var c=DriverManager.getConnection(url(),env("M1_DEMO_DB_USER"),env("M1_DEMO_DB_PASSWORD"))) {
            c.setReadOnly(false);
            for(String sql:new String[]{"UPDATE m1_demo.datasets SET version=1", "SELECT * FROM public.analytics_core_sentinel"})
                assertThatThrownBy(()->{try(var s=c.createStatement()){s.execute(sql);}}).isInstanceOf(java.sql.SQLException.class).extracting(e->((java.sql.SQLException)e).getSQLState()).isEqualTo("42501");
        }
    }
    @Test void stagedMissingAndTamperedDatasetFailWithoutFallback() throws Exception {
        try(var db=provider()) {
            try {
                write("UPDATE m1_demo.datasets SET state='STAGING' WHERE dataset_key='occupancy-v1'");
                assertThatThrownBy(()->new OccupancyDemoService(db).replay("HOTEL_A")).isInstanceOf(IllegalStateException.class).hasMessage("Synthetic demo data unavailable");
            } finally { write("UPDATE m1_demo.datasets SET state='READY' WHERE dataset_key='occupancy-v1'"); }
            try {
                write("UPDATE m1_demo.group_quotas SET room_count=room_count+1 WHERE dataset_key='group-v1'");
                assertThatThrownBy(()->new GroupDemoService(db).replay("FIT_AND_TIE")).isInstanceOf(IllegalStateException.class);
                write("UPDATE m1_demo.datasets SET content_digest=encode(sha256(convert_to(m1_demo.dataset_payload(dataset_key)::text,'UTF8')),'hex') WHERE dataset_key='group-v1'");
                assertThatThrownBy(()->new GroupDemoService(db).replay("FIT_AND_TIE")).isInstanceOf(IllegalStateException.class);
            } finally {
                write("UPDATE m1_demo.group_quotas SET room_count=room_count-1 WHERE dataset_key='group-v1'");
                write("UPDATE m1_demo.datasets SET content_digest=encode(sha256(convert_to(m1_demo.dataset_payload(dataset_key)::text,'UTF8')),'hex') WHERE dataset_key='group-v1'");
            }
        }
    }
    @Test void authorizationRevokedDuringReplayIsDenied() {
        try(var db=provider()) {
            assertThatThrownBy(()->db.occupancy("HOTEL_A",d->{SecurityContextHolder.clearContext();return d;})).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }
    @Test void actualDatabaseFailureIs503AtBothHttpBoundaries() throws Exception {
        try(var db=provider()) {
            var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new vn.edu.utc.hotel_booking.modules.ai.demo.forecast.OccupancyDemoController(new OccupancyDemoService(db)),
                new vn.edu.utc.hotel_booking.modules.ai.demo.group.GroupDemoController(new GroupDemoService(db))).build();
            try {
                write("UPDATE m1_demo.datasets SET state='STAGING'");
                for(String route:new String[]{"/api/v1/ai/demo/occupancy","/api/v1/ai/demo/group-room-assignment"})
                    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(route))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isServiceUnavailable())
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Dịch vụ demo tạm thời chưa sẵn sàng."));
            } finally { write("UPDATE m1_demo.datasets SET state='READY'"); }
        }
    }
    @Test void dbUnavailableIsSanitizedAndDoesNotUseFixture() {
        admin();
        try(var db=new PostgresDemoDataProvider("jdbc:postgresql://127.0.0.1:1/m1_analytics_test",env("M1_DEMO_DB_USER"),env("M1_DEMO_DB_PASSWORD"))) {
            assertThatThrownBy(()->db.occupancy("HOTEL_A",d->d)).isInstanceOf(IllegalStateException.class).hasMessage("Synthetic demo data unavailable");
        }
    }
}
