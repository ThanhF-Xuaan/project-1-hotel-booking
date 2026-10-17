package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingChargeRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingChargeServiceImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingChargeServiceImplTest {

    @Mock
    BookingChargeRepository bookingChargeRepository;

    @InjectMocks
    BookingChargeServiceImpl bookingChargeService;

    @Test
    @DisplayName("save - Lưu phụ phí thành công")
    void save_Success() {
        BookingCharge charge = BookingCharge.builder().id(1L).build();
        when(bookingChargeRepository.save(charge)).thenReturn(charge);

        BookingCharge result = bookingChargeService.save(charge);

        assertThat(result).isNotNull();
        verify(bookingChargeRepository).save(charge);
    }

    @Test
    @DisplayName("saveAll - Lưu danh sách phụ phí thành công")
    void saveAll_Success() {
        BookingCharge charge = BookingCharge.builder().id(1L).build();
        when(bookingChargeRepository.saveAll(List.of(charge))).thenReturn(List.of(charge));

        List<BookingCharge> result = bookingChargeService.saveAll(List.of(charge));

        assertThat(result).hasSize(1);
        verify(bookingChargeRepository).saveAll(List.of(charge));
    }

    @Test
    @DisplayName("findByBookingRoomId - Tìm danh sách phụ phí theo phòng đặt")
    void findByBookingRoomId_Success() {
        BookingCharge charge = BookingCharge.builder().id(1L).build();
        when(bookingChargeRepository.findByBookingRoomId(20L)).thenReturn(List.of(charge));

        List<BookingCharge> result = bookingChargeService.findByBookingRoomId(20L);

        assertThat(result).hasSize(1);
        verify(bookingChargeRepository).findByBookingRoomId(20L);
    }

    @Test
    @DisplayName("createCharge - Tính toán subtotal, VAT, service fee và lưu thành công")
    void createCharge_Success() {
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).build();
        BookingChargeCreateRequest request = BookingChargeCreateRequest.builder()
                .chargeType(BookingChargeType.EXTRA_BED)
                .itemName("Giường phụ")
                .unitPrice(new BigDecimal("200000"))
                .quantity(1)
                .serviceFeeRate(new BigDecimal("5.00"))
                .vatRate(new BigDecimal("10.00"))
                .build();

        when(bookingChargeRepository.save(any(BookingCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingCharge result = bookingChargeService.createCharge(bookingRoom, request);

        assertThat(result).isNotNull();
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(result.getServiceFeeAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(result.getVatAmount()).isEqualByComparingTo(new BigDecimal("21000.00")); // (200000 + 10000) * 10%
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("231000.00")); // 210000 + 21000
        verify(bookingChargeRepository).save(any(BookingCharge.class));
    }
}
