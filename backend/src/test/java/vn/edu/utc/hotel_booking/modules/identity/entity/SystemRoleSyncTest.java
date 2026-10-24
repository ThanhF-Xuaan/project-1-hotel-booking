package vn.edu.utc.hotel_booking.modules.identity.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemRoleSyncTest {

    @Test
    @DisplayName("Kiểm tra 10 vai trò nòng cốt của chuỗi khách sạn theo SRS Mục 1.2.1.1")
    void testSystemRoles_MatchSRS() {
        Set<String> codes = SystemRole.getSystemRoleCodes();

        assertThat(codes).containsExactlyInAnyOrder(
                "CHAIN_EXECUTIVE",
                "CHAIN_ADMIN",
                "REGION_MANAGER",
                "PROPERTY_MANAGER",
                "RECEPTIONIST",
                "SALES_GROUP",
                "HOUSEKEEPING",
                "F_AND_B",
                "ENGINEERING",
                "FINANCE"
        );
    }

    @Test
    @DisplayName("Kiểm tra phân giải vai trò hệ thống và alias tương thích SRS")
    void testIsSystemRole_RecognizesSystemRolesAndAliases() {
        // Core roles
        assertThat(SystemRole.isSystemRole("CHAIN_ADMIN")).isTrue();
        assertThat(SystemRole.isSystemRole("chain_executive")).isTrue();
        assertThat(SystemRole.isSystemRole("PROPERTY_MANAGER")).isTrue();
        assertThat(SystemRole.isSystemRole("RECEPTIONIST")).isTrue();

        // SRS Aliases
        assertThat(SystemRole.isSystemRole("REGIONAL_MANAGER")).isTrue();
        assertThat(SystemRole.isSystemRole("FRONT_OFFICE")).isTrue();

        // Custom roles
        assertThat(SystemRole.isSystemRole("INTERN")).isFalse();
        assertThat(SystemRole.isSystemRole("SECURITY")).isFalse();
        assertThat(SystemRole.isSystemRole(null)).isFalse();
    }

    @Test
    @DisplayName("Kiểm tra StaffStatus khớp với statusId số nguyên quy chuẩn Backend")
    void testStaffStatus_StatusIdsAndCodes() {
        assertThat(StaffStatus.RESIGNED.getStatusId()).isEqualTo(0);
        assertThat(StaffStatus.ACTIVE.getStatusId()).isEqualTo(1);
        assertThat(StaffStatus.SUSPENDED.getStatusId()).isEqualTo(2);

        assertThat(StaffStatus.fromCode("ACTIVE")).isEqualTo(StaffStatus.ACTIVE);
        assertThat(StaffStatus.fromCode("suspended")).isEqualTo(StaffStatus.SUSPENDED);
        assertThat(StaffStatus.fromCode("RESIGNED")).isEqualTo(StaffStatus.RESIGNED);

        assertThatThrownBy(() -> StaffStatus.fromCode("UNKNOWN_STATUS"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }
}
