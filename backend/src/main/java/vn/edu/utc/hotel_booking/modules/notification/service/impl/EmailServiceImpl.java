package vn.edu.utc.hotel_booking.modules.notification.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.edu.utc.hotel_booking.modules.notification.event.BookingConfirmationEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.event.PaymentReceiptEmailEvent;
import vn.edu.utc.hotel_booking.modules.notification.service.EmailService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailServiceImpl implements EmailService {

    final JavaMailSender mailSender;
    final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from-address:booking@smartstay.utc.edu.vn}")
    String fromAddress;

    @Value("${app.mail.from-name:SmartStay Hotel Booking}")
    String fromName;

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void sendBookingConfirmation(BookingConfirmationEmailEvent event) {
        if (event == null || event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()) {
            log.warn("[EMAIL] Skip booking confirmation email: empty recipient");
            return;
        }

        try {
            Context context = new Context(new Locale("vi", "VN"));
            context.setVariable("customerName", safe(event.getCustomerName(), "Quý khách"));
            context.setVariable("bookingCode", safe(event.getBookingCode(), "N/A"));
            context.setVariable("hotelName", safe(event.getHotelName(), "Khách sạn SmartStay"));
            context.setVariable("hotelAddress", event.getHotelAddress());
            context.setVariable("hotelPhone", event.getHotelPhone());
            context.setVariable("roomTypeName", safe(event.getRoomTypeName(), "Phòng tiêu chuẩn"));
            context.setVariable("totalRooms", event.getTotalRooms() != null ? event.getTotalRooms() : 1);
            context.setVariable("totalGuests", event.getTotalGuests() != null ? event.getTotalGuests() : 1);
            context.setVariable("checkInDate", event.getCheckInDate() != null ? event.getCheckInDate().format(DATE_FMT) + " (Từ 14:00)" : "N/A");
            context.setVariable("checkOutDate", event.getCheckOutDate() != null ? event.getCheckOutDate().format(DATE_FMT) + " (Trước 12:00)" : "N/A");
            context.setVariable("subtotalFormatted", formatMoney(event.getSubtotalAmount()));
            context.setVariable("serviceFeeFormatted", event.getServiceFeeAmount() != null && event.getServiceFeeAmount().compareTo(BigDecimal.ZERO) > 0
                    ? formatMoney(event.getServiceFeeAmount()) : null);
            context.setVariable("vatFormatted", event.getVatAmount() != null && event.getVatAmount().compareTo(BigDecimal.ZERO) > 0
                    ? formatMoney(event.getVatAmount()) : null);
            context.setVariable("totalFormatted", formatMoney(event.getTotalAmount()));
            context.setVariable("portalUrl", safe(event.getPortalUrl(), "http://localhost:3000/my-bookings"));

            BigDecimal totalServices = BigDecimal.ZERO;
            if (event.getServices() != null) {
                for (var s : event.getServices()) {
                    if (s.getUnitPriceFormatted() == null && s.getUnitPrice() != null) {
                        s.setUnitPriceFormatted(formatMoney(s.getUnitPrice()));
                    }
                    if (s.getTotalPriceFormatted() == null && s.getTotalPrice() != null) {
                        s.setTotalPriceFormatted(formatMoney(s.getTotalPrice()));
                    }
                    if (s.getTotalPrice() != null) {
                        totalServices = totalServices.add(s.getTotalPrice());
                    }
                }
                context.setVariable("services", event.getServices());
            }
            if (totalServices.compareTo(BigDecimal.ZERO) > 0) {
                context.setVariable("totalServicesFormatted", formatMoney(totalServices));
            }

            BigDecimal totalSurcharges = BigDecimal.ZERO;
            if (event.getSurcharges() != null) {
                for (var s : event.getSurcharges()) {
                    if (s.getUnitPriceFormatted() == null && s.getUnitPrice() != null) {
                        s.setUnitPriceFormatted(formatMoney(s.getUnitPrice()));
                    }
                    if (s.getTotalPriceFormatted() == null && s.getTotalPrice() != null) {
                        s.setTotalPriceFormatted(formatMoney(s.getTotalPrice()));
                    }
                    if (s.getTotalPrice() != null) {
                        totalSurcharges = totalSurcharges.add(s.getTotalPrice());
                    }
                }
                context.setVariable("surcharges", event.getSurcharges());
            }
            if (totalSurcharges.compareTo(BigDecimal.ZERO) > 0) {
                context.setVariable("totalSurchargesFormatted", formatMoney(totalSurcharges));
            }

            String htmlBody = templateEngine.process("mail/booking-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress, fromName);
            helper.setTo(event.getRecipientEmail().trim());
            helper.setSubject("Xác nhận đặt phòng thành công - Mã đơn: " + event.getBookingCode());
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[EMAIL] Sent booking confirmation for code {} to {}", event.getBookingCode(), event.getRecipientEmail());
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send booking confirmation for {}: {}", event.getBookingCode(), e.getMessage(), e);
        }
    }

    @Override
    public void sendPaymentReceipt(PaymentReceiptEmailEvent event) {
        if (event == null || event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()) {
            log.warn("[EMAIL] Skip payment receipt email: empty recipient");
            return;
        }

        try {
            Context context = new Context(new Locale("vi", "VN"));
            context.setVariable("customerName", safe(event.getCustomerName(), "Quý khách"));
            context.setVariable("bookingCode", safe(event.getBookingCode(), "N/A"));
            context.setVariable("hotelName", safe(event.getHotelName(), "Khách sạn SmartStay"));
            context.setVariable("amountFormatted", formatMoney(event.getAmount()));
            context.setVariable("paymentMethod", safe(event.getPaymentMethod(), "Cổng thanh toán"));
            context.setVariable("transactionRef", event.getTransactionReference());
            context.setVariable("paidAtFormatted", event.getPaidAt() != null ? event.getPaidAt().format(DATE_TIME_FMT) : "N/A");
            context.setVariable("totalBookingFormatted", formatMoney(event.getTotalBookingAmount()));

            String rem = "0 VND (Đã hoàn tất)";
            if (event.getRemainingBalance() != null && event.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                rem = formatMoney(event.getRemainingBalance()) + " (Còn phải thanh toán tại quầy)";
            }
            context.setVariable("remainingBalanceFormatted", rem);

            BigDecimal totalServices = BigDecimal.ZERO;
            if (event.getServices() != null) {
                for (var s : event.getServices()) {
                    if (s.getUnitPriceFormatted() == null && s.getUnitPrice() != null) {
                        s.setUnitPriceFormatted(formatMoney(s.getUnitPrice()));
                    }
                    if (s.getTotalPriceFormatted() == null && s.getTotalPrice() != null) {
                        s.setTotalPriceFormatted(formatMoney(s.getTotalPrice()));
                    }
                    if (s.getTotalPrice() != null) {
                        totalServices = totalServices.add(s.getTotalPrice());
                    }
                }
                context.setVariable("services", event.getServices());
            }
            if (totalServices.compareTo(BigDecimal.ZERO) > 0) {
                context.setVariable("totalServicesFormatted", formatMoney(totalServices));
            }

            BigDecimal totalSurcharges = BigDecimal.ZERO;
            if (event.getSurcharges() != null) {
                for (var s : event.getSurcharges()) {
                    if (s.getUnitPriceFormatted() == null && s.getUnitPrice() != null) {
                        s.setUnitPriceFormatted(formatMoney(s.getUnitPrice()));
                    }
                    if (s.getTotalPriceFormatted() == null && s.getTotalPrice() != null) {
                        s.setTotalPriceFormatted(formatMoney(s.getTotalPrice()));
                    }
                    if (s.getTotalPrice() != null) {
                        totalSurcharges = totalSurcharges.add(s.getTotalPrice());
                    }
                }
                context.setVariable("surcharges", event.getSurcharges());
            }
            if (totalSurcharges.compareTo(BigDecimal.ZERO) > 0) {
                context.setVariable("totalSurchargesFormatted", formatMoney(totalSurcharges));
            }

            String htmlBody = templateEngine.process("mail/payment-receipt", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress, fromName);
            helper.setTo(event.getRecipientEmail().trim());
            helper.setSubject("Biên lai xác nhận thanh toán - Đơn " + event.getBookingCode());
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[EMAIL] Sent payment receipt for booking {} to {}", event.getBookingCode(), event.getRecipientEmail());
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send payment receipt for booking {}: {}", event.getBookingCode(), e.getMessage(), e);
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 VND";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " VND";
    }

    private String safe(String str, String defaultVal) {
        return (str != null && !str.isBlank()) ? str : defaultVal;
    }
}
