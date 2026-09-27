package vn.edu.utc.hotel_booking.modules.organization.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface HotelMapper {

    @Mapping(target = "regionId", source = "region.id")
    @Mapping(target = "regionName", source = "region.name")
    @Mapping(target = "regionCode", source = "region.code")
    HotelResponse toResponse(Hotel entity);

    List<HotelResponse> toResponseList(List<Hotel> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "region", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Hotel toEntity(HotelCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "region", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Hotel entity, HotelUpdateRequest request);
}
