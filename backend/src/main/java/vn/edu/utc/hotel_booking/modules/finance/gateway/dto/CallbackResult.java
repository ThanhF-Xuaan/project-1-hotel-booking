package vn.edu.utc.hotel_booking.modules.finance.gateway.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Kết quả xác thực callback (IPN/callback) từ cổng thanh toán.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallbackResult {

    /** Chữ ký có hợp lệ không — false → ném PAYMENT_SIGNATURE_INVALID (8004) */
    boolean valid;

    /** Mã đơn của mình: vnp_TxnRef / MoMo orderId — dùng để tìm Payment */
    String txnRef;

    /**
     * Mã tham chiếu ghi vào transactions: vnp_TransactionNo (VD: TM126345...)
     * / MoMo orderId (theo bảng mục 1 — FE không được nhập tay)
     */
    String transactionReference;

    /** Số tiền callback trả về (đã quy đổi về VND) — so với totalAmount của Payment */
    BigDecimal amount;

    /** vnp_ResponseCode ("00" = thành công) / MoMo resultCode ("0" = thành công) */
    String responseCode;

    /** valid && responseCode báo thành công */
    boolean success;

    /** JSON dump toàn bộ payload — ghi vào payments.raw_callback_payload để debug */
    String rawPayload;
}
