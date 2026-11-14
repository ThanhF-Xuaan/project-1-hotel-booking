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
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.OcrImagePreparation;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
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
            byte[] nameSource = imageBytes;
            var nameBounds = parsed.nameRegion(draft.getFullName() == null ? null : draft.getFullName().getValue());
            var surnameBounds = parsed.surnameRegion(draft.getFullName() == null ? null : draft.getFullName().getValue());
            var addressBounds = parsed.addressRegion();
            byte[] addressSource = imageBytes;
            boolean identityCard = (draft.getDocumentType() != null && draft.getDocumentType().getValue() == DocumentType.CCCD)
                    || OcrImagePreparation.hasIdentityCardLabel(parsed.text());
            if (identityCard
                    && draft.getStatus() != OcrExtractionStatus.SUCCESS
                    && deadlineNanos - System.nanoTime() > TimeUnit.SECONDS.toNanos(2)) {
                byte[] region = OcrImagePreparation.identityRegion(imageBytes);
                if (region != null) {
                    try {
                        var retry = tsvParser.parse(runTesseract(region, deadlineNanos, "6").stdout());
                        var refined = extractionService.extract(copyWithOcrText(metadata, retry.text()));
                        applyEngineConfidence(refined, retry.meanWordConfidence());
                        draft = mergeDrafts(draft, refined);
                        if (nameBounds == null) {
                            nameBounds = retry.nameRegion(refined.getFullName() == null ? null : refined.getFullName().getValue());
                            surnameBounds = retry.surnameRegion(refined.getFullName() == null ? null : refined.getFullName().getValue());
                            nameSource = region;
                        }
                        var refinedAddressBounds = retry.addressRegion();
                        if (refinedAddressBounds != null) {
                            addressBounds = refinedAddressBounds;
                            addressSource = region;
                        }
                    } catch (OcrVisionPort.ExtractionUnavailableException | IllegalArgumentException ignored) {
                        // Do not discard a usable first draft when the bounded retry cannot finish.
                    }
                }
            }
            if (identityCard && nameBounds != null && hasRetryBudget(deadlineNanos)) {
                byte[] row = OcrImagePreparation.textLine(nameSource, nameBounds);
                if (row != null) {
                    try {
                        var line = tsvParser.parse(runTesseract(row, deadlineNanos, "7", "vie").stdout());
                        String name = java.text.Normalizer.normalize(line.text(), java.text.Normalizer.Form.NFC)
                                .trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
                        if (name.matches("[\\p{L}][\\p{L} '\\-]{2,99}") && name.split(" ").length >= 2) {
                            var candidate = ExtractedFieldDto.of(name, line.meanWordConfidence());
                            draft.setFullName(mergeName(draft.getFullName(), candidate));
                            refreshSummary(draft);
                        }
                    } catch (OcrVisionPort.ExtractionUnavailableException | IllegalArgumentException ignored) {
                        // Optional line recognition shares the original deadline and cannot erase the draft.
                    }
                }
            }
            if (identityCard && surnameBounds != null && draft.getFullName() != null
                    && draft.getFullName().getValue() != null && hasRetryBudget(deadlineNanos)) {
                byte[] surname = OcrImagePreparation.textLine(nameSource, surnameBounds);
                if (surname != null) {
                    try {
                        var word = tsvParser.parse(runTesseract(surname, deadlineNanos, "8", "vie").stdout());
                        String value = java.text.Normalizer.normalize(word.text(), java.text.Normalizer.Form.NFC)
                                .trim().toUpperCase(java.util.Locale.ROOT);
                        String[] current = draft.getFullName().getValue().split(" ", 2);
                        if (current.length == 2 && value.matches("[\\p{L}]{2,30}")
                                && foldName(value).equals(foldName(current[0]))) {
                            draft.setFullName(mergeName(draft.getFullName(),
                                    ExtractedFieldDto.of(value + " " + current[1], word.meanWordConfidence())));
                            refreshSummary(draft);
                        }
                    } catch (OcrVisionPort.ExtractionUnavailableException | IllegalArgumentException ignored) {
                        // Keep the earlier draft when optional recognition cannot finish.
                    }
                }
            }
            if (identityCard && addressBounds != null && (draft.getPermanentAddress() == null
                    || !draft.getPermanentAddress().isHighConfidenceAndValid(MIN_FIELD_CONFIDENCE)) && hasRetryBudget(deadlineNanos)) {
                byte[] address = OcrImagePreparation.textBlock(addressSource, addressBounds);
                if (address != null) {
                    try {
                        var block = tsvParser.parse(runTesseract(address, deadlineNanos, "6", "vie").stdout());
                        var candidate = extractionService.extractAddressVn(block.text());
                        if (candidate.getValue() != null) {
                            draft.setPermanentAddress(cap(candidate, block.meanWordConfidence(),
                                    "permanentAddress", new ArrayList<>()));
                            refreshSummary(draft);
                        }
                    } catch (OcrVisionPort.ExtractionUnavailableException | IllegalArgumentException ignored) {
                        // No guessed address when a detected block remains unreadable.
                    }
                }
            }
            return draft;
        } finally {
            concurrency.release();
        }
    }

    private boolean hasRetryBudget(long deadlineNanos) {
        return deadlineNanos - System.nanoTime() > TimeUnit.SECONDS.toNanos(2);
    }

    private TesseractOutput runTesseract(byte[] imageBytes, long deadlineNanos) {
        return runTesseract(imageBytes, deadlineNanos, "3");
    }

    private TesseractOutput runTesseract(byte[] imageBytes, long deadlineNanos, String segmentation) {
        return runTesseract(imageBytes, deadlineNanos, segmentation, properties.getLanguages());
    }

    private TesseractOutput runTesseract(byte[] imageBytes, long deadlineNanos, String segmentation, String languages) {
        Process process;
        try {
            var builder = new ProcessBuilder(properties.getExecutablePath(), "stdin", "stdout",
                    "-l", languages, "--psm", segmentation, "--dpi", "300", "tsv");
            builder.environment().put("OMP_THREAD_LIMIT", "1");
            process = builder.redirectErrorStream(false).start();
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
                && properties.getTimeoutMillis() > 0
                && properties.getTimeoutMillis() <= OcrEngineProperties.MAX_TIMEOUT_MILLIS
                && properties.getMaxOutputBytes() > 0 && properties.getMaxOutputBytes() <= 1_048_576;
    }

    private OcrStayGuestDraftDto mergeDrafts(OcrStayGuestDraftDto first, OcrStayGuestDraftDto second) {
        first.setDocumentType(mergeField(first.getDocumentType(), second.getDocumentType()));
        first.setFullName(mergeName(first.getFullName(), second.getFullName()));
        first.setDateOfBirth(mergeField(first.getDateOfBirth(), second.getDateOfBirth()));
        first.setGender(mergeField(first.getGender(), second.getGender()));
        first.setNationality(mergeField(first.getNationality(), second.getNationality()));
        first.setDocumentNumber(mergeField(first.getDocumentNumber(), second.getDocumentNumber()));
        var address = mergeField(first.getPermanentAddress(), second.getPermanentAddress());
        if (first.getPermanentAddress() != null && second.getPermanentAddress() != null
                && first.getPermanentAddress().getValue() != null && second.getPermanentAddress().getValue() != null
                && !second.getPermanentAddress().getValue().equals(first.getPermanentAddress().getValue())) {
            // Dedicated data-region reading; length is not evidence of accuracy.
            address = ExtractedFieldDto.invalid(second.getPermanentAddress().getValue(),
                    Math.min(first.getPermanentAddress().getConfidence(), second.getPermanentAddress().getConfidence()),
                    "Địa chỉ được bổ sung từ lượt đọc vùng dữ liệu; cần đối chiếu giấy tờ gốc");
        }
        first.setPermanentAddress(address);
        refreshSummary(first);
        return first;
    }

    private void refreshSummary(OcrStayGuestDraftDto first) {
        String[] names = {"fullName", "dateOfBirth", "gender", "nationality", "documentType", "documentNumber", "permanentAddress"};
        var fields = java.util.Arrays.asList(first.getFullName(), first.getDateOfBirth(), first.getGender(),
                first.getNationality(), first.getDocumentType(), first.getDocumentNumber(), first.getPermanentAddress());
        List<String> low = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < names.length; i++) {
            var field = fields.get(i);
            if (field == null || !field.isHighConfidenceAndValid(MIN_FIELD_CONFIDENCE)) low.add(names[i]);
            if (field != null) sum += field.getConfidence();
        }
        first.setMissingOrLowConfidenceFields(List.copyOf(low));
        first.setOverallConfidence(sum / names.length);
        first.setStatus(low.isEmpty() ? OcrExtractionStatus.SUCCESS : OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);
    }

    private ExtractedFieldDto<String> mergeName(ExtractedFieldDto<String> first, ExtractedFieldDto<String> second) {
        if (first == null || first.getValue() == null) return second;
        if (second == null || second.getValue() == null) return first;
        String a = foldName(first.getValue()), b = foldName(second.getValue());
        if (a.equals(b)) {
            long marksA = java.text.Normalizer.normalize(first.getValue(), java.text.Normalizer.Form.NFD)
                    .codePoints().filter(c -> Character.getType(c) == Character.NON_SPACING_MARK).count();
            long marksB = java.text.Normalizer.normalize(second.getValue(), java.text.Normalizer.Form.NFD)
                    .codePoints().filter(c -> Character.getType(c) == Character.NON_SPACING_MARK).count();
            if (marksB > marksA && second.getConfidence() >= .65
                    && second.getConfidence() >= first.getConfidence() - .05) return second;
            if (marksA > marksB) return first;
        }
        return mergeField(first, second);
    }

    private String foldName(String name) {
        return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('Đ', 'D').replace('đ', 'd').toUpperCase(java.util.Locale.ROOT);
    }

    private <T> ExtractedFieldDto<T> mergeField(ExtractedFieldDto<T> first, ExtractedFieldDto<T> second) {
        if (first == null || first.getValue() == null) return second;
        if (second == null || second.getValue() == null) return first;
        if (!first.getValue().equals(second.getValue())) {
            return ExtractedFieldDto.invalid(first.getValue(), Math.min(first.getConfidence(), second.getConfidence()),
                    "Hai lượt đọc OCR khác nhau; cần đối chiếu giấy tờ gốc");
        }
        return second.getConfidence() > first.getConfidence() ? second : first;
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
