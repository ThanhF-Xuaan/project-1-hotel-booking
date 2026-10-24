package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;

import java.time.LocalDate;

public interface RoomSlotService {

    void createOrReserveSlots(RoomInstance roomInstance, LocalDate checkIn, LocalDate checkOut, Long bookingRoomId);

    void releaseSlots(Integer roomInstanceId, LocalDate checkIn, LocalDate checkOut);

    void occupySlots(Integer roomInstanceId, LocalDate checkIn, LocalDate checkOut);

    void markSlotsDirty(Integer roomInstanceId, LocalDate checkIn, LocalDate checkOut);

    void updateTodaySlotStatus(Integer roomInstanceId, String status);

    void swapSlots(Integer oldRoomId, RoomInstance newRoomInstance, LocalDate effectiveDate, LocalDate checkOutDate, Long bookingRoomId, boolean isInHouse);
}
