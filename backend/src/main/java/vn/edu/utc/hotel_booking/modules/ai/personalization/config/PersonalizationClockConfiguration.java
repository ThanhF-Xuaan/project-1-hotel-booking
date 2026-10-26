package vn.edu.utc.hotel_booking.modules.ai.personalization.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PersonalizationClockConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock personalizationClock() {
        return Clock.systemUTC();
    }
}
