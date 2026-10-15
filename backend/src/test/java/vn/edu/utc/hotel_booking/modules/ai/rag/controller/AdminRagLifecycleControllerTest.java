package vn.edu.utc.hotel_booking.modules.ai.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeDocumentVersion;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminRagLifecycleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RagKnowledgeLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminRagLifecycleController(lifecycleService))
                .build();
    }

    @Test
    @DisplayName("RBAC Contract: AdminRagLifecycleController must be secured with ROLE_CHAIN_ADMIN")
    void shouldHaveChainAdminPreAuthorizeAnnotation() {
        PreAuthorize classPreAuthorize = AdminRagLifecycleController.class.getAnnotation(PreAuthorize.class);
        assertThat(classPreAuthorize).isNotNull();
        assertThat(classPreAuthorize.value()).isEqualTo("hasRole('ROLE_CHAIN_ADMIN')");
    }

    @Test
    @DisplayName("POST /stage should return 200 with STAGED status for valid document")
    void shouldStageDocumentSuccessfully() throws Exception {
        RagIngestRequestDto request = RagIngestRequestDto.builder()
                .sourceId("POLICY-CHECKIN")
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .category(RagDocumentCategory.POLICY)
                .title("Chính sách nhận trả phòng")
                .content("Nhận phòng 14:00, trả phòng 12:00.")
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        RagIngestResultDto mockResult = RagIngestResultDto.builder()
                .documentId(UUID.randomUUID())
                .sourceId("POLICY-CHECKIN")
                .version(1)
                .status(RagVersionStatus.STAGED)
                .chunkCount(1)
                .build();

        when(lifecycleService.ingestAndStage(any(), eq(1))).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge/stage?version=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("STAGED"))
                .andExpect(jsonPath("$.result.chunkCount").value(1));
    }

    @Test
    @DisplayName("POST /stage should return 400 when document contains prohibited content")
    void shouldRejectStagingWhenPiiDetected() throws Exception {
        RagIngestRequestDto request = RagIngestRequestDto.builder()
                .sourceId("POLICY-PII")
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .category(RagDocumentCategory.POLICY)
                .title("Bảng lương nội bộ")
                .content("Bảng lương và CCCD 001200001234.")
                .build();

        RagIngestResultDto mockResult = RagIngestResultDto.builder()
                .sourceId("POLICY-PII")
                .version(1)
                .status(RagVersionStatus.REJECTED)
                .errorMessage("REJECTED_PII_DETECTED: CCCD found")
                .build();

        when(lifecycleService.ingestAndStage(any(), eq(1))).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge/stage?version=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("REJECTED_PII_DETECTED: CCCD found"));
    }

    @Test
    @DisplayName("POST /activate should return 200 with activated document")
    void shouldActivateVersionSuccessfully() throws Exception {
        RagActivationRequestDto request = RagActivationRequestDto.builder()
                .sourceId("POLICY-CHECKIN")
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .version(1)
                .build();

        KnowledgeDocumentVersion mockDoc = KnowledgeDocumentVersion.builder()
                .documentId(UUID.randomUUID())
                .sourceId("POLICY-CHECKIN")
                .version(1)
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .status(RagVersionStatus.ACTIVE)
                .build();

        when(lifecycleService.activate(any())).thenReturn(true);
        when(lifecycleService.getActiveDocument(eq(RagScopeType.HOTEL_SCOPED), eq(1), eq("POLICY-CHECKIN")))
                .thenReturn(Optional.of(mockDoc));

        mockMvc.perform(post("/api/v1/ai/admin/knowledge/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.version").value(1))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /rollback should return 200 with rolled back document")
    void shouldRollbackVersionSuccessfully() throws Exception {
        RagRollbackRequestDto request = RagRollbackRequestDto.builder()
                .sourceId("POLICY-CHECKIN")
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .targetVersion(1)
                .build();

        KnowledgeDocumentVersion mockDoc = KnowledgeDocumentVersion.builder()
                .documentId(UUID.randomUUID())
                .sourceId("POLICY-CHECKIN")
                .version(1)
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .status(RagVersionStatus.ACTIVE)
                .build();

        when(lifecycleService.rollback(any())).thenReturn(true);
        when(lifecycleService.getActiveDocument(eq(RagScopeType.HOTEL_SCOPED), eq(1), eq("POLICY-CHECKIN")))
                .thenReturn(Optional.of(mockDoc));

        mockMvc.perform(post("/api/v1/ai/admin/knowledge/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.version").value(1));
    }

    @Test
    @DisplayName("POST /delete should return 200 on successful deletion")
    void shouldDeleteVersionSuccessfully() throws Exception {
        RagDeleteRequestDto request = RagDeleteRequestDto.builder()
                .sourceId("POLICY-CHECKIN")
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .version(2)
                .build();

        when(lifecycleService.delete(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/ai/admin/knowledge/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /delete should return 400 when attempting to delete active version")
    void shouldRejectDeletingActiveVersion() throws Exception {
        RagDeleteRequestDto request = RagDeleteRequestDto.builder()
                .sourceId("POLICY-CHECKIN")
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(1)
                .version(1)
                .build();

        when(lifecycleService.delete(any()))
                .thenThrow(new IllegalStateException("Cannot delete active corpus version directly."));

        mockMvc.perform(post("/api/v1/ai/admin/knowledge/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Cannot delete active corpus version directly."));
    }
}
