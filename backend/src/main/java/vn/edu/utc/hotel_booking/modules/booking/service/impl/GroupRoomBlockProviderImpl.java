package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;
import vn.edu.utc.hotel_booking.modules.inventory.service.BlockedInventoryProvider;

import java.time.LocalDate;

/**
 * Cầu nối triển khai (Bridge/Adapter) giữa Booking module và Inventory module theo nguyên lý Inversion of Control.
 * Cung cấp số lượng phòng đang bị block bởi các đoàn DEFINITE cho RoomAvailabilityService.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupRoomBlockProviderImpl implements BlockedInventoryProvider {

    GroupRoomBlockService groupRoomBlockService;

    @Override
    public int getActiveBlockedRooms(Integer hotelRoomTypeId, LocalDate stayDate) {
        return groupRoomBlockService.getActiveBlockedRooms(hotelRoomTypeId, stayDate);
    }
}
