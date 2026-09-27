package vn.edu.utc.hotel_booking.modules.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanySearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.CompanyResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.CompanyService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Identity - Companies", description = "APIs quản lý doanh nghiệp đối tác (B2B Corporate Clients)")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm và phân trang doanh nghiệp đối tác")
    public ApiResponse<PageResponse<CompanyResponse>> filter(@RequestBody @Valid CompanySearchDto searchDto) {
        return ApiResponse.<PageResponse<CompanyResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách doanh nghiệp thành công")
                .result(companyService.filter(searchDto))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lấy chi tiết doanh nghiệp theo ID")
    public ApiResponse<CompanyResponse> getById(@PathVariable Long id) {
        return ApiResponse.<CompanyResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin doanh nghiệp thành công")
                .result(companyService.getById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới thông tin doanh nghiệp đối tác")
    public ApiResponse<CompanyResponse> create(@RequestBody @Valid CompanyCreateRequest request) {
        return ApiResponse.<CompanyResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới doanh nghiệp thành công")
                .result(companyService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật thông tin doanh nghiệp")
    public ApiResponse<CompanyResponse> update(@PathVariable Long id,
                                              @RequestBody @Valid CompanyUpdateRequest request) {
        return ApiResponse.<CompanyResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật thông tin doanh nghiệp thành công")
                .result(companyService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều doanh nghiệp đối tác (Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Long> ids) {
        companyService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh sách doanh nghiệp thành công")
                .build();
    }
}
