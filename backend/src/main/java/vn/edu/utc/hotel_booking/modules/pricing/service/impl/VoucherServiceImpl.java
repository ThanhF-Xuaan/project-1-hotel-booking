package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherValidateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherUsageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherValidationResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VoucherUsage;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.VoucherMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherUsageRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.VoucherService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherServiceImpl implements VoucherService {

    VoucherRepository voucherRepository;
    VoucherUsageRepository voucherUsageRepository;
    HotelService hotelService;
    VoucherMapper voucherMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "voucherCode", "title", "discountValue", "validFrom", "validTo", "status", "usedQuantity", "totalQuantity"
    );

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherCreateRequest request) {
        String normalizedCode = request.getVoucherCode().trim().toUpperCase();

        if (request.getValidFrom().isAfter(request.getValidTo())) {
            throw new AppException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        if (voucherRepository.existsByVoucherCodeAndIsDeletedFalse(normalizedCode)) {
            throw new AppException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
        }

        if ("PERCENT".equalsIgnoreCase(request.getDiscountType())
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Giá trị giảm giá theo phần trăm không được vượt quá 100%");
        }

        String hotelName = "Toàn chuỗi áp dụng";
        if (request.getHotelId() != null) {
            HotelResponse hotel = hotelService.getById(request.getHotelId());
            if (hotel != null) {
                hotelName = hotel.getName();
            }
        }

        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setVoucherCode(normalizedCode);
        Voucher saved = voucherRepository.save(voucher);

        VoucherResponse response = voucherMapper.toResponse(saved);
        response.setHotelName(hotelName);
        log.info("Created new voucher with ID {} and code {}", saved.getId(), saved.getVoucherCode());
        return response;
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Integer id, VoucherUpdateRequest request) {
        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        if (request.getValidFrom().isAfter(request.getValidTo())) {
            throw new AppException(ErrorCode.INVALID_VOUCHER_DATES);
        }

        if ("PERCENT".equalsIgnoreCase(request.getDiscountType())
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Giá trị giảm giá theo phần trăm không được vượt quá 100%");
        }

        String hotelName = "Toàn chuỗi áp dụng";
        if (request.getHotelId() != null) {
            HotelResponse hotel = hotelService.getById(request.getHotelId());
            if (hotel != null) {
                hotelName = hotel.getName();
            }
        }

        voucherMapper.updateEntity(voucher, request);
        Voucher updated = voucherRepository.save(voucher);

        VoucherResponse response = voucherMapper.toResponse(updated);
        response.setHotelName(hotelName);
        log.info("Updated voucher ID {}", id);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Integer id) {
        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        VoucherResponse response = voucherMapper.toResponse(voucher);
        if (voucher.getHotelId() != null) {
            try {
                HotelResponse hotel = hotelService.getById(voucher.getHotelId());
                if (hotel != null) {
                    response.setHotelName(hotel.getName());
                }
            } catch (Exception e) {
                log.debug("Hotel ID {} not found for voucher ID {}", voucher.getHotelId(), id);
            }
        } else {
            response.setHotelName("Toàn chuỗi áp dụng");
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherUsageResponse> getVoucherUsages(Integer voucherId) {
        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        return voucherUsageRepository.findByVoucherId(voucherId).stream()
                .map(u -> VoucherUsageResponse.builder()
                        .id(u.getId())
                        .voucherId(voucherId)
                        .voucherCode(voucher.getVoucherCode())
                        .bookingGuestId(u.getBookingGuest() != null ? u.getBookingGuest().getId() : null)
                        .guestFullName(u.getBookingGuest() != null ? u.getBookingGuest().getFullName() : null)
                        .guestPhoneNumber(u.getBookingGuest() != null ? u.getBookingGuest().getPhone() : null)
                        .discountAmount(u.getDiscountAmount())
                        .usedAt(u.getUsedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> filterVouchers(VoucherSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() >= 0 ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        String sortBy = ALLOWED_SORT_FIELDS.contains(searchDto.getSortBy()) ? searchDto.getSortBy() : "id";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        String keyword = searchDto.getKeyword() != null && !searchDto.getKeyword().trim().isBlank()
                ? searchDto.getKeyword().trim()
                : null;
        String discountType = searchDto.getDiscountType() != null && !searchDto.getDiscountType().trim().isBlank()
                ? searchDto.getDiscountType().trim()
                : null;
        String applyScope = searchDto.getApplyScope() != null && !searchDto.getApplyScope().trim().isBlank()
                ? searchDto.getApplyScope().trim()
                : null;
        String status = searchDto.getStatus() != null && !searchDto.getStatus().trim().isBlank()
                ? searchDto.getStatus().trim()
                : null;

        Page<Voucher> voucherPage = voucherRepository.filterVouchers(
                keyword,
                searchDto.getHotelId(),
                discountType,
                applyScope,
                status,
                searchDto.getActiveAt(),
                pageable
        );

        List<VoucherResponse> content = voucherPage.getContent().stream()
                .map(v -> {
                    VoucherResponse res = voucherMapper.toResponse(v);
                    if (v.getHotelId() != null) {
                        try {
                            HotelResponse hotel = hotelService.getById(v.getHotelId());
                            if (hotel != null) {
                                res.setHotelName(hotel.getName());
                            }
                        } catch (Exception ignored) {
                        }
                    } else {
                        res.setHotelName("Toàn chuỗi áp dụng");
                    }
                    return res;
                })
                .toList();

        return PageResponse.<VoucherResponse>builder()
                .content(content)
                .pageNumber(voucherPage.getNumber())
                .pageSize(voucherPage.getSize())
                .totalElements(voucherPage.getTotalElements())
                .totalPages(voucherPage.getTotalPages())
                .last(voucherPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void deleteVouchers(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        voucherRepository.softDeleteByIds(ids);
        log.info("Soft-deleted vouchers with IDs: {}", ids);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(VoucherValidateRequest request) {
        if (request.getVoucherCode() == null || request.getVoucherCode().trim().isBlank()) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .message("Vui lòng cung cấp mã giảm giá")
                    .build();
        }

        String normalizedCode = request.getVoucherCode().trim().toUpperCase();
        OffsetDateTime now = OffsetDateTime.now();

        Optional<Voucher> validVoucherOpt = voucherRepository.findValidVoucher(normalizedCode, request.getHotelId(), now);

        if (validVoucherOpt.isEmpty()) {
            Optional<Voucher> existingOpt = voucherRepository.findByVoucherCodeAndIsDeletedFalse(normalizedCode);
            if (existingOpt.isEmpty()) {
                return VoucherValidationResponse.builder()
                        .valid(false)
                        .voucherCode(normalizedCode)
                        .message("Mã giảm giá \"" + normalizedCode + "\" không tồn tại trên hệ thống")
                        .build();
            }

            Voucher v = existingOpt.get();
            if ("INACTIVE".equalsIgnoreCase(v.getStatus())) {
                return VoucherValidationResponse.builder()
                        .valid(false)
                        .voucherCode(normalizedCode)
                        .message("Mã giảm giá đang tạm ngưng áp dụng")
                        .build();
            }
            if (v.getValidFrom().isAfter(now)) {
                return VoucherValidationResponse.builder()
                        .valid(false)
                        .voucherCode(normalizedCode)
                        .message("Chương trình ưu đãi của mã này chưa bắt đầu hiệu lực")
                        .build();
            }
            if (v.getValidTo().isBefore(now) || "EXPIRED".equalsIgnoreCase(v.getStatus())) {
                return VoucherValidationResponse.builder()
                        .valid(false)
                        .voucherCode(normalizedCode)
                        .message("Mã giảm giá đã hết hạn sử dụng")
                        .build();
            }
            if (v.getTotalQuantity() != null && v.getUsedQuantity() != null && v.getUsedQuantity() >= v.getTotalQuantity()) {
                return VoucherValidationResponse.builder()
                        .valid(false)
                        .voucherCode(normalizedCode)
                        .message("Mã giảm giá đã hết lượt phát hành")
                        .build();
            }
            if (v.getHotelId() != null && request.getHotelId() != null && !v.getHotelId().equals(request.getHotelId())) {
                return VoucherValidationResponse.builder()
                        .valid(false)
                        .voucherCode(normalizedCode)
                        .message("Mã giảm giá không áp dụng cho khách sạn này")
                        .build();
            }

            return VoucherValidationResponse.builder()
                    .valid(false)
                    .voucherCode(normalizedCode)
                    .message("Mã giảm giá không thỏa mãn điều kiện áp dụng")
                    .build();
        }

        Voucher voucher = validVoucherOpt.get();

        // Kiểm tra minOrderAmount
        BigDecimal orderAmount = request.getOrderAmount() != null ? request.getOrderAmount() : BigDecimal.ZERO;
        if (voucher.getMinOrderAmount() != null && orderAmount.compareTo(voucher.getMinOrderAmount()) < 0) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .voucherCode(normalizedCode)
                    .title(voucher.getTitle())
                    .message("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinOrderAmount().toBigInteger() + " ₫ để áp dụng mã này")
                    .build();
        }

        // Kiểm tra applyScope
        if (request.getApplyScope() != null
                && !"TOTAL_BOOKING".equalsIgnoreCase(voucher.getApplyScope())
                && !request.getApplyScope().equalsIgnoreCase(voucher.getApplyScope())) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .voucherCode(normalizedCode)
                    .title(voucher.getTitle())
                    .message("Mã ưu đãi chỉ áp dụng cho phạm vi: " + voucher.getApplyScope())
                    .build();
        }

        // Kiểm tra lượt dùng của khách (nếu có bookingGuestId)
        if (request.getBookingGuestId() != null && voucher.getMaxUsagePerUser() != null) {
            long usedCount = voucherUsageRepository.countByVoucherIdAndBookingGuestId(voucher.getId(), request.getBookingGuestId());
            if (usedCount >= voucher.getMaxUsagePerUser()) {
                return VoucherValidationResponse.builder()
                        .valid(false)
                        .voucherCode(normalizedCode)
                        .title(voucher.getTitle())
                        .message("Bạn đã sử dụng hết số lượt cho phép (" + voucher.getMaxUsagePerUser() + " lượt) của mã giảm giá này")
                        .build();
            }
        }

        // Tính toán số tiền chiết khấu
        BigDecimal discountAmt = BigDecimal.ZERO;
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmt = orderAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null && discountAmt.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discountAmt = voucher.getMaxDiscountAmount();
            }
        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmt = voucher.getDiscountValue();
        }

        if (discountAmt.compareTo(orderAmount) > 0) {
            discountAmt = orderAmount;
        }

        BigDecimal finalAmount = orderAmount.subtract(discountAmt);

        return VoucherValidationResponse.builder()
                .valid(true)
                .message("Áp dụng mã giảm giá thành công")
                .voucherCode(voucher.getVoucherCode())
                .title(voucher.getTitle())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .discountAmount(discountAmt)
                .finalAmount(finalAmount)
                .applyScope(voucher.getApplyScope())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> findValidVoucherForPricing(String code, Short hotelId, OffsetDateTime now) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return voucherRepository.findValidVoucher(code.trim().toUpperCase(), hotelId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return voucherRepository.findByVoucherCodeAndIsDeletedFalse(code.trim().toUpperCase());
    }

    @Override
    @Transactional
    public void recordVoucherUsage(String voucherCode, BookingGuest guest, BigDecimal discountAmount) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return;
        }
        voucherRepository.findByVoucherCodeAndIsDeletedFalse(voucherCode.trim().toUpperCase()).ifPresent(v -> {
            v.setUsedQuantity(v.getUsedQuantity() != null ? v.getUsedQuantity() + 1 : 1);
            if (v.getTotalQuantity() != null && v.getUsedQuantity() >= v.getTotalQuantity()) {
                v.setStatus("EXPIRED");
            }
            voucherRepository.save(v);

            VoucherUsage usage = VoucherUsage.builder()
                    .voucher(v)
                    .bookingGuest(guest)
                    .discountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                    .usedAt(OffsetDateTime.now())
                    .build();
            voucherUsageRepository.save(usage);
            log.info("Recorded voucher usage for code {} by guest ID {}", voucherCode, guest != null ? guest.getId() : "null");
        });
    }
}
