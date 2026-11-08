package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.RoleLookupService;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoleLookupServiceImpl implements RoleLookupService {

    private final RoleRepository roleRepository;

    @Override
    public List<Role> getActiveRolesWithPermissions() {
        return roleRepository.findAllWithPermissions();
    }

    @Override
    public Map<Short, List<Role>> getPermissionRoleMap() {
        Map<Short, List<Role>> map = new HashMap<>();
        try {
            List<Role> roles = roleRepository.findAllWithPermissions();
            for (Role role : roles) {
                if (role.getPermissions() != null) {
                    for (Permission p : role.getPermissions()) {
                        map.computeIfAbsent(p.getId(), k -> new ArrayList<>()).add(role);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải quan hệ vai trò - quyền hạn qua RoleLookupService: {}", e.getMessage());
        }
        return map;
    }

    @Override
    public Role getRoleEntityById(Short id) {
        if (id == null) {
            return null;
        }
        return roleRepository.findByIdAndIsDeletedFalse(id).orElse(null);
    }
}

