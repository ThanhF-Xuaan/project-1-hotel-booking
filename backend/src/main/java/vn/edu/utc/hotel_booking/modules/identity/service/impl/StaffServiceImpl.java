package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.mapper.StaffMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.RoleRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.KeycloakService;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;
import vn.edu.utc.hotel_booking.modules.organization.repository.DepartmentRepository;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.organization.repository.RegionRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final RegionRepository regionRepository;
    private final HotelRepository hotelRepository;
    private final StaffMapper staffMapper;
    private final KeycloakService keycloakService;

    @Override
    public PageResponse<StaffResponse> filter(StaffSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<Staff> resultPage = staffRepository.searchStaffs(
                keyword,
                searchDto.getRoleId(),
                searchDto.getDepartmentId(),
                searchDto.getScopeType(),
                searchDto.getScopeEntityId(),
                status,
                pageable
        );
        return PageResponse.from(resultPage.map(staffMapper::toResponse));
    }

    @Override
    public StaffResponse getById(Integer id) {
        Staff staff = staffRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        return staffMapper.toResponse(staff);
    }

    @Override
    public StaffResponse getByKeycloakId(UUID keycloakId) {
        Staff staff = staffRepository.findByKeycloakIdAndIsDeletedFalse(keycloakId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        return staffMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public StaffResponse create(StaffCreateRequest request) {
        String username = request.getUsername().trim();
        if (staffRepository.existsByUsernameAndIsDeletedFalse(username)) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (StringUtils.hasText(request.getEmail())) {
            String email = request.getEmail().trim().toLowerCase();
            if (staffRepository.existsByEmailAndIsDeletedFalse(email)) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        if (StringUtils.hasText(request.getPhone())) {
            String phone = request.getPhone().trim();
            if (staffRepository.existsByPhoneAndIsDeletedFalse(phone)) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        Role role = roleRepository.findByIdAndIsDeletedFalse(request.getRoleId())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // 1. Create User on Keycloak IAM first
        UUID keycloakId = keycloakService.createUser(
                username,
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                role.getCode(),
                request.getPassword()
        );

        // 2. Map and persist to local Database with compensating transaction
        Staff staff = staffMapper.toEntity(request);
        staff.setKeycloakId(keycloakId);
        staff.setRole(role);
        staff.setUsername(username);
        staff.setFirstName(request.getFirstName().trim());
        staff.setLastName(request.getLastName().trim());
        staff.setFullName(request.getLastName().trim() + " " + request.getFirstName().trim());

        validateAndApplyScope(staff, request.getScopeType().trim().toUpperCase(), request.getScopeEntityId(), request.getDepartmentId());

        Staff saved;
        try {
            saved = staffRepository.save(staff);
        } catch (Exception e) {
            log.error("Failed to persist staff to database after Keycloak creation. Triggering compensating delete: keycloakId={}", keycloakId, e);
            try {
                keycloakService.deleteUser(keycloakId);
            } catch (Exception ex) {
                log.error("CRITICAL: Failed to rollback user on Keycloak: keycloakId={}", keycloakId, ex);
            }
            throw e;
        }

        log.info("Đã tạo mới nhân viên: username={}, id={}, keycloakId={}, scope={}", saved.getUsername(), saved.getId(), keycloakId, saved.getScopeType());
        return staffMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StaffResponse update(Integer id, StaffUpdateRequest request) {
        Staff staff = staffRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (StringUtils.hasText(request.getEmail())) {
            String email = request.getEmail().trim().toLowerCase();
            if (staffRepository.existsByEmailAndIdNotAndIsDeletedFalse(email, id)) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            staff.setEmail(email);
        }

        if (StringUtils.hasText(request.getPhone())) {
            String phone = request.getPhone().trim();
            if (staffRepository.existsByPhoneAndIdNotAndIsDeletedFalse(phone, id)) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            staff.setPhone(phone);
        }

        Role role = null;
        if (request.getRoleId() != null) {
            role = roleRepository.findByIdAndIsDeletedFalse(request.getRoleId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            staff.setRole(role);
        }

        staffMapper.updateEntity(staff, request);

        if (StringUtils.hasText(request.getFirstName()) || StringUtils.hasText(request.getLastName())) {
            String firstName = StringUtils.hasText(request.getFirstName()) ? request.getFirstName().trim() : staff.getFirstName();
            String lastName = StringUtils.hasText(request.getLastName()) ? request.getLastName().trim() : staff.getLastName();
            staff.setFirstName(firstName);
            staff.setLastName(lastName);
            staff.setFullName(lastName + " " + firstName);
        }

        String scopeType = StringUtils.hasText(request.getScopeType()) ? request.getScopeType().trim().toUpperCase() : staff.getScopeType();
        Integer scopeEntityId = request.getScopeEntityId() != null ? request.getScopeEntityId() : staff.getScopeEntityId();
        Short departmentId = request.getDepartmentId() != null ? request.getDepartmentId() : (staff.getDepartment() != null ? staff.getDepartment().getId() : null);

        validateAndApplyScope(staff, scopeType, scopeEntityId, departmentId);

        // Sync updates with Keycloak IAM
        Boolean enabled = request.getStatus() != null ? "ACTIVE".equalsIgnoreCase(request.getStatus()) : null;
        String roleCode = role != null ? role.getCode() : (staff.getRole() != null ? staff.getRole().getCode() : null);
        keycloakService.updateUser(
                staff.getKeycloakId(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                roleCode,
                request.getPassword(),
                enabled
        );

        Staff updated = staffRepository.save(staff);
        log.info("Đã cập nhật nhân viên: id={}, username={}, keycloakId={}", updated.getId(), updated.getUsername(), updated.getKeycloakId());
        return staffMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<UUID> keycloakIds = staffRepository.findAllById(ids).stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .map(Staff::getKeycloakId)
                .filter(Objects::nonNull)
                .toList();

        int count = staffRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} nhân viên với danh sách IDs: {}", count, ids);

        if (!keycloakIds.isEmpty()) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Transaction committed successfully. Triggering async Keycloak disable & logout for {} users", keycloakIds.size());
                        keycloakService.asyncDisableAndLogoutUsers(keycloakIds);
                    }
                });
            } else {
                keycloakService.asyncDisableAndLogoutUsers(keycloakIds);
            }
        }
    }

    private void validateAndApplyScope(Staff staff, String scopeType, Integer scopeEntityId, Short departmentId) {
        if (!List.of("CHAIN", "REGION", "PROPERTY").contains(scopeType)) {
            throw new AppException(ErrorCode.INVALID_SCOPE_CONFIGURATION);
        }
        staff.setScopeType(scopeType);

        if ("CHAIN".equals(scopeType)) {
            if (scopeEntityId != null || departmentId != null) {
                throw new AppException(ErrorCode.INVALID_SCOPE_CONFIGURATION);
            }
            staff.setScopeEntityId(null);
            staff.setDepartment(null);
        } else if ("REGION".equals(scopeType)) {
            if (scopeEntityId == null || departmentId != null) {
                throw new AppException(ErrorCode.INVALID_SCOPE_CONFIGURATION);
            }
            if (!regionRepository.existsById(scopeEntityId.shortValue())) {
                throw new AppException(ErrorCode.REGION_NOT_FOUND);
            }
            staff.setScopeEntityId(scopeEntityId);
            staff.setDepartment(null);
        } else {
            // PROPERTY
            if (scopeEntityId == null) {
                throw new AppException(ErrorCode.INVALID_SCOPE_CONFIGURATION);
            }
            if (!hotelRepository.existsById(scopeEntityId.shortValue())) {
                throw new AppException(ErrorCode.HOTEL_NOT_FOUND);
            }
            staff.setScopeEntityId(scopeEntityId);

            if (departmentId != null) {
                Department dept = departmentRepository.findByIdAndIsDeletedFalse(departmentId)
                        .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
                staff.setDepartment(dept);
            } else {
                staff.setDepartment(null);
            }
        }
    }
}
