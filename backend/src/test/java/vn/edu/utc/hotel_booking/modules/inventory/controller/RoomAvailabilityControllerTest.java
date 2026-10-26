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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilityTriggerRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Room Availability Controller Test")
class RoomAvailabilityControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RoomAvailabilityService roomAvailabilityService;

    @Mock
    private RoomAvailabilityGenerationService roomAvailabilityGenerationService;

    @InjectMocks
    private RoomAvailabilityController roomAvailabilityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomAvailabilityController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("triggerRoomType: Kích hoạt sinh sẵn thành công cho 1 hạng phòng")
    void testTriggerRoomType_Success() throws Exception {
        RoomAvailabilityTriggerRequest req = RoomAvailabilityTriggerRequest.builder()
                .startDate(LocalDate.of(2026, 11, 1))
                .horizonDays(365)
                .build();

        when(roomAvailabilityGenerationService.generateForRoomType(eq(10), eq(LocalDate.of(2026, 11, 1)), eq(365)))
                .thenReturn(365);

        mockMvc.perform(post("/api/v1/inventory/availability/trigger/room-type/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.scope").value("ROOM_TYPE"))
                .andExpect(jsonPath("$.result.targetId").value(10))
                .andExpect(jsonPath("$.result.recordsGenerated").value(365))
                .andExpect(jsonPath("$.result.status").value("SUCCESS"));

        verify(roomAvailabilityGenerationService).generateForRoomType(eq(10), eq(LocalDate.of(2026, 11, 1)), eq(365));
    }

    @Test
    @DisplayName("triggerRoomType: Mặc định startDate và horizonDays khi không truyền body")
    void testTriggerRoomType_DefaultWhenNoBody() throws Exception {
        when(roomAvailabilityGenerationService.generateForRoomType(eq(10), any(LocalDate.class), eq(365)))
                .thenReturn(365);

        mockMvc.perform(post("/api/v1/inventory/availability/trigger/room-type/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.scope").value("ROOM_TYPE"))
                .andExpect(jsonPath("$.result.targetId").value(10))
                .andExpect(jsonPath("$.result.horizonDays").value(365))
                .andExpect(jsonPath("$.result.recordsGenerated").value(365));
    }

    @Test
    @DisplayName("triggerHotel: Kích hoạt cuộn tồn kho thành công cho 1 khách sạn")
    void testTriggerHotel_Success() throws Exception {
        when(roomAvailabilityGenerationService.rollHorizonForHotel(eq((short) 1), eq(365)))
                .thenReturn(15);

        mockMvc.perform(post("/api/v1/inventory/availability/trigger/hotel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.scope").value("HOTEL"))
                .andExpect(jsonPath("$.result.targetId").value(1))
                .andExpect(jsonPath("$.result.recordsGenerated").value(15))
                .andExpect(jsonPath("$.result.status").value("SUCCESS"));

        verify(roomAvailabilityGenerationService).rollHorizonForHotel(eq((short) 1), eq(365));
    }

    @Test
    @DisplayName("triggerChain: Kích hoạt cuộn tồn kho thành công toàn chuỗi")
    void testTriggerChain_Success() throws Exception {
        when(roomAvailabilityGenerationService.rollHorizonForAllActiveRoomTypes(eq(365)))
                .thenReturn(1825);

        mockMvc.perform(post("/api/v1/inventory/availability/trigger/chain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.scope").value("CHAIN"))
                .andExpect(jsonPath("$.result.recordsGenerated").value(1825))
                .andExpect(jsonPath("$.result.status").value("SUCCESS"));

        verify(roomAvailabilityGenerationService).rollHorizonForAllActiveRoomTypes(eq(365));
    }

    @Test
    @DisplayName("triggerRoomType: Báo lỗi 400 Bad Request khi horizonDays vượt quá 730 ngày")
    void testTriggerRoomType_ExceedMaxHorizon() throws Exception {
        RoomAvailabilityTriggerRequest req = RoomAvailabilityTriggerRequest.builder()
                .horizonDays(1000)
                .build();

        mockMvc.perform(post("/api/v1/inventory/availability/trigger/room-type/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("query: Tra cứu tồn phòng thành công")
    void testQuery_Success() throws Exception {
        RoomAvailabilitySearchDto searchDto = RoomAvailabilitySearchDto.builder()
                .hotelRoomTypeId(10)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 5))
                .build();

        when(roomAvailabilityService.getAvailability(any(RoomAvailabilitySearchDto.class)))
                .thenReturn(Collections.singletonList(RoomAvailabilityResponse.builder()
                        .hotelRoomTypeId(10)
                        .date(LocalDate.of(2026, 11, 1))
                        .availableCount(15)
                        .build()));

        mockMvc.perform(post("/api/v1/inventory/availability/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].hotelRoomTypeId").value(10))
                .andExpect(jsonPath("$.result[0].availableCount").value(15));
    }

    @Test
    @DisplayName("hold: Khóa giữ tồn phòng thành công")
    void testHold_Success() throws Exception {
        HoldInventoryRequest req = HoldInventoryRequest.builder()
                .hotelRoomTypeId(10)
                .checkInDate(LocalDate.of(2026, 11, 1))
                .checkOutDate(LocalDate.of(2026, 11, 3))
                .roomsCount(2)
                .holdMinutes(15)
                .build();

        mockMvc.perform(post("/api/v1/inventory/availability/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(roomAvailabilityService).holdInventory(any(HoldInventoryRequest.class));
    }

    @Test
    @DisplayName("release: Hủy khóa giữ tồn phòng thành công")
    void testRelease_Success() throws Exception {
        ReleaseInventoryRequest req = ReleaseInventoryRequest.builder()
                .hotelRoomTypeId(10)
                .checkInDate(LocalDate.of(2026, 11, 1))
                .checkOutDate(LocalDate.of(2026, 11, 3))
                .roomsCount(2)
                .build();

        mockMvc.perform(post("/api/v1/inventory/availability/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(roomAvailabilityService).releaseInventory(any(ReleaseInventoryRequest.class));
    }
}
