package vn.edu.utc.hotel_booking.modules.organization.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.DepartmentResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;
import vn.edu.utc.hotel_booking.modules.organization.mapper.DepartmentMapper;
import vn.edu.utc.hotel_booking.modules.organization.repository.DepartmentRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.DepartmentService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public PageResponse<DepartmentResponse> filter(DepartmentSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<Department> resultPage = departmentRepository.searchDepartments(keyword, status, pageable);
        return PageResponse.from(resultPage.map(departmentMapper::toResponse));
    }

    @Override
    public DepartmentResponse getById(Short id) {
        Department department = departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (departmentRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new AppException(ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS);
        }

        Department department = departmentMapper.toEntity(request);
        department.setCode(code);
        department.setName(request.getName().trim());

        Department saved = departmentRepository.save(department);
        log.info("Đã tạo mới phòng ban: code={}, id={}", saved.getCode(), saved.getId());
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse update(Short id, DepartmentUpdateRequest request) {
        Department department = departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        departmentMapper.updateEntity(department, request);
        if (StringUtils.hasText(request.getName())) {
            department.setName(request.getName().trim());
        }
        if (StringUtils.hasText(request.getStatus())) {
            department.setStatus(request.getStatus().trim());
        }

        Department updated = departmentRepository.save(department);
        log.info("Đã cập nhật phòng ban: id={}, name={}", updated.getId(), updated.getName());
        return departmentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = departmentRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} phòng ban với danh sách IDs: {}", count, ids);
    }
}
