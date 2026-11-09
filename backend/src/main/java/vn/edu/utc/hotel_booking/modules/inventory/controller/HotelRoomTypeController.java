package vn.edu.utc.hotel_booking.modules.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeMediaService;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/hotel-room-types")
@RequiredArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Hotel Room Type", description = "APIs cấu hình loại phòng cho từng khách sạn")
public class HotelRoomTypeController {

    HotelRoomTypeService hotelRoomTypeService;
    HotelRoomTypeMediaService hotelRoomTypeMediaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Thêm cấu hình loại phòng cho khách sạn")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> createHotelRoomType(
            @Valid @RequestBody HotelRoomTypeCreateRequest request) {
        HotelRoomTypeResponse response = hotelRoomTypeService.createHotelRoomType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật cấu hình loại phòng của khách sạn")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> updateHotelRoomType(
            @PathVariable Integer id,
            @Valid @RequestBody HotelRoomTypeUpdateRequest request) {
        HotelRoomTypeResponse response = hotelRoomTypeService.updateHotelRoomType(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết cấu hình loại phòng theo ID")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> getHotelRoomTypeById(@PathVariable Integer id) {
        HotelRoomTypeResponse response = hotelRoomTypeService.getHotelRoomTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/beds")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật riêng danh sách giường của hạng phòng")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> updateBeds(
            @PathVariable Integer id,
            @Valid @RequestBody List<vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeBedItemRequest> beds) {
        HotelRoomTypeResponse response = hotelRoomTypeService.updateBeds(id, beds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/features")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật riêng danh sách tiện ích của hạng phòng")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> updateFeatures(
            @PathVariable Integer id,
            @RequestBody java.util.Set<Short> featureIds) {
        HotelRoomTypeResponse response = hotelRoomTypeService.updateFeatures(id, featureIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm & lọc cấu hình loại phòng khách sạn")
    public ResponseEntity<ApiResponse<PageResponse<HotelRoomTypeResponse>>> filterHotelRoomTypes(
            @RequestBody HotelRoomTypeSearchDto searchDto) {
        PageResponse<HotelRoomTypeResponse> response = hotelRoomTypeService.filterHotelRoomTypes(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt cấu hình loại phòng (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteHotelRoomTypes(@RequestBody List<Integer> ids) {
        hotelRoomTypeService.deleteHotelRoomTypes(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lấy danh sách hình ảnh của hạng phòng cơ sở")
    public ApiResponse<List<HotelImageResponse>> getImages(@PathVariable Integer id) {
        return ApiResponse.<List<HotelImageResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách ảnh hạng phòng thành công")
                .result(hotelRoomTypeMediaService.getImages(id))
                .build();
    }

    @PostMapping(value = "/{id}/images", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tải lên một hoặc nhiều hình ảnh cho hạng phòng cơ sở")
    public ApiResponse<List<HotelImageResponse>> uploadImages(
            @PathVariable Integer id,
            @RequestParam("files") List<MultipartFile> files) {
        String uploaderId = SecurityUtils.getCurrentUsername().orElse("admin");
        return ApiResponse.<List<HotelImageResponse>>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tải lên ảnh hạng phòng thành công")
                .result(hotelRoomTypeMediaService.uploadImages(id, files, uploaderId))
                .build();
    }

    @PatchMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Đặt ảnh làm ảnh đại diện chính của hạng phòng")
    public ApiResponse<Void> setPrimaryImage(
            @PathVariable Integer id,
            @PathVariable Long imageId) {
        hotelRoomTypeMediaService.setPrimaryImage(id, imageId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Đã đặt làm ảnh đại diện chính của hạng phòng")
                .build();
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa ảnh của hạng phòng")
    public ApiResponse<Void> deleteImage(
            @PathVariable Integer id,
            @PathVariable Long imageId) {
        hotelRoomTypeMediaService.deleteImage(id, imageId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa ảnh hạng phòng thành công")
                .build();
    }
}
