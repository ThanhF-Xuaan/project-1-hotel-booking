package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;

import java.time.LocalDate;
import java.util.List;

public interface BookingRoomService {

    BookingRoom getEntityById(Long id);

    BookingRoom save(BookingRoom bookingRoom);

    List<BookingRoom> findOverlappingAssignedRooms(Integer roomInstanceId, LocalDate checkInDate, LocalDate checkOutDate);

    List<BookingRoom> findByBookingId(Long bookingId);

    List<BookingRoom> findByBookingDetailId(Long bookingDetailId);

    java.util.Optional<BookingRoom> findByBookingIdAndRoomInstanceId(Long bookingId, Integer roomInstanceId);

    List<BookingRoom> findByRoomInstanceIdAndStatusIn(Integer roomInstanceId, java.util.Collection<vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus> statuses);

    List<BookingRoom> findActiveByRoomInstanceIdInWithDetails(
            java.util.Collection<Integer> roomInstanceIds,
            java.util.Collection<vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus> statuses
    );
}
