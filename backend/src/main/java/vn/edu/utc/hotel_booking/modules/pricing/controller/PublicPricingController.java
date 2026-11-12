package vn.edu.utc.hotel_booking.modules.pricing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherValidateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherValidationResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;
import vn.edu.utc.hotel_booking.modules.pricing.service.VoucherService;

@RestController
@RequestMapping("/api/v1/public/pricing")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public - Pricing Engine", description = "Public APIs tính toán báo giá chi tiết và kiểm tra voucher cho khách hàng trực tuyến")
public class PublicPricingController {

    PriceEngine priceEngine;
    VoucherService voucherService;

    @PostMapping("/calculate")
    @Operation(summary = "Tính toán báo giá công khai trước khi đặt phòng")
    public ResponseEntity<ApiResponse<PriceBreakdownDto>> calculatePrice(
            @Valid @RequestBody PriceCalculationRequest request) {
        PriceBreakdownDto response = priceEngine.calculatePrice(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/vouchers/validate")
    @Operation(summary = "Kiểm tra tính hợp lệ và số tiền chiết khấu của voucher")
    public ResponseEntity<ApiResponse<VoucherValidationResponse>> validateVoucher(
            @Valid @RequestBody VoucherValidateRequest request) {
        VoucherValidationResponse response = voucherService.validateVoucher(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
