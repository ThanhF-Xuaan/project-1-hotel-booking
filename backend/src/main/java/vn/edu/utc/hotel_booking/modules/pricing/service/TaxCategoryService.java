package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategorySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.TaxCategoryResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;

import java.util.List;

public interface TaxCategoryService {

    TaxCategory getEntityById(Integer id);

    TaxCategoryResponse getById(Integer id);

    TaxCategoryResponse create(TaxCategoryCreateRequest request);

    TaxCategoryResponse update(Integer id, TaxCategoryUpdateRequest request);

    PageResponse<TaxCategoryResponse> filter(TaxCategorySearchDto searchDto);

    void deleteBatch(List<Integer> ids);

    List<TaxCategoryResponse> getAllActive();
}
