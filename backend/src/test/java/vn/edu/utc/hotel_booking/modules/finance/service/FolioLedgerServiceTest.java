package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CheckoutSettlementResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.entity.Folio;
import vn.edu.utc.hotel_booking.modules.finance.entity.FolioStatus;
import vn.edu.utc.hotel_booking.modules.finance.entity.FolioTransaction;
import vn.edu.utc.hotel_booking.modules.finance.repository.FolioRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.FolioTransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.FolioLedgerServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolioLedgerServiceTest {

    @Mock
    private FolioRepository folioRepository;

    @Mock
    private FolioTransactionRepository folioTransactionRepository;

    @Mock
    private BookingLookupService bookingLookupService;

    @InjectMocks
    private FolioLedgerServiceImpl folioLedgerService;

    private Hotel testHotel;
    private Booking testBooking;
    private BookingRoom testBookingRoom;
    private Folio testFolio;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().id((short) 1).name("Grand Hotel").build();
        testBooking = Booking.builder()
                .id(18L)
                .bookingNumber("BK1794972398109239")
                .hotel(testHotel)
                .build();

        RoomInstance roomInstance = RoomInstance.builder().roomNumber("101A").build();
        BookingDetail detail = BookingDetail.builder().id(777L).roomTypeName("Deluxe Room").build();

        testBookingRoom = BookingRoom.builder()
                .id(22L)
                .roomInstance(roomInstance)
                .bookingDetail(detail)
                .build();

        testFolio = Folio.builder()
                .id(100L)
                .booking(testBooking)
                .bookingRoom(testBookingRoom)
                .hotel(testHotel)
                .folioNumber("FOL-BK1794972398109239-101A")
                .status(FolioStatus.OPEN)
                .totalDebits(BigDecimal.ZERO)
                .totalCredits(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .transactions(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("getOrCreateFolio: Tạo mới Folio với mã tất định chuẩn FOL-{bookingNumber}-{roomNumber}")
    void getOrCreateFolio_CreatesDeterministicFolioNumber() {
        when(folioRepository.findByBookingRoomId(22L)).thenReturn(Optional.empty());
        when(bookingLookupService.getBookingEntityById(18L)).thenReturn(testBooking);
        when(bookingLookupService.getBookingRoomEntityById(22L)).thenReturn(testBookingRoom);
        when(folioRepository.save(any(Folio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Folio result = folioLedgerService.getOrCreateFolio(18L, 22L);

        assertThat(result).isNotNull();
        assertThat(result.getFolioNumber()).isEqualTo("FOL-BK1794972398109239-101A");
        assertThat(result.getStatus()).isEqualTo(FolioStatus.OPEN);
        verify(folioRepository).save(any(Folio.class));
    }

    @Test
    @DisplayName("postDebit: Ghi nhận chi phí phát sinh và cập nhật số dư Sổ cái")
    void postDebit_RecordsDebitAndUpdatesBalance() {
        when(folioRepository.findById(100L)).thenReturn(Optional.of(testFolio));
        when(folioTransactionRepository.save(any(FolioTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FolioTransaction debit = FolioTransaction.builder()
                .id(1L)
                .folio(testFolio)
                .transactionType("DEBIT")
                .category("SURCHARGE")
                .amount(new BigDecimal("204120.00"))
                .postedAt(OffsetDateTime.now())
                .build();

        when(folioTransactionRepository.findByFolioIdOrderByPostedAtAsc(100L)).thenReturn(List.of(debit));

        FolioTransaction created = folioLedgerService.postDebit(
                100L, "SURCHARGE", "BOOKING_CHARGE", 6L, "Phụ thu nhận phòng sớm",
                1, new BigDecimal("180000.00"), new BigDecimal("180000.00"),
                new BigDecimal("5.00"), new BigDecimal("9000.00"),
                new BigDecimal("8.00"), new BigDecimal("15120.00"),
                new BigDecimal("204120.00")
        );

        assertThat(created).isNotNull();
        assertThat(created.getAmount()).isEqualByComparingTo("204120.00");
        assertThat(testFolio.getTotalDebits()).isEqualByComparingTo("204120.00");
        assertThat(testFolio.getBalance()).isEqualByComparingTo("204120.00");
    }

    @Test
    @DisplayName("postCredit: Ghi nhận thanh toán và khấu trừ công nợ")
    void postCredit_RecordsCreditAndDeductsBalance() {
        when(folioRepository.findById(100L)).thenReturn(Optional.of(testFolio));
        when(folioTransactionRepository.save(any(FolioTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FolioTransaction debit = FolioTransaction.builder()
                .id(1L)
                .folio(testFolio)
                .transactionType("DEBIT")
                .category("SURCHARGE")
                .amount(new BigDecimal("204120.00"))
                .postedAt(OffsetDateTime.now())
                .build();

        FolioTransaction credit = FolioTransaction.builder()
                .id(2L)
                .folio(testFolio)
                .transactionType("CREDIT")
                .category("PAYMENT")
                .amount(new BigDecimal("204120.00"))
                .postedAt(OffsetDateTime.now())
                .paymentMethod("VNPAY")
                .build();

        when(folioTransactionRepository.findByFolioIdOrderByPostedAtAsc(100L)).thenReturn(List.of(debit, credit));

        FolioTransaction created = folioLedgerService.postCredit(
                100L, "PAYMENT", "PAYMENT", 15L, "Thanh toán VNPay",
                new BigDecimal("204120.00"), "VNPAY"
        );

        assertThat(created).isNotNull();
        assertThat(created.getAmount()).isEqualByComparingTo("204120.00");
        assertThat(testFolio.getTotalCredits()).isEqualByComparingTo("204120.00");
        assertThat(testFolio.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("getSettlementByBookingRoomId: Đọc trực tiếp từ Folio Ledger trả về đúng số dư nợ")
    void getSettlementByBookingRoomId_ReturnsAuthoritativeSettlement() {
        when(bookingLookupService.getBookingRoomEntityById(22L)).thenReturn(testBookingRoom);
        when(bookingLookupService.getBookingByRoomId(22L)).thenReturn(testBooking);
        when(folioRepository.findByBookingRoomId(22L)).thenReturn(Optional.of(testFolio));

        testFolio.setTotalDebits(new BigDecimal("1693020.00"));
        testFolio.setTotalCredits(new BigDecimal("1488900.00"));
        testFolio.setBalance(new BigDecimal("204120.00"));

        FolioTransaction roomTx = FolioTransaction.builder()
                .transactionType("DEBIT")
                .category("ROOM_RATE")
                .sourceId(1L)
                .description("Phòng 101A - Đêm 2026-11-18")
                .quantity(1)
                .amount(new BigDecimal("680400.00"))
                .build();

        FolioTransaction surchargeTx = FolioTransaction.builder()
                .transactionType("DEBIT")
                .category("SURCHARGE")
                .sourceId(6L)
                .description("Phụ thu nhận phòng sớm")
                .quantity(1)
                .amount(new BigDecimal("204120.00"))
                .build();

        FolioTransaction wine1Tx = FolioTransaction.builder()
                .transactionType("DEBIT")
                .category("PRODUCT")
                .sourceId(501L)
                .description("Rượu Vang Đỏ Đà Lạt")
                .quantity(1)
                .amount(new BigDecimal("404250.00"))
                .build();

        FolioTransaction wine2Tx = FolioTransaction.builder()
                .transactionType("DEBIT")
                .category("PRODUCT")
                .sourceId(502L)
                .description("Rượu Vang Đỏ Đà Lạt")
                .quantity(1)
                .amount(new BigDecimal("404250.00"))
                .build();

        FolioTransaction p1 = FolioTransaction.builder()
                .transactionType("CREDIT")
                .category("PAYMENT")
                .description("Thanh toán tiền phòng VNPay")
                .amount(new BigDecimal("680400.00"))
                .paymentMethod("VNPAY")
                .postedAt(OffsetDateTime.now())
                .build();

        FolioTransaction p2 = FolioTransaction.builder()
                .transactionType("CREDIT")
                .category("PAYMENT")
                .description("Thanh toán rượu 1 VNPay")
                .amount(new BigDecimal("404250.00"))
                .paymentMethod("VNPAY")
                .postedAt(OffsetDateTime.now())
                .build();

        FolioTransaction p3 = FolioTransaction.builder()
                .transactionType("CREDIT")
                .category("PAYMENT")
                .description("Thanh toán rượu 2 VNPay")
                .amount(new BigDecimal("404250.00"))
                .paymentMethod("VNPAY")
                .postedAt(OffsetDateTime.now())
                .build();

        when(folioTransactionRepository.findByFolioIdOrderByPostedAtAsc(100L))
                .thenReturn(List.of(roomTx, surchargeTx, wine1Tx, wine2Tx, p1, p2, p3));

        CheckoutSettlementResponse response = folioLedgerService.getSettlementByBookingRoomId(22L);

        assertThat(response).isNotNull();
        assertThat(response.getBookingNumber()).isEqualTo("BK1794972398109239");
        assertThat(response.getRoomNumber()).isEqualTo("101A");
        assertThat(response.getTotalRoomCharges()).isEqualByComparingTo("680400.00");
        assertThat(response.getTotalSurcharges()).isEqualByComparingTo("204120.00");
        assertThat(response.getTotalServiceCharges()).isEqualByComparingTo("808500.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("1693020.00");
        assertThat(response.getTotalPaid()).isEqualByComparingTo("1488900.00");
        assertThat(response.getBalanceDue()).isEqualByComparingTo("204120.00");
        assertThat(response.isCanCheckOut()).isFalse();
        assertThat(response.getLineItems()).hasSize(4);
        assertThat(response.getPaymentHistory()).hasSize(3);
    }

    @Test
    @DisplayName("closeFolio: Đóng toàn bộ Folio khi hoàn tất Check-out")
    void closeFolio_SetsStatusToClosed() {
        when(folioRepository.findByBookingId(18L)).thenReturn(List.of(testFolio));

        folioLedgerService.closeFolio(18L);

        assertThat(testFolio.getStatus()).isEqualTo(FolioStatus.CLOSED);
        assertThat(testFolio.getClosedAt()).isNotNull();
        verify(folioRepository).save(testFolio);
    }
}

