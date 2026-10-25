package vn.edu.utc.hotel_booking.modules.booking.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractDepositRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquiryCreateRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group DTO Validation Test")
class GroupDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("GroupInquiryCreateRequest: Báo lỗi khi totalRooms < 6")
    void inquiryCreateRequest_whenTotalRoomsLessThan6_shouldViolateConstraint() {
        GroupInquiryCreateRequest request = GroupInquiryCreateRequest.builder()
                .hotelId((short) 1)
                .companyName("VinFast")
                .contactName("Nguyen Van A")
                .contactPhone("0912345678")
                .contactEmail("vinfast@domain.vn")
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .totalRooms(5) // Vi phạm < 6
                .totalGuests(10)
                .requestedRoomTypes(List.of(
                        GroupInquiryCreateRequest.InquiryRoomTypeItem.builder()
                                .hotelRoomTypeId(1)
                                .requestedRooms(5)
                                .build()
                ))
                .build();

        Set<ConstraintViolation<GroupInquiryCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("totalRooms") &&
                v.getMessage().contains("tối thiểu 6 phòng"));
    }

    @Test
    @DisplayName("GroupInquiryCreateRequest: Hợp lệ khi totalRooms >= 6 và đầy đủ thông tin")
    void inquiryCreateRequest_whenValid_shouldHaveNoViolations() {
        GroupInquiryCreateRequest request = GroupInquiryCreateRequest.builder()
                .hotelId((short) 1)
                .companyName("VinFast")
                .contactName("Nguyen Van A")
                .contactPhone("0912345678")
                .contactEmail("vinfast@domain.vn")
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .totalRooms(6)
                .totalGuests(12)
                .requestedRoomTypes(List.of(
                        GroupInquiryCreateRequest.InquiryRoomTypeItem.builder()
                                .hotelRoomTypeId(1)
                                .requestedRooms(6)
                                .build()
                ))
                .build();

        Set<ConstraintViolation<GroupInquiryCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("GroupContractDepositRequest: Báo lỗi khi amount null hoặc <= 0")
    void depositRequest_whenAmountInvalid_shouldViolateConstraint() {
        GroupContractDepositRequest requestZero = GroupContractDepositRequest.builder()
                .amount(BigDecimal.ZERO)
                .transactionReference("TXN-12345")
                .build();

        Set<ConstraintViolation<GroupContractDepositRequest>> violations = validator.validate(requestZero);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("GroupContractCreateRequest: Báo lỗi khi depositRate < 50%")
    void contractCreateRequest_whenDepositRateLessThan50_shouldViolateConstraint() {
        GroupContractCreateRequest request = GroupContractCreateRequest.builder()
                .hotelId((short) 1)
                .companyId(10L)
                .groupName("FPT Software Workshop")
                .companyTaxCode("0101234567")
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(13))
                .totalRooms(10)
                .totalGuests(20)
                .depositRate(BigDecimal.valueOf(30.00)) // Vi phạm < 50%
                .roomBlocks(List.of(
                        GroupContractCreateRequest.ContractRoomBlockItem.builder()
                                .hotelRoomTypeId(1)
                                .blockedRooms(10)
                                .negotiatedRatePerNight(BigDecimal.valueOf(1500000))
                                .build()
                ))
                .build();

        Set<ConstraintViolation<GroupContractCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("depositRate"));
    }
}
