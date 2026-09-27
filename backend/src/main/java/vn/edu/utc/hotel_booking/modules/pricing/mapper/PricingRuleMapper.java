package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PricingRuleMapper {

    @Mapping(target = "hotelRoomTypeId", source = "hotelRoomType.id")
    @Mapping(target = "roomTypeName", source = "hotelRoomType.roomType.name")
    @Mapping(target = "holidayCalendarId", source = "holidayCalendar.id")
    @Mapping(target = "ruleTypeCode", source = "ruleType.code")
    @Mapping(target = "ruleTypeName", source = "ruleType.displayName")
    PricingRuleResponse toResponse(PricingRule pricingRule);

    List<PricingRuleResponse> toResponseList(List<PricingRule> pricingRules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelRoomType", ignore = true)
    @Mapping(target = "holidayCalendar", ignore = true)
    @Mapping(target = "ruleType", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PricingRule toEntity(PricingRuleCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelRoomType", ignore = true)
    @Mapping(target = "holidayCalendar", ignore = true)
    @Mapping(target = "ruleType", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget PricingRule pricingRule, PricingRuleUpdateRequest request);
}
