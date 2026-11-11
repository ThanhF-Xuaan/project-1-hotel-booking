package vn.edu.utc.hotel_booking.modules.pricing.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.modules.pricing.dto.condition.SurchargeConditionsDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.condition.SurchargeTierDto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurchargeConditionsValidatorTest {

    private SurchargeConditionsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SurchargeConditionsValidator();
    }

    @Test
    @DisplayName("Null hoặc danh sách tiers rỗng không ném ngoại lệ")
    void validate_whenNullOrEmptyTiers_shouldPass() {
        assertThatCode(() -> validator.validate("EARLY_CHECKIN", null, LocalTime.of(14, 0)))
                .doesNotThrowAnyException();

        SurchargeConditionsDto emptyDto = SurchargeConditionsDto.builder().tiers(new ArrayList<>()).build();
        assertThatCode(() -> validator.validate("EARLY_CHECKIN", emptyDto, LocalTime.of(14, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Bậc nhận phòng sớm hợp lệ kết thúc đúng giờ check-in của cơ sở (15:00)")
    void validate_earlyCheckin_validTiers_shouldPass() {
        LocalTime hotelCheckIn = LocalTime.of(15, 0);

        List<SurchargeTierDto> tiers = List.of(
                SurchargeTierDto.builder()
                        .fromTime("00:00")
                        .toTime("06:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("100.0"))
                        .description("Nhận trước 6h sáng")
                        .build(),
                SurchargeTierDto.builder()
                        .fromTime("06:00")
                        .toTime("10:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("50.0"))
                        .description("Nhận 6h-10h")
                        .build(),
                SurchargeTierDto.builder()
                        .fromTime("10:00")
                        .toTime("15:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("30.0"))
                        .description("Nhận 10h-15h")
                        .build()
        );

        SurchargeConditionsDto dto = SurchargeConditionsDto.builder()
                .standardTime("15:00")
                .tiers(tiers)
                .build();

        assertThatCode(() -> validator.validate("EARLY_CHECKIN", dto, hotelCheckIn))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Báo lỗi khi bậc nhận sớm kết thúc sau giờ check-in quy chuẩn của cơ sở")
    void validate_earlyCheckin_tierExceedsHotelCheckInTime_shouldThrowException() {
        LocalTime hotelCheckIn = LocalTime.of(14, 0);

        List<SurchargeTierDto> tiers = List.of(
                SurchargeTierDto.builder()
                        .fromTime("10:00")
                        .toTime("14:30") // Vượt quá 14:00 của cơ sở
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("30.0"))
                        .build()
        );

        SurchargeConditionsDto dto = SurchargeConditionsDto.builder().tiers(tiers).build();

        assertThatThrownBy(() -> validator.validate("EARLY_CHECKIN", dto, hotelCheckIn))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Phụ thu nhận sớm không thể kết thúc sau giờ check-in chuẩn của cơ sở (14:00)");
    }

    @Test
    @DisplayName("Bậc trả phòng muộn hợp lệ bắt đầu từ giờ check-out quy chuẩn của cơ sở (11:00)")
    void validate_lateCheckout_validTiers_shouldPass() {
        LocalTime hotelCheckOut = LocalTime.of(11, 0);

        List<SurchargeTierDto> tiers = List.of(
                SurchargeTierDto.builder()
                        .fromTime("11:00")
                        .toTime("14:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("30.0"))
                        .build(),
                SurchargeTierDto.builder()
                        .fromTime("14:00")
                        .toTime("18:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("50.0"))
                        .build(),
                SurchargeTierDto.builder()
                        .fromTime("18:00")
                        .toTime("23:59")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("100.0"))
                        .build()
        );

        SurchargeConditionsDto dto = SurchargeConditionsDto.builder().tiers(tiers).build();

        assertThatCode(() -> validator.validate("LATE_CHECKOUT", dto, hotelCheckOut))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Báo lỗi khi bậc trả phòng muộn bắt đầu trước giờ check-out quy chuẩn của cơ sở")
    void validate_lateCheckout_tierStartsBeforeHotelCheckOutTime_shouldThrowException() {
        LocalTime hotelCheckOut = LocalTime.of(12, 0);

        List<SurchargeTierDto> tiers = List.of(
                SurchargeTierDto.builder()
                        .fromTime("10:00") // Trước 12:00 của cơ sở
                        .toTime("14:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("30.0"))
                        .build()
        );

        SurchargeConditionsDto dto = SurchargeConditionsDto.builder().tiers(tiers).build();

        assertThatThrownBy(() -> validator.validate("LATE_CHECKOUT", dto, hotelCheckOut))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Phụ thu trả muộn không thể bắt đầu trước giờ check-out chuẩn của cơ sở (12:00)");
    }

    @Test
    @DisplayName("Báo lỗi khi giờ bắt đầu lớn hơn hoặc bằng giờ kết thúc")
    void validate_fromTimeNotBeforeToTime_shouldThrowException() {
        List<SurchargeTierDto> tiers = List.of(
                SurchargeTierDto.builder()
                        .fromTime("12:00")
                        .toTime("10:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("30.0"))
                        .build()
        );

        SurchargeConditionsDto dto = SurchargeConditionsDto.builder().tiers(tiers).build();

        assertThatThrownBy(() -> validator.validate("EARLY_CHECKIN", dto, LocalTime.of(14, 0)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("phải trước giờ kết thúc");
    }

    @Test
    @DisplayName("Báo lỗi khi tỷ lệ phần trăm vượt quá 100%")
    void validate_percentExceeds100_shouldThrowException() {
        List<SurchargeTierDto> tiers = List.of(
                SurchargeTierDto.builder()
                        .fromTime("06:00")
                        .toTime("10:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("120.0"))
                        .build()
        );

        SurchargeConditionsDto dto = SurchargeConditionsDto.builder().tiers(tiers).build();

        assertThatThrownBy(() -> validator.validate("EARLY_CHECKIN", dto, LocalTime.of(14, 0)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Tỷ lệ phụ thu phần trăm không được vượt quá 100%");
    }

    @Test
    @DisplayName("Báo lỗi khi các khung giờ bậc phụ thu bị chồng lấn")
    void validate_overlappingTiers_shouldThrowException() {
        List<SurchargeTierDto> tiers = List.of(
                SurchargeTierDto.builder()
                        .fromTime("06:00")
                        .toTime("11:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("50.0"))
                        .build(),
                SurchargeTierDto.builder()
                        .fromTime("10:00") // Chồng lấn với bậc trước kết thúc lúc 11:00
                        .toTime("14:00")
                        .adjustmentType("PERCENT")
                        .adjustmentValue(new BigDecimal("30.0"))
                        .build()
        );

        SurchargeConditionsDto dto = SurchargeConditionsDto.builder().tiers(tiers).build();

        assertThatThrownBy(() -> validator.validate("EARLY_CHECKIN", dto, LocalTime.of(14, 0)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Khung giờ bậc phụ thu bị chồng lấn");
    }
}
