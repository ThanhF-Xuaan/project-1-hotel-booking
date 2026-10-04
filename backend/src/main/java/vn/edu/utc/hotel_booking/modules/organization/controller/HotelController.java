package vn.edu.utc.hotel_booking.modules.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Tag(name = "Organization - Hotels", description = "APIs quản lý khách sạn cơ sở")
public class HotelController {

    private final HotelService hotelService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Tìm kiếm và phân trang khách sạn cơ sở")
    public ApiResponse<PageResponse<HotelResponse>> filter(@RequestBody @Valid HotelSearchDto searchDto) {
        return ApiResponse.<PageResponse<HotelResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách khách sạn thành công")
                .result(hotelService.filter(searchDto))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Lấy chi tiết thông tin một khách sạn cơ sở theo ID")
    public ApiResponse<HotelResponse> getById(@PathVariable Short id) {
        return ApiResponse.<HotelResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin khách sạn thành công")
                .result(hotelService.getById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tạo mới khách sạn cơ sở")
    public ApiResponse<HotelResponse> create(@RequestBody @Valid HotelCreateRequest request) {
        return ApiResponse.<HotelResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới khách sạn thành công")
                .result(hotelService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Cập nhật thông tin khách sạn cơ sở")
    public ApiResponse<HotelResponse> update(@PathVariable Short id,
                                             @RequestBody @Valid HotelUpdateRequest request) {
        return ApiResponse.<HotelResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật thông tin khách sạn thành công")
                .result(hotelService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Xóa mềm một hoặc nhiều khách sạn (Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Short> ids) {
        hotelService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh sách khách sạn thành công")
                .build();
    }
}
