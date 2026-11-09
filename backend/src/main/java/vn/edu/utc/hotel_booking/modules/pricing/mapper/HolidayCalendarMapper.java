package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HolidayCalendarResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HolidayCalendar;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface HolidayCalendarMapper {

    HolidayCalendarResponse toResponse(HolidayCalendar holidayCalendar);

    List<HolidayCalendarResponse> toResponseList(List<HolidayCalendar> list);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    HolidayCalendar toEntity(HolidayCalendarCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(@MappingTarget HolidayCalendar holidayCalendar, HolidayCalendarUpdateRequest request);
}

