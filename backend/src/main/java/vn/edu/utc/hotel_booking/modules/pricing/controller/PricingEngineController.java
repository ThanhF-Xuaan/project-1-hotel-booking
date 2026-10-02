package vn.edu.utc.hotel_booking.modules.pricing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Dynamic Engine", description = "Cỗ máy tính giá động đa tầng và bóc tách cấu phần chi phí")
public class PricingEngineController {

    PriceEngine priceEngine;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Tính toán báo giá chi tiết (Price Breakdown) cho loại phòng theo ngày và lượng khách")
    public ResponseEntity<ApiResponse<PriceBreakdownDto>> calculatePrice(
            @Valid @RequestBody PriceCalculationRequest request) {
        PriceBreakdownDto response = priceEngine.calculatePrice(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
