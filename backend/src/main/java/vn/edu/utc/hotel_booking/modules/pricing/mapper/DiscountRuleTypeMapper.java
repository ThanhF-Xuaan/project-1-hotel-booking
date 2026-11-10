package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DiscountRuleTypeMapper {

    DiscountRuleTypeResponse toResponse(DiscountRuleType entity);

    List<DiscountRuleTypeResponse> toResponseList(List<DiscountRuleType> entities);

    DiscountRuleType toEntity(DiscountRuleTypeCreateRequest request);

    void updateEntityFromRequest(@MappingTarget DiscountRuleType entity, DiscountRuleTypeUpdateRequest request);
}
