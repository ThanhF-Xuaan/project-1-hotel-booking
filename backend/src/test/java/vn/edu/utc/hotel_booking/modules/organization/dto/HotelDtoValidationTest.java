package vn.edu.utc.hotel_booking.modules.organization.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelUpdateRequest;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Hotel DTO Validation Test - Kiểm tra validate Số điện thoại Khách sạn")
class HotelDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private HotelCreateRequest.HotelCreateRequestBuilder createBaseRequestBuilder() {
        return HotelCreateRequest.builder()
                .regionId((short) 1)
                .name("SmartStay Luxury Hanoi")
                .address("123 Phố Huế, Hai Bà Trưng, Hà Nội")
                .checkInTime(LocalTime.of(14, 0))
                .checkOutTime(LocalTime.of(12, 0))
                .serviceFeePercent(BigDecimal.valueOf(5.0));
    }

    @Test
    @DisplayName("HotelCreateRequest: Báo lỗi khi số điện thoại là số âm hoặc không hợp lệ (-2)")
    void hotelCreateRequest_whenPhoneIsNegativeTwo_shouldViolateConstraint() {
        HotelCreateRequest request = createBaseRequestBuilder()
                .phone("-2")
                .build();

        Set<ConstraintViolation<HotelCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("phone") &&
                v.getMessage().contains("Số điện thoại không đúng định dạng")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12345", "--2", "0123456789", "09876", "not-a-phone"})
    @DisplayName("HotelCreateRequest: Báo lỗi khi số điện thoại sai định dạng bất kỳ")
    void hotelCreateRequest_whenPhoneIsInvalid_shouldViolateConstraint(String invalidPhone) {
        HotelCreateRequest request = createBaseRequestBuilder()
                .phone(invalidPhone)
                .build();

        Set<ConstraintViolation<HotelCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "02466668888",      // Máy bàn cố định Hà Nội (11 số)
            "02838220000",      // Máy bàn cố định TP.HCM (11 số)
            "02363822000",      // Máy bàn cố định Đà Nẵng (11 số)
            "0912345678",       // Di động Vina (10 số)
            "0987654321",       // Di động Viettel (10 số)
            "0388889999",       // Di động đầu 03
            "+842466668888",    // Máy bàn quốc tế (+8424...)
            "+84912345678"      // Di động quốc tế (+849...)
    })
    @DisplayName("HotelCreateRequest: Hợp lệ khi số điện thoại là số bàn hoặc di động Việt Nam chuẩn")
    void hotelCreateRequest_whenPhoneIsValid_shouldHaveNoViolations(String validPhone) {
        HotelCreateRequest request = createBaseRequestBuilder()
                .phone(validPhone)
                .build();

        Set<ConstraintViolation<HotelCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("HotelCreateRequest: Hợp lệ khi số điện thoại để trống (optional)")
    void hotelCreateRequest_whenPhoneIsEmptyOrNull_shouldHaveNoViolations() {
        HotelCreateRequest requestNull = createBaseRequestBuilder()
                .phone(null)
                .build();
        assertThat(validator.validate(requestNull)).isEmpty();

        HotelCreateRequest requestEmpty = createBaseRequestBuilder()
                .phone("")
                .build();
        assertThat(validator.validate(requestEmpty)).isEmpty();
    }

    @Test
    @DisplayName("HotelUpdateRequest: Báo lỗi khi cập nhật số điện thoại thành -2")
    void hotelUpdateRequest_whenPhoneIsNegativeTwo_shouldViolateConstraint() {
        HotelUpdateRequest request = HotelUpdateRequest.builder()
                .regionId((short) 1)
                .name("SmartStay Updated")
                .address("456 Kim Mã, Ba Đình, Hà Nội")
                .phone("-2")
                .build();

        Set<ConstraintViolation<HotelUpdateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("phone") &&
                v.getMessage().contains("Số điện thoại không đúng định dạng")
        );
    }
}
