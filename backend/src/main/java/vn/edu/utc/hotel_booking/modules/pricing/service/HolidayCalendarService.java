package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HolidayCalendarResponse;

import java.util.List;

public interface HolidayCalendarService {

    HolidayCalendarResponse getById(Integer id);

    HolidayCalendarResponse create(HolidayCalendarCreateRequest request);

    HolidayCalendarResponse update(Integer id, HolidayCalendarUpdateRequest request);

    PageResponse<HolidayCalendarResponse> filter(HolidayCalendarSearchDto searchDto);

    void deleteBatch(List<Integer> ids);

    List<HolidayCalendarResponse> getActiveHolidaysInYear(Integer year);
}

