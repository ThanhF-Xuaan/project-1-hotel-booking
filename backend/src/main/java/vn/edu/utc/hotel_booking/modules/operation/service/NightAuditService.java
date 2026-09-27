package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.modules.operation.dto.request.NightAuditRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.NightAuditResponse;

import java.time.LocalDate;

public interface NightAuditService {

    NightAuditResponse executeNightAudit(NightAuditRequest request);

    NightAuditResponse getAuditSummary(Short hotelId, LocalDate auditDate);
}
