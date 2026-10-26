package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomMaintenanceBlockRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomAvailabilityGenerationServiceImpl implements RoomAvailabilityGenerationService {

    RoomAvailabilityRepository roomAvailabilityRepository;
    HotelRoomTypeRepository hotelRoomTypeRepository;
    RoomMaintenanceBlockRepository roomMaintenanceBlockRepository;

    @Override
    @Transactional
    public int generateForRoomType(Integer hotelRoomTypeId, LocalDate startDate, int horizonDays) {
        if (hotelRoomTypeId == null || startDate == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID hạng phòng và ngày bắt đầu không được để trống");
        }

        HotelRoomType roomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(hotelRoomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                        "Không tìm thấy cấu hình hạng phòng ID: " + hotelRoomTypeId));

        int days = (horizonDays <= 0) ? 365 : horizonDays;
        LocalDate endDate = startDate.plusDays(days - 1);

        Set<LocalDate> existingDates = roomAvailabilityRepository.findExistingDates(hotelRoomTypeId, startDate, endDate);

        List<RoomAvailability> toCreate = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            if (!existingDates.contains(cur)) {
                int activeOoo = 0;
                try {
                    activeOoo = roomMaintenanceBlockRepository.countActiveOooByHotelRoomTypeIdAndDate(hotelRoomTypeId, cur);
                } catch (Exception ex) {
                    log.warn("Không thể đếm số phòng OOO cho hạng phòng {} ngày {}: {}", hotelRoomTypeId, cur, ex.getMessage());
                }

                toCreate.add(RoomAvailability.builder()
                        .hotelRoomType(roomType)
                        .date(cur)
                        .totalRooms(roomType.getTotalQuantity())
                        .bookedRooms(0)
                        .lockedRooms(0)
                        .oooRooms(activeOoo)
                        .version(0L)
                        .build());
            }
            cur = cur.plusDays(1);
        }

        if (!toCreate.isEmpty()) {
            roomAvailabilityRepository.saveAll(toCreate);
            log.info("Đã sinh sẵn {} bản ghi tồn kho cho hạng phòng [{}] từ ngày {} đến {}",
                    toCreate.size(), roomType.getId(), startDate, endDate);
        }

        return toCreate.size();
    }

    @Override
    @Transactional
    public int rollHorizonForAllActiveRoomTypes(int horizonDays) {
        int days = (horizonDays <= 0) ? 365 : horizonDays;
        List<HotelRoomType> activeTypes = hotelRoomTypeRepository.findByIsDeletedFalseAndStatus("ACTIVE");

        LocalDate today = LocalDate.now();
        int totalGenerated = 0;

        for (HotelRoomType rt : activeTypes) {
            try {
                totalGenerated += generateForRoomType(rt.getId(), today, days);
            } catch (Exception ex) {
                log.error("Lỗi khi cuộn tồn kho cho hạng phòng id {}: {}", rt.getId(), ex.getMessage(), ex);
            }
        }

        log.info("Hoàn tất cuộn chân trời tồn kho {} ngày cho {} hạng phòng. Tổng bản ghi sinh mới: {}",
                days, activeTypes.size(), totalGenerated);
        return totalGenerated;
    }

    @Override
    @Transactional
    public int rollHorizonForHotel(Short hotelId, int horizonDays) {
        if (hotelId == null) {
            return 0;
        }
        int days = (horizonDays <= 0) ? 365 : horizonDays;
        List<HotelRoomType> activeTypes = hotelRoomTypeRepository.findByHotelIdAndIsDeletedFalse(hotelId).stream()
                .filter(rt -> "ACTIVE".equalsIgnoreCase(rt.getStatus()))
                .toList();

        LocalDate today = LocalDate.now();
        int totalGenerated = 0;

        for (HotelRoomType rt : activeTypes) {
            try {
                totalGenerated += generateForRoomType(rt.getId(), today, days);
            } catch (Exception ex) {
                log.error("Lỗi khi cuộn tồn kho cho hạng phòng id {} (khách sạn {}): {}", rt.getId(), hotelId, ex.getMessage(), ex);
            }
        }

        log.info("Đã cuộn chân trời tồn kho cho khách sạn {}. Số bản ghi sinh mới: {}", hotelId, totalGenerated);
        return totalGenerated;
    }

    @Override
    @Transactional
    public int propagateCapacityChange(Integer hotelRoomTypeId, int newTotalQuantity) {
        if (hotelRoomTypeId == null || newTotalQuantity < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Số lượng phòng không hợp lệ");
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int rowsUpdated = roomAvailabilityRepository.bulkUpdateTotalRooms(hotelRoomTypeId, tomorrow, newTotalQuantity);
        log.info("Đã lan truyền total_rooms = {} cho {} bản ghi tương lai của hạng phòng {}",
                newTotalQuantity, rowsUpdated, hotelRoomTypeId);
        return rowsUpdated;
    }

    @Override
    @Transactional
    public void syncOooRooms(Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate) {
        if (hotelRoomTypeId == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return;
        }

        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            int activeOoo = roomMaintenanceBlockRepository.countActiveOooByHotelRoomTypeIdAndDate(hotelRoomTypeId, cur);
            roomAvailabilityRepository.updateOooRooms(hotelRoomTypeId, cur, activeOoo);
            cur = cur.plusDays(1);
        }
    }
}
