package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StaffMapperTest {

    private final StaffMapper mapper = Mappers.getMapper(StaffMapper.class);

    @Test
    @DisplayName("Map StaffCreateRequest to Staff entity")
    void testToEntity() {
        StaffCreateRequest request = StaffCreateRequest.builder()
                .username("receptionist_01")
                .firstName("Nguyen")
                .lastName("Van A")
                .email("receptionist01@utc.edu.vn")
                .phone("0987654321")
                .scopeType("PROPERTY")
                .scopeEntityId(1)
                .status("ACTIVE")
                .password("Secret@123")
                .build();

        Staff staff = mapper.toEntity(request);

        assertThat(staff).isNotNull();
        assertThat(staff.getUsername()).isEqualTo("receptionist_01");
        assertThat(staff.getFirstName()).isEqualTo("Nguyen");
        assertThat(staff.getLastName()).isEqualTo("Van A");
        assertThat(staff.getEmail()).isEqualTo("receptionist01@utc.edu.vn");
        assertThat(staff.getPhone()).isEqualTo("0987654321");
        assertThat(staff.getScopeType()).isEqualTo("PROPERTY");
        assertThat(staff.getScopeEntityId()).isEqualTo(1);
        assertThat(staff.getStatus()).isEqualTo("ACTIVE");
        assertThat(staff.getKeycloakId()).isNull(); // Should be ignored in mapper and assigned from KeycloakService
    }

    @Test
    @DisplayName("Map Staff entity to StaffResponse")
    void testToResponse() {
        Role role = Role.builder()
                .id((short) 1)
                .name("Quản trị viên")
                .code("ROLE_ADMIN")
                .build();

        Department dept = Department.builder()
                .id((short) 2)
                .name("Lễ tân")
                .build();

        UUID keycloakId = UUID.randomUUID();
        Staff staff = Staff.builder()
                .id(100)
                .keycloakId(keycloakId)
                .username("admin_utc")
                .firstName("Le")
                .lastName("Van B")
                .fullName("Le Van B")
                .email("admin@utc.edu.vn")
                .phone("0123456789")
                .role(role)
                .department(dept)
                .scopeType("CHAIN")
                .status("ACTIVE")
                .build();

        StaffResponse response = mapper.toResponse(staff);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100);
        assertThat(response.getKeycloakId()).isEqualTo(keycloakId);
        assertThat(response.getUsername()).isEqualTo("admin_utc");
        assertThat(response.getRoleId()).isEqualTo((short) 1);
        assertThat(response.getRoleName()).isEqualTo("Quản trị viên");
        assertThat(response.getRoleCode()).isEqualTo("ROLE_ADMIN");
        assertThat(response.getDepartmentId()).isEqualTo((short) 2);
        assertThat(response.getDepartmentName()).isEqualTo("Lễ tân");
    }

    @Test
    @DisplayName("Update Staff entity from StaffUpdateRequest")
    void testUpdateEntity() {
        Staff staff = Staff.builder()
                .id(100)
                .username("old_user")
                .firstName("OldFirst")
                .lastName("OldLast")
                .email("old@utc.edu.vn")
                .status("ACTIVE")
                .build();

        StaffUpdateRequest updateRequest = StaffUpdateRequest.builder()
                .firstName("NewFirst")
                .email("new@utc.edu.vn")
                .build();

        mapper.updateEntity(staff, updateRequest);

        assertThat(staff.getFirstName()).isEqualTo("NewFirst");
        assertThat(staff.getLastName()).isEqualTo("OldLast"); // Unchanged
        assertThat(staff.getEmail()).isEqualTo("new@utc.edu.vn");
        assertThat(staff.getUsername()).isEqualTo("old_user"); // Ignored
    }
}
