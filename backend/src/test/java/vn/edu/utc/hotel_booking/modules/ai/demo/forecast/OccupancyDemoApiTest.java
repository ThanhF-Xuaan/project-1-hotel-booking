package vn.edu.utc.hotel_booking.modules.ai.demo.forecast;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OccupancyDemoApiTest {
    private AnnotationConfigWebApplicationContext context;
    private static final String URL = "/api/v1/ai/demo/occupancy";

    private MockMvc setup(boolean enabled, String... profiles) {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles(profiles);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of("hotel.ai.demo.forecast.enabled", enabled)));
        context.register(WebSecurity.class, OccupancyDemoConfiguration.class, OccupancyDemoController.class);
        context.refresh();
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void close() { SecurityContextHolder.clearContext(); if (context != null) context.close(); }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc @EnableWebSecurity @EnableMethodSecurity
    static class WebSecurity {
        @Bean InMemoryUserDetailsManager users() {
            return new InMemoryUserDetailsManager(User.withUsername("admin").password("{noop}synthetic").roles("CHAIN_ADMIN").build(),
                    User.withUsername("customer").password("{noop}synthetic").roles("CUSTOMER").build(),
                    User.withUsername("reception").password("{noop}synthetic").roles("RECEPTIONIST").build());
        }
        @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(a -> a.anyRequest().authenticated()).httpBasic(Customizer.withDefaults()).build();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"", "prod", "production", "m1-demo,prod", "m1-demo,production"})
    void profileGatesExcludeBothControllerAndProvider(String profiles) throws Exception {
        var mvc = setup(true, profiles.isEmpty() ? new String[0] : profiles.split(","));
        assertThat(context.getBeansOfType(OccupancyDemoService.class)).isEmpty();
        assertThat(context.getBeansOfType(OccupancyDemoController.class)).isEmpty();
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
    }

    @Test void flagMustAlsoBeEnabled() throws Exception {
        var mvc = setup(false, "m1-demo");
        assertThat(context.getBeansOfType(OccupancyDemoService.class)).isEmpty();
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
    }

    @Test void anonymousCannotReadDemo() throws Exception {
        setup(true, "m1-demo").perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"customer", "reception"})
    void realFilterAndMethodSecurityDenyNonAdmin(String username) throws Exception {
        setup(true, "m1-demo").perform(get(URL).with(httpBasic(username, "synthetic"))).andExpect(status().isForbidden());
    }

    @ParameterizedTest @ValueSource(strings = {"HOTEL_A", "HOTEL_B"})
    void adminGetsFixedClockSyntheticHistoryAndForecast(String scenario) throws Exception {
        var mvc = setup(true, "m1-demo");
        var first = mvc.perform(get(URL).param("scenario", scenario).with(httpBasic("admin", "synthetic")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.dataMode").value("SYNTHETIC"))
                .andExpect(jsonPath("$.result.businessData").value(false))
                .andExpect(jsonPath("$.result.scenario").value(scenario))
                .andExpect(jsonPath("$.result.history.samples.length()").value(420))
                .andExpect(jsonPath("$.result.history.status").value("PARTIAL"))
                .andExpect(jsonPath("$.result.forecast.status").value("FORECAST_READY"))
                .andExpect(jsonPath("$.result.forecast.dataMode").value("SYNTHETIC"))
                .andExpect(jsonPath("$.result.forecast.points.length()").value(30))
                .andExpect(jsonPath("$.result.forecast.points[0].date").value("2026-10-30"))
                .andReturn().getResponse().getContentAsString();
        var second = mvc.perform(get(URL).param("scenario", scenario).with(httpBasic("admin", "synthetic")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(second).isEqualTo(first);
    }

    @ParameterizedTest @ValueSource(strings = {"", "OTHER", "hotel_a", "1", "HOTEL_A\n"})
    void invalidScenarioNeverEchoesInput(String value) throws Exception {
        setup(true, "m1-demo").perform(get(URL).param("scenario", value).with(httpBasic("admin", "synthetic")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Kịch bản demo không hợp lệ."));
    }

    @Test void duplicateOrBusinessSelectorsAreRejected() throws Exception {
        var mvc = setup(true, "m1-demo");
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scenario").value("HOTEL_A"));
        mvc.perform(get(URL).param("scenario", "HOTEL_A", "HOTEL_B").with(httpBasic("admin", "synthetic"))).andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("hotelId", "1").with(httpBasic("admin", "synthetic"))).andExpect(status().isBadRequest());
    }

    @Test void serviceAlsoFailsClosedWithoutTrustedAdmin() {
        setup(true, "m1-demo");
        assertThatThrownBy(() -> context.getBean(OccupancyDemoService.class).replay("HOTEL_A"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test void adapterHasNoCoreDatabaseOrNetworkDependencies() throws Exception {
        try (var files = Files.list(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/demo/forecast"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("java.sql.", "javax.sql.", "java.net.", "jakarta.persistence.",
                        "modules.booking.", "modules.inventory.", "modules.pricing.", "modules.operation.", "JdbcTemplate");
                for (String line : source.lines().filter(s -> s.startsWith("import ")).toList()) {
                    assertThat(line.startsWith("import java.security.") || line.startsWith("import java.time.")
                            || line.startsWith("import java.util.") || line.startsWith("import com.fasterxml.jackson.databind.")
                            || line.startsWith("import org.springframework.boot.autoconfigure.condition.")
                            || line.startsWith("import org.springframework.context.annotation.")
                            || line.startsWith("import org.springframework.core.io.ClassPathResource;")
                            || line.startsWith("import org.springframework.security.")
                            || line.startsWith("import org.springframework.http.ResponseEntity;")
                            || line.startsWith("import org.springframework.web.bind.annotation.")
                            || line.startsWith("import jakarta.servlet.http.HttpServletRequest;")
                            || line.startsWith("import vn.edu.utc.hotel_booking.common.dto.ApiResponse;")
                            || line.startsWith("import vn.edu.utc.hotel_booking.modules.ai.forecast."))
                            .as("Closed demo dependency boundary: %s", line).isTrue();
                }
            }
        }
    }
}
