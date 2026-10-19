package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchHoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHold;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHoldStatus;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomAvailabilityMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInventoryHoldService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {

    RoomAvailabilityRepository roomAvailabilityRepository;
    HotelRoomTypeService hotelRoomTypeService;
    RoomInventoryHoldService roomInventoryHoldService;
    RoomAvailabilityMapper roomAvailabilityMapper;

    @Override
    public List<RoomAvailabilityResponse> getAvailability(RoomAvailabilitySearchDto searchDto) {
        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(searchDto.getHotelRoomTypeId());

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

        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(request.getHotelRoomTypeId());

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
    public String batchHoldInventory(BatchHoldInventoryRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Danh sách hạng phòng cần giữ không được để trống");
        }
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày check-out phải sau ngày check-in");
        }

        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId().trim()
                : UUID.randomUUID().toString();

        int holdMinutes = request.getHoldMinutes() != null ? request.getHoldMinutes() : 15;
        OffsetDateTime lockExpires = OffsetDateTime.now().plusMinutes(holdMinutes);

        // Lock Ordering: Sắp xếp danh sách hạng phòng theo hotelRoomTypeId tăng dần (ASC) để tránh Deadlock
        List<BatchHoldInventoryRequest.RoomTypeHoldItem> sortedItems = new ArrayList<>(request.getItems());
        sortedItems.sort(Comparator.comparing(BatchHoldInventoryRequest.RoomTypeHoldItem::getHotelRoomTypeId));

        List<RoomInventoryHold> holdsToSave = new ArrayList<>();

        for (BatchHoldInventoryRequest.RoomTypeHoldItem item : sortedItems) {
            HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(item.getHotelRoomTypeId());

            LocalDate cur = request.getCheckInDate();
            while (cur.isBefore(request.getCheckOutDate())) {
                Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(
                        item.getHotelRoomTypeId(), cur);

                RoomAvailability availability;
                if (opt.isPresent()) {
                    availability = opt.get();
                    int currentAvailable = availability.getTotalRooms()
                            - availability.getBookedRooms()
                            - availability.getLockedRooms()
                            - availability.getOooRooms();

                    if (currentAvailable < item.getRoomsCount()) {
                        throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                                "Không đủ phòng trống ngày " + cur + ". Còn lại: " + Math.max(0, currentAvailable));
                    }

                    availability.setLockedRooms(availability.getLockedRooms() + item.getRoomsCount());
                    availability.setLockedUntil(lockExpires);
                } else {
                    int totalRooms = hotelRoomType.getTotalQuantity();
                    if (totalRooms < item.getRoomsCount()) {
                        throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                                "Không đủ phòng trống ngày " + cur + ". Còn lại: " + totalRooms);
                    }

                    availability = RoomAvailability.builder()
                            .hotelRoomType(hotelRoomType)
                            .date(cur)
                            .totalRooms(totalRooms)
                            .bookedRooms(0)
                            .lockedRooms(item.getRoomsCount())
                            .oooRooms(0)
                            .lockedUntil(lockExpires)
                            .build();
                }

                roomAvailabilityRepository.save(availability);

                // Lưu vết hold chi tiết theo từng session
                holdsToSave.add(RoomInventoryHold.builder()
                        .sessionId(sessionId)
                        .hotelRoomType(hotelRoomType)
                        .holdDate(cur)
                        .quantity(item.getRoomsCount())
                        .expiresAt(lockExpires)
                        .status(RoomInventoryHoldStatus.HELD)
                        .build());

                cur = cur.plusDays(1);
            }
        }

        roomInventoryHoldService.saveAll(holdsToSave);
        return sessionId;
    }

    @Override
    @Transactional
    public void batchReleaseInventory(BatchReleaseInventoryRequest request) {
        if (request == null) {
            return;
        }

        // 1. Nhả theo sessionId (Granular Hold Session)
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            String sid = request.getSessionId().trim();
            List<RoomInventoryHold> activeHolds = roomInventoryHoldService.findActiveHoldsBySessionId(sid);
            if (activeHolds.isEmpty()) {
                return; // Idempotent: Đã được giải phóng hoặc không tồn tại
            }

            HoldReleaseReason reason = request.getReleaseReason() != null ? request.getReleaseReason() : HoldReleaseReason.USER_CANCELLED;

            for (RoomInventoryHold hold : activeHolds) {
                Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(
                        hold.getHotelRoomType().getId(), hold.getHoldDate());
                if (opt.isPresent()) {
                    RoomAvailability availability = opt.get();
                    int newLocked = Math.max(0, availability.getLockedRooms() - hold.getQuantity());
                    availability.setLockedRooms(newLocked);
                    roomAvailabilityRepository.save(availability);
                }
            }
            roomInventoryHoldService.markHoldsReleased(activeHolds, reason);
            return;
        }

        // 2. Legacy fallback: Nhả theo mảng items
        if (request.getItems() == null || request.getItems().isEmpty() || request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            return;
        }
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            return;
        }

        List<BatchHoldInventoryRequest.RoomTypeHoldItem> sortedItems = new ArrayList<>(request.getItems());
        sortedItems.sort(Comparator.comparing(BatchHoldInventoryRequest.RoomTypeHoldItem::getHotelRoomTypeId));

        for (BatchHoldInventoryRequest.RoomTypeHoldItem item : sortedItems) {
            ReleaseInventoryRequest relReq = ReleaseInventoryRequest.builder()
                    .hotelRoomTypeId(item.getHotelRoomTypeId())
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .roomsCount(item.getRoomsCount())
                    .build();
            releaseInventory(relReq);
        }
    }

    @Override
    @Transactional
    public void commitHoldInventory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        List<RoomInventoryHold> activeHolds = roomInventoryHoldService.findActiveHoldsBySessionId(sessionId.trim());
        if (activeHolds.isEmpty()) {
            return;
        }

        for (RoomInventoryHold hold : activeHolds) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(
                    hold.getHotelRoomType().getId(), hold.getHoldDate());
            if (opt.isPresent()) {
                RoomAvailability availability = opt.get();
                availability.setLockedRooms(Math.max(0, availability.getLockedRooms() - hold.getQuantity()));
                availability.setBookedRooms(availability.getBookedRooms() + hold.getQuantity());
                roomAvailabilityRepository.save(availability);
            }
        }
        roomInventoryHoldService.markHoldsCommitted(activeHolds);
    }

    @Override
    @Transactional
    public int releaseExpiredHolds() {
        List<RoomInventoryHold> expiredHolds = roomInventoryHoldService.findExpiredHolds(OffsetDateTime.now());
        if (expiredHolds.isEmpty()) {
            return 0;
        }

        for (RoomInventoryHold hold : expiredHolds) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(
                    hold.getHotelRoomType().getId(), hold.getHoldDate());
            if (opt.isPresent()) {
                RoomAvailability availability = opt.get();
                int newLocked = Math.max(0, availability.getLockedRooms() - hold.getQuantity());
                availability.setLockedRooms(newLocked);
                roomAvailabilityRepository.save(availability);
            }
        }
        roomInventoryHoldService.markHoldsReleased(expiredHolds, HoldReleaseReason.EXPIRED);
        return expiredHolds.size();
    }

    @Override
    @Transactional
    public void confirmBookingInventory(Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate, Integer roomsCount) {
        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(hotelRoomTypeId);

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

    @Override
    @Transactional
    public void applyOooDelta(Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate, int delta) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.MAINTENANCE_BLOCK_TIME_INVALID, "Khoảng thời gian bảo trì không hợp lệ");
        }
        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(hotelRoomTypeId);

        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(hotelRoomTypeId, cur);

            RoomAvailability availability;
            if (opt.isPresent()) {
                availability = opt.get();
            } else {
                availability = RoomAvailability.builder()
                        .hotelRoomType(hotelRoomType)
                        .date(cur)
                        .totalRooms(hotelRoomType.getTotalQuantity())
                        .bookedRooms(0)
                        .lockedRooms(0)
                        .oooRooms(0)
                        .build();
            }

            int currentBooked = availability.getBookedRooms() != null ? availability.getBookedRooms() : 0;
            int currentLocked = availability.getLockedRooms() != null ? availability.getLockedRooms() : 0;
            int currentOoo = availability.getOooRooms() != null ? availability.getOooRooms() : 0;
            int total = availability.getTotalRooms() != null ? availability.getTotalRooms() : hotelRoomType.getTotalQuantity();

            if (delta > 0) {
                int available = total - currentBooked - currentLocked - currentOoo;
                if (available - delta < 0) {
                    throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                            "Không đủ phòng trống để thực hiện khóa phòng OOO cho ngày " + cur);
                }
            }

            availability.setOooRooms(Math.max(0, currentOoo + delta));
            roomAvailabilityRepository.save(availability);

            cur = cur.plusDays(1);
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduledReleaseExpiredHolds() {
        int releasedCount = releaseExpiredHolds();
        if (releasedCount > 0) {
            log.info("Đã tự động thu hồi và giải phóng {} bản ghi giữ phòng quá hạn", releasedCount);
        }
    }
}
