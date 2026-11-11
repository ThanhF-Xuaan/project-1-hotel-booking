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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;

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
@DisplayName("MenuController — Unit Tests")
class MenuControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuController controller;

    private MenuResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandlerException())
                .build();

        sampleResponse = MenuResponse.builder()
                .id(10)
                .hotelId((short) 1)
                .hotelName("Grand Hotel")
                .masterItemId(5)
                .masterItemCode("BEER_HN")
                .name("Bia Hà Nội lon")
                .customName("Bia Hà Nội ướp lạnh")
                .menuType(MenuType.PRODUCT)
                .basePrice(new BigDecimal("25000"))
                .taxCategoryId(1)
                .taxCategoryName("VAT 8%")
                .stockQuantity(50)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("POST /create - Thành công tạo món ăn vào thực đơn cơ sở")
    void create_Success() throws Exception {
        MenuCreateRequest request = MenuCreateRequest.builder()
                .hotelId((short) 1)
                .masterItemId(5)
                .name("Bia Hà Nội lon")
                .customName("Bia Hà Nội ướp lạnh")
                .menuType(MenuType.PRODUCT)
                .basePrice(new BigDecimal("25000"))
                .taxCategoryId(1)
                .stockQuantity(50)
                .build();

        when(menuService.create(any(MenuCreateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/menus/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.name").value("Bia Hà Nội lon"))
                .andExpect(jsonPath("$.result.stockQuantity").value(50));

        verify(menuService).create(any(MenuCreateRequest.class));
    }

    @Test
    @DisplayName("POST /create - Báo lỗi validate khi thiếu trường bắt buộc")
    void create_ValidationError() throws Exception {
        MenuCreateRequest invalidRequest = MenuCreateRequest.builder()
                .name("") // Blank name
                .build();

        mockMvc.perform(post("/api/v1/menus/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /update/{id} - Thành công cập nhật món/dịch vụ")
    void update_Success() throws Exception {
        MenuUpdateRequest request = MenuUpdateRequest.builder()
                .customName("Bia Hà Nội siêu lạnh")
                .basePrice(new BigDecimal("30000"))
                .build();

        when(menuService.update(eq(10), any(MenuUpdateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/menus/update/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(10));

        verify(menuService).update(eq(10), any(MenuUpdateRequest.class));
    }

    @Test
    @DisplayName("GET /{id} - Thành công xem chi tiết món theo ID")
    void getById_Success() throws Exception {
        when(menuService.getById(10)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/menus/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(10));

        verify(menuService).getById(10);
    }

    @Test
    @DisplayName("POST /filter - Thành công lọc và phân trang thực đơn")
    void filter_Success() throws Exception {
        MenuSearchDto searchDto = MenuSearchDto.builder()
                .hotelId((short) 1)
                .name("Bia")
                .page(0)
                .pageSize(10)
                .build();

        PageResponse<MenuResponse> pageResponse = PageResponse.<MenuResponse>builder()
                .content(List.of(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(menuService.filter(any(MenuSearchDto.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/menus/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.content[0].id").value(10));

        verify(menuService).filter(any(MenuSearchDto.class));
    }

    @Test
    @DisplayName("DELETE /delete - Thành công xóa nhiều món thực đơn")
    void deleteBatch_Success() throws Exception {
        List<Integer> ids = List.of(10, 11);

        mockMvc.perform(delete("/api/v1/menus/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(menuService).deleteBatch(eq(ids));
    }
}
