package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.FolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentPurpose;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.FolioServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderDetailResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentMethod;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentStatus;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolioServiceTest {

    @Mock
    private BookingLookupService bookingLookupService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ServiceOrderService serviceOrderService;

    @InjectMocks
    private FolioServiceImpl folioService;

    private Booking testBooking;
    private BookingRoom testRoom;

    @BeforeEach
    void setUp() {
        Hotel hotel = Hotel.builder()
                .id((short) 1)
                .name("Grand Hotel Hanoi")
                .build();

        RoomInstance roomInstance = RoomInstance.builder()
                .id(101)
                .roomNumber("P101")
                .build();

        BookingDailyRate dailyRate = BookingDailyRate.builder()
                .id(1L)
                .stayDate(LocalDate.of(2026, 10, 24))
                .netPrice(new BigDecimal("1000000.00"))
                .vatPercent(new BigDecimal("10.00"))
                .build();

        testRoom = BookingRoom.builder()
                .id(11L)
                .roomInstance(roomInstance)
                .adultCount((short) 2)
                .childCount((short) 0)
                .status(BookingRoomStatus.CHECKED_IN)
                .dailyRates(List.of(dailyRate))
                .charges(List.of())
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(101L)
                .roomTypeName("Deluxe Ocean")
                .checkInDate(LocalDate.of(2026, 10, 24))
                .checkOutDate(LocalDate.of(2026, 10, 25))
                .bookingRooms(List.of(testRoom))
                .build();

        vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest bookingGuest = vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .email("vana@example.com")
                .build();

        testBooking = Booking.builder()
                .id(100L)
                .bookingNumber("BK-20261024-001")
                .hotel(hotel)
                .bookingGuest(bookingGuest)
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(new BigDecimal("5.00"))
                .bookingDetails(List.of(detail))
                .build();
    }

    @Test
    @DisplayName("Lấy Folio tổng hợp thành công và tính đúng Debits, Credits, Balance")
    void getFolioByBookingId_Success() {
        // Given
        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(testBooking);
        when(serviceOrderService.getOrdersByBookingId(100L)).thenReturn(List.of());

        // Tiền phòng: netPrice 1,000,000 + 5% service fee (50,000) = 1,050,000 taxable -> 10% VAT (105,000) -> Total = 1,155,000
        PaymentResponse depositPayment = PaymentResponse.builder()
                .id(1L)
                .bookingId(100L)
                .bookingNumber("BK-20261024-001")
                .totalAmount(new BigDecimal("1155000.00"))
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentPurpose(PaymentPurpose.DEPOSIT)
                .status(PaymentStatus.SUCCESS)
                .transactionReference("TXN-DEP-001")
                .paidAt(OffsetDateTime.now())
                .build();

        when(paymentService.getByBookingId(100L)).thenReturn(List.of(depositPayment));

        // When
        FolioResponse response = folioService.getFolioByBookingId(100L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBookingNumber()).isEqualTo("BK-20261024-001");
        assertThat(response.getHotelName()).isEqualTo("Grand Hotel Hanoi");
        assertThat(response.getDebits()).hasSize(1);
        assertThat(response.getDebits().get(0).getLineType()).isEqualTo("ROOM_RATE");
        assertThat(response.getDebits().get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("1155000.00"));

        assertThat(response.getCredits()).hasSize(1);
        assertThat(response.getCredits().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1155000.00"));

        assertThat(response.getTotalDebits()).isEqualByComparingTo(new BigDecimal("1155000.00"));
        assertThat(response.getTotalCredits()).isEqualByComparingTo(new BigDecimal("1155000.00"));
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCanCheckOut()).isTrue();
        assertThat(response.getHasOverpayment()).isFalse();
    }

    @Test
    @DisplayName("Folio tự động gom đơn dịch vụ POS POST_TO_ROOM vào Debits")
    void getFolioByBookingId_WithPosServiceOrder() {
        // Given
        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(testBooking);

        ServiceOrderDetailResponse orderItem = ServiceOrderDetailResponse.builder()
                .id(201L)
                .itemType("PRODUCT")
                .itemName("Lavie Nước khoáng")
                .quantity(2)
                .unitPrice(new BigDecimal("20000.00"))
                .subtotal(new BigDecimal("40000.00"))
                .serviceFeeRate(new BigDecimal("5.00"))
                .serviceFeeAmount(new BigDecimal("2000.00"))
                .vatRate(new BigDecimal("10.00"))
                .vatAmount(new BigDecimal("4200.00"))
                .totalAmount(new BigDecimal("46200.00"))
                .build();

        ServiceOrderResponse posOrder = ServiceOrderResponse.builder()
                .id(501L)
                .issuedAt(OffsetDateTime.now())
                .paymentMethod(ServiceOrderPaymentMethod.POST_TO_ROOM)
                .paymentStatus(ServiceOrderPaymentStatus.UNPAID)
                .roomNumber("P101")
                .details(List.of(orderItem))
                .build();

        when(serviceOrderService.getOrdersByBookingId(100L)).thenReturn(List.of(posOrder));
        when(paymentService.getByBookingId(100L)).thenReturn(List.of());

        // When
        FolioResponse response = folioService.getFolioByBookingId(100L);

        // Then
        assertThat(response.getDebits()).hasSize(2); // 1 room rate + 1 POS product
        BigDecimal expectedTotal = new BigDecimal("1155000.00").add(new BigDecimal("46200.00"));
        assertThat(response.getTotalDebits()).isEqualByComparingTo(expectedTotal);
        assertThat(response.getBalance()).isEqualByComparingTo(expectedTotal);
        assertThat(response.getCanCheckOut()).isFalse(); // còn nợ, chưa được check-out
    }

    @Test
    @DisplayName("Lấy Folio cho phòng đơn lẻ (BookingRoomId) lọc đúng chi phí phòng đó")
    void getFolioByBookingRoomId_Success() {
        // Given
        when(bookingLookupService.getBookingByRoomId(11L)).thenReturn(testBooking);
        when(paymentService.getByBookingId(100L)).thenReturn(List.of());

        // When
        FolioResponse response = folioService.getFolioByBookingRoomId(11L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRooms()).hasSize(1);
        assertThat(response.getRooms().get(0).getBookingRoomId()).isEqualTo(11L);
        assertThat(response.getDebits()).hasSize(1);
    }

    @Test
    @DisplayName("Phát hiện thừa cọc (Overpayment) khi TotalCredits > TotalDebits")
    void getFolioByBookingId_Overpayment() {
        // Given
        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(testBooking);
        when(serviceOrderService.getOrdersByBookingId(100L)).thenReturn(List.of());

        PaymentResponse overPayment = PaymentResponse.builder()
                .id(2L)
                .bookingId(100L)
                .bookingNumber("BK-20261024-001")
                .totalAmount(new BigDecimal("2000000.00"))
                .paymentMethod(PaymentMethod.CASH)
                .paymentPurpose(PaymentPurpose.DEPOSIT)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getByBookingId(100L)).thenReturn(List.of(overPayment));

        // When
        FolioResponse response = folioService.getFolioByBookingId(100L);

        // Then
        // Total Debits = 1,155,000, Total Credits = 2,000,000 -> Balance = -845,000
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("-845000.00"));
        assertThat(response.getHasOverpayment()).isTrue();
        assertThat(response.getOverpaymentAmount()).isEqualByComparingTo(new BigDecimal("845000.00"));
        assertThat(response.getCanCheckOut()).isFalse();
    }
}
