package vn.edu.utc.hotel_booking.modules.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.RoleResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.mapper.RoleMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.PermissionService;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffLookupService;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.RoleServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private StaffLookupService staffLookupService;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
    private RoleResponse roleResponse;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id((short) 1)
                .code("CHAIN_ADMIN")
                .name("Quản trị viên Chuỗi")
                .build();

        roleResponse = RoleResponse.builder()
                .id((short) 1)
                .code("CHAIN_ADMIN")
                .name("Quản trị viên Chuỗi")
                .build();
    }

    @Test
    @DisplayName("Happy path: Lấy thông tin vai trò theo ID")
    void getById_Success() {
        when(roleRepository.findByIdWithPermissions((short) 1)).thenReturn(Optional.of(role));
        when(roleMapper.toResponse(role)).thenReturn(roleResponse);

        RoleResponse result = roleService.getById((short) 1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 1);
        assertThat(result.getCode()).isEqualTo("CHAIN_ADMIN");
        verify(roleRepository).findByIdWithPermissions((short) 1);
    }

    @Test
    @DisplayName("Error path: Ném ROLE_NOT_FOUND khi ID vai trò không tồn tại")
    void getById_NotFound_ThrowsException() {
        when(roleRepository.findByIdWithPermissions((short) 99)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> roleService.getById((short) 99));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROLE_NOT_FOUND);
    }

    @Test
    @DisplayName("Happy path: Tạo mới vai trò và gán quyền thành công")
    void create_Success() {
        RoleCreateRequest request = RoleCreateRequest.builder()
                .code("RECEPTIONIST")
                .name("Lễ tân")
                .permissionIds(Set.of((short) 10, (short) 11))
                .build();

        Permission p1 = Permission.builder().id((short) 10).action("VIEW").resource("BOOKING").build();
        Permission p2 = Permission.builder().id((short) 11).action("CREATE").resource("BOOKING").build();

        Role createdRole = Role.builder().code("RECEPTIONIST").name("Lễ tân").build();
        Role savedRole = Role.builder().id((short) 2).code("RECEPTIONIST").name("Lễ tân").build();

        when(roleRepository.existsByCodeAndIsDeletedFalse("RECEPTIONIST")).thenReturn(false);
        when(roleMapper.toEntity(request)).thenReturn(createdRole);
        when(permissionService.getEntitiesByIds(request.getPermissionIds())).thenReturn(List.of(p1, p2));
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);
        when(roleMapper.toResponse(savedRole)).thenReturn(
                RoleResponse.builder().id((short) 2).code("RECEPTIONIST").build()
        );

        RoleResponse result = roleService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 2);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("Happy path: Lấy tất cả quyền hạn ủy quyền sang PermissionService")
    void getAllPermissions_Success() {
        PermissionResponse pRes = PermissionResponse.builder().id((short) 1).action("VIEW").resource("ROOM").build();
        when(permissionService.getAllPermissions()).thenReturn(List.of(pRes));

        List<PermissionResponse> result = roleService.getAllPermissions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResource()).isEqualTo("ROOM");
        verify(permissionService).getAllPermissions();
    }

    @Test
    @DisplayName("Error path: Báo lỗi ROLE_CODE_ALREADY_EXISTS khi trùng mã vai trò")
    void create_DuplicateCode_ThrowsException() {
        RoleCreateRequest request = RoleCreateRequest.builder()
                .code("CHAIN_ADMIN")
                .name("Admin")
                .build();

        when(roleRepository.existsByCodeAndIsDeletedFalse("CHAIN_ADMIN")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> roleService.create(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Batch Delete: Xóa mềm danh sách vai trò tùy biến thành công")
    void deleteBatch_Success() {
        List<Short> ids = List.of((short) 10, (short) 11);
        Role custom1 = Role.builder().id((short) 10).code("INTERN").name("Thực tập sinh").build();
        Role custom2 = Role.builder().id((short) 11).code("SECURITY").name("Bảo vệ").build();

        when(roleRepository.findAllById(ids)).thenReturn(List.of(custom1, custom2));
        when(staffLookupService.hasActiveStaffWithRoles(ids)).thenReturn(false);
        when(roleRepository.softDeleteBatch(ids)).thenReturn(2);

        roleService.deleteBatch(ids);

        verify(roleRepository).softDeleteBatch(ids);
    }

    @Test
    @DisplayName("Batch Delete: Chặn xóa vai trò hệ thống nòng cốt (System Role)")
    void deleteBatch_CannotDeleteSystemRole() {
        List<Short> ids = List.of((short) 1);
        Role systemRole = Role.builder().id((short) 1).code("CHAIN_ADMIN").name("Quản trị viên Chuỗi").build();

        when(roleRepository.findAllById(ids)).thenReturn(List.of(systemRole));

        AppException exception = assertThrows(AppException.class, () -> roleService.deleteBatch(ids));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_DELETE_SYSTEM_ROLE);
        verify(roleRepository, never()).softDeleteBatch(any());
    }

    @Test
    @DisplayName("Batch Delete: Chặn xóa tất cả 10 vai trò hệ thống theo SRS Mục 1.2.1.1")
    void deleteBatch_All10SystemRoles_Blocked() {
        for (vn.edu.utc.hotel_booking.modules.identity.entity.SystemRole sr : vn.edu.utc.hotel_booking.modules.identity.entity.SystemRole.values()) {
            Role roleObj = Role.builder().id((short) 99).code(sr.getCode()).name(sr.name()).build();
            when(roleRepository.findAllById(List.of((short) 99))).thenReturn(List.of(roleObj));

            AppException ex = assertThrows(AppException.class, () -> roleService.deleteBatch(List.of((short) 99)));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_DELETE_SYSTEM_ROLE);
        }
    }

    @Test
    @DisplayName("Batch Delete: Chặn xóa vai trò đang có nhân viên trực thuộc gán")
    void deleteBatch_RoleInUse() {
        List<Short> ids = List.of((short) 10);
        Role customRole = Role.builder().id((short) 10).code("INTERN").name("Thực tập sinh").build();

        when(roleRepository.findAllById(ids)).thenReturn(List.of(customRole));
        when(staffLookupService.hasActiveStaffWithRoles(ids)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> roleService.deleteBatch(ids));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROLE_IN_USE);
        verify(roleRepository, never()).softDeleteBatch(any());
    }
}
