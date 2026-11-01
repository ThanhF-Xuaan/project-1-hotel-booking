package vn.edu.utc.hotel_booking.modules.ai.demo.forecast;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/ai/demo/occupancy")
@Profile("m1-demo & !prod & !production")
@ConditionalOnProperty(name = "hotel.ai.demo.forecast.enabled", havingValue = "true")
@PreAuthorize("hasRole('ROLE_CHAIN_ADMIN')")
public class OccupancyDemoController {
    private final OccupancyDemoService service;

    public OccupancyDemoController(OccupancyDemoService service) { this.service = service; }

    @GetMapping
    public ApiResponse<OccupancyDemoService.DemoResponse> replay(HttpServletRequest request) {
        if (request.getParameterMap().keySet().stream().anyMatch(key -> !"scenario".equals(key))
                || (request.getParameterValues("scenario") != null && request.getParameterValues("scenario").length != 1)) {
            throw new IllegalArgumentException("Invalid demo selectors");
        }
        String scenario = request.getParameter("scenario");
        return ApiResponse.success(service.replay(scenario == null ? "HOTEL_A" : scenario));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> invalidScenario() {
        return ResponseEntity.badRequest().body(ApiResponse.<Void>builder().code(400).message("Kịch bản demo không hợp lệ.").build());
    }
}
