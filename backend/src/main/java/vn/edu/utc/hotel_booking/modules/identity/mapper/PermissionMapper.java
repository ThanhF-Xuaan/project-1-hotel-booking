package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PermissionMapper {

    PermissionResponse toResponse(Permission entity);

    List<PermissionResponse> toResponseList(List<Permission> entities);

    Set<PermissionResponse> toResponseSet(Set<Permission> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Permission toEntity(PermissionCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Permission entity, PermissionUpdateRequest request);
}
