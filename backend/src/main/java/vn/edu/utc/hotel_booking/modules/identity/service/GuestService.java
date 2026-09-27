package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestResponse;

import java.util.List;
import java.util.UUID;

public interface GuestService {

    PageResponse<GuestResponse> filter(GuestSearchDto searchDto);

    GuestResponse getById(Long id);

    GuestResponse getByPublicId(UUID publicId);

    GuestResponse getByPhone(String phone);

    GuestResponse create(GuestCreateRequest request);

    GuestResponse update(Long id, GuestUpdateRequest request);

    void deleteBatch(List<Long> ids);
}
