package vn.edu.utc.hotel_booking.modules.identity.personalization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentView;
import vn.edu.utc.hotel_booking.modules.identity.personalization.enums.PersonalizationConsentStatus;
import vn.edu.utc.hotel_booking.modules.identity.personalization.service.PersonalizationConsentService;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestPropertySource(properties = "hotel.ai.personalization.local-demo-enabled=true")
@ContextConfiguration(classes = {
        PersonalizationConsentController.class,
        PersonalizationConsentExceptionHandler.class,
        SecurityConfig.class,
        PersonalizationConsentControllerTest.TestSecurityContextConfig.class
})
class PersonalizationConsentControllerTest {

    private static final String ISSUER = "http://localhost:18081/realms/hotel-acceptance";
    private static final String SUBJECT = "20000000-0000-0000-0000-000000000006";
    private static final String ROUTE = "/api/v1/me/personalization-consent";

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
        PersonalizationConsentService personalizationConsentService() {
            return mock(PersonalizationConsentService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    @Configuration
    @Import(PersonalizationConsentController.class)
    static class ControllerOnlyConfiguration {
        @Bean
        PersonalizationConsentService personalizationConsentService() {
            return mock(PersonalizationConsentService.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PersonalizationConsentService consentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(consentService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void getReturnsCurrentAccountConsentWithPrivateNoStore() throws Exception {
        when(consentService.getCurrentConsent(ISSUER, SUBJECT, (short) 1)).thenReturn(notGrantedView());

        mockMvc.perform(get(ROUTE)
                        .queryParam("hotelId", "1")
                        .with(customerJwt()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.hotelId").value(1))
                .andExpect(jsonPath("$.result.status").value("NOT_GRANTED"))
                .andExpect(jsonPath("$.result.revision").value(0))
                .andExpect(jsonPath("$.result.bindingVersion").value(1))
                .andExpect(jsonPath("$.result.dataMode").value("LOCAL_SYNTHETIC"));

        verify(consentService).getCurrentConsent(ISSUER, SUBJECT, (short) 1);
    }

    @Test
    void getRejectsUnknownQueryParametersAndRepeatedHotelId() throws Exception {
        mockMvc.perform(get(ROUTE)
                        .queryParam("hotelId", "1")
                        .queryParam("debug", "true")
                        .with(customerJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.result.reasonCode").value("INVALID_REQUEST"));

        mockMvc.perform(get(ROUTE)
                        .queryParam("hotelId", "1", "2")
                        .with(customerJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.result.reasonCode").value("INVALID_REQUEST"));

        verifyNoInteractions(consentService);
    }

    @Test
    void putAcceptsCurrentGrantAndUsesJwtIssuerAndSubject() throws Exception {
        PersonalizationConsentView grantedView = new PersonalizationConsentView(
                (short) 1,
                PersonalizationConsentStatus.GRANTED,
                true,
                1,
                1,
                "ROOM_TYPE_RECOMMENDATION",
                "u14-local-room-affinity-v1",
                "u14-local-room-affinity-v1",
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z"),
                12,
                "Demo synthetic data notice",
                "/portal/privacy",
                "LOCAL_SYNTHETIC"
        );
        when(consentService.updateConsent(eq(ISSUER), eq(SUBJECT), any())).thenReturn(grantedView);

        mockMvc.perform(put(ROUTE)
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hotelId":1,"granted":true,"expectedRevision":0,
                                 "expectedBindingVersion":1,"policyVersion":"u14-local-room-affinity-v1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.result.status").value("GRANTED"))
                .andExpect(jsonPath("$.result.revision").value(1));

        org.mockito.ArgumentCaptor<PersonalizationConsentUpdateRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(PersonalizationConsentUpdateRequest.class);
        verify(consentService).updateConsent(eq(ISSUER), eq(SUBJECT), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getHotelId()).isEqualTo((short) 1);
        assertThat(requestCaptor.getValue().getExpectedRevision()).isZero();
        assertThat(requestCaptor.getValue().isPolicyVersionProvided()).isTrue();
    }

    @Test
    void putRejectsUnknownJsonFieldsAndPolicyVersionOnRevoke() throws Exception {
        mockMvc.perform(put(ROUTE)
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hotelId":1,"granted":true,"expectedRevision":0,
                                 "expectedBindingVersion":1,"policyVersion":"u14-local-room-affinity-v1",
                                 "guestId":999}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.result.reasonCode").value("INVALID_REQUEST"));

        mockMvc.perform(put(ROUTE)
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hotelId":1,"granted":true,"expectedRevision":0,
                                 "expectedBindingVersion":1,"policyVersion":123}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.result.reasonCode").value("INVALID_REQUEST"));

        mockMvc.perform(put(ROUTE)
                        .with(customerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hotelId":1,"granted":false,"expectedRevision":0,
                                 "expectedBindingVersion":1,"policyVersion":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.result.reasonCode").value("INVALID_REQUEST"));

        verifyNoInteractions(consentService);
    }

    @Test
    void roleOtherThanCustomerGetsScopedForbiddenResponse() throws Exception {
        mockMvc.perform(get(ROUTE)
                        .queryParam("hotelId", "1")
                        .with(jwt().jwt(jwt -> jwt.issuer(ISSUER).subject(SUBJECT))
                                .authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.code").value(1007))
                .andExpect(jsonPath("$.result.reasonCode").value("FORBIDDEN"));

        verifyNoInteractions(consentService);
    }

    @Test
    void anonymousRequestKeepsExistingAuthenticationFilter401() throws Exception {
        mockMvc.perform(get(ROUTE).queryParam("hotelId", "1"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(consentService);
    }

    @Test
    void controllerIsNotRegisteredWhenLocalDemoFlagIsMissingOrFalse() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(ControllerOnlyConfiguration.class);

        runner.run(context -> assertThat(context.getBeansOfType(PersonalizationConsentController.class)).isEmpty());
        runner.withPropertyValues("hotel.ai.personalization.local-demo-enabled=false")
                .run(context -> assertThat(context.getBeansOfType(PersonalizationConsentController.class)).isEmpty());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor customerJwt() {
        return jwt().jwt(jwt -> jwt.issuer(ISSUER).subject(SUBJECT))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private PersonalizationConsentView notGrantedView() {
        return new PersonalizationConsentView(
                (short) 1,
                PersonalizationConsentStatus.NOT_GRANTED,
                false,
                0,
                1,
                "ROOM_TYPE_RECOMMENDATION",
                "u14-local-room-affinity-v1",
                null,
                null,
                12,
                "Demo synthetic data notice",
                "/portal/privacy",
                "LOCAL_SYNTHETIC"
        );
    }
}
