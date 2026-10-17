package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingRoomServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingRoomServiceImplTest {

    @Mock
    BookingRoomRepository bookingRoomRepository;

    @InjectMocks
    BookingRoomServiceImpl bookingRoomService;

    @Test
    @DisplayName("getEntityById - Thành công khi ID tồn tại")
    void getEntityById_Success() {
        BookingRoom room = BookingRoom.builder().id(10L).build();
        when(bookingRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        BookingRoom result = bookingRoomService.getEntityById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(bookingRoomRepository).findById(10L);
    }

    @Test
    @DisplayName("getEntityById - Ném BOOKING_ROOM_NOT_FOUND khi ID không tồn tại")
    void getEntityById_NotFound_ThrowsException() {
        when(bookingRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingRoomService.getEntityById(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.BOOKING_ROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("save - Lưu thông tin thành công")
    void save_Success() {
        BookingRoom room = BookingRoom.builder().id(10L).build();
        when(bookingRoomRepository.save(room)).thenReturn(room);

        BookingRoom result = bookingRoomService.save(room);

        assertThat(result).isNotNull();
        verify(bookingRoomRepository).save(room);
    }

    @Test
    @DisplayName("findOverlappingAssignedRooms - Trả về danh sách phòng trùng lịch")
    void findOverlappingAssignedRooms_Success() {
        LocalDate checkIn = LocalDate.of(2026, 6, 1);
        LocalDate checkOut = LocalDate.of(2026, 6, 5);
        BookingRoom room = BookingRoom.builder().id(10L).build();

        when(bookingRoomRepository.findOverlappingAssignedRooms(101, checkIn, checkOut))
                .thenReturn(List.of(room));

        List<BookingRoom> result = bookingRoomService.findOverlappingAssignedRooms(101, checkIn, checkOut);

        assertThat(result).hasSize(1);
        verify(bookingRoomRepository).findOverlappingAssignedRooms(101, checkIn, checkOut);
    }

    @Test
    @DisplayName("findByBookingId - Trả về danh sách phòng theo bookingId qua hierarchy booking_details")
    void findByBookingId_Success() {
        BookingRoom room = BookingRoom.builder().id(10L).build();
        when(bookingRoomRepository.findByBookingId(1L)).thenReturn(List.of(room));

        List<BookingRoom> result = bookingRoomService.findByBookingId(1L);

        assertThat(result).hasSize(1);
        verify(bookingRoomRepository).findByBookingId(1L);
    }

    @Test
    @DisplayName("findByBookingId - Trả về danh sách rỗng khi bookingId null")
    void findByBookingId_NullId_ReturnsEmpty() {
        List<BookingRoom> result = bookingRoomService.findByBookingId(null);
        assertThat(result).isEmpty();
        verifyNoInteractions(bookingRoomRepository);
    }

    @Test
    @DisplayName("findByBookingDetailId - Trả về danh sách phòng theo bookingDetailId")
    void findByBookingDetailId_Success() {
        BookingRoom room = BookingRoom.builder().id(10L).build();
        when(bookingRoomRepository.findByBookingDetailId(20L)).thenReturn(List.of(room));

        List<BookingRoom> result = bookingRoomService.findByBookingDetailId(20L);

        assertThat(result).hasSize(1);
        verify(bookingRoomRepository).findByBookingDetailId(20L);
    }

    @Test
    @DisplayName("findByBookingDetailId - Trả về danh sách rỗng khi bookingDetailId null")
    void findByBookingDetailId_NullId_ReturnsEmpty() {
        List<BookingRoom> result = bookingRoomService.findByBookingDetailId(null);
        assertThat(result).isEmpty();
        verifyNoInteractions(bookingRoomRepository);
    }

    @Test
    @DisplayName("findByBookingIdAndRoomInstanceId - Tìm phòng thành công")
    void findByBookingIdAndRoomInstanceId_Success() {
        BookingRoom room = BookingRoom.builder().id(10L).build();
        when(bookingRoomRepository.findByBookingIdAndRoomInstanceId(1L, 101)).thenReturn(Optional.of(room));

        Optional<BookingRoom> result = bookingRoomService.findByBookingIdAndRoomInstanceId(1L, 101);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        verify(bookingRoomRepository).findByBookingIdAndRoomInstanceId(1L, 101);
    }

    @Test
    @DisplayName("findByRoomInstanceIdAndStatusIn - Trả về danh sách phòng theo trạng thái")
    void findByRoomInstanceIdAndStatusIn_Success() {
        BookingRoom room = BookingRoom.builder().id(10L).build();
        when(bookingRoomRepository.findByRoomInstanceIdAndStatusIn(101, List.of(vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus.CHECKED_IN)))
                .thenReturn(List.of(room));

        List<BookingRoom> result = bookingRoomService.findByRoomInstanceIdAndStatusIn(
                101,
                List.of(vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus.CHECKED_IN)
        );

        assertThat(result).hasSize(1);
        verify(bookingRoomRepository).findByRoomInstanceIdAndStatusIn(101, List.of(vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus.CHECKED_IN));
    }

    @Test
    @DisplayName("findByRoomInstanceIdAndStatusIn - Trả về rỗng khi input null hoặc rỗng")
    void findByRoomInstanceIdAndStatusIn_EmptyInput_ReturnsEmpty() {
        List<BookingRoom> resultNullRoom = bookingRoomService.findByRoomInstanceIdAndStatusIn(null, List.of(vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus.CHECKED_IN));
        List<BookingRoom> resultNullStatuses = bookingRoomService.findByRoomInstanceIdAndStatusIn(101, null);
        List<BookingRoom> resultEmptyStatuses = bookingRoomService.findByRoomInstanceIdAndStatusIn(101, List.of());

        assertThat(resultNullRoom).isEmpty();
        assertThat(resultNullStatuses).isEmpty();
        assertThat(resultEmptyStatuses).isEmpty();
        verifyNoInteractions(bookingRoomRepository);
    }
}
