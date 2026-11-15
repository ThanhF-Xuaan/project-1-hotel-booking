package vn.edu.utc.hotel_booking.modules.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.notification.event.BookingConfirmationEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.event.PaymentReceiptEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.service.EmailService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public Notification Test", description = "Endpoint công khai thử nghiệm tính năng gửi Email (Mailpit/SMTP)")
public class PublicNotificationTestController {

    EmailService emailService;

    @PostMapping("/test-email")
    @Operation(summary = "Gửi thử nghiệm 1 email mẫu (BOOKING_CONFIRMATION hoặc PAYMENT_RECEIPT)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(
            @RequestParam(defaultValue = "guest@example.com") String to,
            @RequestParam(defaultValue = "BOOKING_CONFIRMATION") String type
    ) {
        if ("PAYMENT_RECEIPT".equalsIgnoreCase(type)) {
            PaymentReceiptEmailEvent event = PaymentReceiptEmailEvent.builder()
                    .paymentId(9999L)
                    .bookingCode("BK-TEST-20261115")
                    .recipientEmail(to)
                    .customerName("Nguyễn Văn An (Demo)")
                    .hotelName("SmartStay Cầu Giấy Grand")
                    .amount(BigDecimal.valueOf(3402000))
                    .paymentMethod("VNPay (Cổng thanh toán điện tử)")
                    .transactionReference("VNP2026111599999")
                    .paidAt(OffsetDateTime.now())
                    .totalBookingAmount(BigDecimal.valueOf(3402000))
                    .remainingBalance(BigDecimal.ZERO)
                    .currency("VND")
                    .build();
            emailService.sendPaymentReceipt(event);
        } else {
            BookingConfirmationEmailEvent event = BookingConfirmationEmailEvent.builder()
                    .bookingId(9999L)
                    .bookingCode("BK-TEST-20261115")
                    .recipientEmail(to)
                    .customerName("Nguyễn Văn An (Demo)")
                    .hotelName("SmartStay Cầu Giấy Grand")
                    .hotelAddress("Số 3 Cầu Giấy, Láng Thượng, Đống Đa, Hà Nội")
                    .hotelPhone("1900 8888")
                    .checkInDate(LocalDate.now().plusDays(2))
                    .checkOutDate(LocalDate.now().plusDays(5))
                    .roomTypeName("Deluxe King City View")
                    .totalRooms(1)
                    .totalGuests(2)
                    .subtotalAmount(BigDecimal.valueOf(3000000))
                    .serviceFeeAmount(BigDecimal.valueOf(150000))
                    .vatAmount(BigDecimal.valueOf(252000))
                    .totalAmount(BigDecimal.valueOf(3402000))
                    .currency("VND")
                    .portalUrl("http://localhost:3000/my-bookings")
                    .build();
            emailService.sendBookingConfirmation(event);
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "QUEUED",
                "recipient", to,
                "type", type.toUpperCase(),
                "mailpit_web_ui", "http://localhost:8025"
        )));
    }
}
