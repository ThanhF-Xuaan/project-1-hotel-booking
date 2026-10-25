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
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCancelRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractDepositRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupContractResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupContractService;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Group Contract Controller Test")
class GroupContractControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GroupContractService groupContractService;

    @Mock
    private StaffService staffService;

    @InjectMocks
    private GroupContractController groupContractController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(groupContractController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/group-contracts: Tạo hợp đồng thành công trả về HTTP 201")
    void createContract_shouldReturn201Created() throws Exception {
        GroupContractCreateRequest request = GroupContractCreateRequest.builder()
                .hotelId((short) 1)
                .companyId(5L)
                .groupName("VinFast Global Convention")
                .companyTaxCode("0108999999")
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(13))
                .totalRooms(10)
                .totalGuests(20)
                .depositRate(BigDecimal.valueOf(50.00))
                .roomBlocks(List.of(
                        GroupContractCreateRequest.ContractRoomBlockItem.builder()
                                .hotelRoomTypeId(1)
                                .blockedRooms(10)
                                .negotiatedRatePerNight(BigDecimal.valueOf(1500000))
                                .build()
                ))
                .build();

        GroupContractResponse response = GroupContractResponse.builder()
                .id(100L)
                .contractNumber("GC-20261026-0001")
                .status(GroupContractStatus.TENTATIVE)
                .totalRooms(10)
                .build();

        when(groupContractService.createContract(any(GroupContractCreateRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/group-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.contractNumber").value("GC-20261026-0001"))
                .andExpect(jsonPath("$.result.status").value("TENTATIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/group-contracts/filter: Tìm kiếm hợp đồng trả về HTTP 200")
    void filterContracts_shouldReturn200Ok() throws Exception {
        GroupContractSearchDto searchDto = GroupContractSearchDto.builder()
                .hotelId((short) 1)
                .status(GroupContractStatus.TENTATIVE)
                .build();

        PageResponse<GroupContractResponse> pageResponse = PageResponse.<GroupContractResponse>builder()
                .content(Collections.emptyList())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(groupContractService.filterContracts(any(GroupContractSearchDto.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/group-contracts/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/group-contracts/{id}: Lấy chi tiết hợp đồng trả về HTTP 200")
    void getContractById_shouldReturn200Ok() throws Exception {
        GroupContractResponse response = GroupContractResponse.builder()
                .id(1L)
                .contractNumber("GC-20261026-0001")
                .status(GroupContractStatus.DEPOSIT_PAID)
                .masterFolioNumber("MF-20261026-0001")
                .build();

        when(groupContractService.getContractById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/group-contracts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.contractNumber").value("GC-20261026-0001"))
                .andExpect(jsonPath("$.result.masterFolioNumber").value("MF-20261026-0001"));
    }

    @Test
    @DisplayName("POST /api/v1/group-contracts/{id}/deposit: Xác nhận cọc 50% trả về HTTP 200")
    void confirmDeposit_shouldReturn200Ok() throws Exception {
        GroupContractDepositRequest request = GroupContractDepositRequest.builder()
                .amount(BigDecimal.valueOf(50000000))
                .transactionReference("TXN-VNPAY-123456")
                .build();

        GroupContractResponse response = GroupContractResponse.builder()
                .id(1L)
                .contractNumber("GC-20261026-0001")
                .status(GroupContractStatus.DEPOSIT_PAID)
                .depositPaidAmount(BigDecimal.valueOf(50000000))
                .masterFolioId(10L)
                .build();

        when(groupContractService.confirmDeposit(eq(1L), any(GroupContractDepositRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/group-contracts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("DEPOSIT_PAID"))
                .andExpect(jsonPath("$.result.masterFolioId").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/group-contracts/{id}/cancel: Hủy hợp đồng trả về HTTP 200")
    void cancelContract_shouldReturn200Ok() throws Exception {
        GroupContractCancelRequest request = GroupContractCancelRequest.builder()
                .cancellationReason("Lịch công tác bị hoãn")
                .build();

        GroupContractResponse response = GroupContractResponse.builder()
                .id(1L)
                .status(GroupContractStatus.CANCELLED)
                .cancellationReason("Lịch công tác bị hoãn")
                .build();

        when(groupContractService.cancelContract(eq(1L), any(GroupContractCancelRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/group-contracts/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("CANCELLED"));
    }
}
