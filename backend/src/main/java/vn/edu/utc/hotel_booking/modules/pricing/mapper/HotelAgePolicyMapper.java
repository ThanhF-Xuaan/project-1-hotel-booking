package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HotelAgePolicyResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HotelAgePolicy;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface HotelAgePolicyMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", expression = "java(hotelAgePolicy.getHotel() != null ? hotelAgePolicy.getHotel().getName() : \"Toàn chuỗi (Mặc định)\")")
    @Mapping(target = "isChainDefault", expression = "java(hotelAgePolicy.getHotel() == null)")
    HotelAgePolicyResponse toResponse(HotelAgePolicy hotelAgePolicy);

    List<HotelAgePolicyResponse> toResponseList(List<HotelAgePolicy> policies);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    HotelAgePolicy toEntity(HotelAgePolicyCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "guestType", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(@MappingTarget HotelAgePolicy hotelAgePolicy, HotelAgePolicyUpdateRequest request);
}

