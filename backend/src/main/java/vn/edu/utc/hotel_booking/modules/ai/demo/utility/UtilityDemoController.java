package vn.edu.utc.hotel_booking.modules.ai.demo.utility;

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
@RequestMapping("/api/v1/ai/demo/utility-anomalies")
@Profile("m1-demo & !prod & !production")
@ConditionalOnProperty(name = "hotel.ai.demo.utility.enabled", havingValue = "true")
@PreAuthorize("hasRole('ROLE_CHAIN_ADMIN')")
public class UtilityDemoController {
    private final UtilityDemoService service;
    public UtilityDemoController(UtilityDemoService service) { this.service = service; }

    @GetMapping
    public ApiResponse<UtilityDemoService.DemoResponse> replay(HttpServletRequest request) {
        if (request.getParameterMap().keySet().stream().anyMatch(key -> !"scenario".equals(key))
                || (request.getParameterValues("scenario") != null && request.getParameterValues("scenario").length != 1)) {
            throw new IllegalArgumentException("Invalid demo selectors");
        }
        String scenario = request.getParameter("scenario");
        return ApiResponse.success(service.replay(scenario == null ? "ELECTRIC_SPIKES" : scenario));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> invalidScenario() {
        return ResponseEntity.badRequest().body(ApiResponse.<Void>builder().code(400).message("Kịch bản demo không hợp lệ.").build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> unavailable() {
        return ResponseEntity.status(503).body(ApiResponse.<Void>builder().code(503).message("Dịch vụ demo tạm thời chưa sẵn sàng.").build());
    }
}
