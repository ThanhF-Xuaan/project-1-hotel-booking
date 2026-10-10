package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSurchargePolicyDto;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.SurchargeRuleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurchargeRuleServiceImpl implements SurchargeRuleService {

    private final SurchargeRuleRepository surchargeRuleRepository;

    @Override
    @Transactional(readOnly = true)
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
}
