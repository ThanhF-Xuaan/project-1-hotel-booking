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
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.RoomStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.RoomHousekeepingStatusResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.HousekeepingServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HousekeepingServiceTest {

    @Mock RoomInstanceRepository roomInstanceRepository;
    @Mock RoomSlotRepository roomSlotRepository;
    @Mock BookingRoomRepository bookingRoomRepository;

    @InjectMocks
    HousekeepingServiceImpl housekeepingService;

    private Hotel testHotel;
    private RoomInstance testRoom;
    private RoomSlot testSlot;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        testHotel.setId((short) 1);

        RoomType roomType = RoomType.builder().code("STD").name("Standard").build();
        roomType.setId((short) 1);

        HotelRoomType hrt = HotelRoomType.builder().roomType(roomType).build();
        hrt.setId(10);

        testRoom = RoomInstance.builder()
                .hotel(testHotel)
                .hotelRoomType(hrt)
                .roomNumber("P.101")
                .currentStatus("DIRTY")
                .build();
        testRoom.setId(5);

        testSlot = RoomSlot.builder()
                .roomInstance(testRoom)
                .slotDate(LocalDate.now())
                .status("DIRTY")
                .build();
    }

    @Test
    @DisplayName("Cập nhật trạng thái phòng vật lý thành CLEANING và đồng bộ RoomSlot")
    void updateRoomStatus_Success() {
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("CLEANING")
                .build();

        when(roomInstanceRepository.findByIdWithDetails(5)).thenReturn(Optional.of(testRoom));
        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(eq(5), any(LocalDate.class)))
                .thenReturn(Optional.of(testSlot));
        when(bookingRoomRepository.findByRoomInstanceIdAndStatusIn(eq(5), anyList()))
                .thenReturn(List.of());

        RoomHousekeepingStatusResponse response = housekeepingService.updateRoomStatus(5, request);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentStatus()).isEqualTo("CLEANING");
        assertThat(testRoom.getCurrentStatus()).isEqualTo("CLEANING");
        assertThat(testSlot.getStatus()).isEqualTo("CLEANING");

        verify(roomInstanceRepository, times(1)).save(testRoom);
        verify(roomSlotRepository, times(1)).save(testSlot);
    }

    @Test
    @DisplayName("Báo lỗi INVALID_REQUEST_DATA khi truyền trạng thái phòng không hợp lệ")
    void updateRoomStatus_InvalidStatus_ThrowsException() {
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("UNKNOWN_STATUS")
                .build();

        when(roomInstanceRepository.findByIdWithDetails(5)).thenReturn(Optional.of(testRoom));

        assertThatThrownBy(() -> housekeepingService.updateRoomStatus(5, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA));
    }

    @Test
    @DisplayName("Lấy danh sách trạng thái buồng phòng theo khách sạn thành công")
    void getRoomsByHotel_Success() {
        when(roomInstanceRepository.findAll()).thenReturn(List.of(testRoom));
        when(bookingRoomRepository.findByRoomInstanceIdAndStatusIn(eq(5), anyList())).thenReturn(List.of());

        List<RoomHousekeepingStatusResponse> list = housekeepingService.getRoomsByHotel((short) 1, null);

        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getRoomNumber()).isEqualTo("P.101");
        assertThat(list.get(0).getCurrentStatus()).isEqualTo("DIRTY");
    }
}
