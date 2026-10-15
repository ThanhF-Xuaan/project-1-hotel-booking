package vn.edu.utc.hotel_booking.modules.ai.rag.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeDocumentVersion;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/admin/knowledge")
@PreAuthorize("hasRole('ROLE_CHAIN_ADMIN')")
@RequiredArgsConstructor
public class AdminRagLifecycleController {

    private final RagKnowledgeLifecycleService lifecycleService;

    @PostMapping("/stage")
    public ResponseEntity<ApiResponse<RagIngestResultDto>> stage(
            @Valid @RequestBody RagIngestRequestDto request,
            @RequestParam(name = "version", defaultValue = "1") int version
    ) {
        log.info("Admin RAG stage requested for sourceId: {}, version: {}", request.getSourceId(), version);
        RagIngestResultDto result = lifecycleService.ingestAndStage(request, version);

        if (result.getStatus() == RagVersionStatus.REJECTED) {
            log.warn("Admin RAG stage rejected: {}", result.getErrorMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<RagIngestResultDto>builder()
                            .code(400)
                            .message(result.getErrorMessage())
                            .result(result)
                            .build());
        }

        return ResponseEntity.ok(ApiResponse.success("Stage tài liệu thành công", result));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<KnowledgeDocumentVersion>> activate(
            @Valid @RequestBody RagActivationRequestDto request
    ) {
        log.info("Admin RAG activate requested for sourceId: {}, version: {}", request.getSourceId(), request.getVersion());
        boolean success = lifecycleService.activate(request);

        if (!success) {
            log.warn("Admin RAG activate failed for sourceId: {}, version: {}", request.getSourceId(), request.getVersion());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<KnowledgeDocumentVersion>builder()
                            .code(400)
                            .message("Không thể kích hoạt phiên bản: phiên bản không tồn tại hoặc không ở trạng thái STAGED")
                            .build());
        }

        KnowledgeDocumentVersion doc = lifecycleService.getActiveDocument(
                request.getScopeType(), request.getHotelId(), request.getSourceId()
        ).orElse(null);

        return ResponseEntity.ok(ApiResponse.success("Kích hoạt tài liệu thành công", doc));
    }

    @PostMapping("/rollback")
    public ResponseEntity<ApiResponse<KnowledgeDocumentVersion>> rollback(
            @Valid @RequestBody RagRollbackRequestDto request
    ) {
        log.info("Admin RAG rollback requested for sourceId: {}, targetVersion: {}", request.getSourceId(), request.getTargetVersion());
        boolean success = lifecycleService.rollback(request);

        if (!success) {
            log.warn("Admin RAG rollback failed for sourceId: {}", request.getSourceId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<KnowledgeDocumentVersion>builder()
                            .code(400)
                            .message("Không thể hoàn tác phiên bản: không tìm thấy phiên bản hợp lệ để hoàn tác")
                            .build());
        }

        KnowledgeDocumentVersion doc = lifecycleService.getActiveDocument(
                request.getScopeType(), request.getHotelId(), request.getSourceId()
        ).orElse(null);

        return ResponseEntity.ok(ApiResponse.success("Hoàn tác tài liệu thành công", doc));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Valid @RequestBody RagDeleteRequestDto request
    ) {
        log.info("Admin RAG delete requested for sourceId: {}, version: {}", request.getSourceId(), request.getVersion());
        try {
            boolean success = lifecycleService.delete(request);
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<Void>builder()
                                .code(404)
                                .message("Không tìm thấy phiên bản tài liệu để xóa")
                                .build());
            }
            return ResponseEntity.ok(ApiResponse.success("Xóa phiên bản tài liệu thành công", null));
        } catch (IllegalStateException e) {
            log.warn("Admin RAG delete rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .code(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("Admin RAG validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(message)
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        log.warn("Admin RAG invalid request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(e.getMessage())
                        .build());
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleStateConflict(IllegalStateException e) {
        log.warn("Admin RAG state conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(e.getMessage())
                        .build());
    }
}
