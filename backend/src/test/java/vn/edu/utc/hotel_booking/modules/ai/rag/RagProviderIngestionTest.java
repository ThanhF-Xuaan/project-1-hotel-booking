package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.embedding.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.utc.hotel_booking.modules.ai.rag.controller.AdminRagLifecycleController;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RagProviderIngestionTest {
    private final EmbeddingModel model = mock(EmbeddingModel.class);
    private final InMemoryScopedVectorStore store = spy(new InMemoryScopedVectorStore(3));

    private RagKnowledgeLifecycleService lifecycle(int batchSize) {
        return new RagKnowledgeLifecycleService(new RagCorpusGovernanceValidator(),
                new GeminiRagEmbeddingService(model, GeminiRagEmbeddingServiceTest.properties(batchSize)), store);
    }

    private static RagIngestRequestDto request(String content) {
        return RagIngestRequestDto.builder().sourceId("SYNTHETIC-POLICY")
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(1).category(RagDocumentCategory.POLICY)
                .title("Synthetic check-in policy").content(content).build();
    }

    private void activate(RagKnowledgeLifecycleService service, int version) {
        assertThat(service.activate(RagActivationRequestDto.builder().scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1).sourceId("SYNTHETIC-POLICY").version(version).build())).isTrue();
    }

    private void assertNoVersionTwoAndOldVersionStillActive() {
        assertThat(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, "SYNTHETIC-POLICY", 2)).isEmpty();
        assertThat(store.getActiveDocument(RagScopeType.HOTEL_SCOPED, 1, "SYNTHETIC-POLICY").orElseThrow().getVersion())
                .isEqualTo(1);
        assertThat(store.findCandidateChunks(null, 1, LocalDate.of(2026, 11, 4))).hasSize(1);
    }

    @Test void completeBatchIsValidatedBeforeOneAtomicStageAndQueryUsesQueryOperation() {
        when(model.call(any())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            verify(store, never()).stageDocument(any(), anyList());
            var req = (EmbeddingRequest) call.getArgument(0);
            assertThat(req.getInstructions()).hasSize(2);
            return GeminiRagEmbeddingServiceTest.response(new float[]{3, 4, 0}, new float[]{0, 3, 4});
        });
        var service = lifecycle(64);
        var result = service.ingestAndStage(request("Nhận phòng từ 15:00.\n\nTrả phòng trước 11:00."), 1);
        assertThat(result.getStatus()).isEqualTo(RagVersionStatus.STAGED);
        assertThat(result.getChunkCount()).isEqualTo(2);
        verify(store, times(1)).stageDocument(any(), argThat(chunks -> chunks.size() == 2
                && Math.abs(chunks.getFirst().getEmbedding()[0] - 0.6f) < 0.0001));
        activate(service, 1);
        reset(model);
        when(model.call(any())).thenReturn(GeminiRagEmbeddingServiceTest.response(new float[]{1, 0, 0}));
        var retrieval = new ScopedRagRetrievalService(store,
                new GeminiRagEmbeddingService(model, GeminiRagEmbeddingServiceTest.properties(64)));
        assertThat(retrieval.retrieve(RagQueryRequestDto.builder().hotelId(1).question("Giờ nhận phòng?")
                .asOfDate(LocalDate.of(2026, 11, 4)).build())).isNotEmpty();
        var query = org.mockito.ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(model).call(query.capture());
        assertThat(((org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions) query.getValue().getOptions())
                .getTaskType().name()).isEqualTo("RETRIEVAL_QUERY");
    }

    static Stream<String> oversizedDocuments() {
        return Stream.of("X".repeat(2001), "Nhận phòng từ 15:00.\n\n" + "X".repeat(2001),
                String.join("\n\n", Collections.nCopies(65, "Chính sách synthetic.")));
    }

    @ParameterizedTest @MethodSource("oversizedDocuments")
    void allParagraphsAndChunkCountAreCheckedBeforeOutbound(String content) {
        assertThat(lifecycle(1).ingestAndStage(request(content), 1).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        verifyNoInteractions(model);
        verify(store, never()).stageDocument(any(), anyList());
    }

    @Test void duplicatePrecheckConsumesNoProviderQuotaAndDoesNotTouchActiveVersion() {
        when(model.call(any())).thenReturn(GeminiRagEmbeddingServiceTest.response(new float[]{1, 0, 0}));
        var service = lifecycle(64);
        assertThat(service.ingestAndStage(request("Nhận phòng từ 15:00."), 1).getStatus()).isEqualTo(RagVersionStatus.STAGED);
        activate(service, 1); clearInvocations(model, store);
        var duplicate = service.ingestAndStage(request("Nhận phòng từ 16:00."), 1);
        assertThat(duplicate.getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        assertThat(duplicate.getErrorMessage()).isEqualTo("REJECTED_DUPLICATE_VERSION");
        verifyNoInteractions(model); verify(store, never()).stageDocument(any(), anyList());
        assertThat(store.getActiveDocument(RagScopeType.HOTEL_SCOPED, 1, "SYNTHETIC-POLICY").orElseThrow().getRawContent())
                .contains("15:00").doesNotContain("16:00");
    }

    @Test void secondBatchFailureLeavesNoPartialVersionOrChunksAndNoTransactionAroundProvider() {
        var service = lifecycle(1);
        when(model.call(any())).thenReturn(GeminiRagEmbeddingServiceTest.response(new float[]{1, 0, 0}));
        service.ingestAndStage(request("Nhận phòng từ 15:00."), 1); activate(service, 1);
        reset(model); clearInvocations(store);
        when(model.call(any())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return GeminiRagEmbeddingServiceTest.response(new float[]{1, 0, 0});
        }).thenThrow(new IllegalStateException("SYNTHETIC_PRIVATE_PROVIDER_BODY"));
        assertThatThrownBy(() -> service.ingestAndStage(request("Nhận phòng từ 16:00.\n\nTrả phòng trước 11:00."), 2))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class).hasNoCause();
        verify(model, times(2)).call(any()); verify(store, never()).stageDocument(any(), anyList());
        assertNoVersionTwoAndOldVersionStillActive();
    }

    @ParameterizedTest @MethodSource("vn.edu.utc.hotel_booking.modules.ai.rag.GeminiRagEmbeddingServiceTest#invalidVectors")
    void invalidProviderVectorsCannotReplaceAnActiveCorpus(float[] invalid) {
        var service = lifecycle(64);
        when(model.call(any())).thenReturn(GeminiRagEmbeddingServiceTest.response(new float[]{1, 0, 0}));
        service.ingestAndStage(request("Nhận phòng từ 15:00."), 1); activate(service, 1);
        reset(model); clearInvocations(store);
        when(model.call(any())).thenReturn(GeminiRagEmbeddingServiceTest.response(invalid));
        assertThatThrownBy(() -> service.ingestAndStage(request("Nhận phòng từ 16:00."), 2))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
        verify(store, never()).stageDocument(any(), anyList());
        assertNoVersionTwoAndOldVersionStillActive();
    }

    @Test void wrongStoreDimensionFailsBeforeProviderAndCannotFallBackToHashing() {
        var localStore = spy(new InMemoryScopedVectorStore(256));
        var service = new RagKnowledgeLifecycleService(new RagCorpusGovernanceValidator(),
                new GeminiRagEmbeddingService(model, GeminiRagEmbeddingServiceTest.properties(64)), localStore);
        assertThatThrownBy(() -> service.ingestAndStage(request("Nhận phòng từ 15:00."), 1))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
        verifyNoInteractions(model); verify(localStore, never()).stageDocument(any(), anyList());
    }

    @Test void providerCountMismatchCannotLeaveAStagedVersionOrRemoveOldActiveChunks() {
        var service = lifecycle(64);
        when(model.call(any())).thenReturn(GeminiRagEmbeddingServiceTest.response(new float[]{1, 0, 0}));
        service.ingestAndStage(request("Nhận phòng từ 15:00."), 1); activate(service, 1);
        reset(model); clearInvocations(store);
        when(model.call(any())).thenReturn(new EmbeddingResponse(List.of()));
        assertThatThrownBy(() -> service.ingestAndStage(request("Nhận phòng từ 16:00."), 2))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
        verify(store, never()).stageDocument(any(), anyList());
        assertNoVersionTwoAndOldVersionStillActive();
    }

    @Test void exactParagraphAndChunkLimitsAreAcceptedWithoutTruncatingContent() {
        when(model.call(any())).thenAnswer(call -> {
            var req = (EmbeddingRequest) call.getArgument(0);
            return new EmbeddingResponse(java.util.stream.IntStream.range(0, req.getInstructions().size())
                    .mapToObj(i -> new Embedding(new float[]{1, 0, 0}, i)).toList());
        });
        var service = lifecycle(64);
        String paragraph = "Nội dung synthetic. " + "X".repeat(1980);
        assertThat(paragraph).hasSize(2000);
        assertThat(service.ingestAndStage(request(paragraph), 1).getStatus()).isEqualTo(RagVersionStatus.STAGED);
        assertThat(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, "SYNTHETIC-POLICY", 1).orElseThrow().getRawContent())
                .isEqualTo(paragraph);
        var result = service.ingestAndStage(request(String.join("\n\n", Collections.nCopies(64, "Chính sách synthetic."))), 2);
        assertThat(result.getStatus()).isEqualTo(RagVersionStatus.STAGED);
        assertThat(result.getChunkCount()).isEqualTo(64);
    }

    @Test void concurrentPrechecksStillRelyOnAtomicStoreDuplicateProtection() throws Exception {
        var service = lifecycle(64);
        var barrier = new CyclicBarrier(2);
        when(model.call(any())).thenAnswer(call -> {
            barrier.await(5, TimeUnit.SECONDS);
            return GeminiRagEmbeddingServiceTest.response(new float[]{1, 0, 0});
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> service.ingestAndStage(request("Nhận phòng từ 15:00."), 1));
            var second = workers.submit(() -> service.ingestAndStage(request("Nhận phòng từ 16:00."), 1));
            assertThat(List.of(first.get(10, TimeUnit.SECONDS).getStatus(), second.get(10, TimeUnit.SECONDS).getStatus()))
                    .containsExactlyInAnyOrder(RagVersionStatus.STAGED, RagVersionStatus.REJECTED);
            assertThat(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, "SYNTHETIC-POLICY", 1)).isPresent();
            assertThat(store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, "SYNTHETIC-POLICY", 1)).isTrue();
            assertThat(store.findCandidateChunks(null, 1, LocalDate.of(2026, 11, 4))).hasSize(1);
        }
        finally { workers.shutdownNow(); }
    }

    @Test void stageProviderFailureReturnsGeneric503ButInvalidInputRemains400() throws Exception {
        when(model.call(any())).thenThrow(new IllegalStateException("SYNTHETIC_PRIVATE_PROVIDER_BODY"));
        var mvc = MockMvcBuilders.standaloneSetup(new AdminRagLifecycleController(lifecycle(64))).build();
        var mapper = new ObjectMapper();
        mvc.perform(post("/api/v1/ai/admin/knowledge/stage").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request("Nhận phòng từ 15:00."))))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("RAG_TEMP_UNAVAILABLE")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SYNTHETIC_PRIVATE_PROVIDER_BODY"))));
        clearInvocations(model);
        mvc.perform(post("/api/v1/ai/admin/knowledge/stage").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request("X".repeat(2001)))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(model);
        mvc.perform(post("/api/v1/ai/admin/knowledge/stage").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request(""))))
                .andExpect(status().isBadRequest());
    }
}
