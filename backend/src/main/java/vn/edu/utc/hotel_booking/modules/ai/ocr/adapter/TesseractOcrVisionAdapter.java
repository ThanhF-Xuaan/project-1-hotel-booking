package vn.edu.utc.hotel_booking.modules.ai.ocr.adapter;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.enums.OcrExtractionStatus;
import vn.edu.utc.hotel_booking.modules.ai.ocr.model.ExtractedFieldDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.port.OcrVisionPort;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser;
import vn.edu.utc.hotel_booking.modules.ai.ocr.validator.OcrUploadValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/** Chạy Tesseract local qua stdin/stdout; dữ liệu ảnh và OCR không được lưu ra đĩa. */
@Component
public class TesseractOcrVisionAdapter implements OcrVisionPort {

    private static final Pattern LANGUAGES = Pattern.compile("[a-z]{3}(\\+[a-z]{3}){1,4}");
    private static final Set<String> REQUIRED_LANGUAGES = Set.of("vie", "eng");
    private static final double MIN_FIELD_CONFIDENCE = 0.85;

    private final OcrEngineProperties properties;
    private final IdentityDocumentOcrService extractionService;
    private final OcrUploadValidator validator;
    private final TesseractTsvParser tsvParser;
    private final Semaphore concurrency;

    public TesseractOcrVisionAdapter(OcrEngineProperties properties,
                                     IdentityDocumentOcrService extractionService,
                                     OcrUploadValidator validator,
                                     TesseractTsvParser tsvParser) {
        this.properties = properties;
        this.extractionService = extractionService;
        this.validator = validator;
        this.tsvParser = tsvParser;
        this.concurrency = new Semaphore(Math.min(2, Math.max(1, properties.getMaxConcurrent())));
    }

    /** Giữ route text hiện có, không phụ thuộc feature flag ảnh. */
    @Override
    public OcrStayGuestDraftDto extract(OcrDocumentExtractRequestDto request) {
        return extractionService.extract(request);
    }

