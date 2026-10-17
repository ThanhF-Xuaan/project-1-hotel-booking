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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityMeterResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;
import vn.edu.utc.hotel_booking.modules.operation.service.UtilityMeterService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UtilityMeterControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private UtilityMeterService service;

    @InjectMocks
    private UtilityMeterController controller;

    private UtilityMeterResponse meterResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandlerException())
                .build();

        meterResponse = new UtilityMeterResponse();
        meterResponse.setId(1);
        meterResponse.setHotelId((short) 1);
        meterResponse.setMeterCode("EL-01");
        meterResponse.setMeterType(MeterType.ELECTRICITY);
        meterResponse.setLocationLabel("Lobby");
    }

    @Test
    @DisplayName("POST /api/v1/utility-meters/filter - Thành công lọc danh sách đồng hồ")
    void search_Success() throws Exception {
        UtilityMeterSearchDto searchDto = UtilityMeterSearchDto.builder()
                .hotelId((short) 1)
                .page(0)
                .pageSize(10)
                .build();

        PageResponse<UtilityMeterResponse> pageResponse = PageResponse.<UtilityMeterResponse>builder()
                .content(List.of(meterResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(service.search(any(UtilityMeterSearchDto.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/utility-meters/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].meterCode").value("EL-01"));

        verify(service).search(any(UtilityMeterSearchDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/utility-meters/filter - Lỗi validation khi pageSize vượt quá 100")
    void search_ValidationError_PageSizeTooLarge() throws Exception {
        UtilityMeterSearchDto searchDto = UtilityMeterSearchDto.builder()
                .page(0)
                .pageSize(200) // Vượt quá @Max(100)
                .build();

        mockMvc.perform(post("/api/v1/utility-meters/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).search(any());
    }

    @Test
    @DisplayName("GET /api/v1/utility-meters/{id} - Lấy chi tiết đồng hồ thành công")
    void getById_Success() throws Exception {
        when(service.getById(1)).thenReturn(meterResponse);

        mockMvc.perform(get("/api/v1/utility-meters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.meterCode").value("EL-01"));

        verify(service).getById(1);
    }

    @Test
    @DisplayName("GET /api/v1/utility-meters/{id} - Lỗi 404 khi không tìm thấy đồng hồ")
    void getById_NotFound() throws Exception {
        when(service.getById(99)).thenThrow(new AppException(ErrorCode.UTILITY_METER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/utility-meters/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(9041));

        verify(service).getById(99);
    }

    @Test
    @DisplayName("POST /api/v1/utility-meters - Thêm mới đồng hồ thành công")
    void create_Success() throws Exception {
        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        request.setHotelId((short) 1);
        request.setMeterCode("EL-01");
        request.setMeterType(MeterType.ELECTRICITY);
        request.setLocationLabel("Lobby");

        when(service.create(any(UtilityMeterCreateRequest.class))).thenReturn(meterResponse);

        mockMvc.perform(post("/api/v1/utility-meters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.meterCode").value("EL-01"));

        verify(service).create(any(UtilityMeterCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/utility-meters - Lỗi validation khi thiếu meterCode hoặc hotelId")
    void create_ValidationError_MissingFields() throws Exception {
        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        // Thiếu hotelId, meterCode, meterType

        mockMvc.perform(post("/api/v1/utility-meters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/utility-meters - Lỗi validation khi locationLabel vượt quá 100 ký tự")
    void create_ValidationError_LocationLabelTooLong() throws Exception {
        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        request.setHotelId((short) 1);
        request.setMeterCode("EL-01");
        request.setMeterType(MeterType.ELECTRICITY);
        request.setLocationLabel("A".repeat(101));

        mockMvc.perform(post("/api/v1/utility-meters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/utility-meters - Lỗi khi mã đồng hồ đã tồn tại")
    void create_AlreadyExists() throws Exception {
        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        request.setHotelId((short) 1);
        request.setMeterCode("EL-01");
        request.setMeterType(MeterType.ELECTRICITY);

        when(service.create(any(UtilityMeterCreateRequest.class)))
                .thenThrow(new AppException(ErrorCode.UTILITY_METER_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/utility-meters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9042));
    }

    @Test
    @DisplayName("PUT /api/v1/utility-meters/{id} - Cập nhật đồng hồ thành công")
    void update_Success() throws Exception {
        UtilityMeterUpdateRequest request = new UtilityMeterUpdateRequest();
        request.setMeterCode("EL-02");
        request.setLocationLabel("Floor 2");

        when(service.update(eq(1), any(UtilityMeterUpdateRequest.class))).thenReturn(meterResponse);

        mockMvc.perform(put("/api/v1/utility-meters/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        verify(service).update(eq(1), any(UtilityMeterUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/utility-meters/{id} - Lỗi validation khi meterCode vượt quá 50 ký tự")
    void update_ValidationError_MeterCodeTooLong() throws Exception {
        UtilityMeterUpdateRequest request = new UtilityMeterUpdateRequest();
        request.setMeterCode("A".repeat(51));

        mockMvc.perform(put("/api/v1/utility-meters/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        verify(service, never()).update(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/utility-meters/delete - Xóa mềm danh sách đồng hồ thành công")
    void delete_Success() throws Exception {
        doNothing().when(service).delete(List.of(1, 2));

        mockMvc.perform(delete("/api/v1/utility-meters/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        verify(service).delete(List.of(1, 2));
    }
}
