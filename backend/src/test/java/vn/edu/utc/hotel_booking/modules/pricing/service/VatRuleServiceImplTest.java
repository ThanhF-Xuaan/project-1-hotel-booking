package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.VatRuleServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VatRuleServiceImplTest {

    @Mock
    VatRuleRepository vatRuleRepository;

    @InjectMocks
    VatRuleServiceImpl vatRuleService;

    VatRule sampleVatRule;

    @BeforeEach
    void setUp() {
        TaxCategory taxCat = TaxCategory.builder().id(1).categoryCode("VAT_10").categoryName("VAT 10%").build();
        sampleVatRule = VatRule.builder()
                .id(100)
                .taxCategory(taxCat)
                .vatCode("VAT_10")
                .vatName("VAT 10%")
                .vatPercent(new BigDecimal("10.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getActiveVatPercent - Trả về thuế suất chính xác khi có quy tắc VAT hiệu lực")
    void getActiveVatPercent_Success() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        when(vatRuleRepository.findActiveVatRule(1, today)).thenReturn(Optional.of(sampleVatRule));

        BigDecimal result = vatRuleService.getActiveVatPercent(1, today);

        assertThat(result).isEqualByComparingTo(new BigDecimal("10.00"));
        verify(vatRuleRepository).findActiveVatRule(1, today);
    }

    @Test
    @DisplayName("getActiveVatPercent - Trả về ZERO khi taxCategoryId là null")
    void getActiveVatPercent_NullTaxCategoryId() {
        BigDecimal result = vatRuleService.getActiveVatPercent(null, LocalDate.now());
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getActiveVatPercent - Trả về ZERO khi không tìm thấy quy tắc hiệu lực")
    void getActiveVatPercent_NotFound() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        when(vatRuleRepository.findActiveVatRule(99, today)).thenReturn(Optional.empty());

        BigDecimal result = vatRuleService.getActiveVatPercent(99, today);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(vatRuleRepository).findActiveVatRule(99, today);
    }
}
