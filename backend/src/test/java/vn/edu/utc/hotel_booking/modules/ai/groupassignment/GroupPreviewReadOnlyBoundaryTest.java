package vn.edu.utc.hotel_booking.modules.ai.groupassignment;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class GroupPreviewReadOnlyBoundaryTest {
    @Test
    void everyProductionSourceHasOnlyPureLocalDependenciesAndNoAutomaticWiring() throws Exception {
        var root = Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/groupassignment");
        List<Path> paths;
        try (var stream = Files.walk(root)) { paths = stream.filter(p -> p.toString().endsWith(".java")).toList(); }
        assertThat(paths).hasSize(9);
        var imports = Pattern.compile("^import ([^;]+);", Pattern.MULTILINE);
        for (Path path : paths) {
            String source = Files.readString(path);
            var matcher = imports.matcher(source);
            while (matcher.find()) assertThat(matcher.group(1)).as("Dependency in %s", path)
                    .matches("(?:java\\.(?:time|util|math)\\..*|vn\\.edu\\.utc\\.hotel_booking\\.modules\\.ai\\.groupassignment\\..*)");
            assertThat(source).doesNotContain("org.springframework.", "jakarta.persistence.", "java.sql.", "javax.sql.",
                    "java.net.", "java.nio.file.", "java.io.", "modules.booking.", "modules.inventory.", "modules.payment.",
                    "@Service", "@Bean", "@RestController", "preview-cases.json", "System.out", "System.err", "ProcessBuilder", "Runtime.getRuntime");
        }
        assertThat(Arrays.stream(GroupAssignmentDataPort.class.getDeclaredMethods()).map(method -> method.getName()).toList()).containsExactly("read");
    }
}
