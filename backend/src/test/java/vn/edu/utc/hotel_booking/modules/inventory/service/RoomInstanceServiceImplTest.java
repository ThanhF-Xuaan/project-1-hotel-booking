package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomInstanceMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomInstanceServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomInstanceServiceImplTest {

    @Mock RoomInstanceRepository roomInstanceRepository;
    @Mock HotelRepository hotelRepository;
    @Mock HotelRoomTypeRepository hotelRoomTypeRepository;
    @Mock RoomInstanceMapper roomInstanceMapper;

    @InjectMocks
    RoomInstanceServiceImpl roomInstanceService;

    RoomInstance sampleRoom;

    @BeforeEach
    void setUp() {
        sampleRoom = RoomInstance.builder()
                .id(101)
                .roomNumber("101")
                .currentStatus("DIRTY")
                .build();
    }

    @Test
    @DisplayName("getRoomsByHotelAndStatus - Trả về danh sách phòng theo hotelId và status")
    void getRoomsByHotelAndStatus_Success() {
        when(roomInstanceRepository.findByHotelIdAndCurrentStatus((short) 1, "DIRTY"))
                .thenReturn(List.of(sampleRoom));

        List<RoomInstance> rooms = roomInstanceService.getRoomsByHotelAndStatus((short) 1, "DIRTY");

        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getRoomNumber()).isEqualTo("101");
        verify(roomInstanceRepository).findByHotelIdAndCurrentStatus((short) 1, "DIRTY");
    }

    @Test
    @DisplayName("getRoomsByHotelAndStatus - Trả về rỗng khi hotelId null")
    void getRoomsByHotelAndStatus_NullHotelId_ReturnsEmpty() {
        List<RoomInstance> rooms = roomInstanceService.getRoomsByHotelAndStatus(null, "DIRTY");
        assertThat(rooms).isEmpty();
        verifyNoInteractions(roomInstanceRepository);
    }

    @Test
    @DisplayName("updateRoomStatusEntity - Cập nhật trạng thái thành công")
    void updateRoomStatusEntity_Success() {
        when(roomInstanceRepository.findByIdWithDetails(101)).thenReturn(Optional.of(sampleRoom));
        when(roomInstanceRepository.save(sampleRoom)).thenReturn(sampleRoom);

        RoomInstance updated = roomInstanceService.updateRoomStatusEntity(101, "CLEANING");

        assertThat(updated).isNotNull();
        assertThat(updated.getCurrentStatus()).isEqualTo("CLEANING");
        verify(roomInstanceRepository).save(sampleRoom);
    }

    @Test
    @DisplayName("updateRoomStatusEntity - Báo lỗi khi phòng không tồn tại")
    void updateRoomStatusEntity_NotFound_ThrowsException() {
        when(roomInstanceRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomInstanceService.updateRoomStatusEntity(999, "CLEANING"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_INSTANCE_NOT_FOUND));
    }

    @Test
    @DisplayName("countOccupiedRooms - Đếm số lượng phòng OCCUPIED thành công")
    void countOccupiedRooms_Success() {
        when(roomInstanceRepository.countOccupiedRoomsByHotelId((short) 1)).thenReturn(15L);

        int count = roomInstanceService.countOccupiedRooms((short) 1);

        assertThat(count).isEqualTo(15);
        verify(roomInstanceRepository).countOccupiedRoomsByHotelId((short) 1);
    }

    @Test
    @DisplayName("countOccupiedRooms - Trả về 0 khi hotelId là null")
    void countOccupiedRooms_NullHotelId_ReturnsZero() {
        int count = roomInstanceService.countOccupiedRooms(null);

        assertThat(count).isEqualTo(0);
        verifyNoInteractions(roomInstanceRepository);
    }
}
