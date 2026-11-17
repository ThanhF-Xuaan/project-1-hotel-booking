package vn.edu.utc.hotel_booking.modules.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingChargeRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.finance.entity.Payment;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.notification.dto.ServiceItemEmailDto;
import vn.edu.utc.hotel_booking.modules.notification.event.BookingConfirmationEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.event.PaymentReceiptEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.service.EmailService;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public Notification Test", description = "Endpoint công khai thử nghiệm và gửi lại thông báo Email theo đơn thực tế")
public class PublicNotificationTestController {

    EmailService emailService;
    BookingRepository bookingRepository;
    PaymentRepository paymentRepository;
    ServiceOrderService serviceOrderService;
    BookingChargeRepository bookingChargeRepository;

    @PostMapping("/test-email")
    @Operation(summary = "Gửi email thông báo (tự động lấy theo mã booking thực tế nếu truyền hoặc lấy đơn mới nhất)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "BOOKING_CONFIRMATION") String type,
            @RequestParam(required = false) String bookingNumber
    ) {
        Booking booking = findTargetBooking(bookingNumber);
        Map<String, Object> resultInfo = new HashMap<>();

        if (booking != null) {
            String targetEmail = (to != null && !to.isBlank())
                    ? to.trim()
                    : (booking.getGuest() != null ? booking.getGuest().getEmail() : "guest@example.com");

            if ("PAYMENT_RECEIPT".equalsIgnoreCase(type)) {
                PaymentReceiptEmailEvent event = buildPaymentReceiptEvent(booking, targetEmail);
                emailService.sendPaymentReceipt(event);
                resultInfo.put("event", event);
            } else if ("BOTH".equalsIgnoreCase(type) || "ALL".equalsIgnoreCase(type)) {
                BookingConfirmationEmailEvent bookingEvent = buildBookingConfirmationEvent(booking, targetEmail);
                emailService.sendBookingConfirmation(bookingEvent);
                PaymentReceiptEmailEvent paymentEvent = buildPaymentReceiptEvent(booking, targetEmail);
                emailService.sendPaymentReceipt(paymentEvent);
                resultInfo.put("bookingEvent", bookingEvent);
                resultInfo.put("paymentEvent", paymentEvent);
            } else {
                BookingConfirmationEmailEvent event = buildBookingConfirmationEvent(booking, targetEmail);
                emailService.sendBookingConfirmation(event);
                resultInfo.put("event", event);
            }

            resultInfo.put("status", "SENT");
            resultInfo.put("source", "REAL_BOOKING");
            resultInfo.put("bookingCode", booking.getBookingNumber());
            resultInfo.put("recipient", targetEmail);
            resultInfo.put("customerName", booking.getGuest() != null ? booking.getGuest().getFullName() : "N/A");
            resultInfo.put("totalAmount", booking.getTotalAmount());
        } else {
            // Fallback sang mock data nếu không có đơn nào trong DB
            String fallbackTo = (to != null && !to.isBlank()) ? to.trim() : "guest@example.com";
            if ("PAYMENT_RECEIPT".equalsIgnoreCase(type)) {
                PaymentReceiptEmailEvent event = PaymentReceiptEmailEvent.builder()
                        .paymentId(9999L)
                        .bookingCode("BK-TEST-20261115")
                        .recipientEmail(fallbackTo)
                        .customerName("Nguyễn Văn An (Demo)")
                        .hotelName("SmartStay Cầu Giấy Grand")
                        .amount(BigDecimal.valueOf(3402000))
                        .paymentMethod("VNPay (Cổng thanh toán điện tử)")
                        .transactionReference("VNP2026111599999")
                        .paidAt(OffsetDateTime.now())
                        .totalBookingAmount(BigDecimal.valueOf(3402000))
                        .remainingBalance(BigDecimal.ZERO)
                        .currency("VND")
                        .services(List.of(
                                ServiceItemEmailDto.builder()
                                        .name("Set Trà chiều Thượng hạng & Bánh ngọt")
                                        .quantity(1)
                                        .unitPrice(BigDecimal.valueOf(162000))
                                        .totalPrice(BigDecimal.valueOf(162000))
                                        .build(),
                                ServiceItemEmailDto.builder()
                                        .name("Nước cam ép tươi nguyên chất")
                                        .quantity(2)
                                        .unitPrice(BigDecimal.valueOf(40000))
                                        .totalPrice(BigDecimal.valueOf(80000))
                                        .build()
                        ))
                        .surcharges(List.of(
                                ServiceItemEmailDto.builder()
                                        .name("Phụ thu nhận phòng sớm (Early Check-in)")
                                        .quantity(1)
                                        .unitPrice(BigDecimal.valueOf(270000))
                                        .totalPrice(BigDecimal.valueOf(270000))
                                        .build()
                        ))
                        .build();
                emailService.sendPaymentReceipt(event);
            } else {
                BookingConfirmationEmailEvent event = BookingConfirmationEmailEvent.builder()
                        .bookingId(9999L)
                        .bookingCode("BK-TEST-20261115")
                        .recipientEmail(fallbackTo)
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
                        .services(List.of(
                                ServiceItemEmailDto.builder()
                                        .name("Set Trà chiều Thượng hạng & Bánh ngọt")
                                        .quantity(1)
                                        .unitPrice(BigDecimal.valueOf(162000))
                                        .totalPrice(BigDecimal.valueOf(162000))
                                        .build(),
                                ServiceItemEmailDto.builder()
                                        .name("Nước cam ép tươi nguyên chất")
                                        .quantity(2)
                                        .unitPrice(BigDecimal.valueOf(40000))
                                        .totalPrice(BigDecimal.valueOf(80000))
                                        .build()
                        ))
                        .surcharges(List.of(
                                ServiceItemEmailDto.builder()
                                        .name("Phụ thu nhận phòng sớm (Early Check-in)")
                                        .quantity(1)
                                        .unitPrice(BigDecimal.valueOf(270000))
                                        .totalPrice(BigDecimal.valueOf(270000))
                                        .build()
                        ))
                        .build();
                emailService.sendBookingConfirmation(event);
            }
            resultInfo.put("status", "SENT");
            resultInfo.put("source", "MOCK_FALLBACK");
            resultInfo.put("recipient", fallbackTo);
        }

        resultInfo.put("type", type.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(resultInfo));
    }

