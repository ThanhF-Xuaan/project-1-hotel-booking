package vn.edu.utc.hotel_booking.modules.operation.service.support;

import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;

import java.util.Objects;

/** Validates the effective association tuple before any managed guest is changed. */
public final class StayGuestAssociationValidator {
    private StayGuestAssociationValidator() { }
    public record Associations(Booking booking, BookingRoom bookingRoom, RoomInstance room, String roomNumber) { }

    public static Associations validate(Short hotelId, Booking booking, BookingRoom bookingRoom,
                                        RoomInstance room, String roomNumber) {
        if (hotelId == null || hotelId < 1) throw invalid("Khách sạn không hợp lệ");
        if (bookingRoom != null) {
            if (bookingRoom.getRoomInstance() == null) {
                throw new AppException(ErrorCode.ROOM_NOT_YET_ASSIGNED, "Phòng đặt chưa được gán phòng vật lý");
            }
            if (bookingRoom.getBookingDetail() == null || bookingRoom.getBookingDetail().getBooking() == null) {
                throw invalid("Liên kết phòng đặt không hợp lệ");
            }
            Booking parent = bookingRoom.getBookingDetail().getBooking();
            if (booking != null && !Objects.equals(booking.getId(), parent.getId())) {
                throw invalid("Phòng đặt không thuộc đơn đặt phòng được chỉ định");
            }
            if (room != null && !Objects.equals(room.getId(), bookingRoom.getRoomInstance().getId())) {
                throw invalid("Phòng vật lý không khớp phòng đặt");
            }
            // Validate both parent objects, even when an explicit object has the same ID.
            requireBookingHotel(parent, hotelId);
            requireRoomHotel(bookingRoom.getRoomInstance(), hotelId);
            booking = parent;
            room = bookingRoom.getRoomInstance();
        }
        if (booking != null) requireBookingHotel(booking, hotelId);
        String number = roomNumber == null ? null : roomNumber.trim();
        if (room != null) {
            requireRoomHotel(room, hotelId);
            if (room.getRoomNumber() == null || number == null || !room.getRoomNumber().equals(number)) {
                throw invalid("Số phòng không khớp phòng vật lý");
            }
        }
        return new Associations(booking, bookingRoom, room, number);
    }

    private static void requireBookingHotel(Booking booking, Short hotelId) {
        if (booking.getHotel() == null || !hotelId.equals(booking.getHotel().getId())) {
            throw invalid("Đơn đặt phòng không thuộc khách sạn này");
        }
    }
    private static void requireRoomHotel(RoomInstance room, Short hotelId) {
        if (Boolean.TRUE.equals(room.getIsDeleted()) || room.getHotel() == null || !hotelId.equals(room.getHotel().getId())) {
            throw invalid("Phòng vật lý không thuộc khách sạn này");
        }
    }
    private static AppException invalid(String message) { return new AppException(ErrorCode.INVALID_REQUEST_DATA, message); }
}
