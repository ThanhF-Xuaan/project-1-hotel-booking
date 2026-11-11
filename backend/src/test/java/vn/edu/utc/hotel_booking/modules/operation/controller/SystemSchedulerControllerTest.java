package vn.edu.utc.hotel_booking.modules.operation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.common.exception.GlobalHandlerException;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.SystemSchedulerSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SchedulerTriggerResult;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SystemSchedulerTaskResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.SystemSchedulerService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SystemSchedulerControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private SystemSchedulerService service;

    @InjectMocks
    private SystemSchedulerController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/operation/schedulers/filter: Trả về 200 OK và danh sách tác vụ")
    void testFilter_Success() throws Exception {
        SystemSchedulerTaskResponse task1 = SystemSchedulerTaskResponse.builder()
                .taskKey("ROOM_AVAILABILITY_ROLL")
                .taskName("Cuộn tồn kho 365 ngày")
                .hasDistributedLock(true)
                .allowManualTrigger(true)
                .build();

        when(service.getSchedulerTasks(any())).thenReturn(List.of(task1));

        SystemSchedulerSearchDto searchDto = SystemSchedulerSearchDto.builder().build();

        mockMvc.perform(post("/api/v1/operation/schedulers/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].taskKey").value("ROOM_AVAILABILITY_ROLL"))
                .andExpect(jsonPath("$.result[0].hasDistributedLock").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/operation/schedulers/{taskKey}/trigger: Kích hoạt tác vụ thành công")
    void testTrigger_Success() throws Exception {
        SchedulerTriggerResult result = SchedulerTriggerResult.builder()
                .taskKey("ROOM_AVAILABILITY_ROLL")
                .taskName("Cuộn tồn kho 365 ngày")
                .success(true)
                .message("Thành công")
                .processedCount(365)
                .build();

        when(service.triggerTask("ROOM_AVAILABILITY_ROLL")).thenReturn(result);

        mockMvc.perform(post("/api/v1/operation/schedulers/ROOM_AVAILABILITY_ROLL/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.taskKey").value("ROOM_AVAILABILITY_ROLL"))
                .andExpect(jsonPath("$.result.success").value(true));

        verify(service).triggerTask("ROOM_AVAILABILITY_ROLL");
    }
}

