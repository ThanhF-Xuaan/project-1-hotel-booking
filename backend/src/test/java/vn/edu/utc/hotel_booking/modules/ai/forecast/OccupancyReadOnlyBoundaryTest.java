package vn.edu.utc.hotel_booking.modules.ai.forecast;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class OccupancyReadOnlyBoundaryTest {
    @Test
    void historySliceHasNoCorePersistenceNetworkOrAutomaticProductionWiring() throws Exception {
        Path root = Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast");
        Pattern imports = Pattern.compile("^import ([^;]+);", Pattern.MULTILINE);
        List<Path> sources;
        try (var files = Files.walk(root)) {
            sources = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertThat(sources).isNotEmpty();
        for (Path source : sources) {
            String text = Files.readString(source);
            var matcher = imports.matcher(text);
            while (matcher.find()) {
                assertThat(matcher.group(1)).as("Dependency in %s", source)
                        .matches("(?:java\\.(?:time|util|math)\\..*|vn\\.edu\\.utc\\.hotel_booking\\.modules\\.ai\\.forecast\\..*)");
            }
            assertThat(text).as("Production boundary in %s", source).doesNotContain(
                    "org.springframework.", "jakarta.persistence.", "java.sql.", "javax.sql.", "java.net.",
                    "java.nio.file.", "java.io.", "modules.booking.", "modules.inventory.", "modules.pricing.",
                    "getResourceAsStream(", "occupancy-history-420-days.json", "System.out", "System.err"
            );
        }
    }
}
