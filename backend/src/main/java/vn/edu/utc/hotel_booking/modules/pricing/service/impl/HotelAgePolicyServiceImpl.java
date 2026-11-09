package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HotelAgePolicyResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HotelAgePolicy;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.HotelAgePolicyMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.HotelAgePolicyRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.HotelAgePolicyService;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class HotelAgePolicyServiceImpl implements HotelAgePolicyService {

    HotelAgePolicyRepository hotelAgePolicyRepository;
    HotelService hotelService;
    HotelAgePolicyMapper hotelAgePolicyMapper;

    @Override
    public HotelAgePolicyResponse getById(Short id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID chính sách độ tuổi không được để trống");
        }
        HotelAgePolicy policy = hotelAgePolicyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_AGE_POLICY_NOT_FOUND, "Không tìm thấy chính sách độ tuổi ID: " + id));
        return hotelAgePolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional
    public HotelAgePolicyResponse create(HotelAgePolicyCreateRequest request) {
        if (request.getMinAge() > request.getMaxAge()) {
            throw new AppException(ErrorCode.HOTEL_AGE_POLICY_RANGE_INVALID, "Độ tuổi tối thiểu không được lớn hơn độ tuổi tối đa");
        }

        Hotel hotel = null;
        if (request.getHotelId() != null && request.getHotelId() > 0) {
            hotel = hotelService.getEntityById(request.getHotelId());
            if (hotelAgePolicyRepository.existsByHotelIdAndGuestTypeAndIsDeletedFalse(request.getHotelId(), request.getGuestType())) {
                throw new AppException(ErrorCode.HOTEL_AGE_POLICY_ALREADY_EXISTS,
                        "Chính sách cho loại khách '" + request.getGuestType() + "' đã tồn tại tại cơ sở ID: " + request.getHotelId());
            }
        } else {
            if (hotelAgePolicyRepository.existsByHotelIsNullAndGuestTypeAndIsDeletedFalse(request.getGuestType())) {
                throw new AppException(ErrorCode.HOTEL_AGE_POLICY_ALREADY_EXISTS,
                        "Chính sách mặc định toàn chuỗi cho loại khách '" + request.getGuestType() + "' đã tồn tại");
            }
        }

        validateNoAgeOverlap(request.getHotelId(), null, request.getMinAge(), request.getMaxAge(), request.getGuestType());

        HotelAgePolicy entity = hotelAgePolicyMapper.toEntity(request);
        entity.setHotel(hotel);
        entity.setStatus("ACTIVE");
        entity.setIsDeleted(false);

        HotelAgePolicy saved = hotelAgePolicyRepository.save(entity);
        return hotelAgePolicyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HotelAgePolicyResponse update(Short id, HotelAgePolicyUpdateRequest request) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID chính sách độ tuổi không được để trống");
        }
        if (request.getMinAge() > request.getMaxAge()) {
            throw new AppException(ErrorCode.HOTEL_AGE_POLICY_RANGE_INVALID, "Độ tuổi tối thiểu không được lớn hơn độ tuổi tối đa");
        }

        HotelAgePolicy entity = hotelAgePolicyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_AGE_POLICY_NOT_FOUND, "Không tìm thấy chính sách độ tuổi ID: " + id));

        Short hotelId = entity.getHotel() != null ? entity.getHotel().getId() : null;
        String newStatus = StringUtils.hasText(request.getStatus()) ? request.getStatus() : entity.getStatus();
        if ("ACTIVE".equalsIgnoreCase(newStatus)) {
            validateNoAgeOverlap(hotelId, id, request.getMinAge(), request.getMaxAge(), entity.getGuestType());
        }

        hotelAgePolicyMapper.updateEntityFromRequest(entity, request);
        entity.setUpdatedAt(OffsetDateTime.now());

        HotelAgePolicy updated = hotelAgePolicyRepository.save(entity);
        return hotelAgePolicyMapper.toResponse(updated);
    }

    @Override
    public PageResponse<HotelAgePolicyResponse> filter(HotelAgePolicySearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "minAge";
        String sortDir = StringUtils.hasText(searchDto.getSortDirection()) ? searchDto.getSortDirection() : "ASC";

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        String guestType = StringUtils.hasText(searchDto.getGuestType()) ? searchDto.getGuestType().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<HotelAgePolicy> resultPage = hotelAgePolicyRepository.filter(
                searchDto.getHotelId(),
                guestType,
                status,
                searchDto.getChainOnly(),
                pageable
        );

        return PageResponse.<HotelAgePolicyResponse>builder()
                .content(hotelAgePolicyMapper.toResponseList(resultPage.getContent()))
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .first(resultPage.isFirst())
                .last(resultPage.isLast())
                .empty(resultPage.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public void deleteBatch(List<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Short id : ids) {
            HotelAgePolicy entity = hotelAgePolicyRepository.findByIdAndIsDeletedFalse(id).orElse(null);
            if (entity != null) {
                entity.setIsDeleted(true);
                entity.setUpdatedAt(OffsetDateTime.now());
                hotelAgePolicyRepository.save(entity);
            }
        }
    }

    @Override
    public List<HotelAgePolicyResponse> getEffectivePoliciesForHotel(Short hotelId) {
        List<HotelAgePolicy> candidates = hotelAgePolicyRepository.findCandidatesForHotel(hotelId);

        // Fallback Map: GuestType -> Policy (ưu tiên bản ghi của hotelId trước bản ghi chain-wide)
        Map<String, HotelAgePolicy> effectiveMap = new LinkedHashMap<>();
        for (HotelAgePolicy p : candidates) {
            if (!effectiveMap.containsKey(p.getGuestType())) {
                effectiveMap.put(p.getGuestType(), p);
            } else if (p.getHotel() != null && hotelId != null && p.getHotel().getId().equals(hotelId)) {
                // Ghi đè nếu bản ghi này của chính cơ sở
                effectiveMap.put(p.getGuestType(), p);
            }
        }

        return hotelAgePolicyMapper.toResponseList(new ArrayList<>(effectiveMap.values()));
    }

    @Override
    public String resolveGuestTypeByAge(Short hotelId, int age) {
        List<HotelAgePolicyResponse> effectivePolicies = getEffectivePoliciesForHotel(hotelId);
        for (HotelAgePolicyResponse policy : effectivePolicies) {
            if (age >= policy.getMinAge() && age <= policy.getMaxAge()) {
                return policy.getGuestType();
            }
        }
        // Quy ước chuẩn: nếu tuổi >= 12 hoặc không khớp dải nào thì tính là ADULT
        return age < 6 ? "INFANT" : (age < 12 ? "CHILD" : "ADULT");
    }

    private void validateNoAgeOverlap(Short hotelId, Short excludeId, short minAge, short maxAge, String guestType) {
        List<HotelAgePolicy> existing = hotelAgePolicyRepository.findActivePoliciesByScope(hotelId, excludeId);
        for (HotelAgePolicy other : existing) {
            if (!other.getGuestType().equalsIgnoreCase(guestType)) {
                // Kiểm tra xem dải [minAge, maxAge] có giao với [other.minAge, other.maxAge] hay không
                if (Math.max(minAge, other.getMinAge()) <= Math.min(maxAge, other.getMaxAge())) {
                    throw new AppException(ErrorCode.HOTEL_AGE_POLICY_RANGE_INVALID,
                            "Dải tuổi (" + minAge + " - " + maxAge + ") bị chồng lấn với chính sách của loại khách '" +
                                    other.getGuestType() + "' (" + other.getMinAge() + " - " + other.getMaxAge() + ")");
                }
            }
        }
    }
}

