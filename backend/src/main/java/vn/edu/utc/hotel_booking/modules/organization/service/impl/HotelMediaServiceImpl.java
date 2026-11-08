package vn.edu.utc.hotel_booking.modules.organization.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaUploadResponse;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;
import vn.edu.utc.hotel_booking.common.storage.repository.MediaObjectRepository;
import vn.edu.utc.hotel_booking.common.storage.service.InternalMediaDomainService;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelMediaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelMediaServiceImpl implements HotelMediaService {

    private final HotelRepository hotelRepository;
    private final MediaObjectRepository mediaObjectRepository;
    private final InternalMediaDomainService internalMediaDomainService;

    @Override
    @Transactional
    public List<HotelImageResponse> uploadImages(Short hotelId, List<MultipartFile> files, String uploaderId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new AppException(ErrorCode.HOTEL_NOT_FOUND);
        }

        if (files == null || files.isEmpty()) {
            return getHotelImages(hotelId);
        }

        boolean hasPrimary = mediaObjectRepository
                .findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(StorageCategory.HOTEL_PROPERTY_PHOTO, hotelId.longValue())
                .isPresent();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            MediaUploadResponse uploadRes = internalMediaDomainService.upload(
                    file,
                    StorageCategory.HOTEL_PROPERTY_PHOTO,
                    hotelId,
                    hotelId.longValue(),
                    uploaderId
            );

            MediaObject mediaObj = mediaObjectRepository.findById(uploadRes.getMediaId())
                    .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

            if (!hasPrimary) {
                mediaObj.setIsPrimary(true);
                mediaObjectRepository.saveAndFlush(mediaObj);
                hasPrimary = true;
            }
        }

        return getHotelImages(hotelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelImageResponse> getHotelImages(Short hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new AppException(ErrorCode.HOTEL_NOT_FOUND);
        }

        List<MediaObject> mediaList = mediaObjectRepository
                .findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                        StorageCategory.HOTEL_PROPERTY_PHOTO,
                        hotelId.longValue()
                );

        return mediaList.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void setPrimaryImage(Short hotelId, Long imageId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new AppException(ErrorCode.HOTEL_NOT_FOUND);
        }

        MediaObject targetMedia = mediaObjectRepository.findByIdAndIsDeletedFalse(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        if (targetMedia.getCategory() != StorageCategory.HOTEL_PROPERTY_PHOTO
                || !Objects.equals(targetMedia.getReferenceId(), hotelId.longValue())) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Ảnh không thuộc khách sạn này");
        }

        if (Boolean.TRUE.equals(targetMedia.getIsPrimary())) {
            return;
        }

        // Bỏ primary của ảnh cũ (nếu có)
        mediaObjectRepository
                .findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(StorageCategory.HOTEL_PROPERTY_PHOTO, hotelId.longValue())
                .ifPresent(oldPrimary -> {
                    oldPrimary.setIsPrimary(false);
                    mediaObjectRepository.saveAndFlush(oldPrimary);
                });

        targetMedia.setIsPrimary(true);
        mediaObjectRepository.save(targetMedia);
    }

    @Override
    @Transactional
    public void deleteImage(Short hotelId, Long imageId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new AppException(ErrorCode.HOTEL_NOT_FOUND);
        }

        MediaObject media = mediaObjectRepository.findByIdAndIsDeletedFalse(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        if (media.getCategory() != StorageCategory.HOTEL_PROPERTY_PHOTO
                || !Objects.equals(media.getReferenceId(), hotelId.longValue())) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Ảnh không thuộc khách sạn này");
        }

        boolean wasPrimary = Boolean.TRUE.equals(media.getIsPrimary());

        internalMediaDomainService.deleteMedia(media.getStorageKey());

        // Nếu ảnh vừa xóa là ảnh primary, tự động thăng cấp ảnh tiếp theo (nếu còn)
        if (wasPrimary) {
            List<MediaObject> remaining = mediaObjectRepository
                    .findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                            StorageCategory.HOTEL_PROPERTY_PHOTO,
                            hotelId.longValue()
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

