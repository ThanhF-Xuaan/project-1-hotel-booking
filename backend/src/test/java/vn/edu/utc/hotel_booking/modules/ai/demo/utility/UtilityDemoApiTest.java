package vn.edu.utc.hotel_booking.modules.ai.demo.utility;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse.PointStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse.Status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UtilityDemoApiTest {
    private static final String URL = "/api/v1/ai/demo/utility-anomalies";
    private AnnotationConfigWebApplicationContext context;

    private MockMvc setup(Boolean enabled, String... profiles) {
        return setup(enabled, false, profiles);
    }
    private MockMvc setup(Boolean enabled, boolean occupancy, String... profiles) {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles(profiles);
        Map<String, Object> properties = new HashMap<>();
        properties.put("hotel.ai.demo.forecast.enabled", occupancy);
        if (enabled != null) properties.put("hotel.ai.demo.utility.enabled", enabled);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.register(WebSecurity.class, UtilityDemoConfiguration.class, UtilityDemoController.class);
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
    void profileExcludesBothControllerAndProvider(String profile) throws Exception {
        var mvc = setup(true, profile.isEmpty() ? new String[0] : profile.split(","));
        assertThat(context.getBeansOfType(UtilityDemoService.class)).isEmpty();
        assertThat(context.getBeansOfType(UtilityDemoController.class)).isEmpty();
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
    }
    @Test void falseFlagDisablesDespiteProfile() throws Exception {
        setup(false, "m1-demo").perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
        assertThat(context.getBeansOfType(UtilityDemoService.class)).isEmpty();
    }
    @Test void absentFlagDefaultsOffEvenWhenOccupancyFlagIsOn() throws Exception {
        var mvc = setup(null, true, "m1-demo");
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
        assertThat(context.getBeansOfType(UtilityDemoService.class)).isEmpty();
    }
    @Test void anonymousDenied() throws Exception { setup(true, "m1-demo").perform(get(URL)).andExpect(status().isUnauthorized()); }
    @ParameterizedTest @ValueSource(strings = {"customer", "reception"})
    void nonAdminDeniedByRealFiltersAndMethodSecurity(String user) throws Exception {
        setup(true, "m1-demo").perform(get(URL).with(httpBasic(user, "synthetic"))).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings = {"ELECTRIC_FLAT", "WATER_FLAT", "ELECTRIC_SPIKES", "WATER_SPIKES", "ZERO_BOUNDARY", "MAD_NOISE", "GAP", "RESET", "REPLACEMENT", "NEGATIVE_DROP", "LOW_COVERAGE", "SLOW_VARIATION"})
    void eachScenarioUsesExistingEngineWithExplicitProvenanceAndDeterministicCounts(String scenario) throws Exception {
        var mvc = setup(true, "m1-demo");
        var first = mvc.perform(get(URL).param("scenario", scenario).with(httpBasic("admin", "synthetic")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.scenario").value(scenario)).andExpect(jsonPath("$.result.dataMode").value("SYNTHETIC"))
                .andExpect(jsonPath("$.result.businessData").value(false)).andExpect(jsonPath("$.result.scenarios.length()").value(12))
                .andExpect(jsonPath("$.result.referenceInstant").value("2026-10-30T17:00:00Z"))
                .andExpect(jsonPath("$.result.analysis.dataMode").value("SYNTHETIC"))
                .andExpect(jsonPath("$.result.analysis.modelVersion").value("u22-causal-median-mad-v1"))
                .andExpect(jsonPath("$.result.analysis.points.length()").value(42))
                .andExpect(jsonPath("$.result.analysis.fromInclusive").value("2026-09-19"))
                .andExpect(jsonPath("$.result.analysis.toInclusive").value("2026-10-30"))
                .andExpect(jsonPath("$.result.analysis.points[0].status").value(scenario.equals("LOW_COVERAGE") ? "EXCLUDED" : "INSUFFICIENT_COVERAGE"))
                .andExpect(jsonPath("$.result.analysis.points[0].median").isEmpty())
                .andExpect(jsonPath("$.result.expected").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThat(mvc.perform(get(URL).param("scenario", scenario).with(httpBasic("admin", "synthetic")))
                .andReturn().getResponse().getContentAsString()).isEqualTo(first);
    }
    @ParameterizedTest @ValueSource(strings = {"", "OTHER", "electric_spikes", "1", "ELECTRIC_SPIKES\n", " ELECTRIC_SPIKES"})
    void invalidScenarioReturnsFixedError(String id) throws Exception {
        setup(true, "m1-demo").perform(get(URL).param("scenario", id).with(httpBasic("admin", "synthetic")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Kịch bản demo không hợp lệ."));
    }
    @Test void defaultDuplicateAndUntrustedSelectors() throws Exception {
        var mvc = setup(true, "m1-demo");
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isOk()).andExpect(jsonPath("$.result.scenario").value("ELECTRIC_SPIKES"));
        mvc.perform(get(URL).param("scenario", "GAP", "RESET").with(httpBasic("admin", "synthetic"))).andExpect(status().isBadRequest());
        for (String key : List.of("hotelId", "meterId", "regionId", "unit", "clock", "role", "url"))
            mvc.perform(get(URL).param(key, "untrusted").with(httpBasic("admin", "synthetic"))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Kịch bản demo không hợp lệ."));
    }
    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("synthetic", null, List.of(new SimpleGrantedAuthority(role))));
    }
    @Test void directServiceCannotUseCachedAdminPermissionAfterRoleRevocation() {
        var service = new UtilityDemoService();
        assertThatThrownBy(() -> service.replay("ELECTRIC_SPIKES")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        authenticate("ROLE_CHAIN_ADMIN"); assertThat(service.replay("ELECTRIC_SPIKES").analysis().status()).isEqualTo(Status.ANALYZED);
        authenticate("ROLE_CUSTOMER");
        assertThatThrownBy(() -> service.replay("ELECTRIC_SPIKES")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void responsePreservesCausalBoundaryUnitsWarmupGapsAndBarriersWithoutOracle() {
        authenticate("ROLE_CHAIN_ADMIN"); var service = new UtilityDemoService();
        var electric = service.replay("ELECTRIC_FLAT");
        assertThat(electric.analysis().unit().name()).isEqualTo("KWH"); assertThat(electric.counts().evaluated()).isEqualTo(14);
        assertThat(electric.counts().insufficient()).isEqualTo(28); assertThat(electric.counts().needsReview()).isZero();
        assertThat(service.replay("WATER_FLAT").analysis().unit().name()).isEqualTo("M3");
        var spikes = service.replay("ELECTRIC_SPIKES"); assertThat(spikes.counts().needsReview()).isEqualTo(2);
        assertThat(spikes.analysis().points().stream().filter(p -> p.status() == PointStatus.NEEDS_REVIEW)).allMatch(p -> "Cần kiểm tra".equals(p.label()));
        var gap = service.replay("GAP"); assertThat(gap.counts().excluded()).isEqualTo(2);
        assertThat(gap.analysis().points().stream().filter(p -> p.status() == PointStatus.EXCLUDED)).allMatch(p -> p.usage() == null);
        for (String scenario : List.of("RESET", "REPLACEMENT", "NEGATIVE_DROP", "LOW_COVERAGE")) {
            var value = service.replay(scenario); assertThat(value.analysis().points()).hasSize(42);
            assertThat(value.counts().evaluated() + value.counts().excluded() + value.counts().insufficient()).isEqualTo(42);
            assertThat(value.counts().needsReview()).isZero();
        }
        assertThat(service.replay("LOW_COVERAGE").analysis().status()).isEqualTo(Status.INSUFFICIENT_COVERAGE);
    }
    @Test void runtimeProjectionExactlyRemovesFrozenOracleColumnAndPreservesRawBytes() throws Exception {
        String frozen = Files.readString(Path.of("src/test/resources/ai/u22/utility-readings-42-days.csv"));
        String projection = frozen.lines().map(line -> line.substring(0, line.lastIndexOf(','))).reduce("", (left, right) -> left + right + "\n");
        byte[] actual = Files.readAllBytes(Path.of("src/main/resources/ai/demo/utility-readings-v1.csv"));
        assertThat(new String(actual, StandardCharsets.UTF_8)).isEqualTo(projection).doesNotContain("expected", "ANCHOR", "WARMUP", "ANOMALY", "EXCLUDED");
        new UtilityDemoService(actual);
        for (byte[] bad : List.of(new byte[0], new byte[128_001], "private payload".getBytes(StandardCharsets.UTF_8)))
            assertThatThrownBy(() -> new UtilityDemoService(bad)).isInstanceOf(IllegalStateException.class).hasMessage("Synthetic utility demo corpus unavailable");
    }
    @Test void demoImportsHaveClosedReadOnlyBoundary() throws Exception {
        try (var paths = Files.list(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/demo/utility"))) {
            for (var path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("java.sql.", "javax.sql.", "java.net.", "java.io.", "java.nio.file.", "jakarta.persistence.", "modules.operation.", "modules.booking.", "modules.inventory.", "modules.pricing.", "JdbcTemplate", "System.out", "System.err");
                for (String line : source.lines().filter(s -> s.startsWith("import ")).toList())
                    assertThat(line).matches("import (?:java\\.(?:math|security|time|util)\\..*|java\\.nio\\.charset\\.StandardCharsets|org\\.springframework\\.(?:boot\\.autoconfigure\\.condition|context\\.annotation|security|web\\.bind\\.annotation)\\..*|org\\.springframework\\.core\\.io\\.ClassPathResource|org\\.springframework\\.http\\.ResponseEntity|jakarta\\.servlet\\.http\\.HttpServletRequest|vn\\.edu\\.utc\\.hotel_booking\\.(?:common\\.dto\\.ApiResponse|modules\\.ai\\.forecast\\..*));");
            }
        }
    }
}
