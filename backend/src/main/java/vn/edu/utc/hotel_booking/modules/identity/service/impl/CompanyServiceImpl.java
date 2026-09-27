package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanySearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.CompanyResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.mapper.CompanyMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.CompanyRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.CompanyService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    public PageResponse<CompanyResponse> filter(CompanySearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<Company> resultPage = companyRepository.searchCompanies(keyword, status, pageable);
        return PageResponse.from(resultPage.map(companyMapper::toResponse));
    }

    @Override
    public CompanyResponse getById(Long id) {
        Company company = companyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        if (StringUtils.hasText(request.getTaxCode())) {
            String taxCode = request.getTaxCode().trim();
            if (companyRepository.existsByTaxCodeAndIsDeletedFalse(taxCode)) {
                throw new AppException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
            }
        }

        Company company = companyMapper.toEntity(request);
        company.setName(request.getName().trim());
        if (request.getTaxCode() != null) {
            company.setTaxCode(request.getTaxCode().trim());
        }

        Company saved = companyRepository.save(company);
        log.info("Đã tạo mới doanh nghiệp đối tác: id={}, name={}", saved.getId(), saved.getName());
        return companyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CompanyResponse update(Long id, CompanyUpdateRequest request) {
        Company company = companyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        if (StringUtils.hasText(request.getTaxCode())) {
            String taxCode = request.getTaxCode().trim();
            if (companyRepository.existsByTaxCodeAndIdNotAndIsDeletedFalse(taxCode, id)) {
                throw new AppException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
            }
            company.setTaxCode(taxCode);
        }

        companyMapper.updateEntity(company, request);
        company.setName(request.getName().trim());

        Company updated = companyRepository.save(company);
        log.info("Đã cập nhật doanh nghiệp đối tác: id={}, name={}", updated.getId(), updated.getName());
        return companyMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = companyRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} doanh nghiệp đối tác với danh sách IDs: {}", count, ids);
    }
}
