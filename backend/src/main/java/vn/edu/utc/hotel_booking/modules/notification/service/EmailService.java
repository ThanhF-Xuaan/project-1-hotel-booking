package vn.edu.utc.hotel_booking.modules.notification.service;

import vn.edu.utc.hotel_booking.modules.notification.event.BookingConfirmationEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.event.PaymentReceiptEmailEvent;

public interface EmailService {

    void sendBookingConfirmation(BookingConfirmationEmailEvent event);

    void sendPaymentReceipt(PaymentReceiptEmailEvent event);
}
