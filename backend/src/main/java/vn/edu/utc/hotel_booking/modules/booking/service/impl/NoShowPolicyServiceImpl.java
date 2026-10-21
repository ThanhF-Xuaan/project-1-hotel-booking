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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.NoShowPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;
import vn.edu.utc.hotel_booking.modules.booking.mapper.PolicyMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.NoShowPolicyRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.booking.service.NoShowPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideQueryService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NoShowPolicyServiceImpl implements NoShowPolicyService {

    private final NoShowPolicyRepository noShowPolicyRepository;
    private final BookingLookupService bookingLookupService;
    private final PropertyPolicyOverrideQueryService propertyPolicyOverrideQueryService;
    private final PolicyMapper policyMapper;

    @Override
    public NoShowPolicyResponse create(NoShowPolicyCreateRequest request) {
        if (noShowPolicyRepository.existsByPolicyCodeAndIsDeletedFalse(request.getPolicyCode())) {
            throw new AppException(ErrorCode.NO_SHOW_POLICY_CODE_EXISTS);
        }

        NoShowPolicy policy = policyMapper.toEntity(request);
        policy.setIsDeleted(false);
        NoShowPolicy saved = noShowPolicyRepository.save(policy);
        return policyMapper.toResponse(saved);
    }

    @Override
    public NoShowPolicyResponse update(Integer id, NoShowPolicyUpdateRequest request) {
        NoShowPolicy policy = noShowPolicyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.NO_SHOW_POLICY_NOT_FOUND));

        policy.setName(request.getName());
        policy.setBookingType(request.getBookingType());
        policy.setDescription(request.getDescription());
        policy.setReleaseRoomTime(request.getReleaseRoomTime());
        policy.setPenaltyType(request.getPenaltyType());
        policy.setPenaltyValue(request.getPenaltyValue());
        if (request.getIsActive() != null) {
            policy.setIsActive(request.getIsActive());
        }

        NoShowPolicy updated = noShowPolicyRepository.save(policy);
        return policyMapper.toResponse(updated);
    }

    @Override
    public void delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Integer id : ids) {
            NoShowPolicy policy = noShowPolicyRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new AppException(ErrorCode.NO_SHOW_POLICY_NOT_FOUND));

            // Ràng buộc RESTRICT: Kiểm tra xem chính sách có đang được booking hoặc override tham chiếu không
            if (bookingLookupService.existsBookingDetailByNoShowPolicyId(id) ||
                    propertyPolicyOverrideQueryService.existsByNoShowPolicyId(id)) {
                throw new AppException(ErrorCode.POLICY_IN_USE);
            }

            policy.setIsDeleted(true);
            policy.setIsActive(false);
            noShowPolicyRepository.save(policy);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NoShowPolicyResponse getById(Integer id) {
        NoShowPolicy policy = getEntityById(id);
        return policyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public NoShowPolicy getEntityById(Integer id) {
        return noShowPolicyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.NO_SHOW_POLICY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public NoShowPolicy resolveDefaultChainPolicy(vn.edu.utc.hotel_booking.modules.booking.entity.BookingType bookingType) {
        vn.edu.utc.hotel_booking.modules.booking.entity.BookingType effectiveType =
                (bookingType != null) ? bookingType : vn.edu.utc.hotel_booking.modules.booking.entity.BookingType.FIT;
        return noShowPolicyRepository
                .findFirstByBookingTypeInAndIsActiveTrueAndIsDeletedFalseOrderByIdAsc(List.of(effectiveType, vn.edu.utc.hotel_booking.modules.booking.entity.BookingType.ALL))
                .orElseThrow(() -> new AppException(ErrorCode.NO_SHOW_POLICY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NoShowPolicyResponse> filter(NoShowPolicySearchDto searchDto) {
        Specification<NoShowPolicy> spec = (root, query, cb) -> {
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

            if (searchDto.getPenaltyType() != null) {
                predicates.add(cb.equal(root.get("penaltyType"), searchDto.getPenaltyType()));
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
        Page<NoShowPolicy> resultPage = noShowPolicyRepository.findAll(spec, pageable);

        return PageResponse.from(resultPage.map(policyMapper::toResponse));
    }
}
