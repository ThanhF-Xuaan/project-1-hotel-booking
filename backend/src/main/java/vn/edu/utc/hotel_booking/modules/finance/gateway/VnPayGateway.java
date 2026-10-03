package vn.edu.utc.hotel_booking.modules.finance.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CallbackResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * VNPay sandbox — ký/xác thực HMAC-SHA512 (vnpay-payment v2.1.0).
 *
 * Flow: build query (sort theo tên field ASCII) → ký → redirect khách ra URL;
 * IPN gửi lại query + vnp_SecureHash → verify lại cùng chuỗi ký.
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VnPayGateway implements PaymentGateway {

    String tmnId;
    String hashSecret;
    String paymentUrl;
    String returnUrl;

    private static final String VNPAY_VERSION = "2.1.0";
    private static final String VNPAY_COMMAND = "pay";
    private static final String VNPAY_CURR_CODE = "VND";
    private static final String VNPAY_LOCALE = "vn";
    private static final String HMAC_ALGO = "HmacSHA512";
    private static final ZoneId VN_TZ = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // Constructor tường minh với @Value — Spring inject từ application.yaml (payment.gateway.vnpay.*)
    public VnPayGateway(
            @Value("${payment.gateway.vnpay.tmn-id}") String tmnId,
            @Value("${payment.gateway.vnpay.hash-secret}") String hashSecret,
            @Value("${payment.gateway.vnpay.url}") String paymentUrl,
            @Value("${payment.gateway.vnpay.return-url}") String returnUrl) {
        this.tmnId = tmnId;
        this.hashSecret = hashSecret;
        this.paymentUrl = paymentUrl;
        this.returnUrl = returnUrl;
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public String createPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String ipAddress) {
        // Chưa cấu hình key sandbox (dev) → lỗi rõ ràng thay vì crash khi ký
        if (tmnId == null || tmnId.isBlank() || hashSecret == null || hashSecret.isBlank()) {
            throw new AppException(ErrorCode.GATEWAY_ERROR,
                    "Chưa cấu hình VNPay (VNPAY_TMN_ID / VNPAY_HASH_SECRET)");
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("vnp_Version", VNPAY_VERSION);
        data.put("vnp_Command", VNPAY_COMMAND);
        data.put("vnp_TmnCode", tmnId);
        // VNPay nhận số tiền VND × 100 (không decimals)
        data.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.DOWN).toPlainString());
        data.put("vnp_CreateDate", LocalDateTime.now(VN_TZ).format(VNPAY_DATE));
        data.put("vnp_CurrCode", VNPAY_CURR_CODE);
        data.put("vnp_IpAddr", ipAddress);
        data.put("vnp_Locale", VNPAY_LOCALE);
        data.put("vnp_OrderInfo", orderInfo);
        data.put("vnp_ReturnUrl", returnUrl);
        data.put("vnp_TxnRef", txnRef);

        String query = buildQueryString(data);
        String sign = sign(query);
        return paymentUrl + "?" + query
                + "&vnp_SecureHash=" + sign
                + "&vnp_SecureHashType=SHA512";
    }

    @Override
    public CallbackResult verifyCallback(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        // Secret rỗng → không xác thực được chữ ký → coi như KHÔNG hợp lệ
        boolean secretReady = hashSecret != null && !hashSecret.isBlank();
        boolean valid = false;
        if (secretReady && secureHash != null && !secureHash.isBlank()
                && txnRef != null && !txnRef.isBlank()) {
            // Loại 2 field chữ ký khỏi chuỗi ký rồi xác thực lại
            Map<String, String> data = params.entrySet().stream()
                    .filter(e -> !"vnp_SecureHash".equals(e.getKey())
                            && !"vnp_SecureHashType".equals(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            String expected = sign(buildQueryString(data));
            valid = secureHash.equalsIgnoreCase(expected);
        }

        // vnp_Amount báo về là VND × 100 → quy về VND để so với Payment.totalAmount
        BigDecimal amount = null;
        String rawAmount = params.get("vnp_Amount");
        if (rawAmount != null && !rawAmount.isBlank()) {
            try {
                amount = new BigDecimal(rawAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException | NumberFormatException e) {
                log.warn("vnp_Amount không hợp lệ: {}", rawAmount);
            }
        }

        return CallbackResult.builder()
                .valid(valid)
                .txnRef(txnRef)
                // Mã tham chiếu thật cho transactions: vnp_TransactionNo (VD: TM126345...)
                .transactionReference(params.get("vnp_TransactionNo"))
                .amount(amount)
                .responseCode(responseCode)
                .success(valid && "00".equals(responseCode))
                .rawPayload(toJson(params))
                .build();
    }

    /**
     * Chuỗi ký: sort key ASCII, mỗi value encode URL (UTF-8, space → %20),
     * nối "key=value" bằng "&" — chuẩn VNPay v2.
     */
    public String buildQueryString(Map<String, String> data) {
        return new TreeMap<>(data).entrySet().stream()
                .map(e -> e.getKey() + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    /** Ký HMAC-SHA512, trả về hex lowercase */
    public String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Không ký được dữ liệu VNPay", e);
        }
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String toJson(Map<String, String> params) {
        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return String.valueOf(params);
        }
    }
}
