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
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaUploadResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private HotelRoomTypeRepository hotelRoomTypeRepository;

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

    @Test
    @DisplayName("uploadMedia - BRAND_ASSET tự động xóa hotelId về null (phạm vi toàn chuỗi)")
    void uploadMedia_brandAsset_forcesHotelIdNull() {
        MockMultipartFile file = new MockMultipartFile("files", "logo.png", "image/png", new byte[10]);
        MediaUploadResponse uploadResp = MediaUploadResponse.builder().mediaId(100L).build();
        MediaObject saved = MediaObject.builder()
                .id(100L)
                .category(StorageCategory.BRAND_ASSET)
                .storageKey("public/common/brand/logo.png")
                .originalFilename("logo.png")
                .build();

        when(internalMediaDomainService.upload(eq(file), eq(StorageCategory.BRAND_ASSET), isNull(), isNull(), any()))
                .thenReturn(uploadResp);
        when(mediaObjectRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(saved));

        List<MediaItemResponse> result = mediaManagementService.uploadMedia(
                StorageCategory.BRAND_ASSET, (short) 1, null, List.of(file), "admin"
        );

        assertThat(result).hasSize(1);
        verify(internalMediaDomainService).upload(eq(file), eq(StorageCategory.BRAND_ASSET), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("uploadMedia - HOTEL_PROPERTY_PHOTO ném lỗi khi thiếu hotelId")
    void uploadMedia_propertyPhoto_missingHotelId_throws() {
        MockMultipartFile file = new MockMultipartFile("files", "hotel.jpg", "image/jpeg", new byte[10]);

        assertThatThrownBy(() -> mediaManagementService.uploadMedia(
                StorageCategory.HOTEL_PROPERTY_PHOTO, null, null, List.of(file), "admin"
        )).isInstanceOf(AppException.class)
          .hasMessageContaining("Ảnh khuôn viên khách sạn bắt buộc phải chọn Khách sạn cơ sở!");
    }

    @Test
    @DisplayName("uploadMedia - HOTEL_ROOM_PHOTO ném lỗi khi thiếu hạng phòng")
    void uploadMedia_roomPhoto_missingRoomType_throws() {
        MockMultipartFile file = new MockMultipartFile("files", "room.jpg", "image/jpeg", new byte[10]);

        assertThatThrownBy(() -> mediaManagementService.uploadMedia(
                StorageCategory.HOTEL_ROOM_PHOTO, (short) 1, null, List.of(file), "admin"
        )).isInstanceOf(AppException.class)
          .hasMessageContaining("Ảnh hạng phòng bắt buộc phải chọn Hạng phòng cụ thể!");
    }

    @Test
    @DisplayName("uploadMedia - HOTEL_ROOM_PHOTO ném lỗi khi hạng phòng không thuộc khách sạn cơ sở đã chọn")
    void uploadMedia_roomPhoto_mismatchedHotel_throws() {
        MockMultipartFile file = new MockMultipartFile("files", "room.jpg", "image/jpeg", new byte[10]);

        Hotel otherHotel = Hotel.builder().id((short) 2).name("Khách sạn Đà Nẵng").build();
        RoomType rtEntity = RoomType.builder().id((short) 1).code("DLX").name("Deluxe Room").build();
        HotelRoomType roomType = HotelRoomType.builder()
                .id(10)
                .hotel(otherHotel)
                .roomType(rtEntity)
                .build();

        when(hotelRoomTypeRepository.findById(10)).thenReturn(Optional.of(roomType));

        assertThatThrownBy(() -> mediaManagementService.uploadMedia(
                StorageCategory.HOTEL_ROOM_PHOTO, (short) 1, 10L, List.of(file), "admin"
        )).isInstanceOf(AppException.class)
          .hasMessageContaining("Hạng phòng được chọn không thuộc khách sạn cơ sở này!");
    }

    @Test
    @DisplayName("uploadMedia - HOTEL_ROOM_PHOTO thành công khi khách sạn và hạng phòng hợp lệ")
    void uploadMedia_roomPhoto_success() {
        MockMultipartFile file = new MockMultipartFile("files", "room.jpg", "image/jpeg", new byte[10]);
        Hotel targetHotel = Hotel.builder().id((short) 1).name("Khách sạn Hà Nội").build();
        RoomType rtEntity = RoomType.builder().id((short) 1).code("DLX").name("Deluxe Room").build();
        HotelRoomType roomType = HotelRoomType.builder()
                .id(10)
                .hotel(targetHotel)
                .roomType(rtEntity)
                .build();

        when(hotelRoomTypeRepository.findById(10)).thenReturn(Optional.of(roomType));

        MediaUploadResponse uploadResp = MediaUploadResponse.builder().mediaId(200L).build();
        MediaObject saved = MediaObject.builder()
                .id(200L)
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .hotelId((short) 1)
                .referenceId(10L)
                .storageKey("public/hotels/1/rooms/10/uuid_room.jpg")
                .originalFilename("room.jpg")
                .build();

        when(internalMediaDomainService.upload(eq(file), eq(StorageCategory.HOTEL_ROOM_PHOTO), eq((short) 1), eq(10L), any()))
                .thenReturn(uploadResp);
        when(mediaObjectRepository.findByIdAndIsDeletedFalse(200L)).thenReturn(Optional.of(saved));
        when(hotelRepository.findById((short) 1)).thenReturn(Optional.of(targetHotel));

        List<MediaItemResponse> result = mediaManagementService.uploadMedia(
                StorageCategory.HOTEL_ROOM_PHOTO, (short) 1, 10L, List.of(file), "admin"
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getReferenceName()).isEqualTo("Deluxe Room (DLX)");
    }
}

