package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.VatRuleService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class VatRuleServiceImpl implements VatRuleService {

    VatRuleRepository vatRuleRepository;

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
}
