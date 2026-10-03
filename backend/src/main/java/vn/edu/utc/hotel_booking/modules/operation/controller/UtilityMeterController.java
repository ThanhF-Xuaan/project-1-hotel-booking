package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityMeterResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.UtilityMeterService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utility-meters")
@RequiredArgsConstructor
@Tag(name = "Utility Meter API")
public class UtilityMeterController {

    private final UtilityMeterService service;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING', 'REGION_MANAGER')")
    @Operation(summary = "Lọc danh sách đồng hồ")
    public ApiResponse<PageResponse<UtilityMeterResponse>> search(@RequestBody UtilityMeterSearchDto request) {
        return ApiResponse.<PageResponse<UtilityMeterResponse>>builder()
                .result(service.search(request))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING', 'REGION_MANAGER')")
    @Operation(summary = "Lấy chi tiết đồng hồ")
    public ApiResponse<UtilityMeterResponse> getById(@PathVariable Integer id) {
        return ApiResponse.<UtilityMeterResponse>builder()
                .result(service.getById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING')")
    @Operation(summary = "Thêm mới đồng hồ")
    public ApiResponse<UtilityMeterResponse> create(@Valid @RequestBody UtilityMeterCreateRequest request) {
        return ApiResponse.<UtilityMeterResponse>builder()
                .result(service.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING')")
    @Operation(summary = "Cập nhật đồng hồ")
    public ApiResponse<UtilityMeterResponse> update(@PathVariable Integer id, @Valid @RequestBody UtilityMeterUpdateRequest request) {
        return ApiResponse.<UtilityMeterResponse>builder()
                .result(service.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING')")
    @Operation(summary = "Xóa (mềm) đồng hồ")
    public ApiResponse<Void> delete(@RequestBody List<Integer> ids) {
        service.delete(ids);
        return ApiResponse.<Void>builder().build();
    }
}
