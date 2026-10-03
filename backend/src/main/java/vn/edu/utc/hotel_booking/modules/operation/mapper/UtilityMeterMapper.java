package vn.edu.utc.hotel_booking.modules.operation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityMeterResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UtilityMeterMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    UtilityMeterResponse toResponse(UtilityMeter entity);
    
    List<UtilityMeterResponse> toResponseList(List<UtilityMeter> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    UtilityMeter toEntity(UtilityMeterCreateRequest request);
}
