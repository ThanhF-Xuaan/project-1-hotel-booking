package vn.edu.utc.hotel_booking.modules.operation.controller;

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
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.exception.GlobalHandlerException;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityReadingResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.UtilityReadingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UtilityReadingControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private UtilityReadingService service;

    @InjectMocks
    private UtilityReadingController controller;

    private UtilityReadingResponse readingResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandlerException())
                .build();

        readingResponse = new UtilityReadingResponse();
        readingResponse.setId(1L);
        readingResponse.setMeterId(1);
        readingResponse.setMeterCode("EL-01");
        readingResponse.setReadingDate(LocalDate.now());
        readingResponse.setReadingValue(new BigDecimal("100.500"));
        readingResponse.setIsMeterReset(false);
        readingResponse.setUsage(new BigDecimal("10.500"));
    }

    @Test
    @DisplayName("POST /api/v1/utility-readings/filter - Thành công lọc danh sách chỉ số")
    void search_Success() throws Exception {
        UtilityReadingSearchDto searchDto = UtilityReadingSearchDto.builder()
                .meterId(1)
                .page(0)
                .pageSize(10)
                .build();

        PageResponse<UtilityReadingResponse> pageResponse = PageResponse.<UtilityReadingResponse>builder()
                .content(List.of(readingResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(service.search(any(UtilityReadingSearchDto.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/utility-readings/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].meterCode").value("EL-01"));

        verify(service).search(any(UtilityReadingSearchDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/utility-readings/filter - Lỗi validation khi page là số âm")
    void search_ValidationError_NegativePage() throws Exception {
        UtilityReadingSearchDto searchDto = UtilityReadingSearchDto.builder()
                .page(-1)
                .pageSize(10)
                .build();

        mockMvc.perform(post("/api/v1/utility-readings/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).search(any());
    }

    @Test
    @DisplayName("GET /api/v1/utility-readings/{id} - Lấy chi tiết chỉ số thành công")
    void getById_Success() throws Exception {
        when(service.getById(1L)).thenReturn(readingResponse);

        mockMvc.perform(get("/api/v1/utility-readings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.readingValue").value(100.5));

        verify(service).getById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/utility-readings/{id} - Lỗi 404 khi không tìm thấy chỉ số")
    void getById_NotFound() throws Exception {
        when(service.getById(99L)).thenThrow(new AppException(ErrorCode.UTILITY_READING_NOT_FOUND));

        mockMvc.perform(get("/api/v1/utility-readings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(9051));

        verify(service).getById(99L);
    }

    @Test
    @DisplayName("POST /api/v1/utility-readings - Thêm mới chỉ số thành công")
    void create_Success() throws Exception {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.now());
        request.setReadingValue(new BigDecimal("100.500"));
        request.setIsMeterReset(false);

        when(service.create(any(UtilityReadingCreateRequest.class))).thenReturn(readingResponse);

        mockMvc.perform(post("/api/v1/utility-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.readingValue").value(100.5));

        verify(service).create(any(UtilityReadingCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/utility-readings - Lỗi validation khi ngày đọc ở tương lai")
    void create_ValidationError_FutureDate() throws Exception {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.now().plusDays(2)); // Future date violates @PastOrPresent
        request.setReadingValue(new BigDecimal("100.500"));
        request.setIsMeterReset(false);

        mockMvc.perform(post("/api/v1/utility-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/utility-readings - Lỗi validation khi số chữ số thập phân vượt quá 3")
    void create_ValidationError_DecimalPrecisionTooHigh() throws Exception {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.now());
        request.setReadingValue(new BigDecimal("100.1234")); // 4 fraction digits violates @Digits(integer = 9, fraction = 3)
        request.setIsMeterReset(false);

        mockMvc.perform(post("/api/v1/utility-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/utility-readings - Lỗi khi đã có chỉ số trong ngày")
    void create_AlreadyExists() throws Exception {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.now());
        request.setReadingValue(new BigDecimal("100.500"));
        request.setIsMeterReset(false);

        when(service.create(any(UtilityReadingCreateRequest.class)))
                .thenThrow(new AppException(ErrorCode.UTILITY_READING_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/utility-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9052));
    }

    @Test
    @DisplayName("PUT /api/v1/utility-readings/{id} - Cập nhật chỉ số thành công")
    void update_Success() throws Exception {
        UtilityReadingUpdateRequest request = new UtilityReadingUpdateRequest();
        request.setReadingValue(new BigDecimal("110.000"));
        request.setIsMeterReset(false);

        when(service.update(eq(1L), any(UtilityReadingUpdateRequest.class))).thenReturn(readingResponse);

        mockMvc.perform(put("/api/v1/utility-readings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        verify(service).update(eq(1L), any(UtilityReadingUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/utility-readings/{id} - Lỗi validation khi giá trị chỉ số âm")
    void update_ValidationError_NegativeValue() throws Exception {
        UtilityReadingUpdateRequest request = new UtilityReadingUpdateRequest();
        request.setReadingValue(new BigDecimal("-5.0"));

        mockMvc.perform(put("/api/v1/utility-readings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).update(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/utility-readings/delete - Xóa mềm danh sách chỉ số thành công")
    void delete_Success() throws Exception {
        doNothing().when(service).delete(List.of(1L, 2L));

        mockMvc.perform(delete("/api/v1/utility-readings/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        verify(service).delete(List.of(1L, 2L));
    }
}
