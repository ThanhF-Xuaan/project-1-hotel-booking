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

    /**
     * Tính tổng doanh thu dịch vụ phát sinh trong ngày của khách sạn
     *
     * @param hotelId ID khách sạn
     * @param date Ngày kiểm toán
     * @return Tổng doanh thu dịch vụ
     */
    java.math.BigDecimal getDailyServiceRevenue(Short hotelId, java.time.LocalDate date);

    /**
     * Đánh dấu toàn bộ các đơn dịch vụ POST_TO_ROOM của booking là đã thanh toán (PAID) khi đóng sổ check-out
     *
     * @param bookingId ID đơn đặt phòng
     */
    void markOrdersPaidForBooking(Long bookingId);
}

