package vn.edu.utc.hotel_booking.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Bộ tiện ích làm sạch và ẩn danh hóa dữ liệu (Data Masking Sanitizer).
 * Tuân thủ quy định tại Nghị định 13/2023/NĐ-CP (Bảo vệ dữ liệu cá nhân - PDPD).
 */
@UtilityClass
@Slf4j
public class AuditDataMasker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Set<String> DEFAULT_PROTECTED_FIELDS = new HashSet<>(Arrays.asList(
            "password", "newpassword", "oldpassword", "confirmpassword",
            "secret", "clientsecret", "token", "accesstoken", "refreshtoken",
            "cvv", "cvc", "pin", "otp"
    ));

    private static final Set<String> CITIZEN_ID_FIELDS = new HashSet<>(Arrays.asList(
            "citizenid", "cccd", "cmnd", "passport", "nationalid", "idcard"
    ));

    private static final Set<String> CARD_NUMBER_FIELDS = new HashSet<>(Arrays.asList(
            "cardnumber", "creditcard", "bankaccount", "accountnumber"
    ));

    private static final Set<String> PHONE_FIELDS = new HashSet<>(Arrays.asList(
            "phone", "phonenumber", "telephone", "mobilephone"
    ));

    public static String maskToJson(Object object, String... extraMaskFields) {
        if (object == null) {
            return null;
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.valueToTree(object);
            Set<String> extraFieldsSet = new HashSet<>();
            if (extraMaskFields != null) {
                for (String field : extraMaskFields) {
                    if (StringUtils.hasText(field)) {
                        extraFieldsSet.add(field.toLowerCase().replaceAll("[^a-z0-9]", ""));
                    }
                }
            }

            maskNode(rootNode, extraFieldsSet);
            return OBJECT_MAPPER.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            log.warn("Không thể chuyển đổi object thành JSON để ghi audit log: {}", e.getMessage());
            return "{\"error\":\"Serialization failed\"}";
        } catch (Exception e) {
            log.warn("Lỗi khi mask dữ liệu audit log: {}", e.getMessage());
            return "{\"error\":\"Masking failed\"}";
        }
    }

    public static String maskJsonString(String jsonString, String... extraMaskFields) {
        if (!StringUtils.hasText(jsonString)) {
            return jsonString;
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonString);
            Set<String> extraFieldsSet = new HashSet<>();
            if (extraMaskFields != null) {
                for (String field : extraMaskFields) {
                    if (StringUtils.hasText(field)) {
                        extraFieldsSet.add(field.toLowerCase().replaceAll("[^a-z0-9]", ""));
                    }
                }
            }

            maskNode(rootNode, extraFieldsSet);
            return OBJECT_MAPPER.writeValueAsString(rootNode);
        } catch (Exception e) {
            log.warn("Chuỗi không phải JSON hợp lệ để mask: {}", e.getMessage());
            return jsonString;
        }
    }

    private static void maskNode(JsonNode node, Set<String> extraFields) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            List<String> fieldNames = new ArrayList<>();
            while (fields.hasNext()) {
                fieldNames.add(fields.next().getKey());
            }

            for (String fieldName : fieldNames) {
                JsonNode child = objectNode.get(fieldName);
                String normalizedName = fieldName.toLowerCase().replaceAll("[^a-z0-9]", "");

                if (DEFAULT_PROTECTED_FIELDS.contains(normalizedName) || extraFields.contains(normalizedName)) {
                    objectNode.set(fieldName, TextNode.valueOf("[PROTECTED]"));
                } else if (CITIZEN_ID_FIELDS.contains(normalizedName) && child.isTextual()) {
                    objectNode.set(fieldName, TextNode.valueOf(maskCitizenId(child.asText())));
                } else if (CARD_NUMBER_FIELDS.contains(normalizedName) && child.isTextual()) {
                    objectNode.set(fieldName, TextNode.valueOf(maskCardNumber(child.asText())));
                } else if (PHONE_FIELDS.contains(normalizedName) && child.isTextual()) {
                    objectNode.set(fieldName, TextNode.valueOf(maskPhone(child.asText())));
                } else {
                    maskNode(child, extraFields);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                maskNode(arrayNode.get(i), extraFields);
            }
        }
    }

    public static String maskCitizenId(String citizenId) {
        if (!StringUtils.hasText(citizenId)) {
            return citizenId;
        }
        String clean = citizenId.trim();
        if (clean.length() < 7) {
            return "******";
        }
        return clean.substring(0, 3) + "******" + clean.substring(clean.length() - 3);
    }

    public static String maskCardNumber(String cardNumber) {
        if (!StringUtils.hasText(cardNumber)) {
            return cardNumber;
        }
        String clean = cardNumber.replaceAll("\\s+", "");
        if (clean.length() < 9) {
            return "******";
        }
        return clean.substring(0, 4) + "******" + clean.substring(clean.length() - 4);
    }

    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String clean = phone.trim();
        if (clean.length() < 7) {
            return "******";
        }
        return clean.substring(0, 3) + "****" + clean.substring(clean.length() - 3);
    }
}
