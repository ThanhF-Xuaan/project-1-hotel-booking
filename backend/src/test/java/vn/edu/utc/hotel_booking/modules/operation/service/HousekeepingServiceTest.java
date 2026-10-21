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
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomOccupancyDto;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.RoomStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.RoomHousekeepingStatusResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.HousekeepingServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HousekeepingServiceTest {

    @Mock RoomInstanceService roomInstanceService;
    @Mock RoomSlotService roomSlotService;
    @Mock BookingLookupService bookingLookupService;

    @InjectMocks
    HousekeepingServiceImpl housekeepingService;

    private Hotel testHotel;
    private RoomInstance testRoom;

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
    }

    @Test
    @DisplayName("Cập nhật trạng thái phòng vật lý thành CLEANING và đồng bộ RoomSlot qua Service")
    void updateRoomStatus_Success() {
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("CLEANING")
                .build();

        testRoom.setCurrentStatus("CLEANING");
        when(roomInstanceService.updateRoomStatusEntity(5, "CLEANING")).thenReturn(testRoom);

        RoomHousekeepingStatusResponse response = housekeepingService.updateRoomStatus(5, request);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentStatus()).isEqualTo("CLEANING");
        assertThat(testRoom.getCurrentStatus()).isEqualTo("CLEANING");

        verify(roomInstanceService, times(1)).updateRoomStatusEntity(5, "CLEANING");
        verify(roomSlotService, times(1)).updateTodaySlotStatus(5, "CLEANING");
    }

    @Test
    @DisplayName("Cập nhật trạng thái thành OCCUPIED sẽ tra cứu RoomOccupancyDto qua BookingLookupService")
    void updateRoomStatus_Occupied_IncludesOccupantInfo() {
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("OCCUPIED")
                .build();

        testRoom.setCurrentStatus("OCCUPIED");
        when(roomInstanceService.updateRoomStatusEntity(5, "OCCUPIED")).thenReturn(testRoom);
        when(bookingLookupService.getActiveOccupancyByRoomInstanceId(5)).thenReturn(RoomOccupancyDto.builder()
                .roomInstanceId(5)
                .bookingNumber("BK12345678")
                .guestName("Nguyễn Văn A")
                .checkOutDate(LocalDate.of(2026, 6, 10))
                .build());

        RoomHousekeepingStatusResponse response = housekeepingService.updateRoomStatus(5, request);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentStatus()).isEqualTo("OCCUPIED");
        assertThat(response.getBookingNumber()).isEqualTo("BK12345678");
        assertThat(response.getGuestName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 6, 10));

        verify(bookingLookupService, times(1)).getActiveOccupancyByRoomInstanceId(5);
    }

    @Test
    @DisplayName("Báo lỗi INVALID_REQUEST_DATA khi truyền trạng thái phòng không hợp lệ")
    void updateRoomStatus_InvalidStatus_ThrowsException() {
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("UNKNOWN_STATUS")
                .build();

        assertThatThrownBy(() -> housekeepingService.updateRoomStatus(5, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA));

        verifyNoInteractions(roomInstanceService);
        verifyNoInteractions(roomSlotService);
    }

    @Test
    @DisplayName("Lấy danh sách trạng thái buồng phòng theo khách sạn thành công (không có phòng OCCUPIED)")
    void getRoomsByHotel_Success() {
        when(roomInstanceService.getRoomsByHotelAndStatus((short) 1, null)).thenReturn(List.of(testRoom));
        when(bookingLookupService.getActiveOccupanciesByRoomInstanceIds(anyList())).thenReturn(Map.of());

        List<RoomHousekeepingStatusResponse> list = housekeepingService.getRoomsByHotel((short) 1, null);

        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getRoomNumber()).isEqualTo("P.101");
        assertThat(list.get(0).getCurrentStatus()).isEqualTo("DIRTY");
        verify(roomInstanceService, times(1)).getRoomsByHotelAndStatus((short) 1, null);
    }

    @Test
    @DisplayName("Lấy danh sách buồng phòng có phòng OCCUPIED: batch lookup thông tin khách chống N+1")
    void getRoomsByHotel_WithOccupiedRoom_BatchLookupsSuccessfully() {
        testRoom.setCurrentStatus("OCCUPIED");
        when(roomInstanceService.getRoomsByHotelAndStatus((short) 1, "OCCUPIED")).thenReturn(List.of(testRoom));
        when(bookingLookupService.getActiveOccupanciesByRoomInstanceIds(List.of(5))).thenReturn(Map.of(
                5, RoomOccupancyDto.builder()
                        .roomInstanceId(5)
                        .bookingNumber("BK888888")
                        .guestName("Trần Thị B")
                        .checkOutDate(LocalDate.of(2026, 6, 15))
                        .build()
        ));

        List<RoomHousekeepingStatusResponse> list = housekeepingService.getRoomsByHotel((short) 1, "OCCUPIED");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getBookingNumber()).isEqualTo("BK888888");
        assertThat(list.get(0).getGuestName()).isEqualTo("Trần Thị B");
        assertThat(list.get(0).getCheckOutDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        verify(bookingLookupService, times(1)).getActiveOccupanciesByRoomInstanceIds(List.of(5));
    }
}
