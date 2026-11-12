package vn.edu.utc.hotel_booking.modules.pricing.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VoucherMapper {

    @Mapping(target = "hotelName", ignore = true)
    @Mapping(target = "remainingQuantity", expression = "java(voucher.getTotalQuantity() != null && voucher.getUsedQuantity() != null ? Math.max(0, voucher.getTotalQuantity() - voucher.getUsedQuantity()) : 0)")
    VoucherResponse toResponse(Voucher voucher);

    List<VoucherResponse> toResponseList(List<Voucher> vouchers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedQuantity", constant = "0")
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Voucher toEntity(VoucherCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "voucherCode", ignore = true)
    @Mapping(target = "usedQuantity", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Voucher voucher, VoucherUpdateRequest request);
}

