package vn.edu.utc.hotel_booking.modules.ai.demo.data;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods=false)
@Profile("m1-demo & !prod & !production")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${hotel.ai.demo.forecast.enabled:false} or ${hotel.ai.demo.group.enabled:false}")
public class DemoDataConfiguration {
    @Bean(destroyMethod="close") DemoDataProvider demoDataProvider(Environment env) {
        return switch(env.getProperty("hotel.ai.demo.data-source","fixture")) {
            case "fixture" -> new FixtureDemoDataProvider();
            case "db" -> new PostgresDemoDataProvider(env.getProperty("M1_DEMO_DB_URL"),env.getProperty("M1_DEMO_DB_USER"),env.getProperty("M1_DEMO_DB_PASSWORD"));
            default -> throw new IllegalStateException("Invalid synthetic demo source configuration");
        };
    }
}
