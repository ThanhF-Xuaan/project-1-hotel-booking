package vn.edu.utc.hotel_booking.modules.dashboard.service;

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
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.request.ExecutiveReportFilterDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveBiReportResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveKpiSummaryDto;
import vn.edu.utc.hotel_booking.modules.dashboard.service.impl.ExecutiveReportServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderDetail;
import vn.edu.utc.hotel_booking.modules.operation.service.NightAuditService;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.organization.service.RegionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutiveReportServiceTest {

    @Mock HotelService hotelService;
    @Mock RegionService regionService;
    @Mock RoomInstanceService roomInstanceService;
    @Mock BookingLookupService bookingLookupService;
    @Mock ServiceOrderService serviceOrderService;
    @Mock NightAuditService nightAuditService;

    @InjectMocks
    ExecutiveReportServiceImpl executiveReportService;

    private Region northRegion;
    private Region centralRegion;
    private Hotel hanoiHotel;
    private Hotel danangHotel;
    private RoomInstance r1;
    private RoomInstance r2;
    private RoomInstance r3;

    @BeforeEach
    void setUp() {
        northRegion = Region.builder().name("Miền Bắc").build();
        northRegion.setId((short) 1);

        centralRegion = Region.builder().name("Miền Trung").build();
        centralRegion.setId((short) 2);

        hanoiHotel = Hotel.builder().name("Grand Hà Nội").region(northRegion).build();
        hanoiHotel.setId((short) 10);

        danangHotel = Hotel.builder().name("Luxury Đà Nẵng").region(centralRegion).build();
        danangHotel.setId((short) 20);

        r1 = RoomInstance.builder().hotel(hanoiHotel).roomNumber("101").build();
        r1.setId(1);
        r2 = RoomInstance.builder().hotel(hanoiHotel).roomNumber("102").build();
        r2.setId(2);
        r3 = RoomInstance.builder().hotel(danangHotel).roomNumber("201").build();
        r3.setId(3);
    }

    @Test
    @DisplayName("Scope CHAIN: Tính toán toàn diện KPI USALI, phân tách F&B, FIT vs GIT và kiểm toán đêm hoàn tất")
    void generateExecutiveReport_ChainScope_Success() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2026, 10, 21); // 2 ngày

        when(hotelService.getAllActiveHotels()).thenReturn(List.of(hanoiHotel, danangHotel));
        when(nightAuditService.isNightAuditCompleted(eq((short) 10), eq(end))).thenReturn(true);
        when(nightAuditService.isNightAuditCompleted(eq((short) 20), eq(end))).thenReturn(true);

        when(roomInstanceService.getActiveRoomsByHotelIds(List.of((short) 10, (short) 20)))
                .thenReturn(List.of(r1, r2, r3)); // 3 phòng * 2 ngày = 6 available room nights

        // Daily rate 1: FIT booking
        Booking fitBooking = Booking.builder().hotel(hanoiHotel).build();
        BookingDetail bd1 = BookingDetail.builder().booking(fitBooking).build();
        BookingRoom br1 = BookingRoom.builder().bookingDetail(bd1).build();
        BookingDailyRate rate1 = BookingDailyRate.builder()
                .bookingRoom(br1)
                .stayDate(start)
                .netPrice(BigDecimal.valueOf(1000000))
                .build();

        // Daily rate 2: GIT booking
        GroupContract contract = GroupContract.builder().contractNumber("HD-001").build();
        Booking gitBooking = Booking.builder().hotel(danangHotel).groupContract(contract).build();
        BookingDetail bd2 = BookingDetail.builder().booking(gitBooking).build();
        BookingRoom br2 = BookingRoom.builder().bookingDetail(bd2).build();
        BookingDailyRate rate2 = BookingDailyRate.builder()
                .bookingRoom(br2)
                .stayDate(end)
                .netPrice(BigDecimal.valueOf(2000000))
                .build();

        when(bookingLookupService.getActiveDailyRatesForHotelsAndDateRange(any(), eq(start), eq(end)))
                .thenReturn(List.of(rate1, rate2)); // 2 phòng occupied, tổng 3,000,000 VND

        // Service orders
        ServiceOrderDetail banquetItem = ServiceOrderDetail.builder()
                .itemName("Tiệc Gala Dinner")
                .itemType("BANQUET")
                .totalAmount(BigDecimal.valueOf(5000000))
                .build();
        ServiceOrderDetail restaurantItem = ServiceOrderDetail.builder()
                .itemName("Ăn trưa Buffet")
                .itemType("RESTAURANT")
                .totalAmount(BigDecimal.valueOf(500000))
                .build();
        ServiceOrderDetail spaItem = ServiceOrderDetail.builder()
                .itemName("Spa massage toàn thân")
                .itemType("SPA")
                .totalAmount(BigDecimal.valueOf(300000))
                .build();

        ServiceOrder order1 = ServiceOrder.builder()
                .hotel(hanoiHotel)
                .totalAmount(BigDecimal.valueOf(5500000))
                .issuedAt(OffsetDateTime.now())
                .details(List.of(banquetItem, restaurantItem))
                .build();

        ServiceOrder order2 = ServiceOrder.builder()
                .hotel(danangHotel)
                .totalAmount(BigDecimal.valueOf(300000))
                .issuedAt(OffsetDateTime.now())
                .details(List.of(spaItem))
                .build();

        when(serviceOrderService.getActiveOrdersForHotelsAndDateRange(any(), any(), any()))
                .thenReturn(List.of(order1, order2));

        ExecutiveReportFilterDto filter = ExecutiveReportFilterDto.builder()
                .scope("CHAIN")
                .startDate(start)
                .endDate(end)
                .build();

        ExecutiveBiReportResponse response = executiveReportService.generateExecutiveReport(filter);

        assertThat(response).isNotNull();
        assertThat(response.getScope()).isEqualTo("CHAIN");
        assertThat(response.getIsProvisional()).isFalse();
        assertThat(response.getProvisionalNotice()).isNull();

        // Kiểm tra KPI USALI
        ExecutiveKpiSummaryDto summary = response.getSummary();
        assertThat(summary.getTotalActiveRooms()).isEqualTo(3);
        assertThat(summary.getAvailableRoomNights()).isEqualTo(6L); // 3 rooms * 2 days
        assertThat(summary.getOccupiedRoomNights()).isEqualTo(2);
        assertThat(summary.getOccupancyRate()).isEqualTo(33.3); // 2/6 = 33.3%
        assertThat(summary.getTotalRoomRevenue()).isEqualByComparingTo(BigDecimal.valueOf(3000000));
        assertThat(summary.getAverageDailyRate()).isEqualByComparingTo(BigDecimal.valueOf(1500000)); // 3M / 2
        assertThat(summary.getRevPar()).isEqualByComparingTo(BigDecimal.valueOf(500000)); // 3M / 6

        // Kiểm tra phân tách F&B vs Other
        assertThat(summary.getTotalFnbRevenue()).isEqualByComparingTo(BigDecimal.valueOf(5500000));
        assertThat(summary.getTotalOtherRevenue()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(summary.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(8800000));

        // Kiểm tra FIT vs GIT (Bao gồm Room + Service orders)
        assertThat(response.getSegmentRevenue().getFitRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1300000));
        assertThat(response.getSegmentRevenue().getGitRevenue()).isEqualByComparingTo(BigDecimal.valueOf(7500000));

        // Kiểm tra phân tách Vùng và Khách sạn
        assertThat(response.getRegionBreakdowns()).hasSize(2);
        assertThat(response.getHotelBreakdowns()).hasSize(2);
        assertThat(response.getTrends()).hasSize(2); // 2 ngày
    }

    @Test
    @DisplayName("Ngoại lệ 3a SRS: Chưa hoàn tất Night Audit -> Bật cờ isProvisional và cảnh báo tạm tính")
    void generateExecutiveReport_NightAuditPending_SetsProvisionalWarning() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2026, 10, 21);

        when(hotelService.getAllActiveHotels()).thenReturn(List.of(hanoiHotel, danangHotel));
        when(nightAuditService.isNightAuditCompleted(eq((short) 10), eq(end))).thenReturn(true);
        when(nightAuditService.isNightAuditCompleted(eq((short) 20), eq(end))).thenReturn(false); // Khách sạn 20 chưa chốt!

        when(roomInstanceService.getActiveRoomsByHotelIds(any())).thenReturn(List.of(r1));
        when(bookingLookupService.getActiveDailyRatesForHotelsAndDateRange(any(), any(), any())).thenReturn(List.of());
        when(serviceOrderService.getActiveOrdersForHotelsAndDateRange(any(), any(), any())).thenReturn(List.of());

        ExecutiveReportFilterDto filter = ExecutiveReportFilterDto.builder()
                .scope("CHAIN")
                .startDate(start)
                .endDate(end)
                .build();

        ExecutiveBiReportResponse response = executiveReportService.generateExecutiveReport(filter);

        assertThat(response.getIsProvisional()).isTrue();
        assertThat(response.getProvisionalNotice()).contains("TẠM TÍNH");
    }

    @Test
    @DisplayName("Scope REGION: Lọc dữ liệu theo Vùng chỉ định thành công")
    void generateExecutiveReport_RegionScope_Success() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();

        when(regionService.getEntityById((short) 1)).thenReturn(northRegion);
        when(hotelService.getHotelsByRegionId((short) 1)).thenReturn(List.of(hanoiHotel));
        when(roomInstanceService.getActiveRoomsByHotelIds(List.of((short) 10))).thenReturn(List.of(r1, r2));
        when(bookingLookupService.getActiveDailyRatesForHotelsAndDateRange(any(), any(), any())).thenReturn(List.of());
        when(serviceOrderService.getActiveOrdersForHotelsAndDateRange(any(), any(), any())).thenReturn(List.of());

        ExecutiveReportFilterDto filter = ExecutiveReportFilterDto.builder()
                .scope("REGION")
                .regionId((short) 1)
                .startDate(start)
                .endDate(end)
                .build();

        ExecutiveBiReportResponse response = executiveReportService.generateExecutiveReport(filter);

        assertThat(response).isNotNull();
        assertThat(response.getScope()).isEqualTo("REGION");
        assertThat(response.getScopeName()).isEqualTo("Vùng Miền Bắc");
    }

    @Test
    @DisplayName("Scope HOTEL: Cung cấp thiếu hotelId ném ngoại lệ INVALID_REQUEST_DATA")
    void generateExecutiveReport_HotelScope_MissingHotelId_ThrowsException() {
        ExecutiveReportFilterDto filter = ExecutiveReportFilterDto.builder()
                .scope("HOTEL")
                .hotelId(null)
                .build();

        assertThatThrownBy(() -> executiveReportService.generateExecutiveReport(filter))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("Khoảng thời gian không hợp lệ: startDate sau endDate ném INVALID_REQUEST_DATA")
    void generateExecutiveReport_InvalidDates_ThrowsException() {
        ExecutiveReportFilterDto filter = ExecutiveReportFilterDto.builder()
                .startDate(LocalDate.of(2026, 10, 25))
                .endDate(LocalDate.of(2026, 10, 20))
                .build();

        assertThatThrownBy(() -> executiveReportService.generateExecutiveReport(filter))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("getQuickSummary trả về tóm tắt KPI hợp lệ cho một ngày cụ thể")
    void getQuickSummary_Success() {
        LocalDate today = LocalDate.now();

        when(hotelService.getAllActiveHotels()).thenReturn(List.of(hanoiHotel));
        when(roomInstanceService.getActiveRoomsByHotelIds(any())).thenReturn(List.of(r1));
        when(bookingLookupService.getActiveDailyRatesForHotelsAndDateRange(any(), any(), any())).thenReturn(List.of());
        when(serviceOrderService.getActiveOrdersForHotelsAndDateRange(any(), any(), any())).thenReturn(List.of());

        ExecutiveKpiSummaryDto summary = executiveReportService.getQuickSummary("CHAIN", null, null, today);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalActiveRooms()).isEqualTo(1);
    }
}
