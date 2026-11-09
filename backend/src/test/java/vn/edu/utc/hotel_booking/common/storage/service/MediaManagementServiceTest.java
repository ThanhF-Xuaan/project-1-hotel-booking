package vn.edu.utc.hotel_booking.common.storage.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.CategoryMetaResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaItemResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaSearchDto;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicy;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicyRegistry;
import vn.edu.utc.hotel_booking.common.storage.repository.MediaObjectRepository;
import vn.edu.utc.hotel_booking.common.storage.service.impl.MediaManagementServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaManagementService — Unit Tests")
class MediaManagementServiceTest {

    @Mock
    private MediaObjectRepository mediaObjectRepository;

    @Mock
    private InternalMediaDomainService internalMediaDomainService;

    @Mock
    private StoragePolicyRegistry storagePolicyRegistry;

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private MediaManagementServiceImpl mediaManagementService;

    @Test
    @DisplayName("filterMedia - Lọc danh sách hình ảnh thành công với phân trang")
    void filterMedia_success() {
        MediaObject mo = MediaObject.builder()
                .id(1L)
                .storageKey("public/hotels/1/properties/hotel1.jpg")
                .originalFilename("hotel1.jpg")
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .hotelId((short) 1)
                .isPrimary(true)
                .sizeBytes(2048L)
                .build();

        when(mediaObjectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mo)));
        when(internalMediaDomainService.getDownloadUrl(eq("public/hotels/1/properties/hotel1.jpg"), isNull()))
                .thenReturn("http://localhost:9000/download/hotel1.jpg");

        MediaSearchDto criteria = MediaSearchDto.builder()
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .page(0)
                .pageSize(10)
                .build();

        PageResponse<MediaItemResponse> response = mediaManagementService.filterMedia(criteria);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getOriginalFilename()).isEqualTo("hotel1.jpg");
        assertThat(response.getContent().getFirst().getUrl()).isEqualTo("http://localhost:9000/download/hotel1.jpg");
    }

    @Test
    @DisplayName("getAvailableCategories - Trả về đúng metadata các loại đa phương tiện")
    void getAvailableCategories_success() {
        StoragePolicy policy = StoragePolicy.builder()
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .folderPrefix("properties")
                .defaultAccessLevel(StorageAccessLevel.PUBLIC_READ)
                .maxSizeBytes(10 * 1024 * 1024L)
                .allowedMimeTypes(Set.of("image/jpeg", "image/png"))
                .build();

        when(storagePolicyRegistry.getPolicy(any())).thenReturn(policy);

        List<CategoryMetaResponse> categories = mediaManagementService.getAvailableCategories();

        assertThat(categories).isNotEmpty();
        assertThat(categories.stream().anyMatch(c -> c.getCategory() == StorageCategory.HOTEL_PROPERTY_PHOTO)).isTrue();
    }

    @Test
    @DisplayName("setPrimary - Thành công đổi ảnh chính")
    void setPrimary_success() {
        MediaObject target = MediaObject.builder()
                .id(50L)
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .referenceId(101L)
                .isPrimary(false)
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(target));
        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                StorageCategory.HOTEL_ROOM_PHOTO, 101L))
                .thenReturn(Optional.empty());

        mediaManagementService.setPrimary(50L);

        assertThat(target.getIsPrimary()).isTrue();
        verify(mediaObjectRepository).save(target);
    }

    @Test
    @DisplayName("deleteMedia - Xóa ảnh thành công")
    void deleteMedia_success() {
        MediaObject target = MediaObject.builder()
                .id(60L)
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .referenceId(101L)
                .storageKey("public/hotels/1/rooms/101/del.jpg")
                .isPrimary(false)
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(60L)).thenReturn(Optional.of(target));

        mediaManagementService.deleteMedia(60L);

        verify(internalMediaDomainService).deleteMedia("public/hotels/1/rooms/101/del.jpg");
    }
}

