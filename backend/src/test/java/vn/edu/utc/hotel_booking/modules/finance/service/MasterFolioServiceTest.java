package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.BillingRoutingRuleRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioAdjustmentRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.BillingRoutingRuleResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.MasterFolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.repository.BillingRoutingRuleRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioItemAdjustmentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.MasterFolioRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.MasterFolioServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Master Folio & Automated Cost Routing Service Unit Test")
class MasterFolioServiceTest {

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

    @InjectMocks
    private MasterFolioServiceImpl masterFolioService;

    private Hotel testHotel;
    private Company testCompany;
    private GroupContract testContract;
    private MasterFolio testMasterFolio;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder()
                .id((short) 1)
                .name("Grand Hotel Saigon")
                .build();

        testCompany = Company.builder()
                .id(10L)
                .taxCode("0102030405")
                .name("Công ty Cổ phần Du lịch FPT")
                .address("10 Phạm Văn Bạch, Cầu Giấy, Hà Nội")
                .contactEmail("accounting@fpt.vn")
                .build();

        testContract = GroupContract.builder()
                .id(100L)
                .contractNumber("GC-20261026-0001")
                .hotel(testHotel)
                .company(testCompany)
                .groupName("Đoàn Hội Nghị FPT Software")
                .companyTaxCode("0102030405")
                .subtotalAmount(new BigDecimal("20000000.00"))
                .serviceFeeAmount(new BigDecimal("1000000.00"))
                .vatAmount(new BigDecimal("2100000.00"))
                .totalContractValue(new BigDecimal("23100000.00"))
                .status(GroupContractStatus.DEPOSIT_PAID)
                .build();

