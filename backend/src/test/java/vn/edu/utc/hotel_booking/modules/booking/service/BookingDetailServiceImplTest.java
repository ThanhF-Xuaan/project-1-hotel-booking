package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDetailRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingDetailServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingDetailServiceImplTest {

    @Mock
    BookingDetailRepository bookingDetailRepository;

    @InjectMocks
    BookingDetailServiceImpl bookingDetailService;

    @Test
    @DisplayName("getEntityById - Thành công khi ID hợp lệ và tồn tại")
    void getEntityById_Success() {
        BookingDetail detail = BookingDetail.builder().id(10L).build();
        when(bookingDetailRepository.findById(10L)).thenReturn(Optional.of(detail));

        BookingDetail result = bookingDetailService.getEntityById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(bookingDetailRepository).findById(10L);
    }

    @Test
    @DisplayName("getEntityById - Ném INVALID_REQUEST_DATA khi ID là null")
    void getEntityById_NullId_ThrowsException() {
        assertThatThrownBy(() -> bookingDetailService.getEntityById(null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA));
    }

    @Test
    @DisplayName("getEntityById - Ném BOOKING_DETAIL_NOT_FOUND khi không tìm thấy")
    void getEntityById_NotFound_ThrowsException() {
        when(bookingDetailRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingDetailService.getEntityById(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.BOOKING_DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("findByBookingId - Trả về danh sách khi bookingId hợp lệ")
    void findByBookingId_Success() {
        BookingDetail detail = BookingDetail.builder().id(10L).build();
        when(bookingDetailRepository.findByBookingId(1L)).thenReturn(List.of(detail));

        List<BookingDetail> result = bookingDetailService.findByBookingId(1L);

        assertThat(result).hasSize(1);
        verify(bookingDetailRepository).findByBookingId(1L);
    }

    @Test
    @DisplayName("findByBookingId - Trả về danh sách rỗng khi bookingId là null")
    void findByBookingId_NullId_ReturnsEmpty() {
        List<BookingDetail> result = bookingDetailService.findByBookingId(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(bookingDetailRepository);
    }

    @Test
    @DisplayName("existsByCancellationPolicyId - Trả về true khi có bản ghi tham chiếu")
    void existsByCancellationPolicyId_Success() {
        when(bookingDetailRepository.existsByCancellationPolicyId(5)).thenReturn(true);

        boolean exists = bookingDetailService.existsByCancellationPolicyId(5);

        assertThat(exists).isTrue();
        verify(bookingDetailRepository).existsByCancellationPolicyId(5);
    }

    @Test
    @DisplayName("existsByCancellationPolicyId - Trả về false khi id là null")
    void existsByCancellationPolicyId_NullId_ReturnsFalse() {
        boolean exists = bookingDetailService.existsByCancellationPolicyId(null);

        assertThat(exists).isFalse();
        verifyNoInteractions(bookingDetailRepository);
    }

    @Test
    @DisplayName("existsByNoShowPolicyId - Trả về true khi có bản ghi tham chiếu")
    void existsByNoShowPolicyId_Success() {
        when(bookingDetailRepository.existsByNoShowPolicyId(6)).thenReturn(true);

        boolean exists = bookingDetailService.existsByNoShowPolicyId(6);

        assertThat(exists).isTrue();
        verify(bookingDetailRepository).existsByNoShowPolicyId(6);
    }

    @Test
    @DisplayName("existsByNoShowPolicyId - Trả về false khi id là null")
    void existsByNoShowPolicyId_NullId_ReturnsFalse() {
        boolean exists = bookingDetailService.existsByNoShowPolicyId(null);

        assertThat(exists).isFalse();
        verifyNoInteractions(bookingDetailRepository);
    }
}
