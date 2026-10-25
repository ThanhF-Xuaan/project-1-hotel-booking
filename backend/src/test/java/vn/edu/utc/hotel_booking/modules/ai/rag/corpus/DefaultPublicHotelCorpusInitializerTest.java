package vn.edu.utc.hotel_booking.modules.ai.rag.corpus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagActivationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

            verify(lifecycleService, times(6)).ingestAndStage(any(RagIngestRequestDto.class), anyInt());
            verify(lifecycleService, times(6)).activate(any(RagActivationRequestDto.class));
        });
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
