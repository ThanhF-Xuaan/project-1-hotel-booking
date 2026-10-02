package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface StaffMapper {

    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "roleCode", source = "role.code")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    StaffResponse toResponse(Staff entity);

    List<StaffResponse> toResponseList(List<Staff> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Staff toEntity(StaffCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Staff entity, StaffUpdateRequest request);
}
