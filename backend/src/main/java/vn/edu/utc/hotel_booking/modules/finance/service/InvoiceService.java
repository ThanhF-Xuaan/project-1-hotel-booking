package vn.edu.utc.hotel_booking.modules.finance.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.InvoiceResponse;

public interface InvoiceService {

    InvoiceResponse createInvoice(InvoiceCreateRequest request);

    PageResponse<InvoiceResponse> filter(InvoiceSearchDto searchDto);

    InvoiceResponse getById(Long id);

    InvoiceResponse getByBookingId(Long bookingId);

    InvoiceResponse issueInvoice(Long id);

    /**
     * Quyết toán và đóng hóa đơn Folio (PAID) khi hoàn tất check-out
     *
     * @param bookingId ID đơn đặt phòng
     * @return Hóa đơn đã đóng sổ
     */
    InvoiceResponse settleAndCloseInvoice(Long bookingId);
}

