package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;

public interface ServiceOrderService {

    ServiceOrderResponse createOrder(ServiceOrderCreateRequest request);

    ServiceOrderResponse getById(Long id);

    PageResponse<ServiceOrderResponse> filter(ServiceOrderSearchDto searchDto);

    ServiceOrderResponse updateStatus(Long id, ServiceOrderStatus status);
}
