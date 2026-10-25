package vn.edu.utc.hotel_booking.modules.ai.personalization.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationResultDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationEvalReportDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.GuestPersonalizationService;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.PersonalizationBaselineEvaluator;

/**
 * REST Controller đánh giá U14 bằng dữ liệu giả lập do admin cung cấp.
 *
 * <p><strong>Phân quyền RBAC:</strong>
 * Mặc định tắt. Chỉ CHAIN_ADMIN được gọi khi bật synthetic-evaluation-enabled.
 * Consent/history trong request chưa phải dữ liệu nghiệp vụ được server xác minh.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/ai/personalization")
@ConditionalOnProperty(prefix = "hotel.ai.personalization", name = "synthetic-evaluation-enabled", havingValue = "true")
@PreAuthorize("hasRole('CHAIN_ADMIN')")
public class GuestPersonalizationController {

    private static final Logger log = LoggerFactory.getLogger(GuestPersonalizationController.class);

    private final GuestPersonalizationService personalizationService;
    private final PersonalizationBaselineEvaluator baselineEvaluator;

    public GuestPersonalizationController(
            GuestPersonalizationService personalizationService,
            PersonalizationBaselineEvaluator baselineEvaluator
    ) {
        this.personalizationService = personalizationService;
        this.baselineEvaluator = baselineEvaluator;
    }

    /**
     * Phân tích lịch sử lưu trú và trả về các gợi ý hạng phòng, đặc tính và dịch vụ cá nhân hoá.
     */
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<GuestPersonalizationResultDto>> recommend(
            @Valid @RequestBody GuestPersonalizationRequestDto request
    ) {
        log.debug("U14 synthetic recommendation requested");
        GuestPersonalizationResultDto result = personalizationService.personalize(request);
        return ResponseEntity.ok(ApiResponse.success("Phân tích sở thích khách hàng thành công", result));
    }

    /**
     * Đánh giá Holdout Offline kiểm chứng mô hình Time-Decayed so với Simple Frequency Baseline.
     */
    @PostMapping("/evaluate-baseline")
    public ResponseEntity<ApiResponse<PersonalizationEvalReportDto>> evaluateBaseline(
            @Valid @RequestBody GuestPersonalizationRequestDto request,
            @RequestParam(name = "k", defaultValue = "1") int k
    ) {
        log.debug("U14 synthetic baseline evaluation requested");
        PersonalizationEvalReportDto report = baselineEvaluator.evaluateHoldout(request, k);
        return ResponseEntity.ok(ApiResponse.success("Đánh giá mô hình baseline thành công", report));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("U14 synthetic request failed validation");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(message)
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        log.warn("U14 synthetic request rejected");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(e.getMessage())
                        .build());
    }
}
