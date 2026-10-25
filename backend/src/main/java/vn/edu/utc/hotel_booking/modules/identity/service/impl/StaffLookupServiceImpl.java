package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffLookupService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffLookupServiceImpl implements StaffLookupService {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveStaffInDepartments(List<Short> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return false;
        }
        return staffRepository.existsByDepartmentIdInAndIsDeletedFalse(departmentIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveStaffWithRoles(List<Short> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        return staffRepository.existsByRoleIdInAndIsDeletedFalse(roleIds);
    }

    @Override
    @Transactional(readOnly = true)
    public vn.edu.utc.hotel_booking.modules.identity.entity.Staff getStaffEntityById(Integer id) {
        if (id == null) {
            return null;
        }
        return staffRepository.findByIdAndIsDeletedFalse(id).orElse(null);
    }
}
