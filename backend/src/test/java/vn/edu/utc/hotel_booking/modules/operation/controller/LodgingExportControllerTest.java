package vn.edu.utc.hotel_booking.modules.operation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.common.exception.GlobalHandlerException;
import vn.edu.utc.hotel_booking.modules.operation.service.LodgingExportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LodgingExportControllerTest {
    private LodgingExportService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(LodgingExportService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LodgingExportController(service))
                .setControllerAdvice(new GlobalHandlerException()).build();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 24, 999})
    void invalidCutoffReturns400WithoutCallingService(int hour) throws Exception {
        mvc.perform(post("/api/v1/lodging-export/download").contentType(MediaType.APPLICATION_JSON)
                        .content(request(hour)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 23})
    void boundaryHoursRemainValid(int hour) throws Exception {
        when(service.exportBcaLodgingReport(any())).thenReturn(new byte[]{80, 75});
        mvc.perform(post("/api/v1/lodging-export/download").contentType(MediaType.APPLICATION_JSON)
                        .content(request(hour)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        verify(service).exportBcaLodgingReport(argThat(request -> request.getCutOffHour() == hour));
    }

    private String request(int hour) {
        return "{\"hotelId\":1,\"date\":\"2026-10-25\",\"category\":\"VIETNAMESE\",\"cutOffHour\":" + hour + "}";
    }
}
