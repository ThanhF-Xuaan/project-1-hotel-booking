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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.PricingRuleTypeMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.PricingRuleTypeService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PricingRuleTypeServiceImpl implements PricingRuleTypeService {

    PricingRuleTypeRepository pricingRuleTypeRepository;
    PricingRuleRepository pricingRuleRepository;
    PricingRuleTypeMapper pricingRuleTypeMapper;

    @Override
    public List<PricingRuleTypeResponse> getAll() {
        return pricingRuleTypeMapper.toResponseList(pricingRuleTypeRepository.findByIsDeletedFalseOrderByPriorityDesc());
    }

    @Override
    public List<PricingRuleTypeResponse> getActive() {
        return pricingRuleTypeMapper.toResponseList(pricingRuleTypeRepository.findByStatusAndIsDeletedFalseOrderByPriorityDesc("ACTIVE"));
    }

    @Override
    public PricingRuleTypeResponse getByCode(String code) {
        return pricingRuleTypeMapper.toResponse(getEntityByCode(code));
    }

    @Override
    public PricingRuleType getEntityByCode(String code) {
        return pricingRuleTypeRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy loại quy tắc giá: " + code));
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public PricingRuleTypeResponse create(PricingRuleTypeCreateRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();
        if (pricingRuleTypeRepository.existsByCodeAndIsDeletedFalse(cleanCode)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mã loại quy tắc giá đã tồn tại: " + cleanCode);
        }

        PricingRuleType entity = pricingRuleTypeMapper.toEntity(request);
        entity.setCode(cleanCode);
        entity.setDescription(normalizeDescription(request.getDescription()));
        PricingRuleType saved = pricingRuleTypeRepository.save(entity);
        return pricingRuleTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public PricingRuleTypeResponse update(String code, PricingRuleTypeUpdateRequest request) {
        PricingRuleType entity = getEntityByCode(code);
        pricingRuleTypeMapper.updateEntityFromRequest(entity, request);
        entity.setDescription(normalizeDescription(request.getDescription()));
        PricingRuleType updated = pricingRuleTypeRepository.save(entity);
        return pricingRuleTypeMapper.toResponse(updated);
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public void delete(String code) {
        PricingRuleType entity = getEntityByCode(code);
        if (pricingRuleRepository.existsByRuleTypeCodeAndIsDeletedFalse(code)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không thể xóa loại quy tắc đang được sử dụng trong các quy tắc giá hiện tại");
        }
        entity.setIsDeleted(true);
        pricingRuleTypeRepository.save(entity);
    }
}
