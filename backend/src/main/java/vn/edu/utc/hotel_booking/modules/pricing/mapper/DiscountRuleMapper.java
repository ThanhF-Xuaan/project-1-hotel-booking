package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DiscountRuleMapper {

    @Mapping(target = "hotelRoomTypeId", source = "hotelRoomType.id")
    @Mapping(target = "hotelRoomTypeName", source = "hotelRoomType.roomType.name")
    @Mapping(target = "hotelId", source = "hotelRoomType.hotel.id")
    @Mapping(target = "hotelName", source = "hotelRoomType.hotel.name")
    @Mapping(target = "campaignId", source = "campaign.id")
    @Mapping(target = "campaignName", source = "campaign.name")
    @Mapping(target = "ruleTypeCode", source = "ruleType.code")
    @Mapping(target = "ruleTypeDisplayName", source = "ruleType.displayName")
    DiscountRuleResponse toResponse(DiscountRule discountRule);

    List<DiscountRuleResponse> toResponseList(List<DiscountRule> discountRules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelRoomType", ignore = true)
    @Mapping(target = "campaign", ignore = true)
    @Mapping(target = "ruleType", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DiscountRule toEntity(DiscountRuleCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelRoomType", ignore = true)
    @Mapping(target = "campaign", ignore = true)
    @Mapping(target = "ruleType", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget DiscountRule discountRule, DiscountRuleUpdateRequest request);
}