    @Override
    public OcrStayGuestDraftDto extractImage(byte[] imageBytes, OcrDocumentExtractRequestDto metadata) {
        if (!properties.isEnabled()) throw unavailable("Local image OCR is disabled");
        if (metadata == null) throw unavailable("OCR request metadata is missing");
        OcrUploadValidator.ValidationResult validation = validator.validateForOcr(
                metadata.getFileName(), metadata.getContentType(), imageBytes);
        if (!validation.isValid()) throw unavailable("OCR image validation failed");
        if (metadata.getFileSizeBytes() > 0 && metadata.getFileSizeBytes() != imageBytes.length) {
            throw unavailable("OCR image size metadata does not match");
        }
        if (!validRuntimeConfiguration()) throw unavailable("Local OCR runtime configuration is invalid");

        long deadlineNanos = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(properties.getTimeoutMillis());
        boolean acquired;
        try {
            long remaining = deadlineNanos - System.nanoTime();
            acquired = remaining > 0 && concurrency.tryAcquire(remaining, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("Local OCR was interrupted", e);
        }
        if (!acquired) throw unavailable("Local OCR capacity is unavailable");
        try {
            TesseractOutput output = runTesseract(imageBytes, deadlineNanos);
            TesseractTsvParser.ParsedTsv parsed;
            try {
                parsed = tsvParser.parse(output.stdout());
            } catch (IllegalArgumentException e) {
                throw unavailable("Local OCR engine returned invalid output", e);
            }
            OcrDocumentExtractRequestDto request = copyWithOcrText(metadata, parsed.text());
            OcrStayGuestDraftDto draft = extractionService.extract(request);
            applyEngineConfidence(draft, parsed.meanWordConfidence());
            return draft;
        } finally {
            concurrency.release();
        }
    }

    private TesseractOutput runTesseract(byte[] imageBytes, long deadlineNanos) {
        Process process;
        try {
            process = new ProcessBuilder(properties.getExecutablePath(), "stdin", "stdout",
                    "-l", properties.getLanguages(), "tsv")
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException | RuntimeException e) {
            throw unavailable("Local OCR engine is unavailable", e);
        }

        int cap = Math.min(1_048_576, properties.getMaxOutputBytes());
        ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> tasks = new ArrayList<>(3);
        AtomicBoolean outputOverflow = new AtomicBoolean();
        try {
            Future<CappedBytes> stdout = io.submit(() -> readCapped(process.getInputStream(), cap, process, outputOverflow));
            tasks.add(stdout);
            Future<CappedBytes> stderr = io.submit(() -> readCapped(process.getErrorStream(), cap, process, outputOverflow));
            tasks.add(stderr);
            Future<?> stdin = io.submit(() -> {
                try (var stream = process.getOutputStream()) { stream.write(imageBytes); }
                catch (IOException e) { throw new RuntimeException(e); }
            });
            tasks.add(stdin);

            long remaining = deadlineNanos - System.nanoTime();
            boolean exited = remaining > 0 && process.waitFor(remaining, TimeUnit.NANOSECONDS);
            if (outputOverflow.get()) throw unavailable("Local OCR output exceeded limit");
            if (!exited) throw unavailable("Local OCR engine timed out");
            try {
                stdin.get(remainingNanos(deadlineNanos), TimeUnit.NANOSECONDS);
                CappedBytes out = stdout.get(remainingNanos(deadlineNanos), TimeUnit.NANOSECONDS);
                CappedBytes err = stderr.get(remainingNanos(deadlineNanos), TimeUnit.NANOSECONDS);
                if (outputOverflow.get()) throw unavailable("Local OCR output exceeded limit");
                if (process.exitValue() != 0) throw unavailable("Local OCR engine returned an error");
                return new TesseractOutput(new String(out.bytes(), StandardCharsets.UTF_8));
            } catch (ExecutionException | TimeoutException e) {
                if (outputOverflow.get()) throw unavailable("Local OCR output exceeded limit");
                throw unavailable("Local OCR engine could not complete", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("Local OCR was interrupted", e);
        } finally {
            cleanup(process, io, tasks);
        }
    }

    private CappedBytes readCapped(java.io.InputStream stream, int cap, Process process,
                                  AtomicBoolean outputOverflow) throws IOException {
        try (stream) {
            ByteArrayOutputStream kept = new ByteArrayOutputStream(Math.min(cap, 8192));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer, 0, Math.min(buffer.length, cap - kept.size() + 1))) != -1) {
                if (read > cap - kept.size()) {
                    outputOverflow.set(true);
                    stopProcess(process);
                    throw unavailable("Local OCR output exceeded limit");
                }
                kept.write(buffer, 0, read);
            }
            return new CappedBytes(kept.toByteArray());
        }
    }

    private boolean validRuntimeConfiguration() {
        String languages = properties.getLanguages();
        if (languages == null || !LANGUAGES.matcher(languages).matches()) return false;
        return Set.of(languages.split("\\+")).containsAll(REQUIRED_LANGUAGES)
                && properties.getExecutablePath() != null && !properties.getExecutablePath().isBlank()
                && properties.getTimeoutMillis() > 0 && properties.getTimeoutMillis() <= 3000
                && properties.getMaxOutputBytes() > 0 && properties.getMaxOutputBytes() <= 1_048_576;
    }

