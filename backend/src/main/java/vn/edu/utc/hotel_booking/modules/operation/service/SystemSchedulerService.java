package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.modules.operation.dto.request.SystemSchedulerSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SchedulerTriggerResult;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SystemSchedulerTaskResponse;

import java.util.List;

public interface SystemSchedulerService {

    List<SystemSchedulerTaskResponse> getSchedulerTasks(SystemSchedulerSearchDto searchDto);

    SchedulerTriggerResult triggerTask(String taskKey);
}

