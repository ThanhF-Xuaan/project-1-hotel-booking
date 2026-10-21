package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingGuestType;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalkInBookingRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    Short hotelId;

    @Builder.Default
    BookingType bookingType = BookingType.FIT;

    // --- Khách hàng Doanh nghiệp (Company) ---
    Long companyId;
    NewCompanyRequest newCompany;

    // --- Người đặt / Đại diện thanh toán (BookingGuest) ---
    Long guestId;
    NewGuestRequest newGuest;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    LocalDate checkOutDate;

    String notes;

    @NotEmpty(message = "Danh sách phòng đặt không được để trống")
    @Size(max = 5, message = "Đơn đặt phòng walk-in tối đa 5 phòng")
    @Valid
    @Builder.Default
    List<WalkInRoomItemRequest> rooms = new ArrayList<>();

    @Builder.Default
    List<WalkInConfirmedChargeRequest> confirmedCharges = new ArrayList<>();

    @Builder.Default
    List<WalkInServiceItemRequest> serviceItems = new ArrayList<>();

    @NotNull(message = "Thông tin thanh toán không được để trống")
    @Valid
    WalkInPaymentRequest payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class NewCompanyRequest {
        String name;
        String taxCode;
        String address;
        String contactName;
        String contactPhone;
        String contactEmail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class NewGuestRequest {
        String fullName;
        String phone;
        String email;
        String identityType;
        String identityNumber;
        LocalDate birthDate;
        String nationality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WalkInRoomItemRequest {
        @NotNull(message = "Loại phòng không được để trống")
        Integer hotelRoomTypeId;

        @NotNull(message = "Phòng vật lý không được để trống")
        Integer roomInstanceId;

        @Builder.Default
        Short adultCount = 1;

        @Builder.Default
        Short childCount = 0;

        @Builder.Default
        Short infantCount = 0;

        @Builder.Default
        Short extraBeds = 0;

        @Builder.Default
        List<WalkInStayGuestRequest> stayGuests = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WalkInStayGuestRequest {
        String fullName;
        LocalDate birthDate;
        @Builder.Default
        BookingGuestType guestType = BookingGuestType.ADULT;
        Gender gender;
        String identityType;
        String identityNumber;
        @Builder.Default
        String nationality = "Việt Nam";
        Boolean isPrimaryGuest;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WalkInConfirmedChargeRequest {
        Integer roomInstanceId;
        BookingChargeType chargeType;
        BigDecimal amount;
        String description;
        Boolean isWaived;
        String waivedReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WalkInServiceItemRequest {
        Integer roomInstanceId;
        Long menuItemId;
        Integer quantity;
        String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WalkInPaymentRequest {
        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod paymentMethod;

        // "FULL_PAYMENT" hoặc "DEPOSIT"
        @Builder.Default
        String paymentPurpose = "FULL_PAYMENT";

        @NotNull(message = "Số tiền thanh toán không được để trống")
        BigDecimal amount;

        String transactionReference;
    }
}
