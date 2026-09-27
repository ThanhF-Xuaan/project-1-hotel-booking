package vn.edu.utc.hotel_booking.modules.inventory.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RoomAvailabilityMapper {

    @Mapping(target = "hotelRoomTypeId", source = "hotelRoomType.id")
    RoomAvailabilityResponse toResponse(RoomAvailability roomAvailability);

    List<RoomAvailabilityResponse> toResponseList(List<RoomAvailability> roomAvailabilities);
}
