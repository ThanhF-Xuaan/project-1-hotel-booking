package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;

import java.util.List;

public interface ServiceOrderService {

    ServiceOrderResponse createOrder(ServiceOrderCreateRequest request);

    ServiceOrderResponse getById(Long id);

    List<ServiceOrderResponse> getOrdersByBookingId(Long bookingId);

    PageResponse<ServiceOrderResponse> filter(ServiceOrderSearchDto searchDto);

    ServiceOrderResponse updateStatus(Long id, ServiceOrderStatus status);
}
