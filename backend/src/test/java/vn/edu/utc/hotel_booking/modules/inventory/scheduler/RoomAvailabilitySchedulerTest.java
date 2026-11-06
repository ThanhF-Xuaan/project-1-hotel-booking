package vn.edu.utc.hotel_booking.modules.inventory.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilitySchedulerTest {

    @Mock
    RoomAvailabilityGenerationService generationService;

    @InjectMocks
    RoomAvailabilityScheduler scheduler;

    @Test
    @DisplayName("scheduleDailyRollingHorizon: Thành công gọi cuộn tồn kho 365 ngày cho toàn bộ hạng phòng ACTIVE")
    void scheduleDailyRollingHorizon_success() {
        when(generationService.rollHorizonForAllActiveRoomTypes(365)).thenReturn(100);

        scheduler.scheduleDailyRollingHorizon();

        verify(generationService, times(1)).rollHorizonForAllActiveRoomTypes(365);
    }

    @Test
    @DisplayName("scheduleDailyRollingHorizon: Xử lý ngoại lệ an toàn không để crash scheduler")
    void scheduleDailyRollingHorizon_exceptionHandled() {
        when(generationService.rollHorizonForAllActiveRoomTypes(365)).thenThrow(new RuntimeException("Database timeout"));

        scheduler.scheduleDailyRollingHorizon();

        verify(generationService, times(1)).rollHorizonForAllActiveRoomTypes(365);
    }
}
