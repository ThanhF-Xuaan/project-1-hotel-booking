package vn.edu.utc.hotel_booking.modules.booking.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupCutoffSchedulerTest {

    @Mock
    GroupRoomBlockService groupRoomBlockService;

    @InjectMocks
    GroupCutoffScheduler groupCutoffScheduler;

    @Test
    @DisplayName("executeCutoffAutoRelease invokes processCutoffAutoRelease")
    void testExecuteCutoffAutoRelease_Success() {
        when(groupRoomBlockService.processCutoffAutoRelease()).thenReturn(5);

        groupCutoffScheduler.executeCutoffAutoRelease();

        verify(groupRoomBlockService, times(1)).processCutoffAutoRelease();
    }

    @Test
    @DisplayName("executeCutoffAutoRelease handles exception safely without propagating")
    void testExecuteCutoffAutoRelease_HandlesExceptionSafely() {
        when(groupRoomBlockService.processCutoffAutoRelease()).thenThrow(new RuntimeException("DB Connection Timeout"));

        // Should not throw exception
        groupCutoffScheduler.executeCutoffAutoRelease();

        verify(groupRoomBlockService, times(1)).processCutoffAutoRelease();
    }

    @Test
    @DisplayName("executeDepositExpiredAutoCancel invokes processDepositExpiredAutoCancel")
    void testExecuteDepositExpiredAutoCancel_Success() {
        when(groupRoomBlockService.processDepositExpiredAutoCancel()).thenReturn(2);

        groupCutoffScheduler.executeDepositExpiredAutoCancel();

        verify(groupRoomBlockService, times(1)).processDepositExpiredAutoCancel();
    }

    @Test
    @DisplayName("executeCutoffReminders invokes processCutoffReminders")
    void testExecuteCutoffReminders_Success() {
        when(groupRoomBlockService.processCutoffReminders()).thenReturn(3);

        groupCutoffScheduler.executeCutoffReminders();

        verify(groupRoomBlockService, times(1)).processCutoffReminders();
    }
}
