package vn.edu.utc.hotel_booking.modules.booking.validator;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationTierDto;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationFeeType;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class PolicyTierValidator {

    /**
     * Kiểm tra tính toàn vẹn của mảng bậc phạt (tiers)
     * - Bắt buộc có phần tử khi là TIERED hoặc FULL_DEPOSIT
     * - Không hở (gaps), không chồng lấn (overlaps)
     * - Mức phạt tăng dần khi ngày đến càng gần (minDays càng nhỏ)
     */
    public void validateTiers(CancellationFeeType feeType, List<CancellationTierDto> tiers) {
        if (feeType != CancellationFeeType.TIERED && feeType != CancellationFeeType.FULL_DEPOSIT) {
            return;
        }

        if (tiers == null || tiers.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_POLICY_TIERS);
        }

        // Sắp xếp tăng dần theo minDays (từ 0 ngày đến mốc xa nhất)
        List<CancellationTierDto> sortedTiers = tiers.stream()
                .sorted(Comparator.comparing(CancellationTierDto::getMinDays))
                .toList();

        // Tier đầu tiên bắt buộc phải bắt đầu từ ngày 0
        if (sortedTiers.get(0).getMinDays() != 0) {
            throw new AppException(ErrorCode.INVALID_POLICY_TIERS);
        }

        for (int i = 0; i < sortedTiers.size(); i++) {
            CancellationTierDto current = sortedTiers.get(i);

            if (current.getPenaltyPercent() == null ||
                    current.getPenaltyPercent().compareTo(BigDecimal.ZERO) < 0 ||
                    current.getPenaltyPercent().compareTo(new BigDecimal("100.00")) > 0) {
                throw new AppException(ErrorCode.INVALID_POLICY_TIERS);
            }

            if (i < sortedTiers.size() - 1) {
                // Tier ở giữa bắt buộc phải có maxDays
                if (current.getMaxDays() == null || current.getMaxDays() < current.getMinDays()) {
                    throw new AppException(ErrorCode.INVALID_POLICY_TIERS);
                }

                CancellationTierDto next = sortedTiers.get(i + 1);
                // Kiểm tra tính liên tục: minDays của tier kế tiếp phải = maxDays + 1 của tier hiện tại
                if (next.getMinDays() != current.getMaxDays() + 1) {
                    throw new AppException(ErrorCode.INVALID_POLICY_TIERS);
                }

                // Kiểm tra mức phạt: hủy càng xa ngày check-in (minDays lớn hơn) thì % phạt phải <= hoặc =
                if (next.getPenaltyPercent().compareTo(current.getPenaltyPercent()) > 0) {
                    throw new AppException(ErrorCode.INVALID_POLICY_TIERS);
                }
            } else {
                // Tier cuối cùng: nếu có maxDays thì phải >= minDays
                if (current.getMaxDays() != null && current.getMaxDays() < current.getMinDays()) {
                    throw new AppException(ErrorCode.INVALID_POLICY_TIERS);
                }
            }
        }
    }
}
