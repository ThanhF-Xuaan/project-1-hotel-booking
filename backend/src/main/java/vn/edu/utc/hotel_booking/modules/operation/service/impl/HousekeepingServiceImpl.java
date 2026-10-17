package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomOccupancyDto;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.RoomStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.RoomHousekeepingStatusResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.HousekeepingService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class HousekeepingServiceImpl implements HousekeepingService {

    RoomInstanceService roomInstanceService;
    RoomSlotService roomSlotService;
    BookingLookupService bookingLookupService;

    private static final Set<String> VALID_STATUSES = Set.of("READY", "OCCUPIED", "CLEANING", "DIRTY", "MAINTENANCE");

    @Override
    public List<RoomHousekeepingStatusResponse> getRoomsByHotel(Short hotelId, String status) {
        List<RoomInstance> rooms = roomInstanceService.getRoomsByHotelAndStatus(hotelId, status);

        // Lấy danh sách ID phòng OCCUPIED để batch lookup thông tin lưu trú chống N+1
        List<Integer> occupiedRoomIds = rooms.stream()
                .filter(r -> "OCCUPIED".equalsIgnoreCase(r.getCurrentStatus()))
                .map(RoomInstance::getId)
                .toList();

        Map<Integer, RoomOccupancyDto> occupancyMap = bookingLookupService.getActiveOccupanciesByRoomInstanceIds(occupiedRoomIds);

        return rooms.stream()
                .map(room -> mapToHousekeepingResponse(room, occupancyMap.get(room.getId())))
                .toList();
    }

    @Override
    @Transactional
    public RoomHousekeepingStatusResponse updateRoomStatus(Integer roomInstanceId, RoomStatusUpdateRequest request) {
        String newStatus = request.getStatus().trim().toUpperCase();
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Trạng thái không hợp lệ. Các trạng thái hợp lệ: " + VALID_STATUSES);
        }

        RoomInstance room = roomInstanceService.updateRoomStatusEntity(roomInstanceId, newStatus);

        // Đồng bộ trạng thái vào RoomSlot hôm nay qua RoomSlotService
        roomSlotService.updateTodaySlotStatus(roomInstanceId, newStatus);

        RoomOccupancyDto occupancy = null;
        if ("OCCUPIED".equalsIgnoreCase(newStatus)) {
            occupancy = bookingLookupService.getActiveOccupancyByRoomInstanceId(roomInstanceId);
        }

        return mapToHousekeepingResponse(room, occupancy);
    }

    private RoomHousekeepingStatusResponse mapToHousekeepingResponse(RoomInstance room, RoomOccupancyDto occupancy) {
        RoomHousekeepingStatusResponse.RoomHousekeepingStatusResponseBuilder builder = RoomHousekeepingStatusResponse.builder()
                .roomInstanceId(room.getId())
                .roomNumber(room.getRoomNumber())
                .hotelId(room.getHotel().getId())
                .hotelName(room.getHotel().getName())
                .currentStatus(room.getCurrentStatus());

        if (room.getHotelRoomType() != null) {
            builder.hotelRoomTypeId(room.getHotelRoomType().getId());
            if (room.getHotelRoomType().getRoomType() != null) {
                builder.roomTypeName(room.getHotelRoomType().getRoomType().getName());
            }
        }

        if (occupancy != null) {
            builder.bookingNumber(occupancy.getBookingNumber());
            builder.checkOutDate(occupancy.getCheckOutDate());
            builder.guestName(occupancy.getGuestName());
        }

        return builder.build();
    }
}
