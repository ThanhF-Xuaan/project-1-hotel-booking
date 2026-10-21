package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomSlotServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomSlotServiceImplTest {

    @Mock
    RoomSlotRepository roomSlotRepository;

    @InjectMocks
    RoomSlotServiceImpl roomSlotService;

    RoomInstance sampleRoom;

    @BeforeEach
    void setUp() {
        sampleRoom = RoomInstance.builder()
                .id(101)
                .roomNumber("101")
                .build();
    }

    @Test
    @DisplayName("createOrReserveSlots - Tạo hoặc cập nhật slot sang RESERVED")
    void createOrReserveSlots_Success() {
        LocalDate checkIn = LocalDate.of(2026, 6, 1);
        LocalDate checkOut = LocalDate.of(2026, 6, 3);
        Long bookingRoomId = 55L;

        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(eq(101), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        roomSlotService.createOrReserveSlots(sampleRoom, checkIn, checkOut, bookingRoomId);

        // 2 đêm: 2026-06-01 và 2026-06-02
        verify(roomSlotRepository, times(2)).save(any(RoomSlot.class));
    }

    @Test
    @DisplayName("releaseSlots - Đặt trạng thái READY và xóa bookingRoomId")
    void releaseSlots_Success() {
        LocalDate checkIn = LocalDate.of(2026, 6, 1);
        LocalDate checkOut = LocalDate.of(2026, 6, 2);

        RoomSlot existingSlot = RoomSlot.builder()
                .id(1L)
                .roomInstance(sampleRoom)
                .slotDate(checkIn)
                .status("RESERVED")
                .bookingRoomId(55L)
                .build();

        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(101, checkIn))
                .thenReturn(Optional.of(existingSlot));

        roomSlotService.releaseSlots(101, checkIn, checkOut);

        assertThat(existingSlot.getStatus()).isEqualTo("READY");
        assertThat(existingSlot.getBookingRoomId()).isNull();
        verify(roomSlotRepository, times(1)).save(existingSlot);
    }

    @Test
    @DisplayName("occupySlots - Chuyển trạng thái sang OCCUPIED khi check-in")
    void occupySlots_Success() {
        LocalDate checkIn = LocalDate.of(2026, 6, 1);
        LocalDate checkOut = LocalDate.of(2026, 6, 2);

        RoomSlot existingSlot = RoomSlot.builder()
                .id(1L)
                .roomInstance(sampleRoom)
                .slotDate(checkIn)
                .status("RESERVED")
                .bookingRoomId(55L)
                .build();

        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(101, checkIn))
                .thenReturn(Optional.of(existingSlot));

        roomSlotService.occupySlots(101, checkIn, checkOut);

        assertThat(existingSlot.getStatus()).isEqualTo("OCCUPIED");
        verify(roomSlotRepository, times(1)).save(existingSlot);
    }

    @Test
    @DisplayName("markSlotsDirty - Chuyển trạng thái sang DIRTY khi check-out")
    void markSlotsDirty_Success() {
        LocalDate checkIn = LocalDate.of(2026, 6, 1);
        LocalDate checkOut = LocalDate.of(2026, 6, 2);

        RoomSlot existingSlot = RoomSlot.builder()
                .id(1L)
                .roomInstance(sampleRoom)
                .slotDate(checkIn)
                .status("OCCUPIED")
                .build();

        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(101, checkIn))
                .thenReturn(Optional.of(existingSlot));

        roomSlotService.markSlotsDirty(101, checkIn, checkOut);

        assertThat(existingSlot.getStatus()).isEqualTo("DIRTY");
        verify(roomSlotRepository, times(1)).save(existingSlot);
    }

    @Test
    @DisplayName("updateTodaySlotStatus - Cập nhật trạng thái slot ngày hôm nay nếu tồn tại")
    void updateTodaySlotStatus_SlotExists_UpdatesStatus() {
        LocalDate today = LocalDate.now();
        RoomSlot slot = RoomSlot.builder()
                .id(1L)
                .roomInstance(sampleRoom)
                .slotDate(today)
                .status("READY")
                .build();

        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(101, today))
                .thenReturn(Optional.of(slot));

        roomSlotService.updateTodaySlotStatus(101, "CLEANING");

        assertThat(slot.getStatus()).isEqualTo("CLEANING");
        verify(roomSlotRepository, times(1)).save(slot);
    }

    @Test
    @DisplayName("updateTodaySlotStatus - Không làm gì khi tham số null")
    void updateTodaySlotStatus_NullArgs_NoOp() {
        roomSlotService.updateTodaySlotStatus(null, "CLEANING");
        roomSlotService.updateTodaySlotStatus(101, null);
        verifyNoInteractions(roomSlotRepository);
    }
}