    @PostMapping("/resend-booking-email")
    @Operation(summary = "Gửi lại cả email xác nhận đặt phòng và biên lai thanh toán theo mã booking thực tế")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendBookingEmail(
            @RequestParam(required = false) String bookingNumber,
            @RequestParam(required = false) String to
    ) {
        Booking booking = findTargetBooking(bookingNumber);
        if (booking == null) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .code(400)
                    .message("Không tìm thấy đơn đặt phòng nào")
                    .build());
        }

        String targetEmail = (to != null && !to.isBlank())
                ? to.trim()
                : (booking.getGuest() != null ? booking.getGuest().getEmail() : null);

        if (targetEmail == null || targetEmail.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .code(400)
                    .message("Đơn đặt phòng không có email khách hàng")
                    .build());
        }

        BookingConfirmationEmailEvent bookingEvent = buildBookingConfirmationEvent(booking, targetEmail);
        emailService.sendBookingConfirmation(bookingEvent);

        PaymentReceiptEmailEvent paymentEvent = buildPaymentReceiptEvent(booking, targetEmail);
        emailService.sendPaymentReceipt(paymentEvent);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("bookingCode", booking.getBookingNumber());
        resp.put("customerName", booking.getGuest() != null ? booking.getGuest().getFullName() : "N/A");
        resp.put("recipientEmail", targetEmail);
        resp.put("hotelName", booking.getHotel() != null ? booking.getHotel().getName() : "SmartStay Hotel");
        resp.put("totalAmount", booking.getTotalAmount());
        resp.put("bookingConfirmationSent", true);
        resp.put("paymentReceiptSent", true);

        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    private Booking findTargetBooking(String bookingNumber) {
        if (bookingNumber != null && !bookingNumber.isBlank()) {
            return bookingRepository.findByBookingNumber(bookingNumber.trim()).orElse(null);
        }
        var page = bookingRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")));
        if (page.hasContent()) {
            return page.getContent().get(0);
        }
        return null;
    }

    private BookingConfirmationEmailEvent buildBookingConfirmationEvent(Booking booking, String recipientEmail) {
        LocalDate checkIn = null;
        LocalDate checkOut = null;
        String roomTypeName = null;
        int totalRooms = 0;

        if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
            BookingDetail first = booking.getBookingDetails().get(0);
            checkIn = first.getCheckInDate();
            checkOut = first.getCheckOutDate();
            roomTypeName = first.getRoomTypeName();
            for (BookingDetail d : booking.getBookingDetails()) {
                totalRooms += (d.getQuantity() != null ? d.getQuantity() : 1);
            }
        }

        return BookingConfirmationEmailEvent.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingNumber())
                .recipientEmail(recipientEmail)
                .customerName(booking.getGuest() != null ? booking.getGuest().getFullName() : "Quý khách")
                .hotelName(booking.getHotel() != null ? booking.getHotel().getName() : "SmartStay Hotel")
                .hotelAddress(booking.getHotel() != null ? booking.getHotel().getAddress() : "")
                .hotelPhone(booking.getHotel() != null ? booking.getHotel().getPhone() : "")
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .roomTypeName(roomTypeName != null ? roomTypeName : "Phòng tiêu chuẩn")
                .totalRooms(totalRooms > 0 ? totalRooms : 1)
                .totalGuests(1)
                .subtotalAmount(booking.getSubtotalAmount() != null ? booking.getSubtotalAmount() : booking.getTotalAmount())
                .serviceFeeAmount(booking.getServiceFeeAmount() != null ? booking.getServiceFeeAmount() : BigDecimal.ZERO)
                .vatAmount(booking.getTotalVatAmount() != null ? booking.getTotalVatAmount() : BigDecimal.ZERO)
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO)
                .currency("VND")
                .portalUrl("http://localhost:3000/my-bookings")
                .services(extractEmailServices(booking.getId()))
                .surcharges(extractEmailSurcharges(booking))
                .build();
    }

    private PaymentReceiptEmailEvent buildPaymentReceiptEvent(Booking booking, String recipientEmail) {
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        Payment successfulPayment = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst()
                .orElse(payments.isEmpty() ? null : payments.get(payments.size() - 1));

        BigDecimal paidAmount = successfulPayment != null ? successfulPayment.getTotalAmount() : booking.getTotalAmount();
        String paymentMethod = (successfulPayment != null && successfulPayment.getPaymentMethod() != null)
                ? successfulPayment.getPaymentMethod().name()
                : "VNPAY";
        String txnRef = successfulPayment != null ? successfulPayment.getTransactionReference() : "";
        OffsetDateTime paidAt = (successfulPayment != null && successfulPayment.getPaidAt() != null)
                ? successfulPayment.getPaidAt()
                : OffsetDateTime.now();

        return PaymentReceiptEmailEvent.builder()
                .paymentId(successfulPayment != null ? successfulPayment.getId() : 0L)
                .bookingCode(booking.getBookingNumber())
                .recipientEmail(recipientEmail)
                .customerName(booking.getGuest() != null ? booking.getGuest().getFullName() : "Quý khách")
                .hotelName(booking.getHotel() != null ? booking.getHotel().getName() : "SmartStay Hotel")
                .amount(paidAmount)
                .paymentMethod(paymentMethod)
                .transactionReference(txnRef)
                .paidAt(paidAt)
                .totalBookingAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : paidAmount)
                .remainingBalance(BigDecimal.ZERO)
                .currency("VND")
                .services(extractEmailServices(booking.getId()))
                .surcharges(extractEmailSurcharges(booking))
                .build();
    }

    private List<ServiceItemEmailDto> extractEmailServices(Long bookingId) {
        List<ServiceItemEmailDto> emailServices = new ArrayList<>();
        if (bookingId == null || serviceOrderService == null) {
            return emailServices;
        }
        try {
            List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(bookingId);
            if (orders != null) {
                for (ServiceOrderResponse o : orders) {
                    if (o.getStatus() != ServiceOrderStatus.CANCELLED && o.getDetails() != null) {
                        for (var d : o.getDetails()) {
                            emailServices.add(ServiceItemEmailDto.builder()
                                    .name(d.getItemName())
                                    .quantity(d.getQuantity() != null ? d.getQuantity() : 1)
                                    .unitPrice(d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO)
                                    .totalPrice(d.getTotalAmount() != null ? d.getTotalAmount() : BigDecimal.ZERO)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Lỗi trích xuất dịch vụ gửi email: {}", ex.getMessage());
        }
        return emailServices;
    }

    private List<ServiceItemEmailDto> extractEmailSurcharges(Booking booking) {
        List<ServiceItemEmailDto> emailSurcharges = new ArrayList<>();
        if (booking == null || booking.getBookingDetails() == null) {
            return emailSurcharges;
        }
        try {
            for (BookingDetail bd : booking.getBookingDetails()) {
                if (bd.getBookingRooms() != null) {
                    for (BookingRoom br : bd.getBookingRooms()) {
                        List<BookingCharge> charges = (br.getId() != null && bookingChargeRepository != null)
                                ? bookingChargeRepository.findByBookingRoomId(br.getId())
                                : (br.getCharges() != null ? br.getCharges() : List.of());
                        for (BookingCharge c : charges) {
                            if (c.getTotalAmount() != null && c.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                                emailSurcharges.add(ServiceItemEmailDto.builder()
                                        .name(c.getItemName() != null ? c.getItemName() : c.getChargeType().name())
                                        .quantity(c.getQuantity() != null ? c.getQuantity() : 1)
                                        .unitPrice(c.getUnitPrice() != null ? c.getUnitPrice() : BigDecimal.ZERO)
                                        .totalPrice(c.getTotalAmount())
                                        .build());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Lỗi trích xuất phụ thu gửi email: {}", ex.getMessage());
        }
        return emailSurcharges;
    }
}
