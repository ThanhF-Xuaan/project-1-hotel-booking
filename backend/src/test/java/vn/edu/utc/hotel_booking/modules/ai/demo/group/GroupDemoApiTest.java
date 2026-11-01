package vn.edu.utc.hotel_booking.modules.ai.demo.group;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.Assignment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupDemoApiTest {
    private static final String URL = "/api/v1/ai/demo/group-room-assignment";
    private AnnotationConfigWebApplicationContext context;
    private MockMvc setup(Boolean enabled, String... profiles) {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles(profiles);
        Map<String, Object> properties = new HashMap<>(Map.of("hotel.ai.demo.forecast.enabled", true, "hotel.ai.demo.utility.enabled", true));
        if (enabled != null) properties.put("hotel.ai.demo.group.enabled", enabled);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.register(WebSecurity.class, GroupDemoConfiguration.class, GroupDemoController.class);
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
    void profileExcludesControllerAndProvider(String profile) throws Exception {
        var mvc = setup(true, profile.isEmpty() ? new String[0] : profile.split(","));
        assertThat(context.getBeansOfType(GroupDemoService.class)).isEmpty();
        assertThat(context.getBeansOfType(GroupDemoController.class)).isEmpty();
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
    }
    @Test void falseFlagKeepsBothBeansOff() throws Exception {
        setup(false, "m1-demo").perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
        assertThat(context.getBeansOfType(GroupDemoService.class)).isEmpty();
    }
    @Test void missingFlagCannotBeEnabledByOtherDemoFlags() throws Exception {
        setup(null, "m1-demo").perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isNotFound());
        assertThat(context.getBeansOfType(GroupDemoService.class)).isEmpty();
    }
    @Test void anonymousDenied() throws Exception { setup(true, "m1-demo").perform(get(URL)).andExpect(status().isUnauthorized()); }
    @ParameterizedTest @ValueSource(strings = {"customer", "reception"})
    void nonAdminDeniedThroughFilterAndMethodSecurity(String user) throws Exception {
        setup(true, "m1-demo").perform(get(URL).with(httpBasic(user, "synthetic"))).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings = {"FIT_AND_TIE", "QUOTA_ONE", "MIDDLE_NIGHT_LIMIT", "ZERO_QUOTA", "QUOTA_CONTROL", "NO_TRANSFER_BLOCK", "NO_TRANSFER_ROOM_TYPE"})
    void allCasesHaveFixedClockSyntheticPreviewOnlyAndDeterministicOutput(String scenario) throws Exception {
        var mvc = setup(true, "m1-demo");
        var first = mvc.perform(get(URL).param("scenario", scenario).with(httpBasic("admin", "synthetic")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.dataMode").value("SYNTHETIC")).andExpect(jsonPath("$.result.businessData").value(false))
                .andExpect(jsonPath("$.result.previewOnly").value(true)).andExpect(jsonPath("$.result.scenario").value(scenario))
                .andExpect(jsonPath("$.result.referenceInstant").value("2026-10-31T02:00:00Z"))
                .andExpect(jsonPath("$.result.scenarios.length()").value(7)).andExpect(jsonPath("$.result.preview.status").value("COMPLETE"))
                .andExpect(jsonPath("$.result.preview.previewOnly").value(true)).andExpect(jsonPath("$.result.preview.dataMode").value("SYNTHETIC"))
                .andExpect(jsonPath("$.result.snapshot.dataMode").value("SYNTHETIC")).andExpect(jsonPath("$.result.snapshot.coverage").value("COMPLETE"))
                .andExpect(jsonPath("$.result.request.demands.length()").value(2)).andExpect(jsonPath("$.result.expected").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(mvc.perform(get(URL).param("scenario", scenario).with(httpBasic("admin", "synthetic")))
                .andReturn().getResponse().getContentAsString()).isEqualTo(first);
    }
    @ParameterizedTest @ValueSource(strings = {"", "OTHER", "fit_and_tie", "P01", "FIT_AND_TIE\n", " FIT_AND_TIE"})
    void invalidScenarioIsSafe400(String value) throws Exception {
        setup(true, "m1-demo").perform(get(URL).param("scenario", value).with(httpBasic("admin", "synthetic")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Kịch bản demo không hợp lệ."));
    }
    @Test void defaultDuplicateAndAllBusinessSelectors() throws Exception {
        var mvc = setup(true, "m1-demo");
        mvc.perform(get(URL).with(httpBasic("admin", "synthetic"))).andExpect(status().isOk()).andExpect(jsonPath("$.result.scenario").value("FIT_AND_TIE"));
        mvc.perform(get(URL).param("scenario", "FIT_AND_TIE", "ZERO_QUOTA").with(httpBasic("admin", "synthetic"))).andExpect(status().isBadRequest());
        for (String key : List.of("hotelId", "contractId", "checkInDate", "demands", "rooms", "quota", "role", "clock", "workLimit", "action"))
            mvc.perform(get(URL).param(key, "untrusted").with(httpBasic("admin", "synthetic"))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Kịch bản demo không hợp lệ."));
    }
    @Test void controllerHasNoWriteMethodEvenWithValidAuthenticationAndCsrf() throws Exception {
        var mvc = setup(true, "m1-demo");
        for (var command : List.of(post(URL), put(URL), patch(URL), delete(URL)))
            mvc.perform(command.with(httpBasic("admin", "synthetic")).with(csrf())).andExpect(status().isMethodNotAllowed());
    }
    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("synthetic", null, List.of(new SimpleGrantedAuthority(role))));
    }
    @Test void directServiceDoesNotCacheAnAdminRole() {
        var service = new GroupDemoService();
        assertThatThrownBy(() -> service.replay("FIT_AND_TIE")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        authenticate("ROLE_CHAIN_ADMIN"); assertThat(service.replay("FIT_AND_TIE").previewOnly()).isTrue();
        authenticate("ROLE_CUSTOMER");
        assertThatThrownBy(() -> service.replay("FIT_AND_TIE")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void independentQuotaOraclesAndPartialDemandCoverageRemainExact() {
        authenticate("ROLE_CHAIN_ADMIN"); var service = new GroupDemoService();
        var fit = service.replay("FIT_AND_TIE");
        assertThat(fit.preview().assignments()).containsExactly(new Assignment("A", 11), new Assignment("B", 12));
        assertThat(fit.preview().objective().bedWaste()).isZero(); assertThat(fit.preview().objective().floorSpan()).isZero();
        assertThat(fit.allDemandsAssigned()).isTrue();
        for (String scenario : List.of("QUOTA_ONE", "MIDDLE_NIGHT_LIMIT")) {
            var result = service.replay(scenario);
            assertThat(result.preview().assignments()).containsExactly(new Assignment("A", 11));
            assertThat(result.preview().unmet()).extracting(p -> p.demandKey()).containsExactly("B");
            assertThat(result.allDemandsAssigned()).isFalse();
        }
        var zero = service.replay("ZERO_QUOTA");
        assertThat(zero.preview().assignments()).isEmpty(); assertThat(zero.preview().unmet()).hasSize(2);
        assertThat(zero.allDemandsAssigned()).isFalse();
        assertThat(service.replay("QUOTA_CONTROL").preview().assignments()).hasSize(2);
        for (String scenario : List.of("NO_TRANSFER_BLOCK", "NO_TRANSFER_ROOM_TYPE"))
            assertThat(service.replay(scenario).preview().assignments()).containsExactly(new Assignment("A", 12));
        assertThatThrownBy(() -> fit.request().demands().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> fit.snapshot().candidates().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void resourceIsExactOracleFreeProjectionAndCorruptionFailsClosed() throws Exception {
        var mapper = new ObjectMapper();
        var frozen = mapper.readTree(Files.readAllBytes(Path.of("src/test/resources/ai/group-assignment/preview-cases.json")));
        byte[] raw = Files.readAllBytes(Path.of("src/main/resources/ai/demo/group-preview-v1.json"));
        var actual = mapper.readTree(raw);
        assertThat(actual.size()).isEqualTo(4); assertThat(actual.path("cases").size()).isEqualTo(7);
        assertThat(actual.path("referenceInstant").asText()).isEqualTo(frozen.path("asOf").asText());
        for (int i = 0; i < 7; i++) {
            var projected = frozen.path("cases").get(i).deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) projected).remove("expected");
            assertThat(actual.path("cases").get(i)).isEqualTo(projected);
        }
        assertThat(new String(raw, java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("\"expected\"", "\"c01Sha256\"", "\"source\"");
        for (byte[] bad : List.of(new byte[0], new byte[128_001], new byte[] {1, 2, 3}))
            assertThatThrownBy(() -> new GroupDemoService(bad)).isInstanceOf(IllegalStateException.class).hasMessage("Synthetic group demo corpus unavailable");
    }
    @Test void demoHasClosedReadOnlyDependencies() throws Exception {
        try (var paths = Files.list(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/demo/group"))) {
            for (var path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("java.sql.", "javax.sql.", "java.net.", "java.io.", "java.nio.file.", "jakarta.persistence.", "modules.booking.", "modules.inventory.", "modules.operation.", "modules.payment.", "JdbcTemplate", "System.out", "System.err", "ProcessBuilder", "Runtime.getRuntime");
                for (String line : source.lines().filter(s -> s.startsWith("import ")).toList())
                    assertThat(line).matches("import (?:java\\.(?:security|time|util)\\..*|com\\.fasterxml\\.jackson\\.databind\\..*|org\\.springframework\\.(?:boot\\.autoconfigure\\.condition|context\\.annotation|security|web\\.bind\\.annotation)\\..*|org\\.springframework\\.core\\.io\\.ClassPathResource|org\\.springframework\\.http\\.ResponseEntity|jakarta\\.servlet\\.http\\.HttpServletRequest|vn\\.edu\\.utc\\.hotel_booking\\.(?:common\\.dto\\.ApiResponse|modules\\.ai\\.groupassignment\\..*));");
            }
        }
    }
}
