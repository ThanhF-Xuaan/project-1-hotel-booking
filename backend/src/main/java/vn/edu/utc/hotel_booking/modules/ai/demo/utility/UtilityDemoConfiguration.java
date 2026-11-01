package vn.edu.utc.hotel_booking.modules.ai.demo.utility;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("m1-demo & !prod & !production")
@ConditionalOnProperty(name = "hotel.ai.demo.utility.enabled", havingValue = "true")
public class UtilityDemoConfiguration {
    @Bean UtilityDemoService utilityDemoService() { return new UtilityDemoService(); }
}
