package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.AuditLogResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.AuditLog;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AuditLogMapper {

    @Mapping(target = "staffId", source = "staff.id")
    @Mapping(target = "staffUsername", source = "staff.username")
    @Mapping(target = "staffFullName", source = "staff.fullName")
    AuditLogResponse toResponse(AuditLog entity);

    List<AuditLogResponse> toResponseList(List<AuditLog> entities);
}
