package vn.edu.utc.hotel_booking.modules.inventory.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RoomTypeMapper {

    RoomTypeResponse toResponse(RoomType roomType);

    List<RoomTypeResponse> toResponseList(List<RoomType> roomTypes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RoomType toEntity(RoomTypeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget RoomType roomType, RoomTypeUpdateRequest request);
}
