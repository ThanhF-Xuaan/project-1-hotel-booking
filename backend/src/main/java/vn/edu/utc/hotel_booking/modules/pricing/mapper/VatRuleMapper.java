package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VatRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VatRuleMapper {

    @Mapping(target = "taxCategoryId", source = "taxCategory.id")
    @Mapping(target = "taxCategoryCode", source = "taxCategory.categoryCode")
    @Mapping(target = "taxCategoryName", source = "taxCategory.categoryName")
    VatRuleResponse toResponse(VatRule vatRule);

    List<VatRuleResponse> toResponseList(List<VatRule> vatRules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taxCategory", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    VatRule toEntity(VatRuleCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taxCategory", ignore = true)
    @Mapping(target = "vatCode", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(@MappingTarget VatRule vatRule, VatRuleUpdateRequest request);
}
