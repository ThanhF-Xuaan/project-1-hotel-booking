package vn.edu.utc.hotel_booking.modules.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.StaffProvisioningServiceImpl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffProvisioningServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private RoleRepository roleRepository;

    private StaffProvisioningService provisioningService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        provisioningService = new StaffProvisioningServiceImpl(staffRepository, roleRepository, objectMapper);
    }

    private String createTestJwt(String sub, String username, String email, String name, String roleCode) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"preferred_username\":\"%s\",\"email\":\"%s\",\"name\":\"%s\",\"realm_access\":{\"roles\":[\"%s\"]}}",
                sub, username, email, name, roleCode
        );
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".dummySignature";
    }

    @Test
    @DisplayName("JIT Provisioning: Tạo mới hồ sơ Staff khi keycloakId chưa tồn tại trong PostgreSQL")
    void provisionOrUpdateFromJwt_NewUser_CreatesStaff() {
        UUID keycloakId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        String token = createTestJwt(keycloakId.toString(), "admin", "admin@hotel.utc.edu.vn", "System Admin", "CHAIN_ADMIN");

        Role chainAdminRole = Role.builder().id((short) 1).code("CHAIN_ADMIN").name("Quản trị viên").build();

        when(staffRepository.findByKeycloakIdAndIsDeletedFalse(keycloakId)).thenReturn(Optional.empty());
        when(staffRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.empty());
        when(roleRepository.findByCodeAndIsDeletedFalse("CHAIN_ADMIN")).thenReturn(Optional.of(chainAdminRole));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff result = provisioningService.provisionOrUpdateFromJwt(token, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(keycloakId);
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getEmail()).isEqualTo("admin@hotel.utc.edu.vn");
        assertThat(result.getFullName()).isEqualTo("System Admin");
        assertThat(result.getRole()).isEqualTo(chainAdminRole);
        assertThat(result.getScopeType()).isEqualTo("CHAIN");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        verify(staffRepository, times(1)).save(any(Staff.class));
    }

    @Test
    @DisplayName("JIT Provisioning: Liên kết keycloakId khi tìm thấy Staff theo username")
    void provisionOrUpdateFromJwt_ExistingByUsername_LinksKeycloakId() {
        UUID keycloakId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        String token = createTestJwt(keycloakId.toString(), "admin", "admin@hotel.utc.edu.vn", "System Admin", "CHAIN_ADMIN");

        Staff existingStaff = Staff.builder()
                .id(1)
                .username("admin")
                .keycloakId(null)
                .fullName("Old Admin Name")
                .build();

        when(staffRepository.findByKeycloakIdAndIsDeletedFalse(keycloakId)).thenReturn(Optional.empty());
        when(staffRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.of(existingStaff));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff result = provisioningService.provisionOrUpdateFromJwt(token, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(keycloakId);
        assertThat(result.getUsername()).isEqualTo("admin");

        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(captor.capture());
        assertThat(captor.getValue().getKeycloakId()).isEqualTo(keycloakId);
    }

    @Test
    @DisplayName("JIT Provisioning: Idempotent khi Staff đã tồn tại đúng keycloakId")
    void provisionOrUpdateFromJwt_AlreadyLinked_ReturnsExisting() {
        UUID keycloakId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        String token = createTestJwt(keycloakId.toString(), "admin", "admin@hotel.utc.edu.vn", "System Admin", "CHAIN_ADMIN");

        Staff existingStaff = Staff.builder()
                .id(1)
                .keycloakId(keycloakId)
                .username("admin")
                .email("admin@hotel.utc.edu.vn")
                .fullName("System Admin")
                .build();

        when(staffRepository.findByKeycloakIdAndIsDeletedFalse(keycloakId)).thenReturn(Optional.of(existingStaff));

        Staff result = provisioningService.provisionOrUpdateFromJwt(token, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        verify(staffRepository, never()).save(any(Staff.class));
    }

    @Test
    @DisplayName("JIT Provisioning: Trả về null an toàn khi token rỗng hoặc không hợp lệ")
    void provisionOrUpdateFromJwt_InvalidToken_ReturnsNull() {
        Staff result = provisioningService.provisionOrUpdateFromJwt("", "admin");
        assertThat(result).isNull();

        Staff resultInvalid = provisioningService.provisionOrUpdateFromJwt("invalid.jwt", "admin");
        assertThat(resultInvalid).isNull();

        verifyNoInteractions(staffRepository);
    }
}

