package vn.edu.utc.hotel_booking.modules.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CreatePaymentRequest;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.PaymentUrlResponse;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Controller & Callback Controller Routing No-Ambiguity Test")
class PaymentControllerRoutingTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentController paymentController = new PaymentController(paymentService);
        PaymentCallbackController paymentCallbackController =
                new PaymentCallbackController(paymentService, "http://localhost:3000/payment/result");

        // Đăng ký CẢ 2 Controllers vào cùng 1 MockMvc context.
        // Nếu có ambiguous mapping giữa 2 controllers, Spring MVC sẽ ném IllegalStateException ngay tại đây.
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController, paymentCallbackController).build();
    }

    @Test
    @DisplayName("POST /api/v1/payments/vnpay/create maps to PaymentController without ambiguity")
    void testCreateVnPayPayment_PaymentController_noAmbiguity() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(18L);
        request.setServiceOrderId(5L);
        request.setMethod(PaymentMethod.VNPAY);
        request.setBankCode("NCB");
        request.setAmount(new BigDecimal("404250"));

        PaymentUrlResponse mockUrlResponse = PaymentUrlResponse.builder()
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=1")
                .txnRef("VNP123456")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(paymentService.createGatewayPayment(any(CreatePaymentRequest.class), anyString()))
                .thenReturn(mockUrlResponse);

        mockMvc.perform(post("/api/v1/payments/vnpay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.paymentUrl").value("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=1"));
    }

    @Test
    @DisplayName("POST /api/v1/public/payments/vnpay/create maps to PaymentCallbackController")
    void testCreatePublicVnPayPayment_PaymentCallbackController() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(18L);
        request.setMethod(PaymentMethod.VNPAY);
        request.setAmount(new BigDecimal("500000"));

        PaymentUrlResponse mockUrlResponse = PaymentUrlResponse.builder()
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?public=1")
                .txnRef("VNP999999")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(paymentService.createGatewayPayment(any(CreatePaymentRequest.class), anyString()))
                .thenReturn(mockUrlResponse);

        mockMvc.perform(post("/api/v1/public/payments/vnpay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.paymentUrl").value("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?public=1"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay-return redirects (302) to /api/v1/public/payments/vnpay/return")
    void testLegacyReturnRedirect() throws Exception {
        mockMvc.perform(get("/api/v1/payments/vnpay-return")
                        .param("vnp_TxnRef", "VNP123456")
                        .param("vnp_ResponseCode", "00"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/v1/public/payments/vnpay/return?vnp_TxnRef=VNP123456&vnp_ResponseCode=00"));
    }
}
