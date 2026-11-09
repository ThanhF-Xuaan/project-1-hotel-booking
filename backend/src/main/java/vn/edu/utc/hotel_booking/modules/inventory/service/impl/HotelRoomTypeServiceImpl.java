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
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;
import vn.edu.utc.hotel_booking.common.storage.repository.MediaObjectRepository;
import vn.edu.utc.hotel_booking.common.storage.service.InternalMediaDomainService;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeBedItemRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeBed;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeBedId;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.HotelRoomTypeMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomBedService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomFeatureService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class HotelRoomTypeServiceImpl implements HotelRoomTypeService {

    HotelRoomTypeRepository hotelRoomTypeRepository;
    HotelService hotelService;
    RoomTypeService roomTypeService;
    RoomBedService roomBedService;
    RoomFeatureService roomFeatureService;
    HotelRoomTypeMapper hotelRoomTypeMapper;
    RoomAvailabilityGenerationService roomAvailabilityGenerationService;
    MediaObjectRepository mediaObjectRepository;
    InternalMediaDomainService internalMediaDomainService;

    @Override
    @Transactional
    public HotelRoomTypeResponse createHotelRoomType(HotelRoomTypeCreateRequest request) {
        Hotel hotel = hotelService.getEntityById(request.getHotelId());
        RoomType roomType = roomTypeService.getEntityById(request.getRoomTypeId());

        if (hotelRoomTypeRepository.existsByHotelIdAndRoomTypeIdAndIsDeletedFalse(request.getHotelId(), request.getRoomTypeId())) {
            throw new AppException(ErrorCode.HOTEL_ROOM_TYPE_ALREADY_EXISTS, "Khách sạn này đã cấu hình loại phòng này rồi");
        }

        HotelRoomType hotelRoomType = hotelRoomTypeMapper.toEntity(request);
        hotelRoomType.setHotel(hotel);
        hotelRoomType.setRoomType(roomType);

        if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
            Set<RoomFeature> features = roomFeatureService.getEntitiesByIds(request.getFeatureIds());
            hotelRoomType.setFeatures(features);
        }

        HotelRoomType saved = hotelRoomTypeRepository.save(hotelRoomType);

        if (request.getBeds() != null && !request.getBeds().isEmpty()) {
            applyBeds(saved, request.getBeds());
            saved = hotelRoomTypeRepository.save(saved);
        }

        roomAvailabilityGenerationService.generateForRoomType(saved.getId(), LocalDate.now(), 365);
        return hotelRoomTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HotelRoomTypeResponse updateHotelRoomType(Integer id, HotelRoomTypeUpdateRequest request) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + id));

        Integer oldQuantity = hotelRoomType.getTotalQuantity();
        hotelRoomTypeMapper.updateEntity(hotelRoomType, request);

        if (request.getFeatureIds() != null) {
            Set<RoomFeature> features = request.getFeatureIds().isEmpty()
                    ? new HashSet<>()
                    : roomFeatureService.getEntitiesByIds(request.getFeatureIds());
            hotelRoomType.setFeatures(features);
        }

        if (request.getBeds() != null) {
            applyBeds(hotelRoomType, request.getBeds());
        }

        HotelRoomType updated = hotelRoomTypeRepository.save(hotelRoomType);
        if (request.getTotalQuantity() != null && !request.getTotalQuantity().equals(oldQuantity)) {
            roomAvailabilityGenerationService.propagateCapacityChange(updated.getId(), updated.getTotalQuantity());
        }
        return hotelRoomTypeMapper.toResponse(updated);
    }

    @Override
    public HotelRoomTypeResponse getHotelRoomTypeById(Integer id) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + id));
        HotelRoomTypeResponse response = hotelRoomTypeMapper.toResponse(hotelRoomType);
        populateCoverImage(response);
        return response;
    }

    @Override
    public HotelRoomType getEntityById(Integer id) {
        return hotelRoomTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + id));
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

        List<HotelRoomTypeResponse> responses = hotelRoomTypeMapper.toResponseList(page.getContent());
        populateCoverImages(responses);

        return PageResponse.<HotelRoomTypeResponse>builder()
                .content(responses)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    private void populateCoverImages(List<HotelRoomTypeResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        List<Long> ids = responses.stream()
                .map(r -> r.getId().longValue())
                .toList();

        List<MediaObject> primaryImages = mediaObjectRepository
                .findByCategoryAndReferenceIdInAndIsPrimaryTrueAndIsDeletedFalse(
                        StorageCategory.HOTEL_ROOM_PHOTO, ids);

        Map<Long, String> coverUrlMap = primaryImages.stream()
                .collect(Collectors.toMap(
                        MediaObject::getReferenceId,
                        mo -> internalMediaDomainService.getDownloadUrl(mo.getStorageKey(), null),
                        (existing, replacement) -> existing
                ));

        responses.forEach(r -> r.setCoverImageUrl(coverUrlMap.get(r.getId().longValue())));
    }

    private void populateCoverImage(HotelRoomTypeResponse response) {
        if (response == null || response.getId() == null) {
            return;
        }
        mediaObjectRepository.findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                        StorageCategory.HOTEL_ROOM_PHOTO, response.getId().longValue())
                .ifPresent(mo -> response.setCoverImageUrl(internalMediaDomainService.getDownloadUrl(mo.getStorageKey(), null)));
    }

    @Override
    @Transactional
    public HotelRoomTypeResponse updateBeds(Integer id, List<HotelRoomTypeBedItemRequest> beds) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + id));

        applyBeds(hotelRoomType, beds);
        HotelRoomType updated = hotelRoomTypeRepository.save(hotelRoomType);
        return hotelRoomTypeMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public HotelRoomTypeResponse updateFeatures(Integer id, Set<Short> featureIds) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy cấu hình loại phòng id: " + id));

        Set<RoomFeature> features = (featureIds == null || featureIds.isEmpty())
                ? new HashSet<>()
                : roomFeatureService.getEntitiesByIds(featureIds);
        hotelRoomType.setFeatures(features);

        HotelRoomType updated = hotelRoomTypeRepository.save(hotelRoomType);
        return hotelRoomTypeMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteHotelRoomTypes(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            hotelRoomTypeRepository.softDeleteByIds(ids);
        }
    }

    private void applyBeds(HotelRoomType hotelRoomType, List<HotelRoomTypeBedItemRequest> bedRequests) {
        if (bedRequests == null || bedRequests.isEmpty()) {
            hotelRoomType.getBeds().clear();
            return;
        }

        // 1. Kiểm tra trùng lặp roomBedId trong request
        Set<Short> seenIds = new HashSet<>();
        for (HotelRoomTypeBedItemRequest item : bedRequests) {
            if (item.getRoomBedId() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID loại giường không được để trống!");
            }
            if (!seenIds.add(item.getRoomBedId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Loại giường (ID: " + item.getRoomBedId() + ") bị chọn trùng lặp trong danh sách!");
            }
        }

        // 2. Kiểm tra nghiệp vụ: Tổng số lượng giường cấu hình không được vượt quá maxBeds
        int totalBeds = bedRequests.stream()
                .mapToInt(item -> item.getBaseQuantity() != null ? item.getBaseQuantity() : 1)
                .sum();
        if (hotelRoomType.getMaxBeds() != null && totalBeds > hotelRoomType.getMaxBeds()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    String.format("Tổng số lượng giường cấu hình (%d) vượt quá số giường tối đa cho phép của hạng phòng (%d)!",
                            totalBeds, hotelRoomType.getMaxBeds()));
        }

        // 3. Cập nhật tại chỗ (In-Place Update) để tránh đụng độ khóa chính với Hibernate orphanRemoval
        Map<Short, HotelRoomTypeBedItemRequest> reqMap = bedRequests.stream()
                .collect(Collectors.toMap(HotelRoomTypeBedItemRequest::getRoomBedId, Function.identity()));

        // Xóa những giường không còn được chọn
        hotelRoomType.getBeds().removeIf(bed -> !reqMap.containsKey(bed.getRoomBed().getId()));

        // Ánh xạ các giường hiện có theo roomBedId
        Map<Short, HotelRoomTypeBed> existingBeds = hotelRoomType.getBeds().stream()
                .collect(Collectors.toMap(b -> b.getRoomBed().getId(), Function.identity()));

        for (HotelRoomTypeBedItemRequest item : bedRequests) {
            short quantity = item.getBaseQuantity() != null && item.getBaseQuantity() > 0 ? item.getBaseQuantity() : (short) 1;
            HotelRoomTypeBed existing = existingBeds.get(item.getRoomBedId());

            if (existing != null) {
                // Đã tồn tại -> Chỉ cập nhật số lượng baseQuantity (Hibernate sinh lệnh UPDATE, không đụng độ PK)
                existing.setBaseQuantity(quantity);
            } else {
                // Thêm mới loại giường
                RoomBed roomBed = roomBedService.getEntityById(item.getRoomBedId());
                HotelRoomTypeBedId bedId = HotelRoomTypeBedId.builder()
                        .hotelRoomTypeId(hotelRoomType.getId())
                        .roomBedId(roomBed.getId())
                        .build();
                HotelRoomTypeBed newBed = HotelRoomTypeBed.builder()
                        .id(bedId)
                        .hotelRoomType(hotelRoomType)
                        .roomBed(roomBed)
                        .baseQuantity(quantity)
                        .build();
                hotelRoomType.getBeds().add(newBed);
            }
        }
    }
}
