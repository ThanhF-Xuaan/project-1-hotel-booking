package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCancelRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractDepositRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupContractResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupContractService;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/group-contracts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Group Contract Controller", description = "Quản lý hợp đồng khách đoàn, báo giá và xác nhận đặt cọc 50%")
public class GroupContractController {

    GroupContractService groupContractService;
    StaffService staffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới hợp đồng khách đoàn (Báo giá & Giữ chỗ 3 ngày)")
    public ApiResponse<GroupContractResponse> createContract(
            @Valid @RequestBody GroupContractCreateRequest request,
            @RequestParam(required = false) Integer staffId
    ) {
        Integer effectiveStaffId = resolveStaffId(staffId);
        return ApiResponse.success("Tạo hợp đồng đặt phòng đoàn thành công", groupContractService.createContract(request, effectiveStaffId));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN', 'FINANCE')")
    @Operation(summary = "Tìm kiếm và phân trang danh sách hợp đồng khách đoàn (POST Search DTO)")
    public ApiResponse<PageResponse<GroupContractResponse>> filterContracts(@RequestBody GroupContractSearchDto searchDto) {
        return ApiResponse.success(groupContractService.filterContracts(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN', 'FINANCE')")
    @Operation(summary = "Xem chi tiết hợp đồng khách đoàn, bóc tách thuế phí và Sổ cái MasterFolio")
    public ApiResponse<GroupContractResponse> getContractById(@PathVariable Long id) {
        return ApiResponse.success(groupContractService.getContractById(id));
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN', 'FINANCE')")
    @Operation(summary = "Xác nhận đặt cọc tối thiểu 50% theo Điều 328 BLDS 2015, kích hoạt block phòng DEFINITE & sinh MasterFolio")
    public ApiResponse<GroupContractResponse> confirmDeposit(
            @PathVariable Long id,
            @Valid @RequestBody GroupContractDepositRequest request
    ) {
        return ApiResponse.success("Xác nhận đặt cọc hợp đồng đoàn thành công", groupContractService.confirmDeposit(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN')")
    @Operation(summary = "Hủy hợp đồng khách đoàn và giải phóng block phòng")
    public ApiResponse<GroupContractResponse> cancelContract(
            @PathVariable Long id,
            @Valid @RequestBody GroupContractCancelRequest request
    ) {
        return ApiResponse.success("Hủy hợp đồng khách đoàn thành công", groupContractService.cancelContract(id, request));
    }

    private Integer resolveStaffId(Integer staffId) {
        if (staffId != null) {
            return staffId;
        }
        return SecurityUtils.getCurrentUserKeycloakId()
                .map(keycloakId -> {
                    try {
                        return staffService.getByKeycloakId(keycloakId).getId();
                    } catch (Exception e) {
                        return 1;
                    }
                })
                .orElse(1);
    }
}
