package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;
import vn.edu.utc.hotel_booking.modules.identity.mapper.PermissionMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.PermissionRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.PermissionService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAllByIsDeletedFalse();
        log.debug("Đã tải danh sách quyền hạn hệ thống: count={}", permissions.size());
        return permissionMapper.toResponseList(permissions);
    }

    @Override
    public List<Permission> getEntitiesByIds(Collection<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionRepository.findByIdInAndIsDeletedFalse(ids);
    }

    @Override
    public Permission getEntityById(Short id) {
        if (id == null) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return permissionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
    }
}
