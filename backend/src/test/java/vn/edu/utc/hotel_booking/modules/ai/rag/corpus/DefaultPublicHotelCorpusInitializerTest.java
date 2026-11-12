package vn.edu.utc.hotel_booking.modules.ai.rag.corpus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagActivationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.ScopedRagRetrievalService;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.InMemoryScopedVectorStore;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class DefaultPublicHotelCorpusInitializerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void doesNotSeedSyntheticCorpusWhenFlagIsMissing() {
        contextRunner.run(context -> {
            RagKnowledgeLifecycleService lifecycleService = context.getBean(RagKnowledgeLifecycleService.class);
            RagVectorStorePort vectorStore = context.getBean(RagVectorStorePort.class);

            verify(lifecycleService, never()).ingestAndStage(any(RagIngestRequestDto.class), anyInt());
            verify(lifecycleService, never()).activate(any(RagActivationRequestDto.class));
            verify(vectorStore, never()).clearAll();
        });
    }

    @Test
    void seedsSyntheticCorpusWhenExplicitlyEnabled() {
        contextRunner.withPropertyValues("hotel.ai.rag.demo-corpus.enabled=true").run(context -> {
            RagKnowledgeLifecycleService lifecycleService = context.getBean(RagKnowledgeLifecycleService.class);

            verify(lifecycleService, times(13)).ingestAndStage(any(RagIngestRequestDto.class), anyInt());
            verify(lifecycleService, times(13)).activate(any(RagActivationRequestDto.class));
        });
    }

    @Test
    @DisplayName("Seeds real 13 docs into vector store and verifies policy retrieval and isolation")
    void realCorpusSeedingAndRetrievalVerification() {
        RagCorpusGovernanceValidator validator = new RagCorpusGovernanceValidator();
        HybridRagEmbeddingService embeddingService = new HybridRagEmbeddingService();
        InMemoryScopedVectorStore vectorStore = new InMemoryScopedVectorStore(256);
        RagKnowledgeLifecycleService lifecycleService = new RagKnowledgeLifecycleService(validator, embeddingService, vectorStore);
        ScopedRagRetrievalService retrievalService = new ScopedRagRetrievalService(vectorStore, embeddingService);
        RagGroundedAnswerService answerService = new RagGroundedAnswerService(retrievalService, validator);

        DefaultPublicHotelCorpusInitializer initializer = new DefaultPublicHotelCorpusInitializer(
                lifecycleService,
                vectorStore,
                true
        );
        initializer.seedDefaultCorpus();

        // 1. Verify HN-01 (hotelId = 1) has [DLX], [STE], [EXE]
        RagQueryResultDto hnRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(1)
                        .question("Mô tả diện tích m2 và bố cục các hạng phòng của khách sạn")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(hnRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(hnRes.getAnswer())
                .contains("[DLX] Deluxe")
                .contains("[STE] Suite")
                .contains("[EXE] Executive Suite");
        assertThat(hnRes.getCitations()).isNotEmpty();
        assertThat(hnRes.getCitations().get(0).getSourceId()).isEqualTo("ROOM-DESC-HN01");
        assertThat(hnRes.getCitations().get(0).getCategory()).isEqualTo(RagDocumentCategory.STATIC_ROOM_DESCRIPTION);

        // 2. Verify DN-01 (hotelId = 2) has [SUP], [DLX], [FAM], [STE]
        RagQueryResultDto dnRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(2)
                        .question("Mô tả diện tích m2 và bố cục các hạng phòng của khách sạn")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(dnRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(dnRes.getAnswer())
                .contains("[SUP] Superior")
                .contains("[DLX] Deluxe")
                .contains("[STE] Suite")
                .contains("[FAM] Family Room");
        assertThat(dnRes.getCitations()).isNotEmpty();
        assertThat(dnRes.getCitations().get(0).getSourceId()).isEqualTo("ROOM-DESC-DN01");

        // 3. Verify SP-01 (hotelId = 3) has [STD], [SUP]
        RagQueryResultDto spRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(3)
                        .question("Mô tả diện tích m2 và bố cục các hạng phòng của khách sạn")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(spRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(spRes.getAnswer())
                .contains("[STD] Standard")
                .contains("[SUP] Superior");
        assertThat(spRes.getCitations()).isNotEmpty();
        assertThat(spRes.getCitations().get(0).getSourceId()).isEqualTo("ROOM-DESC-SP01");

        // 4. Fail-closed on missing hotel scope with unaccented query
        RagQueryResultDto scopeReq = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(null)
                        .question("dien tich va bo cuc cac phong khach san")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(scopeReq.getStatus()).isEqualTo(RagAnswerStatus.SCOPE_REQUIRED);

        // 5. Verify isolation: pool inquiry at hotelId=1 does NOT leak room description
        RagQueryResultDto poolRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(1)
                        .question("Hồ bơi mở cửa lúc mấy giờ?")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(poolRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(poolRes.getAnswer()).contains("06:00").contains("21:00");
        assertThat(poolRes.getAnswer()).doesNotContain("[DLX]", "[STE]", "Diện tích");

        // 6. Verify Pet policy retrieval with colloquial terms (chó mèo, cún poodle)
        RagQueryResultDto petRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(1)
                        .question("Khách sạn có cho mang theo chó mèo, bé cún poodle không?")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(petRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(petRes.getAnswer()).contains("không cho phép mang theo thú cưng");
        assertThat(petRes.getCitations()).isNotEmpty();
        assertThat(petRes.getCitations().get(0).getSourceId()).isEqualTo("POLICY-PETS");

        // 7. Verify Early Check-in policy retrieval
        RagQueryResultDto earlyCheckInRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(1)
                        .question("Mình muốn nhận phòng sớm tầm 8h sáng thì có được không?")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(earlyCheckInRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(earlyCheckInRes.getAnswer()).contains("nhận phòng sớm");
        assertThat(earlyCheckInRes.getCitations()).isNotEmpty();
        assertThat(earlyCheckInRes.getCitations().get(0).getSourceId()).isEqualTo("POLICY-EARLY-CHECKIN");

        // 8. Verify Infant & Child surcharge policy retrieval
        RagQueryResultDto childSurchargeRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(1)
                        .question("Bé sơ sinh 6 tháng tuổi có bị tính thêm tiền không?")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(childSurchargeRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(childSurchargeRes.getAnswer()).contains("miễn phí hoàn toàn tiền phòng");
        assertThat(childSurchargeRes.getCitations()).isNotEmpty();
        assertThat(childSurchargeRes.getCitations().get(0).getSourceId()).isEqualTo("POLICY-CHILD-SURCHARGE");

        // 9. Verify Deposit refund policy retrieval
        RagQueryResultDto refundRes = answerService.answerQuestion(
                RagQueryRequestDto.builder()
                        .hotelId(1)
                        .question("Nếu bận việc đột xuất hủy phòng có được hoàn tiền cọc không?")
                        .asOfDate(LocalDate.of(2026, 3, 1))
                        .build()
        );
        assertThat(refundRes.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(refundRes.getAnswer()).contains("hoàn trả 100% tiền đặt cọc");
        assertThat(refundRes.getCitations()).isNotEmpty();
        assertThat(refundRes.getCitations().get(0).getSourceId()).isEqualTo("FAQ-CANCELLATION");
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DefaultPublicHotelCorpusInitializer.class)
    static class TestConfiguration {

        @Bean
        RagKnowledgeLifecycleService ragKnowledgeLifecycleService() {
            return mock(RagKnowledgeLifecycleService.class);
        }

        @Bean
        RagVectorStorePort ragVectorStorePort() {
            return mock(RagVectorStorePort.class);
        }
    }
}
