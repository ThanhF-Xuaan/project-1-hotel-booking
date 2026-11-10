package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.*;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Campaign;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.DiscountRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.CampaignService;
import vn.edu.utc.hotel_booking.modules.pricing.service.DiscountRuleService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class DiscountRuleServiceImpl implements DiscountRuleService {

    DiscountRuleRepository discountRuleRepository;
    DiscountRuleTypeRepository discountRuleTypeRepository;
    HotelRoomTypeService hotelRoomTypeService;
    CampaignService campaignService;
    DiscountRuleMapper discountRuleMapper;
    ObjectMapper objectMapper;

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public DiscountRuleResponse createDiscountRule(DiscountRuleCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày kết thúc quy tắc giảm giá phải sau hoặc bằng ngày bắt đầu");
        }
        if ("PERCENT".equalsIgnoreCase(request.getDiscountType()) && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mức giảm giá theo phần trăm không được vượt quá 100%");
        }

        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(request.getHotelRoomTypeId());

        DiscountRuleType ruleType = discountRuleTypeRepository.findByCode(request.getRuleTypeCode())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Không tìm thấy loại quy tắc giảm giá: " + request.getRuleTypeCode()));

        Campaign campaign = null;
        boolean isCampaignType = "SPECIAL_CAMPAIGN".equalsIgnoreCase(request.getRuleTypeCode()) || request.getCampaignId() != null;
        if (isCampaignType) {
            if (request.getCampaignId() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Quy tắc giảm giá theo chiến dịch bắt buộc phải chọn Chiến dịch áp dụng");
            }
            campaign = campaignService.getEntityById(request.getCampaignId());
            if (request.getStartDate().isBefore(campaign.getStartDate()) || request.getEndDate().isAfter(campaign.getEndDate())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        String.format("Thời gian hiệu lực của quy tắc giảm giá (%s đến %s) phải nằm trong khoảng thời gian diễn ra chiến dịch '%s' (%s đến %s)",
                                request.getStartDate(), request.getEndDate(), campaign.getName(), campaign.getStartDate(), campaign.getEndDate()));
            }
        }

        DiscountConditionDto validatedConditions = parseAndValidateConditions(
                request.getRuleTypeCode(),
                request.getConditions(),
                request.getDiscountType(),
                request.getDiscountValue()
        );

        DiscountRule discountRule = discountRuleMapper.toEntity(request);
        discountRule.setHotelRoomType(hotelRoomType);
        discountRule.setRuleType(ruleType);
        discountRule.setCampaign(campaign);

        if (validatedConditions != null && validatedConditions.getTiers() != null && !validatedConditions.getTiers().isEmpty()) {
            try {
                discountRule.setConditions(objectMapper.writeValueAsString(validatedConditions));
            } catch (Exception ignored) {
            }
        }

        DiscountRule saved = discountRuleRepository.save(discountRule);
        return discountRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public DiscountRuleResponse updateDiscountRule(Integer id, DiscountRuleUpdateRequest request) {
        DiscountRule discountRule = discountRuleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy quy tắc giảm giá ID: " + id));

        if (request.getEndDate() != null && request.getStartDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày kết thúc quy tắc giảm giá phải sau hoặc bằng ngày bắt đầu");
        }
        if ("PERCENT".equalsIgnoreCase(request.getDiscountType()) && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mức giảm giá theo phần trăm không được vượt quá 100%");
        }

        DiscountConditionDto validatedConditions = parseAndValidateConditions(
                discountRule.getRuleType().getCode(),
                request.getConditions(),
                request.getDiscountType() != null ? request.getDiscountType() : discountRule.getDiscountType(),
                request.getDiscountValue() != null ? request.getDiscountValue() : discountRule.getDiscountValue()
        );

        discountRuleMapper.updateEntity(discountRule, request);

        if (validatedConditions != null && validatedConditions.getTiers() != null && !validatedConditions.getTiers().isEmpty()) {
            try {
                discountRule.setConditions(objectMapper.writeValueAsString(validatedConditions));
            } catch (Exception ignored) {
            }
        }

        boolean isCampaignType = "SPECIAL_CAMPAIGN".equalsIgnoreCase(discountRule.getRuleType().getCode()) || request.getCampaignId() != null;
        if (isCampaignType) {
            if (request.getCampaignId() == null && discountRule.getCampaign() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Quy tắc giảm giá theo chiến dịch bắt buộc phải chọn Chiến dịch áp dụng");
            }
            Integer targetCampaignId = request.getCampaignId() != null ? request.getCampaignId() : discountRule.getCampaign().getId();
            Campaign campaign = campaignService.getEntityById(targetCampaignId);
            if (discountRule.getStartDate().isBefore(campaign.getStartDate()) || discountRule.getEndDate().isAfter(campaign.getEndDate())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        String.format("Thời gian hiệu lực của quy tắc giảm giá (%s đến %s) phải nằm trong khoảng thời gian diễn ra chiến dịch '%s' (%s đến %s)",
                                discountRule.getStartDate(), discountRule.getEndDate(), campaign.getName(), campaign.getStartDate(), campaign.getEndDate()));
            }
            discountRule.setCampaign(campaign);
        } else {
            discountRule.setCampaign(null);
        }

        DiscountRule updated = discountRuleRepository.save(discountRule);
        return discountRuleMapper.toResponse(updated);
    }

    @Override
    public DiscountRuleResponse getDiscountRuleById(Integer id) {
        DiscountRule discountRule = discountRuleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy quy tắc giảm giá ID: " + id));
        return discountRuleMapper.toResponse(discountRule);
    }

    @Override
    public PageResponse<DiscountRuleResponse> filterDiscountRules(DiscountRuleSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<DiscountRule> page = discountRuleRepository.filterDiscountRules(
                searchDto.getHotelRoomTypeId(),
                searchDto.getCampaignId(),
                searchDto.getRuleTypeCode(),
                searchDto.getStatus(),
                searchDto.getActiveOnDate(),
                pageable
        );

        return PageResponse.<DiscountRuleResponse>builder()
                .content(discountRuleMapper.toResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public void deleteDiscountRules(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            discountRuleRepository.softDeleteByIds(ids);
        }
    }

    private DiscountConditionDto parseAndValidateConditions(String ruleTypeCode, String conditionsJson, String defaultDiscountType, BigDecimal defaultDiscountValue) {
        if (conditionsJson == null || conditionsJson.trim().isEmpty() || "{}".equals(conditionsJson.trim())) {
            return null;
        }

        DiscountConditionDto conditionDto;
        try {
            conditionDto = objectMapper.readValue(conditionsJson, DiscountConditionDto.class);
        } catch (Exception e) {
            log.warn("Không thể parse conditions JSON: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Định dạng JSON của trường điều kiện (conditions) không hợp lệ");
        }

        if (conditionDto == null) {
            return null;
        }

        List<DiscountTierDto> tiers = conditionDto.getTiers();
        if (tiers != null && !tiers.isEmpty()) {
            tiers.sort(Comparator.comparing(DiscountTierDto::getMinNights, Comparator.nullsLast(Comparator.naturalOrder())));

            for (int i = 0; i < tiers.size(); i++) {
                DiscountTierDto tier = tiers.get(i);
                if (tier.getMinNights() == null || tier.getMinNights() < 1) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Số đêm tối thiểu của mỗi bậc phải lớn hơn hoặc bằng 1");
                }
                if (tier.getDiscountValue() == null || tier.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Giá trị giảm giá của bậc phải lớn hơn 0");
                }
                String dType = tier.getDiscountType() != null && !tier.getDiscountType().isBlank() ? tier.getDiscountType() : defaultDiscountType;
                if ("PERCENT".equalsIgnoreCase(dType) && tier.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mức giảm giá theo phần trăm của bậc không được vượt quá 100%");
                }

                if (tier.getMaxNights() != null) {
                    if (tier.getMaxNights() < tier.getMinNights()) {
                        throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                                String.format("Bậc từ %d đêm: Số đêm tối đa (%d) không được nhỏ hơn số đêm tối thiểu (%d)",
                                        tier.getMinNights(), tier.getMaxNights(), tier.getMinNights()));
                    }
                } else if (i < tiers.size() - 1) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Chỉ bậc lưu trú cuối cùng mới được phép không giới hạn số đêm tối đa");
                }

                if (i > 0) {
                    DiscountTierDto prevTier = tiers.get(i - 1);
                    if (prevTier.getMaxNights() == null) {
                        throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Bậc lưu trú phía trước chưa giới hạn số đêm tối đa nên không thể thêm bậc tiếp theo");
                    }
                    if (tier.getMinNights() <= prevTier.getMaxNights()) {
                        throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                                String.format("Khoảng đêm của các bậc bị chồng lấn: Bậc trước kết thúc ở %d đêm, bậc sau bắt đầu từ %d đêm",
                                        prevTier.getMaxNights(), tier.getMinNights()));
                    }
                }
            }
        }

        return conditionDto;
    }
}

