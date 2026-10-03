package vn.edu.utc.hotel_booking.modules.finance.gateway.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;

/**
 * Request tạo phiên thanh toán online (POST /payments/vnpay/create, /payments/momo/create).
 * Contract: { bookingId, method } — paymentId optional (tái sử dụng Payment PENDING đã tạo).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentRequest {

    /** Tùy chọn: nếu FE đã tạo Payment (qua /payments/create) thì gửi kèm để tái sử dụng */
    Long paymentId;

    @NotNull(message = "bookingId không được để trống")
    Long bookingId;

    /** Chỉ nhận VNPAY hoặc MOMO — validate trong Service */
    @NotNull(message = "method không được để trống")
    PaymentMethod method;
}
