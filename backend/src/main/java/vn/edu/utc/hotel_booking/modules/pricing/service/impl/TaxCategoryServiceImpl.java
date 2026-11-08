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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategorySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.TaxCategoryResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.TaxCategoryMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class TaxCategoryServiceImpl implements TaxCategoryService {

    TaxCategoryRepository taxCategoryRepository;
    VatRuleRepository vatRuleRepository;
    TaxCategoryMapper taxCategoryMapper;

    @Override
    public TaxCategory getEntityById(Integer id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID nhóm thuế không được để trống");
        }
        return taxCategoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAX_CATEGORY_NOT_FOUND, "Không tìm thấy nhóm thuế ID: " + id));
    }

    @Override
    public TaxCategoryResponse getById(Integer id) {
        return taxCategoryMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional
    public TaxCategoryResponse create(TaxCategoryCreateRequest request) {
        if (taxCategoryRepository.existsByCategoryCodeAndIsDeletedFalse(request.getCategoryCode())) {
            throw new AppException(ErrorCode.TAX_CATEGORY_CODE_ALREADY_EXISTS,
                    "Mã nhóm thuế '" + request.getCategoryCode() + "' đã tồn tại");
        }

        TaxCategory entity = taxCategoryMapper.toEntity(request);
        entity.setStatus("ACTIVE");
        entity.setIsDeleted(false);

        TaxCategory saved = taxCategoryRepository.save(entity);
        return taxCategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TaxCategoryResponse update(Integer id, TaxCategoryUpdateRequest request) {
        TaxCategory entity = getEntityById(id);

        taxCategoryMapper.updateEntityFromRequest(entity, request);
        entity.setUpdatedAt(OffsetDateTime.now());

        TaxCategory updated = taxCategoryRepository.save(entity);
        return taxCategoryMapper.toResponse(updated);
    }

    @Override
    public PageResponse<TaxCategoryResponse> filter(TaxCategorySearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        String sortDir = StringUtils.hasText(searchDto.getSortDirection()) ? searchDto.getSortDirection() : "DESC";

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<TaxCategory> resultPage = taxCategoryRepository.filter(keyword, status, pageable);

        return PageResponse.<TaxCategoryResponse>builder()
                .content(taxCategoryMapper.toResponseList(resultPage.getContent()))
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
            TaxCategory entity = taxCategoryRepository.findByIdAndIsDeletedFalse(id).orElse(null);
            if (entity != null) {
                // Kiểm tra xem nhóm thuế có đang có quy tắc VAT liên kết hay không
                if (vatRuleRepository.existsByTaxCategoryIdAndIsDeletedFalse(id)) {
                    throw new AppException(ErrorCode.TAX_CATEGORY_IN_USE,
                            "Nhóm thuế '" + entity.getCategoryName() + "' (ID: " + id + ") đang được liên kết với quy tắc thuế VAT, không thể xóa");
                }
                entity.setIsDeleted(true);
                entity.setUpdatedAt(OffsetDateTime.now());
                taxCategoryRepository.save(entity);
            }
        }
    }

    @Override
    public List<TaxCategoryResponse> getAllActive() {
        TaxCategorySearchDto searchDto = new TaxCategorySearchDto();
        searchDto.setStatus("ACTIVE");
        searchDto.setPage(0);
        searchDto.setPageSize(100);
        return filter(searchDto).getContent();
    }
}
