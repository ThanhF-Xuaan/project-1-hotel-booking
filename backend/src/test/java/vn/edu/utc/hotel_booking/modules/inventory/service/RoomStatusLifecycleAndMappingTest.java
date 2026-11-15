package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstanceStatus;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomMaintenanceBlock;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlotStatus;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomMaintenanceBlockRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomMaintenanceBlockServiceImpl;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomStatusLifecycleAndMappingTest {

    @Mock
    RoomMaintenanceBlockRepository repository;

    @Mock
    RoomInstanceRepository roomInstanceRepository;

    @InjectMocks
    RoomMaintenanceBlockServiceImpl maintenanceBlockService;

    @Test
    @DisplayName("RoomInstanceStatus: Kiểm tra tập giá trị hợp lệ")
    void testRoomInstanceStatus_Validation() {
        assertThat(RoomInstanceStatus.isValid("READY")).isTrue();
        assertThat(RoomInstanceStatus.isValid("occupied")).isTrue();
        assertThat(RoomInstanceStatus.isValid("DIRTY")).isTrue();
        assertThat(RoomInstanceStatus.isValid("cleaning")).isTrue();
        assertThat(RoomInstanceStatus.isValid("MAINTENANCE")).isTrue();

        assertThat(RoomInstanceStatus.isValid("UNKNOWN")).isFalse();
        assertThat(RoomInstanceStatus.isValid(null)).isFalse();
        assertThat(RoomInstanceStatus.isValid("")).isFalse();
    }

    @Test
    @DisplayName("RoomSlotStatus: isBusy nhận diện đúng các trạng thái bận theo Sparse Data")
    void testRoomSlotStatus_IsBusy() {
        // Trạng thái bận
        assertThat(RoomSlotStatus.isBusy("RESERVED")).isTrue();
        assertThat(RoomSlotStatus.isBusy("OCCUPIED")).isTrue();
        assertThat(RoomSlotStatus.isBusy("BLOCKED")).isTrue();
        assertThat(RoomSlotStatus.isBusy("MAINTENANCE")).isTrue();
        assertThat(RoomSlotStatus.isBusy("DIRTY")).isTrue();
        assertThat(RoomSlotStatus.isBusy("CLEANING")).isTrue();

        // Trạng thái rảnh
        assertThat(RoomSlotStatus.isBusy("READY")).isFalse();
        assertThat(RoomSlotStatus.isBusy(null)).isFalse();
        assertThat(RoomSlotStatus.isBusy("")).isFalse();
    }

    @Test
    @DisplayName("activateBlocksStartingToday: Cập nhật phòng sang MAINTENANCE nếu có lệnh bảo trì hôm nay")
    void testActivateBlocksStartingToday_UpdatesRoomToMaintenance() {
        LocalDate today = LocalDate.now();
        RoomInstance room = RoomInstance.builder().id(101).roomNumber("101A").currentStatus("READY").build();
        RoomMaintenanceBlock block = RoomMaintenanceBlock.builder()
                .id(1L)
                .roomInstance(room)
                .startDate(today)
                .endDate(today.plusDays(3))
                .status("ACTIVE")
                .build();

        when(repository.findActiveBlocksCoveringDate(today)).thenReturn(List.of(block));

        int activated = maintenanceBlockService.activateBlocksStartingToday();

        assertThat(activated).isEqualTo(1);
        assertThat(room.getCurrentStatus()).isEqualTo("MAINTENANCE");
        verify(roomInstanceRepository, times(1)).save(room);
    }

    @Test
    @DisplayName("activateBlocksStartingToday: Bỏ qua nếu phòng đã ở trạng thái MAINTENANCE")
    void testActivateBlocksStartingToday_SkipsAlreadyMaintenance() {
        LocalDate today = LocalDate.now();
        RoomInstance room = RoomInstance.builder().id(102).roomNumber("102A").currentStatus("MAINTENANCE").build();
        RoomMaintenanceBlock block = RoomMaintenanceBlock.builder()
                .id(2L)
                .roomInstance(room)
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(2))
                .status("ACTIVE")
                .build();

        when(repository.findActiveBlocksCoveringDate(today)).thenReturn(List.of(block));

        int activated = maintenanceBlockService.activateBlocksStartingToday();

        assertThat(activated).isEqualTo(0);
        verify(roomInstanceRepository, never()).save(any());
    }
}

