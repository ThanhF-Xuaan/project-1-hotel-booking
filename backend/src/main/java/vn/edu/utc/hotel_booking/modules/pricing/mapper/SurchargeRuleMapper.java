package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.SurchargeRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SurchargeRuleMapper {

    @Mapping(target = "hotelRoomTypeId", source = "hotelRoomType.id")
    @Mapping(target = "roomTypeName", source = "hotelRoomType.roomType.name")
    @Mapping(target = "hotelId", source = "hotelRoomType.hotel.id")
    @Mapping(target = "hotelName", source = "hotelRoomType.hotel.name")
    @Mapping(target = "agePolicyId", source = "agePolicy.id")
    @Mapping(target = "agePolicyGuestType", source = "agePolicy.guestType")
    @Mapping(target = "agePolicyMinAge", source = "agePolicy.minAge")
    @Mapping(target = "agePolicyMaxAge", source = "agePolicy.maxAge")
    SurchargeRuleResponse toResponse(SurchargeRule surchargeRule);

    List<SurchargeRuleResponse> toResponseList(List<SurchargeRule> surchargeRules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelRoomType", ignore = true)
    @Mapping(target = "agePolicy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SurchargeRule toEntity(SurchargeRuleCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelRoomType", ignore = true)
    @Mapping(target = "agePolicy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget SurchargeRule surchargeRule, SurchargeRuleUpdateRequest request);
}
