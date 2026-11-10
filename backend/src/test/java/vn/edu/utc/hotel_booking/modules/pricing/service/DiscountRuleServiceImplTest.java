package vn.edu.utc.hotel_booking.modules.pricing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Campaign;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.DiscountRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.DiscountRuleServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountRuleServiceImplTest {

    @Mock
    DiscountRuleRepository discountRuleRepository;

    @Mock
    DiscountRuleTypeRepository discountRuleTypeRepository;

    @Mock
    HotelRoomTypeService hotelRoomTypeService;

    @Mock
    CampaignService campaignService;

    @Mock
    DiscountRuleMapper discountRuleMapper;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    DiscountRuleServiceImpl discountRuleService;

    HotelRoomType sampleRoomType;
    DiscountRuleType sampleRuleType;
    Campaign sampleCampaign;
    DiscountRule sampleRule;

    @BeforeEach
    void setUp() {
        sampleRoomType = HotelRoomType.builder().id(1).build();
        sampleRuleType = DiscountRuleType.builder()
                .code("SPECIAL_CAMPAIGN")
                .displayName("Chiến dịch đặc biệt")
                .priority((short) 90)
                .build();
        sampleCampaign = Campaign.builder()
                .id(10)
                .name("Siêu Sale Hè")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();
        sampleRule = DiscountRule.builder()
                .id(50)
                .hotelRoomType(sampleRoomType)
                .ruleType(sampleRuleType)
                .campaign(sampleCampaign)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .startDate(LocalDate.of(2026, 6, 5))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();
    }

    @Test
    @DisplayName("Tạo quy tắc giảm giá thành công khi ngày nằm trong thời gian của Campaign")
    void createDiscountRule_Success_WithinCampaignDates() {
        DiscountRuleCreateRequest request = DiscountRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("SPECIAL_CAMPAIGN")
                .campaignId(10)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .startDate(LocalDate.of(2026, 6, 5))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        DiscountRuleResponse response = DiscountRuleResponse.builder()
                .id(50)
                .hotelRoomTypeId(1)
                .campaignId(10)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .startDate(LocalDate.of(2026, 6, 5))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        when(hotelRoomTypeService.getEntityById(1)).thenReturn(sampleRoomType);
        when(discountRuleTypeRepository.findByCode("SPECIAL_CAMPAIGN")).thenReturn(Optional.of(sampleRuleType));
        when(campaignService.getEntityById(10)).thenReturn(sampleCampaign);
        when(discountRuleMapper.toEntity(request)).thenReturn(sampleRule);
        when(discountRuleRepository.save(any(DiscountRule.class))).thenReturn(sampleRule);
        when(discountRuleMapper.toResponse(sampleRule)).thenReturn(response);

        DiscountRuleResponse result = discountRuleService.createDiscountRule(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(50);
        verify(hotelRoomTypeService).getEntityById(1);
        verify(campaignService).getEntityById(10);
        verify(discountRuleRepository).save(any(DiscountRule.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi ngày của DiscountRule vượt quá thời gian của Campaign")
    void createDiscountRule_Fail_DatesOutsideCampaign() {
        DiscountRuleCreateRequest request = DiscountRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("SPECIAL_CAMPAIGN")
                .campaignId(10)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .startDate(LocalDate.of(2026, 5, 25)) // Trước ngày bắt đầu của campaign (2026-06-01)
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        when(hotelRoomTypeService.getEntityById(1)).thenReturn(sampleRoomType);
        when(discountRuleTypeRepository.findByCode("SPECIAL_CAMPAIGN")).thenReturn(Optional.of(sampleRuleType));
        when(campaignService.getEntityById(10)).thenReturn(sampleCampaign);

        AppException ex = assertThrows(AppException.class, () -> discountRuleService.createDiscountRule(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        assertThat(ex.getMessage()).contains("phải nằm trong khoảng thời gian diễn ra chiến dịch");
        verify(discountRuleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi chọn loại SPECIAL_CAMPAIGN nhưng không truyền campaignId")
    void createDiscountRule_Fail_CampaignTypeWithoutCampaignId() {
        DiscountRuleCreateRequest request = DiscountRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("SPECIAL_CAMPAIGN")
                .campaignId(null)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(20))
                .startDate(LocalDate.of(2026, 6, 5))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        when(hotelRoomTypeService.getEntityById(1)).thenReturn(sampleRoomType);
        when(discountRuleTypeRepository.findByCode("SPECIAL_CAMPAIGN")).thenReturn(Optional.of(sampleRuleType));

        AppException ex = assertThrows(AppException.class, () -> discountRuleService.createDiscountRule(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        assertThat(ex.getMessage()).contains("bắt buộc phải chọn Chiến dịch áp dụng");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi phần trăm giảm giá vượt quá 100%")
    void createDiscountRule_Fail_PercentExceeds100() {
        DiscountRuleCreateRequest request = DiscountRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("SPECIAL_CAMPAIGN")
                .campaignId(10)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(110))
                .startDate(LocalDate.of(2026, 6, 5))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        AppException ex = assertThrows(AppException.class, () -> discountRuleService.createDiscountRule(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        assertThat(ex.getMessage()).contains("không được vượt quá 100%");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi ngày kết thúc trước ngày bắt đầu")
    void createDiscountRule_Fail_EndDateBeforeStartDate() {
        DiscountRuleCreateRequest request = DiscountRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("SPECIAL_CAMPAIGN")
                .campaignId(10)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(15))
                .startDate(LocalDate.of(2026, 6, 25))
                .endDate(LocalDate.of(2026, 6, 5))
                .build();

        AppException ex = assertThrows(AppException.class, () -> discountRuleService.createDiscountRule(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("Tạo thành công quy tắc giảm giá dài ngày LONG_STAY với các bậc (tiers) hợp lệ")
    void createDiscountRule_Success_WithValidTiers() {
        String validConditions = """
                {
                  "tiers": [
                    {"minNights": 3, "maxNights": 6, "discountType": "PERCENT", "discountValue": 10},
                    {"minNights": 7, "maxNights": 13, "discountType": "PERCENT", "discountValue": 15},
                    {"minNights": 14, "maxNights": null, "discountType": "PERCENT", "discountValue": 20}
                  ]
                }
                """;

        DiscountRuleCreateRequest request = DiscountRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("LONG_STAY")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(10))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .conditions(validConditions)
                .build();

        DiscountRuleType longStayType = DiscountRuleType.builder().code("LONG_STAY").displayName("Dài ngày").build();
        DiscountRule ruleEntity = DiscountRule.builder().id(99).build();
        DiscountRuleResponse expectedResp = DiscountRuleResponse.builder().id(99).ruleTypeCode("LONG_STAY").build();

        when(hotelRoomTypeService.getEntityById(1)).thenReturn(sampleRoomType);
        when(discountRuleTypeRepository.findByCode("LONG_STAY")).thenReturn(Optional.of(longStayType));
        when(discountRuleMapper.toEntity(request)).thenReturn(ruleEntity);
        when(discountRuleRepository.save(any(DiscountRule.class))).thenReturn(ruleEntity);
        when(discountRuleMapper.toResponse(ruleEntity)).thenReturn(expectedResp);

        DiscountRuleResponse actual = discountRuleService.createDiscountRule(request);

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(99);
        verify(discountRuleRepository).save(any(DiscountRule.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi các bậc lưu trú bị chồng lấn số đêm")
    void createDiscountRule_Fail_OverlappingTiers() {
        String overlappingConditions = """
                {
                  "tiers": [
                    {"minNights": 3, "maxNights": 7, "discountType": "PERCENT", "discountValue": 10},
                    {"minNights": 6, "maxNights": 12, "discountType": "PERCENT", "discountValue": 15}
                  ]
                }
                """;

        DiscountRuleCreateRequest request = DiscountRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("LONG_STAY")
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(10))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .conditions(overlappingConditions)
                .build();

        DiscountRuleType longStayType = DiscountRuleType.builder().code("LONG_STAY").displayName("Dài ngày").build();
        when(hotelRoomTypeService.getEntityById(1)).thenReturn(sampleRoomType);
        when(discountRuleTypeRepository.findByCode("LONG_STAY")).thenReturn(Optional.of(longStayType));

        AppException ex = assertThrows(AppException.class, () -> discountRuleService.createDiscountRule(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        assertThat(ex.getMessage()).contains("chồng lấn");
    }
}

