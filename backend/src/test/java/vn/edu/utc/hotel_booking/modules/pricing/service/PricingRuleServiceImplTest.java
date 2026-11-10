package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.PricingRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.HolidayCalendarRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.PricingRuleServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingRuleServiceImplTest {

    @Mock
    PricingRuleRepository pricingRuleRepository;

    @Mock
    HotelRoomTypeService hotelRoomTypeService;

    @Mock
    PricingRuleTypeRepository pricingRuleTypeRepository;

    @Mock
    HolidayCalendarRepository holidayCalendarRepository;

    @Mock
    PricingRuleMapper pricingRuleMapper;

    @InjectMocks
    PricingRuleServiceImpl pricingRuleService;

    HotelRoomType sampleRoomType;
    PricingRuleType sampleRuleType;
    PricingRule sampleRule;

    @BeforeEach
    void setUp() {
        sampleRoomType = HotelRoomType.builder().id(1).build();
        sampleRuleType = PricingRuleType.builder().code("PEAK_SEASON").displayName("Mùa cao điểm").priority((short) 80).build();
        sampleRule = PricingRule.builder()
                .id(100)
                .hotelRoomType(sampleRoomType)
                .ruleType(sampleRuleType)
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(15))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Tạo quy tắc giá thành công và gọi qua HotelRoomTypeService")
    void createPricingRule_Success() {
        PricingRuleCreateRequest request = PricingRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("PEAK_SEASON")
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(15))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();

        PricingRuleResponse response = PricingRuleResponse.builder()
                .id(100)
                .hotelRoomTypeId(1)
                .ruleTypeCode("PEAK_SEASON")
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(15))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();

        when(hotelRoomTypeService.getEntityById(1)).thenReturn(sampleRoomType);
        when(pricingRuleTypeRepository.findByCode("PEAK_SEASON")).thenReturn(Optional.of(sampleRuleType));
        when(pricingRuleMapper.toEntity(request)).thenReturn(sampleRule);
        when(pricingRuleRepository.save(any(PricingRule.class))).thenReturn(sampleRule);
        when(pricingRuleMapper.toResponse(sampleRule)).thenReturn(response);

        PricingRuleResponse result = pricingRuleService.createPricingRule(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100);
        verify(hotelRoomTypeService).getEntityById(1);
        verify(pricingRuleRepository).save(any(PricingRule.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi ngày kết thúc trước ngày bắt đầu")
    void createPricingRule_EndDateBeforeStartDate_ThrowsException() {
        PricingRuleCreateRequest request = PricingRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("PEAK_SEASON")
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(15))
                .startDate(LocalDate.of(2026, 8, 31))
                .endDate(LocalDate.of(2026, 6, 1))
                .build();

        AppException ex = assertThrows(AppException.class, () -> pricingRuleService.createPricingRule(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        verifyNoInteractions(hotelRoomTypeService);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi phần trăm điều chỉnh lớn hơn 100%")
    void createPricingRule_PercentExceeds100_ThrowsException() {
        PricingRuleCreateRequest request = PricingRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("PEAK_SEASON")
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(150))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();

        AppException ex = assertThrows(AppException.class, () -> pricingRuleService.createPricingRule(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        verifyNoInteractions(hotelRoomTypeService);
    }
}
