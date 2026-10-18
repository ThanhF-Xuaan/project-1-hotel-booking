package vn.edu.utc.hotel_booking.modules.finance.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.ErrorRowDto;
import vn.edu.utc.hotel_booking.modules.finance.service.PayrollSummaryService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PayrollSummaryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PayrollSummaryService payrollSummaryService;

    @InjectMocks
    private PayrollSummaryController payrollSummaryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(payrollSummaryController).build();
    }

    @Test
    void importCsv_success_returnsSuccessMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payroll.csv", "text/csv", "dummy content".getBytes()
        );

        when(payrollSummaryService.importCsv(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(multipart("/api/v1/payroll/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Import process completed"))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void importCsv_hasErrors_returnsErrorRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payroll.csv", "text/csv", "dummy content".getBytes()
        );

        ErrorRowDto errorRow = ErrorRowDto.builder().row(3).error("Lỗi dữ liệu").build();
        when(payrollSummaryService.importCsv(any())).thenReturn(List.of(errorRow));

        mockMvc.perform(multipart("/api/v1/payroll/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].row").value(3))
                .andExpect(jsonPath("$.result[0].error").value("Lỗi dữ liệu"));
    }
}
