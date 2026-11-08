package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VatRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.VatRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.VatRuleServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VatRuleServiceImplTest {

    @Mock
    VatRuleRepository vatRuleRepository;

    @Mock
    TaxCategoryService taxCategoryService;

    @Mock
    VatRuleMapper vatRuleMapper;

    @InjectMocks
    VatRuleServiceImpl vatRuleService;

    TaxCategory sampleTaxCategory;
    VatRule sampleVatRule;
    VatRuleResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleTaxCategory = TaxCategory.builder()
                .id(1)
                .categoryCode("ROOM_TAX")
                .categoryName("Thuế dịch vụ lưu trú")
                .build();

        sampleVatRule = VatRule.builder()
                .id(100)
                .taxCategory(sampleTaxCategory)
                .vatCode("VAT_ROOM_8")
                .vatName("Thuế phòng 8%")
                .vatPercent(new BigDecimal("8.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status("ACTIVE")
                .build();

        sampleResponse = VatRuleResponse.builder()
                .id(100)
                .taxCategoryId(1)
                .taxCategoryName("Thuế dịch vụ lưu trú")
                .vatCode("VAT_ROOM_8")
                .vatName("Thuế phòng 8%")
                .vatPercent(new BigDecimal("8.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getActiveVatPercent - Trả về thuế suất chính xác khi có quy tắc VAT hiệu lực")
    void getActiveVatPercent_Success() {
        LocalDate today = LocalDate.of(2026, 3, 1);
        when(vatRuleRepository.findActiveVatRule(1, today)).thenReturn(Optional.of(sampleVatRule));

        BigDecimal result = vatRuleService.getActiveVatPercent(1, today);

        assertThat(result).isEqualByComparingTo(new BigDecimal("8.00"));
        verify(vatRuleRepository).findActiveVatRule(1, today);
    }

    @Test
    @DisplayName("create - Thành công khi thông tin hợp lệ")
    void create_Success() {
        VatRuleCreateRequest request = VatRuleCreateRequest.builder()
                .taxCategoryId(1)
                .vatCode("VAT_ROOM_8")
                .vatName("Thuế phòng 8%")
                .vatPercent(new BigDecimal("8.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        when(vatRuleRepository.existsByVatCodeAndIsDeletedFalse("VAT_ROOM_8")).thenReturn(false);
        when(taxCategoryService.getEntityById(1)).thenReturn(sampleTaxCategory);
        when(vatRuleRepository.findOverlappingActiveRules(1, request.getStartDate(), request.getEndDate(), null))
                .thenReturn(Collections.emptyList());
        when(vatRuleMapper.toEntity(request)).thenReturn(sampleVatRule);
        when(vatRuleRepository.save(any(VatRule.class))).thenReturn(sampleVatRule);
        when(vatRuleMapper.toResponse(sampleVatRule)).thenReturn(sampleResponse);

        VatRuleResponse response = vatRuleService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100);
        verify(vatRuleRepository).save(any(VatRule.class));
    }

    @Test
    @DisplayName("create - Ném ngoại lệ khi ngày kết thúc trước ngày bắt đầu")
    void create_InvalidDateRange_ThrowsException() {
        VatRuleCreateRequest request = VatRuleCreateRequest.builder()
                .taxCategoryId(1)
                .vatCode("VAT_ROOM_8")
                .vatName("Thuế phòng 8%")
                .vatPercent(new BigDecimal("8.00"))
                .startDate(LocalDate.of(2026, 6, 30))
                .endDate(LocalDate.of(2026, 1, 1))
                .build();

        assertThatThrownBy(() -> vatRuleService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("create - Ném ngoại lệ VAT_RULE_DATE_OVERLAP khi bị trùng lặp dải ngày hiệu lực")
    void create_DateOverlap_ThrowsException() {
        VatRuleCreateRequest request = VatRuleCreateRequest.builder()
                .taxCategoryId(1)
                .vatCode("VAT_ROOM_NEW")
                .vatName("Thuế phòng mới")
                .vatPercent(new BigDecimal("8.00"))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 8, 30))
                .build();

        when(vatRuleRepository.existsByVatCodeAndIsDeletedFalse("VAT_ROOM_NEW")).thenReturn(false);
        when(taxCategoryService.getEntityById(1)).thenReturn(sampleTaxCategory);
        when(vatRuleRepository.findOverlappingActiveRules(1, request.getStartDate(), request.getEndDate(), null))
                .thenReturn(List.of(sampleVatRule));

        assertThatThrownBy(() -> vatRuleService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VAT_RULE_DATE_OVERLAP);
    }

    @Test
    @DisplayName("filter - Phân trang tìm kiếm thành công")
    void filter_Success() {
        VatRuleSearchDto searchDto = new VatRuleSearchDto();
        searchDto.setPage(0);
        searchDto.setPageSize(10);
        searchDto.setTaxCategoryId(1);

        Page<VatRule> page = new PageImpl<>(List.of(sampleVatRule));
        when(vatRuleRepository.filter(eq(1), any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(vatRuleMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<VatRuleResponse> result = vatRuleService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}
