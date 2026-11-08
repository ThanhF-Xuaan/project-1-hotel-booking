package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.TaxCategoryResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TaxCategoryMapper {

    TaxCategoryResponse toResponse(TaxCategory taxCategory);

    List<TaxCategoryResponse> toResponseList(List<TaxCategory> taxCategories);

    TaxCategory toEntity(TaxCategoryCreateRequest request);

    void updateEntityFromRequest(@MappingTarget TaxCategory taxCategory, TaxCategoryUpdateRequest request);
}
