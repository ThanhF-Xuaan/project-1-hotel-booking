package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAssignmentCandidateSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAssignmentCandidateResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomInstanceResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstanceStatus;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlotStatus;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomInstanceMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomInstanceServiceImpl implements RoomInstanceService {

    RoomInstanceRepository roomInstanceRepository;
    HotelRepository hotelRepository;
    HotelRoomTypeRepository hotelRoomTypeRepository;
    RoomSlotRepository roomSlotRepository;
    BookingRoomRepository bookingRoomRepository;
    RoomInstanceMapper roomInstanceMapper;

    @Override
    @Transactional
    public RoomInstanceResponse createRoomInstance(RoomInstanceCreateRequest request) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn id: " + request.getHotelId()));

        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        String normalizedRoomNumber = request.getRoomNumber() != null ? request.getRoomNumber().trim() : "";
        if (normalizedRoomNumber.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Số phòng không được để trống");
        }

        if (roomInstanceRepository.existsByHotelIdAndRoomNumberIgnoreCaseTrim(request.getHotelId(), normalizedRoomNumber)) {
            throw new AppException(ErrorCode.ROOM_NUMBER_ALREADY_EXISTS, "Số phòng '" + normalizedRoomNumber + "' đã tồn tại trong khách sạn này");
        }

        RoomInstance roomInstance = roomInstanceMapper.toEntity(request);
        roomInstance.setRoomNumber(normalizedRoomNumber);
        roomInstance.setHotel(hotel);
        roomInstance.setHotelRoomType(hotelRoomType);

        RoomInstance saved = roomInstanceRepository.save(roomInstance);
        return roomInstanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoomInstanceResponse updateRoomInstance(Integer id, RoomInstanceUpdateRequest request) {
        RoomInstance roomInstance = roomInstanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng id: " + id));

        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        String normalizedRoomNumber = request.getRoomNumber() != null ? request.getRoomNumber().trim() : "";
        if (normalizedRoomNumber.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Số phòng không được để trống");
        }

        if (roomInstanceRepository.existsByHotelIdAndRoomNumberAndIdNotIgnoreCaseTrim(
                roomInstance.getHotel().getId(), normalizedRoomNumber, id)) {
            throw new AppException(ErrorCode.ROOM_NUMBER_ALREADY_EXISTS, "Số phòng '" + normalizedRoomNumber + "' đã tồn tại trong khách sạn");
        }

        roomInstanceMapper.updateEntity(roomInstance, request);
        roomInstance.setRoomNumber(normalizedRoomNumber);
        roomInstance.setHotelRoomType(hotelRoomType);

        RoomInstance updated = roomInstanceRepository.save(roomInstance);
        return roomInstanceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public RoomInstanceResponse updateRoomStatus(Integer id, String status) {
        RoomInstance updated = updateRoomStatusEntity(id, status);
        return roomInstanceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public RoomInstance updateRoomStatusEntity(Integer id, String status) {
        RoomInstance roomInstance = roomInstanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng id: " + id));

        roomInstance.setCurrentStatus(status);
        RoomInstance saved = roomInstanceRepository.save(roomInstance);

        // Đồng bộ trạng thái vào RoomSlot hôm nay
        LocalDate today = LocalDate.now();
        roomSlotRepository.findByRoomInstanceIdAndSlotDate(id, today).ifPresent(slot -> {
            slot.setStatus(status);
            if (RoomSlotStatus.READY.name().equalsIgnoreCase(status)) {
                slot.setBookingRoomId(null);
                slot.setReservedAt(null);
            }
            roomSlotRepository.save(slot);
        });

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomInstance> getRoomsByHotelAndStatus(Short hotelId, String status) {
        if (hotelId == null) {
            return List.of();
        }
        String searchStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        return roomInstanceRepository.findByHotelIdAndCurrentStatus(hotelId, searchStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public int countOccupiedRooms(Short hotelId) {
        if (hotelId == null) {
            return 0;
        }
        return (int) roomInstanceRepository.countOccupiedRoomsByHotelId(hotelId);
    }

    @Override
    public RoomInstanceResponse getRoomInstanceById(Integer id) {
        RoomInstance roomInstance = getEntityById(id);
        return roomInstanceMapper.toResponse(roomInstance);
    }

    @Override
    public RoomInstance getEntityById(Integer id) {
        return roomInstanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng vật lý id: " + id));
    }

    @Override
    public PageResponse<RoomInstanceResponse> filterRoomInstances(RoomInstanceSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        org.springframework.data.jpa.domain.Specification<RoomInstance> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (searchDto.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), searchDto.getHotelId()));
            }
            if (searchDto.getHotelRoomTypeId() != null) {
                predicates.add(cb.equal(root.get("hotelRoomType").get("id"), searchDto.getHotelRoomTypeId()));
            }
            if (searchDto.getRoomNumber() != null && !searchDto.getRoomNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("roomNumber")), "%" + searchDto.getRoomNumber().trim().toLowerCase() + "%"));
            }
            if (searchDto.getCurrentStatus() != null && !searchDto.getCurrentStatus().isBlank()) {
                predicates.add(cb.equal(root.get("currentStatus"), searchDto.getCurrentStatus()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<RoomInstance> page = roomInstanceRepository.findAll(spec, pageable);

        return PageResponse.<RoomInstanceResponse>builder()
                .content(roomInstanceMapper.toResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public void deleteRoomInstances(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            roomInstanceRepository.softDeleteByIds(ids);
        }
    }

    @Override
    public List<RoomInstance> getActiveRoomsByHotelIds(List<Short> hotelIds) {
        if (hotelIds == null || hotelIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return roomInstanceRepository.findByHotelIdInAndIsDeletedFalse(hotelIds);
    }

    @Override
    public List<RoomAssignmentCandidateResponse> findAvailableRoomsForAssignment(RoomAssignmentCandidateSearchDto request) {
        if (request.getHotelId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID khách sạn không được để trống");
        }
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new AppException(ErrorCode.INVALID_BOOKING_DATES, "Ngày nhận phòng và ngày trả phòng không được để trống");
        }
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_DATES, "Ngày nhận phòng phải trước ngày trả phòng");
        }

        // 1. Lấy tất cả phòng vật lý của khách sạn
        List<RoomInstance> allRooms = roomInstanceRepository.findByHotelIdAndIsDeletedFalseWithDetails(request.getHotelId());

        // 2. Query RoomSlotRepository: Lấy tất cả các slot BẬN trong dải ngày [checkInDate, checkOutDate)
        // (Chiến lược Sparse Data: Không có bản ghi hoặc bản ghi READY thì coi như TRỐNG)
        List<RoomSlot> busySlots = roomSlotRepository.findBusySlotsBetween(
                request.getHotelId(), request.getCheckInDate(), request.getCheckOutDate());

        Map<Integer, String> busySlotReasons = new HashMap<>();
        for (RoomSlot slot : busySlots) {
            if (request.getExcludeBookingRoomId() != null
                    && slot.getBookingRoomId() != null
                    && slot.getBookingRoomId().equals(request.getExcludeBookingRoomId())) {
                continue;
            }

            String status = slot.getStatus() != null ? slot.getStatus().toUpperCase() : "BUSY";
            String reason;
            if (RoomSlotStatus.BLOCKED.name().equals(status) || RoomSlotStatus.MAINTENANCE.name().equals(status)) {
                reason = "Đang khóa bảo trì (OOO)";
            } else if (RoomSlotStatus.RESERVED.name().equals(status) || RoomSlotStatus.OCCUPIED.name().equals(status)) {
                reason = "Đã có khách đặt / lưu trú";
            } else if (RoomSlotStatus.DIRTY.name().equals(status) || RoomSlotStatus.CLEANING.name().equals(status)) {
                // Self-healing: Nếu slot bị đánh dấu DIRTY/CLEANING trong ngày hôm nay nhưng phòng vật lý thực tế đã được xác nhận READY
                RoomInstance physRoom = slot.getRoomInstance();
                if (slot.getSlotDate() != null && slot.getSlotDate().equals(LocalDate.now())
                        && physRoom != null && RoomInstanceStatus.READY.name().equalsIgnoreCase(physRoom.getCurrentStatus())) {
                    slot.setStatus(RoomSlotStatus.READY.name());
                    slot.setBookingRoomId(null);
                    slot.setReservedAt(null);
                    roomSlotRepository.save(slot);
                    continue; // Phòng đã sẵn sàng, không đưa vào danh sách bận
                }
                reason = "Phòng chưa dọn dẹp xong";
            } else {
                reason = "Phòng không khả dụng (" + status + ")";
            }
            busySlotReasons.put(slot.getRoomInstance().getId(), reason);
        }

        // 3. Query BookingRoomRepository: Lấy tất cả BookingRoom trùng lịch trong khách sạn (bảo vệ 2 tầng)
        List<BookingRoom> overlappingBookings = bookingRoomRepository.findOverlappingAssignedRoomsInHotel(
                request.getHotelId(), request.getCheckInDate(), request.getCheckOutDate());

        Map<Integer, String> bookingOverlapReasons = new HashMap<>();
        for (BookingRoom br : overlappingBookings) {
            if (request.getExcludeBookingRoomId() != null && br.getId().equals(request.getExcludeBookingRoomId())) {
                continue;
            }
            if (br.getRoomInstance() != null) {
                bookingOverlapReasons.put(br.getRoomInstance().getId(), "Đã có khách đặt trong khoảng ngày này");
            }
        }

        // 4. Kiểm tra điều kiện hôm nay (Housekeeping guard nếu check-in hôm nay)
        LocalDate today = LocalDate.now();
        boolean isCheckInToday = !request.getCheckInDate().isAfter(today);

        // 5. Build danh sách Candidate Responses
        List<RoomAssignmentCandidateResponse> candidates = new ArrayList<>();
        for (RoomInstance room : allRooms) {
            boolean isSameRoomType = request.getTargetHotelRoomTypeId() != null
                    && room.getHotelRoomType() != null
                    && room.getHotelRoomType().getId().equals(request.getTargetHotelRoomTypeId());

            boolean isAvailable = true;
            String conflictReason = null;

            if (busySlotReasons.containsKey(room.getId())) {
                isAvailable = false;
                conflictReason = busySlotReasons.get(room.getId());
            } else if (bookingOverlapReasons.containsKey(room.getId())) {
                isAvailable = false;
                conflictReason = bookingOverlapReasons.get(room.getId());
            } else if (isCheckInToday && room.getCurrentStatus() != null && !RoomInstanceStatus.READY.name().equalsIgnoreCase(room.getCurrentStatus())) {
                isAvailable = false;
                conflictReason = "Phòng chưa sẵn sàng hôm nay (" + room.getCurrentStatus() + ")";
            }

            HotelRoomType hrt = room.getHotelRoomType();
            String roomTypeCode = hrt != null && hrt.getRoomType() != null ? hrt.getRoomType().getCode() : "";
            String roomTypeName = hrt != null && hrt.getRoomType() != null ? hrt.getRoomType().getName() : "";

            candidates.add(RoomAssignmentCandidateResponse.builder()
                    .id(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .hotelId(room.getHotel() != null ? room.getHotel().getId() : null)
                    .hotelName(room.getHotel() != null ? room.getHotel().getName() : null)
                    .hotelRoomTypeId(hrt != null ? hrt.getId() : null)
                    .roomTypeCode(roomTypeCode)
                    .roomTypeName(roomTypeName)
                    .currentStatus(room.getCurrentStatus())
                    .isAvailable(isAvailable)
                    .isSameRoomType(isSameRoomType)
                    .conflictReason(conflictReason)
                    .build());
        }

        // Sắp xếp ưu tiên:
        // 1. isAvailable DESC (phòng trống lên trước)
        // 2. isSameRoomType DESC (cùng hạng lên trước)
        // 3. roomNumber ASC
        candidates.sort(Comparator
                .comparing(RoomAssignmentCandidateResponse::isAvailable, Comparator.reverseOrder())
                .thenComparing(RoomAssignmentCandidateResponse::isSameRoomType, Comparator.reverseOrder())
                .thenComparing(RoomAssignmentCandidateResponse::getRoomNumber, Comparator.nullsLast(String::compareToIgnoreCase)));

        return candidates;
    }
}
