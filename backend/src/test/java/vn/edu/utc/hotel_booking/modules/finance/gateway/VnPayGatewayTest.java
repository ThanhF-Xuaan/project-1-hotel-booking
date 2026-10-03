package vn.edu.utc.hotel_booking.modules.finance.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CallbackResult;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test thuần — ký/xác thực HMAC-SHA512 của VNPay (không Spring context).
 */
class VnPayGatewayTest {

    VnPayGateway gateway;
    VnPayGateway gatewaySecretKhac;

    @BeforeEach
    void setUp() {
        gateway = new VnPayGateway(
                "TESTTMN01",
                "correct-hash-secret",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/result");
        gatewaySecretKhac = new VnPayGateway(
                "TESTTMN01",
                "wrong-hash-secret",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/result");
    }

    /** Ký dữ liệu callback bằng gateway truyền vào (mô phỏng IPN trả về) */
    private Map<String, String> signedCallback(Map<String, String> data, VnPayGateway signer) {
        Map<String, String> params = new LinkedHashMap<>(data);
        params.put("vnp_SecureHash", signer.sign(signer.buildQueryString(data)));
        params.put("vnp_SecureHashType", "SHA512");
        return params;
    }

    private Map<String, String> successData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("vnp_TxnRef", "VNP20261001120000-1234");
        data.put("vnp_Amount", "150000000"); // 1.500.000 VND × 100
        data.put("vnp_ResponseCode", "00");
        data.put("vnp_TransactionStatus", "00");
        data.put("vnp_TransactionNo", "TM1263456789");
        return data;
    }

    @Test
    @DisplayName("createPaymentUrl chứa field bắt buộc + số tiền ×100 + chữ ký")
    void createPaymentUrl_CoFieldBatBuoc() {
        String url = gateway.createPaymentUrl(
                "VNP20261001120000-1234", BigDecimal.valueOf(1500000),
                "Thanh toan dat phong BK1", "127.0.0.1");

        assertThat(url)
                .contains("vnp_TxnRef=VNP20261001120000-1234")
                .contains("vnp_Amount=150000000")
                .contains("vnp_TmnCode=TESTTMN01")
                .contains("vnp_SecureHash=")
                .contains("vnp_SecureHashType=SHA512")
                .contains("vnp_ReturnUrl=");
    }

    @Test
    @DisplayName("Round-trip: URL đã ký → parse query → verifyCallback valid=true, success=true")
    void verifyCallback_ChuKyDung_HopLe() {
        String url = gateway.createPaymentUrl(
                "VNP20261001120000-1234", BigDecimal.valueOf(1500000),
                "Thanh toan dat phong BK1", "127.0.0.1");

        // Giải query như VNPay gửi IPN (decode value)
        Map<String, String> params = new LinkedHashMap<>();
        String query = url.substring(url.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TM1263456789");
        // responseCode/transactionNo thêm vào sau → ký lại như IPN thật
        Map<String, String> data = new LinkedHashMap<>(params);
        data.remove("vnp_SecureHash");
        data.remove("vnp_SecureHashType");
        params.put("vnp_SecureHash", gateway.sign(gateway.buildQueryString(data)));

        CallbackResult result = gateway.verifyCallback(params);

        assertThat(result.isValid()).isTrue();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTxnRef()).isEqualTo("VNP20261001120000-1234");
        assertThat(result.getTransactionReference()).isEqualTo("TM1263456789");
        assertThat(result.getAmount()).isEqualByComparingTo("1500000");
        assertThat(result.getRawPayload()).contains("vnp_TxnRef");
    }

    @Test
    @DisplayName("verifyCallback: chữ ký KHÁC secret → valid=false, success=false")
    void verifyCallback_SaiSecret_KhongHopLe() {
        Map<String, String> params = signedCallback(successData(), gatewaySecretKhac);

        CallbackResult result = gateway.verifyCallback(params);

        assertThat(result.isValid()).isFalse();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("verifyCallback: sửa số tiền sau khi ký → valid=false (payload bị giả mạo)")
    void verifyCallback_SuaSoTien_KhongHopLe() {
        Map<String, String> params = signedCallback(successData(), gateway);
        params.put("vnp_Amount", "1"); // sửa sau ký

        CallbackResult result = gateway.verifyCallback(params);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("verifyCallback: thiếu vnp_TxnRef → valid=false dù chữ ký đúng")
    void verifyCallback_ThieuTxnRef_KhongHopLe() {
        Map<String, String> data = successData();
        data.remove("vnp_TxnRef");
        Map<String, String> params = signedCallback(data, gateway);

        CallbackResult result = gateway.verifyCallback(params);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("verifyCallback: thiếu vnp_SecureHash → valid=false")
    void verifyCallback_ThieuChuKy_KhongHopLe() {
        Map<String, String> params = new LinkedHashMap<>(successData());

        CallbackResult result = gateway.verifyCallback(params);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("verifyCallback: responseCode != 00 → valid=true nhưng success=false")
    void verifyCallback_GiaoDichThatBai_CoHopLeNhungKhongThanhCong() {
        Map<String, String> data = successData();
        data.put("vnp_ResponseCode", "24"); // khách hủy
        Map<String, String> params = signedCallback(data, gateway);

        CallbackResult result = gateway.verifyCallback(params);

        assertThat(result.isValid()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResponseCode()).isEqualTo("24");
    }

    @Test
    @DisplayName("Chưa cấu hình key (secret rỗng): createPaymentUrl → GATEWAY_ERROR (8008), verify → invalid")
    void ChuaCauHinhKey_Loi8008() {
        VnPayGateway khongKey = new VnPayGateway(
                "", "",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/result");

        assertThatThrownBy(() -> khongKey.createPaymentUrl(
                "VNP1", BigDecimal.valueOf(100000), "test", "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.GATEWAY_ERROR));

        // Callback khi secret rỗng → không ký/verify được → coi như chữ ký sai
        CallbackResult result = khongKey.verifyCallback(successData());
        assertThat(result.isValid()).isFalse();
    }
}
