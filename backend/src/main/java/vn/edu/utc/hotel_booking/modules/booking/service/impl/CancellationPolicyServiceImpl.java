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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CancellationPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;
import vn.edu.utc.hotel_booking.modules.booking.mapper.PolicyMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.CancellationPolicyRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.booking.service.CancellationPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideQueryService;
import vn.edu.utc.hotel_booking.modules.booking.validator.PolicyTierValidator;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CancellationPolicyServiceImpl implements CancellationPolicyService {

    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final BookingLookupService bookingLookupService;
    private final PropertyPolicyOverrideQueryService propertyPolicyOverrideQueryService;
    private final PolicyMapper policyMapper;
    private final PolicyTierValidator policyTierValidator;

    @Override
    public CancellationPolicyResponse create(CancellationPolicyCreateRequest request) {
        if (cancellationPolicyRepository.existsByPolicyCodeAndIsDeletedFalse(request.getPolicyCode())) {
            throw new AppException(ErrorCode.CANCELLATION_POLICY_CODE_EXISTS);
        }

        policyTierValidator.validateTiers(request.getCancellationFeeType(), request.getTiers());

        CancellationPolicy policy = policyMapper.toEntity(request);
        policy.setIsDeleted(false);
        CancellationPolicy saved = cancellationPolicyRepository.save(policy);
        return policyMapper.toResponse(saved);
    }

    @Override
    public CancellationPolicyResponse update(Integer id, CancellationPolicyUpdateRequest request) {
        CancellationPolicy policy = cancellationPolicyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_POLICY_NOT_FOUND));

        policyTierValidator.validateTiers(request.getCancellationFeeType(), request.getTiers());

        policy.setName(request.getName());
        policy.setBookingType(request.getBookingType());
        policy.setDescription(request.getDescription());
        policy.setFreeCancellationBeforeDays(request.getFreeCancellationBeforeDays());
        policy.setCancellationFeeType(request.getCancellationFeeType());
        policy.setDefaultPenaltyPercent(request.getDefaultPenaltyPercent());
        policy.setTiers(policyMapper.tiersToJson(request.getTiers()));
        if (request.getIsActive() != null) {
            policy.setIsActive(request.getIsActive());
        }

        CancellationPolicy updated = cancellationPolicyRepository.save(policy);
        return policyMapper.toResponse(updated);
    }

    @Override
    public void delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Integer id : ids) {
            CancellationPolicy policy = cancellationPolicyRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_POLICY_NOT_FOUND));

            // Ràng buộc RESTRICT: Kiểm tra xem chính sách có đang được booking hoặc override tham chiếu không
            if (bookingLookupService.existsBookingDetailByCancellationPolicyId(id) ||
                    propertyPolicyOverrideQueryService.existsByCancellationPolicyId(id)) {
                throw new AppException(ErrorCode.POLICY_IN_USE);
            }

            policy.setIsDeleted(true);
            policy.setIsActive(false);
            cancellationPolicyRepository.save(policy);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPolicyResponse getById(Integer id) {
        CancellationPolicy policy = getEntityById(id);
        return policyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPolicy getEntityById(Integer id) {
        return cancellationPolicyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_POLICY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPolicy resolveDefaultChainPolicy(vn.edu.utc.hotel_booking.modules.booking.entity.BookingType bookingType) {
        vn.edu.utc.hotel_booking.modules.booking.entity.BookingType effectiveType =
                (bookingType != null) ? bookingType : vn.edu.utc.hotel_booking.modules.booking.entity.BookingType.FIT;
        return cancellationPolicyRepository
                .findFirstByBookingTypeInAndIsActiveTrueAndIsDeletedFalseOrderByIdAsc(List.of(effectiveType, vn.edu.utc.hotel_booking.modules.booking.entity.BookingType.ALL))
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_POLICY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CancellationPolicyResponse> filter(CancellationPolicySearchDto searchDto) {
        Specification<CancellationPolicy> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (searchDto.getKeyword() != null && !searchDto.getKeyword().isBlank()) {
                String pattern = "%" + searchDto.getKeyword().trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("policyCode")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch));
            }

            if (searchDto.getBookingType() != null) {
                predicates.add(cb.equal(root.get("bookingType"), searchDto.getBookingType()));
            }

            if (searchDto.getCancellationFeeType() != null) {
                predicates.add(cb.equal(root.get("cancellationFeeType"), searchDto.getCancellationFeeType()));
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
        Page<CancellationPolicy> resultPage = cancellationPolicyRepository.findAll(spec, pageable);

        return PageResponse.from(resultPage.map(policyMapper::toResponse));
    }
}
