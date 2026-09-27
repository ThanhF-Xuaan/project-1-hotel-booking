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
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomTypeMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomTypeService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomTypeServiceImpl implements RoomTypeService {

    RoomTypeRepository roomTypeRepository;
    RoomTypeMapper roomTypeMapper;

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(RoomTypeCreateRequest request) {
        if (roomTypeRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.ROOM_TYPE_CODE_ALREADY_EXISTS, "Mã loại phòng đã tồn tại: " + request.getCode());
        }

        RoomType roomType = roomTypeMapper.toEntity(request);
        RoomType saved = roomTypeRepository.save(roomType);
        return roomTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(Short id, RoomTypeUpdateRequest request) {
        RoomType roomType = roomTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND, "Không tìm thấy loại phòng id: " + id));

        roomTypeMapper.updateEntity(roomType, request);
        RoomType updated = roomTypeRepository.save(roomType);
        return roomTypeMapper.toResponse(updated);
    }

    @Override
    public RoomTypeResponse getRoomTypeById(Short id) {
        RoomType roomType = roomTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND, "Không tìm thấy loại phòng id: " + id));
        return roomTypeMapper.toResponse(roomType);
    }

    @Override
    public PageResponse<RoomTypeResponse> filterRoomTypes(RoomTypeSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<RoomType> page = roomTypeRepository.filterRoomTypes(
                searchDto.getKeyword(),
                searchDto.getStatus(),
                pageable
        );

        return PageResponse.<RoomTypeResponse>builder()
                .content(roomTypeMapper.toResponseList(page.getContent()))
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
    public void deleteRoomTypes(List<Short> ids) {
        if (ids != null && !ids.isEmpty()) {
            roomTypeRepository.softDeleteByIds(ids);
        }
    }
}
