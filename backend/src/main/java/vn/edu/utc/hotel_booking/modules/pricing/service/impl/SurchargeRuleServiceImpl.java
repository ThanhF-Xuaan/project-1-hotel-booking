package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
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
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSurchargePolicyDto;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.condition.SurchargeConditionsDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.SurchargeRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HotelAgePolicy;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.SurchargeRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.HotelAgePolicyService;
import vn.edu.utc.hotel_booking.modules.pricing.service.SurchargeRuleService;
import vn.edu.utc.hotel_booking.modules.pricing.validator.SurchargeConditionsValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class SurchargeRuleServiceImpl implements SurchargeRuleService {

    SurchargeRuleRepository surchargeRuleRepository;
    HotelRoomTypeService hotelRoomTypeService;
    HotelAgePolicyService hotelAgePolicyService;
    SurchargeRuleMapper surchargeRuleMapper;
    ObjectMapper objectMapper;
    SurchargeConditionsValidator surchargeConditionsValidator;

    @Override
    public SurchargeRuleResponse getById(Integer id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID quy tắc phụ thu không được để trống");
        }
        SurchargeRule rule = surchargeRuleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.SURCHARGE_RULE_NOT_FOUND, "Không tìm thấy quy tắc phụ thu ID: " + id));
        return surchargeRuleMapper.toResponse(rule);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public SurchargeRuleResponse create(SurchargeRuleCreateRequest request) {
        validateRequest(request.getStartDate(), request.getEndDate(), request.getAdjustmentType(), request.getAdjustmentValue());

        // Lấy thông tin Hạng phòng thông qua HotelRoomTypeService (Cross-aggregate root)
        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(request.getHotelRoomTypeId());

        // Kiểm tra logic kê giường phụ
        if ("EXTRA_BED".equalsIgnoreCase(request.getRuleType())
                && (hotelRoomType.getExtraBeds() == null || hotelRoomType.getExtraBeds() <= 0)) {
            throw new AppException(ErrorCode.INVALID_SURCHARGE_CONFIG, "Hạng phòng này không được cấu hình cho phép kê thêm giường (extraBeds = 0)");
        }

        // Lấy thông tin chính sách độ tuổi nếu có
        HotelAgePolicy agePolicy = null;
        if (request.getAgePolicyId() != null) {
            agePolicy = hotelAgePolicyService.getEntityById(request.getAgePolicyId());
        }

        // Kiểm tra chống trùng lặp dải ngày hiệu lực
        boolean isOverlapped = surchargeRuleRepository.existsOverlappingRule(
                null,
                request.getHotelRoomTypeId(),
                request.getRuleType(),
                request.getAgePolicyId(),
                request.getStartDate(),
                request.getEndDate()
        );
        if (isOverlapped) {
            throw new AppException(ErrorCode.SURCHARGE_RULE_OVERLAP, "Khoảng thời gian hiệu lực bị trùng lặp với quy tắc phụ thu cùng loại đã có");
        }

        String validatedConditions = processAndValidateConditions(request.getRuleType(), request.getConditions(), hotelRoomType);

        SurchargeRule entity = surchargeRuleMapper.toEntity(request);
        entity.setHotelRoomType(hotelRoomType);
        entity.setAgePolicy(agePolicy);
        entity.setConditions(validatedConditions);

        SurchargeRule saved = surchargeRuleRepository.save(entity);
        return surchargeRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public SurchargeRuleResponse update(Integer id, SurchargeRuleUpdateRequest request) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID quy tắc phụ thu không được để trống");
        }
        validateRequest(request.getStartDate(), request.getEndDate(), request.getAdjustmentType(), request.getAdjustmentValue());

        SurchargeRule rule = surchargeRuleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SURCHARGE_RULE_NOT_FOUND, "Không tìm thấy quy tắc phụ thu ID: " + id));

        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(request.getHotelRoomTypeId());

        if ("EXTRA_BED".equalsIgnoreCase(request.getRuleType())
                && (hotelRoomType.getExtraBeds() == null || hotelRoomType.getExtraBeds() <= 0)) {
            throw new AppException(ErrorCode.INVALID_SURCHARGE_CONFIG, "Hạng phòng này không được cấu hình cho phép kê thêm giường");
        }

        HotelAgePolicy agePolicy = null;
        if (request.getAgePolicyId() != null) {
            agePolicy = hotelAgePolicyService.getEntityById(request.getAgePolicyId());
        }

        boolean isOverlapped = surchargeRuleRepository.existsOverlappingRule(
                id,
                request.getHotelRoomTypeId(),
                request.getRuleType(),
                request.getAgePolicyId(),
                request.getStartDate(),
                request.getEndDate()
        );
        if (isOverlapped) {
            throw new AppException(ErrorCode.SURCHARGE_RULE_OVERLAP, "Khoảng thời gian hiệu lực bị trùng lặp với quy tắc phụ thu cùng loại khác");
        }

        String validatedConditions = processAndValidateConditions(request.getRuleType(), request.getConditions(), hotelRoomType);

        surchargeRuleMapper.updateEntity(rule, request);
        rule.setHotelRoomType(hotelRoomType);
        rule.setAgePolicy(agePolicy);
        rule.setConditions(validatedConditions);

        SurchargeRule updated = surchargeRuleRepository.save(rule);
        return surchargeRuleMapper.toResponse(updated);
    }

    @Override
    public PageResponse<SurchargeRuleResponse> filter(SurchargeRuleSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() >= 0 ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "id";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        Page<SurchargeRule> pageResult = surchargeRuleRepository.filterSurchargeRules(
                searchDto.getHotelId(),
                searchDto.getHotelRoomTypeId(),
                searchDto.getAgePolicyId(),
                searchDto.getRuleType(),
                searchDto.getPricingType(),
                searchDto.getStatus(),
                searchDto.getActiveOnDate(),
                pageable
        );

        List<SurchargeRuleResponse> content = surchargeRuleMapper.toResponseList(pageResult.getContent());

        return PageResponse.<SurchargeRuleResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .empty(pageResult.isEmpty())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Danh sách ID xóa không được để trống");
        }
        surchargeRuleRepository.softDeleteByIds(ids);
    }

    @Override
    public List<RoomSurchargePolicyDto> getActiveSurchargePolicies(
            Integer hotelRoomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (hotelRoomTypeId == null || checkInDate == null || checkOutDate == null) {
            return Collections.emptyList();
        }

        List<SurchargeRule> rules = surchargeRuleRepository.findActiveSurchargesForRoomType(
                hotelRoomTypeId, checkInDate, checkOutDate
        );

        return rules.stream()
                .filter(r -> "EXTRA_PERSON".equalsIgnoreCase(r.getRuleType())
                        || "EXTRA_BED".equalsIgnoreCase(r.getRuleType()))
                .map(this::mapToPolicyDto)
                .toList();
    }

    private void validateRequest(LocalDate startDate, LocalDate endDate, String adjustmentType, BigDecimal adjustmentValue) {
        if (startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày bắt đầu áp dụng phải nhỏ hơn hoặc bằng ngày kết thúc");
        }
        if ("PERCENT".equalsIgnoreCase(adjustmentType) && adjustmentValue.compareTo(new BigDecimal("100")) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Giá trị phụ thu phần trăm không được vượt quá 100%");
        }
    }

    private RoomSurchargePolicyDto mapToPolicyDto(SurchargeRule rule) {
        String description = buildFormattedDescription(rule);
        return RoomSurchargePolicyDto.builder()
                .ruleType(rule.getRuleType())
                .pricingType(rule.getPricingType())
                .adjustmentType(rule.getAdjustmentType())
                .adjustmentValue(rule.getAdjustmentValue())
                .conditions(rule.getConditions())
                .formattedDescription(description)
                .build();
    }

    private String buildFormattedDescription(SurchargeRule rule) {
        boolean isPerson = "EXTRA_PERSON".equalsIgnoreCase(rule.getRuleType());
        String subject = isPerson ? "Phụ thu thêm người: " : "Phụ thu kê thêm giường: ";
        String suffix = "PER_STAY".equalsIgnoreCase(rule.getPricingType()) ? " / kỳ lưu trú" : " / đêm";

        if ("PERCENT".equalsIgnoreCase(rule.getAdjustmentType())) {
            return subject + rule.getAdjustmentValue() + "% giá phòng" + suffix;
        } else {
            BigDecimal val = rule.getAdjustmentValue() != null ? rule.getAdjustmentValue() : BigDecimal.ZERO;
            return subject + String.format("%,.0f", val) + " VND" + suffix;
        }
    }

    private String processAndValidateConditions(String ruleType, String conditionsJson, HotelRoomType hotelRoomType) {
        LocalTime hotelStandardTime = null;
        if (hotelRoomType != null && hotelRoomType.getHotel() != null) {
            if ("EARLY_CHECKIN".equalsIgnoreCase(ruleType)) {
                hotelStandardTime = hotelRoomType.getHotel().getCheckInTime() != null
                        ? hotelRoomType.getHotel().getCheckInTime() : LocalTime.of(14, 0);
            } else if ("LATE_CHECKOUT".equalsIgnoreCase(ruleType)) {
                hotelStandardTime = hotelRoomType.getHotel().getCheckOutTime() != null
                        ? hotelRoomType.getHotel().getCheckOutTime() : LocalTime.of(12, 0);
            }
        }

        if (StringUtils.hasText(conditionsJson) && !"{}".equals(conditionsJson.trim())) {
            try {
                SurchargeConditionsDto conditionsDto = objectMapper.readValue(conditionsJson, SurchargeConditionsDto.class);
                if (hotelStandardTime != null) {
                    // Cưỡng chế standard_time theo giờ của cơ sở (Hạng phòng tuyệt đối không được phép override)
                    conditionsDto.setStandardTime(hotelStandardTime.toString());
                }
                surchargeConditionsValidator.validate(ruleType, conditionsDto, hotelStandardTime);
                return objectMapper.writeValueAsString(conditionsDto);
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Định dạng JSON điều kiện phụ thu không hợp lệ: " + e.getMessage());
            }
        } else if (hotelStandardTime != null && ("EARLY_CHECKIN".equalsIgnoreCase(ruleType) || "LATE_CHECKOUT".equalsIgnoreCase(ruleType))) {
            try {
                SurchargeConditionsDto conditionsDto = SurchargeConditionsDto.builder()
                        .standardTime(hotelStandardTime.toString())
                        .build();
                return objectMapper.writeValueAsString(conditionsDto);
            } catch (Exception ignored) {
                return "{}";
            }
        }
        return StringUtils.hasText(conditionsJson) ? conditionsJson : "{}";
    }
}
