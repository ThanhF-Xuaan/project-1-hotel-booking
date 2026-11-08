package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomFeatureResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomFeatureMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomFeatureRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomFeatureService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomFeatureServiceImpl implements RoomFeatureService {

    RoomFeatureRepository roomFeatureRepository;
    RoomFeatureMapper roomFeatureMapper;

    @Override
    @Transactional
    public RoomFeatureResponse create(RoomFeatureCreateRequest request) {
        if (roomFeatureRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.ROOM_FEATURE_CODE_ALREADY_EXISTS,
                    "Mã tiện ích đã tồn tại: " + request.getCode());
        }

        RoomFeature roomFeature = roomFeatureMapper.toEntity(request);
        RoomFeature saved = roomFeatureRepository.save(roomFeature);
        return roomFeatureMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoomFeatureResponse update(Short id, RoomFeatureUpdateRequest request) {
        RoomFeature roomFeature = roomFeatureRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_FEATURE_NOT_FOUND,
                        "Không tìm thấy tiện ích phòng id: " + id));

        roomFeatureMapper.updateEntity(roomFeature, request);
        RoomFeature updated = roomFeatureRepository.save(roomFeature);
        return roomFeatureMapper.toResponse(updated);
    }

    @Override
    public RoomFeatureResponse getById(Short id) {
        RoomFeature roomFeature = roomFeatureRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_FEATURE_NOT_FOUND,
                        "Không tìm thấy tiện ích phòng id: " + id));
        return roomFeatureMapper.toResponse(roomFeature);
    }

    @Override
    public RoomFeature getEntityById(Short id) {
        return roomFeatureRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_FEATURE_NOT_FOUND,
                        "Không tìm thấy tiện ích phòng id: " + id));
    }

    @Override
    public Set<RoomFeature> getEntitiesByIds(Set<Short> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return roomFeatureRepository.findByIdInAndIsDeletedFalse(ids);
    }

    @Override
    public PageResponse<RoomFeatureResponse> filter(RoomFeatureSearchDto searchDto) {
        int page = (searchDto.getPage() != null && searchDto.getPage() >= 0) ? searchDto.getPage() : 0;
        int pageSize = (searchDto.getPageSize() != null && searchDto.getPageSize() > 0) ? searchDto.getPageSize() : 10;
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : "";
        String category = StringUtils.hasText(searchDto.getCategory()) ? searchDto.getCategory().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<RoomFeature> pageResult = roomFeatureRepository.filterRoomFeatures(
                keyword,
                category,
                status,
                pageable
        );

        return PageResponse.<RoomFeatureResponse>builder()
                .content(roomFeatureMapper.toResponseList(pageResult.getContent()))
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .empty(pageResult.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public void deleteBatch(List<Short> ids) {
        if (ids != null && !ids.isEmpty()) {
            roomFeatureRepository.softDeleteByIds(ids);
        }
    }
}

