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
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
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
import vn.edu.utc.hotel_booking.modules.operation.repository.StayGuestRepository;
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
    private final BookingRepository bookingRepository;
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
            booking = bookingRepository.findById(request.getBookingId()).orElse(null);
        }

        RoomInstance room = null;
        if (request.getRoomId() != null) {
            room = roomInstanceRepository.findById(request.getRoomId()).orElse(null);
        }

        // Kiểm tra xem khách có CCCD/Passport này đã từng lưu trú tại khách sạn chưa
        Optional<StayGuest> existingOpt = stayGuestRepository
                .findByHotelIdAndDocumentNumberAndIsDeletedFalse(hotel.getId(), request.getDocumentNumber().trim());

        StayGuest guest;
        if (existingOpt.isPresent()) {
            guest = existingOpt.get();
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
        } else {
            guest = stayGuestMapper.toEntity(request);
            guest.setHotel(hotel);
            guest.setBooking(booking);
            guest.setRoom(room);
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
        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN") || SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER")) {
            return;
        }

        if (SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Staff currentStaff = resolveCurrentStaff();
            if ("PROPERTY".equalsIgnoreCase(currentStaff.getScopeType())
                    && currentStaff.getScopeEntityId() != null
                    && !currentStaff.getScopeEntityId().equals(hotelId.intValue())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Lễ tân không có quyền thao tác trên khách sạn khác");
            }
        }
    }

    private Short resolveEffectiveHotelId(Short requestedHotelId) {
        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN") || SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER")) {
            return requestedHotelId;
        }

        if (SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Staff currentStaff = resolveCurrentStaff();
            if ("PROPERTY".equalsIgnoreCase(currentStaff.getScopeType()) && currentStaff.getScopeEntityId() != null) {
                return currentStaff.getScopeEntityId().shortValue();
            }
        }

        return requestedHotelId;
    }

    private Staff resolveCurrentStaff() {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(staffRepository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername()
                        .flatMap(staffRepository::findByUsernameAndIsDeletedFalse))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    }
}
