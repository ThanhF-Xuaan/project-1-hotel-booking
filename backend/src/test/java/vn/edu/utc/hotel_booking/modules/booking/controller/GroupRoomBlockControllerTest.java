package vn.edu.utc.hotel_booking.modules.booking.controller;

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
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockDailyResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockDailyStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockStatus;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Group Room Block Controller Test")
class GroupRoomBlockControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GroupRoomBlockService groupRoomBlockService;

    @InjectMocks
    private GroupRoomBlockController groupRoomBlockController;

    private GroupRoomBlockResponse sampleBlockResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(groupRoomBlockController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        GroupRoomBlockDailyResponse daily = GroupRoomBlockDailyResponse.builder()
                .id(1L)
                .roomBlockId(10L)
                .hotelRoomTypeId(5)
                .roomTypeName("Deluxe Sea View")
                .stayDate(LocalDate.of(2026, 11, 15))
                .blockedQuantity(6)
                .pickedUpQuantity(2)
                .releasedQuantity(0)
                .status(GroupRoomBlockDailyStatus.ACTIVE)
                .build();

        sampleBlockResponse = GroupRoomBlockResponse.builder()
                .id(10L)
                .contractId(100L)
                .hotelRoomTypeId(5)
                .roomTypeName("Deluxe Sea View")
                .blockedRooms(6)
                .pickedUpRooms(2)
                .releasedRooms(0)
                .negotiatedRatePerNight(BigDecimal.valueOf(1500000))
                .status(GroupRoomBlockStatus.DEFINITE)
                .dailyBlocks(List.of(daily))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/group-bookings/contracts/{contractId}/room-blocks returns room blocks list")
    void testGetBlocksByContractId_Success() throws Exception {
        when(groupRoomBlockService.getBlocksByContractId(eq(100L)))
                .thenReturn(List.of(sampleBlockResponse));

        mockMvc.perform(get("/api/v1/group-bookings/contracts/100/room-blocks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(10))
                .andExpect(jsonPath("$.result[0].contractId").value(100))
                .andExpect(jsonPath("$.result[0].blockedRooms").value(6))
                .andExpect(jsonPath("$.result[0].pickedUpRooms").value(2))
                .andExpect(jsonPath("$.result[0].dailyBlocks[0].blockedQuantity").value(6));
    }

    @Test
    @DisplayName("POST /api/v1/group-bookings/contracts/{contractId}/room-blocks/{blockId}/release-cutoff releases unpicked rooms")
    void testReleaseUnpickedUpRoomsManually_Success() throws Exception {
        sampleBlockResponse.setReleasedRooms(4);
        sampleBlockResponse.setBlockedRooms(2);

        when(groupRoomBlockService.releaseUnpickedUpRoomsManually(eq(100L), eq(10L)))
                .thenReturn(sampleBlockResponse);

        mockMvc.perform(post("/api/v1/group-bookings/contracts/100/room-blocks/10/release-cutoff")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.releasedRooms").value(4))
                .andExpect(jsonPath("$.message").value("Giải phóng phòng đoàn dôi dư thành công"));
    }

    @Test
    @DisplayName("POST /api/v1/group-bookings/scheduler/trigger-cutoff triggers manual cutoff auto-release")
    void testTriggerCutoffAutoRelease_Success() throws Exception {
        when(groupRoomBlockService.processCutoffAutoRelease()).thenReturn(8);

        mockMvc.perform(post("/api/v1/group-bookings/scheduler/trigger-cutoff")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8))
                .andExpect(jsonPath("$.message").value("Đã quét và giải phóng thành công 8 phòng dôi dư về Public Inventory"));
    }
}
