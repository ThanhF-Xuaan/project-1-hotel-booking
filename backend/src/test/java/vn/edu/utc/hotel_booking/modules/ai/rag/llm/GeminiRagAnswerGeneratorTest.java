package vn.edu.utc.hotel_booking.modules.ai.rag.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeminiRagAnswerGeneratorTest {

    private static final String TEST_KEY = "synthetic-rag-provider-key-never-sent-to-network";
    private static final List<RagAnswerGenerationPort.Evidence> EVIDENCE = List.of(
            new RagAnswerGenerationPort.Evidence(1, "Chính sách nhận phòng: nhận phòng từ 14:00."),
            new RagAnswerGenerationPort.Evidence(2, "Khách sạn có dịch vụ giữ hành lý tại quầy lễ tân."));

    @Test
    void disabledProviderDoesNotReadCredentialOrCallTransport() {
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked") Supplier<String> key = mock(Supplier.class);
        GeminiRagAnswerGenerator generator = new GeminiRagAnswerGenerator(
                new RagGeminiGenerationProperties(), client, key);

        assertThat(generator.generate("Mấy giờ nhận phòng?", "vi", EVIDENCE)).isEmpty();
        verifyNoInteractions(client, key);
    }

    @Test
    void explicitOptInUsesPinnedModelStructuredOutputAndRedactsPii() throws Exception {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 200, candidate("STOP", """
                {"answer":"Quý khách có thể nhận phòng từ 14:00.","evidenceIds":[1]}
                """));
        GeminiRagAnswerGenerator generator = new GeminiRagAnswerGenerator(enabled(), client, () -> TEST_KEY);

        var result = generator.generate(
                "Mấy giờ nhận phòng? Email synthetic@example.test, CCCD 079190012345", "vi", EVIDENCE);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().answer()).isEqualTo("Quý khách có thể nhận phòng từ 14:00.");
        assertThat(result.orElseThrow().evidenceIds()).containsExactly(1);

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).sendAsync(requestCaptor.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri().toString()).contains("/models/gemini-3.5-flash-lite:generateContent");
        assertThat(request.uri().getQuery()).isNull();
        assertThat(request.headers().firstValue("x-goog-api-key")).contains(TEST_KEY);
        assertThat(request.uri().toString()).doesNotContain(TEST_KEY);

        JsonNode payload = new ObjectMapper().readTree(readBody(request));
        String serialized = payload.toString();
        assertThat(serialized).contains("[REDACTED_EMAIL]", "[REDACTED_CCCD]", "responseJsonSchema")
                .doesNotContain("synthetic@example.test", "079190012345");
        assertThat(payload.path("store").asBoolean()).isFalse();
        assertThat(payload.has("tools")).isFalse();
    }

    @Test
    void unsupportedEvidenceIdOrInventedNumberIsRejected() throws Exception {
        HttpClient badIdClient = mock(HttpClient.class);
        stubResponse(badIdClient, 200, candidate("STOP",
                "{\"answer\":\"Nhận phòng từ 14:00.\",\"evidenceIds\":[99]}"));
        assertThat(new GeminiRagAnswerGenerator(enabled(), badIdClient, () -> TEST_KEY)
                .generate("Mấy giờ nhận phòng?", "vi", EVIDENCE)).isEmpty();

        HttpClient inventedClient = mock(HttpClient.class);
        stubResponse(inventedClient, 200, candidate("STOP",
                "{\"answer\":\"Nhận phòng từ 15:00.\",\"evidenceIds\":[1]}"));
        assertThat(new GeminiRagAnswerGenerator(enabled(), inventedClient, () -> TEST_KEY)
                .generate("Mấy giờ nhận phòng?", "vi", EVIDENCE)).isEmpty();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void providerFailureDoesNotLogCredentialPromptOrResponse(CapturedOutput output) {
        HttpClient client = mock(HttpClient.class);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException(
                        TEST_KEY + " synthetic@example.test Mấy giờ nhận phòng?")));

        assertThat(new GeminiRagAnswerGenerator(enabled(), client, () -> TEST_KEY)
                .generate("Mấy giờ nhận phòng?", "vi", EVIDENCE)).isEmpty();
        assertThat(output.getAll()).doesNotContain(TEST_KEY, "synthetic@example.test", "Mấy giờ nhận phòng?");
        verify(client, times(1)).sendAsync(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void propertiesAreBoundedAndOptInByDefault() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var properties = context.getBean(RagGeminiGenerationProperties.class);
            assertThat(properties.isGeminiEnabled()).isFalse();
            assertThat(properties.getTimeoutMillis()).isEqualTo(6_000);
            assertThat(properties.getMaxOutputTokens()).isEqualTo(512);
        });
        runner.withPropertyValues(
                "hotel.ai.rag.generation.gemini-enabled=true",
                "hotel.ai.rag.generation.timeout-millis=800",
                "hotel.ai.rag.generation.max-output-tokens=256")
                .run(context -> assertThat(context.getBean(RagGeminiGenerationProperties.class)
                        .permitsExternalCall()).isTrue());
    }

    private RagGeminiGenerationProperties enabled() {
        RagGeminiGenerationProperties properties = new RagGeminiGenerationProperties();
        properties.setGeminiEnabled(true);
        properties.setTimeoutMillis(1_000);
        return properties;
    }

    private String candidate(String finishReason, String modelJson) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                "finishReason", finishReason,
                "content", Map.of("parts", List.of(Map.of("text", modelJson)))))));
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(HttpClient client, int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    private String readBody(HttpRequest request) throws Exception {
        HttpResponse.BodySubscriber<String> reader = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { reader.onSubscribe(subscription); }
            @Override public void onNext(ByteBuffer item) { reader.onNext(List.of(item)); }
            @Override public void onError(Throwable throwable) { reader.onError(throwable); }
            @Override public void onComplete() { reader.onComplete(); }
        });
        return reader.getBody().toCompletableFuture().get(1, TimeUnit.SECONDS);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RagGeminiGenerationProperties.class)
    static class PropertiesConfig { }
}
