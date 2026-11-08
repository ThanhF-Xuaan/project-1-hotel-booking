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
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;
import vn.edu.utc.hotel_booking.modules.identity.entity.Role;
import vn.edu.utc.hotel_booking.modules.identity.mapper.PermissionMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.PermissionRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.PermissionService;
import vn.edu.utc.hotel_booking.modules.identity.service.RoleLookupService;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleLookupService roleLookupService;
    private final PermissionMapper permissionMapper;

    @Override
    public PageResponse<PermissionResponse> filter(PermissionSearchDto searchDto) {
        int page = searchDto != null && searchDto.getPage() != null && searchDto.getPage() >= 0 ? searchDto.getPage() : 0;
        int pageSize = searchDto != null && searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 50;
        String sortBy = searchDto != null && StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "resource";
        Sort.Direction direction = searchDto != null && "DESC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        String keyword = searchDto != null && StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String resource = searchDto != null && StringUtils.hasText(searchDto.getResource()) ? searchDto.getResource().trim().toUpperCase() : null;
        String action = searchDto != null && StringUtils.hasText(searchDto.getAction()) ? searchDto.getAction().trim().toUpperCase() : null;
        String status = searchDto != null && StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim().toUpperCase() : null;

        Page<Permission> pageResult = permissionRepository.searchPermissions(keyword, resource, action, status, pageable);

        Map<Short, List<Role>> permRoleMap = roleLookupService.getPermissionRoleMap();
        Page<PermissionResponse> responsePage = pageResult.map(p -> toEnhancedResponse(p, permRoleMap));

        log.debug("Tra cứu danh mục quyền hạn: totalElements={}", responsePage.getTotalElements());
        return PageResponse.from(responsePage);
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAllByIsDeletedFalse();
        Map<Short, List<Role>> permRoleMap = roleLookupService.getPermissionRoleMap();

        log.debug("Đã tải danh sách quyền hạn hệ thống: count={}", permissions.size());
        return permissions.stream()
                .map(p -> toEnhancedResponse(p, permRoleMap))
                .toList();
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

    @Override
    @Transactional
    public PermissionResponse create(PermissionCreateRequest request) {
        String action = request.getAction().trim().toUpperCase();
        String resource = request.getResource().trim().toUpperCase();

        if (permissionRepository.existsByActionAndResourceAndIsDeletedFalse(action, resource)) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        Permission permission = permissionMapper.toEntity(request);
        permission.setAction(action);
        permission.setResource(resource);
        if (!StringUtils.hasText(permission.getStatus())) {
            permission.setStatus("ACTIVE");
        } else {
            permission.setStatus(permission.getStatus().trim().toUpperCase());
        }

        Permission saved = permissionRepository.save(permission);
        log.info("Đã tạo mới quyền hạn: id={}, action={}, resource={}", saved.getId(), saved.getAction(), saved.getResource());
        return toEnhancedResponse(saved, roleLookupService.getPermissionRoleMap());
    }

    @Override
    @Transactional
    public PermissionResponse update(Short id, PermissionUpdateRequest request) {
        Permission permission = permissionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        String action = request.getAction().trim().toUpperCase();
        String resource = request.getResource().trim().toUpperCase();

        if (permissionRepository.existsByActionAndResourceAndIdNotAndIsDeletedFalse(action, resource, id)) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        permission.setAction(action);
        permission.setResource(resource);
        if (StringUtils.hasText(request.getStatus())) {
            permission.setStatus(request.getStatus().trim().toUpperCase());
        }

        Permission updated = permissionRepository.save(permission);
        log.info("Đã cập nhật quyền hạn: id={}, action={}, resource={}, status={}", updated.getId(), updated.getAction(), updated.getResource(), updated.getStatus());
        return toEnhancedResponse(updated, roleLookupService.getPermissionRoleMap());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        if (permissionRepository.hasActiveRolesUsingPermissions(ids)) {
            throw new AppException(ErrorCode.PERMISSION_IN_USE);
        }

        int count = permissionRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} quyền hạn với danh sách IDs: {}", count, ids);
    }

    @Override
    public List<String> getAllResources() {
        return permissionRepository.findDistinctResources();
    }

    @Override
    public List<String> getAllActions() {
        return permissionRepository.findDistinctActions();
    }

    private PermissionResponse toEnhancedResponse(Permission permission, Map<Short, List<Role>> permRoleMap) {
        PermissionResponse res = permissionMapper.toResponse(permission);
        res.setDescription(resolvePermissionDescription(permission.getAction(), permission.getResource()));
        List<Role> assignedRoles = permRoleMap.getOrDefault(permission.getId(), Collections.emptyList());
        res.setRoleCodes(assignedRoles.stream().map(Role::getCode).toList());
        res.setRoleNames(assignedRoles.stream().map(Role::getName).toList());
        return res;
    }

    private String resolvePermissionDescription(String action, String resource) {
        String act = action != null ? action.toUpperCase().trim() : "";
        String res = resource != null ? resource.toUpperCase().trim() : "";

        return switch (res) {
            case "CHAIN_DASHBOARD" -> "Xem báo cáo tổng hợp và chỉ số vận hành toàn chuỗi";
            case "REGION_DASHBOARD" -> "Xem báo cáo tổng quan và tình hình kinh doanh vùng quản lý";
            case "PROPERTY_DASHBOARD" -> "Xem báo cáo doanh thu và công suất phòng của khách sạn";
            case "PROPERTY" -> act.equals("CREATE") ? "Tạo và thiết lập khách sạn cơ sở mới" : "Quản lý khách sạn cơ sở";
            case "REGION_MANAGER_ACCOUNT" -> "Tạo tài khoản Giám đốc Vùng";
            case "PROPERTY_MANAGER_ACCOUNT" -> "Tạo tài khoản Giám đốc Khách sạn Chi nhánh";
            case "STAFF" -> switch (act) {
                case "VIEW" -> "Xem danh sách và thông tin nhân sự";
                case "CREATE" -> "Thêm mới tài khoản nhân sự";
                case "UPDATE" -> "Cập nhật hồ sơ và điều chuyển nhân sự";
                default -> "Quản lý nhân sự khách sạn";
            };
            case "BOOKING" -> switch (act) {
                case "VIEW" -> "Tra cứu danh sách và chi tiết đơn đặt phòng";
                case "CREATE" -> "Tạo mới đơn đặt phòng (Front Office / Walk-in)";
                case "UPDATE" -> "Cập nhật ngày lưu trú, đổi phòng hoặc dịch vụ đặt phòng";
                case "CANCEL" -> "Hủy đơn đặt phòng và tính phí phạt theo chính sách";
                default -> "Quản lý đơn đặt phòng";
            };
            case "SERVICE_ORDER" -> act.equals("CREATE") ? "Tạo đơn gọi dịch vụ phòng (F&B / Giặt là)" : "Xem danh sách đơn dịch vụ phòng";
            case "ROOM_STATUS" -> "Cập nhật trạng thái buồng phòng (Sạch/Bẩn/Đang dọn)";
            case "MAINTENANCE_TICKET" -> "Báo hỏng và tạo phiếu bảo trì trang thiết bị";
            case "INVENTORY" -> act.equals("UPDATE") ? "Đóng/mở và phân bổ quỹ phòng trống" : "Tra cứu tồn kho phòng trống";
            case "PRICING" -> act.equals("UPDATE") ? "Cập nhật bảng giá, phụ thu và chính sách giá" : "Tra cứu cấu hình giá phòng";
            case "GUEST" -> switch (act) {
                case "VIEW" -> "Tra cứu hồ sơ và lịch sử khách lưu trú";
                case "CREATE" -> "Thêm mới hồ sơ khách lưu trú";
                case "UPDATE" -> "Cập nhật thông tin khách lưu trú";
                default -> "Quản lý hồ sơ khách hàng";
            };
            case "PAYMENT" -> switch (act) {
                case "VIEW" -> "Tra cứu giao dịch và lịch sử thanh toán";
                case "CREATE" -> "Tạo giao dịch thanh toán hoặc thu tiền";
                case "REFUND" -> "Hoàn tiền giao dịch cho khách";
                default -> "Quản lý thanh toán";
            };
            case "INVOICE" -> act.equals("CREATE") ? "Xuất hóa đơn dịch vụ" : "Tra cứu hóa đơn và chứng từ";
            case "NIGHT_AUDIT" -> act.equals("EXECUTE") ? "Thực thi khóa sổ và kiểm toán đêm" : "Xem báo cáo kiểm toán đêm";
            case "COMPANY" -> switch (act) {
                case "VIEW" -> "Tra cứu danh sách doanh nghiệp đối tác";
                case "CREATE" -> "Thêm mới hồ sơ doanh nghiệp";
                case "UPDATE" -> "Cập nhật thông tin doanh nghiệp";
                default -> "Quản lý đối tác doanh nghiệp";
            };
            case "MENU" -> switch (act) {
                case "VIEW" -> "Xem thực đơn F&B và dịch vụ";
                case "CREATE" -> "Thêm món mới vào thực đơn";
                case "UPDATE" -> "Cập nhật giá và trạng thái món";
                default -> "Quản lý thực đơn F&B";
            };
            case "ROOM_INSTANCE" -> "Tra cứu và quản lý phòng vật lý cụ thể";
            case "ROLE" -> "Quản trị vai trò và phân quyền hệ thống";
            case "UTILITY" -> switch (act) {
                case "VIEW" -> "Tra cứu đồng hồ và chỉ số điện nước";
                case "CREATE" -> "Ghi nhận chỉ số tiêu thụ tiện ích mới";
                case "UPDATE" -> "Điều chỉnh chỉ số điện nước";
                default -> "Quản lý tiện ích điện nước";
            };
            case "PAYROLL" -> "Nhập liệu và xem bảng lương nhân sự";
            case "LODGING" -> "Khai báo và xuất dữ liệu lưu trú cho cơ quan chức năng";
            default -> act + " " + res;
        };
    }
}
