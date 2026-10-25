package vn.edu.utc.hotel_booking.modules.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.annotation.Auditable;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffPasswordResetRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staffs")
@RequiredArgsConstructor
@Tag(name = "Identity - Staffs", description = "APIs quản lý nhân sự & tài khoản nhân viên")
public class StaffController {

    private final StaffService staffService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tìm kiếm và phân trang nhân viên")
    public ApiResponse<PageResponse<StaffResponse>> filter(@RequestBody @Valid StaffSearchDto searchDto) {
        return ApiResponse.<PageResponse<StaffResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách nhân viên thành công")
                .result(staffService.filter(searchDto))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Lấy chi tiết nhân viên theo ID")
    public ApiResponse<StaffResponse> getById(@PathVariable Integer id) {
        return ApiResponse.<StaffResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin nhân viên thành công")
                .result(staffService.getById(id))
                .build();
    }

    @GetMapping("/keycloak/{keycloakId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy thông tin nhân viên theo Keycloak UUID")
    public ApiResponse<StaffResponse> getByKeycloakId(@PathVariable UUID keycloakId) {
        return ApiResponse.<StaffResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin nhân viên thành công")
                .result(staffService.getByKeycloakId(keycloakId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới tài khoản nhân viên")
    public ApiResponse<StaffResponse> create(@RequestBody @Valid StaffCreateRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới nhân viên thành công")
                .result(staffService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật thông tin nhân viên")
    public ApiResponse<StaffResponse> update(@PathVariable Integer id,
                                             @RequestBody @Valid StaffUpdateRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật thông tin nhân viên thành công")
                .result(staffService.update(id, request))
                .build();
    }

    @Auditable(
            actionType = "STAFF_STATUS_UPDATE",
            entityName = "Staff",
            entityId = "#id.toString()",
            description = "'Cập nhật trạng thái nhân viên ID ' + #id + ' sang ' + #request.status"
    )
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật trạng thái vòng đời nhân sự (ACTIVE, SUSPENDED, RESIGNED) & thu hồi phiên làm việc")
    public ApiResponse<StaffResponse> updateStatus(@PathVariable Integer id,
                                                   @RequestBody @Valid vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffStatusUpdateRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật trạng thái nhân viên thành công")
                .result(staffService.updateStatus(id, request))
                .build();
    }

    @Auditable(
            actionType = "STAFF_PASSWORD_RESET",
            entityName = "Staff",
            entityId = "#id.toString()",
            description = "'Đặt lại mật khẩu cho nhân viên ID ' + #id",
            maskFields = {"newPassword", "confirmPassword"}
    )
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Đặt lại mật khẩu cho tài khoản nhân viên")
    public ApiResponse<Void> resetPassword(@PathVariable Integer id,
                                           @RequestBody @Valid vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffPasswordResetRequest request) {
        staffService.resetPassword(id, request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Đặt lại mật khẩu nhân viên thành công")
                .build();
    }

    @Auditable(
            actionType = "STAFF_DELETE_BATCH",
            entityName = "Staff",
            entityId = "#ids.toString()",
            description = "'Xóa các nhân viên IDs: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Xóa mềm một hoặc nhiều nhân viên (Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        staffService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh sách nhân viên thành công")
                .build();
    }
}
