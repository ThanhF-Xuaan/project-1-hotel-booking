package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.PropertyPolicyOverrideResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationFeeType;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.PropertyPolicyOverride;
import vn.edu.utc.hotel_booking.modules.booking.mapper.PolicyMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.PropertyPolicyOverrideRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.CancellationPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.NoShowPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideQueryService;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PropertyPolicyOverrideServiceImpl implements PropertyPolicyOverrideService {

    private final PropertyPolicyOverrideRepository propertyPolicyOverrideRepository;
    private final HotelService hotelService;
    private final HotelRoomTypeService hotelRoomTypeService;
    private final CancellationPolicyService cancellationPolicyService;
    private final NoShowPolicyService noShowPolicyService;
    private final PropertyPolicyOverrideQueryService propertyPolicyOverrideQueryService;
    private final PolicyMapper policyMapper;

    @Override
    public PropertyPolicyOverrideResponse create(PropertyPolicyOverrideCreateRequest request) {
        Hotel hotel = hotelService.getEntityById(request.getHotelId());

        HotelRoomType hotelRoomType = null;
        if (request.getHotelRoomTypeId() != null) {
            hotelRoomType = hotelRoomTypeService.getEntityById(request.getHotelRoomTypeId());

            // Khóa ngoại kép / logic xác thực: Hạng phòng phải thuộc đúng khách sạn
            if (!hotelRoomType.getHotel().getId().equals(request.getHotelId())) {
                throw new AppException(ErrorCode.ROOM_TYPE_NOT_BELONG_TO_HOTEL);
            }
        }

        // Kiểm tra trùng lặp cấu hình active
        if (request.getIsActive() != null && request.getIsActive()) {
            if (hotelRoomType != null) {
                if (propertyPolicyOverrideRepository.existsByHotelIdAndHotelRoomTypeIdAndIsActiveTrueAndIsDeletedFalse(
                        request.getHotelId(), request.getHotelRoomTypeId())) {
                    throw new AppException(ErrorCode.PROPERTY_POLICY_OVERRIDE_EXISTS);
                }
            } else {
                if (propertyPolicyOverrideRepository.existsByHotelIdAndHotelRoomTypeIsNullAndIsActiveTrueAndIsDeletedFalse(
                        request.getHotelId())) {
                    throw new AppException(ErrorCode.PROPERTY_POLICY_OVERRIDE_EXISTS);
                }
            }
        }

        CancellationPolicy cancellationPolicy = null;
        if (request.getCancellationPolicyId() != null) {
            cancellationPolicy = cancellationPolicyService.getEntityById(request.getCancellationPolicyId());

            // Ràng buộc chống mâu thuẫn: Không cho phép override free days với policy TIERED
            if (cancellationPolicy.getCancellationFeeType() == CancellationFeeType.TIERED &&
                    request.getFreeCancellationBeforeDaysOverride() != null) {
                throw new AppException(ErrorCode.CANNOT_OVERRIDE_TIERED_POLICY_DAYS);
            }
        }

        NoShowPolicy noShowPolicy = null;
        if (request.getNoShowPolicyId() != null) {
            noShowPolicy = noShowPolicyService.getEntityById(request.getNoShowPolicyId());
        }

        PropertyPolicyOverride override = PropertyPolicyOverride.builder()
                .hotel(hotel)
                .hotelRoomType(hotelRoomType)
                .cancellationPolicy(cancellationPolicy)
                .noShowPolicy(noShowPolicy)
                .freeCancellationBeforeDaysOverride(request.getFreeCancellationBeforeDaysOverride())
                .noShowReleaseTimeOverride(request.getNoShowReleaseTimeOverride())
                .overrideReason(request.getOverrideReason())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        override.setIsDeleted(false);

        PropertyPolicyOverride saved = propertyPolicyOverrideRepository.save(override);
        return policyMapper.toResponse(saved);
    }

    @Override
    public PropertyPolicyOverrideResponse update(Integer id, PropertyPolicyOverrideUpdateRequest request) {
        PropertyPolicyOverride override = propertyPolicyOverrideRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_POLICY_OVERRIDE_NOT_FOUND));

        CancellationPolicy cancellationPolicy = null;
        if (request.getCancellationPolicyId() != null) {
            cancellationPolicy = cancellationPolicyService.getEntityById(request.getCancellationPolicyId());

            if (cancellationPolicy.getCancellationFeeType() == CancellationFeeType.TIERED &&
                    request.getFreeCancellationBeforeDaysOverride() != null) {
                throw new AppException(ErrorCode.CANNOT_OVERRIDE_TIERED_POLICY_DAYS);
            }
        }

        NoShowPolicy noShowPolicy = null;
        if (request.getNoShowPolicyId() != null) {
            noShowPolicy = noShowPolicyService.getEntityById(request.getNoShowPolicyId());
        }

        // Kiểm tra trùng lặp nếu bật lại active
        if (request.getIsActive() != null && request.getIsActive() && !Boolean.TRUE.equals(override.getIsActive())) {
            Short hotelId = override.getHotel().getId();
            if (override.getHotelRoomType() != null) {
                if (propertyPolicyOverrideRepository.existsByHotelIdAndHotelRoomTypeIdAndIsActiveTrueAndIsDeletedFalseAndIdNot(
                        hotelId, override.getHotelRoomType().getId(), id)) {
                    throw new AppException(ErrorCode.PROPERTY_POLICY_OVERRIDE_EXISTS);
                }
            } else {
                if (propertyPolicyOverrideRepository.existsByHotelIdAndHotelRoomTypeIsNullAndIsActiveTrueAndIsDeletedFalseAndIdNot(
                        hotelId, id)) {
                    throw new AppException(ErrorCode.PROPERTY_POLICY_OVERRIDE_EXISTS);
                }
            }
        }

        override.setCancellationPolicy(cancellationPolicy);
        override.setNoShowPolicy(noShowPolicy);
        override.setFreeCancellationBeforeDaysOverride(request.getFreeCancellationBeforeDaysOverride());
        override.setNoShowReleaseTimeOverride(request.getNoShowReleaseTimeOverride());
        override.setOverrideReason(request.getOverrideReason());
        if (request.getIsActive() != null) {
            override.setIsActive(request.getIsActive());
        }

        PropertyPolicyOverride updated = propertyPolicyOverrideRepository.save(override);
        return policyMapper.toResponse(updated);
    }

    @Override
    public void delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Integer id : ids) {
            PropertyPolicyOverride override = propertyPolicyOverrideRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_POLICY_OVERRIDE_NOT_FOUND));

            override.setIsDeleted(true);
            override.setIsActive(false);
            propertyPolicyOverrideRepository.save(override);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyPolicyOverrideResponse getById(Integer id) {
        PropertyPolicyOverride override = getEntityById(id);
        return policyMapper.toResponse(override);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyPolicyOverride getEntityById(Integer id) {
        return propertyPolicyOverrideRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_POLICY_OVERRIDE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<PropertyPolicyOverride> findActiveOverride(Short hotelId, Integer hotelRoomTypeId) {
        if (hotelId == null) {
            return java.util.Optional.empty();
        }
        if (hotelRoomTypeId != null) {
            java.util.Optional<PropertyPolicyOverride> roomTypeOverride = propertyPolicyOverrideRepository
                    .findByHotelIdAndHotelRoomTypeIdAndIsActiveTrueAndIsDeletedFalse(hotelId, hotelRoomTypeId);
            if (roomTypeOverride.isPresent()) {
                return roomTypeOverride;
            }
        }
        return propertyPolicyOverrideRepository
                .findByHotelIdAndHotelRoomTypeIsNullAndIsActiveTrueAndIsDeletedFalse(hotelId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PropertyPolicyOverrideResponse> filter(PropertyPolicyOverrideSearchDto searchDto) {
        Specification<PropertyPolicyOverride> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (searchDto.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), searchDto.getHotelId()));
            }

            if (searchDto.getHotelRoomTypeId() != null) {
                predicates.add(cb.equal(root.get("hotelRoomType").get("id"), searchDto.getHotelRoomTypeId()));
            }

            if (searchDto.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), searchDto.getIsActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = "ASC".equalsIgnoreCase(searchDto.getSortDirection())
                ? Sort.by(searchDto.getSortBy()).ascending()
                : Sort.by(searchDto.getSortBy()).descending();

        Pageable pageable = PageRequest.of(searchDto.getPage(), searchDto.getPageSize(), sort);
        Page<PropertyPolicyOverride> resultPage = propertyPolicyOverrideRepository.findAll(spec, pageable);

        return PageResponse.from(resultPage.map(policyMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCancellationPolicyId(Integer cancellationPolicyId) {
        return propertyPolicyOverrideQueryService.existsByCancellationPolicyId(cancellationPolicyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNoShowPolicyId(Integer noShowPolicyId) {
        return propertyPolicyOverrideQueryService.existsByNoShowPolicyId(noShowPolicyId);
    }
}
