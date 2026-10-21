package vn.edu.utc.hotel_booking.modules.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationPolicySnapshotDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationTierDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.NoShowPolicySnapshotDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.ResolvedBookingPolicies;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.service.CancellationPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.NoShowPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideService;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingPolicyResolverServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPolicyResolverServiceTest {

    @Mock
    CancellationPolicyService cancellationPolicyService;

    @Mock
    NoShowPolicyService noShowPolicyService;

    @Mock
    PropertyPolicyOverrideService propertyPolicyOverrideService;

    @Spy
    ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @InjectMocks
    BookingPolicyResolverServiceImpl resolverService;

    CancellationPolicy chainFitCancel;
    CancellationPolicy chainGitCancel;
    NoShowPolicy chainNoShow;

    @BeforeEach
    void setUp() throws Exception {
        List<CancellationTierDto> fitTiers = List.of(
                CancellationTierDto.builder().minDays(7).maxDays(null).penaltyPercent(BigDecimal.ZERO).build(),
                CancellationTierDto.builder().minDays(4).maxDays(6).penaltyPercent(new BigDecimal("50.0")).build(),
                CancellationTierDto.builder().minDays(0).maxDays(3).penaltyPercent(new BigDecimal("100.0")).build()
        );

        chainFitCancel = CancellationPolicy.builder()
                .id(1)
                .policyCode("CANCEL_FIT_STANDARD")
                .name("Chính sách hủy tiêu chuẩn khách lẻ FIT")
                .bookingType(BookingType.FIT)
                .cancellationFeeType(CancellationFeeType.TIERED)
                .tiers(objectMapper.writeValueAsString(fitTiers))
                .freeCancellationBeforeDays(7)
                .defaultPenaltyPercent(new BigDecimal("100.00"))
                .isActive(true)
                .build();
        chainFitCancel.setIsDeleted(false);

        List<CancellationTierDto> gitTiers = List.of(
                CancellationTierDto.builder().minDays(30).maxDays(null).penaltyPercent(BigDecimal.ZERO).build(),
                CancellationTierDto.builder().minDays(15).maxDays(29).penaltyPercent(new BigDecimal("50.0")).build(),
                CancellationTierDto.builder().minDays(0).maxDays(14).penaltyPercent(new BigDecimal("100.0")).build()
        );

        chainGitCancel = CancellationPolicy.builder()
                .id(2)
                .policyCode("CANCEL_GIT_STANDARD")
                .name("Chính sách hủy tiêu chuẩn khách đoàn GIT")
                .bookingType(BookingType.GIT)
                .cancellationFeeType(CancellationFeeType.FULL_DEPOSIT)
                .tiers(objectMapper.writeValueAsString(gitTiers))
                .freeCancellationBeforeDays(30)
                .defaultPenaltyPercent(new BigDecimal("100.00"))
                .isActive(true)
                .build();
        chainGitCancel.setIsDeleted(false);

        chainNoShow = NoShowPolicy.builder()
                .id(1)
                .policyCode("NOSHOW_STANDARD")
                .name("Chính sách vắng mặt tiêu chuẩn toàn chuỗi")
                .bookingType(BookingType.ALL)
                .releaseRoomTime(LocalTime.of(23, 59, 0))
                .penaltyType(NoShowPenaltyType.PERCENT_FIRST_NIGHT_RATE)
                .penaltyValue(new BigDecimal("100.0"))
                .isActive(true)
                .build();
        chainNoShow.setIsDeleted(false);

        lenient().when(propertyPolicyOverrideService.findActiveOverride(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(cancellationPolicyService.resolveDefaultChainPolicy(any()))
                .thenReturn(chainFitCancel);
        lenient().when(noShowPolicyService.resolveDefaultChainPolicy(any()))
                .thenReturn(chainNoShow);
    }

    @Test
    @DisplayName("1. Resolve fallback chain default khi khách sạn không có override")
    void resolvePolicyForBooking_DefaultChain_FIT() {
        Short hotelId = 1;
        Integer roomTypeId = 10;

        ResolvedBookingPolicies resolved = resolverService.resolvePolicyForBooking(hotelId, roomTypeId, BookingType.FIT);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getCancellationPolicy().getPolicyCode()).isEqualTo("CANCEL_FIT_STANDARD");
        assertThat(resolved.getNoShowPolicy().getPolicyCode()).isEqualTo("NOSHOW_STANDARD");
        assertThat(resolved.getCancellationSnapshot().getPolicyCode()).isEqualTo("CANCEL_FIT_STANDARD");
        assertThat(resolved.getNoShowSnapshot().getPenaltyType()).isEqualTo(NoShowPenaltyType.PERCENT_FIRST_NIGHT_RATE);
        assertThat(resolved.getCancellationPolicySnapshotJson()).contains("CANCEL_FIT_STANDARD");
    }

    @Test
    @DisplayName("2. Resolve ưu tiên override theo hạng phòng cụ thể (RoomType override > Hotel override)")
    void resolvePolicyForBooking_RoomTypeOverride_TakesPrecedence() {
        Short hotelId = 1;
        Integer roomTypeId = 10;

        Hotel testHotel = Hotel.builder().name("Khách sạn Hà Nội").build();
        testHotel.setId(hotelId);

        HotelRoomType roomType = HotelRoomType.builder().id(roomTypeId).hotel(testHotel).build();

        CancellationPolicy customCancel = CancellationPolicy.builder()
                .id(99)
                .policyCode("CANCEL_LUXURY_VILLA")
                .name("Hủy phòng Villa đặc thù")
                .bookingType(BookingType.FIT)
                .cancellationFeeType(CancellationFeeType.TIERED)
                .tiers(chainFitCancel.getTiers())
                .freeCancellationBeforeDays(14)
                .defaultPenaltyPercent(new BigDecimal("100.00"))
                .isActive(true)
                .build();
        customCancel.setIsDeleted(false);

        PropertyPolicyOverride roomTypeOverride = PropertyPolicyOverride.builder()
                .id(5)
                .hotel(testHotel)
                .hotelRoomType(roomType)
                .cancellationPolicy(customCancel)
                .overrideReason("Chính sách áp dụng riêng cho Villa sang trọng")
                .isActive(true)
                .build();
        roomTypeOverride.setIsDeleted(false);

        when(propertyPolicyOverrideService.findActiveOverride(hotelId, roomTypeId))
                .thenReturn(Optional.of(roomTypeOverride));

        ResolvedBookingPolicies resolved = resolverService.resolvePolicyForBooking(hotelId, roomTypeId, BookingType.FIT);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getCancellationPolicy().getPolicyCode()).isEqualTo("CANCEL_LUXURY_VILLA");
        assertThat(resolved.getCancellationSnapshot().getPolicyCode()).isEqualTo("CANCEL_LUXURY_VILLA");
    }

    @Test
    @DisplayName("3. Tính toán tỷ lệ phạt hủy FIT theo snapshot bất biến: 10 ngày (0%), 5 ngày (50%), 2 ngày (100%)")
    void calculateRoomCancellationPenaltyRate_TieredFit() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        ResolvedBookingPolicies resolved = resolverService.resolvePolicyForBooking(null, null, BookingType.FIT);
        BookingDetail detail = BookingDetail.builder()
                .checkInDate(checkIn)
                .cancellationPolicySnapshot(resolved.getCancellationPolicySnapshotJson())
                .build();

        // >= 7 ngày -> 0%
        BigDecimal rate10Days = resolverService.calculateRoomCancellationPenaltyRate(detail, LocalDate.now());
        assertThat(rate10Days).isEqualByComparingTo(BigDecimal.ZERO);

        // 5 ngày -> 50%
        BigDecimal rate5Days = resolverService.calculateRoomCancellationPenaltyRate(detail, checkIn.minusDays(5));
        assertThat(rate5Days).isEqualByComparingTo(new BigDecimal("0.50"));

        // 2 ngày -> 100%
        BigDecimal rate2Days = resolverService.calculateRoomCancellationPenaltyRate(detail, checkIn.minusDays(2));
        assertThat(rate2Days).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("4. Tính toán tỷ lệ phạt hủy đoàn GIT (FULL_DEPOSIT) theo snapshot: 35 ngày (0%), 20 ngày (50%), 5 ngày (100%)")
    void calculateRoomCancellationPenaltyRate_FullDepositGit() {
        LocalDate checkIn = LocalDate.now().plusDays(35);

        when(cancellationPolicyService.resolveDefaultChainPolicy(BookingType.GIT))
                .thenReturn(chainGitCancel);

        ResolvedBookingPolicies gitResolved = resolverService.resolvePolicyForBooking(null, null, BookingType.GIT);

        BookingDetail detail = BookingDetail.builder()
                .checkInDate(checkIn)
                .cancellationPolicySnapshot(gitResolved.getCancellationPolicySnapshotJson())
                .build();

        // 35 ngày -> 0%
        BigDecimal rate35Days = resolverService.calculateRoomCancellationPenaltyRate(detail, LocalDate.now());
        assertThat(rate35Days).isEqualByComparingTo(BigDecimal.ZERO);

        // 20 ngày -> 50%
        BigDecimal rate20Days = resolverService.calculateRoomCancellationPenaltyRate(detail, checkIn.minusDays(20));
        assertThat(rate20Days).isEqualByComparingTo(new BigDecimal("0.50"));

        // 5 ngày -> 100%
        BigDecimal rate5Days = resolverService.calculateRoomCancellationPenaltyRate(detail, checkIn.minusDays(5));
        assertThat(rate5Days).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("5. Tính phạt No-Show theo snapshot PERCENT_FIRST_NIGHT_RATE (100% giá đêm đầu)")
    void calculateSingleRoomNoShowPenalty_PercentFirstNight() {
        ResolvedBookingPolicies resolved = resolverService.resolvePolicyForBooking(null, null, BookingType.FIT);

        BookingDetail detail = BookingDetail.builder()
                .noShowPolicySnapshot(resolved.getNoShowPolicySnapshotJson())
                .build();

        BigDecimal firstNightRate = new BigDecimal("1200000.00");
        BigDecimal penalty = resolverService.calculateSingleRoomNoShowPenalty(null, detail, null, firstNightRate);

        assertThat(penalty).isEqualByComparingTo("1200000.00");
    }

    @Test
    @DisplayName("6. Tính phạt No-Show theo snapshot FIXED_AMOUNT (Phạt cố định 500,000 VND)")
    void calculateSingleRoomNoShowPenalty_FixedAmount() {
        NoShowPolicy fixedNoShow = NoShowPolicy.builder()
                .id(10)
                .policyCode("NOSHOW_FIXED_500K")
                .name("Phạt cố định 500k")
                .bookingType(BookingType.ALL)
                .releaseRoomTime(LocalTime.of(23, 59, 0))
                .penaltyType(NoShowPenaltyType.FIXED_AMOUNT)
                .penaltyValue(new BigDecimal("500000.00"))
                .isActive(true)
                .build();
        fixedNoShow.setIsDeleted(false);

        PropertyPolicyOverride override = PropertyPolicyOverride.builder()
                .id(12)
                .noShowPolicy(fixedNoShow)
                .overrideReason("Test fixed penalty")
                .isActive(true)
                .build();
        override.setIsDeleted(false);

        when(propertyPolicyOverrideService.findActiveOverride(any(), any()))
                .thenReturn(Optional.of(override));

        ResolvedBookingPolicies resolved = resolverService.resolvePolicyForBooking((short) 1, 10, BookingType.FIT);

        BookingDetail detail = BookingDetail.builder()
                .noShowPolicySnapshot(resolved.getNoShowPolicySnapshotJson())
                .build();

        BigDecimal firstNightRate = new BigDecimal("2000000.00");
        BigDecimal penalty = resolverService.calculateSingleRoomNoShowPenalty(null, detail, null, firstNightRate);

        assertThat(penalty).isEqualByComparingTo("500000.00");
    }

    @Test
    @DisplayName("7. Fallback an toàn cho đơn hàng cũ không có snapshot (100% đêm đầu)")
    void calculateSingleRoomNoShowPenalty_LegacyFallback() {
        BookingDetail detail = BookingDetail.builder()
                .noShowPolicySnapshot(null)
                .build();

        BigDecimal firstNightRate = new BigDecimal("850000.00");
        BigDecimal penalty = resolverService.calculateSingleRoomNoShowPenalty(null, detail, null, firstNightRate);

        assertThat(penalty).isEqualByComparingTo("850000.00");
    }

    @Test
    @DisplayName("8. Tính tỷ lệ phạt FIRST_NIGHT cho đơn 4 đêm (tương đương 25% tổng phòng)")
    void calculateRoomCancellationPenaltyRate_FirstNightFee() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = checkIn.plusDays(4);

        CancellationPolicySnapshotDto snapshot = CancellationPolicySnapshotDto.builder()
                .cancellationFeeType(CancellationFeeType.FIRST_NIGHT)
                .freeCancellationBeforeDays(3)
                .build();

        BookingDetail detail = BookingDetail.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .cancellationPolicySnapshot(serialize(snapshot))
                .build();

        // 2 ngày trước check-in (< 3 ngày) -> Phạt 1 đêm trên 4 đêm = 1/4 = 0.25
        BigDecimal rate = resolverService.calculateRoomCancellationPenaltyRate(detail, LocalDate.now());
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.2500"));

        // 5 ngày trước check-in (>= 3 ngày) -> Miễn phí = 0
        BigDecimal freeRate = resolverService.calculateRoomCancellationPenaltyRate(detail, checkIn.minusDays(5));
        assertThat(freeRate).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("9. Tính phạt No-Show PERCENT_TOTAL_BOOKING tính đủ Net + Fee + VAT")
    void calculateSingleRoomNoShowPenalty_PercentTotalBooking_IncludesVatAndFee() {
        NoShowPolicySnapshotDto snapshot = NoShowPolicySnapshotDto.builder()
                .penaltyType(NoShowPenaltyType.PERCENT_TOTAL_BOOKING)
                .penaltyValue(new BigDecimal("50.00")) // Phạt 50% tổng tiền phòng
                .build();

        BookingDetail detail = BookingDetail.builder()
                .noShowPolicySnapshot(serialize(snapshot))
                .build();

        BookingDailyRate rate1 = BookingDailyRate.builder()
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(new BigDecimal("50000.00"))
                .vatAmount(new BigDecimal("105000.00"))
                .build();
        BookingDailyRate rate2 = BookingDailyRate.builder()
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(new BigDecimal("50000.00"))
                .vatAmount(new BigDecimal("105000.00"))
                .build();

        BookingRoom room = BookingRoom.builder()
                .dailyRates(List.of(rate1, rate2)) // Tổng = 2,310,000 đ
                .build();

        // 50% của 2,310,000 = 1,155,000 đ
        BigDecimal penalty = resolverService.calculateSingleRoomNoShowPenalty(room, detail, null, new BigDecimal("1155000.00"));
        assertThat(penalty).isEqualByComparingTo("1155000.00");
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
