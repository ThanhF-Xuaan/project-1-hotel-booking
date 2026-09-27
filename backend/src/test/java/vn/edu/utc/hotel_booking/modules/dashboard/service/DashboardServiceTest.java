package vn.edu.utc.hotel_booking.modules.dashboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDailyRateRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDetailRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.DashboardKpiResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.OccupancyTrendDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.RevenueTrendDto;
import vn.edu.utc.hotel_booking.modules.dashboard.service.impl.DashboardServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock HotelRepository hotelRepository;
    @Mock RoomInstanceRepository roomInstanceRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingDetailRepository bookingDetailRepository;
    @Mock BookingDailyRateRepository bookingDailyRateRepository;
    @Mock ServiceOrderRepository serviceOrderRepository;

    @InjectMocks
    DashboardServiceImpl dashboardService;

    private Hotel testHotel;
    private RoomInstance room1;
    private RoomInstance room2;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        testHotel.setId((short) 1);

        room1 = RoomInstance.builder().hotel(testHotel).roomNumber("101").currentStatus("OCCUPIED").build();
        room1.setId(1);

        room2 = RoomInstance.builder().hotel(testHotel).roomNumber("102").currentStatus("READY").build();
        room2.setId(2);
    }

    @Test
    @DisplayName("Tính toán chính xác các chỉ số KPI ngành khách sạn: OCC %, ADR, RevPAR")
    void getExecutiveKpis_Success() {
        LocalDate today = LocalDate.now();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(testHotel));
        when(roomInstanceRepository.findAll()).thenReturn(List.of(room1, room2));

        BookingDailyRate rate = BookingDailyRate.builder()
                .netPrice(BigDecimal.valueOf(1000000))
                .stayDate(today)
                .build();
        when(bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate((short) 1, today))
                .thenReturn(List.of(rate));

        ServiceOrder order = ServiceOrder.builder()
                .totalAmount(BigDecimal.valueOf(200000))
                .build();
        when(serviceOrderRepository.findActiveOrdersForHotelAndDate(eq((short) 1), any(), any()))
                .thenReturn(List.of(order));

        when(bookingDetailRepository.findAll()).thenReturn(List.of());

        DashboardKpiResponse kpis = dashboardService.getExecutiveKpis((short) 1, today);

        assertThat(kpis).isNotNull();
        assertThat(kpis.getTotalActiveRooms()).isEqualTo(2);
        assertThat(kpis.getOccupiedRooms()).isEqualTo(1);
        assertThat(kpis.getOccupancyRate()).isEqualTo(50.0); // 1 / 2 * 100
        assertThat(kpis.getTotalRoomRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(kpis.getTotalServiceRevenue()).isEqualByComparingTo(BigDecimal.valueOf(200000));
        assertThat(kpis.getTotalRevenueToday()).isEqualByComparingTo(BigDecimal.valueOf(1200000));
        assertThat(kpis.getAverageDailyRate()).isEqualByComparingTo(BigDecimal.valueOf(1000000)); // 1M / 1
        assertThat(kpis.getRevPar()).isEqualByComparingTo(BigDecimal.valueOf(500000)); // 1M / 2
    }

    @Test
    @DisplayName("Lấy xu hướng doanh thu theo chu kỳ ngày thành công")
    void getRevenueTrends_Success() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now();

        when(bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(eq((short) 1), any()))
                .thenReturn(List.of());
        when(serviceOrderRepository.findActiveOrdersForHotelAndDate(eq((short) 1), any(), any()))
                .thenReturn(List.of());

        List<RevenueTrendDto> trends = dashboardService.getRevenueTrends((short) 1, start, end);

        assertThat(trends).hasSize(3); // 3 ngày: -2, -1, 0
    }

    @Test
    @DisplayName("Lấy xu hướng tỷ lệ lấp đầy phòng thành công")
    void getOccupancyTrends_Success() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();

        when(roomInstanceRepository.findAll()).thenReturn(List.of(room1, room2));
        when(bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(eq((short) 1), any()))
                .thenReturn(List.of());

        List<OccupancyTrendDto> trends = dashboardService.getOccupancyTrends((short) 1, start, end);

        assertThat(trends).hasSize(2);
    }
}
