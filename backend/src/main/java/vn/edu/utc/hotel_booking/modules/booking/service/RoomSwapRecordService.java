package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.RoomSwapRecord;

import java.time.LocalDate;
import java.util.List;

public interface RoomSwapRecordService {

    RoomSwapRecord save(RoomSwapRecord record);

    List<RoomSwapRecord> findByBookingRoomId(Long bookingRoomId);

    List<RoomSwapRecord> findByBookingId(Long bookingId);

    List<RoomSwapRecord> findBySwapDateBetween(LocalDate startDate, LocalDate endDate);
}
