package vn.edu.utc.hotel_booking.modules.pricing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.CampaignResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.CampaignService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/campaigns")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Campaigns", description = "APIs quản lý chiến dịch khuyến mại")
public class CampaignController {

    CampaignService campaignService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới chiến dịch khuyến mại")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật thông tin chiến dịch")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable Integer id,
            @Valid @RequestBody CampaignUpdateRequest request) {
        CampaignResponse response = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Xem chi tiết chiến dịch theo ID")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(@PathVariable Integer id) {
        CampaignResponse response = campaignService.getCampaignById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Tìm kiếm & lọc danh sách chiến dịch khuyến mại")
    public ResponseEntity<ApiResponse<PageResponse<CampaignResponse>>> filterCampaigns(
            @RequestBody CampaignSearchDto searchDto) {
        PageResponse<CampaignResponse> response = campaignService.filterCampaigns(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt chiến dịch (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteCampaigns(@RequestBody List<Integer> ids) {
        campaignService.deleteCampaigns(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
