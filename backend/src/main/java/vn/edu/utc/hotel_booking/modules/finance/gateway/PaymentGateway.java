package vn.edu.utc.hotel_booking.modules.finance.gateway;

import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CallbackResult;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Strategy Pattern — mỗi cổng thanh toán (VNPay, MoMo) tự cung cấp
 * cách tạo URL thanh toán và xác thực chữ ký callback.
 */
public interface PaymentGateway {

    /** Phương thức mà gateway này phục vụ (VNPAY hoặc MOMO) */
    PaymentMethod supportedMethod();

    /**
     * Tạo URL thanh toán đã ký chữ ký.
     *
     * @param txnRef    mã đơn của mình (lưu payments.gateway_txn_id)
     * @param amount    số tiền VND
     * @param orderInfo mô tả đơn hàng hiển thị trên cổng
     * @param ipAddress IP của khách (giao dịch thanh toán)
     * @return URL redirect ra cổng thanh toán
     */
    String createPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String ipAddress);

    /**
     * Xác thực chữ ký callback (IPN) và trích xuất thông tin giao dịch.
     * Trả về CallbackResult.valid = false nếu chữ ký sai/thiếu param.
     */
    CallbackResult verifyCallback(Map<String, String> params);
}
