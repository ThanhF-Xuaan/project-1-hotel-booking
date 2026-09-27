package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;

import java.util.List;

public interface HotelRoomTypeService {

    HotelRoomTypeResponse createHotelRoomType(HotelRoomTypeCreateRequest request);

    HotelRoomTypeResponse updateHotelRoomType(Integer id, HotelRoomTypeUpdateRequest request);

    HotelRoomTypeResponse getHotelRoomTypeById(Integer id);

    PageResponse<HotelRoomTypeResponse> filterHotelRoomTypes(HotelRoomTypeSearchDto searchDto);

    void deleteHotelRoomTypes(List<Integer> ids);
}
