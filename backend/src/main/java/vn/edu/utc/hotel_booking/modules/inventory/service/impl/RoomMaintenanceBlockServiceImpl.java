package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomMaintenanceBlockResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomMaintenanceBlock;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomMaintenanceBlockMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomMaintenanceBlockRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomMaintenanceBlockService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomMaintenanceBlockServiceImpl implements RoomMaintenanceBlockService {

    RoomMaintenanceBlockRepository repository;
    RoomInstanceRepository roomInstanceRepository;
    RoomSlotRepository roomSlotRepository;
    RoomAvailabilityService roomAvailabilityService;
    BookingService bookingService;
    StaffRepository staffRepository;
    RoomMaintenanceBlockMapper mapper;

    @Override
    @Transactional
    public RoomMaintenanceBlockResponse create(RoomMaintenanceBlockCreateRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new AppException(ErrorCode.MAINTENANCE_BLOCK_TIME_INVALID, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        RoomInstance room = roomInstanceRepository.findByIdWithLock(request.getRoomInstanceId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng vật lý"));

        if ("OCCUPIED".equalsIgnoreCase(room.getCurrentStatus())) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BLOCKED,
                    "Phòng " + room.getRoomNumber() + " đang có khách lưu trú, không thể tạo lệnh khóa phòng");
        }

        List<RoomMaintenanceBlock> overlapBlocks = repository.findActiveOverlap(
                room.getId(), request.getStartDate(), request.getEndDate());
        if (!overlapBlocks.isEmpty()) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BLOCKED,
                    "Phòng " + room.getRoomNumber() + " đã có lệnh khóa phòng khác đang hiệu lực trong khoảng thời gian này");
        }

        boolean isBooked = bookingService.isRoomBookedBetween(room.getId(), request.getStartDate(), request.getEndDate());
        if (isBooked) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BLOCKED,
                    "Phòng " + room.getRoomNumber() + " đã có đơn đặt phòng trong khoảng thời gian này");
        }

        if ("OOO".equalsIgnoreCase(request.getBlockType())) {
            roomAvailabilityService.applyOooDelta(
                    room.getHotelRoomType().getId(), request.getStartDate(), request.getEndDate(), 1);
        }

        Integer staffId = resolveCurrentStaffId();

        RoomMaintenanceBlock block = mapper.toEntity(request);
        block.setRoomInstance(room);
        block.setStatus("ACTIVE");
        block.setCreatedByStaffId(staffId);
        block = repository.save(block);

        room.setCurrentStatus("MAINTENANCE");
        roomInstanceRepository.save(room);

        if ("OOO".equalsIgnoreCase(request.getBlockType())) {
            LocalDate today = LocalDate.now();
            LocalDate slotStart = request.getStartDate().isBefore(today) ? today : request.getStartDate();
            LocalDate cur = slotStart;
            while (!cur.isAfter(request.getEndDate())) {
                Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDateWithLock(room.getId(), cur);
                RoomSlot slot;
                if (optSlot.isPresent()) {
                    slot = optSlot.get();
                } else {
                    slot = RoomSlot.builder()
                            .roomInstance(room)
                            .slotDate(cur)
                            .build();
                }
                slot.setStatus("BLOCKED");
                slot.setMaintenanceBlock(block);
                roomSlotRepository.save(slot);
                cur = cur.plusDays(1);
            }
        }

        return mapper.toResponse(block);
    }

    @Override
    @Transactional
    public RoomMaintenanceBlockResponse update(Long id, RoomMaintenanceBlockUpdateRequest request) {
        RoomMaintenanceBlock block = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAINTENANCE_BLOCK_NOT_FOUND, "Không tìm thấy thông tin khóa phòng"));

        if (!"ACTIVE".equalsIgnoreCase(block.getStatus())) {
            throw new AppException(ErrorCode.MAINTENANCE_BLOCK_TIME_INVALID,
                    "Chỉ có thể chỉnh sửa lệnh khóa phòng đang ở trạng thái ACTIVE");
        }

        RoomInstance room = roomInstanceRepository.findByIdWithLock(block.getRoomInstance().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng vật lý"));

        LocalDate newStart = request.getStartDate() != null ? request.getStartDate() : block.getStartDate();
        LocalDate newEnd = request.getEndDate() != null ? request.getEndDate() : block.getEndDate();

        if (newStart.isAfter(newEnd)) {
            throw new AppException(ErrorCode.MAINTENANCE_BLOCK_TIME_INVALID, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        String newType = request.getBlockType() != null ? request.getBlockType().toUpperCase() : block.getBlockType();

        List<RoomMaintenanceBlock> overlap = repository.findActiveOverlapExcluding(room.getId(), block.getId(), newStart, newEnd);
        if (!overlap.isEmpty()) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BLOCKED, "Khoảng thời gian điều chỉnh bị trùng với lệnh khóa phòng khác");
        }

        boolean isBooked = bookingService.isRoomBookedBetween(room.getId(), newStart, newEnd);
        if (isBooked) {
            throw new AppException(ErrorCode.ROOM_ALREADY_BLOCKED, "Phòng đã có đơn đặt phòng trong khoảng thời gian mới");
        }

        boolean oldIsOoo = "OOO".equalsIgnoreCase(block.getBlockType());
        boolean newIsOoo = "OOO".equalsIgnoreCase(newType);

        if (oldIsOoo) {
            roomAvailabilityService.applyOooDelta(room.getHotelRoomType().getId(), block.getStartDate(), block.getEndDate(), -1);
        }
        if (newIsOoo) {
            roomAvailabilityService.applyOooDelta(room.getHotelRoomType().getId(), newStart, newEnd, 1);
        }

        List<RoomSlot> currentSlots = roomSlotRepository.findByMaintenanceBlockIdWithLock(block.getId());
        for (RoomSlot s : currentSlots) {
            if (!newIsOoo || s.getSlotDate().isBefore(newStart) || s.getSlotDate().isAfter(newEnd)) {
                s.setStatus("READY");
                s.setMaintenanceBlock(null);
                roomSlotRepository.save(s);
            }
        }

        if (newIsOoo) {
            LocalDate today = LocalDate.now();
            LocalDate slotStart = newStart.isBefore(today) ? today : newStart;
            LocalDate cur = slotStart;
            while (!cur.isAfter(newEnd)) {
                final LocalDate slotDate = cur;
                Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDateWithLock(room.getId(), slotDate);
                RoomSlot slot = optSlot.orElseGet(() -> RoomSlot.builder()
                        .roomInstance(room)
                        .slotDate(slotDate)
                        .build());
                slot.setStatus("BLOCKED");
                slot.setMaintenanceBlock(block);
                roomSlotRepository.save(slot);
                cur = cur.plusDays(1);
            }
        }

        block.setStartDate(newStart);
        block.setEndDate(newEnd);
        block.setBlockType(newType);
        if (request.getReason() != null) {
            block.setReason(request.getReason());
        }
        if (request.getNotes() != null) {
            block.setNotes(request.getNotes());
        }
        if (request.getMaintenanceTicketId() != null) {
            block.setMaintenanceTicketId(request.getMaintenanceTicketId());
        }
        block.setUpdatedByStaffId(resolveCurrentStaffId());
        block = repository.save(block);

        return mapper.toResponse(block);
    }

    @Override
    @Transactional
    public RoomMaintenanceBlockResponse complete(Long id) {
        return releaseBlock(id, "COMPLETED");
    }

    @Override
    @Transactional
    public RoomMaintenanceBlockResponse cancel(Long id) {
        return releaseBlock(id, "CANCELLED");
    }

    private RoomMaintenanceBlockResponse releaseBlock(Long id, String targetStatus) {
        RoomMaintenanceBlock block = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAINTENANCE_BLOCK_NOT_FOUND, "Không tìm thấy thông tin khóa phòng"));

        if (!"ACTIVE".equalsIgnoreCase(block.getStatus())) {
            throw new AppException(ErrorCode.MAINTENANCE_BLOCK_TIME_INVALID,
                    "Thao tác chỉ áp dụng cho lệnh khóa phòng đang ACTIVE");
        }

        RoomInstance room = roomInstanceRepository.findByIdWithLock(block.getRoomInstance().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng vật lý"));

        if ("OOO".equalsIgnoreCase(block.getBlockType())) {
            roomAvailabilityService.applyOooDelta(room.getHotelRoomType().getId(), block.getStartDate(), block.getEndDate(), -1);
        }

        List<RoomSlot> slots = roomSlotRepository.findByMaintenanceBlockIdWithLock(block.getId());
        for (RoomSlot s : slots) {
            s.setStatus("READY");
            s.setMaintenanceBlock(null);
            roomSlotRepository.save(s);
        }

        block.setStatus(targetStatus);
        block.setUpdatedByStaffId(resolveCurrentStaffId());
        block = repository.save(block);

        boolean hasOtherActive = repository.existsByRoomInstanceIdAndStatus(room.getId(), "ACTIVE");
        if (hasOtherActive) {
            room.setCurrentStatus("MAINTENANCE");
        } else {
            Optional<RoomSlot> todaySlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(room.getId(), LocalDate.now());
            if (todaySlot.isPresent() && "OCCUPIED".equalsIgnoreCase(todaySlot.get().getStatus())) {
                room.setCurrentStatus("OCCUPIED");
            } else if (todaySlot.isPresent() && ("DIRTY".equalsIgnoreCase(todaySlot.get().getStatus()) || "CLEANING".equalsIgnoreCase(todaySlot.get().getStatus()))) {
                room.setCurrentStatus("CLEANING");
            } else {
                room.setCurrentStatus("READY");
                todaySlot.ifPresent(s -> {
                    s.setStatus("READY");
                    roomSlotRepository.save(s);
                });
            }
        }
        roomInstanceRepository.save(room);

        return mapper.toResponse(block);
    }

    @Override
    public PageResponse<RoomMaintenanceBlockResponse> filter(RoomMaintenanceBlockSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() >= 0 ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").descending());

        Page<RoomMaintenanceBlock> p = repository.filter(
                searchDto.getRoomInstanceId(),
                searchDto.getHotelId(),
                searchDto.getBlockType(),
                searchDto.getStatus(),
                searchDto.getFromDate(),
                searchDto.getToDate(),
                pageable
        );

        return PageResponse.from(p.map(mapper::toResponse));
    }

    @Override
    public RoomMaintenanceBlockResponse getById(Long id) {
        RoomMaintenanceBlock block = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAINTENANCE_BLOCK_NOT_FOUND, "Không tìm thấy thông tin khóa phòng"));
        return mapper.toResponse(block);
    }

    @Override
    public List<RoomMaintenanceBlockResponse> getByRoomInstanceId(Integer roomInstanceId) {
        List<RoomMaintenanceBlock> list = repository.findByRoomInstanceIdOrderByCreatedAtDesc(roomInstanceId);
        return mapper.toResponseList(list);
    }

    private Integer resolveCurrentStaffId() {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(staffRepository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername()
                        .flatMap(staffRepository::findByUsernameAndIsDeletedFalse))
                .map(Staff::getId)
                .orElse(null);
    }
}
