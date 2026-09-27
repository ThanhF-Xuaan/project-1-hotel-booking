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
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
import vn.edu.utc.hotel_booking.modules.organization.mapper.HotelMapper;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.organization.repository.RegionRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RegionRepository regionRepository;
    private final HotelMapper hotelMapper;

    @Override
    public PageResponse<HotelResponse> filter(HotelSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<Hotel> resultPage = hotelRepository.searchHotels(searchDto.getRegionId(), keyword, status, pageable);
        return PageResponse.from(resultPage.map(hotelMapper::toResponse));
    }

    @Override
    public HotelResponse getById(Short id) {
        Hotel hotel = hotelRepository.findByIdWithRegion(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        return hotelMapper.toResponse(hotel);
    }

    @Override
    @Transactional
    public HotelResponse create(HotelCreateRequest request) {
        Region region = regionRepository.findByIdAndIsDeletedFalse(request.getRegionId())
                .orElseThrow(() -> new AppException(ErrorCode.REGION_NOT_FOUND));

        String name = request.getName().trim();
        if (hotelRepository.existsByNameAndRegionIdAndIsDeletedFalse(name, region.getId())) {
            throw new AppException(ErrorCode.HOTEL_NAME_ALREADY_EXISTS);
        }

        Hotel hotel = hotelMapper.toEntity(request);
        hotel.setRegion(region);
        hotel.setName(name);
        hotel.setAddress(request.getAddress().trim());
        if (request.getPhone() != null) {
            hotel.setPhone(request.getPhone().trim());
        }

        Hotel saved = hotelRepository.save(hotel);
        log.info("Đã tạo mới khách sạn: id={}, name={}, regionId={}", saved.getId(), saved.getName(), region.getId());
        return hotelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HotelResponse update(Short id, HotelUpdateRequest request) {
        Hotel hotel = hotelRepository.findByIdWithRegion(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));

        Region region = regionRepository.findByIdAndIsDeletedFalse(request.getRegionId())
                .orElseThrow(() -> new AppException(ErrorCode.REGION_NOT_FOUND));

        String name = request.getName().trim();
        if (hotelRepository.existsByNameAndRegionIdAndIdNotAndIsDeletedFalse(name, region.getId(), id)) {
            throw new AppException(ErrorCode.HOTEL_NAME_ALREADY_EXISTS);
        }

        hotelMapper.updateEntity(hotel, request);
        hotel.setRegion(region);
        hotel.setName(name);
        hotel.setAddress(request.getAddress().trim());
        if (request.getPhone() != null) {
            hotel.setPhone(request.getPhone().trim());
        }

        Hotel updated = hotelRepository.save(hotel);
        log.info("Đã cập nhật khách sạn: id={}, name={}", updated.getId(), updated.getName());
        return hotelMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = hotelRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} khách sạn với danh sách IDs: {}", count, ids);
    }
}
