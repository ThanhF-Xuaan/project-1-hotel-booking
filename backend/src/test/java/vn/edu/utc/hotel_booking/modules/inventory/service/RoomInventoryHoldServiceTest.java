package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHold;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHoldStatus;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInventoryHoldRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomInventoryHoldServiceImpl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomInventoryHoldServiceTest {

    @Mock
    RoomInventoryHoldRepository roomInventoryHoldRepository;

    @InjectMocks
    RoomInventoryHoldServiceImpl roomInventoryHoldService;

    @Test
    @DisplayName("saveAll: Lưu danh sách hold thành công và xử lý danh sách rỗng")
    void testSaveAll() {
        RoomInventoryHold hold = RoomInventoryHold.builder()
                .sessionId("sess-1")
                .holdDate(LocalDate.now())
                .quantity(1)
                .status(RoomInventoryHoldStatus.HELD)
                .build();

        when(roomInventoryHoldRepository.saveAll(List.of(hold))).thenReturn(List.of(hold));

        List<RoomInventoryHold> saved = roomInventoryHoldService.saveAll(List.of(hold));
        assertThat(saved).hasSize(1);
        verify(roomInventoryHoldRepository, times(1)).saveAll(List.of(hold));

        List<RoomInventoryHold> emptySaved = roomInventoryHoldService.saveAll(Collections.emptyList());
        assertThat(emptySaved).isEmpty();
        verifyNoMoreInteractions(roomInventoryHoldRepository);
    }

    @Test
    @DisplayName("findActiveHoldsBySessionId: Tìm kiếm hold đang hoạt động theo sessionId hợp lệ")
    void testFindActiveHoldsBySessionId() {
        RoomInventoryHold hold = RoomInventoryHold.builder()
                .sessionId("sess-1")
                .holdDate(LocalDate.now())
                .quantity(2)
                .status(RoomInventoryHoldStatus.HELD)
                .build();

        when(roomInventoryHoldRepository.findBySessionIdAndStatusAndIsDeletedFalse("sess-1", RoomInventoryHoldStatus.HELD))
                .thenReturn(List.of(hold));

        List<RoomInventoryHold> results = roomInventoryHoldService.findActiveHoldsBySessionId("sess-1");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSessionId()).isEqualTo("sess-1");

        List<RoomInventoryHold> blankResult = roomInventoryHoldService.findActiveHoldsBySessionId("   ");
        assertThat(blankResult).isEmpty();
    }

    @Test
    @DisplayName("findExpiredHolds: Tìm danh sách hold quá hạn")
    void testFindExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        RoomInventoryHold expiredHold = RoomInventoryHold.builder()
                .sessionId("expired-sess")
                .holdDate(LocalDate.now())
                .status(RoomInventoryHoldStatus.HELD)
                .expiresAt(now.minusMinutes(1))
                .build();

        when(roomInventoryHoldRepository.findByStatusAndExpiresAtBeforeAndIsDeletedFalse(eq(RoomInventoryHoldStatus.HELD), eq(now)))
                .thenReturn(List.of(expiredHold));

        List<RoomInventoryHold> results = roomInventoryHoldService.findExpiredHolds(now);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSessionId()).isEqualTo("expired-sess");
    }

    @Test
    @DisplayName("markHoldsReleased: Cập nhật status sang RELEASED và gán releaseReason")
    void testMarkHoldsReleased() {
        RoomInventoryHold hold = RoomInventoryHold.builder()
                .sessionId("sess-rel")
                .status(RoomInventoryHoldStatus.HELD)
                .build();

        roomInventoryHoldService.markHoldsReleased(List.of(hold), HoldReleaseReason.SESSION_SUPERSEDED);

        assertThat(hold.getStatus()).isEqualTo(RoomInventoryHoldStatus.RELEASED);
        assertThat(hold.getReleaseReason()).isEqualTo(HoldReleaseReason.SESSION_SUPERSEDED);
        verify(roomInventoryHoldRepository, times(1)).saveAll(List.of(hold));
    }

    @Test
    @DisplayName("markHoldsCommitted: Cập nhật status sang COMMITTED")
    void testMarkHoldsCommitted() {
        RoomInventoryHold hold = RoomInventoryHold.builder()
                .sessionId("sess-commit")
                .status(RoomInventoryHoldStatus.HELD)
                .build();

        roomInventoryHoldService.markHoldsCommitted(List.of(hold));

        assertThat(hold.getStatus()).isEqualTo(RoomInventoryHoldStatus.COMMITTED);
        verify(roomInventoryHoldRepository, times(1)).saveAll(List.of(hold));
    }
}
