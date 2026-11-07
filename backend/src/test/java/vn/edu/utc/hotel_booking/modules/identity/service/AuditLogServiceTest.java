package vn.edu.utc.hotel_booking.modules.identity.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.AuditLogSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.AuditLogResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.AuditLog;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.mapper.AuditLogMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.AuditLogRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.AuditLogServiceImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("Lọc kiểm toán với actionType=STATUS_CHANGED và entityName=staff (chữ thường)")
    void filter_WithStatusChangedAndLowerCaseEntity_Success() {
        AuditLogSearchDto searchDto = AuditLogSearchDto.builder()
                .actionType("STATUS_CHANGED")
                .entityName("staff")
                .page(0)
                .pageSize(10)
                .build();

        AuditLog log = AuditLog.builder()
                .id(1L)
                .actionType("STATUS_CHANGED")
                .entityName("Staff")
                .entityId("1")
                .createdAt(OffsetDateTime.now())
                .build();

        AuditLogResponse response = AuditLogResponse.builder()
                .id(1L)
                .actionType("STATUS_CHANGED")
                .entityName("Staff")
                .entityId("1")
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.searchAuditLogs(
                isNull(),
                isNull(),
                eq("STATUS_CHANGED"),
                eq("%staff%"),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);
        when(auditLogMapper.toResponse(log)).thenReturn(response);

        PageResponse<AuditLogResponse> result = auditLogService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActionType()).isEqualTo("STATUS_CHANGED");
        assertThat(result.getContent().get(0).getEntityName()).isEqualTo("Staff");
        verify(auditLogRepository).searchAuditLogs(isNull(), isNull(), eq("STATUS_CHANGED"), eq("%staff%"), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Ghi log kiểm toán với actionType=STATUS_CHANGED thành công")
    void recordLog_StatusChanged_Success() {
        Staff staff = Staff.builder().id(1).build();
        when(staffRepository.findById(1)).thenReturn(Optional.of(staff));

        auditLogService.recordLog(
                1,
                "STATUS_CHANGED",
                "Staff",
                "1",
                "{\"status\":\"ACTIVE\"}",
                "{\"status\":\"SUSPENDED\"}",
                "127.0.0.1",
                "Mozilla/5.0"
        );

        verify(auditLogRepository).saveAndFlush(argThat(logEntry ->
                "STATUS_CHANGED".equals(logEntry.getActionType()) &&
                "Staff".equals(logEntry.getEntityName()) &&
                "1".equals(logEntry.getEntityId())
        ));
        verify(auditLogRepository).purgeOldAuditLogsExceedingLimit();
    }
}

