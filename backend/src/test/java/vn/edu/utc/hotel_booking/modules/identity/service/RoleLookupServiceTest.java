package vn.edu.utc.hotel_booking.modules.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.RoleLookupServiceImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleLookupServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleLookupServiceImpl roleLookupService;

    private Role roleAdmin;
    private Role roleStaff;
    private Permission permView;
    private Permission permCreate;

    @BeforeEach
    void setUp() {
        permView = Permission.builder().id((short) 1).action("VIEW").resource("ROOM").build();
        permCreate = Permission.builder().id((short) 2).action("CREATE").resource("ROOM").build();

        roleAdmin = Role.builder()
                .id((short) 10)
                .code("ADMIN")
                .name("Quản trị viên")
                .permissions(new HashSet<>(Set.of(permView, permCreate)))
                .build();

        roleStaff = Role.builder()
                .id((short) 20)
                .code("STAFF")
                .name("Nhân viên")
                .permissions(new HashSet<>(Set.of(permView)))
                .build();
    }

    @Test
    @DisplayName("Happy path: Lấy danh sách vai trò đang hoạt động kèm quyền hạn")
    void getActiveRolesWithPermissions_Success() {
        when(roleRepository.findAllWithPermissions()).thenReturn(List.of(roleAdmin, roleStaff));

        List<Role> result = roleLookupService.getActiveRolesWithPermissions();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(roleAdmin, roleStaff);
        verify(roleRepository).findAllWithPermissions();
    }

    @Test
    @DisplayName("Happy path: Ánh xạ quyền hạn sang danh sách vai trò chính xác")
    void getPermissionRoleMap_Success() {
        when(roleRepository.findAllWithPermissions()).thenReturn(List.of(roleAdmin, roleStaff));

        Map<Short, List<Role>> map = roleLookupService.getPermissionRoleMap();

        assertThat(map).isNotNull();
        assertThat(map.get((short) 1)).hasSize(2); // cả ADMIN và STAFF đều có permView (id=1)
        assertThat(map.get((short) 2)).hasSize(1); // chỉ ADMIN có permCreate (id=2)
        assertThat(map.get((short) 2).get(0).getCode()).isEqualTo("ADMIN");
        verify(roleRepository).findAllWithPermissions();
    }

    @Test
    @DisplayName("Edge case: Vai trò không có quyền nào hoặc permissions là null")
    void getPermissionRoleMap_RoleWithNullPermissions() {
        Role emptyRole = Role.builder().id((short) 30).code("EMPTY").permissions(null).build();
        when(roleRepository.findAllWithPermissions()).thenReturn(List.of(emptyRole));

        Map<Short, List<Role>> map = roleLookupService.getPermissionRoleMap();

        assertThat(map).isEmpty();
        verify(roleRepository).findAllWithPermissions();
    }

    @Test
    @DisplayName("Happy path: Lấy entity Role theo ID thành công")
    void getRoleEntityById_Success() {
        when(roleRepository.findByIdAndIsDeletedFalse((short) 10)).thenReturn(Optional.of(roleAdmin));

        Role result = roleLookupService.getRoleEntityById((short) 10);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("ADMIN");
        verify(roleRepository).findByIdAndIsDeletedFalse((short) 10);
    }

    @Test
    @DisplayName("Edge case: Trả về null khi ID là null")
    void getRoleEntityById_NullId_ReturnsNull() {
        Role result = roleLookupService.getRoleEntityById(null);

        assertThat(result).isNull();
        verify(roleRepository, never()).findByIdAndIsDeletedFalse(any());
    }

    @Test
    @DisplayName("Edge case: Trả về null khi không tìm thấy Role theo ID")
    void getRoleEntityById_NotFound_ReturnsNull() {
        when(roleRepository.findByIdAndIsDeletedFalse((short) 99)).thenReturn(Optional.empty());

        Role result = roleLookupService.getRoleEntityById((short) 99);

        assertThat(result).isNull();
        verify(roleRepository).findByIdAndIsDeletedFalse((short) 99);
    }
}

