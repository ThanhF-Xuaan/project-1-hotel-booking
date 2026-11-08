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
        int page = (searchDto.getPage() != null && searchDto.getPage() >= 0) ? searchDto.getPage() : 0;
        int pageSize = (searchDto.getPageSize() != null && searchDto.getPageSize() > 0) ? searchDto.getPageSize() : 10;
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : "";
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<RoomBed> pageResult = roomBedRepository.filterRoomBeds(
                keyword,
                status,
                pageable
        );

        return PageResponse.<RoomBedResponse>builder()
                .content(roomBedMapper.toResponseList(pageResult.getContent()))
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
            roomBedRepository.softDeleteByIds(ids);
        }
    }
}

