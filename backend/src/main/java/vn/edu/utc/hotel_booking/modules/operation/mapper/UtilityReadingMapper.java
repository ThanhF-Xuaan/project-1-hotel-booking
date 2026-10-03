package vn.edu.utc.hotel_booking.modules.operation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityReadingResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityReading;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UtilityReadingMapper {

    @Mapping(target = "meterId", source = "meter.id")
    @Mapping(target = "meterCode", source = "meter.meterCode")
    @Mapping(target = "usage", ignore = true) // Set manually in service
    UtilityReadingResponse toResponse(UtilityReading entity);
    
    List<UtilityReadingResponse> toResponseList(List<UtilityReading> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "meter", ignore = true)
    @Mapping(target = "recordedBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    UtilityReading toEntity(UtilityReadingCreateRequest request);
}
