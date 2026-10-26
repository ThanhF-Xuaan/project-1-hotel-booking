package vn.edu.utc.hotel_booking.modules.finance.controller;

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
import vn.edu.utc.hotel_booking.modules.finance.dto.request.BillingRoutingRuleRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioAdjustmentRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.BillingRoutingRuleResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.MasterFolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.ChargeCategory;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolioStatus;
import vn.edu.utc.hotel_booking.modules.finance.entity.RoutingTarget;
import vn.edu.utc.hotel_booking.modules.finance.service.MasterFolioService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Master Folio Controller Test")
class MasterFolioControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private MasterFolioService masterFolioService;

    @InjectMocks
    private MasterFolioController masterFolioController;

    private MasterFolioResponse mockResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(masterFolioController).build();

        mockResponse = MasterFolioResponse.builder()
                .id(1L)
                .folioNumber("MF-20261026-0001")
                .contractId(100L)
                .contractNumber("GC-20261026-0001")
                .hotelId((short) 1)
                .hotelName("Grand Hotel Saigon")
                .companyId(10L)
                .companyName("Công ty Cổ phần Du lịch FPT")
                .buyerLegalName("Đoàn FPT Software")
                .buyerTaxCode("0102030405")
                .buyerAddress("10 Phạm Văn Bạch, Cầu Giấy, Hà Nội")
                .buyerEmailForInvoice("accounting@fpt.vn")
                .subtotalAmount(new BigDecimal("20000000.00"))
                .serviceFeeAmount(new BigDecimal("1000000.00"))
                .totalVatAmount(new BigDecimal("2100000.00"))
                .totalAmount(new BigDecimal("23100000.00"))
                .depositCredited(new BigDecimal("11550000.00"))
                .totalPaid(new BigDecimal("11550000.00"))
                .balance(new BigDecimal("11550000.00"))
                .status(MasterFolioStatus.OPEN)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/master-folios/{id} - Lấy Master Folio thành công")
    void getMasterFolioById_Success() throws Exception {
        when(masterFolioService.getMasterFolioById(1L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/master-folios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.folioNumber").value("MF-20261026-0001"))
                .andExpect(jsonPath("$.result.depositCredited").value(11550000.00))
                .andExpect(jsonPath("$.result.balance").value(11550000.00));
    }

    @Test
    @DisplayName("GET /api/v1/master-folios/contract/{contractId} - Lấy Master Folio theo hợp đồng thành công")
    void getMasterFolioByContractId_Success() throws Exception {
        when(masterFolioService.getMasterFolioByContractId(100L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/master-folios/contract/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.contractNumber").value("GC-20261026-0001"));
    }

    @Test
    @DisplayName("POST /api/v1/master-folios/{id}/routing-rules - Thêm quy tắc định tuyến thành công")
    void addRoutingRule_Success() throws Exception {
        BillingRoutingRuleRequest request = BillingRoutingRuleRequest.builder()
                .chargeCategory(ChargeCategory.BREAKFAST)
                .routeTarget(RoutingTarget.MASTER)
                .maxCoveredAmount(new BigDecimal("200000.00"))
                .isActive(true)
                .build();

        BillingRoutingRuleResponse ruleResponse = BillingRoutingRuleResponse.builder()
                .id(10L)
                .masterFolioId(1L)
                .chargeCategory(ChargeCategory.BREAKFAST)
                .routeTarget(RoutingTarget.MASTER)
                .maxCoveredAmount(new BigDecimal("200000.00"))
                .isActive(true)
                .build();

        when(masterFolioService.addRoutingRule(eq(1L), any(BillingRoutingRuleRequest.class))).thenReturn(ruleResponse);

        mockMvc.perform(post("/api/v1/master-folios/1/routing-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.chargeCategory").value("BREAKFAST"))
                .andExpect(jsonPath("$.result.routeTarget").value("MASTER"));
    }

    @Test
    @DisplayName("POST /api/v1/master-folios/{id}/adjust-routing - Điều chuyển chi phí thủ công thành công")
    void adjustRouting_Success() throws Exception {
        MasterFolioAdjustmentRequest request = MasterFolioAdjustmentRequest.builder()
                .bookingRoomId(101L)
                .chargeCategory(ChargeCategory.MINIBAR)
                .itemType("SERVICE_ORDER_DETAIL")
                .referenceId(302L)
                .itemDescription("Tiền Minibar phòng 301")
                .adjustedTarget(RoutingTarget.MASTER)
                .amount(new BigDecimal("44000.00"))
                .adjustmentReason("Giám đốc bao trọn gói Minibar")
                .build();

        when(masterFolioService.adjustItemRouting(eq(1L), any(MasterFolioAdjustmentRequest.class), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/master-folios/1/adjust-routing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/master-folios/{id}/status - Cập nhật trạng thái Master Folio thành công")
    void updateStatus_Success() throws Exception {
        MasterFolioStatusUpdateRequest request = MasterFolioStatusUpdateRequest.builder()
                .status(MasterFolioStatus.LOCKED_FOR_SETTLEMENT)
                .note("Khóa sổ để Kế toán đoàn rà soát công nợ")
                .build();

        mockResponse.setStatus(MasterFolioStatus.LOCKED_FOR_SETTLEMENT);
        when(masterFolioService.updateMasterFolioStatus(eq(1L), any(MasterFolioStatusUpdateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(patch("/api/v1/master-folios/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("LOCKED_FOR_SETTLEMENT"));
    }
}
