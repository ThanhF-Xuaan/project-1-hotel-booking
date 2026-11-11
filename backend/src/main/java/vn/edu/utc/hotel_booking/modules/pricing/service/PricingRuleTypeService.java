package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;

import java.util.List;

public interface PricingRuleTypeService {

    List<PricingRuleTypeResponse> getAll();

    List<PricingRuleTypeResponse> getActive();

    PricingRuleTypeResponse getByCode(String code);

    PricingRuleType getEntityByCode(String code);

    PricingRuleTypeResponse create(PricingRuleTypeCreateRequest request);

    PricingRuleTypeResponse update(String code, PricingRuleTypeUpdateRequest request);

    void delete(String code);
}
