package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupContractRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.GroupCheckoutRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioAdjustmentRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.GroupCheckoutResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.repository.BillingRoutingRuleRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioItemAdjustmentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.MasterFolioServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("T-SP2-08: Group Checkout & Master/Sub-Folio Settlement Unit Test")
class MasterFolioGroupCheckoutTest {

    @Mock
    private MasterFolioRepository masterFolioRepository;

    @Mock
    private BillingRoutingRuleRepository billingRoutingRuleRepository;

    @Mock
    private MasterFolioItemAdjustmentRepository masterFolioItemAdjustmentRepository;

    @Mock
    private BookingLookupService bookingLookupService;

    @Mock
    private ServiceOrderService serviceOrderService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BookingRoomRepository bookingRoomRepository;

    @Mock
    private GroupContractRepository groupContractRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RoomInstanceService roomInstanceService;

    @Mock
    private RoomSlotService roomSlotService;

    @InjectMocks
    private MasterFolioServiceImpl masterFolioService;

    private Hotel testHotel;
    private Company testCompany;
    private GroupContract testContract;
    private MasterFolio testMasterFolio;
    private Booking testBooking;
    private RoomInstance room101;
    private RoomInstance room102;
    private BookingRoom bookingRoom101;
    private BookingRoom bookingRoom102;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder()
                .id((short) 1)
                .name("Grand Hotel Saigon")
                .build();

        testCompany = Company.builder()
                .id(100L)
                .name("FPT Software Global")
                .taxCode("0101234567")
                .address("Khu CNC Hoa Lac, Ha Noi")
                .contactEmail("contact@fpt.com")
                .build();

        testContract = GroupContract.builder()
                .id(50L)
                .contractNumber("GC-202611-001")
                .groupName("Đoàn Hội nghị FPT 2026")
                .hotel(testHotel)
                .company(testCompany)
                .companyTaxCode("0101234567")
                .subtotalAmount(BigDecimal.valueOf(8000000))
                .serviceFeeAmount(BigDecimal.valueOf(400000))
                .vatAmount(BigDecimal.valueOf(840000))
                .totalContractValue(BigDecimal.valueOf(9240000))
                .depositAmount(BigDecimal.valueOf(4620000))
                .depositPaidAmount(BigDecimal.valueOf(4620000))
                .status(GroupContractStatus.IN_HOUSE)
                .build();

        testMasterFolio = MasterFolio.builder()
                .id(1L)
                .folioNumber("MF-20261106-0001")
                .contract(testContract)
                .hotel(testHotel)
                .company(testCompany)
                .buyerLegalName("FPT Software Global")
                .buyerTaxCode("0101234567")
                .subtotalAmount(BigDecimal.valueOf(8000000))
                .serviceFeeAmount(BigDecimal.valueOf(400000))
                .totalVatAmount(BigDecimal.valueOf(840000))
                .totalAmount(BigDecimal.valueOf(9240000))
                .depositCredited(BigDecimal.valueOf(4620000))
                .totalPaid(BigDecimal.valueOf(4620000))
                .balance(BigDecimal.valueOf(4620000))
                .status(MasterFolioStatus.OPEN)
                .build();

        room101 = RoomInstance.builder()
                .id(101)
                .roomNumber("101")
                .build();

        room102 = RoomInstance.builder()
                .id(102)
                .roomNumber("102")
                .build();

        BookingDailyRate rate101 = BookingDailyRate.builder()
                .id(1001L)
                .stayDate(LocalDate.now())
                .netPrice(BigDecimal.valueOf(4000000))
                .vatPercent(BigDecimal.valueOf(10.00))
                .build();

        BookingDailyRate rate102 = BookingDailyRate.builder()
                .id(1002L)
                .stayDate(LocalDate.now())
                .netPrice(BigDecimal.valueOf(4000000))
                .vatPercent(BigDecimal.valueOf(10.00))
                .build();

        bookingRoom101 = BookingRoom.builder()
                .id(201L)
                .roomInstance(room101)
                .status(BookingRoomStatus.CHECKED_IN)
                .dailyRates(List.of(rate101))
                .charges(new ArrayList<>())
                .build();

        bookingRoom102 = BookingRoom.builder()
                .id(202L)
                .roomInstance(room102)
                .status(BookingRoomStatus.CHECKED_IN)
                .dailyRates(List.of(rate102))
                .charges(new ArrayList<>())
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(301L)
                .roomTypeName("Deluxe King Ocean View")
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .bookingRooms(List.of(bookingRoom101, bookingRoom102))
                .build();

        bookingRoom101.setBookingDetail(detail);
        bookingRoom102.setBookingDetail(detail);

