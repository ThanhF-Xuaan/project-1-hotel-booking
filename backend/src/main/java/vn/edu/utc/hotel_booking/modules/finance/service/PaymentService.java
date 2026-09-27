package vn.edu.utc.hotel_booking.modules.finance.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(PaymentCreateRequest request);

    PageResponse<PaymentResponse> filter(PaymentSearchDto searchDto);

    PaymentResponse getById(Long id);

    List<PaymentResponse> getByBookingId(Long bookingId);

    PaymentResponse completePayment(Long id);

    PaymentResponse refundPayment(Long id, BigDecimal refundAmount, String reason);
}
