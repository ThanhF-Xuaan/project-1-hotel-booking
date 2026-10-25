package vn.edu.utc.hotel_booking.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditDataMasker — PDPD Compliance Unit Tests")
class AuditDataMaskerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Mask mật khẩu password, newPassword -> [PROTECTED]")
    void maskPassword_shouldBeProtected() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("username", "receptionist_01");
        data.put("password", "SuperSecret123!");
        data.put("newPassword", "NewP@ssw0rd2026");

        String maskedJson = AuditDataMasker.maskToJson(data);
        JsonNode node = objectMapper.readTree(maskedJson);

        assertThat(node.get("username").asText()).isEqualTo("receptionist_01");
        assertThat(node.get("password").asText()).isEqualTo("[PROTECTED]");
        assertThat(node.get("newPassword").asText()).isEqualTo("[PROTECTED]");
    }

    @Test
    @DisplayName("Mask số CCCD 12 chữ số theo chuẩn PDPD NĐ 13/2023/NĐ-CP (giữ 3 đầu, 3 cuối)")
    void maskCitizenId_shouldMaskMiddle() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("citizenId", "001089123456");
        data.put("cccd", "038195987654");

        String maskedJson = AuditDataMasker.maskToJson(data);
        JsonNode node = objectMapper.readTree(maskedJson);

        assertThat(node.get("citizenId").asText()).isEqualTo("001******456");
        assertThat(node.get("cccd").asText()).isEqualTo("038******654");
    }

    @Test
    @DisplayName("Mask số thẻ thanh toán/ngân hàng (giữ 4 đầu, 4 cuối)")
    void maskCardNumber_shouldMaskMiddle() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", "9704198526173849");
        data.put("cvv", "123");

        String maskedJson = AuditDataMasker.maskToJson(data);
        JsonNode node = objectMapper.readTree(maskedJson);

        assertThat(node.get("cardNumber").asText()).isEqualTo("9704******3849");
        assertThat(node.get("cvv").asText()).isEqualTo("[PROTECTED]");
    }

    @Test
    @DisplayName("Mask số điện thoại (giữ 3 đầu, 3 cuối)")
    void maskPhone_shouldMaskMiddle() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("phone", "0987654321");
        data.put("phoneNumber", "0912345678");

        String maskedJson = AuditDataMasker.maskToJson(data);
        JsonNode node = objectMapper.readTree(maskedJson);

        assertThat(node.get("phone").asText()).isEqualTo("098****321");
        assertThat(node.get("phoneNumber").asText()).isEqualTo("091****678");
    }

    @Test
    @DisplayName("Hỗ trợ extraMaskFields tùy biến từ @Auditable")
    void extraMaskFields_shouldBeMasked() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("customSecretKey", "my-super-secret-token");
        data.put("hotelCode", "HANOI-01");

        String maskedJson = AuditDataMasker.maskToJson(data, "customSecretKey");
        JsonNode node = objectMapper.readTree(maskedJson);

        assertThat(node.get("customSecretKey").asText()).isEqualTo("[PROTECTED]");
        assertThat(node.get("hotelCode").asText()).isEqualTo("HANOI-01");
    }

    @Test
    @DisplayName("Object null hoặc rỗng -> trả về chuỗi JSON rỗng {} hoặc null an toàn")
    void nullObject_shouldReturnSafeNull() {
        assertThat(AuditDataMasker.maskToJson(null)).isNull();
    }
}
