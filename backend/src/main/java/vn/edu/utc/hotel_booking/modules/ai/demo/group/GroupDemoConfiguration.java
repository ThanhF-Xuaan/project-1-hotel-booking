package vn.edu.utc.hotel_booking.modules.ai.demo.group;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("m1-demo & !prod & !production")
@ConditionalOnProperty(name = "hotel.ai.demo.group.enabled", havingValue = "true")
public class GroupDemoConfiguration {
    @Bean GroupDemoService groupDemoService() { return new GroupDemoService(); }
}
