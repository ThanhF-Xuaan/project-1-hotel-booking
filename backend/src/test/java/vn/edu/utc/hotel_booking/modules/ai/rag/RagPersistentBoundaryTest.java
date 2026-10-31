package vn.edu.utc.hotel_booking.modules.ai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagVectorStoreConfiguration;
import vn.edu.utc.hotel_booking.modules.ai.rag.corpus.DefaultPublicHotelCorpusInitializer;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.ScopedRagRetrievalService;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.*;

import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RagPersistentBoundaryTest {
    private ApplicationContextRunner context() {
        return new ApplicationContextRunner().withUserConfiguration(InMemoryScopedVectorStore.class,
                RagVectorStoreConfiguration.class, RagKnowledgeLifecycleService.class, ScopedRagRetrievalService.class,
                RagCorpusGovernanceValidator.class, DefaultPublicHotelCorpusInitializer.class)
                .withBean(RagEmbeddingPort.class, HybridRagEmbeddingService::new);
    }

    @Test void defaultModeRemainsInMemoryWithoutADatasource() {
        context().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(RagVectorStorePort.class);
            assertThat(ctx.getBean(RagVectorStorePort.class)).isInstanceOf(InMemoryScopedVectorStore.class);
            assertThat(ctx).doesNotHaveBean(DataSource.class);
        });
    }

    @Test void persistentDependencyIsDeferredAndCannotReplaceBusinessDatasource() {
        DataSource business = mock(DataSource.class);
        context().withPropertyValues("hotel.ai.rag.store-type=pgvector")
                .withBean(DataSource.class, () -> business).run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(InMemoryScopedVectorStore.class);
                    assertThat(ctx.getBean(DataSource.class)).isSameAs(business);
                    assertThatThrownBy(() -> ctx.getBean(RagKnowledgeLifecycleService.class)
                            .getDocument(RagScopeType.CHAIN_PUBLIC, null, "PUBLIC", 1)).isInstanceOf(RuntimeException.class);
                    assertThat(ctx).hasNotFailed();
                    verifyNoInteractions(business);
                });
    }

    @Test void persistentModeRejectsDemoInitializationBeforeAnyConnection() {
        context().withPropertyValues("hotel.ai.rag.store-type=pgvector", "hotel.ai.rag.demo-corpus.enabled=true")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test void unsupportedModeDoesNotSilentlyFallBackToMemory() {
        new ApplicationContextRunner().withUserConfiguration(InMemoryScopedVectorStore.class, RagVectorStoreConfiguration.class)
                .withPropertyValues("hotel.ai.rag.store-type=unknown")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RagVectorStorePort.class));
    }

    @Test void oversizedCorpusIsRejectedBeforeEmbeddingOrStorage() {
        RagEmbeddingPort embedding = mock(RagEmbeddingPort.class);
        RagVectorStorePort store = mock(RagVectorStorePort.class);
        RagKnowledgeLifecycleService lifecycle = new RagKnowledgeLifecycleService(new RagCorpusGovernanceValidator(), embedding, store);
        RagIngestRequestDto request = RagIngestRequestDto.builder().sourceId("PUBLIC").scopeType(RagScopeType.CHAIN_PUBLIC)
                .category(RagDocumentCategory.FAQ).title("Synthetic pool").content("A".repeat(64001)).build();
        assertThat(lifecycle.ingestAndStage(request, 1).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        request.setContent("Public pool policy."); request.setTitle("A".repeat(501));
        assertThat(lifecycle.ingestAndStage(request, 1).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        request.setTitle("Synthetic pool"); request.setSourceId("A".repeat(129));
        assertThat(lifecycle.ingestAndStage(request, 1).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        request.setSourceId("PUBLIC"); request.setContent("Public pool policy.\n\n".repeat(257));
        assertThat(lifecycle.ingestAndStage(request, 1).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        verifyNoInteractions(embedding, store);
    }
}
