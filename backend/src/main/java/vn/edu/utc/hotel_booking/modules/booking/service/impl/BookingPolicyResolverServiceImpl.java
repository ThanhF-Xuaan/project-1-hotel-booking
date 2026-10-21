package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationPolicySnapshotDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationTierDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.NoShowPolicySnapshotDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.ResolvedBookingPolicies;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingPolicyResolverService;
import vn.edu.utc.hotel_booking.modules.booking.service.CancellationPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.NoShowPolicyService;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingPolicyResolverServiceImpl implements BookingPolicyResolverService {

    private final CancellationPolicyService cancellationPolicyService;
    private final NoShowPolicyService noShowPolicyService;
    private final PropertyPolicyOverrideService propertyPolicyOverrideService;
    private final ObjectMapper objectMapper;

    @Override
    public ResolvedBookingPolicies resolvePolicyForBooking(Short hotelId, Integer hotelRoomTypeId, BookingType bookingType) {
        BookingType effectiveBookingType = (bookingType != null) ? bookingType : BookingType.FIT;

        // 1. Tìm override tại cơ sở (Ưu tiên hạng phòng trước, sau đó tới cấp toàn khách sạn - đóng gói trong service)
        Optional<PropertyPolicyOverride> overrideOpt = propertyPolicyOverrideService.findActiveOverride(hotelId, hotelRoomTypeId);

        // 2. Resolve Chính Sách Hủy Phòng (Cancellation Policy)
        CancellationPolicy cancellationPolicy;
        if (overrideOpt.isPresent() && overrideOpt.get().getCancellationPolicy() != null) {
            cancellationPolicy = overrideOpt.get().getCancellationPolicy();
            if (!cancellationPolicy.getIsActive() || cancellationPolicy.getIsDeleted()) {
                throw new AppException(ErrorCode.POLICY_NOT_ACTIVE_OR_DELETED);
            }
        } else {
            cancellationPolicy = cancellationPolicyService.resolveDefaultChainPolicy(effectiveBookingType);
        }

        Integer effectiveFreeDays = cancellationPolicy.getFreeCancellationBeforeDays();
        // Đối với Non-Tiered: nếu có override số ngày thì áp dụng
        if (cancellationPolicy.getCancellationFeeType() != CancellationFeeType.TIERED &&
                overrideOpt.isPresent() && overrideOpt.get().getFreeCancellationBeforeDaysOverride() != null) {
            effectiveFreeDays = overrideOpt.get().getFreeCancellationBeforeDaysOverride();
        }

        List<CancellationTierDto> tiersList = parseTiersJson(cancellationPolicy.getTiers());

        CancellationPolicySnapshotDto cancelSnapshot = CancellationPolicySnapshotDto.builder()
                .policyId(cancellationPolicy.getId())
                .policyCode(cancellationPolicy.getPolicyCode())
                .name(cancellationPolicy.getName())
                .bookingType(cancellationPolicy.getBookingType())
                .freeCancellationBeforeDays(effectiveFreeDays)
                .cancellationFeeType(cancellationPolicy.getCancellationFeeType())
                .defaultPenaltyPercent(cancellationPolicy.getDefaultPenaltyPercent())
                .tiers(tiersList)
                .overrideApplied(overrideOpt.isPresent() && (overrideOpt.get().getCancellationPolicy() != null || overrideOpt.get().getFreeCancellationBeforeDaysOverride() != null))
                .overrideReason(overrideOpt.map(PropertyPolicyOverride::getOverrideReason).orElse(null))
                .build();

        String cancelSnapshotJson = serializeJson(cancelSnapshot);

        // 3. Resolve Chính Sách No-Show (No-Show Policy)
        NoShowPolicy noShowPolicy;
        if (overrideOpt.isPresent() && overrideOpt.get().getNoShowPolicy() != null) {
            noShowPolicy = overrideOpt.get().getNoShowPolicy();
            if (!noShowPolicy.getIsActive() || noShowPolicy.getIsDeleted()) {
                throw new AppException(ErrorCode.POLICY_NOT_ACTIVE_OR_DELETED);
            }
        } else {
            noShowPolicy = noShowPolicyService.resolveDefaultChainPolicy(effectiveBookingType);
        }

        LocalTime effectiveReleaseTime = noShowPolicy.getReleaseRoomTime();
        if (overrideOpt.isPresent() && overrideOpt.get().getNoShowReleaseTimeOverride() != null) {
            effectiveReleaseTime = overrideOpt.get().getNoShowReleaseTimeOverride();
        }

        NoShowPolicySnapshotDto noShowSnapshot = NoShowPolicySnapshotDto.builder()
                .policyId(noShowPolicy.getId())
                .policyCode(noShowPolicy.getPolicyCode())
                .name(noShowPolicy.getName())
                .bookingType(noShowPolicy.getBookingType())
                .releaseRoomTime(effectiveReleaseTime)
                .penaltyType(noShowPolicy.getPenaltyType())
                .penaltyValue(noShowPolicy.getPenaltyValue())
                .overrideApplied(overrideOpt.isPresent() && (overrideOpt.get().getNoShowPolicy() != null || overrideOpt.get().getNoShowReleaseTimeOverride() != null))
                .overrideReason(overrideOpt.map(PropertyPolicyOverride::getOverrideReason).orElse(null))
                .build();

        String noShowSnapshotJson = serializeJson(noShowSnapshot);

        return ResolvedBookingPolicies.builder()
                .cancellationPolicy(cancellationPolicy)
                .noShowPolicy(noShowPolicy)
                .cancellationPolicySnapshotJson(cancelSnapshotJson)
                .noShowPolicySnapshotJson(noShowSnapshotJson)
                .cancellationSnapshot(cancelSnapshot)
                .noShowSnapshot(noShowSnapshot)
                .build();
    }

    @Override
    public BigDecimal calculateRoomCancellationPenaltyRate(BookingDetail detail, LocalDate cancelDate) {
        if (detail == null) {
            return BigDecimal.ZERO;
        }
        if (cancelDate == null) {
            cancelDate = LocalDate.now();
        }

        // Tương thích ngược: Nếu đơn cũ chưa có snapshot -> Thử tra cứu chính sách mặc định chuỗi đang active
        if (detail.getCancellationPolicySnapshot() == null || detail.getCancellationPolicySnapshot().isBlank()) {
            if (detail.getCheckInDate() == null) {
                return BigDecimal.ONE;
            }
            Booking booking = detail.getBooking();
            BookingType type = (booking != null && booking.getBookingType() != null) ? booking.getBookingType() : BookingType.FIT;
            CancellationPolicy defaultPolicy = cancellationPolicyService.resolveDefaultChainPolicy(type);
            if (defaultPolicy != null && defaultPolicy.getTiers() != null && !defaultPolicy.getTiers().isBlank()) {
                List<CancellationTierDto> tiers = parseTiersJson(defaultPolicy.getTiers());
                long daysBefore = ChronoUnit.DAYS.between(cancelDate, detail.getCheckInDate());
                if (daysBefore < 0) daysBefore = 0;
                for (CancellationTierDto tier : tiers) {
                    long min = tier.getMinDays() != null ? tier.getMinDays() : 0;
                    boolean matchMin = daysBefore >= min;
                    boolean matchMax = (tier.getMaxDays() == null) || (daysBefore <= tier.getMaxDays());
                    if (matchMin && matchMax) {
                        BigDecimal percent = tier.getPenaltyPercent() != null ? tier.getPenaltyPercent() : BigDecimal.ZERO;
                        return percent.divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP);
                    }
                }
                return BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
        }

        // Đơn đã có snapshot: Tính toán chặt chẽ theo snapshot đã chốt lúc đặt phòng
        CancellationPolicySnapshotDto snapshot = deserializeJson(detail.getCancellationPolicySnapshot(), CancellationPolicySnapshotDto.class);
        if (snapshot == null) {
            return BigDecimal.ONE;
        }

        long daysBeforeCheckIn = ChronoUnit.DAYS.between(cancelDate, detail.getCheckInDate());
        if (daysBeforeCheckIn < 0) {
            daysBeforeCheckIn = 0;
        }

        CancellationFeeType feeType = snapshot.getCancellationFeeType() != null ? snapshot.getCancellationFeeType() : CancellationFeeType.TIERED;

        switch (feeType) {
            case TIERED:
            case FULL_DEPOSIT:
                if (snapshot.getTiers() != null && !snapshot.getTiers().isEmpty()) {
                    for (CancellationTierDto tier : snapshot.getTiers()) {
                        long min = tier.getMinDays() != null ? tier.getMinDays() : 0;
                        boolean matchMin = daysBeforeCheckIn >= min;
                        boolean matchMax = (tier.getMaxDays() == null) || (daysBeforeCheckIn <= tier.getMaxDays());
                        if (matchMin && matchMax) {
                            BigDecimal percent = tier.getPenaltyPercent() != null ? tier.getPenaltyPercent() : BigDecimal.ZERO;
                            return percent.divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP);
                        }
                    }
                }
                // Nếu vượt quá mốc cao nhất của tiers -> Miễn phí hủy
                return BigDecimal.ZERO;

            case PERCENT:
                if (snapshot.getFreeCancellationBeforeDays() != null && daysBeforeCheckIn >= snapshot.getFreeCancellationBeforeDays()) {
                    return BigDecimal.ZERO;
                }
                BigDecimal defaultPercent = snapshot.getDefaultPenaltyPercent() != null ? snapshot.getDefaultPenaltyPercent() : new BigDecimal("100.00");
                return defaultPercent.divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP);

            case FIRST_NIGHT:
                if (snapshot.getFreeCancellationBeforeDays() != null && daysBeforeCheckIn >= snapshot.getFreeCancellationBeforeDays()) {
                    return BigDecimal.ZERO;
                }
                long totalNights = (detail.getCheckInDate() != null && detail.getCheckOutDate() != null)
                        ? Math.max(1, ChronoUnit.DAYS.between(detail.getCheckInDate(), detail.getCheckOutDate()))
                        : 1;
                return BigDecimal.ONE.divide(BigDecimal.valueOf(totalNights), 4, RoundingMode.HALF_UP);

            case FIXED_AMOUNT:
                if (snapshot.getFreeCancellationBeforeDays() != null && daysBeforeCheckIn >= snapshot.getFreeCancellationBeforeDays()) {
                    return BigDecimal.ZERO;
                }
                if (snapshot.getDefaultPenaltyPercent() != null && snapshot.getDefaultPenaltyPercent().compareTo(BigDecimal.ZERO) > 0) {
                    return snapshot.getDefaultPenaltyPercent().divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP);
                }
                return BigDecimal.ONE;

            default:
                if (snapshot.getFreeCancellationBeforeDays() != null && daysBeforeCheckIn >= snapshot.getFreeCancellationBeforeDays()) {
                    return BigDecimal.ZERO;
                }
                return BigDecimal.ONE;
        }
    }

    @Override
    public String getCancellationPolicyDescription(BookingDetail detail, LocalDate cancelDate) {
        if (detail == null) {
            return "Chính sách hủy tiêu chuẩn";
        }
        if (cancelDate == null) {
            cancelDate = LocalDate.now();
        }

        long daysUntilCheckIn = detail.getCheckInDate() != null
                ? ChronoUnit.DAYS.between(cancelDate, detail.getCheckInDate())
                : 0;

        if (detail.getCancellationPolicySnapshot() == null || detail.getCancellationPolicySnapshot().isBlank()) {
            BigDecimal rate = calculateRoomCancellationPenaltyRate(detail, cancelDate);
            BigDecimal percent = rate.multiply(new BigDecimal("100.00")).setScale(0, RoundingMode.HALF_UP);
            return String.format("Chính sách hủy tiêu chuẩn (Áp dụng: Phạt %s%% khi hủy trước %d ngày).", percent.toPlainString(), Math.max(0, daysUntilCheckIn));
        }

        CancellationPolicySnapshotDto snapshot = deserializeJson(detail.getCancellationPolicySnapshot(), CancellationPolicySnapshotDto.class);
        if (snapshot == null) {
            return "Chính sách hủy theo snapshot đơn đặt";
        }

        BigDecimal rate = calculateRoomCancellationPenaltyRate(detail, cancelDate);
        BigDecimal percent = rate.multiply(new BigDecimal("100.00")).setScale(0, RoundingMode.HALF_UP);

        return String.format("%s (Áp dụng: Phạt %s%% khi hủy trước %d ngày).", snapshot.getName(), percent.toPlainString(), Math.max(0, daysUntilCheckIn));
    }

    @Override
    public BigDecimal calculateSingleRoomNoShowPenalty(BookingRoom room, BookingDetail detail, Booking booking, BigDecimal firstNightRate) {
        if (firstNightRate == null) {
            firstNightRate = BigDecimal.ZERO;
        }

        // Tương thích ngược: Đơn cũ chưa có snapshot No-Show -> Mặc định 100% đêm đầu
        if (detail == null || detail.getNoShowPolicySnapshot() == null || detail.getNoShowPolicySnapshot().isBlank()) {
            return firstNightRate;
        }

        NoShowPolicySnapshotDto snapshot = deserializeJson(detail.getNoShowPolicySnapshot(), NoShowPolicySnapshotDto.class);
        if (snapshot == null) {
            return firstNightRate;
        }

        NoShowPenaltyType penaltyType = snapshot.getPenaltyType() != null ? snapshot.getPenaltyType() : NoShowPenaltyType.PERCENT_FIRST_NIGHT_RATE;
        BigDecimal penaltyValue = snapshot.getPenaltyValue() != null ? snapshot.getPenaltyValue() : new BigDecimal("100.00");

        switch (penaltyType) {
            case PERCENT_FIRST_NIGHT_RATE:
                return firstNightRate.multiply(penaltyValue).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

            case PERCENT_TOTAL_BOOKING:
                // Tính % trên tổng tiền của phòng (Bao gồm Net + Fee + VAT)
                BigDecimal roomTotal = room != null ? calculateRoomTotal(room) : firstNightRate;
                return roomTotal.multiply(penaltyValue).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);

            case FULL_DEPOSIT:
                // Đối với đơn phạt toàn bộ cọc/tiền phòng, tính theo toàn bộ tiền phòng (Net + Fee + VAT)
                return room != null ? calculateRoomTotal(room) : firstNightRate;

            case FIXED_AMOUNT:
                return penaltyValue;

            default:
                return firstNightRate;
        }
    }

    private BigDecimal calculateRoomTotal(BookingRoom room) {
        if (room == null || room.getDailyRates() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BookingDailyRate dr : room.getDailyRates()) {
            BigDecimal net = dr.getNetPrice() != null ? dr.getNetPrice() : BigDecimal.ZERO;
            BigDecimal fee = dr.getServiceFeeAmount() != null ? dr.getServiceFeeAmount() : BigDecimal.ZERO;
            BigDecimal vat = dr.getVatAmount() != null ? dr.getVatAmount() : BigDecimal.ZERO;
            total = total.add(net).add(fee).add(vat);
        }
        return total;
    }

    private List<CancellationTierDto> parseTiersJson(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CancellationTierDto>>() {});
        } catch (Exception e) {
            log.error("Lỗi parse tiers JSON: {}", json, e);
            return Collections.emptyList();
        }
    }

    private String serializeJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Lỗi serialize snapshot object: {}", object, e);
            return "{}";
        }
    }

    private <T> T deserializeJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Lỗi deserialize snapshot JSON: {}", json, e);
            return null;
        }
    }
}
