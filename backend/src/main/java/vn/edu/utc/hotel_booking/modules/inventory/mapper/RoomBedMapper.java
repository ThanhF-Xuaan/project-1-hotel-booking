package vn.edu.utc.hotel_booking.modules.inventory.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomBedResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RoomBedMapper {

    RoomBedResponse toResponse(RoomBed roomBed);

    List<RoomBedResponse> toResponseList(List<RoomBed> roomBeds);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RoomBed toEntity(RoomBedCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget RoomBed roomBed, RoomBedUpdateRequest request);
}

