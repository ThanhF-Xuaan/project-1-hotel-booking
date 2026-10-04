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
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingQueueSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.LodgingQueueResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.LodgingQueueMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.LodgingQueueService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LodgingQueueServiceImpl implements LodgingQueueService {

    private final LodgingQueueRepository repository;
    private final StaffRepository staffRepository;
    private final LodgingQueueMapper mapper;

    @Override
    public PageResponse<LodgingQueueResponse> search(LodgingQueueSearchDto request) {
        Short effectiveHotelId = resolveEffectiveHotelId(request.getHotelId());
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize(), Sort.by("createdAt").descending());

        Page<LodgingQueue> page = repository.search(
                effectiveHotelId,
                request.getStatus(),
                request.getFromDate(),
                request.getToDate(),
                pageable
        );

        return PageResponse.from(page.map(mapper::toResponse));
    }

    @Override
    @Transactional
    public LodgingQueueResponse retryValidate(Long queueId) {
        LodgingQueue queue = repository.findById(queueId)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.LODGING_QUEUE_NOT_FOUND));

        validateStaffScope(queue.getHotel().getId());

        StayGuest guest = queue.getStayGuest();
        List<String> missingFields = validateBcaFields(guest);

        if (missingFields.isEmpty()) {
            queue.setStatus(LodgingQueueStatus.PENDING);
            queue.setErrorMessage(null);
        } else {
            queue.setStatus(LodgingQueueStatus.ERROR);
            queue.setErrorMessage("Thiếu thông tin chuẩn BCA: " + String.join(", ", missingFields));
        }

        LodgingQueue saved = repository.save(queue);
        return mapper.toResponse(saved);
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
