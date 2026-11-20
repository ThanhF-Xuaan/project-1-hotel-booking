package vn.edu.utc.hotel_booking.modules.notification.service.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import vn.edu.utc.hotel_booking.modules.notification.event.BookingConfirmationEmailEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    private JavaMailSender mailSender;
    private SpringTemplateEngine templateEngine;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        emailService = new EmailServiceImpl(mailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "fromAddress", "booking@smartstay.space");
        ReflectionTestUtils.setField(emailService, "fromName", "SmartStay Hotel Booking");
        ReflectionTestUtils.setField(emailService, "portalUrlConfig", "https://smartstay.space/my-bookings");
    }

    @Test
    @DisplayName("Render email All-in-One chứa cả thông tin phòng và chi tiết thanh toán VNPAY")
    void shouldRenderAllInOneEmailWithPaymentReceipt() throws Exception {
        BookingConfirmationEmailEvent event = BookingConfirmationEmailEvent.builder()
                .bookingId(15L)
                .bookingCode("BK1795156347565907")
                .recipientEmail("guest@test.com")
                .customerName("Phạm Thị D")
                .hotelName("SmartStay Hà Nội Grand")
                .hotelAddress("12 Lý Thường Kiệt, Q. Hoàn Kiếm, Hà Nội")
                .hotelPhone("02439123456")
                .checkInDate(LocalDate.of(2026, 11, 17))
                .checkOutDate(LocalDate.of(2026, 11, 19))
                .roomTypeName("Standard Room")
                .totalRooms(1)
                .totalGuests(1)
                .subtotalAmount(new BigDecimal("2500000.00"))
                .serviceFeeAmount(new BigDecimal("120000.00"))
                .vatAmount(new BigDecimal("201600.00"))
                .totalAmount(new BigDecimal("2821600.00"))
                .isPaid(true)
                .paymentMethod("VNPAY")
                .transactionReference("15738002")
                .paidAt(OffsetDateTime.now())
                .portalUrl("https://smartstay.space/my-bookings")
                .build();

        emailService.sendBookingConfirmation(event);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        MimeMessage sentMsg = captor.getValue();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        sentMsg.writeTo(out);
        String content = out.toString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(content).contains("BK1795156347565907");
        assertThat(content).contains("Standard Room");
        assertThat(content).contains("(PAID)");
        assertThat(content).contains("VNPAY");
        assertThat(content).contains("15738002");
        assertThat(content).contains("https://smartstay.space/my-bookings");
    }

    @Test
    @DisplayName("Kiểm tra sendBookingConfirmation đính kèm đầy đủ 4 MIME Headers quan trọng")
    void shouldSetHighPriorityHeadersForBookingConfirmation() throws Exception {
        BookingConfirmationEmailEvent event = BookingConfirmationEmailEvent.builder()
                .bookingCode("BK1795156347565907")
                .recipientEmail("guest@test.com")
                .customerName("Phạm Thị D")
                .hotelName("SmartStay Hà Nội Grand")
                .roomTypeName("Standard Room")
                .totalAmount(new BigDecimal("2821600.00"))
                .build();

        emailService.sendBookingConfirmation(event);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        MimeMessage msg = captor.getValue();

        assertThat(msg.getHeader("X-Priority", null)).isEqualTo("1");
        assertThat(msg.getHeader("Importance", null)).isEqualTo("high");
        assertThat(msg.getHeader("X-MSMail-Priority", null)).isEqualTo("High");
        assertThat(msg.getHeader("Priority", null)).isEqualTo("urgent");
    }

    @Test
    @DisplayName("Kiểm tra sendPaymentReceipt đính kèm đầy đủ 4 MIME Headers quan trọng")
    void shouldSetHighPriorityHeadersForPaymentReceipt() throws Exception {
        vn.edu.utc.hotel_booking.modules.notification.event.PaymentReceiptEmailEvent event =
                vn.edu.utc.hotel_booking.modules.notification.event.PaymentReceiptEmailEvent.builder()
                        .bookingCode("BK1795156347565907")
                        .recipientEmail("guest@test.com")
                        .customerName("Phạm Thị D")
                        .hotelName("SmartStay Hà Nội Grand")
                        .amount(new BigDecimal("2821600.00"))
                        .paymentMethod("VNPAY")
                        .build();

        emailService.sendPaymentReceipt(event);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        MimeMessage msg = captor.getValue();

        assertThat(msg.getHeader("X-Priority", null)).isEqualTo("1");
        assertThat(msg.getHeader("Importance", null)).isEqualTo("high");
        assertThat(msg.getHeader("X-MSMail-Priority", null)).isEqualTo("High");
        assertThat(msg.getHeader("Priority", null)).isEqualTo("urgent");
    }
}

