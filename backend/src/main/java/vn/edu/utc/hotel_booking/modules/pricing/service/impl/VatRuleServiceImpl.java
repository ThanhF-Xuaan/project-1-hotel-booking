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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VatRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.VatRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;
import vn.edu.utc.hotel_booking.modules.pricing.service.VatRuleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class VatRuleServiceImpl implements VatRuleService {

    VatRuleRepository vatRuleRepository;
    TaxCategoryService taxCategoryService;
    VatRuleMapper vatRuleMapper;

    @Override
    public BigDecimal getActiveVatPercent(Integer taxCategoryId, LocalDate effectiveDate) {
        if (taxCategoryId == null) {
            return BigDecimal.ZERO;
        }
        LocalDate date = effectiveDate != null ? effectiveDate : LocalDate.now();
        return vatRuleRepository.findActiveVatRule(taxCategoryId, date)
                .map(VatRule::getVatPercent)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public VatRuleResponse getById(Integer id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID quy tắc thuế VAT không được để trống");
        }
        VatRule vatRule = vatRuleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VAT_RULE_NOT_FOUND, "Không tìm thấy quy tắc thuế VAT ID: " + id));
        return vatRuleMapper.toResponse(vatRule);
    }

    @Override
    @Transactional
    public VatRuleResponse create(VatRuleCreateRequest request) {
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày kết thúc không được trước ngày bắt đầu");
        }

        if (vatRuleRepository.existsByVatCodeAndIsDeletedFalse(request.getVatCode())) {
            throw new AppException(ErrorCode.VAT_RULE_CODE_ALREADY_EXISTS,
                    "Mã quy tắc thuế VAT '" + request.getVatCode() + "' đã tồn tại");
        }

        TaxCategory taxCategory = taxCategoryService.getEntityById(request.getTaxCategoryId());

        // Kiểm tra chồng lấn dải ngày với các rule đang active khác trong cùng taxCategory
        List<VatRule> overlapping = vatRuleRepository.findOverlappingActiveRules(
                request.getTaxCategoryId(), request.getStartDate(), request.getEndDate(), null
        );
        if (!overlapping.isEmpty()) {
            throw new AppException(ErrorCode.VAT_RULE_DATE_OVERLAP,
                    "Dải ngày hiệu lực (" + request.getStartDate() + " đến " +
                            (request.getEndDate() != null ? request.getEndDate() : "vô thời hạn") +
                            ") bị trùng lặp với quy tắc VAT '" + overlapping.get(0).getVatName() + "'");
        }

        VatRule entity = vatRuleMapper.toEntity(request);
        entity.setTaxCategory(taxCategory);
        entity.setStatus("ACTIVE");
        entity.setIsDeleted(false);

        VatRule saved = vatRuleRepository.save(entity);
        return vatRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VatRuleResponse update(Integer id, VatRuleUpdateRequest request) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID quy tắc thuế VAT không được để trống");
        }
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày kết thúc không được trước ngày bắt đầu");
        }

        VatRule entity = vatRuleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VAT_RULE_NOT_FOUND, "Không tìm thấy quy tắc thuế VAT ID: " + id));

        // Nếu rule vẫn active, kiểm tra trùng lặp dải ngày ngoại trừ chính nó
        String newStatus = StringUtils.hasText(request.getStatus()) ? request.getStatus() : entity.getStatus();
        if ("ACTIVE".equalsIgnoreCase(newStatus)) {
            List<VatRule> overlapping = vatRuleRepository.findOverlappingActiveRules(
                    entity.getTaxCategory().getId(), request.getStartDate(), request.getEndDate(), id
            );
            if (!overlapping.isEmpty()) {
                throw new AppException(ErrorCode.VAT_RULE_DATE_OVERLAP,
                        "Dải ngày hiệu lực bị trùng lặp với quy tắc VAT '" + overlapping.get(0).getVatName() + "'");
            }
        }

        vatRuleMapper.updateEntityFromRequest(entity, request);
        entity.setUpdatedAt(OffsetDateTime.now());

        VatRule updated = vatRuleRepository.save(entity);
        return vatRuleMapper.toResponse(updated);
    }

    @Override
    public PageResponse<VatRuleResponse> filter(VatRuleSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "startDate";
        String sortDir = StringUtils.hasText(searchDto.getSortDirection()) ? searchDto.getSortDirection() : "DESC";

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<VatRule> resultPage = vatRuleRepository.filter(
                searchDto.getTaxCategoryId(),
                keyword,
                searchDto.getActiveOnDate(),
                status,
                pageable
        );

        return PageResponse.<VatRuleResponse>builder()
                .content(vatRuleMapper.toResponseList(resultPage.getContent()))
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
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Integer id : ids) {
            VatRule entity = vatRuleRepository.findByIdAndIsDeletedFalse(id).orElse(null);
            if (entity != null) {
                entity.setIsDeleted(true);
                entity.setUpdatedAt(OffsetDateTime.now());
                vatRuleRepository.save(entity);
            }
        }
    }
}
