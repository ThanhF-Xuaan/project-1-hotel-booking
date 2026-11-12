package vn.edu.utc.hotel_booking.modules.pricing.controller;

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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherUsageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.VoucherService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VoucherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private VoucherController voucherController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(voucherController).build();
    }

    @Test
    @DisplayName("API POST /api/v1/pricing/vouchers - Tạo mới voucher thành công trả về 201")
    void createVoucher_Success() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .voucherCode("SUMMER2026")
                .title("Ưu đãi hè")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(15))
                .applyScope("ROOM_ONLY")
                .totalQuantity(100)
                .maxUsagePerUser((short) 1)
                .validFrom(now)
                .validTo(now.plusDays(30))
                .build();

        VoucherResponse response = VoucherResponse.builder()
                .id(1)
                .voucherCode("SUMMER2026")
                .title("Ưu đãi hè")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(15))
                .build();

        when(voucherService.createVoucher(any(VoucherCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/pricing/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.voucherCode").value("SUMMER2026"));
    }

    @Test
    @DisplayName("API PUT /api/v1/pricing/vouchers/{id} - Cập nhật voucher thành công trả về 200")
    void updateVoucher_Success() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherUpdateRequest request = VoucherUpdateRequest.builder()
                .title("Cập nhật tiêu đề")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .applyScope("ROOM_ONLY")
                .totalQuantity(150)
                .maxUsagePerUser((short) 2)
                .validFrom(now)
                .validTo(now.plusDays(20))
                .status("ACTIVE")
                .build();

        VoucherResponse response = VoucherResponse.builder()
                .id(1)
                .voucherCode("SUMMER2026")
                .title("Cập nhật tiêu đề")
                .build();

        when(voucherService.updateVoucher(eq(1), any(VoucherUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/pricing/vouchers/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("Cập nhật tiêu đề"));
    }

    @Test
    @DisplayName("API GET /api/v1/pricing/vouchers/{id} - Lấy chi tiết voucher theo ID thành công")
    void getVoucherById_Success() throws Exception {
        VoucherResponse response = VoucherResponse.builder()
                .id(10)
                .voucherCode("DISCOUNT50")
                .title("Giảm 50K")
                .build();

        when(voucherService.getVoucherById(10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/pricing/vouchers/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10))
                .andExpect(jsonPath("$.result.voucherCode").value("DISCOUNT50"));
    }

    @Test
    @DisplayName("API GET /api/v1/pricing/vouchers/{id}/usages - Lấy lịch sử sử dụng voucher thành công")
    void getVoucherUsages_Success() throws Exception {
        VoucherUsageResponse usage = VoucherUsageResponse.builder()
                .id(100L)
                .voucherId(10)
                .voucherCode("DISCOUNT50")
                .bookingGuestId(5L)
                .guestFullName("Nguyen Van A")
                .guestPhoneNumber("0912345678")
                .discountAmount(BigDecimal.valueOf(50000))
                .usedAt(OffsetDateTime.now())
                .build();

        when(voucherService.getVoucherUsages(10)).thenReturn(List.of(usage));

        mockMvc.perform(get("/api/v1/pricing/vouchers/{id}/usages", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(100))
                .andExpect(jsonPath("$.result[0].voucherCode").value("DISCOUNT50"))
                .andExpect(jsonPath("$.result[0].guestFullName").value("Nguyen Van A"));
    }

    @Test
    @DisplayName("API POST /api/v1/pricing/vouchers/filter - Lọc danh sách vouchers thành công")
    void filterVouchers_Success() throws Exception {
        VoucherSearchDto searchDto = VoucherSearchDto.builder()
                .keyword("VIP")
                .page(0)
                .pageSize(10)
                .build();

        PageResponse<VoucherResponse> pageResponse = PageResponse.<VoucherResponse>builder()
                .content(Collections.emptyList())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(voucherService.filterVouchers(any(VoucherSearchDto.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/pricing/vouchers/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(0));
    }

    @Test
    @DisplayName("API DELETE /api/v1/pricing/vouchers/delete - Xóa hàng loạt voucher thành công")
    void deleteVouchers_Success() throws Exception {
        List<Integer> ids = List.of(1, 2, 3);

        mockMvc.perform(delete("/api/v1/pricing/vouchers/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk());

        verify(voucherService).deleteVouchers(ids);
    }
}
