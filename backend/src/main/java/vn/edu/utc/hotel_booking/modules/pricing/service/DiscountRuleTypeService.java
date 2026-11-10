package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;

import java.util.List;

public interface DiscountRuleTypeService {

    List<DiscountRuleTypeResponse> getAll();

    List<DiscountRuleTypeResponse> getActive();

    DiscountRuleTypeResponse getByCode(String code);

    DiscountRuleType getEntityByCode(String code);

    DiscountRuleTypeResponse create(DiscountRuleTypeCreateRequest request);

    DiscountRuleTypeResponse update(String code, DiscountRuleTypeUpdateRequest request);

    void delete(String code);
}
