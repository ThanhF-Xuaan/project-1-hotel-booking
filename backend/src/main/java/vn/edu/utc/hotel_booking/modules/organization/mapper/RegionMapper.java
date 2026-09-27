package vn.edu.utc.hotel_booking.modules.organization.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.RegionResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RegionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Region toEntity(RegionCreateRequest request);

    RegionResponse toResponse(Region entity);

    List<RegionResponse> toResponseList(List<Region> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Region entity, RegionUpdateRequest request);
}
