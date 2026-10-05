package vn.edu.utc.hotel_booking.modules.finance.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CallbackResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * MoMo wallet sandbox — ký HMAC-SHA256.
 *
 * createPaymentUrl: POST JSON sang MoMo gateway API lấy payUrl.
 * verifyCallback: xác thực signature của callback body (resultCode = "0" là thành công).
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MoMoGateway implements PaymentGateway {

    String partnerCode;
    String accessKey;
    String secretKey;
    String apiUrl;
    String ipnUrl;
    String redirectUrl;

    RestClient restClient = RestClient.create();
    ObjectMapper objectMapper = new ObjectMapper();

    private static final String REQUEST_TYPE = "captureWallet";
    private static final String LANG = "vi";
    private static final String HMAC_ALGO = "HmacSHA256";

    public MoMoGateway(
            @Value("${payment.gateway.momo.partner-code}") String partnerCode,
            @Value("${payment.gateway.momo.access-key}") String accessKey,
            @Value("${payment.gateway.momo.secret-key}") String secretKey,
            @Value("${payment.gateway.momo.url}") String apiUrl,
            @Value("${payment.gateway.momo.ipn-url}") String ipnUrl,
            @Value("${payment.gateway.momo.redirect-url}") String redirectUrl) {
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.apiUrl = apiUrl;
        this.ipnUrl = ipnUrl;
        this.redirectUrl = redirectUrl;
    }

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.MOMO;
    }

    @Override
    public String createPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String ipAddress) {
        // Chưa cấu hình key sandbox (dev) → lỗi rõ ràng thay vì crash khi ký
        if (partnerCode == null || partnerCode.isBlank()
                || accessKey == null || accessKey.isBlank()
                || secretKey == null || secretKey.isBlank()) {
            throw new AppException(ErrorCode.GATEWAY_ERROR,
                    "Chưa cấu hình MoMo (MOMO_PARTNER_CODE / MOMO_ACCESS_KEY / MOMO_SECRET_KEY)");
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("accessKey", accessKey);
        body.put("requestId", txnRef);
        body.put("orderId", txnRef);
        body.put("amount", amount.longValue() + "");
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("requestType", REQUEST_TYPE);
        body.put("lang", LANG);

        // Ký theo đúng thứ tự field của MoMo (không gồm lang)
        String signData = "accessKey=" + accessKey
                + "&amount=" + body.get("amount")
                + "&extraData="
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + txnRef
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + txnRef
                + "&requestType=" + REQUEST_TYPE;
        body.put("signature", sign(signData));

        try {
            String response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode json = objectMapper.readTree(response == null ? "{}" : response);
            int resultCode = json.path("resultCode").asInt(-1);
            String payUrl = json.path("payUrl").asText("");
            if (resultCode != 0 || payUrl.isBlank()) {
                log.error("MoMo create thất bại: {}", response);
                throw new AppException(ErrorCode.GATEWAY_ERROR,
                        "MoMo từ chối tạo giao dịch: " + json.path("message").asText("unknown"));
            }
            return payUrl;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi kết nối MoMo API", e);
            throw new AppException(ErrorCode.GATEWAY_ERROR, "Không kết nối được MoMo: " + e.getMessage());
        }
    }

    @Override
    public CallbackResult verifyCallback(Map<String, String> params) {
        String signature = params.get("signature");
        String orderId = params.get("orderId");

        // Secret rỗng → không xác thực được chữ ký → coi như KHÔNG hợp lệ
        boolean secretReady = secretKey != null && !secretKey.isBlank();
        boolean valid = false;
        if (secretReady && signature != null && !signature.isBlank() && orderId != null && !orderId.isBlank()) {
            // Đúng thứ tự field ký của MoMo callback
            String signData = "accessKey=" + accessKey
                    + "&amount=" + orEmpty(params.get("amount"))
                    + "&extraData=" + orEmpty(params.get("extraData"))
                    + "&message=" + orEmpty(params.get("message"))
                    + "&orderId=" + orderId
                    + "&orderInfo=" + orEmpty(params.get("orderInfo"))
                    + "&orderType=" + orEmpty(params.get("orderType"))
                    + "&partnerCode=" + orEmpty(params.get("partnerCode"))
                    + "&payType=" + orEmpty(params.get("payType"))
                    + "&requestId=" + orEmpty(params.get("requestId"))
                    + "&responseTime=" + orEmpty(params.get("responseTime"))
                    + "&resultCode=" + orEmpty(params.get("resultCode"))
                    + "&transId=" + orEmpty(params.get("transId"));
            valid = signature.equals(sign(signData));
        }

        BigDecimal amount = null;
        String rawAmount = params.get("amount");
        if (rawAmount != null && !rawAmount.isBlank()) {
            try {
                amount = new BigDecimal(rawAmount);
            } catch (NumberFormatException e) {
                log.warn("amount callback MoMo không hợp lệ: {}", rawAmount);
            }
        }

        return CallbackResult.builder()
                .valid(valid)
                .txnRef(orderId)
                // Theo bảng mục 1: MoMo reference = orderId (mã đơn do mình tạo)
                .transactionReference(orderId)
                .amount(amount)
                .responseCode(params.get("resultCode"))
                .success(valid && "0".equals(params.get("resultCode")))
                .rawPayload(toJson(params))
                .build();
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Không ký được dữ liệu MoMo", e);
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String toJson(Map<String, String> params) {
        try {
            return new ObjectMapper().writeValueAsString(new TreeMap<>(params));
        } catch (JsonProcessingException e) {
            return String.valueOf(params);
        }
    }
}
