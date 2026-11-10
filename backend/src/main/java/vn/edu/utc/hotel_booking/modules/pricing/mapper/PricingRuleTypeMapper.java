package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PricingRuleTypeMapper {

    PricingRuleTypeResponse toResponse(PricingRuleType entity);

    List<PricingRuleTypeResponse> toResponseList(List<PricingRuleType> entities);

    PricingRuleType toEntity(PricingRuleTypeCreateRequest request);

    void updateEntityFromRequest(@MappingTarget PricingRuleType entity, PricingRuleTypeUpdateRequest request);
}
