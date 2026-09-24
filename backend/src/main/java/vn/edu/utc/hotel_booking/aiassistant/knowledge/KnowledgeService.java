package vn.edu.utc.hotel_booking.aiassistant.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.audit.service.AuditService;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class KnowledgeService {
    private final KnowledgeRepository repository;
    private final StaffAccessService access;
    private final HotelCatalogService hotels;
    private final AuditService audit;

    public KnowledgeService(KnowledgeRepository repository, StaffAccessService access,
                            HotelCatalogService hotels, AuditService audit) {
        this.repository = repository;
        this.access = access;
        this.hotels = hotels;
        this.audit = audit;
    }

    @Transactional
    public CreatedDocument create(StaffActor actor, String title, String content,
                                  String visibility, Integer hotelId) {
        access.require(actor, "CREATE:KNOWLEDGE");
        if (!"PUBLIC".equals(visibility) && !"STAFF".equals(visibility)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        authorizeScope(actor, hotelId);
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        repository.create(documentId, versionId, jobId, title, content, sha256(content), visibility, hotelId);
        audit.knowledgeChange(actor, "CREATE", documentId);
        return new CreatedDocument(documentId, versionId, jobId, "DRAFT");
    }

    @Transactional(readOnly = true)
    public KnowledgeRepository.JobStatus job(StaffActor actor, UUID jobId) {
        access.require(actor, "VIEW:KNOWLEDGE");
        var jobs = repository.job(jobId);
        if (jobs.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);
        var document = repository.document(jobs.getFirst().documentId());
        if (document.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);
        authorizeScope(actor, document.getFirst().hotelId());
        return jobs.getFirst();
    }

    @Transactional
    public void publish(StaffActor actor, UUID documentId) {
        access.require(actor, "PUBLISH:KNOWLEDGE");
        var document = document(documentId);
        authorizeScope(actor, document.hotelId());
        if (repository.publish(documentId) == 0) throw new AppException(ErrorCode.CONFLICT);
        audit.knowledgeChange(actor, "PUBLISH", documentId);
    }

    @Transactional
    public void revoke(StaffActor actor, UUID documentId) {
        access.require(actor, "REVOKE:KNOWLEDGE");
        var document = document(documentId);
        authorizeScope(actor, document.hotelId());
        repository.revoke(documentId);
        audit.knowledgeChange(actor, "REVOKE", documentId);
    }

    @Transactional(readOnly = true)
    public KnowledgeRepository.SourceChunk source(StaffActor actor, UUID chunkId) {
        access.require(actor, "USE:AI_ASSISTANT");
        var chunks = repository.source(chunkId);
        if (chunks.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);
        var chunk = chunks.getFirst();
        authorizeReadScope(actor, chunk.hotelId());
        return chunk;
    }

    private KnowledgeRepository.DocumentMeta document(UUID id) {
        var documents = repository.document(id);
        if (documents.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);
        return documents.getFirst();
    }

    public void authorizeScope(StaffActor actor, Integer hotelId) {
        if (hotelId == null) {
            if (!actor.scopeType().equals("CHAIN")) throw new AppException(ErrorCode.UNAUTHORIZED);
            return;
        }
        var hotel = hotels.find(hotelId);
        boolean allowed = switch (actor.scopeType()) {
            case "CHAIN" -> true;
            case "REGION" -> actor.scopeEntityId() != null && actor.scopeEntityId() == hotel.regionId();
            case "PROPERTY" -> Objects.equals(actor.scopeEntityId(), hotelId);
            default -> false;
        };
        if (!allowed) throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    public void authorizeReadScope(StaffActor actor, Integer hotelId) {
        if (hotelId != null) authorizeScope(actor, hotelId);
    }

    private String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public record CreatedDocument(UUID documentId, UUID versionId, UUID jobId, String status) {}
}
