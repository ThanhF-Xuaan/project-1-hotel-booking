package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VatRuleResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface VatRuleService {

    BigDecimal getActiveVatPercent(Integer taxCategoryId, LocalDate effectiveDate);

    VatRuleResponse getById(Integer id);

    VatRuleResponse create(VatRuleCreateRequest request);

    VatRuleResponse update(Integer id, VatRuleUpdateRequest request);

    PageResponse<VatRuleResponse> filter(VatRuleSearchDto searchDto);

    void deleteBatch(List<Integer> ids);
}
