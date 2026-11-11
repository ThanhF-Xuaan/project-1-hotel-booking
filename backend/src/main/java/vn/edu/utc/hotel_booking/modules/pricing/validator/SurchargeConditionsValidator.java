package vn.edu.utc.hotel_booking.modules.pricing.validator;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.pricing.dto.condition.SurchargeConditionsDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.condition.SurchargeTierDto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Component
public class SurchargeConditionsValidator {

    public void validate(String ruleType, SurchargeConditionsDto conditions, LocalTime hotelStandardTime) {
        if (conditions == null || conditions.getTiers() == null || conditions.getTiers().isEmpty()) {
            return;
        }

        List<SurchargeTierDto> tiers = conditions.getTiers();

        for (int i = 0; i < tiers.size(); i++) {
            SurchargeTierDto tier = tiers.get(i);
            if (!StringUtils.hasText(tier.getFromTime()) || !StringUtils.hasText(tier.getToTime())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Khung giờ bậc phụ thu thứ " + (i + 1) + " không được để trống");
            }

            LocalTime from;
            LocalTime to;
            try {
                from = LocalTime.parse(tier.getFromTime());
                to = LocalTime.parse(tier.getToTime());
            } catch (DateTimeParseException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Bậc thứ " + (i + 1) + ": Định dạng giờ không hợp lệ (phải theo chuẩn HH:mm)");
            }

            if (!from.isBefore(to)) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Bậc thứ " + (i + 1) + ": Giờ bắt đầu (" + tier.getFromTime() + ") phải trước giờ kết thúc (" + tier.getToTime() + ")");
            }

            if ("PERCENT".equalsIgnoreCase(tier.getAdjustmentType())
                    && tier.getAdjustmentValue() != null
                    && tier.getAdjustmentValue().compareTo(new BigDecimal("100.0")) > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Bậc thứ " + (i + 1) + ": Tỷ lệ phụ thu phần trăm không được vượt quá 100%");
            }

            // Ràng buộc nghiêm ngặt với giờ quy chuẩn của Khách sạn Cơ sở (Hạng phòng không được phép override)
            if (hotelStandardTime != null) {
                if ("EARLY_CHECKIN".equalsIgnoreCase(ruleType) && to.isAfter(hotelStandardTime)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                            "Bậc thứ " + (i + 1) + ": Phụ thu nhận sớm không thể kết thúc sau giờ check-in chuẩn của cơ sở (" + hotelStandardTime + ")");
                }
                if ("LATE_CHECKOUT".equalsIgnoreCase(ruleType) && from.isBefore(hotelStandardTime)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                            "Bậc thứ " + (i + 1) + ": Phụ thu trả muộn không thể bắt đầu trước giờ check-out chuẩn của cơ sở (" + hotelStandardTime + ")");
                }
            }
        }

        // Kiểm tra chống chồng lấn các bậc
        List<SurchargeTierDto> sortedTiers = tiers.stream()
                .sorted(Comparator.comparing(t -> LocalTime.parse(t.getFromTime())))
                .toList();

        for (int i = 0; i < sortedTiers.size() - 1; i++) {
            LocalTime currentTo = LocalTime.parse(sortedTiers.get(i).getToTime());
            LocalTime nextFrom = LocalTime.parse(sortedTiers.get(i + 1).getFromTime());

            if (currentTo.isAfter(nextFrom)) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Khung giờ bậc phụ thu bị chồng lấn: [" + sortedTiers.get(i).getFromTime() + " - " + sortedTiers.get(i).getToTime() +
                                "] và [" + sortedTiers.get(i + 1).getFromTime() + " - " + sortedTiers.get(i + 1).getToTime() + "]");
            }
        }
    }
}
