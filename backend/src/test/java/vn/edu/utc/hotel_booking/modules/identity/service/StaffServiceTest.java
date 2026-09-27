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
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.mapper.StaffMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.StaffServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;
import vn.edu.utc.hotel_booking.modules.organization.repository.DepartmentRepository;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.organization.repository.RegionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private StaffMapper staffMapper;

    @InjectMocks
    private StaffServiceImpl staffService;

    private Role role;
    private Department department;
    private Staff staff;
    private StaffResponse staffResponse;
    private UUID keycloakId;

    @BeforeEach
    void setUp() {
        keycloakId = UUID.randomUUID();

        role = Role.builder()
                .id((short) 4)
                .code("RECEPTIONIST")
                .name("Lễ tân Tiền sảnh")
                .build();

        department = Department.builder()
                .id((short) 1)
                .code("FRONT_OFFICE")
                .name("Lễ tân")
                .build();

        staff = Staff.builder()
                .id(1)
                .keycloakId(keycloakId)
                .role(role)
                .scopeType("PROPERTY")
                .scopeEntityId(1)
                .department(department)
                .username("receptionist_hn")
                .email("receptionist@utc.edu.vn")
                .phone("0988889999")
                .firstName("A")
                .lastName("Nguyễn Văn")
                .fullName("Nguyễn Văn A")
                .status("ACTIVE")
                .build();

        staffResponse = StaffResponse.builder()
                .id(1)
                .keycloakId(keycloakId)
                .username("receptionist_hn")
                .fullName("Nguyễn Văn A")
                .roleCode("RECEPTIONIST")
                .scopeType("PROPERTY")
                .scopeEntityId(1)
                .build();
    }

    @Test
    @DisplayName("Happy path: Lấy thông tin nhân viên theo ID")
    void getById_Success() {
        when(staffRepository.findByIdWithDetails(1)).thenReturn(Optional.of(staff));
        when(staffMapper.toResponse(staff)).thenReturn(staffResponse);

        StaffResponse result = staffService.getById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getUsername()).isEqualTo("receptionist_hn");
        verify(staffRepository).findByIdWithDetails(1);
    }

    @Test
    @DisplayName("Error path: Ném STAFF_NOT_FOUND khi ID không tồn tại")
    void getById_NotFound_ThrowsException() {
        when(staffRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> staffService.getById(999));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STAFF_NOT_FOUND);
    }

    @Test
    @DisplayName("Happy path: Tạo nhân viên cấp PROPERTY thành công")
    void create_PropertyScope_Success() {
        StaffCreateRequest request = StaffCreateRequest.builder()
                .keycloakId(keycloakId)
                .roleId((short) 4)
                .scopeType("PROPERTY")
                .scopeEntityId(1)
                .departmentId((short) 1)
                .username("receptionist_hn")
                .email("receptionist@utc.edu.vn")
                .phone("0988889999")
                .firstName("A")
                .lastName("Nguyễn Văn")
                .build();

        when(staffRepository.existsByUsernameAndIsDeletedFalse("receptionist_hn")).thenReturn(false);
        when(staffRepository.existsByEmailAndIsDeletedFalse("receptionist@utc.edu.vn")).thenReturn(false);
        when(staffRepository.existsByPhoneAndIsDeletedFalse("0988889999")).thenReturn(false);
        when(roleRepository.findByIdAndIsDeletedFalse((short) 4)).thenReturn(Optional.of(role));
        when(hotelRepository.existsById((short) 1)).thenReturn(true);
        when(departmentRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(department));
        when(staffMapper.toEntity(request)).thenReturn(staff);
        when(staffRepository.save(any(Staff.class))).thenReturn(staff);
        when(staffMapper.toResponse(staff)).thenReturn(staffResponse);

        StaffResponse result = staffService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("receptionist_hn");
        verify(staffRepository).save(any(Staff.class));
    }

    @Test
    @DisplayName("Error path: Báo lỗi INVALID_SCOPE_CONFIGURATION khi nhân viên cấp CHAIN lại có scopeEntityId")
    void create_InvalidChainScope_ThrowsException() {
        StaffCreateRequest request = StaffCreateRequest.builder()
                .keycloakId(keycloakId)
                .roleId((short) 1)
                .scopeType("CHAIN")
                .scopeEntityId(1) // CHAIN phải có scopeEntityId = null
                .username("admin_chain")
                .firstName("Admin")
                .lastName("Chain")
                .build();

        when(staffRepository.existsByUsernameAndIsDeletedFalse("admin_chain")).thenReturn(false);
        when(roleRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(role));
        when(staffMapper.toEntity(request)).thenReturn(new Staff());

        AppException exception = assertThrows(AppException.class, () -> staffService.create(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SCOPE_CONFIGURATION);
        verify(staffRepository, never()).save(any());
    }

    @Test
    @DisplayName("Error path: Báo lỗi USERNAME_ALREADY_EXISTS khi tên đăng nhập đã được sử dụng")
    void create_DuplicateUsername_ThrowsException() {
        StaffCreateRequest request = StaffCreateRequest.builder()
                .username("receptionist_hn")
                .firstName("A")
                .lastName("Nguyễn")
                .build();

        when(staffRepository.existsByUsernameAndIsDeletedFalse("receptionist_hn")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> staffService.create(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
        verify(staffRepository, never()).save(any());
    }

    @Test
    @DisplayName("Batch Delete: Xóa mềm danh sách nhân viên")
    void deleteBatch_Success() {
        List<Integer> ids = List.of(1, 2);
        when(staffRepository.softDeleteBatch(ids)).thenReturn(2);

        staffService.deleteBatch(ids);

        verify(staffRepository).softDeleteBatch(ids);
    }
}
