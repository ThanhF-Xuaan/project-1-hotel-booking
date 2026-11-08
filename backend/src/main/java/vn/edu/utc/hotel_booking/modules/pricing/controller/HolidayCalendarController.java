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
import vn.edu.utc.hotel_booking.common.annotation.Auditable;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HolidayCalendarResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.HolidayCalendarService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/holiday-calendars")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Holiday Calendars", description = "APIs quản lý danh mục ngày lễ tết và sự kiện cao điểm")
public class HolidayCalendarController {

    HolidayCalendarService holidayCalendarService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm & lọc danh sách ngày lễ tết")
    public ResponseEntity<ApiResponse<PageResponse<HolidayCalendarResponse>>> filterHolidays(
            @RequestBody HolidayCalendarSearchDto searchDto) {
        PageResponse<HolidayCalendarResponse> response = holidayCalendarService.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết ngày lễ theo ID")
    public ResponseEntity<ApiResponse<HolidayCalendarResponse>> getHolidayById(@PathVariable Integer id) {
        HolidayCalendarResponse response = holidayCalendarService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "HOLIDAY_CALENDAR_CREATE",
            entityName = "HolidayCalendar",
            description = "'Tạo mới ngày lễ: ' + #request.name + ' (' + #request.date + ')'",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới ngày lễ")
    public ResponseEntity<ApiResponse<HolidayCalendarResponse>> createHoliday(
            @Valid @RequestBody HolidayCalendarCreateRequest request) {
        HolidayCalendarResponse response = holidayCalendarService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "HOLIDAY_CALENDAR_UPDATE",
            entityName = "HolidayCalendar",
            entityId = "#id.toString()",
            description = "'Cập nhật ngày lễ ID ' + #id + ': ' + #request.name"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật ngày lễ")
    public ResponseEntity<ApiResponse<HolidayCalendarResponse>> updateHoliday(
            @PathVariable Integer id,
            @Valid @RequestBody HolidayCalendarUpdateRequest request) {
        HolidayCalendarResponse response = holidayCalendarService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "HOLIDAY_CALENDAR_DELETE",
            entityName = "HolidayCalendar",
            entityId = "#ids.toString()",
            description = "'Xóa các ngày lễ: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt ngày lễ (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteHolidays(@RequestBody List<Integer> ids) {
        holidayCalendarService.deleteBatch(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

