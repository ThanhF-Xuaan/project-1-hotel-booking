package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.NightAuditRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.NightAuditResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.NightAudit;
import vn.edu.utc.hotel_booking.modules.operation.repository.NightAuditRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.NightAuditServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NightAuditServiceTest {

    @Mock HotelService hotelService;
    @Mock BookingService bookingService;
    @Mock BookingLookupService bookingLookupService;
    @Mock RoomInstanceService roomInstanceService;
    @Mock ServiceOrderService serviceOrderService;
    @Mock RoomAvailabilityGenerationService roomAvailabilityGenerationService;
    @Mock NightAuditRepository nightAuditRepository;

    @InjectMocks
    NightAuditServiceImpl nightAuditService;

    private Hotel testHotel;
    private LocalDate auditDate;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        testHotel.setId((short) 1);
        auditDate = LocalDate.of(2026, 6, 1);
    }

    @Test
    @DisplayName("executeNightAudit - Chốt ca đêm thành công với đầy đủ doanh thu phòng và dịch vụ, lưu biên bản")
    void executeNightAudit_Success() {
        NightAuditRequest request = NightAuditRequest.builder()
                .hotelId((short) 1)
                .auditDate(auditDate)
                .build();

        when(nightAuditRepository.existsByHotelIdAndAuditDate((short) 1, auditDate)).thenReturn(false);
        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(bookingService.processNoShowBookings((short) 1, auditDate)).thenReturn(2);
        when(roomInstanceService.countOccupiedRooms((short) 1)).thenReturn(10);
        when(bookingLookupService.getDailyRoomRevenue((short) 1, auditDate)).thenReturn(BigDecimal.valueOf(5000000));
        when(serviceOrderService.getDailyServiceRevenue((short) 1, auditDate)).thenReturn(BigDecimal.valueOf(1500000));
        when(nightAuditRepository.save(any(NightAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NightAuditResponse response = nightAuditService.executeNightAudit(request);

        assertThat(response).isNotNull();
        assertThat(response.getHotelId()).isEqualTo((short) 1);
        assertThat(response.getAuditDate()).isEqualTo(auditDate);
        assertThat(response.getNoShowBookingsCount()).isEqualTo(2);
        assertThat(response.getOccupiedRoomsCount()).isEqualTo(10);
        assertThat(response.getTotalDailyRoomRevenue()).isEqualByComparingTo(BigDecimal.valueOf(5000000));
        assertThat(response.getTotalDailyServiceRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        assertThat(response.getTotalDailyRevenue()).isEqualByComparingTo(BigDecimal.valueOf(6500000));
        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        verify(nightAuditRepository).existsByHotelIdAndAuditDate((short) 1, auditDate);
        verify(hotelService).getEntityById((short) 1);
        verify(bookingService).processNoShowBookings((short) 1, auditDate);
        verify(roomInstanceService).countOccupiedRooms((short) 1);
        verify(bookingLookupService).getDailyRoomRevenue((short) 1, auditDate);
        verify(serviceOrderService).getDailyServiceRevenue((short) 1, auditDate);
        verify(nightAuditRepository).save(any(NightAudit.class));
    }

    @Test
    @DisplayName("executeNightAudit - Báo lỗi NIGHT_AUDIT_ALREADY_EXECUTED khi ngày kiểm toán đã được chốt sổ trước đó")
    void executeNightAudit_AlreadyExecuted_ThrowsException() {
        NightAuditRequest request = NightAuditRequest.builder()
                .hotelId((short) 1)
                .auditDate(auditDate)
                .build();

        when(nightAuditRepository.existsByHotelIdAndAuditDate((short) 1, auditDate)).thenReturn(true);

        assertThatThrownBy(() -> nightAuditService.executeNightAudit(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.NIGHT_AUDIT_ALREADY_EXECUTED));

        verify(nightAuditRepository).existsByHotelIdAndAuditDate((short) 1, auditDate);
        verifyNoInteractions(hotelService);
        verifyNoInteractions(bookingService);
        verifyNoInteractions(roomInstanceService);
        verifyNoInteractions(bookingLookupService);
        verifyNoInteractions(serviceOrderService);
    }

    @Test
    @DisplayName("getAuditSummary - Xem trước số liệu kiểm toán khi chưa chốt sổ (PREVIEW mode)")
    void getAuditSummary_PreviewMode_Success() {
        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(nightAuditRepository.findByHotelIdAndAuditDate((short) 1, auditDate)).thenReturn(Optional.empty());
        when(roomInstanceService.countOccupiedRooms((short) 1)).thenReturn(8);
        when(bookingLookupService.getDailyRoomRevenue((short) 1, auditDate)).thenReturn(BigDecimal.valueOf(4000000));
        when(serviceOrderService.getDailyServiceRevenue((short) 1, auditDate)).thenReturn(BigDecimal.ZERO);

        NightAuditResponse response = nightAuditService.getAuditSummary((short) 1, auditDate);

        assertThat(response).isNotNull();
        assertThat(response.getHotelId()).isEqualTo((short) 1);
        assertThat(response.getNoShowBookingsCount()).isEqualTo(0);
        assertThat(response.getOccupiedRoomsCount()).isEqualTo(8);
        assertThat(response.getTotalDailyRoomRevenue()).isEqualByComparingTo(BigDecimal.valueOf(4000000));
        assertThat(response.getTotalDailyServiceRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalDailyRevenue()).isEqualByComparingTo(BigDecimal.valueOf(4000000));
        assertThat(response.getStatus()).isEqualTo("PREVIEW");

        verifyNoInteractions(bookingService);
    }

    @Test
    @DisplayName("getAuditSummary - Trả về biên bản đã lưu trong DB nếu ngày đó đã được chốt sổ (COMPLETED mode)")
    void getAuditSummary_AlreadyCompleted_ReturnsSavedAudit() {
        NightAudit savedAudit = NightAudit.builder()
                .hotel(testHotel)
                .auditDate(auditDate)
                .occupiedRoomsCount(12)
                .noShowBookingsCount(3)
                .totalDailyRoomRevenue(BigDecimal.valueOf(6000000))
                .totalDailyServiceRevenue(BigDecimal.valueOf(1200000))
                .totalDailyRevenue(BigDecimal.valueOf(7200000))
                .status("COMPLETED")
                .executedAt(OffsetDateTime.now())
                .executedBy("night_manager")
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(nightAuditRepository.findByHotelIdAndAuditDate((short) 1, auditDate)).thenReturn(Optional.of(savedAudit));

        NightAuditResponse response = nightAuditService.getAuditSummary((short) 1, auditDate);

        assertThat(response).isNotNull();
        assertThat(response.getHotelId()).isEqualTo((short) 1);
        assertThat(response.getAuditDate()).isEqualTo(auditDate);
        assertThat(response.getNoShowBookingsCount()).isEqualTo(3);
        assertThat(response.getOccupiedRoomsCount()).isEqualTo(12);
        assertThat(response.getTotalDailyRoomRevenue()).isEqualByComparingTo(BigDecimal.valueOf(6000000));
        assertThat(response.getTotalDailyServiceRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1200000));
        assertThat(response.getTotalDailyRevenue()).isEqualByComparingTo(BigDecimal.valueOf(7200000));
        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        verifyNoInteractions(bookingService);
        verifyNoInteractions(roomInstanceService);
        verifyNoInteractions(bookingLookupService);
        verifyNoInteractions(serviceOrderService);
    }

    @Test
    @DisplayName("executeNightAudit - Báo lỗi HOTEL_NOT_FOUND khi khách sạn không tồn tại")
    void executeNightAudit_HotelNotFound_ThrowsException() {
        NightAuditRequest request = NightAuditRequest.builder()
                .hotelId((short) 999)
                .auditDate(auditDate)
                .build();

        when(nightAuditRepository.existsByHotelIdAndAuditDate((short) 999, auditDate)).thenReturn(false);
        when(hotelService.getEntityById((short) 999))
                .thenThrow(new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn"));

        assertThatThrownBy(() -> nightAuditService.executeNightAudit(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.HOTEL_NOT_FOUND));

        verifyNoInteractions(bookingService);
    }
}
