package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.CampaignResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Campaign;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CampaignMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    CampaignResponse toResponse(Campaign campaign);

    List<CampaignResponse> toResponseList(List<Campaign> campaigns);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Campaign toEntity(CampaignCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Campaign campaign, CampaignUpdateRequest request);
}
