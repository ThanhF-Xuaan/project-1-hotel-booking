package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.NoShowPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;

import java.util.List;

public interface NoShowPolicyService {

    NoShowPolicyResponse create(NoShowPolicyCreateRequest request);

    NoShowPolicyResponse update(Integer id, NoShowPolicyUpdateRequest request);

    void delete(List<Integer> ids);

    NoShowPolicyResponse getById(Integer id);

    NoShowPolicy getEntityById(Integer id);

    NoShowPolicy resolveDefaultChainPolicy(BookingType bookingType);

    PageResponse<NoShowPolicyResponse> filter(NoShowPolicySearchDto searchDto);
}
