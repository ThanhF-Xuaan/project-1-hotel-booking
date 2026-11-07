package vn.edu.utc.hotel_booking.modules.booking.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationTierDto;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationFeeType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyTierValidatorTest {

    private PolicyTierValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PolicyTierValidator();
    }

    @Test
    @DisplayName("Happy path: Tiers liên tục hợp lệ 0-3 (100%), 4-6 (50%), 7+ (0%)")
    void testValidateTiers_HappyPath() {
        List<CancellationTierDto> tiers = List.of(
                CancellationTierDto.builder().minDays(0).maxDays(3).penaltyPercent(new BigDecimal("100.00")).description("0-3 ngày").build(),
                CancellationTierDto.builder().minDays(4).maxDays(6).penaltyPercent(new BigDecimal("50.00")).description("4-6 ngày").build(),
                CancellationTierDto.builder().minDays(7).maxDays(null).penaltyPercent(BigDecimal.ZERO).description(">= 7 ngày").build()
        );

        assertThatCode(() -> validator.validateTiers(CancellationFeeType.TIERED, tiers))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Happy path: Tiers chỉ có 1 bậc mở 0+ (100%)")
    void testValidateTiers_SingleTier_HappyPath() {
        List<CancellationTierDto> tiers = List.of(
                CancellationTierDto.builder().minDays(0).maxDays(null).penaltyPercent(new BigDecimal("100.00")).build()
        );

        assertThatCode(() -> validator.validateTiers(CancellationFeeType.TIERED, tiers))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Bỏ qua kiểm tra tiers nếu không phải TIERED hoặc FULL_DEPOSIT")
    void testValidateTiers_NonTieredType_Ignored() {
        assertThatCode(() -> validator.validateTiers(CancellationFeeType.PERCENT, Collections.emptyList()))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateTiers(CancellationFeeType.FIXED_AMOUNT, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Ném lỗi nếu danh sách tiers rỗng hoặc null khi là TIERED")
    void testValidateTiers_EmptyOrNull_ThrowsException() {
        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, Collections.emptyList()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);

        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);
    }

    @Test
    @DisplayName("Ném lỗi nếu tier đầu tiên không bắt đầu từ ngày 0")
    void testValidateTiers_FirstTierNotZero_ThrowsException() {
        List<CancellationTierDto> tiers = List.of(
                CancellationTierDto.builder().minDays(1).maxDays(3).penaltyPercent(new BigDecimal("100.00")).build()
        );

        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, tiers))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);
    }

    @Test
    @DisplayName("Ném lỗi nếu có khoảng hở giữa các tiers (gaps)")
    void testValidateTiers_GapsBetweenTiers_ThrowsException() {
        List<CancellationTierDto> tiers = List.of(
                CancellationTierDto.builder().minDays(0).maxDays(3).penaltyPercent(new BigDecimal("100.00")).build(),
                CancellationTierDto.builder().minDays(5).maxDays(null).penaltyPercent(BigDecimal.ZERO).build()
        );

        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, tiers))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);
    }

    @Test
    @DisplayName("Ném lỗi nếu có chồng lấn giữa các tiers (overlaps)")
    void testValidateTiers_OverlappingTiers_ThrowsException() {
        List<CancellationTierDto> tiers = List.of(
                CancellationTierDto.builder().minDays(0).maxDays(4).penaltyPercent(new BigDecimal("100.00")).build(),
                CancellationTierDto.builder().minDays(4).maxDays(null).penaltyPercent(BigDecimal.ZERO).build()
        );

        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, tiers))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);
    }

    @Test
    @DisplayName("Ném lỗi nếu tỷ lệ phạt tăng lên khi ngày hủy xa hơn")
    void testValidateTiers_PenaltyIncreasesWithDistance_ThrowsException() {
        List<CancellationTierDto> tiers = List.of(
                CancellationTierDto.builder().minDays(0).maxDays(3).penaltyPercent(new BigDecimal("50.00")).build(),
                CancellationTierDto.builder().minDays(4).maxDays(null).penaltyPercent(new BigDecimal("100.00")).build()
        );

        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, tiers))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);
    }

    @Test
    @DisplayName("Ném lỗi nếu tỷ lệ phạt vượt quá 100% hoặc nhỏ hơn 0%")
    void testValidateTiers_InvalidPenaltyPercent_ThrowsException() {
        List<CancellationTierDto> tiersHigh = List.of(
                CancellationTierDto.builder().minDays(0).maxDays(null).penaltyPercent(new BigDecimal("150.00")).build()
        );
        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, tiersHigh))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);

        List<CancellationTierDto> tiersLow = List.of(
                CancellationTierDto.builder().minDays(0).maxDays(null).penaltyPercent(new BigDecimal("-10.00")).build()
        );
        assertThatThrownBy(() -> validator.validateTiers(CancellationFeeType.TIERED, tiersLow))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POLICY_TIERS);
    }
}
