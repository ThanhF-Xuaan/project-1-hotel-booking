package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.embedding.*;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.*;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiRagEmbeddingServiceTest {
    private final EmbeddingModel model = mock(EmbeddingModel.class);

    static RagProviderProperties properties(int batchSize) {
        var p = new RagProviderProperties();
        p.setEnabled(true); p.setChatModel("synthetic-chat"); p.setEmbeddingModel("synthetic-embedding");
        p.setEmbeddingDimension(3); p.setMaxEmbeddingInputs(batchSize);
        return p;
    }

    static EmbeddingResponse response(float[]... vectors) {
        var results = new ArrayList<Embedding>();
        for (int i = 0; i < vectors.length; i++) results.add(new Embedding(vectors[i], i));
        return new EmbeddingResponse(results);
    }

    @Test void queryAndDocumentUseDistinctTasksAndReturnIndependentNormalizedVectors() {
        float[] providerVector = {3, 4, 0};
        when(model.call(any())).thenReturn(response(providerVector));
        var adapter = new GeminiRagEmbeddingService(model, properties(64));
        assertThat(adapter.embedQuery("Synthetic check-in question")).containsExactly(0.6f, 0.8f, 0);
        assertThat(adapter.embedDocument("Synthetic check-in document")).containsExactly(0.6f, 0.8f, 0);
        assertThat(providerVector).containsExactly(3, 4, 0);
        var requests = org.mockito.ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(model, times(2)).call(requests.capture());
        assertThat(((GoogleGenAiTextEmbeddingOptions) requests.getAllValues().get(0).getOptions()).getTaskType())
                .isEqualTo(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_QUERY);
        var documentOptions = (GoogleGenAiTextEmbeddingOptions) requests.getAllValues().get(1).getOptions();
        assertThat(documentOptions.getTaskType()).isEqualTo(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT);
        assertThat(documentOptions.getModel()).isEqualTo("synthetic-embedding");
        assertThat(documentOptions.getDimensions()).isEqualTo(3);
    }

    static Stream<float[]> invalidVectors() {
        return Stream.of(new float[]{Float.NaN, 0, 0}, new float[]{Float.POSITIVE_INFINITY, 1, 0},
                new float[]{0, 0, 0}, new float[]{1, 0}, new float[]{Float.NEGATIVE_INFINITY, 0, 0});
    }

    @ParameterizedTest @MethodSource("invalidVectors")
    void malformedVectorsNeverEscapeTheAdapter(float[] vector) {
        when(model.call(any())).thenReturn(response(vector));
        assertThatThrownBy(() -> new GeminiRagEmbeddingService(model, properties(64)).embedQuery("Synthetic"))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class).hasNoCause();
        verify(model, times(1)).call(any());
    }

    @Test void nullResponsesCountMismatchAndDuplicateIndicesAreProviderFailures() {
        when(model.call(any())).thenReturn(null, response(new float[]{1, 0, 0}),
                new EmbeddingResponse(List.of(new Embedding(new float[]{1, 0, 0}, 0),
                        new Embedding(new float[]{0, 1, 0}, 0))));
        var adapter = new GeminiRagEmbeddingService(model, properties(64));
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> adapter.embedDocuments(List.of("Synthetic one", "Synthetic two")))
                    .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class).hasNoCause();
        }
    }

    @Test void lastOversizedInputIsRejectedBeforeTheFirstBatchAndNoTextIsTruncated() {
        var adapter = new GeminiRagEmbeddingService(model, properties(1));
        assertThatThrownBy(() -> adapter.embedDocuments(List.of("Synthetic valid", "X".repeat(2501))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.embedDocuments(Collections.nCopies(65, "Synthetic")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.embedQuery("X".repeat(2001))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.embedQuery(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(model);
    }

    @Test void successfulFirstBatchDoesNotReturnPartialDataWhenSecondBatchFails() {
        when(model.call(any())).thenReturn(response(new float[]{1, 0, 0}))
                .thenThrow(new IllegalStateException("SYNTHETIC_PRIVATE_PROVIDER_PAYLOAD"));
        var adapter = new GeminiRagEmbeddingService(model, properties(1));
        assertThatThrownBy(() -> adapter.embedDocuments(List.of("Synthetic one", "Synthetic two")))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class)
                .hasMessage("RAG embedding temporarily unavailable").hasNoCause();
        verify(model, times(2)).call(any()); // No retry or hashing fallback.
    }

    @Test void syntheticPiiIsRedactedBeforeCallingTheModel() {
        when(model.call(any())).thenReturn(response(new float[]{1, 0, 0}));
        new GeminiRagEmbeddingService(model, properties(64)).embedQuery("Liên hệ synthetic@example.invalid để hỏi giờ nhận phòng");
        var request = org.mockito.ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(model).call(request.capture());
        assertThat(request.getValue().getInstructions().getFirst()).contains("[REDACTED_EMAIL]")
                .doesNotContain("synthetic@example.invalid");
    }

    @Test void anAmbientDatabaseTransactionCannotRemainOpenDuringProviderIo() {
        var adapter = new GeminiRagEmbeddingService(model, properties(64));
        boolean previous = org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive();
        try {
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
            assertThatThrownBy(() -> adapter.embedDocument("Synthetic policy"))
                    .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
            verifyNoInteractions(model);
        }
        finally { org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(previous); }
    }

    @Test void legacyImplementationsInheritDocumentAndQueryOperations() {
        RagEmbeddingPort legacy = new RagEmbeddingPort() {
            public float[] embedText(String text) { return new float[]{1}; }
            public List<float[]> embedBatch(List<String> texts) { return texts.stream().map(this::embedText).toList(); }
            public int getDimension() { return 1; }
            public String getModelId() { return "synthetic-legacy"; }
        };
        assertThat(legacy.embedQuery("Question")).containsExactly(1);
        assertThat(legacy.embedDocuments(List.of("Document"))).hasSize(1);
    }

    @Test void springChoosesOnlyOnePortAndReadingGeminiIdentityDoesNotConstructSdkClients() {
        var context = new ApplicationContextRunner().withUserConfiguration(RagProviderConfiguration.class,
                HybridRagEmbeddingService.class, GeminiRagEmbeddingService.class)
                .withPropertyValues("RAG_GEMINI_API_KEY=synthetic-test-key");
        try (var sdk = mockStatic(Client.class)) {
            context.run(ctx -> {
                assertThat(ctx).hasNotFailed().hasSingleBean(RagEmbeddingPort.class);
                assertThat(ctx.getBean(RagEmbeddingPort.class)).isInstanceOf(HybridRagEmbeddingService.class);
            });
            context.withPropertyValues("hotel.ai.rag.provider.enabled=true",
                    "hotel.ai.rag.provider.chat-model=synthetic-chat",
                    "hotel.ai.rag.provider.embedding-model=synthetic-embedding",
                    "hotel.ai.rag.provider.embedding-dimension=768").run(ctx -> {
                assertThat(ctx).hasNotFailed().hasSingleBean(RagEmbeddingPort.class)
                        .doesNotHaveBean(HybridRagEmbeddingService.class);
                var port = ctx.getBean(RagEmbeddingPort.class);
                assertThat(port).isInstanceOf(GeminiRagEmbeddingService.class);
                assertThat(port.getModelId()).isEqualTo("synthetic-embedding");
                assertThat(port.getDimension()).isEqualTo(768);
                assertThat(ctx.getSourceApplicationContext().getBeanFactory().containsSingleton("ragEmbeddingModel")).isFalse();
            });
            sdk.verifyNoInteractions();
        }
    }
}
