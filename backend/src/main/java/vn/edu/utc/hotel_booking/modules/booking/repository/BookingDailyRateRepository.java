package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;

import java.util.List;

@Repository
public interface BookingDailyRateRepository extends JpaRepository<BookingDailyRate, Long> {

    List<BookingDailyRate> findByBookingRoomId(Long bookingRoomId);

    @org.springframework.data.jpa.repository.Query("SELECT bdr FROM BookingDailyRate bdr " +
            "JOIN bdr.bookingRoom br " +
            "JOIN br.bookingDetail bd " +
            "JOIN bd.booking b " +
            "WHERE b.hotel.id = :hotelId " +
            "AND bdr.stayDate = :stayDate " +
            "AND br.status NOT IN ('CANCELLED') " +
            "AND b.status NOT IN ('CANCELLED')")
    List<BookingDailyRate> findActiveDailyRatesForHotelAndDate(
            @org.springframework.data.repository.query.Param("hotelId") Short hotelId,
            @org.springframework.data.repository.query.Param("stayDate") java.time.LocalDate stayDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT bdr FROM BookingDailyRate bdr " +
            "JOIN FETCH bdr.bookingRoom br " +
            "JOIN FETCH br.bookingDetail bd " +
            "JOIN FETCH bd.booking b " +
            "LEFT JOIN FETCH b.groupContract " +
            "LEFT JOIN FETCH b.company " +
            "WHERE b.hotel.id IN :hotelIds " +
            "AND bdr.stayDate BETWEEN :startDate AND :endDate " +
            "AND br.status NOT IN ('CANCELLED') " +
            "AND b.status NOT IN ('CANCELLED')")
    List<BookingDailyRate> findActiveDailyRatesForHotelsAndDateRange(
            @org.springframework.data.repository.query.Param("hotelIds") List<Short> hotelIds,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDate endDate
    );
}
