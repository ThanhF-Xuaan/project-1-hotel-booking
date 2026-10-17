package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.stereotype.Repository;

/**
 * @deprecated Bảng {@code guests} đã được đổi tên thành {@code booking_guests} (Task T05).
 * Sử dụng {@link BookingGuestRepository} thay thế.
 */
@Deprecated(forRemoval = true, since = "1.0.0")
@Repository
public interface GuestRepository extends BookingGuestRepository {
}
