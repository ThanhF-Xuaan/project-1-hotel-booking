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
import vn.edu.utc.hotel_booking.common.exception.GlobalHandlerException;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MasterItemResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("MasterItemController — Unit Tests")
class MasterItemControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private MasterItemService masterItemService;

    @InjectMocks
    private MasterItemController controller;

    private MasterItemResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandlerException())
                .build();

        sampleResponse = MasterItemResponse.builder()
                .id(1)
                .code("COFFEE_DEN")
                .name("Cà phê đen")
                .itemType(MenuType.PRODUCT)
                .defaultBasePrice(new BigDecimal("30000"))
                .taxCategoryId(1)
                .taxCategoryName("Đồ uống VAT 8%")
                .unit("Ly")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("POST /create - Thành công tạo MasterItem")
    void create_Success() throws Exception {
        MasterItemCreateRequest request = MasterItemCreateRequest.builder()
                .code("COFFEE_DEN")
                .name("Cà phê đen")
                .itemType(MenuType.PRODUCT)
                .defaultBasePrice(new BigDecimal("30000"))
                .taxCategoryId(1)
                .unit("Ly")
                .status("ACTIVE")
                .build();

        when(masterItemService.create(any(MasterItemCreateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/master-items/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.code").value("COFFEE_DEN"))
                .andExpect(jsonPath("$.result.name").value("Cà phê đen"));

        verify(masterItemService).create(any(MasterItemCreateRequest.class));
    }

    @Test
    @DisplayName("POST /create - Báo lỗi validate khi thiếu trường bắt buộc")
    void create_ValidationError() throws Exception {
        MasterItemCreateRequest invalidRequest = MasterItemCreateRequest.builder()
                .code("") // Blank code
                .name("") // Blank name
                .build();

        mockMvc.perform(post("/api/v1/master-items/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /update/{id} - Thành công cập nhật MasterItem")
    void update_Success() throws Exception {
        MasterItemUpdateRequest request = MasterItemUpdateRequest.builder()
                .name("Cà phê đen đá")
                .defaultBasePrice(new BigDecimal("35000"))
                .build();

        when(masterItemService.update(eq(1), any(MasterItemUpdateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/master-items/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.code").value("COFFEE_DEN"));

        verify(masterItemService).update(eq(1), any(MasterItemUpdateRequest.class));
    }

    @Test
    @DisplayName("GET /{id} - Thành công xem chi tiết MasterItem")
    void getById_Success() throws Exception {
        when(masterItemService.getById(1)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/master-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.code").value("COFFEE_DEN"));

        verify(masterItemService).getById(1);
    }

    @Test
    @DisplayName("POST /filter - Thành công lọc và phân trang")
    void filter_Success() throws Exception {
        MasterItemSearchDto searchDto = MasterItemSearchDto.builder()
                .name("Cà phê")
                .page(0)
                .pageSize(10)
                .build();

        PageResponse<MasterItemResponse> pageResponse = PageResponse.<MasterItemResponse>builder()
                .content(List.of(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(masterItemService.filter(any(MasterItemSearchDto.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/master-items/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.content[0].code").value("COFFEE_DEN"));

        verify(masterItemService).filter(any(MasterItemSearchDto.class));
    }

    @Test
    @DisplayName("DELETE /delete - Thành công xóa nhiều MasterItem")
    void deleteBatch_Success() throws Exception {
        List<Integer> ids = List.of(1, 2);

        mockMvc.perform(delete("/api/v1/master-items/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(masterItemService).deleteBatch(eq(ids));
    }

    @Test
    @DisplayName("GET /active - Thành công lấy danh sách MasterItem active")
    void getAllActive_Success() throws Exception {
        when(masterItemService.getAllActive()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/master-items/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].code").value("COFFEE_DEN"));

        verify(masterItemService).getAllActive();
    }
}
