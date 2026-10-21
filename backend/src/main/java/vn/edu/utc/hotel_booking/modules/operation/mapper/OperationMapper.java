package vn.edu.utc.hotel_booking.modules.operation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderDetailResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderDetail;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface OperationMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "taxCategoryId", source = "taxCategory.id")
    @Mapping(target = "taxCategoryName", source = "taxCategory.categoryName")
    @Mapping(target = "masterItemId", source = "masterItem.id")
    @Mapping(target = "masterItemCode", source = "masterItem.code")
    @Mapping(target = "customName", source = "customName")
    @Mapping(target = "stockQuantity", source = "stockQuantity")
    @Mapping(target = "pricingType", source = "masterItem.pricingType")
    MenuResponse toResponse(Menu entity);

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "referenceType", source = "referenceType")
    @Mapping(target = "referenceId", source = "referenceId")
    @Mapping(target = "bookingId", expression = "java(entity.getBookingId())")
    @Mapping(target = "bookingNumber", ignore = true)
    @Mapping(target = "roomInstanceId", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    @Mapping(target = "details", source = "details")
    ServiceOrderResponse toResponse(ServiceOrder entity);

    @Mapping(target = "menuId", source = "menu.id")
    ServiceOrderDetailResponse toDetailResponse(ServiceOrderDetail entity);
}
