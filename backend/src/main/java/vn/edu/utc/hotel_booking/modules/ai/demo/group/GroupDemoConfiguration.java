package vn.edu.utc.hotel_booking.modules.ai.demo.group;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@org.springframework.context.annotation.Import(vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataConfiguration.class)
@Configuration(proxyBeanMethods = false)
@Profile("m1-demo & !prod & !production")
@ConditionalOnProperty(name = "hotel.ai.demo.group.enabled", havingValue = "true")
public class GroupDemoConfiguration {
    @Bean GroupDemoService groupDemoService(vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider provider) { return new GroupDemoService(provider); }
}
