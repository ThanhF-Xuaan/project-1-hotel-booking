package vn.edu.utc.hotel_booking.modules.organization.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffLookupService;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.DepartmentResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;
import vn.edu.utc.hotel_booking.modules.organization.mapper.DepartmentMapper;
import vn.edu.utc.hotel_booking.modules.organization.repository.DepartmentRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.impl.DepartmentServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StaffLookupService staffLookupService;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    @DisplayName("Lọc và phân trang phòng ban thành công")
    void filter_Success() {
        DepartmentSearchDto searchDto = new DepartmentSearchDto();
        searchDto.setKeyword("Lễ tân");
        searchDto.setPage(0);
        searchDto.setPageSize(10);

        Department dept = Department.builder().id((short) 1).code("FO").name("Lễ tân").status("ACTIVE").build();
        DepartmentResponse response = DepartmentResponse.builder().id((short) 1).code("FO").name("Lễ tân").status("ACTIVE").build();

        Page<Department> page = new PageImpl<>(List.of(dept));
        when(departmentRepository.searchDepartments(eq("Lễ tân"), isNull(), any(Pageable.class))).thenReturn(page);
        when(departmentMapper.toResponse(dept)).thenReturn(response);

        PageResponse<DepartmentResponse> result = departmentService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("FO");
    }

    @Test
    @DisplayName("Lấy chi tiết phòng ban theo ID thành công")
    void getById_Success() {
        Department dept = Department.builder().id((short) 1).code("FO").name("Lễ tân").build();
        DepartmentResponse response = DepartmentResponse.builder().id((short) 1).code("FO").name("Lễ tân").build();

        when(departmentRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(dept));
        when(departmentMapper.toResponse(dept)).thenReturn(response);

        DepartmentResponse result = departmentService.getById((short) 1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 1);
        assertThat(result.getCode()).isEqualTo("FO");
    }

    @Test
    @DisplayName("Lấy chi tiết phòng ban thất bại khi ID không tồn tại")
    void getById_NotFound() {
        when(departmentRepository.findByIdAndIsDeletedFalse((short) 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById((short) 99))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("Tạo mới phòng ban thành công")
    void create_Success() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setCode("HK");
        request.setName("Buồng phòng");

        Department dept = Department.builder().code("HK").name("Buồng phòng").build();
        Department saved = Department.builder().id((short) 2).code("HK").name("Buồng phòng").build();
        DepartmentResponse response = DepartmentResponse.builder().id((short) 2).code("HK").name("Buồng phòng").build();

        when(departmentRepository.existsByCodeAndIsDeletedFalse("HK")).thenReturn(false);
        when(departmentMapper.toEntity(request)).thenReturn(dept);
        when(departmentRepository.save(dept)).thenReturn(saved);
        when(departmentMapper.toResponse(saved)).thenReturn(response);

        DepartmentResponse result = departmentService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 2);
        assertThat(result.getCode()).isEqualTo("HK");
        verify(departmentRepository, times(1)).save(dept);
    }

    @Test
    @DisplayName("Tạo mới phòng ban thất bại khi mã code đã tồn tại")
    void create_DuplicateCode() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setCode("HK");
        request.setName("Buồng phòng");

        when(departmentRepository.existsByCodeAndIsDeletedFalse("HK")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách tất cả phòng ban đang ACTIVE (Dropdown options)")
    void getAllActive_Success() {
        Department dept1 = Department.builder().id((short) 1).code("FO").name("Lễ tân").status("ACTIVE").build();
        Department dept2 = Department.builder().id((short) 2).code("HK").name("Buồng phòng").status("ACTIVE").build();

        DepartmentResponse resp1 = DepartmentResponse.builder().id((short) 1).code("FO").name("Lễ tân").status("ACTIVE").build();
        DepartmentResponse resp2 = DepartmentResponse.builder().id((short) 2).code("HK").name("Buồng phòng").status("ACTIVE").build();

        when(departmentRepository.findAllByStatusAndIsDeletedFalse("ACTIVE")).thenReturn(List.of(dept1, dept2));
        when(departmentMapper.toResponse(dept1)).thenReturn(resp1);
        when(departmentMapper.toResponse(dept2)).thenReturn(resp2);

        List<DepartmentResponse> activeList = departmentService.getAllActive();

        assertThat(activeList).hasSize(2);
        assertThat(activeList.get(0).getCode()).isEqualTo("FO");
        assertThat(activeList.get(1).getCode()).isEqualTo("HK");
    }

    @Test
    @DisplayName("Xóa mềm phòng ban thành công khi không có nhân viên trực thuộc")
    void deleteBatch_Success() {
        List<Short> ids = List.of((short) 1, (short) 2);
        when(staffLookupService.hasActiveStaffInDepartments(ids)).thenReturn(false);
        when(departmentRepository.softDeleteBatch(ids)).thenReturn(2);

        departmentService.deleteBatch(ids);

        verify(departmentRepository, times(1)).softDeleteBatch(ids);
    }

    @Test
    @DisplayName("Xóa mềm phòng ban thất bại khi đang có nhân viên trực thuộc")
    void deleteBatch_InUse() {
        List<Short> ids = List.of((short) 1);
        when(staffLookupService.hasActiveStaffInDepartments(ids)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.deleteBatch(ids))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEPARTMENT_IN_USE);

        verify(departmentRepository, never()).softDeleteBatch(any());
    }
}
