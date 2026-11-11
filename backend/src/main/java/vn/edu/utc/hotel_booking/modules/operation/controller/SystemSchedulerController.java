package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.SystemSchedulerSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SchedulerTriggerResult;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SystemSchedulerTaskResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.SystemSchedulerService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operation/schedulers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Operation - System Schedulers", description = "APIs quản lý và kích hoạt các tác vụ lập lịch và tiến trình nền")
public class SystemSchedulerController {

    SystemSchedulerService systemSchedulerService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Lấy danh sách các tác vụ lập lịch và trạng thái khóa phân tán ShedLock")
    public ResponseEntity<ApiResponse<List<SystemSchedulerTaskResponse>>> getTasks(
            @RequestBody(required = false) SystemSchedulerSearchDto searchDto) {
        List<SystemSchedulerTaskResponse> tasks = systemSchedulerService.getSchedulerTasks(searchDto);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping("/{taskKey}/trigger")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'PROPERTY_MANAGER')")
    @Operation(summary = "Kích hoạt thủ công một tác vụ lập lịch (nếu nghiệp vụ và pháp luật cho phép)")
    public ResponseEntity<ApiResponse<SchedulerTriggerResult>> triggerTask(
            @PathVariable String taskKey) {
        SchedulerTriggerResult result = systemSchedulerService.triggerTask(taskKey);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

