package vn.edu.utc.hotel_booking.modules.ai.rag;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.CriteriaValidationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.DeterministicRoomRecommendationEngine;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.InMemoryRoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.ScopedRagRetrievalService;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.InMemoryScopedVectorStore;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RagRefinementAndEdgeCasesTest {

    private Validator validator;
    private RagCorpusGovernanceValidator governanceValidator;
    private HybridRagEmbeddingService embeddingService;
    private InMemoryScopedVectorStore vectorStore;
    private RagKnowledgeLifecycleService lifecycleService;
    private ScopedRagRetrievalService retrievalService;
    private RagGroundedAnswerService answerService;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        governanceValidator = new RagCorpusGovernanceValidator();
        embeddingService = new HybridRagEmbeddingService(256, "local-multilingual-hashing-v1");
        vectorStore = new InMemoryScopedVectorStore(256);
        lifecycleService = new RagKnowledgeLifecycleService(governanceValidator, embeddingService, vectorStore);
        retrievalService = new ScopedRagRetrievalService(vectorStore, embeddingService);
        answerService = new RagGroundedAnswerService(retrievalService, governanceValidator);
    }

    @Test
    @DisplayName("Fix H1: Jakarta Bean Validation constraints validate DTO fields accurately")
    void testH1BeanValidationDirectOnDtos() {
        // 1. RagIngestRequestDto
        RagIngestRequestDto emptyIngest = RagIngestRequestDto.builder().build();
        Set<ConstraintViolation<RagIngestRequestDto>> ingestViolations = validator.validate(emptyIngest);
        assertThat(ingestViolations).isNotEmpty();
        assertThat(ingestViolations).anyMatch(v -> v.getPropertyPath().toString().equals("sourceId"));
        assertThat(ingestViolations).anyMatch(v -> v.getPropertyPath().toString().equals("scopeType"));
        assertThat(ingestViolations).anyMatch(v -> v.getPropertyPath().toString().equals("category"));
        assertThat(ingestViolations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
        assertThat(ingestViolations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));

        // 2. RagActivationRequestDto & RagRollbackRequestDto & RagDeleteRequestDto
        RagActivationRequestDto invalidAct = RagActivationRequestDto.builder().version(0).build();
        Set<ConstraintViolation<RagActivationRequestDto>> actViolations = validator.validate(invalidAct);
        assertThat(actViolations).anyMatch(v -> v.getPropertyPath().toString().equals("sourceId"));
        assertThat(actViolations).anyMatch(v -> v.getPropertyPath().toString().equals("version"));

        RagRollbackRequestDto invalidRoll = RagRollbackRequestDto.builder().targetVersion(0).build();
        Set<ConstraintViolation<RagRollbackRequestDto>> rollViolations = validator.validate(invalidRoll);
        assertThat(rollViolations).anyMatch(v -> v.getPropertyPath().toString().equals("targetVersion"));

        RagDeleteRequestDto invalidDel = RagDeleteRequestDto.builder().version(0).build();
        Set<ConstraintViolation<RagDeleteRequestDto>> delViolations = validator.validate(invalidDel);
        assertThat(delViolations).anyMatch(v -> v.getPropertyPath().toString().equals("version"));

        // 3. RagQueryRequestDto
        RagQueryRequestDto invalidQuery = RagQueryRequestDto.builder().question("").topK(0).build();
        Set<ConstraintViolation<RagQueryRequestDto>> queryViolations = validator.validate(invalidQuery);
        assertThat(queryViolations).anyMatch(v -> v.getPropertyPath().toString().equals("question"));
        assertThat(queryViolations).anyMatch(v -> v.getPropertyPath().toString().equals("topK"));
    }

    @Test
    @DisplayName("Fix H2: Concurrency & Thread-Safety in InMemoryScopedVectorStore without ConcurrentModificationException")
    void testH2ConcurrencyThreadSafety() throws Exception {
        // Pre-stage initial document
        lifecycleService.ingestAndStage(
                RagIngestRequestDto.builder()
                        .sourceId("POLICY-CONCURRENCY")
                        .scopeType(RagScopeType.CHAIN_PUBLIC)
                        .category(RagDocumentCategory.POLICY)
                        .title("Chính sách cơ bản")
                        .content("Nội dung chính sách cơ bản ban đầu.")
                        .effectiveFrom(LocalDate.of(2026, 1, 1))
                        .build(),
                1
        );
        lifecycleService.activate(
                RagActivationRequestDto.builder()
                        .sourceId("POLICY-CONCURRENCY")
                        .scopeType(RagScopeType.CHAIN_PUBLIC)
                        .version(1)
                        .build()
        );

        int threadCount = 8;
        int operationsPerThread = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();
        AtomicInteger stagedVersion = new AtomicInteger(2);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        if (threadId % 2 == 0) {
                            // Read thread
                            vectorStore.findCandidateChunks(RagScopeType.CHAIN_PUBLIC, null, LocalDate.of(2026, 6, 1));
                            answerService.answerQuestion(
                                    RagQueryRequestDto.builder()
                                            .question("Nội dung chính sách cơ bản?")
                                            .build()
                            );
                        } else {
                            // Mutation thread
                            int v = stagedVersion.getAndIncrement();
                            lifecycleService.ingestAndStage(
                                    RagIngestRequestDto.builder()
                                            .sourceId("POLICY-CONCURRENCY")
                                            .scopeType(RagScopeType.CHAIN_PUBLIC)
                                            .category(RagDocumentCategory.POLICY)
                                            .title("Chính sách phiên bản " + v)
                                            .content("Nội dung cập nhật tại phiên bản " + v)
                                            .effectiveFrom(LocalDate.of(2026, 1, 1))
                                            .build(),
                                    v
                            );
                            lifecycleService.activate(
                                    RagActivationRequestDto.builder()
                                            .sourceId("POLICY-CONCURRENCY")
                                            .scopeType(RagScopeType.CHAIN_PUBLIC)
                                            .version(v)
                                            .build()
                            );
                        }
                    }
                } catch (Throwable ex) {
                    exceptions.add(ex);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
    }

    @Test
    @DisplayName("Fix H3: Multi-Chunk Document Retrieval retains all chunks and finds information in later paragraphs")
    void testH3MultiChunkDocumentRetrieval() {
        // Document > 1200 characters with 3 clear paragraphs
        String paragraph1 = "Khách sạn Smart Hotel xin trân trọng kính chào quý khách. Chúng tôi luôn mong muốn mang lại trải nghiệm lưu trú hoàn hảo và tiện nghi nhất cho toàn thể quý khách trong suốt kỳ nghỉ dưỡng hoặc chuyến công tác tại các cơ sở trên toàn quốc.\n\n";
        String paragraph2 = "Về dịch vụ ăn uống và ẩm thực: Bữa sáng buffet tiêu chuẩn quốc tế được phục vụ tại nhà hàng tầng 2 từ 06:30 đến 10:00 sáng các ngày trong tuần. Thực đơn phong phú bao gồm các món ăn truyền thống Việt Nam và ẩm thực phương Tây cao cấp.\n\n";
        String paragraph3 = "Dịch vụ gửi hành lý miễn phí: Quý khách được miễn phí gửi hành lý tại quầy Concierge tầng 1 tối đa 24 giờ sau khi hoàn tất thủ tục trả phòng. Sau 24 giờ khách sạn sẽ áp dụng mức phụ thu bảo quản là 50.000 VNĐ mỗi kiện mỗi ngày.";

        String longContent = paragraph1 + paragraph2 + paragraph3;
        assertThat(longContent.length()).isGreaterThan(700);

        RagIngestResultDto ingestRes = lifecycleService.ingestAndStage(
                RagIngestRequestDto.builder()
                        .sourceId("POLICY-HOTEL-SERVICES")
                        .scopeType(RagScopeType.HOTEL_SCOPED)
                        .hotelId(1)
                        .category(RagDocumentCategory.POLICY)
                        .title("Quy định dịch vụ lưu trú và hành lý")
                        .content(longContent)
                        .effectiveFrom(LocalDate.of(2026, 1, 1))
                        .build(),
                1
        );

        assertThat(ingestRes.getStatus()).isEqualTo(RagVersionStatus.STAGED);
        assertThat(ingestRes.getChunkCount()).isGreaterThanOrEqualTo(3);

        lifecycleService.activate(
                RagActivationRequestDto.builder()
                        .sourceId("POLICY-HOTEL-SERVICES")
                        .scopeType(RagScopeType.HOTEL_SCOPED)
                        .hotelId(1)
                        .version(1)
                        .build()
        );

        // Query specifically for luggage storage in paragraph 3
        RagQueryResultDto result = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(1)
                        .question("Quy định gửi hành lý miễn phí ở đâu và tối đa bao lâu?")
                        .build()
        );

        assertThat(result.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(result.getAnswer()).contains("hành lý");
        assertThat(result.getAnswer()).contains("Concierge tầng 1");
        assertThat(result.getCitations()).isNotEmpty();
        assertThat(result.getCitations().get(0).getSourceId()).isEqualTo("POLICY-HOTEL-SERVICES");
    }

    @Test
    @DisplayName("Fix M1: Null-safety when querying or storing chunks without hotelId")
    void testM1NullSafetyInFindCandidateChunks() {
        // Even if hotelId is null in query, candidate check never throws NPE
        List<KnowledgeChunk> chunks = vectorStore.findCandidateChunks(null, null, LocalDate.of(2026, 5, 1));
        assertThat(chunks).isNotNull();

        // Staging a hotel-scoped chunk with null hotelId should be caught safely by governance validator
        RagIngestResultDto res = lifecycleService.ingestAndStage(
                RagIngestRequestDto.builder()
                        .sourceId("POLICY-NULL-HOTEL")
                        .scopeType(RagScopeType.HOTEL_SCOPED)
                        .hotelId(null)
                        .category(RagDocumentCategory.POLICY)
                        .title("Chính sách lỗi scope")
                        .content("Nội dung hợp lệ.")
                        .build(),
                1
        );
        assertThat(res.getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        assertThat(res.getErrorMessage()).contains("REJECTED_MISSING_HOTEL_SCOPE");
    }

    @Test
    @DisplayName("Fix M2: Privacy Policy (Chính sách bảo mật) is accepted, while internal confidential & CCCD are rejected")
    void testM2NoFalsePositiveOnPrivacyPolicy() {
        // 1. Valid public privacy policy must be accepted
        RagIngestRequestDto validPrivacyPolicy = RagIngestRequestDto.builder()
                .sourceId("POLICY-PRIVACY")
                .scopeType(RagScopeType.CHAIN_PUBLIC)
                .category(RagDocumentCategory.POLICY)
                .title("Chính sách bảo mật thông tin và quyền riêng tư của khách hàng")
                .content("Khách sạn cam kết bảo mật thông tin cá nhân và dữ liệu thanh toán của khách hàng theo tiêu chuẩn an toàn quốc tế. Hotline hỗ trợ 19001234.")
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        RagIngestResultDto privacyRes = lifecycleService.ingestAndStage(validPrivacyPolicy, 1);
        assertThat(privacyRes.getStatus()).isEqualTo(RagVersionStatus.STAGED);

        // 2. Document with INTERNAL_ONLY or TÀI LIỆU MẬT must still be rejected
        RagIngestRequestDto confidentialDoc = RagIngestRequestDto.builder()
                .sourceId("POLICY-INTERNAL")
                .scopeType(RagScopeType.CHAIN_PUBLIC)
                .category(RagDocumentCategory.POLICY)
                .title("Tài liệu mật nội bộ")
                .content("TÀI LIỆU MẬT: Mật khẩu hệ thống là pass123")
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        RagIngestResultDto confRes = lifecycleService.ingestAndStage(confidentialDoc, 1);
        assertThat(confRes.getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        assertThat(confRes.getErrorMessage()).contains("REJECTED_CONFIDENTIAL_CONTENT");

        // 3. Document with 12-digit CCCD starting with 0 must be rejected
        RagIngestRequestDto cccdDoc = RagIngestRequestDto.builder()
                .sourceId("POLICY-CCCD")
                .scopeType(RagScopeType.CHAIN_PUBLIC)
                .category(RagDocumentCategory.POLICY)
                .title("Thông tin quản lý")
                .content("Số định danh cá nhân quản lý: 001200001234 trực ca đêm.")
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        RagIngestResultDto cccdRes = lifecycleService.ingestAndStage(cccdDoc, 1);
        assertThat(cccdRes.getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        assertThat(cccdRes.getErrorMessage()).contains("REJECTED_PII_DETECTED");
    }

    @Test
    @DisplayName("Fix M3: HOTEL_SCOPED overrides CHAIN_PUBLIC for the same sourceId")
    void testM3HotelScopedOverrideChainPublic() {
        // 1. Stage and activate chain-wide check-in policy (14:00)
        lifecycleService.ingestAndStage(
                RagIngestRequestDto.builder()
                        .sourceId("POLICY-CHECKIN")
                        .scopeType(RagScopeType.CHAIN_PUBLIC)
                        .category(RagDocumentCategory.POLICY)
                        .title("Quy định nhận phòng toàn chuỗi")
                        .content("Quy định nhận phòng tiêu chuẩn toàn chuỗi là từ 14:00 chiều và trả phòng trước 12:00 trưa.")
                        .effectiveFrom(LocalDate.of(2026, 1, 1))
                        .build(),
                1
        );
        lifecycleService.activate(
                RagActivationRequestDto.builder()
                        .sourceId("POLICY-CHECKIN")
                        .scopeType(RagScopeType.CHAIN_PUBLIC)
                        .version(1)
                        .build()
        );

        // 2. Stage and activate hotel-specific check-in policy for Hotel 2 (15:00)
        lifecycleService.ingestAndStage(
                RagIngestRequestDto.builder()
                        .sourceId("POLICY-CHECKIN")
                        .scopeType(RagScopeType.HOTEL_SCOPED)
                        .hotelId(2)
                        .category(RagDocumentCategory.POLICY)
                        .title("Quy định nhận phòng riêng tại Khách sạn 2")
                        .content("Quy định nhận phòng riêng biệt tại cơ sở 2 là từ 15:00 chiều và trả phòng trước 11:00 trưa.")
                        .effectiveFrom(LocalDate.of(2026, 1, 1))
                        .build(),
                1
        );
        lifecycleService.activate(
                RagActivationRequestDto.builder()
                        .sourceId("POLICY-CHECKIN")
                        .scopeType(RagScopeType.HOTEL_SCOPED)
                        .hotelId(2)
                        .version(1)
                        .build()
        );

        // 3. Query for Hotel 2: Must prioritize Hotel 2 policy (15:00)
        RagQueryResultDto resHotel2 = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(2)
                        .question("Nhận phòng lúc mấy giờ tại khách sạn?")
                        .build()
        );
        assertThat(resHotel2.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(resHotel2.getAnswer()).contains("15:00");
        assertThat(resHotel2.getAnswer()).doesNotContain("14:00");
        assertThat(resHotel2.getCitations()).isNotEmpty();
        assertThat(resHotel2.getCitations().get(0).getHotelId()).isEqualTo(2);

        // 4. Query for Hotel 3 (no specific override): Must fall back to chain-wide policy (14:00)
        RagQueryResultDto resHotel3 = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(3)
                        .question("Nhận phòng lúc mấy giờ tại khách sạn?")
                        .build()
        );
        assertThat(resHotel3.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(resHotel3.getAnswer()).contains("14:00");
        assertThat(resHotel3.getCitations()).isNotEmpty();
        assertThat(resHotel3.getCitations().get(0).getHotelId()).isNull(); // Chain-wide
    }

    @Test
    @DisplayName("Fix L1: queryKnowledgeSafely helper in RoomAdvisorOrchestrationService operates fail-safely")
    void testL1RoomAdvisorQueryKnowledgeSafelyDegradation() {
        RoomAdvisorSessionStore sessionStore = new InMemoryRoomAdvisorSessionStore();
        CriteriaValidationService criteriaValidationService = new CriteriaValidationService();
        DeterministicRoomRecommendationEngine recommendationEngine = new DeterministicRoomRecommendationEngine();

        // 1. Service with null RAG service
        RoomAdvisorOrchestrationService serviceWithoutRag = new RoomAdvisorOrchestrationService(
                criteriaValidationService,
                recommendationEngine,
                null,
                sessionStore,
                null,
                null
        );

        Optional<RagQueryResultDto> nullServiceRes = serviceWithoutRag.queryKnowledgeSafely(
                RagQueryRequestDto.builder().question("Test question").build()
        );
        assertThat(nullServiceRes).isEmpty();

        // 2. Service with null request
        RoomAdvisorOrchestrationService serviceWithRag = new RoomAdvisorOrchestrationService(
                criteriaValidationService,
                recommendationEngine,
                null,
                sessionStore,
                null,
                answerService
        );

        Optional<RagQueryResultDto> nullReqRes = serviceWithRag.queryKnowledgeSafely(null);
        assertThat(nullReqRes).isEmpty();

        // 3. Service with active RAG service answering successfully
        lifecycleService.ingestAndStage(
                RagIngestRequestDto.builder()
                        .sourceId("POLICY-WIFI")
                        .scopeType(RagScopeType.CHAIN_PUBLIC)
                        .category(RagDocumentCategory.AMENITY)
                        .title("Quy định wifi")
                        .content("Mạng wifi miễn phí tốc độ cao phủ sóng toàn bộ khuôn viên khách sạn.")
                        .effectiveFrom(LocalDate.of(2026, 1, 1))
                        .build(),
                1
        );
        lifecycleService.activate(
                RagActivationRequestDto.builder()
                        .sourceId("POLICY-WIFI")
                        .scopeType(RagScopeType.CHAIN_PUBLIC)
                        .version(1)
                        .build()
        );

        Optional<RagQueryResultDto> validRes = serviceWithRag.queryKnowledgeSafely(
                RagQueryRequestDto.builder().question("Khách sạn có wifi miễn phí không?").build()
        );
        assertThat(validRes).isPresent();
        assertThat(validRes.get().getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(validRes.get().getAnswer()).contains("wifi miễn phí");
    }
}