        testMasterFolio = MasterFolio.builder()
                .id(1L)
                .folioNumber("MF-20261026-0001")
                .contract(testContract)
                .hotel(testHotel)
                .company(testCompany)
                .buyerLegalName(testContract.getGroupName())
                .buyerTaxCode(testContract.getCompanyTaxCode())
                .buyerAddress(testCompany.getAddress())
                .buyerEmailForInvoice(testCompany.getContactEmail())
                .subtotalAmount(new BigDecimal("20000000.00"))
                .serviceFeeAmount(new BigDecimal("1000000.00"))
                .totalVatAmount(new BigDecimal("2100000.00"))
                .totalAmount(new BigDecimal("23100000.00"))
                .depositCredited(new BigDecimal("11550000.00")) // Cọc 50%
                .totalPaid(new BigDecimal("11550000.00"))
                .balance(new BigDecimal("11550000.00"))
                .status(MasterFolioStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("Khởi tạo Master Folio khi cọc 50% thành công với 4 quy tắc mặc định")
    void createMasterFolioForContract_Success() {
        when(masterFolioRepository.save(any(MasterFolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MasterFolio result = masterFolioService.createMasterFolioForContract(testContract, new BigDecimal("11550000.00"));

        assertThat(result).isNotNull();
        assertThat(result.getFolioNumber()).startsWith("MF-");
        assertThat(result.getBuyerLegalName()).isEqualTo("Đoàn Hội Nghị FPT Software");
        assertThat(result.getDepositCredited()).isEqualByComparingTo(new BigDecimal("11550000.00"));
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("11550000.00"));
        assertThat(result.getStatus()).isEqualTo(MasterFolioStatus.OPEN);
        assertThat(result.getRoutingRules()).hasSize(4);

        verify(masterFolioRepository, times(1)).save(any(MasterFolio.class));
        verify(auditLogService, times(1)).recordLog(any(), eq("CREATE_MASTER_FOLIO"), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lấy Master Folio động: Gom tiền phòng và tiệc về Master, minibar về Sub-Folio")
    void getMasterFolioById_WithDynamicRouting() {
        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));
        when(masterFolioRepository.save(any(MasterFolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2 Routing rules
        BillingRoutingRule roomRule = BillingRoutingRule.builder()
                .id(1L)
                .masterFolio(testMasterFolio)
                .chargeCategory(ChargeCategory.ROOM_RATE)
                .routeTarget(RoutingTarget.MASTER)
                .isActive(true)
                .build();

        BillingRoutingRule banquetRule = BillingRoutingRule.builder()
                .id(2L)
                .masterFolio(testMasterFolio)
                .chargeCategory(ChargeCategory.BANQUET)
                .routeTarget(RoutingTarget.MASTER)
                .isActive(true)
                .build();

        BillingRoutingRule minibarRule = BillingRoutingRule.builder()
                .id(3L)
                .masterFolio(testMasterFolio)
                .chargeCategory(ChargeCategory.MINIBAR)
                .routeTarget(RoutingTarget.SUB_FOLIO)
                .isActive(true)
                .build();

        when(billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(1L))
                .thenReturn(List.of(roomRule, banquetRule, minibarRule));

        when(masterFolioItemAdjustmentRepository.findByMasterFolioId(1L))
                .thenReturn(new ArrayList<>());

        // Tạo 1 Booking đoàn có 1 phòng (ROOM_RATE)
        BookingRoom bookingRoom = BookingRoom.builder()
                .id(101L)
                .roomInstance(RoomInstance.builder().id(1).roomNumber("301").build())
                .status(BookingRoomStatus.CHECKED_IN)
                .build();

        BookingDailyRate dailyRate = BookingDailyRate.builder()
                .id(501L)
                .bookingRoom(bookingRoom)
                .stayDate(LocalDate.now())
                .netPrice(new BigDecimal("1000000.00"))
                .vatPercent(BigDecimal.TEN)
                .build();

        bookingRoom.setDailyRates(List.of(dailyRate));

        BookingDetail detail = BookingDetail.builder()
                .id(11L)
                .roomTypeName("Deluxe Suite")
                .bookingRooms(List.of(bookingRoom))
                .build();

        Booking groupBooking = Booking.builder()
                .id(1001L)
                .bookingNumber("BK-GRP-001")
                .hotel(testHotel)
                .groupContract(testContract)
                .bookingGuest(BookingGuest.builder().fullName("Nguyễn Văn A").build())
                .serviceFeeRate(new BigDecimal("5.00"))
                .bookingDetails(List.of(detail))
                .build();

        when(bookingLookupService.getBookingsByContractId(100L)).thenReturn(List.of(groupBooking));

        // Tạo 1 đơn tiệc Banquet và 1 đơn Minibar
        ServiceOrderResponse banquetOrder = ServiceOrderResponse.builder()
                .id(201L)
                .orderNumber("SO-BANQUET-01")
                .paymentMethod(ServiceOrderPaymentMethod.POST_TO_ROOM)
                .paymentStatus(ServiceOrderPaymentStatus.UNPAID)
                .roomNumber("301")
                .details(List.of(ServiceOrderDetailResponse.builder()
                        .id(301L)
                        .itemName("Tiệc Gala Dinner Đoàn")
                        .quantity(1)
                        .unitPrice(new BigDecimal("5000000.00"))
                        .subtotal(new BigDecimal("5000000.00"))
                        .serviceFeeRate(BigDecimal.ZERO)
                        .serviceFeeAmount(BigDecimal.ZERO)
                        .vatRate(BigDecimal.TEN)
                        .vatAmount(new BigDecimal("500000.00"))
                        .totalAmount(new BigDecimal("5500000.00"))
                        .build()))
                .build();

        ServiceOrderResponse minibarOrder = ServiceOrderResponse.builder()
                .id(202L)
                .orderNumber("SO-MINIBAR-01")
                .paymentMethod(ServiceOrderPaymentMethod.POST_TO_ROOM)
                .paymentStatus(ServiceOrderPaymentStatus.UNPAID)
                .roomNumber("301")
                .details(List.of(ServiceOrderDetailResponse.builder()
                        .id(302L)
                        .itemName("Nước suối Minibar phòng 301")
                        .quantity(2)
                        .unitPrice(new BigDecimal("20000.00"))
                        .subtotal(new BigDecimal("40000.00"))
                        .serviceFeeRate(BigDecimal.ZERO)
                        .serviceFeeAmount(BigDecimal.ZERO)
                        .vatRate(BigDecimal.TEN)
                        .vatAmount(new BigDecimal("4000.00"))
                        .totalAmount(new BigDecimal("44000.00"))
                        .build()))
                .build();

        when(serviceOrderService.getOrdersByBookingId(1001L)).thenReturn(List.of(banquetOrder, minibarOrder));

        // When
        MasterFolioResponse response = masterFolioService.getMasterFolioById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFolioNumber()).isEqualTo("MF-20261026-0001");
        // Debits gom: 1 Room Rate (1,155,000) + 1 Banquet (5,500,000) = 2 debits. Minibar bi loai ve Sub-Folio
        assertThat(response.getDebits()).hasSize(2);
        assertThat(response.getDebits().get(0).getItemType()).isEqualTo("ROOM_RATE");
        assertThat(response.getDebits().get(1).getDescription()).contains("Gala Dinner");

        // SubFolio summary
        assertThat(response.getSubFolios()).hasSize(1);
        assertThat(response.getSubFolios().get(0).getRoomNumber()).isEqualTo("301");
        assertThat(response.getSubFolios().get(0).getCoveredByMasterAmount()).isEqualByComparingTo(new BigDecimal("1155000.00"));

        // Deposit 50%
        assertThat(response.getDepositCredited()).isEqualByComparingTo(new BigDecimal("11550000.00"));
    }

    @Test
    @DisplayName("Thêm và cập nhật quy tắc định tuyến chi phí thành công")
    void addAndUpdateRoutingRule_Success() {
        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));
        when(billingRoutingRuleRepository.findByMasterFolioIdAndChargeCategoryAndIsActiveTrue(1L, ChargeCategory.BREAKFAST))
                .thenReturn(Optional.empty());

        when(billingRoutingRuleRepository.save(any(BillingRoutingRule.class))).thenAnswer(inv -> {
            BillingRoutingRule r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        BillingRoutingRuleRequest addRequest = BillingRoutingRuleRequest.builder()
                .chargeCategory(ChargeCategory.BREAKFAST)
                .routeTarget(RoutingTarget.MASTER)
                .maxCoveredAmount(new BigDecimal("200000.00"))
                .isActive(true)
                .build();

        BillingRoutingRuleResponse response = masterFolioService.addRoutingRule(1L, addRequest);

        assertThat(response).isNotNull();
        assertThat(response.getChargeCategory()).isEqualTo(ChargeCategory.BREAKFAST);
        assertThat(response.getRouteTarget()).isEqualTo(RoutingTarget.MASTER);
        assertThat(response.getMaxCoveredAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));

        verify(billingRoutingRuleRepository, times(1)).save(any(BillingRoutingRule.class));
        verify(auditLogService, times(1)).recordLog(any(), eq("ADD_BILLING_ROUTING_RULE"), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Điều chuyển thủ công chi phí giữa Sub-Folio và Master Folio (Ngoại lệ 2a SRS)")
    void adjustItemRouting_Success() {
        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));
        when(masterFolioItemAdjustmentRepository.save(any(MasterFolioItemAdjustment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(masterFolioRepository.save(any(MasterFolio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(1L)).thenReturn(new ArrayList<>());
        when(masterFolioItemAdjustmentRepository.findByMasterFolioId(1L)).thenReturn(new ArrayList<>());
        when(bookingLookupService.getBookingsByContractId(any())).thenReturn(new ArrayList<>());

        MasterFolioAdjustmentRequest request = MasterFolioAdjustmentRequest.builder()
                .bookingRoomId(101L)
                .chargeCategory(ChargeCategory.MINIBAR)
                .itemType("SERVICE_ORDER_DETAIL")
                .referenceId(302L)
                .itemDescription("Chuyển tiền Minibar phòng 301 sang Công ty chi trả")
                .adjustedTarget(RoutingTarget.MASTER)
                .amount(new BigDecimal("44000.00"))
                .adjustmentReason("Giám đốc kinh doanh đồng ý bao trọn gói Minibar cho phòng VIP")
                .build();

        MasterFolioResponse response = masterFolioService.adjustItemRouting(1L, request, "receptionist_mai");

        assertThat(response).isNotNull();
        verify(masterFolioItemAdjustmentRepository, times(1)).save(any(MasterFolioItemAdjustment.class));
        verify(auditLogService, times(1)).recordLog(any(), eq("MANUAL_ROUTING_ADJUSTMENT"), any(), any(), any(), any(), eq("receptionist_mai"), any());
    }

    @Test
    @DisplayName("Cập nhật trạng thái Master Folio: Khóa quyết toán và Đóng Folio")
    void updateMasterFolioStatus_Success() {
        when(masterFolioRepository.findById(1L)).thenReturn(Optional.of(testMasterFolio));
        when(masterFolioRepository.save(any(MasterFolio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(billingRoutingRuleRepository.findByMasterFolioIdAndIsActiveTrue(1L)).thenReturn(new ArrayList<>());
        when(masterFolioItemAdjustmentRepository.findByMasterFolioId(1L)).thenReturn(new ArrayList<>());
        when(bookingLookupService.getBookingsByContractId(any())).thenReturn(new ArrayList<>());

        MasterFolioStatusUpdateRequest request = MasterFolioStatusUpdateRequest.builder()
                .status(MasterFolioStatus.LOCKED_FOR_SETTLEMENT)
                .note("Khóa sổ để Kế toán đoàn rà soát công nợ")
                .build();

        MasterFolioResponse response = masterFolioService.updateMasterFolioStatus(1L, request);

        assertThat(response).isNotNull();
        assertThat(testMasterFolio.getStatus()).isEqualTo(MasterFolioStatus.LOCKED_FOR_SETTLEMENT);

        verify(masterFolioRepository, times(2)).save(testMasterFolio);
        verify(auditLogService, times(1)).recordLog(any(), eq("UPDATE_MASTER_FOLIO_STATUS"), any(), any(), any(), any(), any(), any());
    }
}
