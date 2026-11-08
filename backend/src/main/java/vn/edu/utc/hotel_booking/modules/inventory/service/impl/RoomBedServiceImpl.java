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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomBedResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomBedMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomBedRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomBedService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomBedServiceImpl implements RoomBedService {

    RoomBedRepository roomBedRepository;
    RoomBedMapper roomBedMapper;

    @Override
    @Transactional
    public RoomBedResponse create(RoomBedCreateRequest request) {
        if (roomBedRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new AppException(ErrorCode.ROOM_BED_NAME_ALREADY_EXISTS,
                    "Tên loại giường đã tồn tại: " + request.getName());
        }

        RoomBed roomBed = roomBedMapper.toEntity(request);
        RoomBed saved = roomBedRepository.save(roomBed);
        return roomBedMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoomBedResponse update(Short id, RoomBedUpdateRequest request) {
        RoomBed roomBed = roomBedRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_BED_NOT_FOUND,
                        "Không tìm thấy loại giường id: " + id));

        if (roomBedRepository.existsByNameAndIdNotAndIsDeletedFalse(request.getName(), id)) {
            throw new AppException(ErrorCode.ROOM_BED_NAME_ALREADY_EXISTS,
                    "Tên loại giường đã tồn tại: " + request.getName());
        }

        roomBedMapper.updateEntity(roomBed, request);
        RoomBed updated = roomBedRepository.save(roomBed);
        return roomBedMapper.toResponse(updated);
    }

    @Override
    public RoomBedResponse getById(Short id) {
        RoomBed roomBed = roomBedRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_BED_NOT_FOUND,
                        "Không tìm thấy loại giường id: " + id));
        return roomBedMapper.toResponse(roomBed);
    }

    @Override
    public RoomBed getEntityById(Short id) {
        return roomBedRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_BED_NOT_FOUND,
                        "Không tìm thấy loại giường id: " + id));
    }

    @Override
    public PageResponse<RoomBedResponse> filter(RoomBedSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        Page<RoomBed> page = roomBedRepository.filterRoomBeds(
                keyword,
                searchDto.getStatus(),
                pageable
        );

        return PageResponse.<RoomBedResponse>builder()
                .content(roomBedMapper.toResponseList(page.getContent()))
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
            roomBedRepository.softDeleteByIds(ids);
        }
    }
}

