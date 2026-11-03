package vn.edu.utc.hotel_booking.modules.operation.security;

import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;

/** Shared identity/scope validation only; each owning service retains its role policy. */
public final class OperationStaffScopeResolver {
    private OperationStaffScopeResolver() { }

    public static Staff resolve(StaffRepository repository) {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(repository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername().flatMap(repository::findByUsernameAndIsDeletedFalse))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    }

    public static Short requireScope(Staff staff, String expectedType) {
        if (staff == null || expectedType == null || !expectedType.equalsIgnoreCase(staff.getScopeType())) throw invalid();
        return requireScopeId(staff);
    }

    public static Short requireScopeId(Staff staff) {
        if (staff == null || !("PROPERTY".equalsIgnoreCase(staff.getScopeType()) || "REGION".equalsIgnoreCase(staff.getScopeType()))
                || staff.getScopeEntityId() == null || staff.getScopeEntityId() < 1 || staff.getScopeEntityId() > Short.MAX_VALUE) {
            throw invalid();
        }
        return staff.getScopeEntityId().shortValue();
    }

    private static AppException invalid() {
        return new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
    }
}
