package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.RoomStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.RoomHousekeepingStatusResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.HousekeepingService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class HousekeepingServiceImpl implements HousekeepingService {

    RoomInstanceRepository roomInstanceRepository;
    RoomSlotRepository roomSlotRepository;
    BookingRoomRepository bookingRoomRepository;

    private static final Set<String> VALID_STATUSES = Set.of("READY", "OCCUPIED", "CLEANING", "DIRTY", "MAINTENANCE");

    @Override
    public List<RoomHousekeepingStatusResponse> getRoomsByHotel(Short hotelId, String status) {
        List<RoomInstance> rooms;
        if (status != null && !status.isBlank()) {
            rooms = roomInstanceRepository.findAll().stream()
                    .filter(r -> !r.getIsDeleted() && r.getHotel().getId().equals(hotelId) && r.getCurrentStatus().equalsIgnoreCase(status.trim()))
                    .toList();
        } else {
            rooms = roomInstanceRepository.findAll().stream()
                    .filter(r -> !r.getIsDeleted() && r.getHotel().getId().equals(hotelId))
                    .toList();
        }

        return rooms.stream().map(this::mapToHousekeepingResponse).toList();
    }

    @Override
    @Transactional
    public RoomHousekeepingStatusResponse updateRoomStatus(Integer roomInstanceId, RoomStatusUpdateRequest request) {
        RoomInstance room = roomInstanceRepository.findByIdWithDetails(roomInstanceId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng: " + roomInstanceId));

        String newStatus = request.getStatus().trim().toUpperCase();
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Trạng thái không hợp lệ. Các trạng thái hợp lệ: " + VALID_STATUSES);
        }

        room.setCurrentStatus(newStatus);
        roomInstanceRepository.save(room);

        // Đồng bộ trạng thái vào RoomSlot hôm nay nếu tồn tại
        LocalDate today = LocalDate.now();
        roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstanceId, today).ifPresent(slot -> {
            slot.setStatus(newStatus);
            roomSlotRepository.save(slot);
        });

        return mapToHousekeepingResponse(room);
    }

    private RoomHousekeepingStatusResponse mapToHousekeepingResponse(RoomInstance room) {
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

        // Tìm thông tin khách đang ở nếu trạng thái là OCCUPIED
        List<BookingRoom> activeRooms = bookingRoomRepository.findByRoomInstanceIdAndStatusIn(
                room.getId(),
                List.of(BookingRoomStatus.CHECKED_IN)
        );

        if (!activeRooms.isEmpty()) {
            BookingRoom activeBr = activeRooms.get(0);
            if (activeBr.getBookingDetail() != null && activeBr.getBookingDetail().getBooking() != null) {
                var booking = activeBr.getBookingDetail().getBooking();
                builder.bookingNumber(booking.getBookingNumber());
                builder.checkOutDate(activeBr.getBookingDetail().getCheckOutDate());
                if (activeBr.getBookingGuests() != null && !activeBr.getBookingGuests().isEmpty()) {
                    builder.guestName(activeBr.getBookingGuests().get(0).getFullName());
                } else if (booking.getGuest() != null) {
                    builder.guestName(booking.getGuest().getPhone());
                }
            }
        }

        return builder.build();
    }
}
