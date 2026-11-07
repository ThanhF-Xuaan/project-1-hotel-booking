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
    @Mapping(target = "targetAccount", expression = "java(extractTargetAccount(entity))")
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
        if (entity == null) {
            return null;
        }
        if (entity.getNewValues() != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(entity.getNewValues());
                if (root.has("targetAccount") && !root.get("targetAccount").isNull()) {
                    return root.get("targetAccount").asText();
                }
            } catch (Exception ignored) {}
        }
        if (entity.getOldValues() != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(entity.getOldValues());
                if (root.has("targetAccount") && !root.get("targetAccount").isNull()) {
                    return root.get("targetAccount").asText();
                }
            } catch (Exception ignored) {}
        }
        if ("Staff".equalsIgnoreCase(entity.getEntityName()) && entity.getEntityId() != null) {
            return "Staff #" + entity.getEntityId();
        }
        return entity.getEntityId() != null ? entity.getEntityName() + " #" + entity.getEntityId() : null;
    }
}
