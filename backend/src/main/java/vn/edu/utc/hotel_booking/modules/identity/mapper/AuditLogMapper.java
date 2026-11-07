package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.AuditLogResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.AuditLog;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AuditLogMapper {

    @Mapping(target = "staffId", source = "staff.id")
    @Mapping(target = "staffUsername", source = "staff.username")
    @Mapping(target = "staffFullName", source = "staff.fullName")
    @Mapping(target = "description", source = "newValues", qualifiedByName = "extractDescription")
    @Mapping(target = "targetAccount", expression = "java(extractTargetResource(entity))")
    @Mapping(target = "targetResource", expression = "java(extractTargetResource(entity))")
    @Mapping(target = "resourceCategory", expression = "java(resolveResourceCategory(entity))")
    AuditLogResponse toResponse(AuditLog entity);

    List<AuditLogResponse> toResponseList(List<AuditLog> entities);

    @Named("extractDescription")
    default String extractDescription(String newValues) {
        if (newValues == null || !newValues.contains("actionDescription")) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(newValues);
            if (root.has("actionDescription")) {
                return root.get("actionDescription").asText();
            }
        } catch (Exception ignored) {}
        return null;
    }

    default String extractTargetAccount(AuditLog entity) {
        return extractTargetResource(entity);
    }

    default String extractTargetResource(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        // 1. Kiểm tra trực tiếp targetResource / targetAccount trong newValues hoặc oldValues
        String explicit = extractFieldFromJson(entity.getNewValues(), "targetResource", "targetAccount");
        if (explicit != null) {
            return explicit;
        }
        explicit = extractFieldFromJson(entity.getOldValues(), "targetResource", "targetAccount");
        if (explicit != null) {
            return explicit;
        }

        String entityName = entity.getEntityName() != null ? entity.getEntityName() : "";
        String entityId = entity.getEntityId();

        // 2. Định dạng thông minh theo loại thực thể
        switch (entityName.toUpperCase()) {
            case "STAFF" -> {
                String fullName = extractFieldFromJson(entity.getOldValues(), "fullName");
                String username = extractFieldFromJson(entity.getOldValues(), "username");
                if (fullName == null) fullName = extractFieldFromJson(entity.getNewValues(), "fullName");
                if (username == null) username = extractFieldFromJson(entity.getNewValues(), "username");

                if (fullName != null && username != null) {
                    return fullName + " (@" + username + ")";
                } else if (username != null) {
                    return "@" + username;
                }
                return entityId != null ? "Nhân sự #" + entityId : "Nhân sự";
            }
            case "PRICINGRULE", "PRICING_RULE" -> {
                String ruleName = extractFieldFromJson(entity.getNewValues(), "name");
                if (ruleName == null) ruleName = extractFieldFromJson(entity.getOldValues(), "name");
                if (ruleName != null) {
                    return ruleName + (entityId != null ? " (#" + entityId + ")" : "");
                }
                return entityId != null ? "Quy tắc giá #" + entityId : "Quy tắc giá";
            }
            case "BOOKING" -> {
                String bookingCode = extractFieldFromJson(entity.getNewValues(), "bookingCode");
                if (bookingCode == null) bookingCode = extractFieldFromJson(entity.getOldValues(), "bookingCode");
                if (bookingCode != null) {
                    return "Đơn " + bookingCode;
                }
                return entityId != null ? "Đơn đặt #" + entityId : "Đơn đặt phòng";
            }
            case "BOOKINGROOM", "BOOKING_ROOM" -> {
                return entityId != null ? "Phòng đặt #" + entityId : "Phòng đặt";
            }
            case "PAYMENT" -> {
                return entityId != null ? "Giao dịch #" + entityId : "Thanh toán";
            }
            case "HOTEL" -> {
                String hotelName = extractFieldFromJson(entity.getNewValues(), "name");
                if (hotelName == null) hotelName = extractFieldFromJson(entity.getOldValues(), "name");
                if (hotelName != null) {
                    return hotelName + (entityId != null ? " (#" + entityId + ")" : "");
                }
                return entityId != null ? "Khách sạn #" + entityId : "Khách sạn";
            }
            default -> {
                return entityId != null ? entityName + " #" + entityId : entityName;
            }
        }
    }

    default String resolveResourceCategory(AuditLog entity) {
        if (entity == null || entity.getEntityName() == null) {
            return "OTHER";
        }
        String name = entity.getEntityName().toUpperCase();
        if (name.contains("STAFF") || name.contains("USER") || name.contains("AUTH")) {
            return "STAFF";
        }
        if (name.contains("BOOKING")) {
            return "BOOKING";
        }
        if (name.contains("PAYMENT") || name.contains("FOLIO") || name.contains("INVOICE")) {
            return "FINANCE";
        }
        if (name.contains("PRICING") || name.contains("RULE") || name.contains("VOUCHER")) {
            return "PRICING";
        }
        if (name.contains("HOTEL") || name.contains("ROOM") || name.contains("MEDIA")) {
            return "ORGANIZATION";
        }
        return "OTHER";
    }

    private String extractFieldFromJson(String jsonStr, String... fieldNames) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonStr);
            for (String fieldName : fieldNames) {
                if (root.has(fieldName) && !root.get(fieldName).isNull()) {
                    String val = root.get(fieldName).asText();
                    if (!val.isBlank()) return val;
                }
            }
            // Kiểm tra lồng trong request hoặc params
            if (root.has("request") && root.get("request").isObject()) {
                com.fasterxml.jackson.databind.JsonNode req = root.get("request");
                for (String fieldName : fieldNames) {
                    if (req.has(fieldName) && !req.get(fieldName).isNull()) {
                        String val = req.get(fieldName).asText();
                        if (!val.isBlank()) return val;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
