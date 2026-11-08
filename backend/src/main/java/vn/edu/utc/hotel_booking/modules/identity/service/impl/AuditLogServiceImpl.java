package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.AuditLogSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.AuditLogResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.AuditLog;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.mapper.AuditLogMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.AuditLogRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final StaffRepository staffRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResponse<AuditLogResponse> filter(AuditLogSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        String staffKeyword = StringUtils.hasText(searchDto.getStaffKeyword())
                ? "%" + searchDto.getStaffKeyword().trim().toLowerCase() + "%"
                : null;
        String actionType = StringUtils.hasText(searchDto.getActionType()) ? searchDto.getActionType().trim() : null;
        String entityName = StringUtils.hasText(searchDto.getEntityName())
                ? "%" + searchDto.getEntityName().trim().toLowerCase() + "%"
                : null;
        String entityId = StringUtils.hasText(searchDto.getEntityId()) ? searchDto.getEntityId().trim() : null;

        Page<AuditLog> resultPage = auditLogRepository.searchAuditLogs(
                searchDto.getStaffId(),
                staffKeyword,
                actionType,
                entityName,
                entityId,
                searchDto.getFromDate(),
                searchDto.getToDate(),
                pageable
        );
        return PageResponse.from(resultPage.map(auditLogMapper::toResponse));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLog(Integer staffId,
                          String actionType,
                          String entityName,
                          String entityId,
                          String oldValues,
                          String newValues,
                          String ipAddress,
                          String userAgent) {
        try {
            Staff staff = null;
            if (staffId != null) {
                staff = staffRepository.findById(staffId).orElse(null);
            }

            AuditLog logEntry = AuditLog.builder()
                    .staff(staff)
                    .actionType(actionType)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValues(toJsonSafe(oldValues))
                    .newValues(toJsonSafe(newValues))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.saveAndFlush(logEntry);

            // Lưu ý: Không tự động xóa log theo giới hạn 50 bản ghi để tuân thủ Điều 26 Luật An ninh mạng 2018
            // và Nghị định 53/2022/NĐ-CP (yêu cầu lưu trữ vết nhật ký hệ thống tối thiểu 12 tháng).
        } catch (Exception e) {
            log.error("Không thể ghi log kiểm toán: actionType={}, entityName={}, entityId={}", actionType, entityName, entityId, e);
        }
    }

    private String toJsonSafe(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        return "{\"message\":\"" + trimmed.replace("\"", "\\\"") + "\"}";
    }
}
