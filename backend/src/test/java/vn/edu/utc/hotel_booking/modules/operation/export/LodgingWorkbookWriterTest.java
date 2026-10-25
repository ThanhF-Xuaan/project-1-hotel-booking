package vn.edu.utc.hotel_booking.modules.operation.export;

import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.TempFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Isolated("Uses the global POI temporary-file strategy")
class LodgingWorkbookWriterTest {
    @TempDir Path temporaryDirectory;
    private final LodgingWorkbookWriter writer = new LodgingWorkbookWriter();

    @BeforeEach
    void useIsolatedTemporaryFiles() {
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(temporaryDirectory.toFile()));
    }

    @AfterEach
    void restorePoiDefaultStrategy() {
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy());
    }

    @Test
    void successfulStreamingExportRemovesTemporaryFiles() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writer.write(syntheticRows(), true, output);
            assertThat(output.size()).isGreaterThan(1024);
            assertThat(output.toByteArray()[0]).isEqualTo((byte) 'P');
        }
        assertNoTemporaryFiles();
    }

    @Test
    void failedOutputAfterPartialWriteRemovesActualSpillFiles() throws IOException {
        AtomicBoolean observedTemporaryFiles = new AtomicBoolean();
        OutputStream failingOutput = new OutputStream() {
            private int written;

            @Override
            public void write(int value) throws IOException {
                if (written == 0) {
                    try (Stream<Path> paths = Files.walk(temporaryDirectory)) {
                        observedTemporaryFiles.set(paths.anyMatch(Files::isRegularFile));
                    }
                }
                if (++written >= 1024) throw new IOException("Synthetic output failure after partial write");
            }
        };
        assertThatThrownBy(() -> writer.write(syntheticRows(), true, failingOutput))
                .isInstanceOf(IOException.class);
        assertThat(observedTemporaryFiles).isTrue();
        assertNoTemporaryFiles();
    }

    private List<LodgingQueue> syntheticRows() {
        return IntStream.range(0, 201).mapToObj(index -> LodgingQueue.builder()
                .stayGuest(StayGuest.builder().fullName("SYNTHETIC " + index).build()).build()).toList();
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(temporaryDirectory)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }
}
