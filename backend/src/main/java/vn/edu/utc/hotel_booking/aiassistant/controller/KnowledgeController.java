package vn.edu.utc.hotel_booking.aiassistant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeRepository;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeService;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/knowledge")
@Tag(name = "AI knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledge;
    private final StaffAccessService access;
    private final boolean enabled;

    public KnowledgeController(KnowledgeService knowledge, StaffAccessService access,
                               @Value("${app.ai.enabled:false}") boolean enabled) {
        this.knowledge = knowledge;
        this.access = access;
        this.enabled = enabled;
    }

    @PostMapping("/documents")
    @Operation(summary = "Create a draft document and queue embedding")
    public ResponseEntity<ApiResponse<KnowledgeService.CreatedDocument>> create(@Valid @RequestBody CreateDocument request) {
        requireEnabled();
        var created = knowledge.create(access.current(), request.title(), request.content(),
                request.visibility(), request.hotelId());
        return ResponseEntity.created(URI.create("/api/v1/ai/knowledge/jobs/" + created.jobId()))
                .body(ApiResponse.<KnowledgeService.CreatedDocument>builder().result(created).build());
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Inspect an ingestion job")
    public ApiResponse<KnowledgeRepository.JobStatus> job(@PathVariable UUID jobId) {
        requireEnabled();
        return ApiResponse.<KnowledgeRepository.JobStatus>builder().result(knowledge.job(access.current(), jobId)).build();
    }

    @PatchMapping("/documents/{documentId}")
    @Operation(summary = "Publish or revoke a document")
    public ResponseEntity<Void> change(@PathVariable UUID documentId, @Valid @RequestBody ChangeDocument request) {
        requireEnabled();
        switch (request.action()) {
            case "PUBLISH" -> knowledge.publish(access.current(), documentId);
            case "REVOKE" -> knowledge.revoke(access.current(), documentId);
            default -> throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sources/{chunkId}")
    @Operation(summary = "Open a cited, currently published source")
    public ApiResponse<SourceResponse> source(@PathVariable UUID chunkId) {
        requireEnabled();
        var source = knowledge.source(access.current(), chunkId);
        return ApiResponse.<SourceResponse>builder().result(new SourceResponse(source.id(),
                source.documentId(), source.title(), source.content())).build();
    }

    private void requireEnabled() {
        if (!enabled) throw new AppException(ErrorCode.AI_UNAVAILABLE);
    }

    public record CreateDocument(@NotBlank @Size(max = 200) String title,
                                 @NotBlank @Size(max = 20000) String content,
                                 @NotBlank @Pattern(regexp = "PUBLIC|STAFF") String visibility,
                                 Integer hotelId) {}
    public record ChangeDocument(@NotBlank @Pattern(regexp = "PUBLISH|REVOKE") String action) {}
    public record SourceResponse(UUID sourceId, UUID documentId, String title, String content) {}
}
