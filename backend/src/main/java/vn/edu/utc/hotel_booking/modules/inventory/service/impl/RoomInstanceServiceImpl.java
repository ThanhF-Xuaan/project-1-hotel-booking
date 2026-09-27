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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomInstanceResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomInstanceMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomInstanceServiceImpl implements RoomInstanceService {

    RoomInstanceRepository roomInstanceRepository;
    HotelRepository hotelRepository;
    HotelRoomTypeRepository hotelRoomTypeRepository;
    RoomInstanceMapper roomInstanceMapper;

    @Override
    @Transactional
    public RoomInstanceResponse createRoomInstance(RoomInstanceCreateRequest request) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn id: " + request.getHotelId()));

        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        if (roomInstanceRepository.existsByHotelIdAndRoomNumberAndIsDeletedFalse(request.getHotelId(), request.getRoomNumber())) {
            throw new AppException(ErrorCode.ROOM_NUMBER_ALREADY_EXISTS, "Số phòng " + request.getRoomNumber() + " đã tồn tại trong khách sạn này");
        }

        RoomInstance roomInstance = roomInstanceMapper.toEntity(request);
        roomInstance.setHotel(hotel);
        roomInstance.setHotelRoomType(hotelRoomType);

        RoomInstance saved = roomInstanceRepository.save(roomInstance);
        return roomInstanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoomInstanceResponse updateRoomInstance(Integer id, RoomInstanceUpdateRequest request) {
        RoomInstance roomInstance = roomInstanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng id: " + id));

        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        if (roomInstanceRepository.existsByHotelIdAndRoomNumberAndIdNotAndIsDeletedFalse(
                roomInstance.getHotel().getId(), request.getRoomNumber(), id)) {
            throw new AppException(ErrorCode.ROOM_NUMBER_ALREADY_EXISTS, "Số phòng " + request.getRoomNumber() + " đã tồn tại trong khách sạn");
        }

        roomInstanceMapper.updateEntity(roomInstance, request);
        roomInstance.setHotelRoomType(hotelRoomType);

        RoomInstance updated = roomInstanceRepository.save(roomInstance);
        return roomInstanceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public RoomInstanceResponse updateRoomStatus(Integer id, String status) {
        RoomInstance roomInstance = roomInstanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng id: " + id));

        roomInstance.setCurrentStatus(status);
        RoomInstance updated = roomInstanceRepository.save(roomInstance);
        return roomInstanceMapper.toResponse(updated);
    }

    @Override
    public RoomInstanceResponse getRoomInstanceById(Integer id) {
        RoomInstance roomInstance = roomInstanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng id: " + id));
        return roomInstanceMapper.toResponse(roomInstance);
    }

    @Override
    public PageResponse<RoomInstanceResponse> filterRoomInstances(RoomInstanceSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<RoomInstance> page = roomInstanceRepository.filterRooms(
                searchDto.getHotelId(),
                searchDto.getHotelRoomTypeId(),
                searchDto.getRoomNumber(),
                searchDto.getCurrentStatus(),
                pageable
        );

        return PageResponse.<RoomInstanceResponse>builder()
                .content(roomInstanceMapper.toResponseList(page.getContent()))
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
    public void deleteRoomInstances(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            roomInstanceRepository.softDeleteByIds(ids);
        }
    }
}
