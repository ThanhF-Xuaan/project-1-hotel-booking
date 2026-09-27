package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanySearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.CompanyResponse;

import java.util.List;

public interface CompanyService {

    PageResponse<CompanyResponse> filter(CompanySearchDto searchDto);

    CompanyResponse getById(Long id);

    CompanyResponse create(CompanyCreateRequest request);

    CompanyResponse update(Long id, CompanyUpdateRequest request);

    void deleteBatch(List<Long> ids);
}
