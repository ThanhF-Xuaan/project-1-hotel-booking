package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VoucherUsage;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherUsageRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.VoucherServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    VoucherUsageRepository voucherUsageRepository;

    @InjectMocks
    VoucherServiceImpl voucherService;

    BookingGuest sampleGuest;

    @BeforeEach
    void setUp() {
        sampleGuest = BookingGuest.builder().id(10L).fullName("Nguyễn Văn A").build();
    }

    @Test
    @DisplayName("findByCode - Tìm thấy voucher hợp lệ")
    void findByCode_Success() {
        Voucher voucher = Voucher.builder().id(1).voucherCode("SUMMER2026").status("ACTIVE").build();
        when(voucherRepository.findByVoucherCodeAndIsDeletedFalse("SUMMER2026")).thenReturn(Optional.of(voucher));

        Optional<Voucher> result = voucherService.findByCode("SUMMER2026");

        assertThat(result).isPresent();
        assertThat(result.get().getVoucherCode()).isEqualTo("SUMMER2026");
    }

    @Test
    @DisplayName("findByCode - Trả về Empty khi mã voucher rỗng hoặc null")
    void findByCode_BlankCode_ReturnsEmpty() {
        assertThat(voucherService.findByCode(null)).isEmpty();
        assertThat(voucherService.findByCode("   ")).isEmpty();
        verifyNoInteractions(voucherRepository);
    }

    @Test
    @DisplayName("recordVoucherUsage - Ghi nhận sử dụng và chuyển EXPIRED khi đạt số lượng tối đa")
    void recordVoucherUsage_ReachedMaxQuantity_SetsExpired() {
        Voucher voucher = Voucher.builder()
                .id(1)
                .voucherCode("DISCOUNT50")
                .usedQuantity(9)
                .totalQuantity(10)
                .status("ACTIVE")
                .build();

        when(voucherRepository.findByVoucherCodeAndIsDeletedFalse("DISCOUNT50")).thenReturn(Optional.of(voucher));

        voucherService.recordVoucherUsage("DISCOUNT50", sampleGuest, new BigDecimal("50000"));

        assertThat(voucher.getUsedQuantity()).isEqualTo(10);
        assertThat(voucher.getStatus()).isEqualTo("EXPIRED");
        verify(voucherRepository).save(voucher);
        verify(voucherUsageRepository).save(any(VoucherUsage.class));
    }
}
