package vn.edu.utc.hotel_booking.common.storage.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.CategoryMetaResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaItemResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaSearchDto;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaUploadResponse;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicy;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicyRegistry;
import vn.edu.utc.hotel_booking.common.storage.repository.MediaObjectRepository;
import vn.edu.utc.hotel_booking.common.storage.service.InternalMediaDomainService;
import vn.edu.utc.hotel_booking.common.storage.service.MediaManagementService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MediaManagementServiceImpl implements MediaManagementService {

    MediaObjectRepository mediaObjectRepository;
    InternalMediaDomainService internalMediaDomainService;
    StoragePolicyRegistry storagePolicyRegistry;
    HotelRepository hotelRepository;
    HotelRoomTypeRepository hotelRoomTypeRepository;

    static final Map<StorageCategory, String> CATEGORY_LABELS = Map.of(
            StorageCategory.HOTEL_PROPERTY_PHOTO, "Khuôn viên / Khách sạn cơ sở",
            StorageCategory.HOTEL_ROOM_PHOTO, "Hạng phòng khách sạn",
            StorageCategory.HOTEL_AMENITY, "Tiện ích & Dịch vụ",
            StorageCategory.BRAND_ASSET, "Thương hiệu & Logo",
            StorageCategory.MARKETING_BANNER, "Banner tiếp thị & Quảng cáo",
            StorageCategory.USER_AVATAR, "Ảnh đại diện người dùng",
            StorageCategory.GUEST_REVIEW_PHOTO, "Ảnh đánh giá khách hàng"
    );

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MediaItemResponse> filterMedia(MediaSearchDto criteria) {
        int page = criteria.getPage() != null && criteria.getPage() >= 0 ? criteria.getPage() : 0;
        int pageSize = criteria.getPageSize() != null && criteria.getPageSize() > 0 ? criteria.getPageSize() : 12;

        Sort.Direction direction = "asc".equalsIgnoreCase(criteria.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortBy = (criteria.getSortBy() != null && !criteria.getSortBy().isBlank())
                ? criteria.getSortBy()
                : "createdAt";

        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Specification<MediaObject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (criteria.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.getCategory()));
            }

            if (criteria.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotelId"), criteria.getHotelId()));
            }

            if (criteria.getReferenceId() != null) {
                predicates.add(cb.equal(root.get("referenceId"), criteria.getReferenceId()));
            }

            if (criteria.getIsPrimary() != null) {
                predicates.add(cb.equal(root.get("isPrimary"), criteria.getIsPrimary()));
            }

            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                String pattern = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("originalFilename")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MediaObject> pageResult = mediaObjectRepository.findAll(spec, pageable);

        // Preload Hotel names to avoid N+1 query
        Set<Short> hotelIds = pageResult.getContent().stream()
                .map(MediaObject::getHotelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Short, String> hotelNameMap = hotelIds.isEmpty() ? Collections.emptyMap() :
                hotelRepository.findAllById(hotelIds).stream()
                        .collect(Collectors.toMap(Hotel::getId, Hotel::getName));

        // Preload Room Type names for HOTEL_ROOM_PHOTO to avoid N+1 query
        Set<Integer> roomTypeIds = pageResult.getContent().stream()
                .filter(mo -> mo.getCategory() == StorageCategory.HOTEL_ROOM_PHOTO && mo.getReferenceId() != null)
                .map(mo -> mo.getReferenceId().intValue())
                .collect(Collectors.toSet());

        Map<Integer, String> roomTypeNameMap = roomTypeIds.isEmpty() ? Collections.emptyMap() :
                hotelRoomTypeRepository.findAllById(roomTypeIds).stream()
                        .collect(Collectors.toMap(
                                HotelRoomType::getId,
                                rt -> rt.getRoomType() != null
                                        ? rt.getRoomType().getName() + (rt.getRoomType().getCode() != null ? " (" + rt.getRoomType().getCode() + ")" : "")
                                        : "Hạng phòng #" + rt.getId()
                        ));

        List<MediaItemResponse> items = pageResult.getContent().stream()
                .map(mo -> {
                    String hotelName = hotelNameMap.get(mo.getHotelId());
                    String refName = (mo.getReferenceId() != null)
                            ? roomTypeNameMap.get(mo.getReferenceId().intValue())
                            : null;
                    return mapToResponse(mo, hotelName, refName);
                })
                .toList();

        return PageResponse.<MediaItemResponse>builder()
                .content(items)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .empty(pageResult.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public List<MediaItemResponse> uploadMedia(StorageCategory category, Short hotelId, Long referenceId, List<MultipartFile> files, String uploaderId) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Danh sách tệp tin tải lên không được rỗng");
        }

        Short resolvedHotelId = validateAndResolveScope(category, hotelId, referenceId);
        Long resolvedReferenceId = referenceId;
        if (category == StorageCategory.HOTEL_PROPERTY_PHOTO && resolvedReferenceId == null && resolvedHotelId != null) {
            resolvedReferenceId = resolvedHotelId.longValue();
        }

        String resolvedRefName = null;
        if (category == StorageCategory.HOTEL_ROOM_PHOTO && referenceId != null) {
            resolvedRefName = hotelRoomTypeRepository.findById(referenceId.intValue())
                    .map(rt -> rt.getRoomType() != null
                            ? rt.getRoomType().getName() + (rt.getRoomType().getCode() != null ? " (" + rt.getRoomType().getCode() + ")" : "")
                            : "Hạng phòng #" + rt.getId())
                    .orElse(null);
        }

        List<MediaItemResponse> uploadedItems = new ArrayList<>();

        for (MultipartFile file : files) {
            MediaUploadResponse uploaded = internalMediaDomainService.upload(file, category, resolvedHotelId, resolvedReferenceId, uploaderId);
            MediaObject media = mediaObjectRepository.findByIdAndIsDeletedFalse(uploaded.getMediaId())
                    .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

            // Nếu đây là ảnh duy nhất hiện có của đối tượng, tự động đánh dấu làm ảnh chính
            Long refIdToCheck = resolvedReferenceId != null ? resolvedReferenceId : (resolvedHotelId != null ? resolvedHotelId.longValue() : null);
            if (refIdToCheck != null) {
                boolean hasPrimary = mediaObjectRepository
                        .findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(category, refIdToCheck)
                        .isPresent();
                if (!hasPrimary) {
                    media.setIsPrimary(true);
                    mediaObjectRepository.save(media);
                }
            }

            String hotelName = null;
            if (resolvedHotelId != null) {
                hotelName = hotelRepository.findById(resolvedHotelId).map(Hotel::getName).orElse(null);
            }

            uploadedItems.add(mapToResponse(media, hotelName, resolvedRefName));
        }

        return uploadedItems;
    }

    private Short validateAndResolveScope(StorageCategory category, Short hotelId, Long referenceId) {
        if (category == StorageCategory.BRAND_ASSET) {
            // Brand Asset thuộc cấp toàn chuỗi, không gắn với cơ sở riêng lẻ
            return null;
        }

        if (category == StorageCategory.HOTEL_PROPERTY_PHOTO) {
            if (hotelId == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ảnh khuôn viên khách sạn bắt buộc phải chọn Khách sạn cơ sở!");
            }
            if (!hotelRepository.existsById(hotelId)) {
                throw new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn cơ sở với ID: " + hotelId);
            }
            return hotelId;
        }

        if (category == StorageCategory.HOTEL_ROOM_PHOTO) {
            if (hotelId == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ảnh hạng phòng bắt buộc phải chọn Khách sạn cơ sở!");
            }
            if (referenceId == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ảnh hạng phòng bắt buộc phải chọn Hạng phòng cụ thể!");
            }
            HotelRoomType roomType = hotelRoomTypeRepository.findById(referenceId.intValue())
                    .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND, "Không tìm thấy hạng phòng với ID: " + referenceId));
            if (roomType.getHotel() == null || !Objects.equals(roomType.getHotel().getId(), hotelId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Hạng phòng được chọn không thuộc khách sạn cơ sở này!");
            }
            return hotelId;
        }

        // Với MARKETING_BANNER, HOTEL_AMENITY, USER_AVATAR... hotelId là tùy chọn
        if (hotelId != null && !hotelRepository.existsById(hotelId)) {
            throw new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn cơ sở với ID: " + hotelId);
        }

        return hotelId;
    }

    @Override
    @Transactional
    public void setPrimary(Long mediaId) {
        MediaObject targetMedia = mediaObjectRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        if (Boolean.TRUE.equals(targetMedia.getIsPrimary())) {
            return;
        }

        Long refId = targetMedia.getReferenceId();
        if (refId == null && targetMedia.getHotelId() != null) {
            refId = targetMedia.getHotelId().longValue();
        }

        if (targetMedia.getCategory() == StorageCategory.HOTEL_PROPERTY_PHOTO && targetMedia.getReferenceId() == null && refId != null) {
            targetMedia.setReferenceId(refId);
        }

        if (refId != null) {
            mediaObjectRepository
                    .findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(targetMedia.getCategory(), refId)
                    .ifPresent(oldPrimary -> {
                        oldPrimary.setIsPrimary(false);
                        mediaObjectRepository.saveAndFlush(oldPrimary);
                    });
        }

        targetMedia.setIsPrimary(true);
        mediaObjectRepository.save(targetMedia);
    }

    @Override
    @Transactional
    public void deleteMedia(Long mediaId) {
        MediaObject media = mediaObjectRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        boolean wasPrimary = Boolean.TRUE.equals(media.getIsPrimary());
        Long refId = media.getReferenceId() != null ? media.getReferenceId() :
                (media.getHotelId() != null ? media.getHotelId().longValue() : null);

        internalMediaDomainService.deleteMedia(media.getStorageKey());

        // Nếu ảnh vừa xóa là ảnh primary, tự động thăng cấp ảnh tiếp theo (nếu còn)
        if (wasPrimary && refId != null) {
            List<MediaObject> remaining = mediaObjectRepository
                    .findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                            media.getCategory(), refId
                    );

            for (MediaObject candidate : remaining) {
                if (!Objects.equals(candidate.getId(), mediaId)) {
                    candidate.setIsPrimary(true);
                    mediaObjectRepository.save(candidate);
                    break;
                }
            }
        }
    }

    @Override
    public List<CategoryMetaResponse> getAvailableCategories() {
        return Arrays.stream(StorageCategory.values())
                .filter(cat -> cat == StorageCategory.HOTEL_PROPERTY_PHOTO
                        || cat == StorageCategory.HOTEL_ROOM_PHOTO
                        || cat == StorageCategory.HOTEL_AMENITY
                        || cat == StorageCategory.BRAND_ASSET
                        || cat == StorageCategory.MARKETING_BANNER)
                .map(cat -> {
                    StoragePolicy policy = storagePolicyRegistry.getPolicy(cat);
                    return CategoryMetaResponse.builder()
                            .category(cat)
                            .label(CATEGORY_LABELS.getOrDefault(cat, cat.name()))
                            .folderPrefix(policy.getFolderPrefix())
                            .isPublic(policy.getDefaultAccessLevel() == StorageAccessLevel.PUBLIC_READ)
                            .maxSizeBytes(policy.getMaxSizeBytes())
                            .allowedMimeTypes(policy.getAllowedMimeTypes())
                            .build();
                })
                .toList();
    }

    private MediaItemResponse mapToResponse(MediaObject mo, String hotelName, String referenceName) {
        String url = internalMediaDomainService.getDownloadUrl(mo.getStorageKey(), null);
        return MediaItemResponse.builder()
                .id(mo.getId())
                .storageKey(mo.getStorageKey())
                .url(url)
                .category(mo.getCategory())
                .categoryLabel(CATEGORY_LABELS.getOrDefault(mo.getCategory(), mo.getCategory().name()))
                .originalFilename(mo.getOriginalFilename())
                .contentType(mo.getContentType())
                .sizeBytes(mo.getSizeBytes())
                .checksumSha256(mo.getChecksumSha256())
                .hotelId(mo.getHotelId())
                .hotelName(hotelName)
                .referenceId(mo.getReferenceId())
                .referenceName(referenceName)
                .isPrimary(Boolean.TRUE.equals(mo.getIsPrimary()))
                .accessLevel(mo.getAccessLevel())
                .lifecycleStatus(mo.getLifecycleStatus())
                .uploadedBy(mo.getUploadedBy())
                .createdAt(mo.getCreatedAt())
                .build();
    }
}

