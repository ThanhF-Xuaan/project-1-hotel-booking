package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDailyRateRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingDailyRateServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingDailyRateServiceImplTest {

    @Mock
    BookingDailyRateRepository bookingDailyRateRepository;

    @InjectMocks
    BookingDailyRateServiceImpl bookingDailyRateService;

    @Test
    @DisplayName("findByBookingRoomId - Thành công khi ID hợp lệ")
    void findByBookingRoomId_Success() {
        BookingDailyRate rate = BookingDailyRate.builder().id(1L).netPrice(BigDecimal.valueOf(1000000)).build();
        when(bookingDailyRateRepository.findByBookingRoomId(10L)).thenReturn(List.of(rate));

        List<BookingDailyRate> result = bookingDailyRateService.findByBookingRoomId(10L);

        assertThat(result).hasSize(1);
        verify(bookingDailyRateRepository).findByBookingRoomId(10L);
    }

    @Test
    @DisplayName("findByBookingRoomId - Trả về danh sách rỗng khi ID là null")
    void findByBookingRoomId_NullId_ReturnsEmpty() {
        List<BookingDailyRate> result = bookingDailyRateService.findByBookingRoomId(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(bookingDailyRateRepository);
    }

    @Test
    @DisplayName("findActiveDailyRatesForHotelAndDate - Thành công khi tham số hợp lệ")
    void findActiveDailyRatesForHotelAndDate_Success() {
        BookingDailyRate rate = BookingDailyRate.builder().id(2L).netPrice(BigDecimal.valueOf(2000000)).build();
        LocalDate date = LocalDate.of(2026, 6, 1);
        when(bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate((short) 1, date))
                .thenReturn(List.of(rate));

        List<BookingDailyRate> result = bookingDailyRateService.findActiveDailyRatesForHotelAndDate((short) 1, date);

        assertThat(result).hasSize(1);
        verify(bookingDailyRateRepository).findActiveDailyRatesForHotelAndDate((short) 1, date);
    }

    @Test
    @DisplayName("findActiveDailyRatesForHotelAndDate - Trả về rỗng khi hotelId hoặc stayDate là null")
    void findActiveDailyRatesForHotelAndDate_NullArgs_ReturnsEmpty() {
        assertThat(bookingDailyRateService.findActiveDailyRatesForHotelAndDate(null, LocalDate.now())).isEmpty();
        assertThat(bookingDailyRateService.findActiveDailyRatesForHotelAndDate((short) 1, null)).isEmpty();
        verifyNoInteractions(bookingDailyRateRepository);
    }

    @Test
    @DisplayName("calculateDailyRoomRevenue - Tính tổng doanh thu thành công")
    void calculateDailyRoomRevenue_Success() {
        BookingDailyRate rate1 = BookingDailyRate.builder().id(1L).netPrice(BigDecimal.valueOf(1500000)).build();
        BookingDailyRate rate2 = BookingDailyRate.builder().id(2L).netPrice(BigDecimal.valueOf(2500000)).build();
        LocalDate date = LocalDate.of(2026, 6, 1);
        when(bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate((short) 1, date))
                .thenReturn(List.of(rate1, rate2));

        BigDecimal total = bookingDailyRateService.calculateDailyRoomRevenue((short) 1, date);

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(4000000));
        verify(bookingDailyRateRepository).findActiveDailyRatesForHotelAndDate((short) 1, date);
    }

    @Test
    @DisplayName("calculateDailyRoomRevenue - Trả về 0 khi tham số là null")
    void calculateDailyRoomRevenue_NullArgs_ReturnsZero() {
        assertThat(bookingDailyRateService.calculateDailyRoomRevenue(null, LocalDate.now())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bookingDailyRateService.calculateDailyRoomRevenue((short) 1, null)).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(bookingDailyRateRepository);
    }
}
