package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.RequiredArgsConstructor;
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
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

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

    @Override
    @Transactional
    public StayGuestResponse createOrCheckIn(StayGuestCreateRequest request) {
        validateStaffScope(request.getHotelId());

        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingLookupService.getBookingEntityById(request.getBookingId());
            if (booking.getHotel() != null && !booking.getHotel().getId().equals(hotel.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Đơn đặt phòng không thuộc khách sạn này");
            }
        }

        RoomInstance room = null;
        if (request.getRoomId() != null) {
            room = roomInstanceRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy phòng vật lý với ID " + request.getRoomId()));
            if (room.getHotel() != null && !room.getHotel().getId().equals(hotel.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Phòng vật lý không thuộc khách sạn này");
            }
        }

        BookingRoom bookingRoom = null;
        if (request.getBookingRoomId() != null) {
            bookingRoom = bookingLookupService.getBookingRoomEntityById(request.getBookingRoomId());
            if (booking != null && bookingRoom.getBookingDetail() != null
                    && bookingRoom.getBookingDetail().getBooking() != null
                    && !bookingRoom.getBookingDetail().getBooking().getId().equals(booking.getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Phòng đặt không thuộc đơn đặt phòng được chỉ định");
            }
        }

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
            guest.setFullName(request.getFullName().trim());
            guest.setDateOfBirth(request.getDateOfBirth());
            guest.setGender(request.getGender());
            guest.setNationality(StringUtils.hasText(request.getNationality()) ? request.getNationality().trim() : "Việt Nam");
            guest.setDocumentType(request.getDocumentType());
            guest.setDocumentNumber(request.getDocumentNumber().trim());
            guest.setPermanentAddress(request.getPermanentAddress());
            guest.setCurrentAddress(request.getCurrentAddress());
            guest.setCheckInTime(request.getCheckInTime());
            guest.setExpectedCheckOutTime(request.getExpectedCheckOutTime());
            guest.setRoomNumber(request.getRoomNumber());
            guest.setReasonForStay(request.getReasonForStay());
            guest.setDocumentImageUrl(request.getDocumentImageUrl());
            if (booking != null) guest.setBooking(booking);
            if (room != null) guest.setRoom(room);
            if (bookingRoom != null) guest.setBookingRoom(bookingRoom);
        } else {
            // Lượt lưu trú mới -> luôn tạo mới bản ghi StayGuest, bảo toàn lịch sử lưu trú trước đó
            guest = stayGuestMapper.toEntity(request);
            guest.setHotel(hotel);
            guest.setBooking(booking);
            guest.setRoom(room);
            guest.setBookingRoom(bookingRoom);
        }

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

        if (request.getRoomId() != null) {
            RoomInstance room = roomInstanceRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy phòng vật lý với ID " + request.getRoomId()));
            if (room.getHotel() != null && !room.getHotel().getId().equals(guest.getHotel().getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Phòng vật lý không thuộc khách sạn này");
            }
            guest.setRoom(room);
        }

        if (request.getBookingRoomId() != null) {
            BookingRoom bookingRoom = bookingLookupService.getBookingRoomEntityById(request.getBookingRoomId());
            if (guest.getBooking() != null && bookingRoom.getBookingDetail() != null
                    && bookingRoom.getBookingDetail().getBooking() != null
                    && !bookingRoom.getBookingDetail().getBooking().getId().equals(guest.getBooking().getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Phòng đặt không thuộc đơn đặt phòng được chỉ định");
            }
            guest.setBookingRoom(bookingRoom);
        }

        stayGuestMapper.updateEntityFromDto(request, guest);
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

    private void syncLodgingQueue(StayGuest guest) {
        List<String> missingFields = validateBcaFields(guest);

        LodgingQueue queue = lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(guest.getId())
                .orElseGet(() -> LodgingQueue.builder()
                        .stayGuest(guest)
                        .hotel(guest.getHotel())
                        .build());

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
            Staff currentStaff = resolveCurrentStaff();
            if ("PROPERTY".equalsIgnoreCase(currentStaff.getScopeType())
                    && currentStaff.getScopeEntityId() != null
                    && !currentStaff.getScopeEntityId().equals(hotelId.intValue())) {
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
            Staff currentStaff = resolveCurrentStaff();
            if ("PROPERTY".equalsIgnoreCase(currentStaff.getScopeType()) && currentStaff.getScopeEntityId() != null) {
                Short staffHotelId = currentStaff.getScopeEntityId().shortValue();
                if (requestedHotelId != null && !requestedHotelId.equals(staffHotelId)) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Không được phép truy vấn dữ liệu của khách sạn khác");
                }
                return staffHotelId;
            }
        }

        if (requestedHotelId != null) {
            return requestedHotelId;
        }
        throw new AppException(ErrorCode.UNAUTHORIZED, "Không xác định được phạm vi khách sạn được phép truy cập");
    }

    private Staff resolveCurrentStaff() {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(staffRepository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername()
                        .flatMap(staffRepository::findByUsernameAndIsDeletedFalse))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    }
}
