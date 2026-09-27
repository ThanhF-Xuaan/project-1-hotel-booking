package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.RoleResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class}, builder = @Builder(disableBuilder = true))
public interface RoleMapper {

    RoleResponse toResponse(Role entity);

    List<RoleResponse> toResponseList(List<Role> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Role toEntity(RoleCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Role entity, RoleUpdateRequest request);
}
