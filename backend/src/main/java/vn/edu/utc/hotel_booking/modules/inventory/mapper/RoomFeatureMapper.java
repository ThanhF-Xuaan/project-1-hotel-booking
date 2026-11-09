package vn.edu.utc.hotel_booking.modules.inventory.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomFeatureResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RoomFeatureMapper {

    RoomFeatureResponse toResponse(RoomFeature roomFeature);

    List<RoomFeatureResponse> toResponseList(List<RoomFeature> roomFeatures);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RoomFeature toEntity(RoomFeatureCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget RoomFeature roomFeature, RoomFeatureUpdateRequest request);
}

