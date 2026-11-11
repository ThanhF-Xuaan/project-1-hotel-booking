package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSurchargePolicyDto;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.SurchargeRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HotelAgePolicy;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.SurchargeRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.SurchargeRuleServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurchargeRuleServiceImplTest {

    @Mock
    private SurchargeRuleRepository surchargeRuleRepository;

    @Mock
    private HotelRoomTypeService hotelRoomTypeService;

    @Mock
    private HotelAgePolicyService hotelAgePolicyService;

    @Mock
    private SurchargeRuleMapper surchargeRuleMapper;

    @InjectMocks
    private SurchargeRuleServiceImpl surchargeRuleService;

    private SurchargeRuleCreateRequest createRequest;
    private HotelRoomType mockRoomType;
    private SurchargeRule mockEntity;
    private SurchargeRuleResponse mockResponse;

    @BeforeEach
    void setUp() {
        createRequest = SurchargeRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .agePolicyId((short) 1)
                .ruleType("EXTRA_PERSON")
                .pricingType("PER_NIGHT")
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("150000"))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status("ACTIVE")
                .build();

        mockRoomType = new HotelRoomType();
        mockRoomType.setId(1);
        mockRoomType.setExtraBeds((short) 2);

        mockEntity = SurchargeRule.builder()
                .id(100)
                .ruleType("EXTRA_PERSON")
                .pricingType("PER_NIGHT")
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("150000"))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        mockResponse = SurchargeRuleResponse.builder()
                .id(100)
                .hotelRoomTypeId(1)
                .ruleType("EXTRA_PERSON")
                .adjustmentValue(new BigDecimal("150000"))
                .build();
    }

    @Test
    @DisplayName("Tạo quy tắc phụ thu thành công khi dữ liệu hợp lệ")
    void create_WhenValid_ShouldSucceed() {
        HotelAgePolicy mockAgePolicy = new HotelAgePolicy();
        mockAgePolicy.setId((short) 1);

        when(hotelRoomTypeService.getEntityById(1)).thenReturn(mockRoomType);
        when(hotelAgePolicyService.getEntityById((short) 1)).thenReturn(mockAgePolicy);
        when(surchargeRuleRepository.existsOverlappingRule(any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(surchargeRuleMapper.toEntity(createRequest)).thenReturn(mockEntity);
        when(surchargeRuleRepository.save(mockEntity)).thenReturn(mockEntity);
        when(surchargeRuleMapper.toResponse(mockEntity)).thenReturn(mockResponse);

        SurchargeRuleResponse result = surchargeRuleService.create(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100);
        verify(hotelRoomTypeService, times(1)).getEntityById(1);
        verify(hotelAgePolicyService, times(1)).getEntityById((short) 1);
        verify(surchargeRuleRepository, times(1)).save(mockEntity);
    }

    @Test
    @DisplayName("Ném lỗi khi ngày bắt đầu lớn hơn ngày kết thúc")
    void create_WhenStartDateAfterEndDate_ShouldThrowException() {
        createRequest.setStartDate(LocalDate.of(2026, 7, 1));
        createRequest.setEndDate(LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> surchargeRuleService.create(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("Ném lỗi khi phụ thu kê giường phụ cho loại phòng không cho phép extraBeds")
    void create_WhenExtraBedNotAllowed_ShouldThrowException() {
        createRequest.setRuleType("EXTRA_BED");
        mockRoomType.setExtraBeds((short) 0);
        when(hotelRoomTypeService.getEntityById(1)).thenReturn(mockRoomType);

        assertThatThrownBy(() -> surchargeRuleService.create(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SURCHARGE_CONFIG);
    }

    @Test
    @DisplayName("Ném lỗi khi bị trùng lặp dải ngày hiệu lực với quy tắc khác")
    void create_WhenOverlapping_ShouldThrowException() {
        when(hotelRoomTypeService.getEntityById(1)).thenReturn(mockRoomType);
        when(surchargeRuleRepository.existsOverlappingRule(any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> surchargeRuleService.create(createRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SURCHARGE_RULE_OVERLAP);
    }

    @Test
    @DisplayName("Xem chi tiết quy tắc theo ID")
    void getById_WhenFound_ShouldReturnResponse() {
        when(surchargeRuleRepository.findByIdWithDetails(100)).thenReturn(Optional.of(mockEntity));
        when(surchargeRuleMapper.toResponse(mockEntity)).thenReturn(mockResponse);

        SurchargeRuleResponse res = surchargeRuleService.getById(100);
        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(100);
    }

    @Test
    @DisplayName("Xem chi tiết ném lỗi NOT_FOUND khi không tồn tại ID")
    void getById_WhenNotFound_ShouldThrowException() {
        when(surchargeRuleRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surchargeRuleService.getById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SURCHARGE_RULE_NOT_FOUND);
    }

    @Test
    @DisplayName("Cập nhật quy tắc phụ thu thành công")
    void update_WhenValid_ShouldSucceed() {
        SurchargeRuleUpdateRequest updateReq = SurchargeRuleUpdateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleType("EXTRA_PERSON")
                .pricingType("PER_NIGHT")
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("180000"))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status("ACTIVE")
                .build();

        when(surchargeRuleRepository.findByIdAndIsDeletedFalse(100)).thenReturn(Optional.of(mockEntity));
        when(hotelRoomTypeService.getEntityById(1)).thenReturn(mockRoomType);
        when(surchargeRuleRepository.existsOverlappingRule(eq(100), any(), any(), any(), any(), any())).thenReturn(false);
        when(surchargeRuleRepository.save(mockEntity)).thenReturn(mockEntity);
        when(surchargeRuleMapper.toResponse(mockEntity)).thenReturn(mockResponse);

        SurchargeRuleResponse updated = surchargeRuleService.update(100, updateReq);
        assertThat(updated).isNotNull();
        verify(surchargeRuleRepository, times(1)).save(mockEntity);
    }

    @Test
    @DisplayName("Lọc danh sách quy tắc phụ thu có phân trang")
    void filter_ShouldReturnPageResponse() {
        SurchargeRuleSearchDto searchDto = SurchargeRuleSearchDto.builder()
                .page(0)
                .pageSize(10)
                .build();

        org.springframework.data.domain.Page<SurchargeRule> page = new PageImpl<>(List.of(mockEntity));
        when(surchargeRuleRepository.filterSurchargeRules(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(surchargeRuleMapper.toResponseList(any())).thenReturn(List.of(mockResponse));

        PageResponse<SurchargeRuleResponse> pageRes = surchargeRuleService.filter(searchDto);
        assertThat(pageRes).isNotNull();
        assertThat(pageRes.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Xóa hàng loạt quy tắc phụ thu")
    void deleteBatch_ShouldCallRepository() {
        surchargeRuleService.deleteBatch(List.of(1, 2, 3));
        verify(surchargeRuleRepository, times(1)).softDeleteByIds(List.of(1, 2, 3));
    }

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
