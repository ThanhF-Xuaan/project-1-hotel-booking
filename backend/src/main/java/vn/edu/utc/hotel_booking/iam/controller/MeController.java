package vn.edu.utc.hotel_booking.iam.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Staff identity")
public class MeController {
    private final StaffAccessService access;

    public MeController(StaffAccessService access) {
        this.access = access;
    }

    @GetMapping
    @Operation(summary = "Current staff permissions and scope")
    public ApiResponse<StaffActor> me() {
        return ApiResponse.<StaffActor>builder().result(access.current()).build();
    }
}
