package vn.edu.utc.hotel_booking.modules.organization.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.DepartmentResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DepartmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Department toEntity(DepartmentCreateRequest request);

    DepartmentResponse toResponse(Department entity);

    List<DepartmentResponse> toResponseList(List<Department> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Department entity, DepartmentUpdateRequest request);
}
