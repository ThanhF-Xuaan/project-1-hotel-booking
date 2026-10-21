package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CancellationPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;

import java.util.List;

public interface CancellationPolicyService {

    CancellationPolicyResponse create(CancellationPolicyCreateRequest request);

    CancellationPolicyResponse update(Integer id, CancellationPolicyUpdateRequest request);

    void delete(List<Integer> ids);

    CancellationPolicyResponse getById(Integer id);

    CancellationPolicy getEntityById(Integer id);

    CancellationPolicy resolveDefaultChainPolicy(BookingType bookingType);

    PageResponse<CancellationPolicyResponse> filter(CancellationPolicySearchDto searchDto);
}
