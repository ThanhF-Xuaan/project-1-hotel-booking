package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleResponse;

import java.util.List;

public interface DiscountRuleService {

    DiscountRuleResponse createDiscountRule(DiscountRuleCreateRequest request);

    DiscountRuleResponse updateDiscountRule(Integer id, DiscountRuleUpdateRequest request);

    DiscountRuleResponse getDiscountRuleById(Integer id);

    PageResponse<DiscountRuleResponse> filterDiscountRules(DiscountRuleSearchDto searchDto);

    void deleteDiscountRules(List<Integer> ids);
}
