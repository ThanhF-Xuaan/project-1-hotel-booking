package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDailyRateRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingDailyRateService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class BookingDailyRateServiceImpl implements BookingDailyRateService {

    BookingDailyRateRepository bookingDailyRateRepository;

    @Override
    public List<BookingDailyRate> findByBookingRoomId(Long bookingRoomId) {
        if (bookingRoomId == null) {
            return List.of();
        }
        return bookingDailyRateRepository.findByBookingRoomId(bookingRoomId);
    }

    @Override
    public List<BookingDailyRate> findActiveDailyRatesForHotelAndDate(Short hotelId, LocalDate stayDate) {
        if (hotelId == null || stayDate == null) {
            return List.of();
        }
        return bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(hotelId, stayDate);
    }

    @Override
    public BigDecimal calculateDailyRoomRevenue(Short hotelId, LocalDate date) {
        if (hotelId == null || date == null) {
            return BigDecimal.ZERO;
        }
        List<BookingDailyRate> dailyRates = findActiveDailyRatesForHotelAndDate(hotelId, date);
        return dailyRates.stream()
                .map(BookingDailyRate::getNetPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
