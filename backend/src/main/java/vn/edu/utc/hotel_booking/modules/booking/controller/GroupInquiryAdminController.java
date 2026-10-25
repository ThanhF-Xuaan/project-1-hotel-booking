package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquirySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupInquiryResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupInquiryService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/group-inquiries")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Group Inquiry Admin Controller", description = "Quản lý và xử lý các yêu cầu báo giá đặt phòng đoàn nội bộ")
public class GroupInquiryAdminController {

    GroupInquiryService groupInquiryService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN')")
    @Operation(summary = "Tìm kiếm & lọc danh sách yêu cầu đặt phòng đoàn (POST Search DTO)")
    public ApiResponse<PageResponse<GroupInquiryResponse>> filterInquiries(@RequestBody GroupInquirySearchDto searchDto) {
        return ApiResponse.success(groupInquiryService.filterInquiries(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN')")
    @Operation(summary = "Xem chi tiết yêu cầu đặt phòng đoàn theo ID")
    public ApiResponse<GroupInquiryResponse> getInquiryById(@PathVariable Long id) {
        return ApiResponse.success(groupInquiryService.getInquiryById(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SALES_GROUP', 'PROPERTY_MANAGER', 'CHAIN_ADMIN')")
    @Operation(summary = "Từ chối yêu cầu đặt phòng đoàn kèm lý do")
    public ApiResponse<GroupInquiryResponse> rejectInquiry(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        String reason = payload != null ? payload.getOrDefault("reason", "Khách sạn không đáp ứng đủ điều kiện theo yêu cầu") : "Từ chối";
        return ApiResponse.success("Đã từ chối yêu cầu đặt phòng đoàn", groupInquiryService.rejectInquiry(id, reason));
    }
}
