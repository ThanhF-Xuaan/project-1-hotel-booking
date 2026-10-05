package vn.edu.utc.hotel_booking.modules.finance.gateway.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

/**
 * Phản hồi cho FE sau khi tạo phiên thanh toán online (VNPay/MoMo).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentUrlResponse {

    /** URL cổng thanh toán — FE redirect khách sang đây */
    String paymentUrl;

    /** ID Payment vừa tạo — FE dùng để hủy (POST /payments/{id}/cancel) khi có lỗi */
    Long paymentId;

    /** Mã đơn của mình (vnp_TxnRef / MoMo orderId) — FE dùng để poll trạng thái */
    String txnRef;

    /** Hết hạn phiên = TTL Redis lock 10 phút (FE đồng bộ countdown) */
    OffsetDateTime expiresAt;
}
