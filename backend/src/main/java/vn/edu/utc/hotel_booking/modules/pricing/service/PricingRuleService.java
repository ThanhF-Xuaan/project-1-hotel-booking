package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleResponse;

import java.util.List;

public interface PricingRuleService {

    PricingRuleResponse createPricingRule(PricingRuleCreateRequest request);

    PricingRuleResponse updatePricingRule(Integer id, PricingRuleUpdateRequest request);

    PricingRuleResponse getPricingRuleById(Integer id);

    PageResponse<PricingRuleResponse> filterPricingRules(PricingRuleSearchDto searchDto);

    void deletePricingRules(List<Integer> ids);
}
