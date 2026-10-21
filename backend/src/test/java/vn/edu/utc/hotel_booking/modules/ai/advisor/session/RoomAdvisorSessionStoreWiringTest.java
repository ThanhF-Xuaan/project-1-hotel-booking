package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Hardware-independent bean wiring test using {@link ApplicationContextRunner} (AC-4.1, AC-4.2).
 *
 * <p>Exercises conditional store bean selection without starting a real application context,
 * network listener, or Redis connection.</p>
 */
class RoomAdvisorSessionStoreWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    InMemoryRoomAdvisorSessionStore.class,
                    RedisRoomAdvisorSessionStore.class
            );

    @Test
    @DisplayName("AC-4.1: Property absent -> exactly one store bean (InMemoryRoomAdvisorSessionStore)")
    void propertyAbsentShouldSelectInMemoryStore() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RoomAdvisorSessionStore.class);
            assertThat(context).hasSingleBean(InMemoryRoomAdvisorSessionStore.class);
            assertThat(context).doesNotHaveBean(RedisRoomAdvisorSessionStore.class);
        });
    }

    @Test
    @DisplayName("Explicit store-type=in-memory -> exactly one store bean (InMemoryRoomAdvisorSessionStore)")
    void propertyInMemoryShouldSelectInMemoryStore() {
        contextRunner
                .withPropertyValues("hotel.ai.advisor.session.store-type=in-memory")
                .run(context -> {
                    assertThat(context).hasSingleBean(RoomAdvisorSessionStore.class);
                    assertThat(context).hasSingleBean(InMemoryRoomAdvisorSessionStore.class);
                    assertThat(context).doesNotHaveBean(RedisRoomAdvisorSessionStore.class);
                });
    }

    @Test
    @DisplayName("AC-4.2: store-type=redis -> exactly one store bean (RedisRoomAdvisorSessionStore) with stubbed template")
    void propertyRedisShouldSelectRedisStoreWithStubbedTemplate() {
        StringRedisTemplate mockTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);
        when(mockTemplate.getConnectionFactory()).thenReturn(mockFactory);

        contextRunner
                .withPropertyValues("hotel.ai.advisor.session.store-type=redis")
                .withBean(StringRedisTemplate.class, () -> mockTemplate)
                .run(context -> {
                    assertThat(context).hasSingleBean(RoomAdvisorSessionStore.class);
                    assertThat(context).hasSingleBean(RedisRoomAdvisorSessionStore.class);
                    assertThat(context).doesNotHaveBean(InMemoryRoomAdvisorSessionStore.class);

                    // Zero interactions with connection factory: constructor performed no I/O
                    verifyNoInteractions(mockFactory);
                });
    }

    @Test
    @DisplayName("Unrecognised store-type value -> zero store beans exist (fail-fast)")
    void unrecognisedStoreTypeShouldYieldZeroStoreBeans() {
        contextRunner
                .withPropertyValues("hotel.ai.advisor.session.store-type=unrecognised-store")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RoomAdvisorSessionStore.class);
                    assertThat(context).doesNotHaveBean(InMemoryRoomAdvisorSessionStore.class);
                    assertThat(context).doesNotHaveBean(RedisRoomAdvisorSessionStore.class);
                });
    }
}
