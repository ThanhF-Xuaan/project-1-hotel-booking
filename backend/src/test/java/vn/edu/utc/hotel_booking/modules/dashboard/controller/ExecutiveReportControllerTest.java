package vn.edu.utc.hotel_booking.modules.dashboard.controller;

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
import vn.edu.utc.hotel_booking.modules.dashboard.dto.request.ExecutiveReportFilterDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveBiReportResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveKpiSummaryDto;
import vn.edu.utc.hotel_booking.modules.dashboard.service.ExecutiveReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Executive BI Report Controller Test")
class ExecutiveReportControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ExecutiveReportService executiveReportService;

    @InjectMocks
    private ExecutiveReportController executiveReportController;

    private ExecutiveBiReportResponse mockReportResponse;
    private ExecutiveKpiSummaryDto mockSummaryDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(executiveReportController).build();

        mockSummaryDto = ExecutiveKpiSummaryDto.builder()
                .occupancyRate(75.5)
                .averageDailyRate(BigDecimal.valueOf(1200000))
                .revPar(BigDecimal.valueOf(906000))
                .totalActiveRooms(100)
                .occupiedRoomNights(75)
                .availableRoomNights(100L)
                .totalRoomRevenue(BigDecimal.valueOf(90000000))
                .totalFnbRevenue(BigDecimal.valueOf(30000000))
                .totalOtherRevenue(BigDecimal.valueOf(5000000))
                .totalRevenue(BigDecimal.valueOf(125000000))
                .build();

        mockReportResponse = ExecutiveBiReportResponse.builder()
                .scope("CHAIN")
                .scopeName("Toàn hệ thống Chuỗi Khách sạn")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 25))
                .isProvisional(false)
                .summary(mockSummaryDto)
                .regionBreakdowns(Collections.emptyList())
                .hotelBreakdowns(Collections.emptyList())
                .trends(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/analytics/reports/executive - Lấy báo cáo quản trị BI thành công")
    void getExecutiveReport_Success() throws Exception {
        when(executiveReportService.generateExecutiveReport(any(ExecutiveReportFilterDto.class)))
                .thenReturn(mockReportResponse);

        ExecutiveReportFilterDto request = ExecutiveReportFilterDto.builder()
                .scope("CHAIN")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 25))
                .build();

        mockMvc.perform(post("/api/v1/analytics/reports/executive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.scope").value("CHAIN"))
                .andExpect(jsonPath("$.result.scopeName").value("Toàn hệ thống Chuỗi Khách sạn"))
                .andExpect(jsonPath("$.result.isProvisional").value(false))
                .andExpect(jsonPath("$.result.summary.occupancyRate").value(75.5))
                .andExpect(jsonPath("$.result.summary.revPar").value(906000));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/reports/executive/summary - Lấy tóm tắt KPI cấp cao thành công")
    void getQuickSummary_Success() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 26);
        when(executiveReportService.getQuickSummary(eq("CHAIN"), any(), any(), eq(date)))
                .thenReturn(mockSummaryDto);

        mockMvc.perform(get("/api/v1/analytics/reports/executive/summary")
                        .param("scope", "CHAIN")
                        .param("targetDate", "2026-10-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.occupancyRate").value(75.5))
                .andExpect(jsonPath("$.result.averageDailyRate").value(1200000))
                .andExpect(jsonPath("$.result.totalRevenue").value(125000000));
    }
}
