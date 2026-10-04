package vn.edu.utc.hotel_booking.modules.operation.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.LodgingQueueResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;

import java.util.List;

@Mapper(componentModel = "spring", uses = {StayGuestMapper.class}, builder = @Builder(disableBuilder = true))
public interface LodgingQueueMapper {

    @Mapping(target = "stayGuestId", source = "stayGuest.id")
    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    LodgingQueueResponse toResponse(LodgingQueue entity);

    List<LodgingQueueResponse> toResponseList(List<LodgingQueue> entities);
}
