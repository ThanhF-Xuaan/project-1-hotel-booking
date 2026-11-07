package vn.edu.utc.hotel_booking.modules.identity.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Runner đối soát và nạp hồ sơ nhân sự quản trị cơ sở (Baseline Staff Reconciliation)
 * khi ứng dụng Backend khởi động.
 * Đảm bảo tài khoản admin cốt lõi từ Keycloak luôn có bản ghi tương ứng trong PostgreSQL.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class StaffBaselineReconciliationRunner implements ApplicationRunner {

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;

    private static final UUID DEFAULT_ADMIN_KEYCLOAK_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            reconcileAdminStaff();
        } catch (Exception e) {
            log.error("Failed to run staff baseline reconciliation: {}", e.getMessage(), e);
        }
    }

    private void reconcileAdminStaff() {
        Optional<Staff> existingByUsername = staffRepository.findByUsernameAndIsDeletedFalse(DEFAULT_ADMIN_USERNAME);
        if (existingByUsername.isPresent()) {
            Staff staff = existingByUsername.get();
            if (staff.getKeycloakId() == null) {
                staff.setKeycloakId(DEFAULT_ADMIN_KEYCLOAK_ID);
                staffRepository.save(staff);
                log.info("Linked existing admin staff to Keycloak UUID: {}", DEFAULT_ADMIN_KEYCLOAK_ID);
            }
            return;
        }

        Optional<Staff> existingByKeycloakId = staffRepository.findByKeycloakIdAndIsDeletedFalse(DEFAULT_ADMIN_KEYCLOAK_ID);
        if (existingByKeycloakId.isPresent()) {
            return;
        }

        Role role = roleRepository.findByCodeAndIsDeletedFalse("CHAIN_ADMIN")
                .orElseGet(() -> roleRepository.findAll().stream().findFirst().orElse(null));

        Staff adminStaff = Staff.builder()
                .keycloakId(DEFAULT_ADMIN_KEYCLOAK_ID)
                .username(DEFAULT_ADMIN_USERNAME)
                .email("admin@hotel.utc.edu.vn")
                .firstName("System")
                .lastName("Admin")
                .fullName("System Admin")
                .phone("0901000001")
                .role(role)
                .scopeType("CHAIN")
                .scopeEntityId(null)
                .status("ACTIVE")
                .build();

        staffRepository.save(adminStaff);
        log.info("Staff baseline reconciliation: Seeded initial System Admin (@admin) into PostgreSQL with role CHAIN_ADMIN");
    }
}
