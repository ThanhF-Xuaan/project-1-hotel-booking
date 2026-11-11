package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;
import vn.edu.utc.hotel_booking.common.storage.repository.MediaObjectRepository;
import vn.edu.utc.hotel_booking.common.storage.service.InternalMediaDomainService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeMediaService;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelRoomTypeMediaServiceImpl implements HotelRoomTypeMediaService {

    HotelRoomTypeRepository hotelRoomTypeRepository;
    MediaObjectRepository mediaObjectRepository;
    InternalMediaDomainService internalMediaDomainService;

    @Override
    @Transactional(readOnly = true)
    public List<HotelImageResponse> getImages(Integer hotelRoomTypeId) {
        if (!hotelRoomTypeRepository.existsById(hotelRoomTypeId)) {
            throw new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND);
        }

        List<MediaObject> mediaList = mediaObjectRepository
                .findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                        StorageCategory.HOTEL_ROOM_PHOTO,
                        hotelRoomTypeId.longValue()
                );

        return mediaList.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<HotelImageResponse> uploadImages(Integer hotelRoomTypeId, List<MultipartFile> files, String uploaderId) {
        HotelRoomType hrt = hotelRoomTypeRepository.findById(hotelRoomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND));

        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Danh sách tệp tin tải lên không được rỗng");
        }

        Short hotelId = hrt.getHotel() != null ? hrt.getHotel().getId() : null;

        for (MultipartFile file : files) {
            internalMediaDomainService.upload(
                    file,
                    StorageCategory.HOTEL_ROOM_PHOTO,
                    hotelId,
                    hotelRoomTypeId.longValue(),
                    uploaderId
            );
        }

        // Nếu trước đó chưa có ảnh primary nào, tự động thăng cấp ảnh mới nhất làm primary
        boolean hasPrimary = mediaObjectRepository
                .findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                        StorageCategory.HOTEL_ROOM_PHOTO,
                        hotelRoomTypeId.longValue()
                )
                .isPresent();

        if (!hasPrimary) {
            List<MediaObject> remaining = mediaObjectRepository
                    .findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                            StorageCategory.HOTEL_ROOM_PHOTO,
                            hotelRoomTypeId.longValue()
                    );
            if (!remaining.isEmpty()) {
                remaining.getFirst().setIsPrimary(true);
                mediaObjectRepository.save(remaining.getFirst());
            }
        }

        return getImages(hotelRoomTypeId);
    }

    @Override
    @Transactional
    public void setPrimaryImage(Integer hotelRoomTypeId, Long imageId) {
        if (!hotelRoomTypeRepository.existsById(hotelRoomTypeId)) {
            throw new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND);
        }

        MediaObject targetMedia = mediaObjectRepository.findByIdAndIsDeletedFalse(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        if (targetMedia.getCategory() != StorageCategory.HOTEL_ROOM_PHOTO
                || !Objects.equals(targetMedia.getReferenceId(), hotelRoomTypeId.longValue())) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Ảnh không thuộc cấu hình hạng phòng này");
        }

        if (Boolean.TRUE.equals(targetMedia.getIsPrimary())) {
            return;
        }

        // Bỏ primary của ảnh cũ (nếu có)
        mediaObjectRepository
                .findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                        StorageCategory.HOTEL_ROOM_PHOTO,
                        hotelRoomTypeId.longValue()
                )
                .ifPresent(oldPrimary -> {
                    oldPrimary.setIsPrimary(false);
                    mediaObjectRepository.saveAndFlush(oldPrimary);
                });

        targetMedia.setIsPrimary(true);
        mediaObjectRepository.save(targetMedia);
    }

    @Override
    @Transactional
    public void deleteImage(Integer hotelRoomTypeId, Long imageId) {
        if (!hotelRoomTypeRepository.existsById(hotelRoomTypeId)) {
            throw new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND);
        }

        MediaObject media = mediaObjectRepository.findByIdAndIsDeletedFalse(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        if (media.getCategory() != StorageCategory.HOTEL_ROOM_PHOTO
                || !Objects.equals(media.getReferenceId(), hotelRoomTypeId.longValue())) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Ảnh không thuộc cấu hình hạng phòng này");
        }

        boolean wasPrimary = Boolean.TRUE.equals(media.getIsPrimary());

        internalMediaDomainService.deleteMedia(media.getStorageKey());

        // Nếu ảnh vừa xóa là ảnh primary, tự động thăng cấp ảnh tiếp theo (nếu còn)
        if (wasPrimary) {
            List<MediaObject> remaining = mediaObjectRepository
                    .findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                            StorageCategory.HOTEL_ROOM_PHOTO,
                            hotelRoomTypeId.longValue()
                    );

            for (MediaObject candidate : remaining) {
                if (!Objects.equals(candidate.getId(), imageId)) {
                    candidate.setIsPrimary(true);
                    mediaObjectRepository.save(candidate);
                    break;
                }
            }
        }
    }

    private HotelImageResponse mapToResponse(MediaObject media) {
        String url = internalMediaDomainService.getDownloadUrl(media.getStorageKey(), null);
        return HotelImageResponse.builder()
                .id(media.getId())
                .url(url)
                .originalFilename(media.getOriginalFilename())
                .sizeBytes(media.getSizeBytes())
                .isPrimary(Boolean.TRUE.equals(media.getIsPrimary()))
                .createdAt(media.getCreatedAt())
                .build();
    }
}

