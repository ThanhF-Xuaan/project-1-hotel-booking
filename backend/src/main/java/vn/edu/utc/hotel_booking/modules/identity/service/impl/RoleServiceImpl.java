package vn.edu.utc.hotel_booking.modules.identity.service.impl;

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
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.RoleResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.mapper.PermissionMapper;
import vn.edu.utc.hotel_booking.modules.identity.mapper.RoleMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.PermissionRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public PageResponse<RoleResponse> filter(RoleSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<Role> resultPage = roleRepository.searchRoles(keyword, status, pageable);
        return PageResponse.from(resultPage.map(roleMapper::toResponse));
    }

    @Override
    public RoleResponse getById(Short id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        return permissionMapper.toResponseList(permissionRepository.findAllByIsDeletedFalse());
    }

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (roleRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new AppException(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        }

        Role role = roleMapper.toEntity(request);
        role.setCode(code);
        role.setName(request.getName().trim());

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findByIdInAndIsDeletedFalse(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        Role saved = roleRepository.save(role);
        log.info("Đã tạo mới vai trò: code={}, id={}", saved.getCode(), saved.getId());
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse update(Short id, RoleUpdateRequest request) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        roleMapper.updateEntity(role, request);
        role.setName(request.getName().trim());

        if (request.getPermissionIds() != null) {
            List<Permission> permissions = permissionRepository.findByIdInAndIsDeletedFalse(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        Role updated = roleRepository.save(role);
        log.info("Đã cập nhật vai trò: id={}, name={}", updated.getId(), updated.getName());
        return roleMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = roleRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} vai trò với danh sách IDs: {}", count, ids);
    }
}
