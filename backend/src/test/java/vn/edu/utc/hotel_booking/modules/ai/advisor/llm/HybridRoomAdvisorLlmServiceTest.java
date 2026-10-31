package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaPatchDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.time.LocalDate;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HybridRoomAdvisorLlmServiceTest {

    private HybridRoomAdvisorLlmService llmService;

    @BeforeEach
    void setUp() {
        llmService = new HybridRoomAdvisorLlmService();
    }

    @Test
    @DisplayName("Should extract dates, guests, destination, and budget from Vietnamese message")
    void shouldExtractCriteriaFromVietnameseMessage() {
        String msg = "Tìm giúp tôi phòng ở Đà Nẵng từ 2026-11-20 đến 2026-11-22 cho 2 người lớn và 1 trẻ em, ngân sách dưới 2 triệu, có ban công";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getLocationKeyword()).isEqualTo("Đà Nẵng");
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(1);
        assertThat(patch.getPreferredFeatureCodes()).contains("BALCONY");
        assertThat(patch.getBudget()).isNotNull();
        assertThat(patch.getBudget().getAmount()).isEqualTo("2000000");
    }

    @Test
    @DisplayName("Should extract dd/MM/yyyy date format")
    void shouldExtractVnDateFormat() {
        String msg = "Mình cần phòng tại Hà Nội từ 15/12/2026 đến 18/12/2026 cho 1 người lớn";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getLocationKeyword()).isEqualTo("Hà Nội");
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 12, 18));
        assertThat(patch.getAdults()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should extract zero children when message specifies không trẻ em")
    void shouldExtractZeroChildrenWhenNoChildrenSpecified() {
        String msg = "Tìm 1 phòng cho 2 người lớn, không trẻ em tại Synthetic City, 20–22/11/2026, tổng tối đa 950000 VND theo tổng API";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(0);
        assertThat(patch.getLocationKeyword()).isEqualTo("Synthetic City");
    }

    private static final String MESSAGE = "Tìm phòng tại Hà Nội cho 2 người lớn";
    private static final String TEST_KEY = "synthetic-provider-key-never-sent-to-network";

    @Test
    void disabledProviderNeverReadsAnAvailableCredentialOrCallsTransport() {
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked") Supplier<String> key = mock(Supplier.class);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(new AdvisorProviderProperties(), client, key);

        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client, key);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void enabledProviderStillRequiresANonblankKey(String key) {
        HttpClient client = mock(HttpClient.class);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(enabled(), client, () -> key);
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client);
    }

    @Test
    void defaultAndExplicitPropertiesBindAndValidateInSpring() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            AdvisorProviderProperties properties = context.getBean(AdvisorProviderProperties.class);
            assertThat(properties.isGeminiEnabled()).isFalse();
            assertThat(properties.getTimeoutMillis()).isEqualTo(4000);
            assertThat(properties.getMaxOutputTokens()).isEqualTo(512);
        });
        runner.withPropertyValues("hotel.ai.advisor.provider.gemini-enabled=true",
                "hotel.ai.advisor.provider.timeout-millis=500", "hotel.ai.advisor.provider.max-output-tokens=256")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AdvisorProviderProperties properties = context.getBean(AdvisorProviderProperties.class);
                    assertThat(properties.permitsExternalCall()).isTrue();
                    assertThat(properties.getTimeoutMillis()).isEqualTo(500);
                    assertThat(properties.getMaxOutputTokens()).isEqualTo(256);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeout-millis=0", "timeout-millis=4001", "max-output-tokens=0", "max-output-tokens=1025"})
    void invalidProviderBoundsFailSpringStartup(String invalid) {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class)
                .withPropertyValues("hotel.ai.advisor.provider.gemini-enabled=true", "hotel.ai.advisor.provider." + invalid)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void canonicalEnvironmentOptInBindsThroughSpringRelaxedBinding() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class)
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("systemEnvironment", Map.of(
                                "HOTEL_AI_ADVISOR_PROVIDER_GEMINIENABLED", "true"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AdvisorProviderProperties.class).isGeminiEnabled()).isTrue();
                });
    }

    @Test
    void springInjectsTheBoundPropertiesWhileNoArgEvaluationRemainsOffline() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class, HybridRoomAdvisorLlmService.class)
                .withPropertyValues("hotel.ai.advisor.provider.gemini-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(org.springframework.test.util.ReflectionTestUtils.getField(
                            context.getBean(HybridRoomAdvisorLlmService.class), "provider"))
                            .isSameAs(context.getBean(AdvisorProviderProperties.class));
                    assertThat(context.getBean(AdvisorProviderProperties.class).isGeminiEnabled()).isTrue();
                });
        assertThat(new HybridRoomAdvisorLlmService().extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
    }

    @Test
    void manuallyInvalidBoundsFailClosedBeforeReadingCredentials() {
        AdvisorProviderProperties properties = enabled();
        properties.setMaxOutputTokens(1025);
        HttpClient client = mock(HttpClient.class);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(properties, client,
                () -> { throw new AssertionError("Invalid config must not read the key"); });
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client);
    }

    @Test
    void explicitOptInSendsOnlyRedactedTextWithHeaderKeyAndTokenBudget() throws Exception {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 200, candidate("STOP", "{\"adults\":4,\"checkInDate\":\"2026-11-20\"}"));
        AdvisorProviderProperties properties = enabled();
        properties.setTimeoutMillis(500);
        properties.setMaxOutputTokens(256);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(properties, client, () -> TEST_KEY);

        CriteriaPatchDto patch = service.extractCriteriaPatch(MESSAGE + " email synthetic@example.test CCCD 079190012345", null);
        assertThat(patch.getAdults()).isEqualTo(4);
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).sendAsync(request.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest sent = request.getValue();
        assertThat(sent.uri().getHost()).isEqualTo("generativelanguage.googleapis.com");
        assertThat(sent.uri().getQuery()).isNull();
        assertThat(sent.uri().toString()).doesNotContain(TEST_KEY);
        assertThat(sent.headers().firstValue("x-goog-api-key")).contains(TEST_KEY);
        assertThat(sent.timeout()).contains(java.time.Duration.ofMillis(500));
        HttpResponse.BodySubscriber<String> payloadReader = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        sent.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { payloadReader.onSubscribe(subscription); }
            @Override public void onNext(ByteBuffer buffer) { payloadReader.onNext(List.of(buffer)); }
            @Override public void onError(Throwable error) { payloadReader.onError(error); }
            @Override public void onComplete() { payloadReader.onComplete(); }
        });
        String body = payloadReader.getBody().toCompletableFuture().get(1, TimeUnit.SECONDS);
        assertThat(body).doesNotContain("synthetic@example.test", "079190012345")
                .contains("[REDACTED_EMAIL]", "[REDACTED_CCCD]");
        JsonNode json = new ObjectMapper().readTree(body);
        assertThat(json.path("generationConfig").path("maxOutputTokens").asInt()).isEqualTo(256);
        assertThat(json.path("generationConfig").path("candidateCount").asInt()).isEqualTo(1);
        assertThat(json.path("generationConfig").path("responseMimeType").asText()).isEqualTo("application/json");
        assertThat(json.has("tools")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "{\"adults\":\"not-a-number\"}", "{\"price\":100}"})
    void malformedOrUnexpectedModelOutputUsesDeterministicFallback(String modelJson) throws Exception {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 200, candidate("STOP", modelJson));
        CriteriaPatchDto patch = new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null);
        // An empty patch is valid JSON but cannot replace meaningful deterministic criteria.
        assertThat(patch.getAdults()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAX_TOKENS", "SAFETY"})
    void incompleteOrBlockedCandidatesCannotBecomeCriteria(String reason) throws Exception {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 200, candidate(reason, "{\"adults\":4}"));
        assertThat(new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void providerErrorsDoNotLogKeysPromptsOrResponseBodies(CapturedOutput output) {
        HttpClient client = mock(HttpClient.class);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException(TEST_KEY + " synthetic@example.test")));
        assertThat(new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        assertThat(output.getAll()).doesNotContain(TEST_KEY, "synthetic@example.test", MESSAGE);
        verify(client, times(1)).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void nonSuccessHttpResponseFallsBackWithoutRetry() {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 429, TEST_KEY);
        assertThat(new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verify(client, times(1)).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    @org.junit.jupiter.api.Timeout(3)
    void timeoutCancelsThePendingHttpCallAndFallsBack() {
        AdvisorProviderProperties properties = enabled();
        properties.setTimeoutMillis(100);
        HttpClient client = mock(HttpClient.class);
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(pending);
        assertThat(new HybridRoomAdvisorLlmService(properties, client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        assertThat(pending.isCancelled()).isTrue();
    }

    @Test
    void interruptCancelsThePendingCallAndPreservesTheInterruptFlag() throws Exception {
        HttpClient client = mock(HttpClient.class);
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        CountDownLatch started = new CountDownLatch(1);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> { started.countDown(); return pending; });
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean fallbackReturned = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            CriteriaPatchDto patch = new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY).extractCriteriaPatch(MESSAGE, null);
            fallbackReturned.set(patch.getAdults() == 2);
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        worker.setDaemon(true);
        try {
            worker.start();
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            worker.interrupt();
            worker.join(2000);
            assertThat(worker.isAlive()).isFalse();
            assertThat(interrupted).isTrue();
            assertThat(fallbackReturned).isTrue();
            assertThat(pending.isCancelled()).isTrue();
        } finally {
            worker.interrupt();
            worker.join(2000);
        }
    }

    @Test
    void responseBodyIsBoundedDuringReceptionAndOverflowCancelsSubscription() {
        var subscriber = new HybridRoomAdvisorLlmService.BoundedBodySubscriber();
        Flow.Subscription subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[HybridRoomAdvisorLlmService.MAX_RESPONSE_BYTES])));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1})));
        subscriber.onComplete();
        verify(subscription).cancel();
        assertThatThrownBy(() -> subscriber.getBody().toCompletableFuture().join()).hasCauseInstanceOf(IOException.class);
    }

    @Test
    void boundedResponsePreservesUtf8AcrossChunks() {
        var subscriber = new HybridRoomAdvisorLlmService.BoundedBodySubscriber();
        subscriber.onSubscribe(mock(Flow.Subscription.class));
        byte[] text = "Hà Nội".getBytes(StandardCharsets.UTF_8);
        subscriber.onNext(List.of(ByteBuffer.wrap(text, 0, 2), ByteBuffer.wrap(text, 2, text.length - 2)));
        subscriber.onComplete();
        assertThat(subscriber.getBody().toCompletableFuture().join()).isEqualTo("Hà Nội");
    }

    private AdvisorProviderProperties enabled() {
        AdvisorProviderProperties properties = new AdvisorProviderProperties();
        properties.setGeminiEnabled(true);
        return properties;
    }

    private String candidate(String finishReason, String modelJson) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                "finishReason", finishReason, "content", Map.of("parts", List.of(Map.of("text", modelJson)))))));
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(HttpClient client, int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AdvisorProviderProperties.class)
    static class PropertiesConfig { }
}
