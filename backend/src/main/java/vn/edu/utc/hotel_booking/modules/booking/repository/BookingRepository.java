package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    boolean existsByBookingNumber(String bookingNumber);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH bd.bookingRooms br " +
            "WHERE b.hotel.id = :hotelId AND b.status = :status")
    List<Booking> findByHotelIdAndStatusWithDetails(
            @Param("hotelId") Short hotelId,
            @Param("status") BookingStatus status
    );

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.hotel WHERE b.bookingGuest.id = :guestId ORDER BY b.createdAt DESC")
    List<Booking> findByGuestIdWithHotel(@Param("guestId") Long guestId);
}
