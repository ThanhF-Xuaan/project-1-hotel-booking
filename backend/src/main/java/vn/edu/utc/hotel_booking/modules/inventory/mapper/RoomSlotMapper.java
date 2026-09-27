package vn.edu.utc.hotel_booking.modules.inventory.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomSlotResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RoomSlotMapper {

    @Mapping(target = "roomInstanceId", source = "roomInstance.id")
    @Mapping(target = "roomNumber", source = "roomInstance.roomNumber")
    RoomSlotResponse toResponse(RoomSlot roomSlot);

    List<RoomSlotResponse> toResponseList(List<RoomSlot> roomSlots);
}
