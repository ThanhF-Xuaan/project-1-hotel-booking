package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import vn.edu.utc.hotel_booking.common.config.JwtBlacklistFilter;
import vn.edu.utc.hotel_booking.common.config.SecurityConfig;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorChatRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        PublicRoomAdvisorController.class,
        SecurityConfig.class,
        PublicRoomAdvisorSecurityIntegrationTest.TestSecurityContextConfig.class
})
class PublicRoomAdvisorSecurityIntegrationTest {

    @Configuration
    @EnableWebMvc
    @Import(JwtBlacklistFilter.class)
    static class TestSecurityContextConfig {

        @Bean
        public RoomAdvisorOrchestrationService roomAdvisorOrchestrationService() {
            RoomAdvisorOrchestrationService mock = mock(RoomAdvisorOrchestrationService.class);
            AdvisorResultDto dummyResult = AdvisorResultDto.builder()
                    .contractVersion("2.0")
                    .requestId(UUID.randomUUID())
                    .sessionId(UUID.randomUUID())
                    .sessionVersion(1L)
                    .status(AdvisorStatus.RESULTS)
                    .reply("Tư vấn thành công")
                    .build();
            when(mock.chat(any(), any())).thenReturn(dummyResult);
            when(mock.search(any(), any())).thenReturn(dummyResult);
            when(mock.refresh(any(), any())).thenReturn(dummyResult);
            return mock;
        }

        @Bean
        public TokenBlacklistService tokenBlacklistService() {
            return mock(TokenBlacklistService.class);
        }

        @Bean
        public JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper;
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Canonical /api/v1/public/ai/room-advisor/chat allows anonymous access without CSRF token (200 OK)")
    void canonicalChatEndpointAllowsAnonymousAccessWithoutCsrf() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Canonical /api/v1/public/ai/room-advisor/search allows anonymous access without CSRF token (200 OK)")
    void canonicalSearchEndpointAllowsAnonymousAccessWithoutCsrf() throws Exception {
        String searchBody = """
                {
                  "contractVersion": "2.0",
                  "criteria": {
                    "hotelId": 1,
                    "checkInDate": "2026-11-20",
                    "checkOutDate": "2026-11-22",
                    "adults": 2,
                    "roomsCount": 1
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Undocumented /api/v1/ai/room-advisor/chat is blocked from anonymous access (401 Unauthorized)")
    void undocumentedAliasIsBlockedFromAnonymousAccess() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng trái phép qua alias")
                .build();

        mockMvc.perform(post("/api/v1/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Canonical /api/v1/public/ai/room-advisor/refresh allows anonymous access without CSRF token (200 OK)")
    void canonicalRefreshEndpointAllowsAnonymousAccessWithoutCsrf() throws Exception {
        String refreshBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s",
                  "expectedSessionVersion": 1
                }
                """, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Undocumented /api/v1/ai/room-advisor/refresh is blocked from anonymous access (401 Unauthorized)")
    void undocumentedRefreshAliasIsBlockedFromAnonymousAccess() throws Exception {
        String refreshBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s"
                }
                """, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }
}
