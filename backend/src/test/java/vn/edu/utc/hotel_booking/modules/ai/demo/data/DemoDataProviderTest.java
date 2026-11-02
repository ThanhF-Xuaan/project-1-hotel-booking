package vn.edu.utc.hotel_booking.modules.ai.demo.data;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class DemoDataProviderTest {
    @Test void frozenDatasetsHaveCorrectIndependentScopesAndCounts() {
        var source=new FixtureDemoDataProvider();
        source.occupancy("HOTEL_A",d->{assertThat(d.rows()).hasSize(420);assertThat(d.regionId()).isEqualTo(10);return null;});
        source.occupancy("HOTEL_B",d->{assertThat(d.rows()).hasSize(420);assertThat(d.regionId()).isEqualTo(20);return null;});
        source.group("P01",d->{assertThat(d.snapshot().scope().regionId()).isEqualTo(10);return null;});
    }
    @Test void dbPoolIsLazyAndRejectsAnonymousBeforeConnecting() {
        try(var db=new PostgresDemoDataProvider("jdbc:postgresql://127.0.0.1:1/m1_analytics_test","m1_demo_reader_0123456789","synthetic")) {
            SecurityContextHolder.clearContext();
            assertThatThrownBy(()->db.occupancy("HOTEL_A",d->d)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }
    @Test void dbTargetAndRoleMustBeExplicitAndRestricted() {
        assertThatThrownBy(()->new PostgresDemoDataProvider("jdbc:postgresql://remote:5432/hotel_booking_db","postgres","synthetic")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new PostgresDemoDataProvider("jdbc:postgresql://localhost:5433/hotel_booking_db","postgres","synthetic")).isInstanceOf(IllegalStateException.class);
    }
    @Test void malformedOccupancyAndForeignScopeFailClosed() throws Exception {
        var mapper=new ObjectMapper();
        var root=mapper.readTree(Files.readString(Path.of("src/main/resources/ai/demo/occupancy-history-v1.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode)root.path("hotels").get(0)).put("regionId",20);
        assertThatThrownBy(()->FixtureDemoDataProvider.occupancy(root,"HOTEL_A")).isInstanceOf(IllegalStateException.class);
    }
    @Test void malformedGroupAndForeignScopeFailClosed() throws Exception {
        var root=new ObjectMapper().readTree(Files.readString(Path.of("src/main/resources/ai/demo/group-preview-v1.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode)root.path("cases").get(0).path("snapshot").path("scope")).put("hotelId",2);
        assertThatThrownBy(()->FixtureDemoDataProvider.group(root,"P01")).isInstanceOf(IllegalStateException.class);
    }
    @Test void runtimeDbBoundaryHasNoBusinessWritesOrCoreImports() throws Exception {
        String source=Files.readString(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/demo/data/PostgresDemoDataProvider.java"));
        assertThat(source).doesNotContain("modules.booking", "modules.inventory", "modules.operation", "INSERT INTO", "UPDATE ", "DELETE FROM");
        assertThat(source).contains("setReadOnly(true)","TRANSACTION_REPEATABLE_READ","statement.setQueryTimeout(5)","m1_demo.dataset_payload");
    }
}
