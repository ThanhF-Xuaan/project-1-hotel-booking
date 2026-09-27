package vn.edu.utc.hotel_booking.modules.finance.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.InvoiceDetailResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.InvoiceResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.TransactionResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.Invoice;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceDetail;
import vn.edu.utc.hotel_booking.modules.finance.entity.Payment;
import vn.edu.utc.hotel_booking.modules.finance.entity.Transaction;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface FinanceMapper {

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "bookingNumber", source = "booking.bookingNumber")
    PaymentResponse toResponse(Payment entity);

    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "bookingId", source = "booking.id")
    TransactionResponse toResponse(Transaction entity);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "bookingNumber", source = "booking.bookingNumber")
    @Mapping(target = "details", source = "details")
    InvoiceResponse toResponse(Invoice entity);

    InvoiceDetailResponse toDetailResponse(InvoiceDetail entity);
}
