package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PermissionMapper {

    PermissionResponse toResponse(Permission entity);

    List<PermissionResponse> toResponseList(List<Permission> entities);

    Set<PermissionResponse> toResponseSet(Set<Permission> entities);
}
