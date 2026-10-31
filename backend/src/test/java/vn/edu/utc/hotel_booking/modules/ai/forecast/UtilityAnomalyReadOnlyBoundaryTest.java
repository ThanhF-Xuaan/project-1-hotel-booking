package vn.edu.utc.hotel_booking.modules.ai.forecast;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class UtilityAnomalyReadOnlyBoundaryTest {
    @Test
    void utilitySliceHasNoCoreWritePersistenceNetworkOrAutomaticRuntimeWiring() throws Exception {
        List<Path> paths;
        try (var files = Files.walk(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast"))) {
            paths = files.filter(p -> p.getFileName().toString().startsWith("Utility") && p.toString().endsWith(".java")).toList();
        }
        assertThat(paths).hasSize(5);
        Pattern imports = Pattern.compile("^import ([^;]+);", Pattern.MULTILINE);
        for (Path path : paths) {
            String source = Files.readString(path); var matcher = imports.matcher(source);
            while (matcher.find()) assertThat(matcher.group(1)).matches("(?:java\\.(?:time|util|math)\\..*|vn\\.edu\\.utc\\.hotel_booking\\.modules\\.ai\\.forecast\\..*)");
            assertThat(source).doesNotContain("org.springframework.", "jakarta.persistence.", "java.sql.", "javax.sql.", "java.net.", "java.io.", "java.nio.file.",
                    "modules.operation.", "modules.booking.", "modules.pricing.", "@Service", "@Bean", "@RestController", "utility-readings-42-days.csv", "System.out", "System.err");
        }
        assertThat(Arrays.stream(UtilityReadingHistoryPort.class.getDeclaredMethods()).map(m -> m.getName()).toList()).containsExactly("read");
    }
}
