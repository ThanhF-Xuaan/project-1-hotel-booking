package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomAvailabilityMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {

    RoomAvailabilityRepository roomAvailabilityRepository;
    HotelRoomTypeRepository hotelRoomTypeRepository;
    RoomAvailabilityMapper roomAvailabilityMapper;

    @Override
    public List<RoomAvailabilityResponse> getAvailability(RoomAvailabilitySearchDto searchDto) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(searchDto.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                        "Không tìm thấy cấu hình loại phòng id: " + searchDto.getHotelRoomTypeId()));

        List<RoomAvailability> existingList = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateBetweenOrderByDateAsc(
                searchDto.getHotelRoomTypeId(),
                searchDto.getStartDate(),
                searchDto.getEndDate()
        );

        Map<LocalDate, RoomAvailability> map = existingList.stream()
                .collect(Collectors.toMap(RoomAvailability::getDate, Function.identity()));

        List<RoomAvailabilityResponse> results = new ArrayList<>();
        LocalDate cur = searchDto.getStartDate();
        while (!cur.isAfter(searchDto.getEndDate())) {
            RoomAvailability row = map.get(cur);
            if (row != null) {
                // If availableCount is null (e.g. before DB persist trigger), compute it
                int avail = (row.getAvailableCount() != null)
                        ? row.getAvailableCount()
                        : (row.getTotalRooms() - row.getBookedRooms() - row.getLockedRooms() - row.getOooRooms());
                RoomAvailabilityResponse res = roomAvailabilityMapper.toResponse(row);
                res.setAvailableCount(Math.max(0, avail));
                results.add(res);
            } else {
                // Sparse model: default available = totalQuantity
                results.add(RoomAvailabilityResponse.builder()
                        .hotelRoomTypeId(hotelRoomType.getId())
                        .date(cur)
                        .totalRooms(hotelRoomType.getTotalQuantity())
                        .bookedRooms(0)
                        .lockedRooms(0)
                        .oooRooms(0)
                        .availableCount(hotelRoomType.getTotalQuantity())
                        .version(0L)
                        .build());
            }
            cur = cur.plusDays(1);
        }

        return results;
    }

    @Override
    @Transactional
    public void holdInventory(HoldInventoryRequest request) {
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày check-out phải sau ngày check-in");
        }

        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                        "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        OffsetDateTime lockExpires = OffsetDateTime.now().plusMinutes(request.getHoldMinutes());

        LocalDate cur = request.getCheckInDate();
        while (cur.isBefore(request.getCheckOutDate())) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(
                    request.getHotelRoomTypeId(), cur);

            RoomAvailability availability;
            if (opt.isPresent()) {
                availability = opt.get();
                int currentAvailable = availability.getTotalRooms()
                        - availability.getBookedRooms()
                        - availability.getLockedRooms()
                        - availability.getOooRooms();

                if (currentAvailable < request.getRoomsCount()) {
                    throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                            "Không đủ phòng trống ngày " + cur + ". Còn lại: " + Math.max(0, currentAvailable));
                }

                availability.setLockedRooms(availability.getLockedRooms() + request.getRoomsCount());
                availability.setLockedUntil(lockExpires);
            } else {
                int totalRooms = hotelRoomType.getTotalQuantity();
                if (totalRooms < request.getRoomsCount()) {
                    throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                            "Không đủ phòng trống ngày " + cur + ". Còn lại: " + totalRooms);
                }

                availability = RoomAvailability.builder()
                        .hotelRoomType(hotelRoomType)
                        .date(cur)
                        .totalRooms(totalRooms)
                        .bookedRooms(0)
                        .lockedRooms(request.getRoomsCount())
                        .oooRooms(0)
                        .lockedUntil(lockExpires)
                        .build();
            }

            roomAvailabilityRepository.save(availability);
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void releaseInventory(ReleaseInventoryRequest request) {
        LocalDate cur = request.getCheckInDate();
        while (cur.isBefore(request.getCheckOutDate())) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(
                    request.getHotelRoomTypeId(), cur);

            if (opt.isPresent()) {
                RoomAvailability availability = opt.get();
                int newLocked = Math.max(0, availability.getLockedRooms() - request.getRoomsCount());
                availability.setLockedRooms(newLocked);
                roomAvailabilityRepository.save(availability);
            }
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void confirmBookingInventory(Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate, Integer roomsCount) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(hotelRoomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                        "Không tìm thấy cấu hình loại phòng id: " + hotelRoomTypeId));

        LocalDate cur = startDate;
        while (cur.isBefore(endDate)) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(hotelRoomTypeId, cur);

            RoomAvailability availability;
            if (opt.isPresent()) {
                availability = opt.get();
                availability.setLockedRooms(Math.max(0, availability.getLockedRooms() - roomsCount));
                availability.setBookedRooms(availability.getBookedRooms() + roomsCount);
            } else {
                availability = RoomAvailability.builder()
                        .hotelRoomType(hotelRoomType)
                        .date(cur)
                        .totalRooms(hotelRoomType.getTotalQuantity())
                        .bookedRooms(roomsCount)
                        .lockedRooms(0)
                        .oooRooms(0)
                        .build();
            }

            roomAvailabilityRepository.save(availability);
            cur = cur.plusDays(1);
        }
    }
}
