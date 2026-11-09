package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.sun.net.httpserver.HttpServer;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.rag.controller.PublicRagKnowledgeController;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagRequestBudget;
import vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RagRequestBudgetTest {
    private RagRequestBudget budget(int concurrency, int rate, long millis) {
        return new RagRequestBudget(concurrency, rate, Duration.ofMinutes(1), Duration.ofMillis(millis), System::nanoTime);
    }

    @Test void thirdConcurrentAskGets429BeforeEligibilityOrRetrieval() throws Exception {
        var budget = budget(2, 20, 4000);
        var entered = new CountDownLatch(2); var release = new CountDownLatch(1);
        var service = mock(RagGroundedAnswerService.class);
        var eligibility = mock(PublicHotelEligibilityReader.class);
        var controller = new PublicRagKnowledgeController(service, eligibility, budget);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Callable<String> work = () -> budget.execute(() -> {
                entered.countDown(); if (!release.await(3, TimeUnit.SECONDS)) throw new AssertionError("Fixture release missing"); return "OK";
            });
            var first = workers.submit(work); var second = workers.submit(work);
            try {
                assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
                var result = controller.ask(RagQueryRequestDto.builder().hotelId(1).question("Giờ nhận phòng?").build());
                assertThat(result.getStatusCode().value()).isEqualTo(429);
                assertThat(Long.parseLong(result.getHeaders().getFirst("Retry-After"))).isPositive();
                assertThat(result.getBody().getCode()).isEqualTo(429); assertThat(result.getBody().getResult()).isNull();
                verifyNoInteractions(service, eligibility); assertThat(budget.activeRequests()).isEqualTo(2);
            } finally { release.countDown(); }
            assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo("OK"); assertThat(second.get(2, TimeUnit.SECONDS)).isEqualTo("OK");
            assertThat(budget.activeRequests()).isZero();
        }
    }

    @Test void twentyAdmissionsUseARollingMinuteAndRetryAfterRoundsUp() {
        var clock = new AtomicLong();
        var budget = new RagRequestBudget(2, 20, Duration.ofMinutes(1), Duration.ofSeconds(12), clock::get);
        var service = mock(RagGroundedAnswerService.class);
        when(service.answerQuestion(any())).thenReturn(RagQueryResultDto.builder().answer("Synthetic").build());
        var controller = new PublicRagKnowledgeController(service, id -> true, budget);
        var request = RagQueryRequestDto.builder().hotelId(1).question("Synthetic question").build();
        for (int i = 0; i < 20; i++) assertThat(controller.ask(request).getStatusCode().value()).isEqualTo(200);
        var blocked = controller.ask(request);
        assertThat(blocked.getStatusCode().value()).isEqualTo(429);
        assertThat(blocked.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        clock.set(TimeUnit.SECONDS.toNanos(59) + 1);
        assertThat(controller.ask(request).getHeaders().getFirst("Retry-After")).isEqualTo("1");
        verify(service, times(20)).answerQuestion(any());
        clock.set(TimeUnit.MINUTES.toNanos(1));
        assertThat(controller.ask(request).getStatusCode().value()).isEqualTo(200);
        verify(service, times(21)).answerQuestion(any());
    }

    @Test void failuresConsumeRateButNeverLeakConcurrencySlotsOrPrivateCauses() {
        var budget = budget(1, 2, 2000);
        assertThatThrownBy(() -> budget.execute(() -> { throw new IllegalStateException("PRIVATE_SYNTHETIC_CAUSE"); }))
                .isInstanceOf(RagRequestBudget.UnavailableException.class).hasNoCause().hasMessage("RAG request temporarily unavailable");
        assertThat(budget.activeRequests()).isZero();
        assertThat(budget.execute(() -> "NEXT")).isEqualTo("NEXT");
        assertThatThrownBy(() -> budget.execute(() -> "BLOCKED")).isInstanceOf(RagRequestBudget.LimitExceededException.class);
        assertThat(budget.activeRequests()).isZero();
    }

    @Test void publicAndCompatibilityAliasesShareTheSame429BudgetAndEnvelope() throws Exception {
        var clock = new AtomicLong();
        var budget = new RagRequestBudget(2, 1, Duration.ofMinutes(1), Duration.ofSeconds(12), clock::get);
        var service = mock(RagGroundedAnswerService.class);
        when(service.answerQuestion(any())).thenReturn(RagQueryResultDto.builder().answer("Synthetic").build());
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new PublicRagKnowledgeController(service, id -> true, budget)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/public/ai/room-advisor/knowledge/ask")
                        .contentType("application/json").content("{\"hotelId\":1,\"question\":\"Synthetic\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/ai/room-advisor/knowledge/ask")
                        .contentType("application/json").content("{\"hotelId\":1,\"question\":\"Synthetic\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "60"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(429))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result").doesNotExist());
        verify(service, times(1)).answerQuestion(any());
    }

    @Test void nestedExecutionCannotReplaceTheCurrentRequestContext() {
        var first = budget(1, 20, 2000); var other = budget(1, 20, 2000);
        assertThat(first.execute(() -> {
            assertThatThrownBy(() -> other.execute(() -> "NO")).isInstanceOf(RagRequestBudget.UnavailableException.class);
            assertThat(first.activeRequests()).isEqualTo(1); assertThat(other.activeRequests()).isZero(); return "OK";
        })).isEqualTo("OK");
        assertThat(other.execute(() -> "AFTER")).isEqualTo("AFTER");
    }

    @Test void deadlineCancelsActualHttpAndKeepsSlotUntilAllCleanupReallyEnds() throws Exception {
        var serverEntered = new CountDownLatch(1); var releaseServer = new CountDownLatch(1);
        var cancelled = new CountDownLatch(1); var finishCleanup = new CountDownLatch(1);
        var calls = new AtomicInteger(); var cancelEvents = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet(); exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                output.write("{\"usageMetadata\":".getBytes(StandardCharsets.UTF_8)); output.flush(); serverEntered.countDown();
                releaseServer.await(4, TimeUnit.SECONDS);
            } catch (Exception ignored) { } finally { exchange.close(); }
        });
        server.start();
        OkHttpClient client = RagRequestBudget.providerHttpClient("synthetic-chat", 3000).newBuilder()
                .addInterceptor(chain -> {
                    if (!"127.0.0.1".equals(chain.request().url().host()) || chain.request().url().port() != server.getAddress().getPort())
                        throw new java.io.IOException("Offline test blocked external address");
                    return chain.proceed(chain.request());
                }).eventListener(new EventListener() {
                    @Override public void canceled(Call call) { cancelEvents.incrementAndGet(); }
                }).build();
        var budget = budget(1, 20, 400);
        try (var worker = Executors.newSingleThreadExecutor()) {
            var result = worker.submit(() -> budget.execute(() -> {
                try (var response = client.newCall(new Request.Builder().url("http://127.0.0.1:" + server.getAddress().getPort()).build()).execute()) {
                    return response.body().string();
                } catch (java.io.IOException failure) {
                    cancelled.countDown();
                    if (!finishCleanup.await(3, TimeUnit.SECONDS)) throw new AssertionError("Cleanup fixture timed out");
                    throw failure;
                }
            }));
            try {
                assertThat(serverEntered.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(cancelled.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(cancelEvents.get()).isPositive(); assertThat(calls.get()).isEqualTo(1);
                assertThat(budget.activeRequests()).isEqualTo(1);
                assertThat(result.isDone()).isFalse(); // no early 503 with abandoned work
                assertThatThrownBy(() -> budget.execute(() -> "NO")).isInstanceOf(RagRequestBudget.LimitExceededException.class);
            } finally { finishCleanup.countDown(); releaseServer.countDown(); }
            assertThatThrownBy(() -> result.get(2, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RagRequestBudget.UnavailableException.class);
            assertThat(budget.activeRequests()).isZero();
            assertThat(budget.execute(() -> "RECOVERED")).isEqualTo("RECOVERED");
        } finally {
            finishCleanup.countDown(); releaseServer.countDown(); server.stop(0);
            client.connectionPool().evictAll(); client.dispatcher().executorService().shutdownNow();
        }
    }

    @Test void elapsedDeadlinePreventsStartingAnotherOutboundCall() {
        var clock = new AtomicLong();
        var budget = new RagRequestBudget(1, 20, Duration.ofMinutes(1), Duration.ofSeconds(1), clock::get);
        var contacted = new AtomicInteger();
        var client = RagRequestBudget.providerHttpClient("synthetic-embedding", 3000).newBuilder().addInterceptor(chain -> {
            contacted.incrementAndGet(); throw new java.io.IOException("No actual network permitted");
        }).build();
        try {
            assertThatThrownBy(() -> budget.execute(() -> {
                clock.addAndGet(TimeUnit.SECONDS.toNanos(2));
                return client.newCall(new Request.Builder().url("http://127.0.0.1:1").build()).execute();
            })).isInstanceOf(RagRequestBudget.UnavailableException.class);
            assertThat(contacted.get()).isZero(); assertThat(budget.activeRequests()).isZero();
            assertThat(budget.execute(() -> "RECOVERED")).isEqualTo("RECOVERED");
        } finally { client.connectionPool().evictAll(); client.dispatcher().executorService().shutdownNow(); }
    }

    @ParameterizedTest @ValueSource(ints = {99, 7001})
    void unsafeProviderTimeoutsAreRejected(int timeout) {
        assertThatThrownBy(() -> RagRequestBudget.providerHttpClient("synthetic", timeout)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void transportCannotSilentlyRetryOrFollowCredentialLeakingRedirects() {
        var client = RagRequestBudget.providerHttpClient("synthetic", 3000);
        assertThat(client.retryOnConnectionFailure()).isFalse(); assertThat(client.followRedirects()).isFalse();
        assertThat(client.followSslRedirects()).isFalse(); assertThat(client.callTimeoutMillis()).isEqualTo(3000);
    }
}
