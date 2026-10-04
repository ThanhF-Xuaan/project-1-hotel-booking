package vn.edu.utc.hotel_booking.modules.finance.controller;

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
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.InvoiceResponse;
import vn.edu.utc.hotel_booking.modules.finance.service.InvoiceService;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Invoice Controller", description = "Quản lý hóa đơn GTGT / VAT Invoice và đóng Folio")
public class InvoiceController {

    InvoiceService invoiceService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Tạo hóa đơn từ đơn đặt phòng (Bất biến số liệu tài chính)")
    public ApiResponse<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceCreateRequest request) {
        return ApiResponse.success("Tạo hóa đơn thành công", invoiceService.createInvoice(request));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Tìm kiếm và phân trang hóa đơn")
    public ApiResponse<PageResponse<InvoiceResponse>> filter(@RequestBody InvoiceSearchDto searchDto) {
        return ApiResponse.success(invoiceService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Lấy chi tiết hóa đơn theo ID")
    public ApiResponse<InvoiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(invoiceService.getById(id));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Lấy hóa đơn theo ID đơn đặt phòng")
    public ApiResponse<InvoiceResponse> getByBookingId(@PathVariable Long bookingId) {
        return ApiResponse.success(invoiceService.getByBookingId(bookingId));
    }

    @PutMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Phát hành hóa đơn chính thức (Chuyển trạng thái sang ISSUED)")
    public ApiResponse<InvoiceResponse> issueInvoice(@PathVariable Long id) {
        return ApiResponse.success("Phát hành hóa đơn thành công", invoiceService.issueInvoice(id));
    }
}
