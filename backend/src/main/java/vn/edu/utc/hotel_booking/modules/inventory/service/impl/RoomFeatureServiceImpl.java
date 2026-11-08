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
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        Page<RoomFeature> page = roomFeatureRepository.filterRoomFeatures(
                keyword,
                searchDto.getCategory(),
                searchDto.getStatus(),
                pageable
        );

        return PageResponse.<RoomFeatureResponse>builder()
                .content(roomFeatureMapper.toResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
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

