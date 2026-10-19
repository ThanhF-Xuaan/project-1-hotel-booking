package vn.edu.utc.hotel_booking.modules.inventory.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomMaintenanceBlockResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomMaintenanceBlock;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RoomMaintenanceBlockMapper {

    @Mapping(target = "roomInstanceId", source = "roomInstance.id")
    @Mapping(target = "roomNumber", source = "roomInstance.roomNumber")
    @Mapping(target = "hotelId", source = "roomInstance.hotel.id")
    @Mapping(target = "hotelName", source = "roomInstance.hotel.name")
    @Mapping(target = "hotelRoomTypeId", source = "roomInstance.hotelRoomType.id")
    @Mapping(target = "roomTypeName", source = "roomInstance.hotelRoomType.roomType.name")
    RoomMaintenanceBlockResponse toResponse(RoomMaintenanceBlock entity);

    List<RoomMaintenanceBlockResponse> toResponseList(List<RoomMaintenanceBlock> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roomInstance", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByStaffId", ignore = true)
    @Mapping(target = "updatedByStaffId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RoomMaintenanceBlock toEntity(RoomMaintenanceBlockCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roomInstance", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByStaffId", ignore = true)
    @Mapping(target = "updatedByStaffId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget RoomMaintenanceBlock entity, RoomMaintenanceBlockUpdateRequest request);
}
