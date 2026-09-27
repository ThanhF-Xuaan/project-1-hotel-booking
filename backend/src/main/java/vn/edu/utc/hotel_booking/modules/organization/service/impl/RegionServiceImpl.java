package vn.edu.utc.hotel_booking.modules.organization.service.impl;

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
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.RegionResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
import vn.edu.utc.hotel_booking.modules.organization.mapper.RegionMapper;
import vn.edu.utc.hotel_booking.modules.organization.repository.RegionRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.RegionService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    @Override
    public PageResponse<RegionResponse> filter(RegionSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<Region> resultPage = regionRepository.searchRegions(keyword, status, pageable);
        return PageResponse.from(resultPage.map(regionMapper::toResponse));
    }

    @Override
    public RegionResponse getById(Short id) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.REGION_NOT_FOUND));
        return regionMapper.toResponse(region);
    }

    @Override
    @Transactional
    public RegionResponse create(RegionCreateRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (regionRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new AppException(ErrorCode.REGION_CODE_ALREADY_EXISTS);
        }

        Region region = regionMapper.toEntity(request);
        region.setCode(code);
        region.setName(request.getName().trim());
        if (request.getDescription() != null) {
            region.setDescription(request.getDescription().trim());
        }

        Region saved = regionRepository.save(region);
        log.info("Đã tạo mới khu vực: code={}, id={}", saved.getCode(), saved.getId());
        return regionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RegionResponse update(Short id, RegionUpdateRequest request) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.REGION_NOT_FOUND));

        regionMapper.updateEntity(region, request);
        if (StringUtils.hasText(request.getName())) {
            region.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            region.setDescription(request.getDescription().trim());
        }
        if (StringUtils.hasText(request.getStatus())) {
            region.setStatus(request.getStatus().trim());
        }

        Region updated = regionRepository.save(region);
        log.info("Đã cập nhật khu vực: id={}, name={}", updated.getId(), updated.getName());
        return regionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = regionRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} khu vực với danh sách IDs: {}", count, ids);
    }
}
