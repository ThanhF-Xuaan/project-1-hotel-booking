package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityReadingResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.UtilityReadingService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utility-readings")
@RequiredArgsConstructor
@Tag(name = "Utility Reading API")
public class UtilityReadingController {

    private final UtilityReadingService service;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING', 'REGION_MANAGER')")
    @Operation(summary = "Lọc danh sách chỉ số đọc")
    public PageResponse<UtilityReadingResponse> search(@RequestBody UtilityReadingSearchDto request) {
        return service.search(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING', 'REGION_MANAGER')")
    @Operation(summary = "Lấy chi tiết chỉ số đọc")
    public UtilityReadingResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING')")
    @Operation(summary = "Thêm mới chỉ số đọc")
    public UtilityReadingResponse create(@Valid @RequestBody UtilityReadingCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING')")
    @Operation(summary = "Cập nhật chỉ số đọc")
    public UtilityReadingResponse update(@PathVariable Long id, @Valid @RequestBody UtilityReadingUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING')")
    @Operation(summary = "Xóa (mềm) chỉ số đọc")
    public void delete(@RequestBody List<Long> ids) {
        service.delete(ids);
    }
}
