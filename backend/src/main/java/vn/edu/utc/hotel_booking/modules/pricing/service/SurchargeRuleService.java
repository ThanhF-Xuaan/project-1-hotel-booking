package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSurchargePolicyDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.SurchargeRuleResponse;

import java.time.LocalDate;
import java.util.List;

public interface SurchargeRuleService {

    SurchargeRuleResponse getById(Integer id);

    SurchargeRuleResponse create(SurchargeRuleCreateRequest request);

    SurchargeRuleResponse update(Integer id, SurchargeRuleUpdateRequest request);

    PageResponse<SurchargeRuleResponse> filter(SurchargeRuleSearchDto searchDto);

    void deleteBatch(List<Integer> ids);

    List<RoomSurchargePolicyDto> getActiveSurchargePolicies(
            Integer hotelRoomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate
    );
}
