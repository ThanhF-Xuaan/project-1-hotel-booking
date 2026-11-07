package vn.edu.utc.hotel_booking.modules.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelMediaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/images")
@RequiredArgsConstructor
@Tag(name = "Organization - Hotel Media", description = "Quản lý ảnh cơ sở khách sạn")
public class HotelMediaController {

    private final HotelMediaService hotelMediaService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tải lên một hoặc nhiều ảnh cơ sở khách sạn")
    public ApiResponse<List<HotelImageResponse>> uploadImages(
            @PathVariable Short hotelId,
            @RequestParam("files") List<MultipartFile> files) {
        String uploaderId = SecurityUtils.getCurrentUsername().orElse("admin");
        return ApiResponse.<List<HotelImageResponse>>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tải lên ảnh khách sạn thành công")
                .result(hotelMediaService.uploadImages(hotelId, files, uploaderId))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lấy danh sách ảnh cơ sở của khách sạn")
    public ApiResponse<List<HotelImageResponse>> getImages(@PathVariable Short hotelId) {
        return ApiResponse.<List<HotelImageResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách ảnh thành công")
                .result(hotelMediaService.getHotelImages(hotelId))
                .build();
    }

    @PatchMapping("/{imageId}/primary")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Đặt ảnh làm ảnh đại diện chính của khách sạn")
    public ApiResponse<Void> setPrimaryImage(
            @PathVariable Short hotelId,
            @PathVariable Long imageId) {
        hotelMediaService.setPrimaryImage(hotelId, imageId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Đã đặt làm ảnh đại diện chính")
                .build();
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa ảnh cơ sở khách sạn")
    public ApiResponse<Void> deleteImage(
            @PathVariable Short hotelId,
            @PathVariable Long imageId) {
        hotelMediaService.deleteImage(hotelId, imageId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa ảnh khách sạn thành công")
                .build();
    }
}

