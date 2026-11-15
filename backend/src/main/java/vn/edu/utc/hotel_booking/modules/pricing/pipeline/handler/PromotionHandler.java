package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountConditionDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountTierDto;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.AbstractPricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.VoucherService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PromotionHandler extends AbstractPricingHandler {

    private final DiscountRuleRepository discountRuleRepository;
    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PromotionHandler(DiscountRuleRepository discountRuleRepository,
                            VoucherService voucherService,
                            ObjectMapper objectMapper) {
        this.discountRuleRepository = discountRuleRepository;
        this.voucherService = voucherService;
        this.voucherRepository = null;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public PromotionHandler(DiscountRuleRepository discountRuleRepository, VoucherService voucherService) {
        this(discountRuleRepository, voucherService, new ObjectMapper());
    }

    public PromotionHandler(DiscountRuleRepository discountRuleRepository, VoucherRepository voucherRepository) {
        this.discountRuleRepository = discountRuleRepository;
        this.voucherService = null;
        this.voucherRepository = voucherRepository;
        this.objectMapper = new ObjectMapper();
    }

    public PromotionHandler(DiscountRuleRepository discountRuleRepository,
                            VoucherRepository voucherRepository,
                            ObjectMapper objectMapper) {
        this.discountRuleRepository = discountRuleRepository;
        this.voucherService = null;
        this.voucherRepository = voucherRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void handle(PricingContext context) {
        if (context.getRequest() == null || context.getHotelRoomType() == null) {
            executeNext(context);
            return;
        }

        // 1. Quét discount_rules tự động
        List<DiscountRule> activeDiscounts = Collections.emptyList();
        if (discountRuleRepository != null && context.getHotelRoomType().getId() != null
                && context.getRequest().getCheckInDate() != null && context.getRequest().getCheckOutDate() != null) {
            activeDiscounts = discountRuleRepository.findActiveDiscountsForRoomType(
                    context.getHotelRoomType().getId(),
                    context.getRequest().getCheckInDate(),
                    context.getRequest().getCheckOutDate()
            );
        }

        // 2. Tra cứu Voucher nếu khách có nhập/chọn mã (ưu tiên qua VoucherService tuân thủ aggregate root)
        String voucherCode = context.getRequest().getVoucherCode();
        Optional<Voucher> voucherOpt = Optional.empty();
        if (voucherCode != null && !voucherCode.trim().isBlank()) {
            Short hotelId = context.getHotel() != null ? context.getHotel().getId() : null;
            if (voucherService != null) {
                voucherOpt = voucherService.findValidVoucherForPricing(voucherCode.trim(), hotelId, OffsetDateTime.now());
            } else if (voucherRepository != null) {
                voucherOpt = voucherRepository.findValidVoucher(voucherCode.trim().toUpperCase(), hotelId, OffsetDateTime.now());
            }

            voucherOpt.ifPresent(v -> {
                context.setAppliedVoucher(v);
                context.setAppliedPromotionName(v.getTitle());
            });
        }

        // 3. Tính tổng số đêm lưu trú của lượt đặt phòng
        long totalNights = 1;
        if (context.getRequest().getCheckInDate() != null && context.getRequest().getCheckOutDate() != null) {
            long nightsBetween = ChronoUnit.DAYS.between(context.getRequest().getCheckInDate(), context.getRequest().getCheckOutDate());
            totalNights = nightsBetween > 0 ? nightsBetween : 1;
        }

        // 4. Bước 1: Tính chiết khấu tự động (Auto Discount) theo từng đêm
        BigDecimal totalStayAdjustedRate = BigDecimal.ZERO;
        final long stayNights = totalNights;
        if (context.getDailyRates() != null) {
            for (DailyRateContext daily : context.getDailyRates()) {
                LocalDate date = daily.getDate();
                BigDecimal adjustedRate = daily.getAdjustedRate() != null ? daily.getAdjustedRate() : BigDecimal.ZERO;
                totalStayAdjustedRate = totalStayAdjustedRate.add(adjustedRate);

                BigDecimal autoDiscountAmt = BigDecimal.ZERO;
                List<DiscountRule> matchingAutoRules = activeDiscounts.stream()
                        .filter(d -> !date.isBefore(d.getStartDate()) && !date.isAfter(d.getEndDate()))
                        .filter(d -> matchesConditions(d, stayNights))
                        .toList();

                if (!matchingAutoRules.isEmpty()) {
                    Optional<DiscountRule> bestAutoRuleOpt = matchingAutoRules.stream().max(
                            Comparator.<DiscountRule, Integer>comparing(
                                    d -> d.getRuleType() != null && d.getRuleType().getPriority() != null ? (int) d.getRuleType().getPriority() : 0
                            ).thenComparing(
                                    d -> calculateDiscountAmount(d, adjustedRate, stayNights)
                            )
                    );

                    if (bestAutoRuleOpt.isPresent()) {
                        DiscountRule bestAutoRule = bestAutoRuleOpt.get();
                        autoDiscountAmt = calculateDiscountAmount(bestAutoRule, adjustedRate, stayNights);
                        daily.setAppliedAutoDiscountRuleCode(bestAutoRule.getRuleType() != null ? bestAutoRule.getRuleType().getCode() : "DISCOUNT");
                    }
                }
                daily.setAutoDiscountAmount(autoDiscountAmt);
            }
        }

        // 5. Bước 2: Tính Voucher người dùng (Áp dụng 1 lần duy nhất cho toàn bộ đợt lưu trú)
        BigDecimal totalVoucherDiscount = BigDecimal.ZERO;
        if (voucherOpt.isPresent()) {
            Voucher voucher = voucherOpt.get();
            boolean satisfiesMinOrder = voucher.getMinOrderAmount() == null
                    || totalStayAdjustedRate.compareTo(voucher.getMinOrderAmount()) >= 0;

            if (satisfiesMinOrder) {
                if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
                    totalVoucherDiscount = totalStayAdjustedRate.multiply(voucher.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    if (voucher.getMaxDiscountAmount() != null && totalVoucherDiscount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                        totalVoucherDiscount = voucher.getMaxDiscountAmount();
                    }
                } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
                    totalVoucherDiscount = voucher.getDiscountValue();
                }

                if (totalVoucherDiscount.compareTo(totalStayAdjustedRate) > 0) {
                    totalVoucherDiscount = totalStayAdjustedRate;
                }
            }
        }

        // 6. Bước 3: Phân bổ khoản giảm voucher vào từng đêm và tổng hợp Net Room Rate
        if (context.getDailyRates() != null && !context.getDailyRates().isEmpty()) {
            int numNights = context.getDailyRates().size();
            BigDecimal allocatedVoucherSoFar = BigDecimal.ZERO;

            for (int i = 0; i < numNights; i++) {
                DailyRateContext daily = context.getDailyRates().get(i);
                BigDecimal adjustedRate = daily.getAdjustedRate() != null ? daily.getAdjustedRate() : BigDecimal.ZERO;
                BigDecimal autoDiscountAmt = daily.getAutoDiscountAmount() != null ? daily.getAutoDiscountAmount() : BigDecimal.ZERO;

                BigDecimal nightVoucherDiscount = BigDecimal.ZERO;
                if (totalVoucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    if (i == numNights - 1) {
                        nightVoucherDiscount = totalVoucherDiscount.subtract(allocatedVoucherSoFar);
                    } else if (totalStayAdjustedRate.compareTo(BigDecimal.ZERO) > 0) {
                        nightVoucherDiscount = totalVoucherDiscount.multiply(adjustedRate)
                                .divide(totalStayAdjustedRate, 2, RoundingMode.HALF_UP);
                        allocatedVoucherSoFar = allocatedVoucherSoFar.add(nightVoucherDiscount);
                    }
                }

                if (nightVoucherDiscount.compareTo(BigDecimal.ZERO) < 0) {
                    nightVoucherDiscount = BigDecimal.ZERO;
                }

                daily.setVoucherDiscountAmount(nightVoucherDiscount);
                if (voucherOpt.isPresent() && nightVoucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    daily.setAppliedVoucherCode(voucherOpt.get().getVoucherCode());
                }

                BigDecimal totalDiscount = autoDiscountAmt.add(nightVoucherDiscount);
                if (totalDiscount.compareTo(adjustedRate) > 0) {
                    totalDiscount = adjustedRate;
                }

                BigDecimal netRoomRate = adjustedRate.subtract(totalDiscount);
                daily.setTotalDiscountAmount(totalDiscount);
                daily.setDiscountAmount(totalDiscount);
                daily.setNetRoomRate(netRoomRate);
            }
        }

        executeNext(context);
    }

    private boolean matchesConditions(DiscountRule rule, long totalNights) {
        if (rule.getConditions() == null || rule.getConditions().trim().isEmpty() || "{}".equals(rule.getConditions().trim())) {
            return true;
        }

        try {
            DiscountConditionDto condition = objectMapper.readValue(rule.getConditions(), DiscountConditionDto.class);
            if (condition == null) {
                return true;
            }

            // 1. Kiểm tra mảng tiers nếu có (phân tầng số đêm lưu trú)
            if (condition.getTiers() != null && !condition.getTiers().isEmpty()) {
                return condition.getTiers().stream().anyMatch(tier ->
                        tier.getMinNights() != null && totalNights >= tier.getMinNights()
                                && (tier.getMaxNights() == null || totalNights <= tier.getMaxNights())
                );
            }

            // 2. Kiểm tra minNights đơn lẻ
            if (condition.getMinNights() != null && totalNights < condition.getMinNights()) {
                return false;
            }

            // 3. Kiểm tra maxNights đơn lẻ
            if (condition.getMaxNights() != null && totalNights > condition.getMaxNights()) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Lỗi kiểm tra conditions của discount rule ID {}: {}", rule.getId(), e.getMessage());
            return true;
        }
    }

    private BigDecimal calculateDiscountAmount(DiscountRule rule, BigDecimal base, long totalNights) {
        String discountType = rule.getDiscountType();
        BigDecimal discountValue = rule.getDiscountValue();

        // Nếu có cấu hình tiers, tìm tier khớp với totalNights
        if (rule.getConditions() != null && !rule.getConditions().trim().isEmpty() && !"{}".equals(rule.getConditions().trim())) {
            try {
                DiscountConditionDto condition = objectMapper.readValue(rule.getConditions(), DiscountConditionDto.class);
                if (condition != null && condition.getTiers() != null && !condition.getTiers().isEmpty()) {
                    Optional<DiscountTierDto> matchingTier = condition.getTiers().stream()
                            .filter(tier -> tier.getMinNights() != null && totalNights >= tier.getMinNights()
                                    && (tier.getMaxNights() == null || totalNights <= tier.getMaxNights()))
                            .findFirst();

                    if (matchingTier.isPresent()) {
                        DiscountTierDto tier = matchingTier.get();
                        if (tier.getDiscountType() != null && !tier.getDiscountType().isBlank()) {
                            discountType = tier.getDiscountType();
                        }
                        if (tier.getDiscountValue() != null) {
                            discountValue = tier.getDiscountValue();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi đọc tiers từ conditions của discount rule ID {}: {}", rule.getId(), e.getMessage());
            }
        }

        if (discountValue == null) {
            return BigDecimal.ZERO;
        }

        if ("PERCENT".equalsIgnoreCase(discountType)) {
            return base.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return discountValue;
    }
}