    private long remainingNanos(long deadlineNanos) throws TimeoutException {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) throw new TimeoutException("Local OCR deadline exceeded");
        return remaining;
    }

    private OcrDocumentExtractRequestDto copyWithOcrText(OcrDocumentExtractRequestDto source, String text) {
        OcrDocumentExtractRequestDto copy = new OcrDocumentExtractRequestDto();
        copy.setFileName(source.getFileName());
        copy.setContentType(source.getContentType());
        copy.setFileSizeBytes(source.getFileSizeBytes());
        copy.setHotelId(source.getHotelId());
        copy.setDocumentTypeHint(source.getDocumentTypeHint());
        copy.setRawOcrText(text == null || text.isBlank() ? "[unreadable image]" : text);
        copy.setFileHeaderBytes(source.getFileHeaderBytes());
        return copy;
    }

    private void applyEngineConfidence(OcrStayGuestDraftDto draft, double engineConfidence) {
        double confidence = Double.isFinite(engineConfidence)
                ? Math.max(0.0, Math.min(1.0, engineConfidence)) : 0.0;
        List<String> lowFields = draft.getMissingOrLowConfidenceFields() == null
                ? new ArrayList<>() : new ArrayList<>(draft.getMissingOrLowConfidenceFields());
        draft.setFullName(cap(draft.getFullName(), confidence, "fullName", lowFields));
        draft.setDateOfBirth(cap(draft.getDateOfBirth(), confidence, "dateOfBirth", lowFields));
        draft.setGender(cap(draft.getGender(), confidence, "gender", lowFields));
        draft.setNationality(cap(draft.getNationality(), confidence, "nationality", lowFields));
        draft.setDocumentType(cap(draft.getDocumentType(), confidence, "documentType", lowFields));
        draft.setDocumentNumber(cap(draft.getDocumentNumber(), confidence, "documentNumber", lowFields));
        draft.setPermanentAddress(cap(draft.getPermanentAddress(), confidence, "permanentAddress", lowFields));
        draft.setMissingOrLowConfidenceFields(List.copyOf(lowFields));
        draft.setOverallConfidence(Math.min(draft.getOverallConfidence(), confidence));
        if (draft.getStatus() != OcrExtractionStatus.UNREADABLE_DOCUMENT && !lowFields.isEmpty()) {
            draft.setStatus(OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);
        }
        if (confidence < MIN_FIELD_CONFIDENCE) {
            List<String> warnings = draft.getWarnings() == null ? new ArrayList<>() : new ArrayList<>(draft.getWarnings());
            warnings.add("Độ tin cậy engine OCR thấp; cần xác minh thủ công.");
            draft.setWarnings(List.copyOf(warnings));
        }
    }

    private <T> ExtractedFieldDto<T> cap(ExtractedFieldDto<T> field, double engineConfidence,
                                         String fieldName, List<String> lowFields) {
        if (field == null) return null;
        double confidence = Math.min(field.getConfidence(), engineConfidence);
        if (confidence < MIN_FIELD_CONFIDENCE && !lowFields.contains(fieldName)) lowFields.add(fieldName);
        if (field.isValid() && confidence >= MIN_FIELD_CONFIDENCE) return ExtractedFieldDto.of(field.getValue(), confidence);
        String message = field.getValidationMessage() == null
                ? "Độ tin cậy OCR dưới ngưỡng yêu cầu" : field.getValidationMessage();
        return ExtractedFieldDto.invalid(field.getValue(), confidence, message);
    }

    private void stopProcess(Process process) {
        if (process.isAlive()) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }

    private void cleanup(Process process, ExecutorService io, List<Future<?>> tasks) {
        // Kill before waiting for pipe tasks. ExecutorService.close() can wait without a bound.
        boolean interrupted = Thread.interrupted();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
        try {
            stopProcess(process);
            tasks.forEach(task -> task.cancel(true));
            io.shutdownNow();
            while ((process.isAlive() || !io.isTerminated()) && System.nanoTime() < deadline) {
                try {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) break;
                    if (process.isAlive()) process.waitFor(remaining, TimeUnit.NANOSECONDS);
                    else io.awaitTermination(remaining, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private OcrVisionPort.ExtractionUnavailableException unavailable(String message) {
        return new OcrVisionPort.ExtractionUnavailableException(message);
    }

    private OcrVisionPort.ExtractionUnavailableException unavailable(String message, Throwable cause) {
        return new OcrVisionPort.ExtractionUnavailableException(message, cause);
    }

    private record CappedBytes(byte[] bytes) { }
    private record TesseractOutput(String stdout) { }
}
