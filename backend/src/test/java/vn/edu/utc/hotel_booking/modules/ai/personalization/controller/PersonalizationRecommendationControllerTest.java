package vn.edu.utc.hotel_booking.modules.ai.personalization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import vn.edu.utc.hotel_booking.common.config.JwtBlacklistFilter;
import vn.edu.utc.hotel_booking.common.config.SecurityConfig;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRecommendationResponse;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRecommendationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.PersonalizationRecommendationService;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestPropertySource(properties = "hotel.ai.personalization.local-demo-enabled=true")
@ContextConfiguration(classes = {
        PersonalizationRecommendationController.class,
        PersonalizationRecommendationExceptionHandler.class,
        SecurityConfig.class,
        PersonalizationRecommendationControllerTest.TestSecurityContextConfig.class
})
class PersonalizationRecommendationControllerTest {

    private static final String ISSUER = "https://identity.example.test/realms/hotel";
    private static final String SUBJECT = "customer-subject";

    @Configuration
    @EnableWebMvc
    @Import(JwtBlacklistFilter.class)
    static class TestSecurityContextConfig {

        @Bean
        TokenBlacklistService tokenBlacklistService() {
            TokenBlacklistService service = mock(TokenBlacklistService.class);
            when(service.isBlacklisted(any())).thenReturn(false);
            return service;
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        PersonalizationRecommendationService recommendationService() {
            return mock(PersonalizationRecommendationService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PersonalizationRecommendationService recommendationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(recommendationService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void defaultsSuggestionLimitAndUsesNoStoreResponse() throws Exception {
        when(recommendationService.recommend(ISSUER, SUBJECT, 1, 3)).thenReturn(response());

        mockMvc.perform(get("/api/v1/me/personalization/recommendations")
                        .param("hotelId", "1")
                        .with(customerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.hotelId").value(1))
                .andExpect(jsonPath("$.result.status").value("PERSONALIZED_READY"))
                .andExpect(jsonPath("$.result.nonBindingSuggestion").value(true))
                .andExpect(jsonPath("$.result.pricingOrPromotionMutated").value(false))
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                        .isEqualTo("private, no-store"));

        verify(recommendationService).recommend(ISSUER, SUBJECT, 1, 3);
    }

    @Test
    void acceptsExplicitBoundedSuggestionLimit() throws Exception {
        when(recommendationService.recommend(ISSUER, SUBJECT, 1, 10)).thenReturn(response());

        mockMvc.perform(get("/api/v1/me/personalization/recommendations")
                        .param("hotelId", "1")
                        .param("maxSuggestions", "10")
                        .with(customerJwt()))
                .andExpect(status().isOk());

        verify(recommendationService).recommend(ISSUER, SUBJECT, 1, 10);
    }

    @Test
    void rejectsUnknownRepeatedAndOutOfRangeQueryParameters() throws Exception {
        var customer = customerJwt();
        mockMvc.perform(get("/api/v1/me/personalization/recommendations").param("hotelId", "1", "2").with(customer))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/me/personalization/recommendations").param("hotelId", "1").param("debug", "true").with(customer))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/me/personalization/recommendations").param("hotelId", "32768").with(customer))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/me/personalization/recommendations").param("hotelId", "1").param("maxSuggestions", "11").with(customer))
                .andExpect(status().isBadRequest());

        verify(recommendationService, never()).recommend(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void requiresAuthenticatedJwtClaimsAndKeepsDemoRouteDisabledByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/me/personalization/recommendations").param("hotelId", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/me/personalization/recommendations")
                        .param("hotelId", "1")
                        .with(jwt().jwt(jwt -> jwt.issuer(ISSUER).subject(SUBJECT))
                                .authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.CACHE_CONTROL, "private, no-store"));

        verify(recommendationService, never()).recommend(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());

        ConditionalOnProperty flag = PersonalizationRecommendationController.class
                .getAnnotation(ConditionalOnProperty.class);
        PreAuthorize role = PersonalizationRecommendationController.class.getAnnotation(PreAuthorize.class);
        assertThat(flag.havingValue()).isEqualTo("true");
        assertThat(flag.matchIfMissing()).isFalse();
        assertThat(role.value()).isEqualTo("hasRole('CUSTOMER')");
    }

    @Test
    void controllerIsNotRegisteredWhenLocalDemoFlagIsMissingOrFalse() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(ControllerOnlyConfiguration.class);

        runner.run(context -> assertThat(context.getBeansOfType(PersonalizationRecommendationController.class)).isEmpty());
        runner.withPropertyValues("hotel.ai.personalization.local-demo-enabled=false")
                .run(context -> assertThat(context.getBeansOfType(PersonalizationRecommendationController.class)).isEmpty());
    }

    @Configuration
    @Import(PersonalizationRecommendationController.class)
    static class ControllerOnlyConfiguration {

        @Bean
        PersonalizationRecommendationService recommendationService() {
            return mock(PersonalizationRecommendationService.class);
        }
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return jwt().jwt(jwt -> jwt.issuer(ISSUER).subject(SUBJECT))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private PersonalizationRecommendationResponse response() {
        return new PersonalizationRecommendationResponse(
                PersonalizationRecommendationStatus.PERSONALIZED_READY,
                1,
                "LOCAL_SYNTHETIC",
                List.of(),
                true,
                false,
                null
        );
    }
}
