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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.HotelRoomTypeMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomFeatureRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class HotelRoomTypeServiceImpl implements HotelRoomTypeService {

    HotelRoomTypeRepository hotelRoomTypeRepository;
    HotelRepository hotelRepository;
    RoomTypeRepository roomTypeRepository;
    RoomFeatureRepository roomFeatureRepository;
    HotelRoomTypeMapper hotelRoomTypeMapper;

    @Override
    @Transactional
    public HotelRoomTypeResponse createHotelRoomType(HotelRoomTypeCreateRequest request) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn id: " + request.getHotelId()));

        RoomType roomType = roomTypeRepository.findByIdAndIsDeletedFalse(request.getRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_TYPE_NOT_FOUND, "Không tìm thấy loại phòng id: " + request.getRoomTypeId()));

        if (hotelRoomTypeRepository.existsByHotelIdAndRoomTypeIdAndIsDeletedFalse(request.getHotelId(), request.getRoomTypeId())) {
            throw new AppException(ErrorCode.HOTEL_ROOM_TYPE_ALREADY_EXISTS, "Khách sạn này đã cấu hình loại phòng này rồi");
        }

        HotelRoomType hotelRoomType = hotelRoomTypeMapper.toEntity(request);
        hotelRoomType.setHotel(hotel);
        hotelRoomType.setRoomType(roomType);

        if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
            Set<RoomFeature> features = roomFeatureRepository.findByIdIn(request.getFeatureIds());
            hotelRoomType.setFeatures(features);
        }

        HotelRoomType saved = hotelRoomTypeRepository.save(hotelRoomType);
        return hotelRoomTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HotelRoomTypeResponse updateHotelRoomType(Integer id, HotelRoomTypeUpdateRequest request) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + id));

        hotelRoomTypeMapper.updateEntity(hotelRoomType, request);

        if (request.getFeatureIds() != null) {
            Set<RoomFeature> features = request.getFeatureIds().isEmpty()
                    ? new HashSet<>()
                    : roomFeatureRepository.findByIdIn(request.getFeatureIds());
            hotelRoomType.setFeatures(features);
        }

        HotelRoomType updated = hotelRoomTypeRepository.save(hotelRoomType);
        return hotelRoomTypeMapper.toResponse(updated);
    }

    @Override
    public HotelRoomTypeResponse getHotelRoomTypeById(Integer id) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + id));
        return hotelRoomTypeMapper.toResponse(hotelRoomType);
    }

    @Override
    public PageResponse<HotelRoomTypeResponse> filterHotelRoomTypes(HotelRoomTypeSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<HotelRoomType> page = hotelRoomTypeRepository.filterHotelRoomTypes(
                searchDto.getHotelId(),
                searchDto.getRoomTypeId(),
                searchDto.getAdults(),
                searchDto.getChildren(),
                searchDto.getMinPrice(),
                searchDto.getMaxPrice(),
                searchDto.getStatus(),
                pageable
        );

        return PageResponse.<HotelRoomTypeResponse>builder()
                .content(hotelRoomTypeMapper.toResponseList(page.getContent()))
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
    public void deleteHotelRoomTypes(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            hotelRoomTypeRepository.softDeleteByIds(ids);
        }
    }
}
