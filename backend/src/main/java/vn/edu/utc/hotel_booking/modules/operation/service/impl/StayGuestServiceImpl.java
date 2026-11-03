package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import vn.edu.utc.hotel_booking.modules.operation.service.support.LodgingExportStateGuard;
import vn.edu.utc.hotel_booking.modules.operation.service.support.StayGuestTimeValidator;
import vn.edu.utc.hotel_booking.modules.operation.security.OperationStaffScopeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService;
import vn.edu.utc.hotel_booking.modules.operation.service.support.StayGuestAssociationValidator;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StayGuestServiceImpl implements StayGuestService {

    private final StayGuestRepository stayGuestRepository;
    private final LodgingQueueRepository lodgingQueueRepository;
    private final HotelRepository hotelRepository;
    private final BookingLookupService bookingLookupService;
    private final RoomInstanceRepository roomInstanceRepository;
    private final StaffRepository staffRepository;
    private final StayGuestMapper stayGuestMapper;
    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional
    public StayGuestResponse createOrCheckIn(StayGuestCreateRequest request) {
        validateStaffScope(request.getHotelId());

        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));

        Booking booking = request.getBookingId() == null ? null : bookingLookupService.getBookingEntityById(request.getBookingId());
        RoomInstance room = request.getRoomId() == null ? null : roomInstanceRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy phòng vật lý"));
        BookingRoom bookingRoom = request.getBookingRoomId() == null ? null
                : bookingLookupService.getBookingRoomEntityById(request.getBookingRoomId());
        if (request.getBookingRoomId() != null && bookingRoom == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Liên kết phòng đặt không hợp lệ");
        }
        if (bookingRoom == null && booking != null && room != null) {
            bookingRoom = bookingLookupService.findBookingRoomByBookingIdAndRoomInstanceId(booking.getId(), room.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Phòng vật lý không thuộc đơn đặt phòng"));
        }
        var associations = StayGuestAssociationValidator.validate(hotel.getId(), booking, bookingRoom, room, request.getRoomNumber());
        booking = associations.booking(); bookingRoom = associations.bookingRoom(); room = associations.room();

        // Kiểm tra xem đây có phải là retry của cùng một lượt lưu trú đang diễn ra (chưa checkout) hay không:
        // 1. Nếu có bookingId: kiểm tra cùng hotelId, bookingId, documentNumber và actualCheckOutTime == null
        // 2. Nếu không có bookingId (khách vãng lai): kiểm tra cùng hotelId, roomNumber, documentNumber và actualCheckOutTime == null
        Optional<StayGuest> existingActiveStay = Optional.empty();
        if (booking != null) {
            existingActiveStay = stayGuestRepository
                    .findFirstByHotelIdAndBookingIdAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                            hotel.getId(), booking.getId(), request.getDocumentNumber().trim());
        } else if (StringUtils.hasText(request.getRoomNumber())) {
            existingActiveStay = stayGuestRepository
                    .findFirstByHotelIdAndRoomNumberAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                            hotel.getId(), request.getRoomNumber().trim(), request.getDocumentNumber().trim());
        }

        StayGuest guest;
        if (existingActiveStay.isPresent()) {
            // Cùng một lượt check-in đang hoạt động -> cập nhật thông tin (idempotent retry)
            guest = existingActiveStay.get();
            LodgingQueue existingQueue = lockQueueAndRefresh(guest);
            StayGuest candidate = LodgingExportStateGuard.copy(guest);
            candidate.setFullName(request.getFullName().trim());
            candidate.setDateOfBirth(request.getDateOfBirth());
            candidate.setGender(request.getGender());
            candidate.setNationality(StringUtils.hasText(request.getNationality()) ? request.getNationality().trim() : "Việt Nam");
            candidate.setDocumentType(request.getDocumentType());
            candidate.setDocumentNumber(request.getDocumentNumber().trim());
            candidate.setPermanentAddress(request.getPermanentAddress());
            candidate.setCurrentAddress(request.getCurrentAddress());
            candidate.setCheckInTime(request.getCheckInTime());
            candidate.setExpectedCheckOutTime(request.getExpectedCheckOutTime());
            candidate.setRoomNumber(request.getRoomNumber());
            candidate.setReasonForStay(request.getReasonForStay());
            candidate.setDocumentImageUrl(request.getDocumentImageUrl());
            if (booking != null) candidate.setBooking(booking);
            if (room != null) candidate.setRoom(room);
            if (bookingRoom != null) candidate.setBookingRoom(bookingRoom);
            candidate.setRoomNumber(associations.roomNumber());
            LodgingExportStateGuard.verifyChange(existingQueue, guest, candidate);
            if (existingQueue != null && existingQueue.getStatus() == LodgingQueueStatus.EXPORTED
                    && LodgingExportStateGuard.sameMutableValues(guest, candidate)) return stayGuestMapper.toResponse(guest);
            StayGuestTimeValidator.validate(candidate);
            org.springframework.beans.BeanUtils.copyProperties(candidate, guest);
        } else {
            // Lượt lưu trú mới -> luôn tạo mới bản ghi StayGuest, bảo toàn lịch sử lưu trú trước đó
            guest = stayGuestMapper.toEntity(request);
            guest.setHotel(hotel);
            guest.setBooking(booking);
            guest.setRoom(room);
            guest.setBookingRoom(bookingRoom);
            StayGuestTimeValidator.validate(guest);
        }

        guest.setRoomNumber(associations.roomNumber());
        StayGuest savedGuest = stayGuestRepository.save(guest);

        // Đánh giá tính đầy đủ theo tiêu chuẩn BCA để đẩy vào LodgingQueue
        syncLodgingQueue(savedGuest);

        return stayGuestMapper.toResponse(savedGuest);
    }

    @Override
    @Transactional
    public StayGuestResponse update(Long id, StayGuestUpdateRequest request) {
        StayGuest guest = stayGuestRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.STAY_GUEST_NOT_FOUND));

        validateStaffScope(guest.getHotel().getId());

        LodgingQueue existingQueue = lockQueueAndRefresh(guest);
        BookingRoom bookingRoom = request.getBookingRoomId() == null ? guest.getBookingRoom()
                : bookingLookupService.getBookingRoomEntityById(request.getBookingRoomId());
        if (request.getBookingRoomId() != null && bookingRoom == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Liên kết phòng đặt không hợp lệ");
        }
        RoomInstance room = request.getRoomId() == null
                ? (request.getBookingRoomId() == null ? guest.getRoom() : bookingRoom.getRoomInstance())
                : roomInstanceRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy phòng vật lý"));
        String number = request.getRoomNumber() != null ? request.getRoomNumber()
                : ((request.getRoomId() != null || request.getBookingRoomId() != null) && room != null ? room.getRoomNumber() : guest.getRoomNumber());
        var associations = StayGuestAssociationValidator.validate(guest.getHotel().getId(), guest.getBooking(), bookingRoom, room, number);
        StayGuest candidate = LodgingExportStateGuard.copy(guest);
        stayGuestMapper.updateEntityFromDto(request, candidate);
        // Validate legacy associations but do not backfill redundant keys without an association patch.
        if (request.getBookingRoomId() != null || request.getRoomId() != null) {
            candidate.setBooking(associations.booking()); candidate.setBookingRoom(associations.bookingRoom());
            candidate.setRoom(associations.room());
        }
        candidate.setRoomNumber(associations.roomNumber());
        LodgingExportStateGuard.verifyChange(existingQueue, guest, candidate);
        if (existingQueue != null && existingQueue.getStatus() == LodgingQueueStatus.EXPORTED
                && LodgingExportStateGuard.sameMutableValues(guest, candidate)) return stayGuestMapper.toResponse(guest);
        if (request.getExpectedCheckOutTime() != null || request.getActualCheckOutTime() != null) {
            StayGuestTimeValidator.validate(candidate);
        }
        org.springframework.beans.BeanUtils.copyProperties(candidate, guest);
        StayGuest savedGuest = stayGuestRepository.save(guest);

        // Cập nhật lại trạng thái queue nếu trước đó bị thiếu trường
        syncLodgingQueue(savedGuest);

        return stayGuestMapper.toResponse(savedGuest);
    }

    @Override
    public PageResponse<StayGuestResponse> search(StayGuestSearchDto request) {
        Short effectiveHotelId = resolveEffectiveHotelId(request.getHotelId());
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize(), Sort.by("checkInTime").descending());

        Page<StayGuest> page = stayGuestRepository.search(
                effectiveHotelId,
                request.getBookingId(),
                request.getRoomNumber(),
                request.getDocumentNumber(),
                request.getKeyword(),
                request.getFromDate(),
                request.getToDate(),
                pageable
        );

        return PageResponse.from(page.map(stayGuestMapper::toResponse));
    }

    @Override
    public StayGuestResponse getById(Long id) {
        StayGuest guest = stayGuestRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.STAY_GUEST_NOT_FOUND));

        validateStaffScope(guest.getHotel().getId());
        return stayGuestMapper.toResponse(guest);
    }

    @Override
    public List<StayGuestResponse> getByBookingRoomId(Long bookingRoomId) {
        if (bookingRoomId == null) {
            return List.of();
        }
        BookingRoom bookingRoom = bookingLookupService.getBookingRoomEntityById(bookingRoomId);
        if (bookingRoom == null || bookingRoom.getBookingDetail() == null || bookingRoom.getBookingDetail().getBooking() == null
                || bookingRoom.getBookingDetail().getBooking().getHotel() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Liên kết phòng đặt không hợp lệ");
        }
        Short hotelId = bookingRoom.getBookingDetail().getBooking().getHotel().getId();
        validateStaffScope(hotelId);
        List<StayGuest> list = stayGuestRepository.findByBookingRoomIdAndHotelIdAndIsDeletedFalse(bookingRoomId, hotelId);
        return list.stream()
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .map(stayGuestMapper::toResponse)
                .toList();
    }

    @Override
    public StayGuest getEntityById(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID khách lưu trú không được để trống");
        }
        return stayGuestRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND, "Không tìm thấy khách lưu trú ID: " + id));
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<StayGuest> guests = stayGuestRepository.findAllByIdInAndIsDeletedFalse(ids);
        for (StayGuest guest : guests) {
            validateStaffScope(guest.getHotel().getId());
            guest.setIsDeleted(true);

            // Cập nhật hàng chờ sang CANCELLED
            lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(guest.getId())
                    .ifPresent(queue -> {
                        queue.setStatus(LodgingQueueStatus.CANCELLED);
                        queue.setIsDeleted(true);
                        lodgingQueueRepository.save(queue);
                    });
        }
        stayGuestRepository.saveAll(guests);
    }

    @Override
    @Transactional
    public void syncLodgingQueueForBookingRoom(Long bookingRoomId) {
        if (bookingRoomId == null) {
            return;
        }
        List<StayGuest> guests = stayGuestRepository.findByBookingRoomId(bookingRoomId);
        OffsetDateTime now = OffsetDateTime.now();
        for (StayGuest guest : guests) {
            if (!Boolean.TRUE.equals(guest.getIsDeleted())) {
                LodgingQueue queue = lockQueueAndRefresh(guest);
                if (queue != null && LodgingExportStateGuard.preservesQueue(queue)) continue;
                if (queue != null && queue.getStatus() == LodgingQueueStatus.PROCESSING) throw LodgingExportStateGuard.stateConflict();
                if (guest.getCheckInTime() == null) {
                    StayGuest candidate = LodgingExportStateGuard.copy(guest);
                    candidate.setCheckInTime(now);
                    StayGuestTimeValidator.validate(candidate);
                    guest.setCheckInTime(now);
                }
                StayGuest saved = stayGuestRepository.save(guest);
                syncLodgingQueue(saved);
            }
        }
    }

    @Override
    @Transactional
    public void cancelLodgingQueueForBookingRoom(Long bookingRoomId) {
        if (bookingRoomId == null) {
            return;
        }
        List<StayGuest> guests = stayGuestRepository.findByBookingRoomId(bookingRoomId);
        for (StayGuest guest : guests) {
            lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(guest.getId())
                    .ifPresent(queue -> {
                        queue.setStatus(LodgingQueueStatus.CANCELLED);
                        lodgingQueueRepository.save(queue);
                    });
        }
    }

    private LodgingQueue lockQueueAndRefresh(StayGuest guest) {
        LodgingQueue queue = lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(guest.getId()).orElse(null);
        if (queue != null) entityManager.refresh(guest);
        return queue;
    }

    private void syncLodgingQueue(StayGuest guest) {
        List<String> missingFields = validateBcaFields(guest);

        LodgingQueue queue = lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(guest.getId())
                .orElseGet(() -> LodgingQueue.builder()
                        .stayGuest(guest)
                        .hotel(guest.getHotel())
                        .build());

        if (LodgingExportStateGuard.preservesQueue(queue)) return;
        if (queue.getStatus() == LodgingQueueStatus.PROCESSING) throw LodgingExportStateGuard.stateConflict();
        if (missingFields.isEmpty()) {
            queue.setStatus(LodgingQueueStatus.PENDING);
            queue.setErrorMessage(null);
        } else {
            queue.setStatus(LodgingQueueStatus.ERROR);
            queue.setErrorMessage("Thiếu thông tin chuẩn BCA: " + String.join(", ", missingFields));
        }

        lodgingQueueRepository.save(queue);
    }

    private List<String> validateBcaFields(StayGuest guest) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(guest.getFullName())) missing.add("Họ và tên");
        if (guest.getDateOfBirth() == null) missing.add("Ngày tháng năm sinh");
        if (guest.getGender() == null) missing.add("Giới tính");
        if (!StringUtils.hasText(guest.getNationality())) missing.add("Quốc tịch");
        if (guest.getDocumentType() == null) missing.add("Loại giấy tờ");
        if (!StringUtils.hasText(guest.getDocumentNumber())) missing.add("Số giấy tờ");
        if (guest.getCheckInTime() == null) missing.add("Thời gian đến");
        if (guest.getExpectedCheckOutTime() == null) missing.add("Thời gian đi dự kiến");
        if (!StringUtils.hasText(guest.getRoomNumber())) missing.add("Số phòng");

        boolean isVietnamese = "Việt Nam".equalsIgnoreCase(guest.getNationality())
                || "Viet Nam".equalsIgnoreCase(guest.getNationality())
                || "Vietnam".equalsIgnoreCase(guest.getNationality());

        if (isVietnamese && !StringUtils.hasText(guest.getPermanentAddress())) {
            missing.add("Nơi ĐKTT / Nơi cấp");
        }

        return missing;
    }

    private void validateStaffScope(Short hotelId) {
        if (hotelId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "hotelId không được để trống");
        }

        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN")) {
            return;
        }

        if (SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") || SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Short staffHotelId = resolvePropertyHotelId();
            if (!staffHotelId.equals(hotelId)) {
                String roleTitle = SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") ? "Quản lý khách sạn" : "Lễ tân";
                throw new AppException(ErrorCode.UNAUTHORIZED, roleTitle + " không có quyền thao tác trên khách sạn khác");
            }
            return;
        }

        throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thao tác trên khách sạn này");
    }

    private Short resolveEffectiveHotelId(Short requestedHotelId) {
        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN")) {
            return requestedHotelId;
        }

        if (SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") || SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Short staffHotelId = resolvePropertyHotelId();
            if (requestedHotelId != null && !requestedHotelId.equals(staffHotelId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Không được phép truy vấn dữ liệu của khách sạn khác");
            }
            return staffHotelId;
        }

        throw new AppException(ErrorCode.UNAUTHORIZED, "Không xác định được phạm vi khách sạn được phép truy cập");
    }

    private Short resolvePropertyHotelId() {
        return OperationStaffScopeResolver.requireScope(resolveCurrentStaff(), "PROPERTY");
    }

    private Staff resolveCurrentStaff() {
        return OperationStaffScopeResolver.resolve(staffRepository);
    }
}
