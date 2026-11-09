package vn.edu.utc.hotel_booking.modules.ai.rag;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.ClientOptions;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.sun.net.httpserver.HttpServer;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.controller.PublicRagKnowledgeController;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RagProviderPrivacyTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final UUID ID = UUID.nameUUIDFromBytes("SYNTHETIC-PUBLIC-CHUNK".getBytes(StandardCharsets.UTF_8));
    private static final LocalDate DATE = LocalDate.of(2026, 11, 20);
    private static final String CONTENT = "Nhận phòng từ 15:00. STATIC_CHUNK_SENTINEL";
    private static final String PRIVATE_BODY = "SYNTHETIC_PRIVATE_PROVIDER_BODY_SENTINEL";

    private RagProviderProperties properties() { return GeminiRagEmbeddingServiceTest.properties(64); }

    private KnowledgeChunk chunk() {
        return KnowledgeChunk.builder().chunkId(ID).documentId(ID).sourceId("SYNTHETIC-CHECKIN").version(1)
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(1).category(RagDocumentCategory.POLICY)
                .status(RagVersionStatus.ACTIVE).effectiveFrom(LocalDate.of(2026, 1, 1)).embedding(new float[]{1, 0, 0})
                .content(CONTENT).sanitizedContent(CONTENT).build();
    }

    private ApplicationContextRunner provider(Wire wire) {
        return new ApplicationContextRunner().withUserConfiguration(RagProviderConfiguration.class)
                .withPropertyValues("hotel.ai.rag.provider.enabled=true", "hotel.ai.rag.provider.chat-model=synthetic-chat",
                        "hotel.ai.rag.provider.embedding-model=synthetic-embedding", "hotel.ai.rag.provider.embedding-dimension=3")
                .withInitializer(ctx -> {
                    ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("synthetic-only-key",
                            Map.of("RAG_GEMINI_API_KEY", "synthetic-sdk-authentication-key")));
                    ctx.addBeanFactoryPostProcessor(factory -> {
                        factory.registerSingleton("ragChatClient", wire.client("synthetic-chat", 7000));
                        factory.registerSingleton("ragEmbeddingClient", wire.client("synthetic-embedding", 3000));
                    });
                });
    }

    private ScopedRagRetrievalService retrievalFixture() {
        var retrieval = mock(ScopedRagRetrievalService.class);
        when(retrieval.retrieve(any())).thenReturn(List.of(new ScopedRagRetrievalService.ScoredChunk(chunk(), .90)));
        return retrieval;
    }

    private PublicRagKnowledgeController controller(ScopedRagRetrievalService retrieval, ChatModel model, RagRequestBudget budget) {
        var generator = new GeminiRagAnswerGenerator(model, properties(), new MockEnvironment());
        return new PublicRagKnowledgeController(new RagGroundedAnswerService(retrieval, new RagCorpusGovernanceValidator(), generator), id -> id == 1, budget);
    }

    private RagQueryRequestDto ask(String question) {
        return RagQueryRequestDto.builder().hotelId(1).question(question).language("vi").asOfDate(DATE).build();
    }

    private static String chatBody() throws Exception {
        String answer = JSON.writeValueAsString(Map.of("answer", "Nhận phòng từ 15:00.", "references",
                List.of(Map.of("chunkId", ID.toString(), "supportingQuote", "Nhận phòng từ 15:00."))));
        return JSON.writeValueAsString(Map.of("candidates", List.of(Map.of("index", 0, "content",
                Map.of("role", "model", "parts", List.of(Map.of("text", answer))), "finishReason", "STOP")),
                "modelVersion", "synthetic-chat", "usageMetadata", Map.of("promptTokenCount", 7, "candidatesTokenCount", 13, "totalTokenCount", 20)));
    }

    @Test void embeddingAndGenerationUseOnlyRedactedPayloadsAndMetadataOnlyAuditLogs() throws Exception {
        String question = "Giờ nhận phòng? CCCD 001200001234, phone 0901234567, privacy@example.invalid, api_key=SYNTHETIC_USER_KEY_SENTINEL";
        try (var logs = new Logs(); var wire = new Wire(200, chatBody(), false)) {
            provider(wire).run(ctx -> {
                var store = mock(RagVectorStorePort.class);
                KnowledgeChunk chunk = chunk();
                when(store.findCandidateChunks(any(), eq(1), eq(DATE))).thenReturn(List.of(chunk));
                when(store.getExpectedDimension()).thenReturn(3);
                when(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, "SYNTHETIC-CHECKIN", 1))
                        .thenReturn(Optional.of(KnowledgeDocumentVersion.builder().documentId(ID).sourceId(chunk.getSourceId()).version(1)
                                .scopeType(chunk.getScopeType()).hotelId(1).category(chunk.getCategory()).status(chunk.getStatus())
                                .effectiveFrom(chunk.getEffectiveFrom()).embeddingModelId("synthetic-embedding").embeddingDimension(3).build()));
                var embedding = new GeminiRagEmbeddingService(ctx.getBean(EmbeddingModel.class), properties());
                var retrieval = new ScopedRagRetrievalService(store, embedding, properties());
                var budget = new RagRequestBudget();
                var response = controller(retrieval, ctx.getBean(ChatModel.class), budget).ask(ask(question));
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getResult().getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
                assertThat(response.getBody().getResult().getWarnings()).doesNotContain("GENERATION_FALLBACK_EXTRACTIVE");
                assertThat(budget.activeRequests()).isZero();
            });
            assertThat(wire.requests).hasSize(2);
            String outbound = wire.requests.toString();
            assertThat(outbound).doesNotContain("001200001234", "0901234567", "privacy@example.invalid", "SYNTHETIC_USER_KEY_SENTINEL", "synthetic-sdk-authentication-key")
                    .contains("REDACTED_CCCD", "REDACTED_PHONE", "REDACTED_EMAIL", "REDACTED_CREDENTIAL");
            assertThat(wire.requests.get(1).path("tools").isMissingNode()).isTrue();
            String audit = logs.text();
            assertThat(audit).contains("RAG_REQUEST", "RAG_PROVIDER", "model=synthetic-chat", "model=synthetic-embedding", "durationMs=",
                    "inputTokens=7", "outputTokens=13", "totalTokens=20", "outcome=SUCCESS")
                    .doesNotContain(question, "001200001234", "0901234567", "privacy@example.invalid", "SYNTHETIC_USER_KEY_SENTINEL",
                            "STATIC_CHUNK_SENTINEL", "rag-answer-v1", "synthetic-sdk-authentication-key", PRIVATE_BODY);
            var ids = new HashSet<String>(); var matcher = Pattern.compile("requestId=([a-f0-9-]{36})").matcher(audit);
            while (matcher.find()) ids.add(matcher.group(1));
            assertThat(ids).hasSize(1);
        }
    }

    @Test void provider503IsNotRetriedAndItsBodyDoesNotReachLogsOrApi() throws Exception {
        try (var logs = new Logs(); var wire = new Wire(503, "{\"error\":{\"code\":503,\"message\":\"" + PRIVATE_BODY + "\"}}", false)) {
            provider(wire).run(ctx -> {
                var response = controller(retrievalFixture(), ctx.getBean(ChatModel.class), new RagRequestBudget()).ask(ask("Giờ nhận phòng?"));
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getResult().getWarnings()).contains("GENERATION_FALLBACK_EXTRACTIVE");
                assertThat(response.getBody().getResult().getAnswer()).contains("15:00").doesNotContain(PRIVATE_BODY);
            });
            assertThat(wire.requests).hasSize(1);
            assertThat(logs.text()).contains("outcome=PROVIDER_ERROR", "inputTokens=-1").doesNotContain(PRIVATE_BODY, "STATIC_CHUNK_SENTINEL");
        }
    }

    @Test void unsolicitedProviderToolCallCannotExecuteOrStartASecondRequest() throws Exception {
        String toolResponse = "{\"candidates\":[{\"index\":0,\"content\":{\"role\":\"model\",\"parts\":[{\"functionCall\":{\"name\":\"create_booking\",\"args\":{}}}]},\"finishReason\":\"STOP\"}]}";
        try (var wire = new Wire(200, toolResponse, false)) {
            provider(wire).run(ctx -> {
                var response = controller(retrievalFixture(), ctx.getBean(ChatModel.class), new RagRequestBudget()).ask(ask("Giờ nhận phòng?"));
                assertThat(response.getBody().getResult().getWarnings()).contains("GENERATION_FALLBACK_EXTRACTIVE");
                assertThat(ctx.getBean(ChatModel.class).getDefaultOptions().getModel()).isEqualTo("synthetic-chat");
            });
            assertThat(wire.requests).hasSize(1);
            assertThat(wire.requests.getFirst().path("tools").isMissingNode()).isTrue();
        }
    }

    @Test void fullSdkTimeoutCancelsSocketBefore503AndFreesTheSlotAfterWorkEnds() throws Exception {
        try (var logs = new Logs(); var wire = new Wire(200, "{", true)) {
            provider(wire).run(ctx -> {
                var budget = new RagRequestBudget(1, 20, Duration.ofMinutes(1), Duration.ofMillis(400), System::nanoTime);
                var controller = controller(retrievalFixture(), ctx.getBean(ChatModel.class), budget);
                try (var worker = Executors.newSingleThreadExecutor()) {
                    var result = worker.submit(() -> controller.ask(ask("Giờ nhận phòng?")));
                    try {
                        assertThat(wire.entered.await(2, TimeUnit.SECONDS)).isTrue();
                        var response = result.get(3, TimeUnit.SECONDS);
                        assertThat(response.getStatusCode().value()).isEqualTo(503);
                        assertThat(response.getBody().getResult().getStatus()).isEqualTo(RagAnswerStatus.INSUFFICIENT_EVIDENCE);
                        assertThat(wire.cancellations.get()).isPositive(); assertThat(budget.activeRequests()).isZero();
                        assertThat(wire.requests).hasSize(1);
                        assertThat(budget.execute(() -> "RECOVERED")).isEqualTo("RECOVERED");
                    } catch (Exception failure) { throw new AssertionError("Offline cancellation failed", failure); }
                    finally { wire.release.countDown(); }
                }
            });
            assertThat(logs.text()).contains("outcome=DEADLINE").doesNotContain("STATIC_CHUNK_SENTINEL", PRIVATE_BODY);
        }
    }

    @Test void publicAskUsesReadOnlyStoreCallsAndNeverInvokesLifecycleWrites() {
        var governance = new RagCorpusGovernanceValidator(); var embedding = new HybridRagEmbeddingService();
        var store = spy(new InMemoryScopedVectorStore());
        var lifecycle = spy(new RagKnowledgeLifecycleService(governance, embedding, store));
        var ingest = RagIngestRequestDto.builder().sourceId("SYNTHETIC-LOCAL").hotelId(1).scopeType(RagScopeType.HOTEL_SCOPED)
                .category(RagDocumentCategory.POLICY).title("Synthetic check-in").content("Nhận phòng từ 15:00.").effectiveFrom(DATE.minusDays(1)).build();
        lifecycle.ingestAndStage(ingest, 1);
        assertThat(lifecycle.activate(RagActivationRequestDto.builder().sourceId("SYNTHETIC-LOCAL").hotelId(1)
                .scopeType(RagScopeType.HOTEL_SCOPED).version(1).build())).isTrue();
        clearInvocations(store, lifecycle);
        var controller = new PublicRagKnowledgeController(new RagGroundedAnswerService(new ScopedRagRetrievalService(store, embedding), governance), id -> true);
        assertThat(controller.ask(ask("Giờ nhận phòng?")).getBody().getResult().getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(controller.ask(ask("Create a booking now")).getBody().getResult().getStatus()).isEqualTo(RagAnswerStatus.OUT_OF_SCOPE);
        verifyNoInteractions(lifecycle);
        assertThat(mockingDetails(store).getInvocations()).allSatisfy(invocation -> assertThat(invocation.getMethod().getName())
                .isIn("findCandidateChunks", "getExpectedDimension", "getDocument", "getActiveDocument"));
        verify(store, never()).stageDocument(any(), any()); verify(store, never()).clearAll();
    }

    @Test void controllerCopiesAndRedactsBeforeServiceWithoutMutatingTheOriginalRequest() {
        var service = mock(RagGroundedAnswerService.class);
        when(service.answerQuestion(any())).thenReturn(RagQueryResultDto.builder().answer("Synthetic answer").build());
        var request = ask("CCCD 001200001234 privacy@example.invalid Bearer SYNTHETIC_TOKEN api_key=SYNTHETIC_SECRET nhận phòng?");
        String original = request.getQuestion();
        new PublicRagKnowledgeController(service, id -> true).ask(request);
        var capture = org.mockito.ArgumentCaptor.forClass(RagQueryRequestDto.class); verify(service).answerQuestion(capture.capture());
        assertThat(capture.getValue()).isNotSameAs(request); assertThat(request.getQuestion()).isEqualTo(original);
        assertThat(capture.getValue().getQuestion()).doesNotContain("001200001234", "privacy@example.invalid", "SYNTHETIC_TOKEN", "SYNTHETIC_SECRET");
        assertThat(capture.getValue().getHotelId()).isEqualTo(1); assertThat(capture.getValue().getAsOfDate()).isEqualTo(DATE);
    }

    private static final class Logs implements AutoCloseable {
        private final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        Logs() { appender.list = Collections.synchronizedList(new ArrayList<>()); appender.start(); root.addAppender(appender); }
        String text() {
            return String.join("\n", appender.list.stream().map(event -> event.getFormattedMessage()
                    + (event.getThrowableProxy() == null ? "" : ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(event.getThrowableProxy()))).toList());
        }
        @Override public void close() { root.detachAppender(appender); appender.stop(); }
    }

    /** No real keys, no recorded headers, and a guard that refuses every non-loopback destination. */
    private static final class Wire implements AutoCloseable {
        private final HttpServer server;
        private final List<JsonNode> requests = Collections.synchronizedList(new ArrayList<>());
        private final List<Client> clients = new ArrayList<>();
        private final List<OkHttpClient> transports = new ArrayList<>();
        private final AtomicInteger cancellations = new AtomicInteger();
        private final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        Wire(int status, String chat, boolean stall) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                requests.add(JSON.readTree(exchange.getRequestBody()));
                String response = exchange.getRequestURI().getPath().toLowerCase(Locale.ROOT).contains("embed")
                        ? "{\"embeddings\":[{\"values\":[1,0,0]}],\"usageMetadata\":{\"promptTokenCount\":11,\"totalTokenCount\":11}}" : chat;
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, stall ? 0 : bytes.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(bytes); output.flush(); entered.countDown();
                    if (stall) release.await(4, TimeUnit.SECONDS);
                } catch (Exception ignored) { } finally { exchange.close(); }
            });
            server.start();
        }
        Client client(String model, int timeout) {
            var transport = RagRequestBudget.providerHttpClient(model, timeout).newBuilder().addInterceptor(chain -> {
                if (!"127.0.0.1".equals(chain.request().url().host()) || chain.request().url().port() != server.getAddress().getPort())
                    throw new java.io.IOException("Offline test blocked external destination");
                return chain.proceed(chain.request());
            }).eventListener(new EventListener() { @Override public void canceled(Call call) { cancellations.incrementAndGet(); } }).build();
            transports.add(transport);
            var client = Client.builder().apiKey("synthetic-sdk-authentication-key").vertexAI(false)
                    .clientOptions(ClientOptions.builder().customHttpClient(transport).build())
                    .httpOptions(HttpOptions.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).apiVersion("v1beta")
                            .timeout(timeout).retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build();
            clients.add(client); return client;
        }
        @Override public void close() {
            release.countDown(); clients.forEach(Client::close); server.stop(0);
            transports.forEach(transport -> { transport.connectionPool().evictAll(); transport.dispatcher().executorService().shutdownNow(); });
        }
    }
}
