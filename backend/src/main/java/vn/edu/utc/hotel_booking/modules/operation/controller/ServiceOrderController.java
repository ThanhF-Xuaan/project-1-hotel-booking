package vn.edu.utc.hotel_booking.modules.operation.controller;

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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;

@RestController
@RequestMapping("/api/v1/service-orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Service Order Controller", description = "Quản lý đặt dịch vụ, minibar, F&B phòng và tự động hạch toán phụ phí")
public class ServiceOrderController {

    ServiceOrderService serviceOrderService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'F_AND_B')")
    @Operation(summary = "Tạo mới đơn đặt dịch vụ/món ăn cho phòng (Tự động trừ kho & đẩy vào Folio)")
    public ApiResponse<ServiceOrderResponse> createOrder(@Valid @RequestBody ServiceOrderCreateRequest request) {
        return ApiResponse.success("Tạo đơn dịch vụ thành công", serviceOrderService.createOrder(request));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'F_AND_B', 'FINANCE')")
    @Operation(summary = "Tìm kiếm và phân trang đơn dịch vụ (POST Search DTO)")
    public ApiResponse<PageResponse<ServiceOrderResponse>> filter(@RequestBody ServiceOrderSearchDto searchDto) {
        return ApiResponse.success(serviceOrderService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'F_AND_B', 'FINANCE')")
    @Operation(summary = "Xem chi tiết đơn đặt dịch vụ theo ID")
    public ApiResponse<ServiceOrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(serviceOrderService.getById(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'F_AND_B')")
    @Operation(summary = "Cập nhật trạng thái đơn dịch vụ (PREPARING, DELIVERED, COMPLETED, CANCELLED)")
    public ApiResponse<ServiceOrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ServiceOrderStatus status
    ) {
        return ApiResponse.success("Cập nhật trạng thái đơn dịch vụ thành công", serviceOrderService.updateStatus(id, status));
    }
}
