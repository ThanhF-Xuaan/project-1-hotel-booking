package vn.edu.utc.hotel_booking.modules.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StaffControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StaffService staffService;

    @InjectMocks
    private StaffController staffController;

    private StaffResponse staffResponse;
    private UUID keycloakId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(staffController).build();
        keycloakId = UUID.randomUUID();

        staffResponse = StaffResponse.builder()
                .id(1)
                .keycloakId(keycloakId)
                .username("receptionist_hn")
                .fullName("Nguyễn Văn A")
                .roleCode("RECEPTIONIST")
                .scopeType("PROPERTY")
                .scopeEntityId(1)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/staffs/filter - Lọc và phân trang nhân viên")
    void filter_Success() throws Exception {
        StaffSearchDto searchDto = new StaffSearchDto();
        searchDto.setKeyword("receptionist");

        PageResponse<StaffResponse> pageResponse = PageResponse.<StaffResponse>builder()
                .content(List.of(staffResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(staffService.filter(any(StaffSearchDto.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/staffs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.content[0].username").value("receptionist_hn"));
    }

    @Test
    @DisplayName("GET /api/v1/staffs/{id} - Lấy chi tiết nhân viên theo ID")
    void getById_Success() throws Exception {
        when(staffService.getById(1)).thenReturn(staffResponse);

        mockMvc.perform(get("/api/v1/staffs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.username").value("receptionist_hn"));
    }

    @Test
    @DisplayName("GET /api/v1/staffs/keycloak/{keycloakId} - Lấy nhân viên theo Keycloak UUID")
    void getByKeycloakId_Success() throws Exception {
        when(staffService.getByKeycloakId(keycloakId)).thenReturn(staffResponse);

        mockMvc.perform(get("/api/v1/staffs/keycloak/" + keycloakId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.keycloakId").value(keycloakId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/staffs - Tạo mới nhân viên")
    void create_Success() throws Exception {
        StaffCreateRequest request = StaffCreateRequest.builder()
                .roleId((short) 4)
                .scopeType("PROPERTY")
                .scopeEntityId(1)
                .departmentId((short) 1)
                .username("receptionist_hn")
                .email("receptionist@utc.edu.vn")
                .phone("0988889999")
                .firstName("A")
                .lastName("Nguyễn Văn")
                .password("Hotel@123456")
                .build();

        when(staffService.create(any(StaffCreateRequest.class))).thenReturn(staffResponse);

        mockMvc.perform(post("/api/v1/staffs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.result.username").value("receptionist_hn"));
    }

    @Test
    @DisplayName("PUT /api/v1/staffs/{id} - Cập nhật nhân viên")
    void update_Success() throws Exception {
        StaffUpdateRequest request = StaffUpdateRequest.builder()
                .firstName("Nguyen Van")
                .email("receptionist.new@utc.edu.vn")
                .build();

        when(staffService.update(eq(1), any(StaffUpdateRequest.class))).thenReturn(staffResponse);

        mockMvc.perform(put("/api/v1/staffs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/staffs/delete - Xóa mềm hàng loạt nhân viên")
    void deleteBatch_Success() throws Exception {
        List<Integer> ids = List.of(1, 2, 3);

        mockMvc.perform(delete("/api/v1/staffs/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(staffService).deleteBatch(ids);
    }
}
