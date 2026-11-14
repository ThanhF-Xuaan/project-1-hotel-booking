package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherValidateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherUsageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherValidationResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VoucherUsage;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.VoucherMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherUsageRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.VoucherServiceImpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceComprehensiveTest {

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    VoucherUsageRepository voucherUsageRepository;

    @Mock
    HotelService hotelService;

    VoucherMapper voucherMapper = Mappers.getMapper(VoucherMapper.class);

    VoucherServiceImpl voucherService;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherServiceImpl(
                voucherRepository,
                voucherUsageRepository,
                hotelService,
                voucherMapper
        );
    }

    @Test
    @DisplayName("Tạo mới voucher thành công với phạm vi toàn chuỗi (hotelId = null)")
    void createVoucher_Success_ChainWide() {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherCreateRequest req = VoucherCreateRequest.builder()
                .voucherCode("SUMMER2026")
                .title("Ưu đãi hè")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(15))
                .applyScope("ROOM_ONLY")
                .minOrderAmount(BigDecimal.valueOf(500000))
                .maxDiscountAmount(BigDecimal.valueOf(200000))
                .totalQuantity(100)
                .maxUsagePerUser((short) 1)
                .validFrom(now)
                .validTo(now.plusDays(30))
                .build();

        when(voucherRepository.existsByVoucherCodeAndIsDeletedFalse("SUMMER2026")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> {
            Voucher v = inv.getArgument(0);
            v.setId(1);
            return v;
        });

        VoucherResponse response = voucherService.createVoucher(req);

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getVoucherCode()).isEqualTo("SUMMER2026");
        assertThat(response.getHotelName()).isEqualTo("Toàn chuỗi áp dụng");
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo voucher có ngày kết thúc trước ngày bắt đầu")
    void createVoucher_ThrowsException_WhenDatesInvalid() {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherCreateRequest req = VoucherCreateRequest.builder()
                .voucherCode("SUMMER2026")
                .title("Ưu đãi hè")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(15))
                .validFrom(now.plusDays(10))
                .validTo(now)
                .build();

        assertThatThrownBy(() -> voucherService.createVoucher(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VOUCHER_DATES);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo voucher trùng mã đã tồn tại")
    void createVoucher_ThrowsException_WhenCodeAlreadyExists() {
        OffsetDateTime now = OffsetDateTime.now();
        VoucherCreateRequest req = VoucherCreateRequest.builder()
                .voucherCode("EXISTING")
                .title("Ưu đãi")
                .discountType("FIXED")
                .discountValue(BigDecimal.valueOf(50000))
                .validFrom(now)
                .validTo(now.plusDays(10))
                .build();

        when(voucherRepository.existsByVoucherCodeAndIsDeletedFalse("EXISTING")).thenReturn(true);

        assertThatThrownBy(() -> voucherService.createVoucher(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Cập nhật voucher thành công")
    void updateVoucher_Success() {
        OffsetDateTime now = OffsetDateTime.now();
        Voucher existing = Voucher.builder()
                .id(1)
                .voucherCode("SUMMER")
                .title("Cũ")
                .discountType("FIXED")
                .discountValue(BigDecimal.valueOf(50000))
                .validFrom(now)
                .validTo(now.plusDays(10))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(existing));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));

        VoucherUpdateRequest updateReq = VoucherUpdateRequest.builder()
                .title("Mới")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .applyScope("ROOM_ONLY")
                .totalQuantity(200)
                .maxUsagePerUser((short) 2)
                .validFrom(now)
                .validTo(now.plusDays(20))
                .status("ACTIVE")
                .build();

        VoucherResponse res = voucherService.updateVoucher(1, updateReq);

        assertThat(res.getTitle()).isEqualTo("Mới");
        assertThat(res.getDiscountType()).isEqualTo("PERCENT");
        assertThat(res.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    @DisplayName("Lọc danh sách voucher phân trang thành công")
    void filterVouchers_Success() {
        Voucher v = Voucher.builder()
                .id(10)
                .voucherCode("TET2027")
                .title("Tết Đinh Mùi")
                .discountType("FIXED")
                .discountValue(BigDecimal.valueOf(100000))
                .totalQuantity(50)
                .usedQuantity(10)
                .build();

        Page<Voucher> page = new PageImpl<>(List.of(v));
        when(voucherRepository.filterVouchers(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        VoucherSearchDto search = VoucherSearchDto.builder().page(0).pageSize(10).build();
        PageResponse<VoucherResponse> response = voucherService.filterVouchers(search);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getVoucherCode()).isEqualTo("TET2027");
        assertThat(response.getContent().get(0).getRemainingQuantity()).isEqualTo(40);
        assertThat(response.getContent().get(0).getHotelName()).isEqualTo("Toàn chuỗi áp dụng");
    }

    @Test
    @DisplayName("filterVouchers: Batch load tên cơ sở thành công khi voucher gắn hotelId")
    void filterVouchers_WithHotelId_BatchLoadsHotelNames() {
        Voucher v1 = Voucher.builder()
                .id(1)
                .hotelId((short) 10)
                .voucherCode("HANOI20")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .build();
        Voucher v2 = Voucher.builder()
                .id(2)
                .hotelId(null)
                .voucherCode("ALLCHAIN")
                .discountType("FIXED")
                .discountValue(BigDecimal.valueOf(50000))
                .build();

        Page<Voucher> page = new PageImpl<>(List.of(v1, v2));
        when(voucherRepository.filterVouchers(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(hotelService.findHotelNamesByIds(any())).thenReturn(java.util.Map.of((short) 10, "Viettel Luxury Hà Nội"));

        VoucherSearchDto search = VoucherSearchDto.builder().page(0).pageSize(10).build();
        PageResponse<VoucherResponse> response = voucherService.filterVouchers(search);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getHotelName()).isEqualTo("Viettel Luxury Hà Nội");
        assertThat(response.getContent().get(1).getHotelName()).isEqualTo("Toàn chuỗi áp dụng");
        verify(hotelService).findHotelNamesByIds(any());
    }

    @Test
    @DisplayName("filterVouchers: Tự phục hồi an toàn khi hotelId không tồn tại hoặc bị xóa trên production (Không rollback)")
    void filterVouchers_WithNonExistentHotelId_ResilientFallbackWithoutError() {
        Voucher v = Voucher.builder()
                .id(99)
                .hotelId((short) 999)
                .voucherCode("DEADHOTEL")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(10))
                .build();

        Page<Voucher> page = new PageImpl<>(List.of(v));
        when(voucherRepository.filterVouchers(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        // Hotel 999 không tồn tại trong map
        when(hotelService.findHotelNamesByIds(any())).thenReturn(java.util.Collections.emptyMap());

        VoucherSearchDto search = VoucherSearchDto.builder().page(0).pageSize(10).build();
        PageResponse<VoucherResponse> response = voucherService.filterVouchers(search);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getHotelName()).isEqualTo("Cơ sở #999");
    }

    @Test
    @DisplayName("getVoucherById: Tự phục hồi hiển thị nhãn fallback khi khách sạn liên kết bị xóa")
    void getVoucherById_HotelDeleted_FallbackSuccess() {
        Voucher v = Voucher.builder()
                .id(5)
                .hotelId((short) 88)
                .voucherCode("VIP88")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(10))
                .build();

        when(voucherRepository.findByIdAndIsDeletedFalse(5)).thenReturn(Optional.of(v));
        when(hotelService.findHotelNameById((short) 88)).thenReturn(Optional.empty());

        VoucherResponse res = voucherService.getVoucherById(5);

        assertThat(res.getHotelName()).isEqualTo("Cơ sở #88");
    }

    @Test
    @DisplayName("Xóa hàng loạt voucher (Soft delete)")
    void deleteVouchers_Success() {
        voucherService.deleteVouchers(List.of(1, 2, 3));
        verify(voucherRepository).softDeleteByIds(List.of(1, 2, 3));

        // Edge case: null hoặc rỗng
        voucherService.deleteVouchers(null);
        voucherService.deleteVouchers(Collections.emptyList());
        verifyNoMoreInteractions(voucherRepository);
    }

    @Test
    @DisplayName("Validate voucher thành công tính đúng phần trăm và trần tối đa")
    void validateVoucher_Success_PercentWithMaxCap() {
        OffsetDateTime now = OffsetDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(5)
                .voucherCode("VIP20")
                .title("Ưu đãi VIP")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .maxDiscountAmount(BigDecimal.valueOf(150000))
                .minOrderAmount(BigDecimal.valueOf(500000))
                .applyScope("ROOM_ONLY")
                .totalQuantity(100)
                .usedQuantity(10)
                .maxUsagePerUser((short) 1)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(10))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findValidVoucher(eq("VIP20"), any(), any())).thenReturn(Optional.of(voucher));

        // Đơn 1.000.000 -> 20% = 200.000 -> Bị chặn trần maxDiscount 150.000
        VoucherValidateRequest req = VoucherValidateRequest.builder()
                .voucherCode("VIP20")
                .orderAmount(BigDecimal.valueOf(1000000))
                .applyScope("ROOM_ONLY")
                .build();

        VoucherValidationResponse res = voucherService.validateVoucher(req);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(150000));
        assertThat(res.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(850000));
    }

    @Test
    @DisplayName("Validate voucher thất bại khi đơn hàng chưa đạt mức tối thiểu")
    void validateVoucher_Fails_WhenMinOrderNotMet() {
        OffsetDateTime now = OffsetDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(5)
                .voucherCode("BIGDEAL")
                .title("Giảm lớn")
                .discountType("FIXED")
                .discountValue(BigDecimal.valueOf(200000))
                .minOrderAmount(BigDecimal.valueOf(2000000))
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(10))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findValidVoucher(eq("BIGDEAL"), any(), any())).thenReturn(Optional.of(voucher));

        VoucherValidateRequest req = VoucherValidateRequest.builder()
                .voucherCode("BIGDEAL")
                .orderAmount(BigDecimal.valueOf(1500000))
                .build();

        VoucherValidationResponse res = voucherService.validateVoucher(req);

        assertThat(res.getValid()).isFalse();
        assertThat(res.getMessage()).contains("chưa đạt giá trị tối thiểu");
    }

    @Test
    @DisplayName("Validate voucher thất bại khi khách đã dùng hết số lượt cho phép")
    void validateVoucher_Fails_WhenGuestUsageExceeded() {
        OffsetDateTime now = OffsetDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(5)
                .voucherCode("ONCE")
                .title("Chỉ dùng 1 lần")
                .discountType("FIXED")
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderAmount(BigDecimal.ZERO)
                .maxUsagePerUser((short) 1)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(10))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findValidVoucher(eq("ONCE"), any(), any())).thenReturn(Optional.of(voucher));
        when(voucherUsageRepository.countByVoucherIdAndBookingGuestId(5, 99L)).thenReturn(1L);

        VoucherValidateRequest req = VoucherValidateRequest.builder()
                .voucherCode("ONCE")
                .orderAmount(BigDecimal.valueOf(500000))
                .bookingGuestId(99L)
                .build();

        VoucherValidationResponse res = voucherService.validateVoucher(req);

        assertThat(res.getValid()).isFalse();
        assertThat(res.getMessage()).contains("đã sử dụng hết số lượt cho phép");
    }

    @Test
    @DisplayName("Giảm giá không vượt quá số tiền đơn hàng (Edge case discount > orderAmount)")
    void validateVoucher_DiscountCappedAtOrderAmount() {
        OffsetDateTime now = OffsetDateTime.now();
        Voucher voucher = Voucher.builder()
                .id(7)
                .voucherCode("HUGEFIXED")
                .discountType("FIXED")
                .discountValue(BigDecimal.valueOf(1000000))
                .minOrderAmount(BigDecimal.ZERO)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(10))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findValidVoucher(eq("HUGEFIXED"), any(), any())).thenReturn(Optional.of(voucher));

        VoucherValidateRequest req = VoucherValidateRequest.builder()
                .voucherCode("HUGEFIXED")
                .orderAmount(BigDecimal.valueOf(300000))
                .build();

        VoucherValidationResponse res = voucherService.validateVoucher(req);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(res.getFinalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Lấy danh sách VoucherUsage theo voucherId thành công")
    void getVoucherUsages_Success() {
        Voucher voucher = Voucher.builder()
                .id(1)
                .voucherCode("SUMMER2026")
                .build();
        when(voucherRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(voucher));

        BookingGuest guest = BookingGuest.builder()
                .id(99L)
                .fullName("Tran Van B")
                .phone("0988776655")
                .build();

        VoucherUsage usage = VoucherUsage.builder()
                .id(1L)
                .voucher(voucher)
                .bookingGuest(guest)
                .discountAmount(BigDecimal.valueOf(50000))
                .usedAt(OffsetDateTime.now())
                .build();

        when(voucherUsageRepository.findByVoucherId(1)).thenReturn(List.of(usage));

        List<VoucherUsageResponse> result = voucherService.getVoucherUsages(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVoucherCode()).isEqualTo("SUMMER2026");
        assertThat(result.get(0).getGuestFullName()).isEqualTo("Tran Van B");
        assertThat(result.get(0).getGuestPhoneNumber()).isEqualTo("0988776655");
    }

    @Test
    @DisplayName("Lấy danh sách VoucherUsage khi voucher không tồn tại ném VOUCHER_NOT_FOUND")
    void getVoucherUsages_NotFound() {
        when(voucherRepository.findByIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.getVoucherUsages(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VOUCHER_NOT_FOUND);
    }
}