        testBooking = Booking.builder()
                .id(401L)
                .bookingNumber("BK-202611-001")
                .serviceFeeRate(BigDecimal.valueOf(5.00))
                .bookingGuest(BookingGuest.builder().id(1L).fullName("Nguyễn Văn A").build())
                .bookingDetails(List.of(detail))
                .build();

        detail.setBooking(testBooking);
    }

    @Test
    @DisplayName("TC_001: Happy Path - Quyết toán đoàn thành công, cấn trừ cọc 50%, phòng đổi sang DIRTY")
    void happyPath_whenAllSubFoliosSettled_thenCheckoutSuccessAndRoomsDirty() {
        // GIVEN
        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));
        when(billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(1L)).thenReturn(List.of(
                BillingRoutingRule.builder()
                        .id(10L)
                        .masterFolio(testMasterFolio)
                        .chargeCategory(ChargeCategory.ROOM_RATE)
                        .routeTarget(RoutingTarget.MASTER)
                        .isActive(true)
                        .build()
        ));
        when(masterFolioItemAdjustmentRepository.findByMasterFolioId(1L)).thenReturn(Collections.emptyList());
        when(bookingLookupService.getBookingsByContractId(50L)).thenReturn(List.of(testBooking));
        when(serviceOrderService.getOrdersByBookingId(401L)).thenReturn(Collections.emptyList());

        GroupCheckoutRequest request = GroupCheckoutRequest.builder()
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentReference("FT26110699281")
                .waiveUnpaidSubFolios(false)
                .notes("Quyết toán tiền phòng còn lại cho đoàn FPT qua VCB")
                .build();

        // WHEN
        GroupCheckoutResponse response = masterFolioService.checkoutGroup(1L, request, "receptionist_hn");

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getMasterFolioId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(MasterFolioStatus.SETTLED);
        assertThat(response.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDepositCredited()).isEqualByComparingTo(BigDecimal.valueOf(4620000));
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(response.getPaymentReference()).isEqualTo("FT26110699281");
        assertThat(response.getCheckedOutRoomsCount()).isEqualTo(2);
        assertThat(response.getCheckedOutRoomNumbers()).containsExactlyInAnyOrder("101", "102");

        // Verify entities updated
        verify(masterFolioRepository, atLeastOnce()).save(any(MasterFolio.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(bookingRoomRepository, times(2)).save(any(BookingRoom.class));
        verify(roomInstanceService, times(1)).updateRoomStatusEntity(101, "DIRTY");
        verify(roomInstanceService, times(1)).updateRoomStatusEntity(102, "DIRTY");
        verify(roomSlotService, times(1)).markSlotsDirty(eq(101), any(), any());
        verify(roomSlotService, times(1)).markSlotsDirty(eq(102), any(), any());
        verify(groupContractRepository, times(1)).save(argThat(c -> c.getStatus() == GroupContractStatus.SETTLED));
        verify(auditLogService, times(1)).recordLog(isNull(), eq("GROUP_CHECKOUT_SETTLED"), eq("MASTER_FOLIO"), eq("1"), any(), any(), eq("receptionist_hn"), any());
    }

    @Test
    @DisplayName("TC_002: Validation Gate - Chặn quyết toán đoàn khi phòng con còn nợ cá nhân và không chọn gộp nợ")
    void validationGate_whenSubFoliosHaveUnpaidDebtAndNoWaiver_thenThrowsException() {
        // GIVEN: Phòng 101 phát sinh phụ phí Minibar 150.000 đ chưa trả
        BookingCharge minibarCharge = BookingCharge.builder()
                .id(5001L)
                .bookingRoom(bookingRoom101)
                .chargeType(BookingChargeType.OTHER)
                .itemName("Bia Tiger & Snack khoai tây")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(75000))
                .subtotal(BigDecimal.valueOf(150000))
                .serviceFeeRate(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatRate(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(150000))
                .createdAt(OffsetDateTime.now())
                .build();
        bookingRoom101.setCharges(List.of(minibarCharge));

        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));
        when(billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(1L)).thenReturn(List.of(
                BillingRoutingRule.builder()
                        .id(10L)
                        .masterFolio(testMasterFolio)
                        .chargeCategory(ChargeCategory.ROOM_RATE)
                        .routeTarget(RoutingTarget.MASTER)
                        .isActive(true)
                        .build()
        ));
        when(masterFolioItemAdjustmentRepository.findByMasterFolioId(1L)).thenReturn(Collections.emptyList());
        when(bookingLookupService.getBookingsByContractId(50L)).thenReturn(List.of(testBooking));
        when(serviceOrderService.getOrdersByBookingId(401L)).thenReturn(Collections.emptyList());

        GroupCheckoutRequest request = GroupCheckoutRequest.builder()
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .waiveUnpaidSubFolios(false) // Không cho phép gộp nợ
                .build();

        // WHEN & THEN
        assertThatThrownBy(() -> masterFolioService.checkoutGroup(1L, request, "receptionist_hn"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CHECKOUT_BALANCE_UNPAID);
                    assertThat(appEx.getMessage()).contains("Phòng 101");
                });

        // Verify phòng không bị đổi trạng thái
        verify(paymentRepository, never()).save(any());
        verify(bookingRoomRepository, never()).save(any());
        verify(roomInstanceService, never()).updateRoomStatusEntity(any(), any());
    }

    @Test
    @DisplayName("TC_003: Sub-Folio Waiver - Cho phép gộp nợ cá nhân vào Master Folio và quyết toán thành công")
    void subFolioWaiver_whenSubFoliosHaveUnpaidDebtAndWaiverTrue_thenTransfersDebtToMasterAndSuccess() {
        // GIVEN: Phòng 101 nợ minibar 150.000 đ
        BookingCharge minibarCharge = BookingCharge.builder()
                .id(5001L)
                .bookingRoom(bookingRoom101)
                .chargeType(BookingChargeType.OTHER)
                .itemName("Bia Tiger & Snack khoai tây")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(75000))
                .subtotal(BigDecimal.valueOf(150000))
                .serviceFeeRate(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatRate(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(150000))
                .createdAt(OffsetDateTime.now())
                .build();
        bookingRoom101.setCharges(List.of(minibarCharge));

        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));
        when(billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(1L)).thenReturn(List.of(
                BillingRoutingRule.builder()
                        .id(10L)
                        .masterFolio(testMasterFolio)
                        .chargeCategory(ChargeCategory.ROOM_RATE)
                        .routeTarget(RoutingTarget.MASTER)
                        .isActive(true)
                        .build()
        ));
        when(bookingLookupService.getBookingsByContractId(50L)).thenReturn(List.of(testBooking));
        when(serviceOrderService.getOrdersByBookingId(401L)).thenReturn(Collections.emptyList());

        // Sau khi adjust, MasterFolioItemAdjustment được lưu và trả về
        when(masterFolioItemAdjustmentRepository.findByMasterFolioId(1L))
                .thenReturn(Collections.emptyList()) // lần 1 phát hiện nợ
                .thenReturn(List.of( // lần 2 sau khi adjust
                        MasterFolioItemAdjustment.builder()
                                .itemType("SUB_FOLIO_WAIVER")
                                .referenceId(201L)
                                .adjustedTarget(RoutingTarget.MASTER)
                                .amount(BigDecimal.valueOf(150000))
                                .build()
                ));

        GroupCheckoutRequest request = GroupCheckoutRequest.builder()
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .waiveUnpaidSubFolios(true) // Bật cờ gộp nợ cá nhân về Master
                .paymentReference("CC-AUTH-88910")
                .build();

        // WHEN
        GroupCheckoutResponse response = masterFolioService.checkoutGroup(1L, request, "manager_hn");

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(MasterFolioStatus.SETTLED);
        verify(masterFolioItemAdjustmentRepository, times(1)).save(any(MasterFolioItemAdjustment.class));
        verify(bookingRoomRepository, times(2)).save(any(BookingRoom.class));
        verify(roomInstanceService, times(1)).updateRoomStatusEntity(101, "DIRTY");
        verify(roomInstanceService, times(1)).updateRoomStatusEntity(102, "DIRTY");
    }

    @Test
    @DisplayName("TC_004: Idempotency - Chặn quyết toán lại khi Master Folio đã đóng hoặc đã SETTLED")
    void idempotency_whenMasterFolioAlreadySettledOrClosed_thenThrowsException() {
        // GIVEN
        testMasterFolio.setStatus(MasterFolioStatus.SETTLED);
        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));

        GroupCheckoutRequest request = GroupCheckoutRequest.builder()
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // WHEN & THEN
        assertThatThrownBy(() -> masterFolioService.checkoutGroup(1L, request, "receptionist_hn"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MASTER_FOLIO_ALREADY_CLOSED);

        verify(paymentRepository, never()).save(any());
        verify(bookingRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_005: Validation - Báo lỗi không tìm thấy khi Master Folio ID không tồn tại")
    void notFound_whenMasterFolioDoesNotExist_thenThrowsException() {
        // GIVEN
        when(masterFolioRepository.findById(999L)).thenReturn(Optional.empty());

        GroupCheckoutRequest request = GroupCheckoutRequest.builder()
                .paymentMethod(PaymentMethod.CASH)
                .build();

        // WHEN & THEN
        assertThatThrownBy(() -> masterFolioService.checkoutGroup(999L, request, "receptionist_hn"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MASTER_FOLIO_NOT_FOUND);
    }
}
