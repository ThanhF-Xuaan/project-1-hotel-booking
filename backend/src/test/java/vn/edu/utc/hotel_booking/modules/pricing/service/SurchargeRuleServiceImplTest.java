package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSurchargePolicyDto;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.SurchargeRuleServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurchargeRuleServiceImplTest {

    @Mock
    private SurchargeRuleRepository surchargeRuleRepository;

    @InjectMocks
    private SurchargeRuleServiceImpl surchargeRuleService;

    @Test
    @DisplayName("Chỉ lọc các quy tắc EXTRA_PERSON và EXTRA_BED, bỏ qua EARLY_CHECKIN và LATE_CHECKOUT")
    void getActiveSurchargePolicies_FiltersOnlyExtraPersonAndBed() {
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(2);

        SurchargeRule r1 = SurchargeRule.builder()
                .ruleType("EXTRA_PERSON")
                .pricingType("PER_NIGHT")
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("200000.00"))
                .build();

        SurchargeRule r2 = SurchargeRule.builder()
                .ruleType("EXTRA_BED")
                .pricingType("PER_NIGHT")
                .adjustmentType("PERCENT")
                .adjustmentValue(new BigDecimal("30.00"))
                .build();

        SurchargeRule r3 = SurchargeRule.builder()
                .ruleType("EARLY_CHECKIN")
                .pricingType("PER_STAY")
                .adjustmentType("PERCENT")
                .adjustmentValue(new BigDecimal("50.00"))
                .build();

        when(surchargeRuleRepository.findActiveSurchargesForRoomType(101, checkIn, checkOut))
                .thenReturn(List.of(r1, r2, r3));

        List<RoomSurchargePolicyDto> policies = surchargeRuleService.getActiveSurchargePolicies(101, checkIn, checkOut);

        assertThat(policies).hasSize(2);
        assertThat(policies).extracting(RoomSurchargePolicyDto::getRuleType)
                .containsExactlyInAnyOrder("EXTRA_PERSON", "EXTRA_BED");

        RoomSurchargePolicyDto personPolicy = policies.stream()
                .filter(p -> "EXTRA_PERSON".equals(p.getRuleType()))
                .findFirst().orElseThrow();
        assertThat(personPolicy.getFormattedDescription()).contains("200,000 VND");

        RoomSurchargePolicyDto bedPolicy = policies.stream()
                .filter(p -> "EXTRA_BED".equals(p.getRuleType()))
                .findFirst().orElseThrow();
        assertThat(bedPolicy.getFormattedDescription()).contains("30.00% giá phòng");
    }

    @Test
    @DisplayName("Trả về danh sách rỗng nếu tham số null")
    void getActiveSurchargePolicies_NullParameters_ReturnsEmptyList() {
        List<RoomSurchargePolicyDto> result = surchargeRuleService.getActiveSurchargePolicies(null, null, null);
        assertThat(result).isEmpty();
    }
}
