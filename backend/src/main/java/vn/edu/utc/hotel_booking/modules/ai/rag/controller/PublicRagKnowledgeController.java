package vn.edu.utc.hotel_booking.modules.ai.rag.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.PiiRedactionUtil;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/public/ai/room-advisor/knowledge", "/api/v1/ai/room-advisor/knowledge"})
@RequiredArgsConstructor
public class PublicRagKnowledgeController {

    private final RagGroundedAnswerService ragGroundedAnswerService;

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<RagQueryResultDto>> ask(
            @Valid @RequestBody RagQueryRequestDto request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        String rawQuestion = request.getQuestion();
        String safeQuestion = PiiRedactionUtil.redact(rawQuestion);
        log.info("Public RAG ask received for hotelId: {}, question: {}", request.getHotelId(), safeQuestion);

        try {
            RagQueryResultDto result = ragGroundedAnswerService.answerQuestion(request);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("T12-V2-030: RAG knowledge service outage/error: {}", e.getMessage(), e);
            RagQueryResultDto fallback = RagQueryResultDto.builder()
                    .status(RagAnswerStatus.INSUFFICIENT_EVIDENCE)
                    .answer("Hệ thống tra cứu thông tin chính sách tạm thời gián đoạn. Quý khách vui lòng liên hệ nhân viên khách sạn để được hỗ trợ trực tiếp.")
                    .disclaimers(List.of("RAG_TEMP_UNAVAILABLE: Dịch vụ RAG tạm thời không khả dụng, nghiệp vụ tìm kiếm và đặt phòng vẫn hoạt động bình thường."))
                    .build();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.<RagQueryResultDto>builder()
                            .code(503)
                            .message("RAG_TEMP_UNAVAILABLE: " + e.getMessage())
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
        log.warn("Public RAG invalid request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(e.getMessage())
                        .build());
    }
}
