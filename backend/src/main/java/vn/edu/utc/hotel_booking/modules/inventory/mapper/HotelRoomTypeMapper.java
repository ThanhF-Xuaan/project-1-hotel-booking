package vn.edu.utc.hotel_booking.modules.inventory.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeBedResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomFeatureResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeBed;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface HotelRoomTypeMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "roomTypeId", source = "roomType.id")
    @Mapping(target = "roomTypeCode", source = "roomType.code")
    @Mapping(target = "roomTypeName", source = "roomType.name")
    @Mapping(target = "beds", source = "beds")
    HotelRoomTypeResponse toResponse(HotelRoomType hotelRoomType);

    List<HotelRoomTypeResponse> toResponseList(List<HotelRoomType> hotelRoomTypes);

    RoomFeatureResponse toFeatureResponse(RoomFeature feature);

    Set<RoomFeatureResponse> toFeatureResponseSet(Set<RoomFeature> features);

    @Mapping(target = "roomBedId", source = "roomBed.id")
    @Mapping(target = "roomBedName", source = "roomBed.name")
    @Mapping(target = "roomBedSize", source = "roomBed.size")
    @Mapping(target = "baseQuantity", source = "baseQuantity")
    HotelRoomTypeBedResponse toBedResponse(HotelRoomTypeBed bed);

    List<HotelRoomTypeBedResponse> toBedResponseList(Collection<HotelRoomTypeBed> beds);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "roomType", ignore = true)
    @Mapping(target = "features", ignore = true)
    @Mapping(target = "beds", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    HotelRoomType toEntity(HotelRoomTypeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "roomType", ignore = true)
    @Mapping(target = "features", ignore = true)
    @Mapping(target = "beds", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget HotelRoomType hotelRoomType, HotelRoomTypeUpdateRequest request);
}
