package vn.edu.utc.hotel_booking.common.storage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.CategoryMetaResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaItemResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaSearchDto;
import vn.edu.utc.hotel_booking.common.storage.service.MediaManagementService;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Storage - Media Management Center", description = "Trung tâm quản trị đa phương tiện và hình ảnh toàn chuỗi")
public class MediaManagementController {

    MediaManagementService mediaManagementService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tìm kiếm & phân trang kho đa phương tiện toàn hệ thống")
    public ApiResponse<PageResponse<MediaItemResponse>> filterMedia(
            @RequestBody(required = false) MediaSearchDto searchDto) {
        MediaSearchDto criteria = searchDto != null ? searchDto : new MediaSearchDto();
        return ApiResponse.<PageResponse<MediaItemResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Tra cứu danh sách tệp tin thành công")
                .result(mediaManagementService.filterMedia(criteria))
                .build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tải lên một hoặc nhiều tệp hình ảnh vào danh mục chỉ định")
    public ApiResponse<List<MediaItemResponse>> uploadMedia(
            @RequestParam("category") StorageCategory category,
            @RequestParam(value = "hotelId", required = false) Short hotelId,
            @RequestParam(value = "referenceId", required = false) Long referenceId,
            @RequestParam("files") List<MultipartFile> files) {
        String uploaderId = SecurityUtils.getCurrentUsername().orElse("admin");
        List<MediaItemResponse> result = mediaManagementService.uploadMedia(category, hotelId, referenceId, files, uploaderId);
        return ApiResponse.<List<MediaItemResponse>>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tải lên hình ảnh thành công")
                .result(result)
                .build();
    }

    @PatchMapping("/{id}/primary")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Đặt ảnh làm ảnh đại diện chính của đối tượng")
    public ApiResponse<Void> setPrimary(@PathVariable Long id) {
        mediaManagementService.setPrimary(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Đặt làm ảnh đại diện chính thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa tệp đa phương tiện")
    public ApiResponse<Void> deleteMedia(@PathVariable Long id) {
        mediaManagementService.deleteMedia(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa hình ảnh thành công")
                .build();
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách các phân loại danh mục đa phương tiện được hỗ trợ")
    public ApiResponse<List<CategoryMetaResponse>> getAvailableCategories() {
        return ApiResponse.<List<CategoryMetaResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh mục đa phương tiện thành công")
                .result(mediaManagementService.getAvailableCategories())
                .build();
    }
}

