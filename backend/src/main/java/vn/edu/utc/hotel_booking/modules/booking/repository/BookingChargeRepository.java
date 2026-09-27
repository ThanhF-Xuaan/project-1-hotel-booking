package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;

import java.util.List;

@Repository
public interface BookingChargeRepository extends JpaRepository<BookingCharge, Long> {

    List<BookingCharge> findByBookingRoomId(Long bookingRoomId);
}
