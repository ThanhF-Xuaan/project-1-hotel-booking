package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.ClientOptions;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderConfiguration;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderProperties;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RagProviderConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(RagProviderConfiguration.class)
            .withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("synthetic-rag-key", Map.of("RAG_GEMINI_API_KEY", "synthetic-test-key"))));

    private ApplicationContextRunner enabled() {
        return context.withPropertyValues("hotel.ai.rag.provider.enabled=true",
                "hotel.ai.rag.provider.chat-model=gemini-3.5-flash-lite",
                "hotel.ai.rag.provider.embedding-model=gemini-embedding-001",
                "hotel.ai.rag.provider.embedding-dimension=768");
    }

    @Test void defaultContextDoesNotNeedAKeyOrCreateAnyProviderClient() {
        try (var sdk = mockStatic(Client.class)) {
            context.withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("no-key", Map.of("RAG_GEMINI_API_KEY", ""))))
                    .withBean(RagEmbeddingPort.class, HybridRagEmbeddingService::new)
                    .run(ctx -> {
                        assertThat(ctx).hasNotFailed().doesNotHaveBean(Client.class)
                                .doesNotHaveBean(ChatModel.class).doesNotHaveBean(EmbeddingModel.class);
                        assertThat(ctx).hasSingleBean(RagEmbeddingPort.class);
                        assertThat(ctx.getBean(RagProviderProperties.class).isEnabled()).isFalse();
                    });
            sdk.verifyNoInteractions();
        }
    }

    @Test void keyAloneOrExplicitFalseCannotEnableOutbound() {
        try (var sdk = mockStatic(Client.class)) {
            context.withPropertyValues("hotel.ai.rag.provider.enabled=false").run(ctx ->
                    assertThat(ctx).hasNotFailed().doesNotHaveBean(Client.class).doesNotHaveBean(ChatModel.class));
            context.run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(EmbeddingModel.class));
            sdk.verifyNoInteractions();
        }
    }

    @Test void enabledModelsAreUniqueLazyAndHaveNoToolsOrImplicitModelDefaults() {
        enabled().run(ctx -> {
            assertThat(ctx).hasNotFailed().hasSingleBean(ChatModel.class).hasSingleBean(EmbeddingModel.class);
            var factory = ctx.getSourceApplicationContext().getBeanFactory();
            for (String bean : List.of("ragChatModel", "ragEmbeddingModel", "ragChatClient", "ragEmbeddingClient")) {
                assertThat(factory.containsSingleton(bean)).as(bean + " must remain lazy").isFalse();
            }
            var chat = ctx.getBean(GoogleGenAiChatModel.class);
            assertThat(chat.getOptions().getModel()).isEqualTo("gemini-3.5-flash-lite");
            assertThat(chat.getOptions().getMaxOutputTokens()).isEqualTo(768);
            assertThat(chat.getOptions().getToolCallbacks()).isEmpty();
            assertThat(chat.getOptions().getGoogleSearchRetrieval()).isFalse();
            assertThat(ctx.getBean(EmbeddingModel.class).dimensions()).isEqualTo(768);
        });
    }

    @Test void enablingWithoutExplicitModelConfigurationFailsDuringContextRefresh() {
        context.withPropertyValues("hotel.ai.rag.provider.enabled=true").run(ctx ->
                assertThat(ctx).hasFailed().getFailure().hasStackTraceContaining("chat-model must be an explicit model ID"));
    }

    @Test void enablingWithoutDedicatedKeyCannotUseGoogleOrAdvisorKeys() {
        enabled().withPropertyValues("GOOGLE_API_KEY=synthetic-other-key", "GEMINI_API_KEY=synthetic-other-key")
                .withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(
                        new MapPropertySource("missing-dedicated-key", Map.of("RAG_GEMINI_API_KEY", ""))))
                .run(ctx -> {
                    assertThat(ctx).hasFailed().getFailure().hasStackTraceContaining("RAG_GEMINI_API_KEY is required");
                    var trace = new java.io.StringWriter();
                    ctx.getStartupFailure().printStackTrace(new java.io.PrintWriter(trace));
                    assertThat(trace.toString()).doesNotContain("synthetic-other-key");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"chat-model=", "chat-model=https://invalid.invalid/model", "embedding-model=",
            "embedding-dimension=0", "embedding-dimension=3073", "embedding-timeout-millis=99",
            "embedding-timeout-millis=3001", "chat-timeout-millis=7001", "request-timeout-millis=9999",
            "request-timeout-millis=12001", "max-output-tokens=0", "max-output-tokens=769",
            "max-embedding-inputs=65", "max-embedding-input-characters=2501"})
    void invalidEnabledSettingsFailBeforeAnySdkCreation(String setting) {
        try (var sdk = mockStatic(Client.class)) {
            enabled().withPropertyValues("hotel.ai.rag.provider." + setting)
                    .run(ctx -> assertThat(ctx).hasFailed());
            sdk.verifyNoInteractions();
        }
    }

    @Test void queryAndDocumentTaskTypesAndDimensionReachTheWire() throws Exception {
        try (var wire = new Wire(200, embeddingBody(768))) {
            enabled().withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("ragEmbeddingClient", wire.client(3000)))).run(ctx -> {
                var model = ctx.getBean(EmbeddingModel.class);
                for (var task : List.of(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_QUERY,
                        GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT)) {
                    var response = model.call(new EmbeddingRequest(List.of("Synthetic public check-in policy."),
                            GoogleGenAiTextEmbeddingOptions.builder().taskType(task).build()));
                    assertThat(response.getResults()).hasSize(1);
                    assertThat(response.getResult().getOutput()).hasSize(768);
                    assertThat(response.getMetadata().getModel()).isEqualTo("gemini-embedding-001");
                }
            });
            assertThat(wire.requests).hasSize(2);
            // SDK 1.65 serializes embedContent(List<String>) as nested batch requests.
            assertThat(wire.requests.get(0).findPath("taskType").asText()).isEqualTo("RETRIEVAL_QUERY");
            assertThat(wire.requests.get(1).findPath("taskType").asText()).isEqualTo("RETRIEVAL_DOCUMENT");
            assertThat(wire.requests.get(0).findPath("outputDimensionality").asInt()).isEqualTo(768);
        }
    }

    @Test void malformedInputAndIdentityOverridesAreRejectedWithoutOutbound() throws Exception {
        try (var wire = new Wire(200, embeddingBody(768))) {
            enabled().withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("ragEmbeddingClient", wire.client(3000)))).run(ctx -> {
                var model = ctx.getBean(EmbeddingModel.class);
                assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of(""), null)))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of("X".repeat(2501)), null)))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> model.call(new EmbeddingRequest(Collections.nCopies(65, "Synthetic"), null)))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of("Synthetic"),
                        GoogleGenAiTextEmbeddingOptions.builder().model("different-model").build())))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of("Synthetic"),
                        GoogleGenAiTextEmbeddingOptions.builder().dimensions(256).build())))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> model.call(new EmbeddingRequest(List.of("Synthetic"),
                        GoogleGenAiTextEmbeddingOptions.builder()
                                .taskType(GoogleGenAiTextEmbeddingOptions.TaskType.CLASSIFICATION).build())))
                        .isInstanceOf(IllegalArgumentException.class);
            });
            assertThat(wire.calls.get()).isZero();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"embeddings\":[]}", "{\"embeddings\":[{\"values\":[1.0,0.0]}]}"})
    void invalidEmbeddingResponsesAreRejected(String response) throws Exception {
        try (var wire = new Wire(200, response)) {
            enabled().withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("ragEmbeddingClient", wire.client(3000)))).run(ctx ->
                    assertThatThrownBy(() -> ctx.getBean(EmbeddingModel.class).embed("Synthetic policy"))
                            .isInstanceOf(IllegalStateException.class));
            assertThat(wire.calls.get()).isEqualTo(1);
        }
    }

    @Test void embeddingProviderErrorsAreNotRetried() throws Exception {
        try (var wire = new Wire(503, "{\"error\":{\"code\":503,\"message\":\"Synthetic unavailable\"}}")) {
            enabled().withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("ragEmbeddingClient", wire.client(3000)))).run(ctx ->
                    assertThatThrownBy(() -> ctx.getBean(EmbeddingModel.class).embed("Synthetic policy"))
                            .isInstanceOf(RuntimeException.class));
            assertThat(wire.calls.get()).isEqualTo(1);
        }
    }

    @Test void chatProviderErrorsAreNotRetriedAndToolsAreNotAdvertised() throws Exception {
        try (var wire = new Wire(503, "{\"error\":{\"code\":503,\"message\":\"Synthetic unavailable\"}}")) {
            enabled().withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("ragChatClient", wire.client(7000)))).run(ctx ->
                    assertThatThrownBy(() -> ctx.getBean(ChatModel.class).call(new Prompt("Synthetic check-in policy")))
                            .isInstanceOf(RuntimeException.class).hasRootCauseInstanceOf(com.google.genai.errors.ServerException.class));
            assertThat(wire.calls.get()).isEqualTo(1);
            assertThat(wire.requests.getFirst().path("tools").isMissingNode()).isTrue();
            assertThat(wire.requests.getFirst().path("generationConfig").path("maxOutputTokens").asInt())
                    .isEqualTo(768);
        }
    }

    @Test void structuredChatSchemaReachesTheWireAndResponseRemainsTyped() throws Exception {
        String response = """
                {"candidates":[{"index":0,"content":{"role":"model","parts":[{"text":"{\\"answer\\":\\"15:00\\",\\"sourceId\\":\\"SYNTHETIC-POC\\"}"}]},"finishReason":"STOP"}],
                 "modelVersion":"synthetic-chat","usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":20,"totalTokenCount":30}}
                """;
        try (var wire = new Wire(200, response)) {
            enabled().withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(factory ->
                    factory.registerSingleton("ragChatClient", wire.client(7000)))).run(ctx -> {
                var result = ctx.getBean(ChatModel.class).call(new Prompt("Synthetic check-in policy",
                        GoogleGenAiChatOptions.builder().model("gemini-3.5-flash-lite")
                                .maxOutputTokens(768).responseMimeType("application/json")
                                .responseSchema("{\"type\":\"object\",\"properties\":{\"answer\":{\"type\":\"string\"},\"sourceId\":{\"type\":\"string\"}},\"required\":[\"answer\",\"sourceId\"]}")
                                .build()));
                assertThat(result.getResult().getOutput().getText())
                        .isEqualTo("{\"answer\":\"15:00\",\"sourceId\":\"SYNTHETIC-POC\"}");
                assertThat(result.getMetadata().getModel()).isEqualTo("synthetic-chat");
                assertThat(result.getResult().getMetadata().getFinishReason()).isEqualTo("STOP");
            });
            assertThat(wire.calls.get()).isEqualTo(1);
            assertThat(wire.requests.getFirst().findPath("responseMimeType").asText()).isEqualTo("application/json");
            assertThat(wire.requests.getFirst().findPath("responseJsonSchema").path("required")).hasSize(2);
            assertThat(wire.requests.getFirst().path("tools").isMissingNode()).isTrue();
        }
    }

    @Test void toolCallbacksAreRejectedBeforeAnHttpRequest() throws Exception {
        try (var wire = new Wire(200, "{}")) {
            var callback = mock(org.springframework.ai.tool.ToolCallback.class);
            when(callback.getToolDefinition()).thenReturn(org.springframework.ai.tool.definition.ToolDefinition.builder()
                    .name("forbidden_write").description("Synthetic forbidden operation").inputSchema("{}").build());
            enabled().withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(factory ->
                    factory.registerSingleton("ragChatClient", wire.client(7000)))).run(ctx ->
                    assertThatThrownBy(() -> ctx.getBean(ChatModel.class).call(new Prompt("Synthetic injection",
                            GoogleGenAiChatOptions.builder().model("gemini-3.5-flash-lite")
                                    .toolCallbacks(List.of(callback)).build())))
                            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not accept tool callbacks"));
            assertThat(wire.calls.get()).isZero();
            verify(callback, never()).call(anyString());
        }
    }

    private static String embeddingBody(int dimension) throws Exception {
        List<Double> values = new ArrayList<>(Collections.nCopies(dimension, 0.0));
        values.set(0, 1.0);
        return new ObjectMapper().writeValueAsString(Map.of("embeddings", List.of(Map.of("values", values))));
    }

    /** Loopback-only synthetic provider. It never records headers, keys or external requests. */
    private static final class Wire implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<JsonNode> requests = Collections.synchronizedList(new ArrayList<>());
        private final List<Client> clients = new ArrayList<>();

        private Wire(int status, String response) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                calls.incrementAndGet();
                requests.add(new ObjectMapper().readTree(exchange.getRequestBody()));
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
                exchange.close();
            });
            server.start();
        }

        private Client client(int timeout) {
            var transport = new okhttp3.OkHttpClient.Builder().addInterceptor(chain -> {
                if (!"127.0.0.1".equals(chain.request().url().host())
                        || chain.request().url().port() != server.getAddress().getPort()) {
                    throw new java.io.IOException("Offline provider test blocked non-loopback request");
                }
                return chain.proceed(chain.request());
            }).build();
            Client client = Client.builder().apiKey("synthetic-loopback-key").vertexAI(false)
                    .clientOptions(ClientOptions.builder().customHttpClient(transport).build())
                    .httpOptions(HttpOptions.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                            .apiVersion("v1beta").timeout(timeout)
                            .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build();
            clients.add(client);
            return client;
        }

        @Override public void close() { clients.forEach(Client::close); server.stop(0); }
    }
}
