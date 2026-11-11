package vn.edu.utc.hotel_booking.modules.ai.advisor.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.controller.PublicUnifiedAdvisorController;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomComparisonService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.UnifiedAdvisorService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class T12UnifiedAdvisorZeroWriteArchitectureTest {

    private static final List<String> FORBIDDEN_WORDS = List.of(
            "Booking", "Payment", "Invoice", "Hold", "Release", "Cancel", "Refund",
            "Repository", "EntityManager", "JdbcTemplate"
    );

    @Test
    @DisplayName("PublicUnifiedAdvisorController has no write, payment, or repository dependencies")
    void controllerHasNoWriteDependencies() {
        Field[] fields = PublicUnifiedAdvisorController.class.getDeclaredFields();
        for (Field f : fields) {
            String typeName = f.getType().getSimpleName();
            for (String forbidden : FORBIDDEN_WORDS) {
                assertThat(typeName)
                        .as("PublicUnifiedAdvisorController should not depend on " + forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    @Test
    @DisplayName("UnifiedAdvisorService has no write, payment, or repository dependencies")
    void serviceHasNoWriteDependencies() {
        Field[] fields = UnifiedAdvisorService.class.getDeclaredFields();
        for (Field f : fields) {
            String typeName = f.getType().getSimpleName();
            for (String forbidden : FORBIDDEN_WORDS) {
                assertThat(typeName)
                        .as("UnifiedAdvisorService should not depend on " + forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    @Test
    @DisplayName("RoomComparisonService has no write, payment, or repository dependencies")
    void comparisonServiceHasNoWriteDependencies() {
        Field[] fields = RoomComparisonService.class.getDeclaredFields();
        for (Field f : fields) {
            String typeName = f.getType().getSimpleName();
            for (String forbidden : FORBIDDEN_WORDS) {
                assertThat(typeName)
                        .as("RoomComparisonService should not depend on " + forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    @Test
    @DisplayName("PublicUnifiedAdvisorController only has converse POST endpoint")
    void controllerOnlyExposesConverseEndpoint() {
        List<String> postMethods = Arrays.stream(PublicUnifiedAdvisorController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class))
                .map(Method::getName)
                .toList();

        assertThat(postMethods).containsExactly("converse");
    }
}
