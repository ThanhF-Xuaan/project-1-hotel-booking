package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherValidateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherUsageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherValidationResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherService {

    VoucherResponse createVoucher(VoucherCreateRequest request);

    VoucherResponse updateVoucher(Integer id, VoucherUpdateRequest request);

    VoucherResponse getVoucherById(Integer id);

    List<VoucherUsageResponse> getVoucherUsages(Integer voucherId);

    PageResponse<VoucherResponse> filterVouchers(VoucherSearchDto searchDto);

    void deleteVouchers(List<Integer> ids);

    VoucherValidationResponse validateVoucher(VoucherValidateRequest request);

    Optional<Voucher> findValidVoucherForPricing(String code, Short hotelId, OffsetDateTime now);

    Optional<Voucher> findByCode(String code);

    void recordVoucherUsage(String voucherCode, BookingGuest guest, BigDecimal discountAmount);
}
