package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VatRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VatRuleMapper {

    @Mapping(target = "taxCategoryId", source = "taxCategory.id")
    VatRuleResponse toResponse(VatRule vatRule);

    List<VatRuleResponse> toResponseList(List<VatRule> vatRules);
}
