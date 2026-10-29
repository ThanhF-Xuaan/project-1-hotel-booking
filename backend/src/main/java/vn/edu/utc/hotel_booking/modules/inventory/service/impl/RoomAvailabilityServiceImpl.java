package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
import vn.edu.utc.hotel_booking.modules.inventory.service.BlockedInventoryProvider;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInventoryHoldService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {

    RoomAvailabilityRepository roomAvailabilityRepository;
    HotelRoomTypeService hotelRoomTypeService;
    RoomInventoryHoldService roomInventoryHoldService;
    RoomAvailabilityMapper roomAvailabilityMapper;
    Optional<BlockedInventoryProvider> blockedInventoryProvider;
    RoomAvailabilityGenerationService roomAvailabilityGenerationService;

    private int getGroupBlockedRooms(Integer hotelRoomTypeId, LocalDate date) {
        if (blockedInventoryProvider == null) {
            return 0;
        }
        return blockedInventoryProvider
                .map(provider -> provider.getActiveBlockedRooms(hotelRoomTypeId, date))
                .orElse(0);
    }

    private RoomAvailability getOrGenerateAvailabilityWithLock(Integer hotelRoomTypeId, LocalDate date) {
        Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(hotelRoomTypeId, date);
        if (opt.isPresent()) {
            return opt.get();
        }

        // Self-healing: Nếu chưa có trong DB (ví dụ hạng phòng mới tạo hoặc query ngoài chân trời), sinh bù
        if (roomAvailabilityGenerationService != null) {
            roomAvailabilityGenerationService.generateForRoomType(hotelRoomTypeId, date, 365);
            return roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(hotelRoomTypeId, date)
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DATE_OUT_OF_HORIZON,
                            "Ngày lưu trú " + date + " nằm ngoài chân trời mở bán của khách sạn"));
        }

        throw new AppException(ErrorCode.BOOKING_DATE_OUT_OF_HORIZON,
                "Ngày lưu trú " + date + " nằm ngoài chân trời mở bán của khách sạn");
    }

    @Override
    public List<RoomAvailabilityResponse> getAvailability(RoomAvailabilitySearchDto searchDto) {
        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(searchDto.getHotelRoomTypeId());

        List<RoomAvailability> existingList = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateBetweenOrderByDateAsc(
                searchDto.getHotelRoomTypeId(),
                searchDto.getStartDate(),
                searchDto.getEndDate()
        );

        // Missing days are virtual read results; only explicit write flows generate rows.
        Map<LocalDate, RoomAvailability> map = existingList.stream()
                .collect(Collectors.toMap(RoomAvailability::getDate, Function.identity()));

        List<RoomAvailabilityResponse> results = new ArrayList<>();
        LocalDate cur = searchDto.getStartDate();
        while (!cur.isAfter(searchDto.getEndDate())) {
            RoomAvailability row = map.get(cur);
            int groupBlocked = getGroupBlockedRooms(searchDto.getHotelRoomTypeId(), cur);
            if (row != null) {
                int avail = (row.getAvailableCount() != null)
                        ? (row.getAvailableCount() - groupBlocked)
                        : (row.getTotalRooms() - row.getBookedRooms() - row.getLockedRooms() - row.getOooRooms() - groupBlocked);
                RoomAvailabilityResponse res = roomAvailabilityMapper.toResponse(row);
                res.setAvailableCount(Math.max(0, avail));
                results.add(res);
            } else {
                int avail = hotelRoomType.getTotalQuantity() - groupBlocked;
                results.add(RoomAvailabilityResponse.builder()
                        .hotelRoomTypeId(hotelRoomType.getId())
                        .date(cur)
                        .totalRooms(hotelRoomType.getTotalQuantity())
                        .bookedRooms(0)
                        .lockedRooms(0)
                        .oooRooms(0)
                        .availableCount(Math.max(0, avail))
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

        OffsetDateTime lockExpires = OffsetDateTime.now().plusMinutes(request.getHoldMinutes());

        LocalDate cur = request.getCheckInDate();
        while (cur.isBefore(request.getCheckOutDate())) {
            int groupBlocked = getGroupBlockedRooms(request.getHotelRoomTypeId(), cur);
            RoomAvailability availability = getOrGenerateAvailabilityWithLock(request.getHotelRoomTypeId(), cur);

            int currentAvailable = availability.getTotalRooms()
                    - availability.getBookedRooms()
                    - availability.getLockedRooms()
                    - availability.getOooRooms()
                    - groupBlocked;

            if (currentAvailable < request.getRoomsCount()) {
                throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                        "Không đủ phòng trống ngày " + cur + ". Còn lại: " + Math.max(0, currentAvailable));
            }

            availability.setLockedRooms(availability.getLockedRooms() + request.getRoomsCount());
            availability.setLockedUntil(lockExpires);
            roomAvailabilityRepository.save(availability);

            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void releaseInventory(ReleaseInventoryRequest request) {
        LocalDate cur = request.getCheckInDate();
        while (cur.isBefore(request.getCheckOutDate())) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(
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
                int groupBlocked = getGroupBlockedRooms(item.getHotelRoomTypeId(), cur);
                RoomAvailability availability = getOrGenerateAvailabilityWithLock(item.getHotelRoomTypeId(), cur);

                int currentAvailable = availability.getTotalRooms()
                        - availability.getBookedRooms()
                        - availability.getLockedRooms()
                        - availability.getOooRooms()
                        - groupBlocked;

                if (currentAvailable < item.getRoomsCount()) {
                    throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                            "Không đủ phòng trống ngày " + cur + ". Còn lại: " + Math.max(0, currentAvailable));
                }

                availability.setLockedRooms(availability.getLockedRooms() + item.getRoomsCount());
                availability.setLockedUntil(lockExpires);
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
                Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(
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
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(
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
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(
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
        LocalDate cur = startDate;
        while (cur.isBefore(endDate)) {
            RoomAvailability availability = getOrGenerateAvailabilityWithLock(hotelRoomTypeId, cur);
            availability.setLockedRooms(Math.max(0, availability.getLockedRooms() - roomsCount));
            availability.setBookedRooms(availability.getBookedRooms() + roomsCount);
            roomAvailabilityRepository.save(availability);
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void releaseBookedInventory(Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate, Integer roomsCount) {
        if (hotelRoomTypeId == null || startDate == null || endDate == null || roomsCount == null || roomsCount <= 0) {
            return;
        }
        LocalDate cur = startDate;
        while (cur.isBefore(endDate)) {
            Optional<RoomAvailability> opt = roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(hotelRoomTypeId, cur);
            if (opt.isPresent()) {
                RoomAvailability availability = opt.get();
                availability.setBookedRooms(Math.max(0, availability.getBookedRooms() - roomsCount));
                roomAvailabilityRepository.save(availability);
            }
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
