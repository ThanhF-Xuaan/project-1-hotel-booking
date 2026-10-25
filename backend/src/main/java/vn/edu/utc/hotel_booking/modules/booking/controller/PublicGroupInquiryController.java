package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquiryCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupInquiryResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupInquiryService;

@RestController
@RequestMapping("/api/v1/public/group-inquiries")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public Group Inquiry Controller", description = "Tiếp nhận yêu cầu báo giá đặt phòng khách đoàn từ Portal công khai")
public class PublicGroupInquiryController {

    GroupInquiryService groupInquiryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gửi yêu cầu báo giá đặt phòng đoàn (Khách đoàn >= 6 phòng)")
    public ApiResponse<GroupInquiryResponse> submitInquiry(@Valid @RequestBody GroupInquiryCreateRequest request) {
        return ApiResponse.success("Gửi yêu cầu báo giá phòng đoàn thành công", groupInquiryService.createInquiry(request));
    }
}
