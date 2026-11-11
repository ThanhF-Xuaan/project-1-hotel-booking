package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MasterItemResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.MasterItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class MasterItemServiceImpl implements MasterItemService {

    MasterItemRepository masterItemRepository;
    TaxCategoryService taxCategoryService;
    OperationMapper operationMapper;

    @Override
    public MasterItem getEntityById(Integer id) {
        if (id == null) {
            return null;
        }
        return masterItemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MASTER_ITEM_NOT_FOUND, "Không tìm thấy danh mục chuỗi MasterItem ID: " + id));
    }

    @Override
    @Transactional
    public MasterItem saveEntity(MasterItem masterItem) {
        return masterItemRepository.save(masterItem);
    }

    @Override
    public MasterItemResponse getById(Integer id) {
        MasterItem item = getEntityById(id);
        return operationMapper.toResponse(item);
    }

    @Override
    @Transactional
    public MasterItemResponse create(MasterItemCreateRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (masterItemRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new AppException(ErrorCode.MASTER_ITEM_CODE_ALREADY_EXISTS, "Mã danh mục chuỗi '" + code + "' đã tồn tại");
        }

        TaxCategory taxCategory = taxCategoryService.getEntityById(request.getTaxCategoryId());

        ServicePricingType pricingType = request.getPricingType();
        if (request.getItemType() == MenuType.SERVICE && pricingType == null) {
            pricingType = ServicePricingType.PER_STAY;
        }

        MasterItem masterItem = MasterItem.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .itemType(request.getItemType())
                .defaultBasePrice(request.getDefaultBasePrice())
                .taxCategory(taxCategory)
                .pricingType(pricingType)
                .unit(StringUtils.hasText(request.getUnit()) ? request.getUnit().trim() : null)
                .status(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase() : "ACTIVE")
                .build();

        MasterItem saved = masterItemRepository.save(masterItem);
        return operationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MasterItemResponse update(Integer id, MasterItemUpdateRequest request) {
        MasterItem item = getEntityById(id);

        if (request.getTaxCategoryId() != null) {
            TaxCategory taxCategory = taxCategoryService.getEntityById(request.getTaxCategoryId());
            item.setTaxCategory(taxCategory);
        }

        if (StringUtils.hasText(request.getName())) {
            item.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }

        if (request.getDefaultBasePrice() != null) {
            item.setDefaultBasePrice(request.getDefaultBasePrice());
        }

        if (request.getPricingType() != null) {
            item.setPricingType(request.getPricingType());
        }

        if (request.getUnit() != null) {
            item.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit().trim() : null);
        }

        if (StringUtils.hasText(request.getStatus())) {
            item.setStatus(request.getStatus().trim().toUpperCase());
        }

        MasterItem saved = masterItemRepository.save(item);
        return operationMapper.toResponse(saved);
    }

    @Override
    public PageResponse<MasterItemResponse> filter(MasterItemSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() >= 0 ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        Specification<MasterItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (StringUtils.hasText(searchDto.getCode())) {
                predicates.add(cb.like(cb.upper(root.get("code")), "%" + searchDto.getCode().trim().toUpperCase() + "%"));
            }

            if (StringUtils.hasText(searchDto.getName())) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + searchDto.getName().trim().toLowerCase() + "%"));
            }

            if (searchDto.getItemType() != null) {
                predicates.add(cb.equal(root.get("itemType"), searchDto.getItemType()));
            }

            if (searchDto.getTaxCategoryId() != null) {
                predicates.add(cb.equal(root.get("taxCategory").get("id"), searchDto.getTaxCategoryId()));
            }

            if (StringUtils.hasText(searchDto.getStatus())) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus().trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MasterItem> pageResult = masterItemRepository.findAll(spec, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(pageResult.map(operationMapper::toResponse));
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<MasterItem> items = masterItemRepository.findAllById(ids);
        items.forEach(item -> item.setIsDeleted(true));
        masterItemRepository.saveAll(items);
    }

    @Override
    public List<MasterItemResponse> getAllActive() {
        return masterItemRepository.findAllByStatusAndIsDeletedFalse("ACTIVE")
                .stream()
                .map(operationMapper::toResponse)
                .toList();
    }
}
