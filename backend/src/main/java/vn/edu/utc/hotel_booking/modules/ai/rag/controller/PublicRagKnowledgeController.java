package vn.edu.utc.hotel_booking.modules.ai.rag.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagRequestBudget;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/public/ai/room-advisor/knowledge", "/api/v1/ai/room-advisor/knowledge"})
public class PublicRagKnowledgeController {

    private final RagGroundedAnswerService ragGroundedAnswerService;
    private final PublicHotelEligibilityReader hotelEligibility;
    private final RagRequestBudget requestBudget;

    public PublicRagKnowledgeController(RagGroundedAnswerService service, PublicHotelEligibilityReader hotelEligibility) {
        this(service, hotelEligibility, new RagRequestBudget());
    }

    @Autowired
    public PublicRagKnowledgeController(RagGroundedAnswerService service, PublicHotelEligibilityReader hotelEligibility,
                                        RagRequestBudget requestBudget) {
        this.ragGroundedAnswerService = service;
        this.hotelEligibility = hotelEligibility;
        this.requestBudget = requestBudget;
    }

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<RagQueryResultDto>> ask(
            @Valid @RequestBody RagQueryRequestDto request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        if (request.getHotelCode() != null && !request.getHotelCode().isBlank()) {
            throw new IllegalArgumentException("Use a verified hotelId");
        }
        try {
            return requestBudget.execute(() -> {
                if (request.getHotelId() != null && !hotelEligibility.isActivePublicHotel(request.getHotelId().shortValue())) {
                    return ResponseEntity.ok(ApiResponse.success(RagQueryResultDto.builder()
                            .status(RagAnswerStatus.SCOPE_REQUIRED)
                            .answer("Vui lòng chọn khách sạn đang hoạt động trong danh sách công khai.").build()));
                }
                // Copy instead of mutating the caller's DTO. Raw current question never enters retrieval/provider work.
                RagQueryRequestDto safe = RagQueryRequestDto.builder().hotelId(request.getHotelId())
                        .question(RagRequestBudget.redactQuestion(request.getQuestion())).language(request.getLanguage())
                        .asOfDate(request.getAsOfDate()).topK(request.getTopK()).build();
                RagQueryResultDto result = ragGroundedAnswerService.answerQuestion(safe);
                return ResponseEntity.ok(ApiResponse.success(result));
            });
        } catch (RagRequestBudget.LimitExceededException limited) {
            // Additive PROPOSED HTTP contract; Member 2 review is still required before shared release.
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", Long.toString(limited.retryAfterSeconds()))
                    .body(ApiResponse.<RagQueryResultDto>builder().code(429)
                            .message("Đã vượt giới hạn tra cứu. Vui lòng thử lại sau.").build());
        } catch (Exception e) {
            log.warn("T12-V2-030: RAG knowledge temporarily unavailable");
            RagQueryResultDto fallback = RagQueryResultDto.builder()
                    .status(RagAnswerStatus.INSUFFICIENT_EVIDENCE)
                    .answer("Hệ thống tra cứu thông tin chính sách tạm thời gián đoạn. Quý khách vui lòng liên hệ nhân viên khách sạn để được hỗ trợ trực tiếp.")
                    .disclaimers(List.of("RAG_TEMP_UNAVAILABLE: Dịch vụ RAG tạm thời không khả dụng, nghiệp vụ tìm kiếm và đặt phòng vẫn hoạt động bình thường."))
                    .build();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.<RagQueryResultDto>builder()
                            .code(503)
                            .message("RAG_TEMP_UNAVAILABLE: Dịch vụ tra cứu tạm thời không khả dụng.")
                            .result(fallback)
                            .build());
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("Public RAG validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(message)
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        log.warn("Public RAG invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message("Yêu cầu tra cứu không hợp lệ.")
                        .build());
    }
}
