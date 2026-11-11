package vn.edu.utc.hotel_booking.modules.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import vn.edu.utc.hotel_booking.common.exception.GlobalHandlerException;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeInclusionRequest;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionFrequency;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionTargetType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeInclusionService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelRoomTypeInclusionController — Unit Tests")
class HotelRoomTypeInclusionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private HotelRoomTypeInclusionService inclusionService;

    @InjectMocks
    private HotelRoomTypeInclusionController controller;

    private RoomInclusionDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandlerException())
                .build();

        sampleResponse = RoomInclusionDto.builder()
                .id(1L)
                .menuId(5)
                .sourceType("HOTEL_MENU")
                .name("Rượu Vang Đỏ Đà Lạt")
                .unit("CHAI")
                .quantity(1)
                .frequency("PER_STAY")
                .targetType("PER_ROOM")
                .description("Tặng 1 chai rượu vang chào mừng")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/inventory/hotel-room-types/{hotelRoomTypeId}/inclusions — Thành công")
    void getInclusions_Success() throws Exception {
        when(inclusionService.getInclusionsByHotelRoomTypeId(101)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/inventory/hotel-room-types/101/inclusions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].id").value(1))
                .andExpect(jsonPath("$.result[0].name").value("Rượu Vang Đỏ Đà Lạt"))
                .andExpect(jsonPath("$.result[0].unit").value("CHAI"));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/hotel-room-types/{hotelRoomTypeId}/inclusions — Thành công")
    void createInclusion_Success() throws Exception {
        HotelRoomTypeInclusionRequest request = HotelRoomTypeInclusionRequest.builder()
                .menuId(5)
                .quantity(1)
                .frequency(InclusionFrequency.PER_STAY)
                .targetType(InclusionTargetType.PER_ROOM)
                .customDisplayName("Rượu vang chào mừng")
                .build();

        when(inclusionService.createInclusion(eq(101), any(HotelRoomTypeInclusionRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/inventory/hotel-room-types/101/inclusions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.name").value("Rượu Vang Đỏ Đà Lạt"));
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/hotel-room-types/{hotelRoomTypeId}/inclusions/{inclusionId} — Thành công")
    void updateInclusion_Success() throws Exception {
        HotelRoomTypeInclusionRequest request = HotelRoomTypeInclusionRequest.builder()
                .masterItemId(2)
                .quantity(2)
                .frequency(InclusionFrequency.PER_NIGHT)
                .targetType(InclusionTargetType.PER_ADULT)
                .build();

        when(inclusionService.updateInclusion(eq(1L), any(HotelRoomTypeInclusionRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/inventory/hotel-room-types/101/inclusions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/v1/inventory/hotel-room-types/{hotelRoomTypeId}/inclusions/{inclusionId} — Thành công")
    void deleteInclusion_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/hotel-room-types/101/inclusions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inclusionService).deleteInclusion(1L);
    }
}

