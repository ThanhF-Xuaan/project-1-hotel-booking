package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;
import vn.edu.utc.hotel_booking.modules.identity.scheduler.DeadLetterScheduler;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.SystemSchedulerSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SchedulerTriggerResult;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SystemSchedulerTaskResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.SystemSchedulerServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSchedulerServiceTest {

    @Mock
    RoomAvailabilityGenerationService roomAvailabilityGenerationService;

    @Mock
    RoomAvailabilityService roomAvailabilityService;

    @Mock
    GroupRoomBlockService groupRoomBlockService;

    @Mock
    DeadLetterScheduler deadLetterScheduler;

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    SystemSchedulerServiceImpl service;

    @Test
    @DisplayName("getSchedulerTasks: Trả về đầy đủ 8 tác vụ và 100% đều có hasDistributedLock == true")
    void testGetSchedulerTasks_ReturnsAllTasksWithDistributedLock() {
        List<SystemSchedulerTaskResponse> tasks = service.getSchedulerTasks(null);

        assertThat(tasks).isNotNull();
        assertThat(tasks).hasSize(8);
        assertThat(tasks).allMatch(SystemSchedulerTaskResponse::isHasDistributedLock);

        long manualAllowed = tasks.stream().filter(SystemSchedulerTaskResponse::isAllowManualTrigger).count();
        long autoOnly = tasks.stream().filter(t -> !t.isAllowManualTrigger()).count();

        assertThat(manualAllowed).isEqualTo(6);
        assertThat(autoOnly).isEqualTo(2);
    }

    @Test
    @DisplayName("getSchedulerTasks: Lọc theo phân hệ INVENTORY")
    void testGetSchedulerTasks_FilterByCategory() {
        SystemSchedulerSearchDto searchDto = SystemSchedulerSearchDto.builder()
                .category("INVENTORY")
                .build();

        List<SystemSchedulerTaskResponse> tasks = service.getSchedulerTasks(searchDto);

        assertThat(tasks).hasSize(2);
        assertThat(tasks).allMatch(t -> "INVENTORY".equals(t.getCategory()));
    }

    @Test
    @DisplayName("triggerTask: Thành công khi kích hoạt tác vụ ROOM_AVAILABILITY_ROLL")
    void testTriggerTask_RoomAvailabilityRoll_Success() {
        when(roomAvailabilityGenerationService.rollHorizonForAllActiveRoomTypes(365)).thenReturn(365);

        SchedulerTriggerResult result = service.triggerTask("ROOM_AVAILABILITY_ROLL");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedCount()).isEqualTo(365);
        verify(roomAvailabilityGenerationService).rollHorizonForAllActiveRoomTypes(365);
    }

    @Test
    @DisplayName("triggerTask: Thành công khi kích hoạt tác vụ RELEASE_EXPIRED_HOLDS")
    void testTriggerTask_ReleaseExpiredHolds_Success() {
        when(roomAvailabilityService.releaseExpiredHolds()).thenReturn(5);

        SchedulerTriggerResult result = service.triggerTask("RELEASE_EXPIRED_HOLDS");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedCount()).isEqualTo(5);
        verify(roomAvailabilityService).releaseExpiredHolds();
    }

    @Test
    @DisplayName("triggerTask: Từ chối kích hoạt thủ công đối với tác vụ ASYNC_IAM_WORKER")
    void testTriggerTask_AsyncIamWorker_Rejected() {
        assertThatThrownBy(() -> service.triggerTask("ASYNC_IAM_WORKER"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Tác vụ này không cho phép kích hoạt thủ công")
                .hasMessageContaining("được kích hoạt tự động theo sự kiện thay đổi dữ liệu nhân sự");
    }

    @Test
    @DisplayName("triggerTask: Từ chối kích hoạt thủ công đối với tác vụ NIGHT_AUDIT_CLOSING")
    void testTriggerTask_NightAudit_Rejected() {
        assertThatThrownBy(() -> service.triggerTask("NIGHT_AUDIT_CLOSING"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Tác vụ này không cho phép kích hoạt thủ công")
                .hasMessageContaining("được kích hoạt tự động vào cuối ngày hoặc thực hiện qua phân hệ Đóng ngày chuyên biệt");
    }
}

