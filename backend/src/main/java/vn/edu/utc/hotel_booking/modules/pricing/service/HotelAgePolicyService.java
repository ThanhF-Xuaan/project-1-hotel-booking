package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HotelAgePolicyResponse;

import java.util.List;

public interface HotelAgePolicyService {

    HotelAgePolicyResponse getById(Short id);

    HotelAgePolicyResponse create(HotelAgePolicyCreateRequest request);

    HotelAgePolicyResponse update(Short id, HotelAgePolicyUpdateRequest request);

    PageResponse<HotelAgePolicyResponse> filter(HotelAgePolicySearchDto searchDto);

    void deleteBatch(List<Short> ids);

    List<HotelAgePolicyResponse> getEffectivePoliciesForHotel(Short hotelId);

    String resolveGuestTypeByAge(Short hotelId, int age);
}

