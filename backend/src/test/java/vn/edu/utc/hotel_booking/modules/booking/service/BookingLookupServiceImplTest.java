package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingLookupServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingLookupServiceImplTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    BookingRoomRepository bookingRoomRepository;

    @Mock
    vn.edu.utc.hotel_booking.modules.booking.repository.BookingDailyRateRepository bookingDailyRateRepository;

    @Mock
    BookingRoomService bookingRoomService;

    @InjectMocks
    BookingLookupServiceImpl bookingLookupService;

    @Test
    @DisplayName("getBookingEntityById - Thành công khi ID hợp lệ và tồn tại")
    void getBookingEntityById_Success() {
        Booking booking = Booking.builder().id(100L).bookingNumber("BK12345678").build();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        Booking result = bookingLookupService.getBookingEntityById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        verify(bookingRepository).findById(100L);
    }

    @Test
    @DisplayName("getBookingEntityById - Ném ngoại lệ khi ID là null")
    void getBookingEntityById_NullId_ThrowsException() {
        assertThatThrownBy(() -> bookingLookupService.getBookingEntityById(null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA));
    }

    @Test
    @DisplayName("getBookingEntityById - Ném ngoại lệ khi không tìm thấy đơn đặt phòng")
    void getBookingEntityById_NotFound_ThrowsException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingLookupService.getBookingEntityById(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.BOOKING_NOT_FOUND));
    }

    @Test
    @DisplayName("getBookingRoomEntityById - Thành công khi ID hợp lệ")
    void getBookingRoomEntityById_Success() {
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).build();
        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);

        BookingRoom result = bookingLookupService.getBookingRoomEntityById(20L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
        verify(bookingRoomService).getEntityById(20L);
    }

    @Test
    @DisplayName("getBookingRoomEntityById - Ném ngoại lệ khi ID là null")
    void getBookingRoomEntityById_NullId_ThrowsException() {
        assertThatThrownBy(() -> bookingLookupService.getBookingRoomEntityById(null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA));
    }

    @Test
    @DisplayName("getBookingRoomsByBookingId - Thành công khi bookingId hợp lệ")
    void getBookingRoomsByBookingId_Success() {
        BookingRoom room = BookingRoom.builder().id(10L).build();
        when(bookingRoomService.findByBookingId(100L)).thenReturn(java.util.List.of(room));

        java.util.List<BookingRoom> result = bookingLookupService.getBookingRoomsByBookingId(100L);

        assertThat(result).hasSize(1);
        verify(bookingRoomService).findByBookingId(100L);
    }

    @Test
    @DisplayName("getBookingRoomsByBookingId - Trả về rỗng khi bookingId null")
    void getBookingRoomsByBookingId_NullId_ReturnsEmpty() {
        java.util.List<BookingRoom> result = bookingLookupService.getBookingRoomsByBookingId(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(bookingRoomService);
    }

    @Test
    @DisplayName("getBookingByRoomId - Thành công khi tìm được Booking qua bookingDetail")
    void getBookingByRoomId_Success() {
        Booking booking = Booking.builder().id(100L).bookingNumber("BK12345678").build();
        vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail detail =
                vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail.builder()
                        .id(50L)
                        .booking(booking)
                        .build();
        BookingRoom room = BookingRoom.builder().id(20L).bookingDetail(detail).build();

        when(bookingRoomService.getEntityById(20L)).thenReturn(room);

        Booking result = bookingLookupService.getBookingByRoomId(20L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getBookingByRoomId - Ném BOOKING_NOT_FOUND khi bookingDetail bị null")
    void getBookingByRoomId_NoBookingDetail_ThrowsException() {
        BookingRoom room = BookingRoom.builder().id(20L).bookingDetail(null).build();
        when(bookingRoomService.getEntityById(20L)).thenReturn(room);

        assertThatThrownBy(() -> bookingLookupService.getBookingByRoomId(20L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.BOOKING_NOT_FOUND));
    }

    @Test
    @DisplayName("getActiveOccupanciesByRoomInstanceIds - Trả về Map rỗng khi danh sách IDs rỗng hoặc null")
    void getActiveOccupanciesByRoomInstanceIds_EmptyIds_ReturnsEmptyMap() {
        assertThat(bookingLookupService.getActiveOccupanciesByRoomInstanceIds(null)).isEmpty();
        assertThat(bookingLookupService.getActiveOccupanciesByRoomInstanceIds(java.util.List.of())).isEmpty();
        verifyNoInteractions(bookingRoomRepository);
    }

    @Test
    @DisplayName("getActiveOccupanciesByRoomInstanceIds - Batch lookup thành công ánh xạ đầy đủ thông tin lưu trú")
    void getActiveOccupanciesByRoomInstanceIds_Success() {
        Booking booking = Booking.builder().id(100L).bookingNumber("BK99999999").build();
        vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail detail =
                vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail.builder()
                        .booking(booking)
                        .checkOutDate(java.time.LocalDate.of(2026, 7, 1))
                        .build();
        vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance roomInst =
                vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance.builder().build();
        roomInst.setId(55);

        vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest guest =
                vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest.builder()
                        .fullName("Phạm Văn C")
                        .build();

        BookingRoom activeRoom = BookingRoom.builder()
                .id(1L)
                .roomInstance(roomInst)
                .bookingDetail(detail)
                .stayGuests(java.util.List.of(guest))
                .status(vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus.CHECKED_IN)
                .build();

        when(bookingRoomRepository.findActiveByRoomInstanceIdInWithDetails(
                eq(java.util.List.of(55)),
                eq(java.util.List.of(vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus.CHECKED_IN))
        )).thenReturn(java.util.List.of(activeRoom));

        java.util.Map<Integer, vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomOccupancyDto> result =
                bookingLookupService.getActiveOccupanciesByRoomInstanceIds(java.util.List.of(55));

        assertThat(result).hasSize(1);
        assertThat(result.get(55)).isNotNull();
        assertThat(result.get(55).getBookingNumber()).isEqualTo("BK99999999");
        assertThat(result.get(55).getGuestName()).isEqualTo("Phạm Văn C");
        assertThat(result.get(55).getCheckOutDate()).isEqualTo(java.time.LocalDate.of(2026, 7, 1));
    }

    @Test
    @DisplayName("getActiveOccupancyByRoomInstanceId - Tra cứu phòng đơn lẻ thành công")
    void getActiveOccupancyByRoomInstanceId_Success() {
        Booking booking = Booking.builder().id(100L).bookingNumber("BK11111111").build();
        vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail detail =
                vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail.builder()
                        .booking(booking)
                        .checkOutDate(java.time.LocalDate.of(2026, 7, 5))
                        .build();
        vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance roomInst =
                vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance.builder().build();
        roomInst.setId(77);

        BookingRoom activeRoom = BookingRoom.builder()
                .id(2L)
                .roomInstance(roomInst)
                .bookingDetail(detail)
                .status(vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus.CHECKED_IN)
                .build();

        when(bookingRoomRepository.findActiveByRoomInstanceIdInWithDetails(
                eq(java.util.List.of(77)),
                any()
        )).thenReturn(java.util.List.of(activeRoom));

        vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomOccupancyDto dto =
                bookingLookupService.getActiveOccupancyByRoomInstanceId(77);

        assertThat(dto).isNotNull();
        assertThat(dto.getBookingNumber()).isEqualTo("BK11111111");
        assertThat(dto.getCheckOutDate()).isEqualTo(java.time.LocalDate.of(2026, 7, 5));
    }

    @Test
    @DisplayName("getActiveOccupancyByRoomInstanceId - Trả về null khi id null")
    void getActiveOccupancyByRoomInstanceId_NullId_ReturnsNull() {
        assertThat(bookingLookupService.getActiveOccupancyByRoomInstanceId(null)).isNull();
        verifyNoInteractions(bookingRoomRepository);
    }

    @Test
    @DisplayName("getDailyRoomRevenue - Tính tổng doanh thu tiền phòng thành công")
    void getDailyRoomRevenue_Success() {
        vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate rate1 =
                vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate.builder()
                        .netPrice(java.math.BigDecimal.valueOf(1500000))
                        .build();
        vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate rate2 =
                vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate.builder()
                        .netPrice(java.math.BigDecimal.valueOf(2500000))
                        .build();

        java.time.LocalDate auditDate = java.time.LocalDate.of(2026, 6, 1);
        when(bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate((short) 1, auditDate))
                .thenReturn(java.util.List.of(rate1, rate2));

        java.math.BigDecimal total = bookingLookupService.getDailyRoomRevenue((short) 1, auditDate);

        assertThat(total).isEqualByComparingTo(java.math.BigDecimal.valueOf(4000000));
        verify(bookingDailyRateRepository).findActiveDailyRatesForHotelAndDate((short) 1, auditDate);
    }

    @Test
    @DisplayName("getDailyRoomRevenue - Trả về 0 khi hotelId hoặc auditDate null")
    void getDailyRoomRevenue_NullArgs_ReturnsZero() {
        assertThat(bookingLookupService.getDailyRoomRevenue(null, java.time.LocalDate.now())).isEqualByComparingTo(java.math.BigDecimal.ZERO);
        assertThat(bookingLookupService.getDailyRoomRevenue((short) 1, null)).isEqualByComparingTo(java.math.BigDecimal.ZERO);
        verifyNoInteractions(bookingDailyRateRepository);
    }
}
