package vn.edu.utc.hotel_booking.modules.identity.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestStayHistoryResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.mapper.GuestMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.BookingGuestRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.GuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock
    private BookingGuestRepository guestRepository;

    @Mock
    private BookingLookupService bookingLookupService;

    @Mock
    private GuestMapper guestMapper;

    @InjectMocks
    private GuestServiceImpl guestService;

    @Test
    @DisplayName("Lọc danh sách khách hàng phân trang thành công")
    void filter_Success() {
        GuestSearchDto searchDto = new GuestSearchDto();
        searchDto.setKeyword("0987");
        searchDto.setPage(0);
        searchDto.setPageSize(10);

        BookingGuest guest = BookingGuest.builder().id(101L).fullName("Nguyễn Văn A").phone("0987654321").build();
        GuestResponse response = GuestResponse.builder().id(101L).fullName("Nguyễn Văn A").phone("0987654321").build();

        Page<BookingGuest> page = new PageImpl<>(List.of(guest));
        when(guestRepository.searchGuests(eq("0987"), isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(guestMapper.toResponse(guest)).thenReturn(response);

        PageResponse<GuestResponse> result = guestService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    @DisplayName("Lấy chi tiết khách hàng theo ID thành công")
    void getById_Success() {
        BookingGuest guest = BookingGuest.builder().id(101L).fullName("Nguyễn Văn A").build();
        GuestResponse response = GuestResponse.builder().id(101L).fullName("Nguyễn Văn A").build();

        when(guestRepository.findByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(guest));
        when(guestMapper.toResponse(guest)).thenReturn(response);

        GuestResponse result = guestService.getById(101L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Lấy chi tiết khách hàng thất bại khi ID không tồn tại")
    void getById_NotFound() {
        when(guestRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestService.getById(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("Tìm nhanh khách hàng theo số CCCD/Passport thành công")
    void getByIdentityNumber_Success() {
        BookingGuest guest = BookingGuest.builder().id(101L).fullName("Nguyễn Văn A").identityNumber("001200000001").build();
        GuestResponse response = GuestResponse.builder().id(101L).fullName("Nguyễn Văn A").identityNumber("001200000001").build();

        when(guestRepository.findByIdentityNumberAndIsDeletedFalse("001200000001")).thenReturn(Optional.of(guest));
        when(guestMapper.toResponse(guest)).thenReturn(response);

        GuestResponse result = guestService.getByIdentityNumber("001200000001");

        assertThat(result).isNotNull();
        assertThat(result.getIdentityNumber()).isEqualTo("001200000001");
    }

    @Test
    @DisplayName("Tìm nhanh khách hàng theo CCCD thất bại khi không tìm thấy")
    void getByIdentityNumber_NotFound() {
        when(guestRepository.findByIdentityNumberAndIsDeletedFalse("001299999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestService.getByIdentityNumber("001299999999"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("Lấy lịch sử lưu trú và tính tổng chi tiêu của khách hàng (CRM Stay History)")
    void getStayHistory_Success() {
        BookingGuest guest = BookingGuest.builder().id(101L).fullName("Nguyễn Văn A").phone("0987654321").build();
        Hotel hotel = Hotel.builder().name("Khách sạn UTC Hà Nội").build();

        Booking booking1 = Booking.builder()
                .id(1L)
                .bookingNumber("BK001")
                .hotel(hotel)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("3000000.00"))
                .createdAt(OffsetDateTime.now())
                .build();

        Booking booking2 = Booking.builder()
                .id(2L)
                .bookingNumber("BK002")
                .hotel(hotel)
                .status(BookingStatus.CANCELLED)
                .totalAmount(new BigDecimal("1500000.00"))
                .createdAt(OffsetDateTime.now())
                .build();

        Booking booking3 = Booking.builder()
                .id(3L)
                .bookingNumber("BK003")
                .hotel(hotel)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("2000000.00"))
                .createdAt(OffsetDateTime.now())
                .build();

        when(guestRepository.findByIdAndIsDeletedFalse(101L)).thenReturn(Optional.of(guest));
        when(bookingLookupService.getBookingsByGuestId(101L)).thenReturn(List.of(booking1, booking2, booking3));

        GuestStayHistoryResponse history = guestService.getStayHistory(101L);

        assertThat(history).isNotNull();
        assertThat(history.getGuestId()).isEqualTo(101L);
        assertThat(history.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(history.getTotalStays()).isEqualTo(3);
        // Tổng tiền không tính booking2 (CANCELLED) => 3,000,000 + 2,000,000 = 5,000,000
        assertThat(history.getTotalSpent()).isEqualByComparingTo("5000000.00");
        assertThat(history.getBookings()).hasSize(3);
    }

    @Test
    @DisplayName("Tạo mới hồ sơ khách hàng thành công")
    void create_Success() {
        GuestCreateRequest request = new GuestCreateRequest();
        request.setFullName("Trần Thị B");
        request.setPhone("0912345678");

        BookingGuest guest = BookingGuest.builder().fullName("Trần Thị B").phone("0912345678").build();
        BookingGuest saved = BookingGuest.builder().id(102L).fullName("Trần Thị B").phone("0912345678").build();
        GuestResponse response = GuestResponse.builder().id(102L).fullName("Trần Thị B").phone("0912345678").build();

        when(guestMapper.toEntity(request)).thenReturn(guest);
        when(guestRepository.save(guest)).thenReturn(saved);
        when(guestMapper.toResponse(saved)).thenReturn(response);

        GuestResponse result = guestService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(102L);
        verify(guestRepository, times(1)).save(guest);
    }

    @Test
    @DisplayName("Xóa mềm danh sách khách hàng thành công")
    void deleteBatch_Success() {
        List<Long> ids = List.of(101L, 102L);
        when(guestRepository.softDeleteBatch(ids)).thenReturn(2);

        guestService.deleteBatch(ids);

        verify(guestRepository, times(1)).softDeleteBatch(ids);
    }
}
