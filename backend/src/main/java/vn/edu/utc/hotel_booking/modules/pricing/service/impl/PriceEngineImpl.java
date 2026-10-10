package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.AppliedAdjustmentDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DailyPriceDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.ServiceItemDto;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServiceItemContext;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;
import vn.edu.utc.hotel_booking.modules.pricing.service.PricingPipelineFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PriceEngineImpl implements PriceEngine {

    HotelRoomTypeRepository hotelRoomTypeRepository;
    PricingPipelineFactory pricingPipelineFactory;

    @Override
    public PriceBreakdownDto calculatePrice(PriceCalculationRequest request) {
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null
                || !request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày check-out phải sau ngày check-in");
        }

        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                        "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        // Check capacity
        int totalGuests = (request.getAdults() != null ? request.getAdults() : 0)
                + (request.getChildren() != null ? request.getChildren() : 0);
        if (totalGuests > hotelRoomType.getMaxTotalGuests()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Tổng số khách (" + totalGuests + ") vượt quá sức chứa tối đa (" + hotelRoomType.getMaxTotalGuests() + ")");
        }
        if (request.getExtraBeds() != null && request.getExtraBeds() > hotelRoomType.getExtraBeds()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Số giường phụ (" + request.getExtraBeds() + ") vượt quá số lượng cho phép (" + hotelRoomType.getExtraBeds() + ")");
        }

        BigDecimal serviceFeePercent = BigDecimal.valueOf(5.00);
        if (hotelRoomType.getHotel() != null && hotelRoomType.getHotel().getServiceFeePercent() != null) {
            serviceFeePercent = hotelRoomType.getHotel().getServiceFeePercent();
        }

        // Khởi tạo PricingContext
        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(hotelRoomType)
                .hotel(hotelRoomType.getHotel())
                .serviceFeePercent(serviceFeePercent)
                .build();

        // Thực thi Pipeline 6 bước
        PricingHandler pipeline = pricingPipelineFactory.createFullBookingPipeline();
        pipeline.handle(context);

        // Map kết quả sang DTO
        List<DailyPriceDto> dailyPriceDtos = new ArrayList<>();
        if (context.getDailyRates() != null) {
            for (DailyRateContext d : context.getDailyRates()) {
                List<AppliedAdjustmentDto> appliedAdjustmentDtos = new ArrayList<>();
                if (d.getAppliedAdjustments() != null) {
                    for (var a : d.getAppliedAdjustments()) {
                        appliedAdjustmentDtos.add(AppliedAdjustmentDto.builder()
                                .ruleCode(a.getRuleCode())
                                .ruleName(a.getRuleName())
                                .adjustmentType(a.getAdjustmentType())
                                .adjustmentValue(a.getAdjustmentValue())
                                .appliedAmount(a.getAppliedAmount())
                                .build());
                    }
                }

                dailyPriceDtos.add(DailyPriceDto.builder()
                        .date(d.getDate())
                        .baseRate(d.getBaseRate())
                        .surchargeAmount(d.getSurchargeAmount())
                        .rateAfterSurcharge(d.getRateAfterSurcharge())
                        .rateAdjustmentAmount(d.getRateAdjustmentAmount())
                        .appliedAdjustments(appliedAdjustmentDtos)
                        .seasonalAdjustment(d.getSeasonalAdjustment())
                        .holidayAdjustment(d.getHolidayAdjustment())
                        .adjustedRate(d.getAdjustedRate())
                        .autoDiscountAmount(d.getAutoDiscountAmount())
                        .voucherDiscountAmount(d.getVoucherDiscountAmount())
                        .discountAmount(d.getTotalDiscountAmount())
                        .netAmount(d.getNetRoomRate())
                        .serviceFeeAmount(d.getServiceFeeAmount())
                        .roomRateWithServiceFee(d.getRoomRateWithServiceFee())
                        .vatPercent(d.getVatPercent())
                        .vatAmount(d.getVatAmount())
                        .grossDailyTotal(d.getGrossDailyTotal())
                        .build());
            }
        }

        List<ServiceItemDto> serviceItemDtos = new ArrayList<>();
        if (context.getServiceItems() != null) {
            for (ServiceItemContext s : context.getServiceItems()) {
                serviceItemDtos.add(ServiceItemDto.builder()
                        .serviceId(s.getServiceId())
                        .serviceName(s.getServiceName())
                        .pricingType(s.getPricingType())
                        .unitPrice(s.getUnitPrice())
                        .quantity(s.getQuantity())
                        .subtotal(s.getSubtotal())
                        .serviceFeeAmount(s.getServiceFeeAmount())
                        .vatPercent(s.getVatPercent())
                        .vatAmount(s.getVatAmount())
                        .grossTotal(s.getGrossServiceTotal())
                        .build());
            }
        }

        String roomTypeName = hotelRoomType.getRoomType() != null
                ? hotelRoomType.getRoomType().getName()
                : "Phòng tiêu chuẩn";

        String voucherCode = context.getAppliedVoucher() != null
                ? context.getAppliedVoucher().getVoucherCode()
                : request.getVoucherCode();

        String promoName = context.getAppliedPromotionName() != null
                ? context.getAppliedPromotionName()
                : (context.getAppliedVoucher() != null ? context.getAppliedVoucher().getTitle() : null);

        var sum = context.getSummary();

        return PriceBreakdownDto.builder()
                .hotelRoomTypeId(hotelRoomType.getId())
                .roomTypeName(roomTypeName)
                .totalNights(dailyPriceDtos.size())
                .appliedVoucherCode(voucherCode)
                .appliedPromotionName(promoName)
                .dailyPrices(dailyPriceDtos)
                .serviceItems(serviceItemDtos)
                .totalBasePrice(sum != null ? sum.getTotalBasePrice() : BigDecimal.ZERO)
                .totalSurchargeAmount(sum != null ? sum.getTotalSurchargeAmount() : BigDecimal.ZERO)
                .totalRateAdjustmentAmount(sum != null && sum.getTotalRateAdjustmentAmount() != null
                        ? sum.getTotalRateAdjustmentAmount()
                        : (sum != null ? sum.getTotalSeasonalAdjustment().add(sum.getTotalHolidayAdjustment()) : BigDecimal.ZERO))
                .totalSeasonalAdjustment(sum != null ? sum.getTotalSeasonalAdjustment().add(sum.getTotalHolidayAdjustment()) : BigDecimal.ZERO)
                .totalDiscountAmount(sum != null ? sum.getTotalDiscountAmount() : BigDecimal.ZERO)
                .totalRoomNetAmount(sum != null ? sum.getTotalRoomNetAmount() : BigDecimal.ZERO)
                .totalRoomServiceFee(sum != null ? sum.getTotalRoomServiceFee() : BigDecimal.ZERO)
                .totalRoomVat(sum != null ? sum.getTotalRoomVat() : BigDecimal.ZERO)
                .totalRoomGrossAmount(sum != null ? sum.getTotalRoomGrossAmount() : BigDecimal.ZERO)
                .totalServiceSubtotal(sum != null ? sum.getTotalServiceSubtotal() : BigDecimal.ZERO)
                .totalServiceFee(sum != null ? sum.getTotalServiceFee() : BigDecimal.ZERO)
                .totalServiceVat(sum != null ? sum.getTotalServiceVat() : BigDecimal.ZERO)
                .totalServiceGrossAmount(sum != null ? sum.getTotalServiceGrossAmount() : BigDecimal.ZERO)
                .preTaxAmount(sum != null ? sum.getTotalRoomNetAmount() : BigDecimal.ZERO)
                .vatAmount(sum != null ? sum.getTotalRoomVat() : BigDecimal.ZERO)
                .vatPercent(dailyPriceDtos.isEmpty() ? BigDecimal.ZERO : dailyPriceDtos.get(0).getVatPercent())
                .finalTotalAmount(sum != null ? sum.getGrandTotal() : BigDecimal.ZERO)
                .build();
    }
}
