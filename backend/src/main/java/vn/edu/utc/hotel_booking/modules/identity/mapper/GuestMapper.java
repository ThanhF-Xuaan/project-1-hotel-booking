package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.mapstruct.*;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface GuestMapper {

    GuestResponse toResponse(BookingGuest entity);

    List<GuestResponse> toResponseList(List<BookingGuest> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BookingGuest toEntity(GuestCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget BookingGuest entity, GuestUpdateRequest request);
}
