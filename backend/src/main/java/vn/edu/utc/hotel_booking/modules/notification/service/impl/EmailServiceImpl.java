package vn.edu.utc.hotel_booking.modules.notification.service.impl;

import jakarta.mail.MessagingException;
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

    @Value("${app.mail.portal-url:http://localhost:3000/my-bookings}")
    String portalUrlConfig;

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
            context.setVariable("isPaid", Boolean.TRUE.equals(event.getIsPaid()));
            context.setVariable("paymentMethod", safe(event.getPaymentMethod(), null));
            context.setVariable("transactionRef", safe(event.getTransactionReference(), null));
            context.setVariable("paidAtFormatted", event.getPaidAt() != null ? event.getPaidAt().format(DATE_TIME_FMT) : null);
            context.setVariable("invoiceNumber", safe(event.getInvoiceNumber(), null));

            String targetPortalUrl = (event.getPortalUrl() != null && !event.getPortalUrl().isBlank())
                    ? event.getPortalUrl().trim()
                    : (portalUrlConfig != null && !portalUrlConfig.isBlank() ? portalUrlConfig : "http://localhost:3000/my-bookings");
            context.setVariable("portalUrl", targetPortalUrl);

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
            applyHighPriorityHeaders(message, helper);

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

            String rem = "0 đ (Đã hoàn tất)";
            if (event.getRemainingBalance() != null && event.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                rem = formatMoney(event.getRemainingBalance()) + " (Còn phải thanh toán tại quầy)";
            }
            context.setVariable("remainingBalanceFormatted", rem);

            // Thông tin hóa đơn GTGT / Folio chi tiết
            context.setVariable("invoiceNumber", event.getInvoiceNumber());
            context.setVariable("roomNumber", event.getRoomNumber());
            String invoiceDateStr = event.getInvoiceDate() != null
                    ? event.getInvoiceDate().format(DATE_FMT)
                    : (event.getPaidAt() != null ? event.getPaidAt().format(DATE_FMT) : "");
            context.setVariable("invoiceDateFormatted", invoiceDateStr);

            context.setVariable("subtotalFormatted", formatMoney(event.getSubtotalAmount()));
            String serviceFeeRateStr = (event.getServiceFeeRate() != null && event.getServiceFeeRate().compareTo(BigDecimal.ZERO) > 0)
                    ? event.getServiceFeeRate().stripTrailingZeros().toPlainString() + "%"
                    : "5%";
            context.setVariable("serviceFeeRate", serviceFeeRateStr);
            context.setVariable("serviceFeeFormatted", formatMoney(event.getServiceFeeAmount()));
            context.setVariable("vatAmountFormatted", formatMoney(event.getVatAmount()));
            context.setVariable("grandTotalFormatted", formatMoney(event.getGrandTotal() != null ? event.getGrandTotal() : event.getAmount()));

            if (event.getInvoiceDetails() != null && !event.getInvoiceDetails().isEmpty()) {
                for (var item : event.getInvoiceDetails()) {
                    if (item.getUnitPriceFormatted() == null && item.getUnitPrice() != null) {
                        item.setUnitPriceFormatted(formatMoney(item.getUnitPrice()));
                    }
                    if (item.getSubtotalFormatted() == null && item.getSubtotal() != null) {
                        item.setSubtotalFormatted(formatMoney(item.getSubtotal()));
                    }
                    if (item.getVatRateFormatted() == null) {
                        String rate = item.getVatRate() != null ? item.getVatRate().stripTrailingZeros().toPlainString() : "8";
                        item.setVatRateFormatted(rate + "%");
                    }
                    if (item.getVatAmountFormatted() == null && item.getVatAmount() != null) {
                        item.setVatAmountFormatted(formatMoney(item.getVatAmount()));
                    }
                }
                context.setVariable("invoiceDetails", event.getInvoiceDetails());
            }

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
            helper.setSubject("Hóa đơn GTGT & Biên lai thanh toán - Đơn " + event.getBookingCode());
            helper.setText(htmlBody, true);
            applyHighPriorityHeaders(message, helper);

            mailSender.send(message);
            log.info("[EMAIL] Sent payment receipt for booking {} to {}", event.getBookingCode(), event.getRecipientEmail());
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send payment receipt for booking {}: {}", event.getBookingCode(), e.getMessage(), e);
        }
    }

    private void applyHighPriorityHeaders(MimeMessage message, MimeMessageHelper helper) throws MessagingException {
        helper.setPriority(1); // Thiết lập Header X-Priority: 1
        message.setHeader("Importance", "high");
        message.setHeader("X-MSMail-Priority", "High");
        message.setHeader("Priority", "urgent");
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " đ";
    }

    private String safe(String str, String defaultVal) {
        return (str != null && !str.isBlank()) ? str : defaultVal;
    }
}
