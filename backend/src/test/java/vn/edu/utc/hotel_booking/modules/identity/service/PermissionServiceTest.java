package vn.edu.utc.hotel_booking.modules.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;
import vn.edu.utc.hotel_booking.modules.identity.mapper.PermissionMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.PermissionRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.RoleLookupService;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.PermissionServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleLookupService roleLookupService;

    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Permission p1;
    private Permission p2;
    private PermissionResponse res1;
    private PermissionResponse res2;

    @BeforeEach
    void setUp() {
        p1 = Permission.builder().id((short) 1).action("VIEW").resource("ROOM").build();
        p2 = Permission.builder().id((short) 2).action("CREATE").resource("ROOM").build();

        res1 = PermissionResponse.builder().id((short) 1).action("VIEW").resource("ROOM").build();
        res2 = PermissionResponse.builder().id((short) 2).action("CREATE").resource("ROOM").build();
    }

    @Test
    @DisplayName("Happy path: Lấy tất cả quyền hạn hệ thống thành công")
    void getAllPermissions_Success() {
        when(permissionRepository.findAllByIsDeletedFalse()).thenReturn(List.of(p1, p2));
        when(roleLookupService.getPermissionRoleMap()).thenReturn(Collections.emptyMap());
        when(permissionMapper.toResponse(p1)).thenReturn(res1);
        when(permissionMapper.toResponse(p2)).thenReturn(res2);

        List<PermissionResponse> results = permissionService.getAllPermissions();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getAction()).isEqualTo("VIEW");
        verify(permissionRepository).findAllByIsDeletedFalse();
    }

    @Test
    @DisplayName("Happy path: Tìm kiếm và phân trang danh mục quyền hạn thành công")
    void filter_Success() {
        PermissionSearchDto searchDto = PermissionSearchDto.builder()
                .keyword("ROOM")
                .page(0)
                .pageSize(10)
                .build();

        Page<Permission> page = new PageImpl<>(List.of(p1, p2));
        when(permissionRepository.searchPermissions(eq("ROOM"), isNull(), isNull(), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(page);
        when(roleLookupService.getPermissionRoleMap()).thenReturn(Collections.emptyMap());
        when(permissionMapper.toResponse(p1)).thenReturn(res1);
        when(permissionMapper.toResponse(p2)).thenReturn(res2);

        PageResponse<PermissionResponse> response = permissionService.filter(searchDto);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Happy path: Lấy danh sách thực thể Permission theo tập IDs hợp lệ")
    void getEntitiesByIds_Success() {
        Set<Short> ids = Set.of((short) 1, (short) 2);
        when(permissionRepository.findByIdInAndIsDeletedFalse(ids)).thenReturn(List.of(p1, p2));

        List<Permission> results = permissionService.getEntitiesByIds(ids);

        assertThat(results).hasSize(2);
        assertThat(results).containsExactlyInAnyOrder(p1, p2);
        verify(permissionRepository).findByIdInAndIsDeletedFalse(ids);
    }

    @Test
    @DisplayName("Edge case: Trả về danh sách rỗng khi IDs là null hoặc empty")
    void getEntitiesByIds_NullOrEmpty() {
        List<Permission> empty1 = permissionService.getEntitiesByIds(null);
        List<Permission> empty2 = permissionService.getEntitiesByIds(Collections.emptySet());

        assertThat(empty1).isEmpty();
        assertThat(empty2).isEmpty();
        verify(permissionRepository, never()).findByIdInAndIsDeletedFalse(any());
    }

    @Test
    @DisplayName("Happy path: Tra cứu thực thể Permission theo ID thành công")
    void getEntityById_Success() {
        when(permissionRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(p1));

        Permission result = permissionService.getEntityById((short) 1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 1);
        verify(permissionRepository).findByIdAndIsDeletedFalse((short) 1);
    }

    @Test
    @DisplayName("Error path: Ném PERMISSION_NOT_FOUND khi ID không tồn tại")
    void getEntityById_NotFound_ThrowsException() {
        when(permissionRepository.findByIdAndIsDeletedFalse((short) 99)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> permissionService.getEntityById((short) 99));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_NOT_FOUND);
    }

    @Test
    @DisplayName("Error path: Ném PERMISSION_NOT_FOUND khi ID là null")
    void getEntityById_NullId_ThrowsException() {
        AppException exception = assertThrows(AppException.class, () -> permissionService.getEntityById(null));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_NOT_FOUND);
        verify(permissionRepository, never()).findByIdAndIsDeletedFalse(any());
    }
}
