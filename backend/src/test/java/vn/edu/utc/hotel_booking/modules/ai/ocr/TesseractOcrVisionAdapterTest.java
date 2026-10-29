package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import vn.edu.utc.hotel_booking.modules.ai.ocr.adapter.TesseractOcrVisionAdapter;
import vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.enums.OcrExtractionStatus;
import vn.edu.utc.hotel_booking.modules.ai.ocr.port.OcrVisionPort;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser;
import vn.edu.utc.hotel_booking.modules.ai.ocr.validator.OcrUploadValidator;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class TesseractOcrVisionAdapterTest {

    @Test
    void imageOcrIsDisabledByDefaultAndFailsClosed() {
        OcrEngineProperties properties = new OcrEngineProperties();
        TesseractOcrVisionAdapter adapter = adapter(properties);

        assertThat(properties.isEnabled()).isFalse();
        assertThatThrownBy(() -> adapter.extractImage(new byte[0], metadata("cccd-synthetic.png", "image/png", 0)))
                .isInstanceOf(OcrVisionPort.ExtractionUnavailableException.class)
                .hasMessage("Local image OCR is disabled");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TESSERACT_EXE", matches = ".+")
    void recognizesSyntheticVietnameseIdentityCardAndKeepsHumanReview() throws IOException {
        byte[] image = fixture("cccd-synthetic.png");
        OcrStayGuestDraftDto draft = adapter(enabledProperties()).extractImage(
                image, metadata("cccd-synthetic.png", "image/png", image.length));

        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getStatus()).isNotEqualTo(OcrExtractionStatus.UNREADABLE_DOCUMENT);
        assertThat(draft.getDocumentNumber().getValue()).isEqualTo("079190012345");
        assertThat(draft.getFullName().getValue()).contains("NGUYEN VAN TEST");
        assertThat(draft.getOverallConfidence()).isGreaterThan(0.0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TESSERACT_EXE", matches = ".+")
    void recognizesSyntheticPassportMrzAndKeepsHumanReview() throws IOException {
        byte[] image = fixture("passport-synthetic.png");
        OcrDocumentExtractRequestDto request = metadata("passport-synthetic.png", "image/png", image.length);
        request.setDocumentTypeHint(DocumentType.PASSPORT);

        OcrStayGuestDraftDto draft = adapter(enabledProperties()).extractImage(image, request);

        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getDocumentType().getValue()).isEqualTo(DocumentType.PASSPORT);
        assertThat(draft.getDocumentNumber().getValue()).contains("A12345678");
        assertThat(draft.getStatus()).isNotEqualTo(OcrExtractionStatus.UNREADABLE_DOCUMENT);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TESSERACT_EXE", matches = ".+")
    void lowContrastSyntheticImageNeverReturnsConfidentSuccess() throws IOException {
        byte[] image = fixture("low-contrast-synthetic.png");
        OcrStayGuestDraftDto draft = adapter(enabledProperties()).extractImage(
                image, metadata("low-contrast-synthetic.png", "image/png", image.length));

        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getStatus()).isIn(OcrExtractionStatus.UNREADABLE_DOCUMENT,
                OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);
        assertThat(draft.getOverallConfidence()).isLessThan(0.85);
    }

    @Test
    void invalidImageFailsBeforeStartingEngine() {
        OcrEngineProperties properties = enabledProperties();
        properties.setExecutablePath("missing-tesseract-for-test");

        assertThatThrownBy(() -> adapter(properties).extractImage(new byte[]{1, 2, 3},
                metadata("invalid.png", "image/png", 3)))
                .isInstanceOf(OcrVisionPort.ExtractionUnavailableException.class)
                .hasMessage("OCR image validation failed");
    }

    @Test
    void interruptStopsLiveProcessAndReleasesIoThreadsAndSlot() throws Exception {
        OcrEngineProperties properties = processTestProperties();
        TesseractOcrVisionAdapter adapter = adapter(properties);
        byte[] image = fixture("cccd-synthetic.png");
        TrackedProcess process = startSyntheticProcess("sleep");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            failure.set(catchThrowable(() -> extractUsingProcess(adapter, image, process)));
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });
        worker.setDaemon(true);
        try {
            worker.start();
            assertThat(process.readersStarted.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(process.isAlive()).isTrue();
            long interruptedAt = System.nanoTime();
            worker.interrupt();
            worker.join(1200);

            assertThat(worker.isAlive()).as("Cancellation must not wait for the engine to exit itself").isFalse();
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - interruptedAt)).isLessThan(1200);
            assertThat(failure.get()).isInstanceOf(OcrVisionPort.ExtractionUnavailableException.class)
                    .hasMessage("Local OCR was interrupted");
            assertThat(interruptRestored).isTrue();
            assertCleanedUp(adapter, process);
        } finally {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
            worker.join(2000);
        }
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void stdoutOverflowStopsProcessBeforeDeadlineWithoutLeakingOutput(CapturedOutput output) throws Exception {
        assertOverflowStopsProcess("stdout", output);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void stderrOverflowStopsProcessBeforeDeadlineWithoutLeakingOutput(CapturedOutput output) throws Exception {
        assertOverflowStopsProcess("stderr", output);
    }

    @Test
    void timeoutStopsLiveProcessAndReleasesIoThreadsAndSlot() throws Exception {
        OcrEngineProperties properties = processTestProperties();
        properties.setTimeoutMillis(200);
        TesseractOcrVisionAdapter adapter = adapter(properties);
        byte[] image = fixture("cccd-synthetic.png");
        TrackedProcess process = startSyntheticProcess("sleep");
        try {
            Throwable failure = catchThrowable(() -> extractUsingProcess(adapter, image, process));
            assertThat(failure).isInstanceOf(OcrVisionPort.ExtractionUnavailableException.class)
                    .hasMessage("Local OCR engine timed out");
            assertThat(process.extractionMillis).isLessThan(1200);
            assertCleanedUp(adapter, process);
        } finally {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
        }
    }

    private void assertOverflowStopsProcess(String stream, CapturedOutput output) throws Exception {
        TesseractOcrVisionAdapter adapter = adapter(processTestProperties());
        byte[] image = fixture("cccd-synthetic.png");
        TrackedProcess process = startSyntheticProcess(stream);
        try {
            Throwable failure = catchThrowable(() -> extractUsingProcess(adapter, image, process));
            assertThat(failure).isInstanceOf(OcrVisionPort.ExtractionUnavailableException.class)
                    .hasMessage("Local OCR output exceeded limit");
            assertThat(process.extractionMillis).isLessThan(1500);
            assertThat(output.getAll()).doesNotContain("SYNTHETIC_OCR_OUTPUT");
            assertCleanedUp(adapter, process);
        } finally {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
        }
    }

    private OcrEngineProperties processTestProperties() {
        OcrEngineProperties properties = new OcrEngineProperties();
        properties.setEnabled(true);
        properties.setMaxConcurrent(1);
        properties.setMaxOutputBytes(1024);
        properties.setExecutablePath("synthetic-process-intercepted-by-test");
        return properties;
    }

    private void extractUsingProcess(TesseractOcrVisionAdapter adapter, byte[] image, TrackedProcess process) throws Exception {
        // Only replace process creation; adapter I/O, wait, cancellation and semaphore run unchanged.
        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            when(builder.redirectErrorStream(false)).thenReturn(builder);
            when(builder.start()).thenReturn(process);
        })) {
            long start = System.nanoTime();
            try {
                adapter.extractImage(image, metadata("cccd-synthetic.png", "image/png", image.length));
            } finally {
                process.extractionMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            }
        }
    }

    private void assertCleanedUp(TesseractOcrVisionAdapter adapter, TrackedProcess process) throws Exception {
        assertThat(process.isAlive()).as("Engine must be killed and reaped").isFalse();
        for (Thread thread : process.ioThreads) {
            thread.join(500);
            assertThat(thread.isAlive()).as("OCR pipe task must finish").isFalse();
        }
        var field = TesseractOcrVisionAdapter.class.getDeclaredField("concurrency");
        field.setAccessible(true);
        assertThat(((Semaphore) field.get(adapter)).availablePermits()).as("OCR slot must be returned").isEqualTo(1);
    }

    private TrackedProcess startSyntheticProcess(String mode) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        String classes = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        return new TrackedProcess(new ProcessBuilder(java.toString(), "-cp", classes,
                SyntheticOcrProcess.class.getName(), mode).start());
    }

    /** Real child with live pipes; emits only a synthetic marker and waits to be terminated. */
    public static class SyntheticOcrProcess {
        public static void main(String[] args) throws Exception {
            if (!"sleep".equals(args[0])) {
                var stream = "stdout".equals(args[0]) ? System.out : System.err;
                stream.print("SYNTHETIC_OCR_OUTPUT".repeat(128));
                stream.flush();
            }
            Thread.sleep(30_000);
        }
    }

    private static class TrackedProcess extends Process {
        private final Process delegate;
        private final CountDownLatch readersStarted = new CountDownLatch(2);
        private final Set<Thread> readers = ConcurrentHashMap.newKeySet();
        private final Set<Thread> ioThreads = ConcurrentHashMap.newKeySet();
        private volatile long extractionMillis;

        TrackedProcess(Process delegate) { this.delegate = delegate; }

        private InputStream tracked(InputStream stream) {
            return new FilterInputStream(stream) {
                @Override
                public int read(byte[] bytes, int offset, int length) throws IOException {
                    ioThreads.add(Thread.currentThread());
                    if (readers.add(Thread.currentThread())) readersStarted.countDown();
                    return super.read(bytes, offset, length);
                }
            };
        }

        @Override public InputStream getInputStream() { return tracked(delegate.getInputStream()); }
        @Override public InputStream getErrorStream() { return tracked(delegate.getErrorStream()); }
        @Override public OutputStream getOutputStream() {
            return new FilterOutputStream(delegate.getOutputStream()) {
                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    ioThreads.add(Thread.currentThread());
                    out.write(bytes, offset, length);
                }
            };
        }
        @Override public int waitFor() throws InterruptedException { return delegate.waitFor(); }
        @Override public boolean waitFor(long time, TimeUnit unit) throws InterruptedException { return delegate.waitFor(time, unit); }
        @Override public int exitValue() { return delegate.exitValue(); }
        @Override public void destroy() { delegate.destroy(); }
        @Override public Process destroyForcibly() { delegate.destroyForcibly(); return this; }
        @Override public boolean isAlive() { return delegate.isAlive(); }
        @Override public ProcessHandle toHandle() { return delegate.toHandle(); }
    }

    private TesseractOcrVisionAdapter adapter(OcrEngineProperties properties) {
        return new TesseractOcrVisionAdapter(properties, new IdentityDocumentOcrService(),
                new OcrUploadValidator(), new TesseractTsvParser());
    }

    private OcrEngineProperties enabledProperties() {
        OcrEngineProperties properties = new OcrEngineProperties();
        properties.setEnabled(true);
        properties.setExecutablePath(System.getenv("TESSERACT_EXE"));
        properties.setLanguages("vie+eng");
        return properties;
    }

    private OcrDocumentExtractRequestDto metadata(String fileName, String contentType, long size) {
        OcrDocumentExtractRequestDto request = new OcrDocumentExtractRequestDto();
        request.setFileName(fileName);
        request.setContentType(contentType);
        request.setFileSizeBytes(size);
        request.setRawOcrText("synthetic input only");
        return request;
    }

    private byte[] fixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/ai/ocr/" + name)) {
            if (input == null) throw new IOException("Missing synthetic OCR fixture: " + name);
            return input.readAllBytes();
        }
    }
}
