package vn.edu.utc.hotel_booking.modules.ai.ocr.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.enums.OcrExtractionStatus;
import vn.edu.utc.hotel_booking.modules.ai.ocr.port.OcrVisionPort;
import vn.edu.utc.hotel_booking.modules.ai.ocr.validator.OcrUploadValidator;

import java.util.stream.Collectors;

/**
 * Controller bóc tách thông tin giấy tờ tuỳ thân phục vụ lễ tân.
 *
 * <p>Chỉ dành cho các vai trò: CHAIN_ADMIN, PROPERTY_MANAGER, RECEPTIONIST.</p>
 *
 * <p><strong>Endpoints:</strong>
 * <ul>
 *   <li>{@code POST /api/v1/ai/ocr/extract} – nhận văn bản OCR thô qua JSON</li>
 *   <li>{@code POST /api/v1/ai/ocr/extract-file} – nhận file ảnh multipart</li>
 * </ul>
 * </p>
 *
 * <p><strong>Bất biến:</strong> Không tự động lưu kết quả vào database.
 * Kết quả chỉ là bản nháp để nhân viên xác minh và nhập thủ công.</p>
 */
@RestController
@RequestMapping("/api/v1/ai/ocr")
@PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
public class IdentityDocumentOcrController {

    private static final Logger log = LoggerFactory.getLogger(IdentityDocumentOcrController.class);

    private final OcrVisionPort ocrVisionPort;
    private final OcrUploadValidator uploadValidator;

    public IdentityDocumentOcrController(OcrVisionPort ocrVisionPort,
                                         OcrUploadValidator uploadValidator) {
        this.ocrVisionPort = ocrVisionPort;
        this.uploadValidator = uploadValidator;
    }

    /**
     * Bóc tách thông tin từ văn bản OCR thô (JSON body).
     *
     * @param request Yêu cầu chứa văn bản OCR thô, metadata file
     * @return Bản nháp trường dữ liệu kèm trạng thái và độ tin cậy
     */
    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<OcrStayGuestDraftDto>> extract(
            @Valid @RequestBody OcrDocumentExtractRequestDto request) {
        log.debug("U13 OCR /extract request received");
        OcrStayGuestDraftDto draft = ocrVisionPort.extract(request);
        return ResponseEntity.ok(ApiResponse.success(draft));
    }

    /**
     * Bóc tách thông tin từ file ảnh tải lên (multipart).
     *
     * <p>Kiểm tra MIME type, kích thước, magic bytes trước khi xử lý.
     * Không ghi file xuống ổ đĩa.</p>
     *
     * @param file       File ảnh (JPEG / PNG / WebP, tối đa 5 MB)
     * @param rawOcrText Văn bản OCR đã trích xuất bởi client scanner (tùy chọn)
     * @return Bản nháp trường dữ liệu hoặc lỗi 400 nếu file không hợp lệ
     */
    @PostMapping("/extract-file")
    public ResponseEntity<ApiResponse<OcrStayGuestDraftDto>> extractFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "rawOcrText", required = false) String rawOcrText) {

        OcrUploadValidator.ValidationResult validation = uploadValidator.validate(file);
        if (!validation.isValid()) {
            log.warn("U13 OCR /extract-file: file không hợp lệ – {}", validation.getReason());
            OcrStayGuestDraftDto invalid = buildInvalidFileResult(validation.getReason());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<OcrStayGuestDraftDto>builder()
                            .code(400).message(validation.getReason()).result(invalid).build());
        }

        // Xây dựng request từ file + rawOcrText
        OcrDocumentExtractRequestDto request = new OcrDocumentExtractRequestDto();
        request.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
        request.setContentType(file.getContentType() != null ? file.getContentType() : "");
        request.setFileSizeBytes(file.getSize());
        // rawOcrText: client cung cấp hoặc dùng tên file làm hint
        String textToProcess = (rawOcrText != null && !rawOcrText.isBlank())
                ? rawOcrText
                : "(file upload - no rawOcrText provided)";
        request.setRawOcrText(textToProcess);

        try {
            byte[] bytes = file.getBytes();
            int headerLen = Math.min(bytes.length, 16);
            byte[] header = new byte[headerLen];
            System.arraycopy(bytes, 0, header, 0, headerLen);
            request.setFileHeaderBytes(header);
        } catch (Exception e) {
            log.warn("U13 OCR: không đọc được header bytes");
        }

        log.debug("U13 OCR /extract-file: sizeBytes={}", file.getSize());
        OcrStayGuestDraftDto draft = ocrVisionPort.extract(request);
        return ResponseEntity.ok(ApiResponse.success(draft));
    }

    // ---- Exception Handlers cục bộ ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.debug("U13 OCR validation error: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder().code(400).message(message).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder().code(400).message(ex.getMessage()).build());
    }

    private OcrStayGuestDraftDto buildInvalidFileResult(String reason) {
        OcrStayGuestDraftDto result = new OcrStayGuestDraftDto();
        result.setStatus(OcrExtractionStatus.INVALID_FILE);
        result.setOverallConfidence(0.0);
        result.setMissingOrLowConfidenceFields(java.util.List.of());
        result.setWarnings(java.util.List.of(reason));
        return result;
    }
}
