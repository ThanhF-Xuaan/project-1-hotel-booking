package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.DiscountRuleTypeMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.DiscountRuleTypeService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class DiscountRuleTypeServiceImpl implements DiscountRuleTypeService {

    DiscountRuleTypeRepository discountRuleTypeRepository;
    DiscountRuleRepository discountRuleRepository;
    DiscountRuleTypeMapper discountRuleTypeMapper;

    @Override
    public List<DiscountRuleTypeResponse> getAll() {
        return discountRuleTypeMapper.toResponseList(discountRuleTypeRepository.findByIsDeletedFalseOrderByPriorityDesc());
    }

    @Override
    public List<DiscountRuleTypeResponse> getActive() {
        return discountRuleTypeMapper.toResponseList(discountRuleTypeRepository.findByStatusAndIsDeletedFalseOrderByPriorityDesc("ACTIVE"));
    }

    @Override
    public DiscountRuleTypeResponse getByCode(String code) {
        return discountRuleTypeMapper.toResponse(getEntityByCode(code));
    }

    @Override
    public DiscountRuleType getEntityByCode(String code) {
        return discountRuleTypeRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy loại quy tắc giảm giá: " + code));
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public DiscountRuleTypeResponse create(DiscountRuleTypeCreateRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();
        if (discountRuleTypeRepository.existsByCodeAndIsDeletedFalse(cleanCode)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mã loại quy tắc giảm giá đã tồn tại: " + cleanCode);
        }

        DiscountRuleType entity = discountRuleTypeMapper.toEntity(request);
        entity.setCode(cleanCode);
        entity.setDescription(normalizeDescription(request.getDescription()));
        DiscountRuleType saved = discountRuleTypeRepository.save(entity);
        return discountRuleTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public DiscountRuleTypeResponse update(String code, DiscountRuleTypeUpdateRequest request) {
        DiscountRuleType entity = getEntityByCode(code);
        discountRuleTypeMapper.updateEntityFromRequest(entity, request);
        entity.setDescription(normalizeDescription(request.getDescription()));
        DiscountRuleType updated = discountRuleTypeRepository.save(entity);
        return discountRuleTypeMapper.toResponse(updated);
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public void delete(String code) {
        DiscountRuleType entity = getEntityByCode(code);
        if (discountRuleRepository.existsByRuleTypeCodeAndIsDeletedFalse(code)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không thể xóa loại quy tắc đang được sử dụng trong các quy tắc giảm giá hiện tại");
        }
        entity.setIsDeleted(true);
        discountRuleTypeRepository.save(entity);
    }
}
