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
    @Mapping(target = "stockQuantity", source = "catalogItem.stockQuantity")
    @Mapping(target = "pricingType", source = "hotelServiceItem.pricingType")
    MenuResponse toResponse(Menu entity);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "bookingNumber", source = "booking.bookingNumber")
    @Mapping(target = "roomInstanceId", source = "roomInstance.id")
    @Mapping(target = "roomNumber", source = "roomInstance.roomNumber")
    @Mapping(target = "details", source = "details")
    ServiceOrderResponse toResponse(ServiceOrder entity);

    @Mapping(target = "menuId", source = "menu.id")
    ServiceOrderDetailResponse toDetailResponse(ServiceOrderDetail entity);
}
