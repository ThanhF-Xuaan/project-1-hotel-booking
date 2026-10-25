package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquiryCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquirySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupInquiryResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiry;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiryRoomType;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiryStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupInquiryRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupInquiryService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupInquiryServiceImpl implements GroupInquiryService {

    GroupInquiryRepository groupInquiryRepository;
    HotelService hotelService;
    HotelRoomTypeService hotelRoomTypeService;

    @Override
    @Transactional
    public GroupInquiryResponse createInquiry(GroupInquiryCreateRequest request) {
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_DATES);
        }

        if (request.getTotalRooms() < 6) {
            throw new AppException(ErrorCode.GROUP_INQUIRY_MIN_ROOMS_INVALID);
        }

        Hotel hotel = hotelService.getEntityById(request.getHotelId());

        String inquiryNumber = generateInquiryNumber();

        GroupInquiry inquiry = GroupInquiry.builder()
                .inquiryNumber(inquiryNumber)
                .hotel(hotel)
                .companyName(request.getCompanyName().trim())
                .taxCode(StringUtils.hasText(request.getTaxCode()) ? request.getTaxCode().trim() : null)
                .contactName(request.getContactName().trim())
                .contactEmail(request.getContactEmail().trim().toLowerCase())
                .contactPhone(request.getContactPhone().trim())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalRooms(request.getTotalRooms())
                .totalGuests(request.getTotalGuests())
                .estimatedBudget(request.getEstimatedBudget())
                .specialRequests(request.getSpecialRequests())
                .status(GroupInquiryStatus.PENDING)
                .build();

        List<GroupInquiryRoomType> roomTypes = new ArrayList<>();
        if (request.getRequestedRoomTypes() != null) {
            for (GroupInquiryCreateRequest.InquiryRoomTypeItem item : request.getRequestedRoomTypes()) {
                HotelRoomType roomType = hotelRoomTypeService.getEntityById(item.getHotelRoomTypeId());

                GroupInquiryRoomType inquiryRoomType = GroupInquiryRoomType.builder()
                        .inquiry(inquiry)
                        .hotelRoomType(roomType)
                        .requestedRooms(item.getRequestedRooms())
                        .build();
                roomTypes.add(inquiryRoomType);
            }
        }
        inquiry.setRequestedRoomTypes(roomTypes);

        GroupInquiry savedInquiry = groupInquiryRepository.save(inquiry);
        return mapToResponse(savedInquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GroupInquiryResponse> filterInquiries(GroupInquirySearchDto searchDto) {
        Sort sort = Sort.by(
                "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "id"
        );
        Pageable pageable = PageRequest.of(searchDto.getPage(), searchDto.getPageSize(), sort);

        Specification<GroupInquiry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (searchDto.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), searchDto.getHotelId()));
            }
            if (StringUtils.hasText(searchDto.getInquiryNumber())) {
                predicates.add(cb.like(cb.lower(root.get("inquiryNumber")), "%" + searchDto.getInquiryNumber().toLowerCase().trim() + "%"));
            }
            if (StringUtils.hasText(searchDto.getCompanyName())) {
                predicates.add(cb.like(cb.lower(root.get("companyName")), "%" + searchDto.getCompanyName().toLowerCase().trim() + "%"));
            }
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }
            if (searchDto.getCheckInDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInDate"), searchDto.getCheckInDateFrom()));
            }
            if (searchDto.getCheckInDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkInDate"), searchDto.getCheckInDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GroupInquiry> page = groupInquiryRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupInquiryResponse getInquiryById(Long id) {
        GroupInquiry inquiry = groupInquiryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_INQUIRY_NOT_FOUND));
        return mapToResponse(inquiry);
    }

    @Override
    @Transactional
    public GroupInquiryResponse rejectInquiry(Long id, String reason) {
        GroupInquiry inquiry = groupInquiryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_INQUIRY_NOT_FOUND));

        if (inquiry.getStatus() == GroupInquiryStatus.CONVERTED) {
            throw new AppException(ErrorCode.GROUP_INQUIRY_ALREADY_PROCESSED);
        }

        inquiry.setStatus(GroupInquiryStatus.REJECTED);
        inquiry.setRejectionReason(reason);
        return mapToResponse(groupInquiryRepository.save(inquiry));
    }

    @Override
    @Transactional
    public void markQuoted(Long inquiryId, vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract contract) {
        if (inquiryId == null) {
            return;
        }
        groupInquiryRepository.findById(inquiryId).ifPresent(inq -> {
            if (inq.getStatus() != GroupInquiryStatus.CONVERTED && inq.getStatus() != GroupInquiryStatus.REJECTED) {
                inq.setStatus(GroupInquiryStatus.QUOTED);
                inq.setConvertedContract(contract);
                groupInquiryRepository.save(inq);
            }
        });
    }

    @Override
    @Transactional
    public void markConvertedByContractId(Long contractId) {
        if (contractId == null) {
            return;
        }
        groupInquiryRepository.findByConvertedContractId(contractId).ifPresent(inq -> {
            inq.setStatus(GroupInquiryStatus.CONVERTED);
            groupInquiryRepository.save(inq);
        });
    }

    private String generateInquiryNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        String code = "INQ-" + datePart + "-" + randomSuffix;
        while (groupInquiryRepository.existsByInquiryNumber(code)) {
            randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
            code = "INQ-" + datePart + "-" + randomSuffix;
        }
        return code;
    }

    private GroupInquiryResponse mapToResponse(GroupInquiry inquiry) {
        List<GroupInquiryResponse.InquiryRoomTypeResponse> roomTypeResponses = new ArrayList<>();
        if (inquiry.getRequestedRoomTypes() != null) {
            for (GroupInquiryRoomType rt : inquiry.getRequestedRoomTypes()) {
                roomTypeResponses.add(GroupInquiryResponse.InquiryRoomTypeResponse.builder()
                        .id(rt.getId())
                        .hotelRoomTypeId(rt.getHotelRoomType() != null ? rt.getHotelRoomType().getId() : null)
                        .roomTypeName(rt.getHotelRoomType() != null && rt.getHotelRoomType().getRoomType() != null
                                ? rt.getHotelRoomType().getRoomType().getName() : null)
                        .requestedRooms(rt.getRequestedRooms())
                        .estimatedRatePerNight(rt.getEstimatedRatePerNight())
                        .build());
            }
        }

        return GroupInquiryResponse.builder()
                .id(inquiry.getId())
                .inquiryNumber(inquiry.getInquiryNumber())
                .hotelId(inquiry.getHotel() != null ? inquiry.getHotel().getId() : null)
                .hotelName(inquiry.getHotel() != null ? inquiry.getHotel().getName() : null)
                .companyName(inquiry.getCompanyName())
                .taxCode(inquiry.getTaxCode())
                .contactName(inquiry.getContactName())
                .contactEmail(inquiry.getContactEmail())
                .contactPhone(inquiry.getContactPhone())
                .checkInDate(inquiry.getCheckInDate())
                .checkOutDate(inquiry.getCheckOutDate())
                .totalRooms(inquiry.getTotalRooms())
                .totalGuests(inquiry.getTotalGuests())
                .estimatedBudget(inquiry.getEstimatedBudget())
                .specialRequests(inquiry.getSpecialRequests())
                .status(inquiry.getStatus())
                .salesStaffId(inquiry.getSalesStaff() != null ? inquiry.getSalesStaff().getId() : null)
                .salesStaffName(inquiry.getSalesStaff() != null ? inquiry.getSalesStaff().getFullName() : null)
                .convertedContractId(inquiry.getConvertedContract() != null ? inquiry.getConvertedContract().getId() : null)
                .convertedContractNumber(inquiry.getConvertedContract() != null ? inquiry.getConvertedContract().getContractNumber() : null)
                .rejectionReason(inquiry.getRejectionReason())
                .createdAt(inquiry.getCreatedAt())
                .requestedRoomTypes(roomTypeResponses)
                .build();
    }
}
