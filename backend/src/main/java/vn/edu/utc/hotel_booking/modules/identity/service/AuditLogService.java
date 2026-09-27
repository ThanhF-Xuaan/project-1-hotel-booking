package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.AuditLogSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.AuditLogResponse;

public interface AuditLogService {

    PageResponse<AuditLogResponse> filter(AuditLogSearchDto searchDto);

    void recordLog(Integer staffId,
                   String actionType,
                   String entityName,
                   String entityId,
                   String oldValues,
                   String newValues,
                   String ipAddress,
                   String userAgent);
}
