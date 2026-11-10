package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.InvoiceResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.Invoice;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceDetail;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceLineType;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceStatus;
import vn.edu.utc.hotel_booking.modules.finance.entity.Payment;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.InvoiceRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.InvoiceServiceImpl;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderDetailResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingLookupService bookingLookupService;

    @Mock
    private ServiceOrderService serviceOrderService;

    @Mock
    private FinanceMapper financeMapper;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        BookingDailyRate dailyRate = BookingDailyRate.builder()
                .id(1L)
                .stayDate(LocalDate.of(2026, 12, 31))
                .netPrice(new BigDecimal("1000000.00"))
                .vatPercent(new BigDecimal("10.00"))
                .build();

        BookingCharge penaltyCharge = BookingCharge.builder()
                .id(10L)
                .chargeType(BookingChargeType.PENALTY)
                .itemName("Làm vỡ đèn ngủ")
                .quantity(1)
                .unitPrice(new BigDecimal("200000.00"))
                .subtotal(new BigDecimal("200000.00"))
                .serviceFeeRate(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatRate(new BigDecimal("10.00"))
                .vatAmount(new BigDecimal("20000.00"))
                .totalAmount(new BigDecimal("220000.00"))
                .build();

        BookingCharge lateCheckoutCharge = BookingCharge.builder()
                .id(11L)
                .chargeType(BookingChargeType.LATE_CHECKOUT)
                .itemName("Phụ thu trả phòng muộn")
                .quantity(1)
                .unitPrice(new BigDecimal("300000.00"))
                .subtotal(new BigDecimal("300000.00"))
                .serviceFeeRate(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatRate(new BigDecimal("10.00"))
                .vatAmount(new BigDecimal("30000.00"))
                .totalAmount(new BigDecimal("330000.00"))
                .build();

        BookingRoom room = BookingRoom.builder()
                .id(200L)
                .dailyRates(List.of(dailyRate))
                .charges(List.of(penaltyCharge, lateCheckoutCharge))
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(100L)
                .roomTypeName("Deluxe Room")
                .bookingRooms(List.of(room))
                .build();

        testBooking = Booking.builder()
                .id(500L)
                .bookingNumber("BK-20261010-001")
                .serviceFeeRate(new BigDecimal("5.00"))
                .bookingDetails(List.of(detail))
                .build();
    }

    @Test
    @DisplayName("Tạo hóa đơn thành công thông qua Service-to-Service: bóc tách đủ ROOM_RATE, SURCHARGE, PENALTY, PRODUCT, SERVICE")
    void shouldCreateInvoiceWithAllDistinctLineTypes() {
        // Arrange
        InvoiceCreateRequest request = InvoiceCreateRequest.builder()
                .bookingId(500L)
                .serviceFeeRate(new BigDecimal("5.00"))
                .build();

        when(bookingLookupService.getBookingEntityById(500L)).thenReturn(testBooking);

        // POS & Dịch vụ qua ServiceOrderService
        ServiceOrderDetailResponse productItem = ServiceOrderDetailResponse.builder()
                .id(101L)
                .itemType("PRODUCT")
                .itemName("Bia Tiger lon")
                .quantity(2)
                .unitPrice(new BigDecimal("35000.00"))
                .subtotal(new BigDecimal("70000.00"))
                .serviceFeeRate(new BigDecimal("5.00"))
                .serviceFeeAmount(new BigDecimal("3500.00"))
                .vatRate(new BigDecimal("10.00"))
                .vatAmount(new BigDecimal("7350.00"))
                .totalAmount(new BigDecimal("80850.00"))
                .build();

        ServiceOrderDetailResponse serviceItem = ServiceOrderDetailResponse.builder()
                .id(102L)
                .itemType("SERVICE")
                .itemName("Dịch vụ Giặt là")
                .quantity(1)
                .unitPrice(new BigDecimal("150000.00"))
                .subtotal(new BigDecimal("150000.00"))
                .serviceFeeRate(new BigDecimal("5.00"))
                .serviceFeeAmount(new BigDecimal("7500.00"))
                .vatRate(new BigDecimal("10.00"))
                .vatAmount(new BigDecimal("15750.00"))
                .totalAmount(new BigDecimal("173250.00"))
                .build();

        ServiceOrderResponse orderResponse = ServiceOrderResponse.builder()
                .id(1L)
                .orderNumber("SO-001")
                .details(List.of(productItem, serviceItem))
                .build();

        when(serviceOrderService.getOrdersByBookingId(500L)).thenReturn(List.of(orderResponse));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(financeMapper.toResponse(any(Invoice.class))).thenReturn(InvoiceResponse.builder().id(1L).invoiceNumber("INV-001").build());

        // Act
        InvoiceResponse response = invoiceService.createInvoice(request);

        // Assert
        assertThat(response).isNotNull();

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice savedInvoice = invoiceCaptor.getValue();

        List<InvoiceDetail> details = savedInvoice.getDetails();
        assertThat(details).hasSize(5); // 1 ROOM_RATE + 1 PENALTY + 1 SURCHARGE + 1 PRODUCT + 1 SERVICE

        List<InvoiceLineType> lineTypes = details.stream().map(InvoiceDetail::getLineType).toList();
        assertThat(lineTypes).containsExactlyInAnyOrder(
                InvoiceLineType.ROOM_RATE,
                InvoiceLineType.PENALTY,
                InvoiceLineType.SURCHARGE,
                InvoiceLineType.PRODUCT,
                InvoiceLineType.SERVICE
        );

        // Verify service-to-service communication was used exclusively
        verify(bookingLookupService, times(1)).getBookingEntityById(500L);
        verify(serviceOrderService, times(1)).getOrdersByBookingId(500L);
        verifyNoMoreInteractions(bookingLookupService, serviceOrderService);
    }

    @Test
    @DisplayName("Chống thu tiền 2 lần: Bỏ qua các đơn dịch vụ đã thanh toán ngay tại POS (PAID hoặc CASH)")
    void shouldIgnoreServiceOrdersThatAreAlreadyPaidAtPos() {
        // Arrange
        InvoiceCreateRequest request = InvoiceCreateRequest.builder()
                .bookingId(500L)
                .serviceFeeRate(new BigDecimal("5.00"))
                .build();

        when(bookingLookupService.getBookingEntityById(500L)).thenReturn(testBooking);

        ServiceOrderDetailResponse paidItem = ServiceOrderDetailResponse.builder()
                .id(103L)
                .itemType("PRODUCT")
                .itemName("Cocktail tại quầy bar")
                .quantity(1)
                .unitPrice(new BigDecimal("120000.00"))
                .subtotal(new BigDecimal("120000.00"))
                .serviceFeeRate(new BigDecimal("5.00"))
                .serviceFeeAmount(new BigDecimal("6000.00"))
                .vatRate(new BigDecimal("10.00"))
                .vatAmount(new BigDecimal("12600.00"))
                .totalAmount(new BigDecimal("138600.00"))
                .build();

        ServiceOrderResponse paidOrder = ServiceOrderResponse.builder()
                .id(2L)
                .orderNumber("SO-PAID-002")
                .paymentMethod(vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentMethod.CASH)
                .paymentStatus(vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentStatus.PAID)
                .details(List.of(paidItem))
                .build();

        when(serviceOrderService.getOrdersByBookingId(500L)).thenReturn(List.of(paidOrder));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(financeMapper.toResponse(any(Invoice.class))).thenReturn(InvoiceResponse.builder().id(1L).invoiceNumber("INV-002").build());

        // Act
        InvoiceResponse response = invoiceService.createInvoice(request);

        // Assert
        assertThat(response).isNotNull();

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice savedInvoice = invoiceCaptor.getValue();

        List<InvoiceDetail> details = savedInvoice.getDetails();
        // Chỉ có 3 dòng (1 ROOM_RATE + 1 PENALTY + 1 SURCHARGE), hoàn toàn không có PRODUCT của đơn paidOrder
        assertThat(details).hasSize(3);
        List<InvoiceLineType> lineTypes = details.stream().map(InvoiceDetail::getLineType).toList();
        assertThat(lineTypes).doesNotContain(InvoiceLineType.PRODUCT);
    }

    @Test
    @DisplayName("Fallback chi tiết phòng khi booking chưa có dailyRates & tự đánh dấu PAID nếu đã thanh toán đủ")
    void shouldBuildRoomDetailsAndMarkPaidWhenDailyRatesEmptyAndSettled() {
        BookingRoom roomWithoutRates = BookingRoom.builder().id(301L).build();
        BookingDetail detail = BookingDetail.builder()
                .id(111L)
                .roomTypeName("Executive Suite")
                .quantity((short) 1)
                .checkInDate(LocalDate.of(2026, 10, 10))
                .checkOutDate(LocalDate.of(2026, 10, 12))
                .bookingRooms(List.of(roomWithoutRates))
                .build();

        Booking seedLikeBooking = Booking.builder()
                .id(11L)
                .bookingNumber("BK-TEST-1005")
                .subtotalAmount(new BigDecimal("4800000.00"))
                .serviceFeeRate(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .totalVatAmount(new BigDecimal("384000.00"))
                .totalAmount(new BigDecimal("5184000.00"))
                .bookingDetails(List.of(detail))
                .build();

        Payment successPay = Payment.builder()
                .id(155L)
                .booking(seedLikeBooking)
                .totalAmount(new BigDecimal("5184000.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(bookingLookupService.getBookingEntityById(11L)).thenReturn(seedLikeBooking);
        when(serviceOrderService.getOrdersByBookingId(11L)).thenReturn(List.of());
        when(paymentRepository.findByBookingId(11L)).thenReturn(List.of(successPay));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Invoice.class))).thenReturn(InvoiceResponse.builder().id(10L).build());

        invoiceService.createInvoice(InvoiceCreateRequest.builder().bookingId(11L).build());

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice saved = captor.getValue();

        assertThat(saved.getDetails()).hasSize(1);
        assertThat(saved.getDetails().get(0).getDescription()).contains("Executive Suite");
        assertThat(saved.getGrandTotal()).isEqualByComparingTo("5184000.00");
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(saved.getIssuedAt()).isNotNull();
    }

    @Test
    @DisplayName("Tự phục hồi hóa đơn bị rỗng 0 đ khi gọi lại createInvoice")
    void shouldRebuildBrokenZeroInvoiceWhenRetried() {
        BookingDetail detail = BookingDetail.builder()
                .id(112L)
                .roomTypeName("Suite")
                .quantity((short) 1)
                .build();

        Booking booking = Booking.builder()
                .id(12L)
                .bookingNumber("BK-TEST-999")
                .subtotalAmount(new BigDecimal("2600000.00"))
                .serviceFeeRate(BigDecimal.ZERO)
                .totalVatAmount(new BigDecimal("208000.00"))
                .totalAmount(new BigDecimal("2808000.00"))
                .bookingDetails(List.of(detail))
                .build();

        Invoice brokenZeroInvoice = Invoice.builder()
                .id(5L)
                .booking(booking)
                .invoiceNumber("INV-ZERO")
                .grandTotal(BigDecimal.ZERO)
                .status(InvoiceStatus.ISSUED)
                .build();

        when(bookingLookupService.getBookingEntityById(12L)).thenReturn(booking);
        when(invoiceRepository.findByBookingId(12L)).thenReturn(List.of(brokenZeroInvoice));
        when(serviceOrderService.getOrdersByBookingId(12L)).thenReturn(List.of());
        when(paymentRepository.findByBookingId(12L)).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Invoice.class))).thenReturn(InvoiceResponse.builder().id(5L).build());

        invoiceService.createInvoice(InvoiceCreateRequest.builder().bookingId(12L).build());

        assertThat(brokenZeroInvoice.getDetails()).hasSize(1);
        assertThat(brokenZeroInvoice.getGrandTotal()).isEqualByComparingTo("2808000.00");
        assertThat(brokenZeroInvoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    @DisplayName("Không tính thuế/phí 2 lần khi BookingDailyRate đã có sẵn basePrice, serviceFeeAmount, vatAmount và netPrice")
    void shouldNotDoubleTaxDailyRatesThatAlreadyIncludeFeeAndVat() {
        BookingDailyRate inclusiveRate = BookingDailyRate.builder()
                .id(77L)
                .stayDate(LocalDate.of(2026, 11, 3))
                .basePrice(new BigDecimal("2500000.00"))
                .discountAmount(BigDecimal.ZERO)
                .surchargeAmount(BigDecimal.ZERO)
                .serviceFeeRate(new BigDecimal("5.00"))
                .serviceFeeAmount(new BigDecimal("125000.00"))
                .vatPercent(new BigDecimal("8.00"))
                .vatAmount(new BigDecimal("210000.00"))
                .netPrice(new BigDecimal("2835000.00"))
                .build();

        BookingRoom room = BookingRoom.builder()
                .id(700L)
                .dailyRates(List.of(inclusiveRate))
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(701L)
                .roomTypeName("Deluxe Room")
                .bookingRooms(List.of(room))
                .build();

        Booking booking = Booking.builder()
                .id(23L)
                .bookingNumber("BKG-20261103-0001")
                .subtotalAmount(new BigDecimal("2500000.00"))
                .serviceFeeRate(new BigDecimal("5.00"))
                .serviceFeeAmount(new BigDecimal("125000.00"))
                .totalVatAmount(new BigDecimal("210000.00"))
                .totalAmount(new BigDecimal("2835000.00"))
                .bookingDetails(List.of(detail))
                .build();

        when(bookingLookupService.getBookingEntityById(23L)).thenReturn(booking);
        when(serviceOrderService.getOrdersByBookingId(23L)).thenReturn(List.of());
        when(paymentRepository.findByBookingId(23L)).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Invoice.class))).thenReturn(InvoiceResponse.builder().id(12L).build());

        invoiceService.createInvoice(InvoiceCreateRequest.builder().bookingId(23L).build());

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice saved = captor.getValue();

        assertThat(saved.getDetails()).hasSize(1);
        assertThat(saved.getDetails().get(0).getUnitPrice()).isEqualByComparingTo("2500000.00");
        assertThat(saved.getDetails().get(0).getTotalAmount()).isEqualByComparingTo("2835000.00");
        assertThat(saved.getSubTotal()).isEqualByComparingTo("2500000.00");
        assertThat(saved.getServiceFeeAmount()).isEqualByComparingTo("125000.00");
        assertThat(saved.getVatAmount()).isEqualByComparingTo("210000.00");
        assertThat(saved.getGrandTotal()).isEqualByComparingTo("2835000.00");
    }
}
