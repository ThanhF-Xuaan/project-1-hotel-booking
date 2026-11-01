package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleUpdateRequest;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RatePlanCacheTest.TestCacheConfig.class,
        PricingRuleServiceImpl.class
})
@DisplayName("Sprint 4 — Task 2: Redis Cache cho Rate Plans Test")
class RatePlanCacheTest {

    @Configuration
    @EnableCaching
    static class TestCacheConfig {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("ratePlans");
        }

        @Bean
        public PricingRuleRepository pricingRuleRepository() {
            return mock(PricingRuleRepository.class);
        }

        @Bean
        public HotelRoomTypeRepository hotelRoomTypeRepository() {
            return mock(HotelRoomTypeRepository.class);
        }

        @Bean
        public PricingRuleTypeRepository pricingRuleTypeRepository() {
            return mock(PricingRuleTypeRepository.class);
        }

        @Bean
        public HolidayCalendarRepository holidayCalendarRepository() {
            return mock(HolidayCalendarRepository.class);
        }

        @Bean
        public PricingRuleMapper pricingRuleMapper() {
            return mock(PricingRuleMapper.class);
        }
    }

    @Autowired
    private PricingRuleService pricingRuleService;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private HotelRoomTypeRepository hotelRoomTypeRepository;

    @Autowired
    private PricingRuleTypeRepository pricingRuleTypeRepository;

    @Autowired
    private PricingRuleMapper pricingRuleMapper;

    @Autowired
    private CacheManager cacheManager;

    private PricingRule sampleRule;
    private PricingRuleResponse sampleResponse;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("ratePlans") != null) {
            cacheManager.getCache("ratePlans").clear();
        }
        reset(pricingRuleRepository, hotelRoomTypeRepository, pricingRuleTypeRepository, pricingRuleMapper);

        RoomType rt = RoomType.builder().id((short) 1).name("Standard Room").code("STD").build();
        HotelRoomType hrt = HotelRoomType.builder().id(1).roomType(rt).build();
        PricingRuleType prt = PricingRuleType.builder().code("WEEKEND").displayName("Cuối tuần").priority((short) 10).build();

        sampleRule = PricingRule.builder()
                .id(100)
                .hotelRoomType(hrt)
                .ruleType(prt)
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(15.00))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status("ACTIVE")
                .build();

        sampleResponse = PricingRuleResponse.builder()
                .id(100)
                .hotelRoomTypeId(1)
                .roomTypeName("Standard Room")
                .ruleTypeCode("WEEKEND")
                .ruleTypeName("Cuối tuần")
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(15.00))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Cache MISS lần đầu gọi DB; Cache HIT lần 2 lấy từ Cache không gọi DB")
    void getPricingRuleById_cacheHitAndMiss() {
        when(pricingRuleRepository.findByIdWithDetails(100)).thenReturn(Optional.of(sampleRule));
        when(pricingRuleMapper.toResponse(sampleRule)).thenReturn(sampleResponse);

        // Lần 1: Cache MISS → Gọi Repository
        PricingRuleResponse res1 = pricingRuleService.getPricingRuleById(100);
        assertThat(res1).isNotNull();
        assertThat(res1.getId()).isEqualTo(100);
        verify(pricingRuleRepository, times(1)).findByIdWithDetails(100);

        // Lần 2: Cache HIT → Trả về từ Cache, KHÔNG gọi lại Repository
        PricingRuleResponse res2 = pricingRuleService.getPricingRuleById(100);
        assertThat(res2).isNotNull();
        assertThat(res2.getId()).isEqualTo(100);
        verify(pricingRuleRepository, times(1)).findByIdWithDetails(100); // Vẫn chỉ là 1 lần
    }

    @Test
    @DisplayName("Create Rate Plan evicts cache → Lần đọc tiếp theo là Cache MISS")
    void createPricingRule_evictsCache() {
        when(pricingRuleRepository.findByIdWithDetails(100)).thenReturn(Optional.of(sampleRule));
        when(pricingRuleMapper.toResponse(sampleRule)).thenReturn(sampleResponse);

        // Nạp vào Cache
        pricingRuleService.getPricingRuleById(100);
        verify(pricingRuleRepository, times(1)).findByIdWithDetails(100);

        // Tạo rule mới
        PricingRuleCreateRequest createReq = PricingRuleCreateRequest.builder()
                .hotelRoomTypeId(1)
                .ruleTypeCode("SEASONAL")
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(20))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .status("ACTIVE")
                .build();

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleRule.getHotelRoomType()));
        when(pricingRuleTypeRepository.findByCode("SEASONAL")).thenReturn(Optional.of(sampleRule.getRuleType()));
        when(pricingRuleMapper.toEntity(createReq)).thenReturn(sampleRule);
        when(pricingRuleRepository.save(any())).thenReturn(sampleRule);

        pricingRuleService.createPricingRule(createReq);

        // Đọc lại ID 100 → Cache đã bị evict → Gọi lại Repository
        pricingRuleService.getPricingRuleById(100);
        verify(pricingRuleRepository, times(2)).findByIdWithDetails(100);
    }

    @Test
    @DisplayName("Update Rate Plan evicts cache")
    void updatePricingRule_evictsCache() {
        when(pricingRuleRepository.findByIdWithDetails(100)).thenReturn(Optional.of(sampleRule));
        when(pricingRuleMapper.toResponse(sampleRule)).thenReturn(sampleResponse);

        // Nạp vào cache (Lần 1)
        pricingRuleService.getPricingRuleById(100);
        verify(pricingRuleRepository, times(1)).findByIdWithDetails(100);

        // Cập nhật rule (updatePricingRule tự gọi findByIdWithDetails(100) lần 2)
        PricingRuleUpdateRequest updateReq = PricingRuleUpdateRequest.builder()
                .adjustmentType("FIXED")
                .adjustmentValue(BigDecimal.valueOf(100000))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .status("ACTIVE")
                .build();

        when(pricingRuleRepository.save(any())).thenReturn(sampleRule);
        pricingRuleService.updatePricingRule(100, updateReq);

        // Đọc lại ID 100 → Cache đã bị evict → Gọi lại Repository (Lần 3)
        pricingRuleService.getPricingRuleById(100);
        verify(pricingRuleRepository, times(3)).findByIdWithDetails(100);
    }

    @Test
    @DisplayName("Delete Rate Plans evicts cache")
    void deletePricingRules_evictsCache() {
        when(pricingRuleRepository.findByIdWithDetails(100)).thenReturn(Optional.of(sampleRule));
        when(pricingRuleMapper.toResponse(sampleRule)).thenReturn(sampleResponse);

        // Nạp vào cache
        pricingRuleService.getPricingRuleById(100);
        verify(pricingRuleRepository, times(1)).findByIdWithDetails(100);

        // Xóa rules
        pricingRuleService.deletePricingRules(List.of(100));

        // Đọc lại ID 100 → Cache đã bị evict → Gọi lại Repository
        pricingRuleService.getPricingRuleById(100);
        verify(pricingRuleRepository, times(2)).findByIdWithDetails(100);
    }
}
