package vn.edu.utc.hotel_booking.modules.organization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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
import vn.edu.utc.hotel_booking.modules.organization.service.impl.HotelMediaServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelMediaService — Unit Tests")
class HotelMediaServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private MediaObjectRepository mediaObjectRepository;

    @Mock
    private InternalMediaDomainService internalMediaDomainService;

    @InjectMocks
    private HotelMediaServiceImpl hotelMediaService;

    private final Short hotelId = 1;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Upload ảnh thành công: ảnh đầu tiên tự động trở thành ảnh đại diện chính (isPrimary = true)")
    void uploadImages_firstImageBecomesPrimary() {
        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                eq(StorageCategory.HOTEL_PROPERTY_PHOTO), eq(1L)))
                .thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("files", "facade.jpg", "image/jpeg", "fake-img".getBytes());

        MediaUploadResponse uploadRes = MediaUploadResponse.builder()
                .mediaId(101L)
                .objectKey("public/hotels/1/properties/facade.jpg")
                .build();

        when(internalMediaDomainService.upload(any(MultipartFile.class), eq(StorageCategory.HOTEL_PROPERTY_PHOTO), eq(hotelId), eq(1L), any()))
                .thenReturn(uploadRes);

        MediaObject mediaObject = MediaObject.builder()
                .id(101L)
                .storageKey("public/hotels/1/properties/facade.jpg")
                .originalFilename("facade.jpg")
                .sizeBytes(1024L)
                .isPrimary(false)
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .referenceId(1L)
                .build();

        when(mediaObjectRepository.findById(101L)).thenReturn(Optional.of(mediaObject));
        when(mediaObjectRepository.saveAndFlush(any())).thenReturn(mediaObject);

        MediaObject primaryMediaObject = MediaObject.builder()
                .id(101L)
                .storageKey("public/hotels/1/properties/facade.jpg")
                .originalFilename("facade.jpg")
                .sizeBytes(1024L)
                .isPrimary(true)
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .referenceId(1L)
                .build();

        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                eq(StorageCategory.HOTEL_PROPERTY_PHOTO), eq(1L)))
                .thenReturn(List.of(primaryMediaObject));

        when(internalMediaDomainService.getDownloadUrl(eq("public/hotels/1/properties/facade.jpg"), isNull()))
                .thenReturn("http://localhost:9000/bucket/facade.jpg");

        List<HotelImageResponse> result = hotelMediaService.uploadImages(hotelId, List.of(file), "admin");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPrimary()).isTrue();
        assertThat(result.get(0).getUrl()).isEqualTo("http://localhost:9000/bucket/facade.jpg");
    }

    @Test
    @DisplayName("Đặt ảnh làm đại diện chính (setPrimaryImage) thành công: đổi ảnh cũ sang false, ảnh mới sang true")
    void setPrimaryImage_success() {
        when(hotelRepository.existsById(hotelId)).thenReturn(true);

        MediaObject targetMedia = MediaObject.builder()
                .id(202L)
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .referenceId(1L)
                .isPrimary(false)
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(202L)).thenReturn(Optional.of(targetMedia));

        MediaObject oldPrimary = MediaObject.builder()
                .id(201L)
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .referenceId(1L)
                .isPrimary(true)
                .build();

        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                eq(StorageCategory.HOTEL_PROPERTY_PHOTO), eq(1L)))
                .thenReturn(Optional.of(oldPrimary));

        hotelMediaService.setPrimaryImage(hotelId, 202L);

        assertThat(oldPrimary.getIsPrimary()).isFalse();
        verify(mediaObjectRepository).saveAndFlush(oldPrimary);
        assertThat(targetMedia.getIsPrimary()).isTrue();
        verify(mediaObjectRepository).save(targetMedia);
    }

    @Test
    @DisplayName("Xóa ảnh đại diện chính (deleteImage): tự động thăng cấp ảnh còn lại thành isPrimary = true")
    void deleteImage_primaryDeleted_promotesNextImage() {
        when(hotelRepository.existsById(hotelId)).thenReturn(true);

        MediaObject primaryMedia = MediaObject.builder()
                .id(301L)
                .storageKey("public/hotels/1/properties/primary.jpg")
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .referenceId(1L)
                .isPrimary(true)
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(301L)).thenReturn(Optional.of(primaryMedia));

        MediaObject nextCandidate = MediaObject.builder()
                .id(302L)
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .referenceId(1L)
                .isPrimary(false)
                .build();

        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                eq(StorageCategory.HOTEL_PROPERTY_PHOTO), eq(1L)))
                .thenReturn(List.of(primaryMedia, nextCandidate));

        hotelMediaService.deleteImage(hotelId, 301L);

        verify(internalMediaDomainService).deleteMedia("public/hotels/1/properties/primary.jpg");
        assertThat(nextCandidate.getIsPrimary()).isTrue();
        verify(mediaObjectRepository).save(nextCandidate);
    }

    @Test
    @DisplayName("Ném lỗi HOTEL_NOT_FOUND khi hotelId không tồn tại")
    void operations_hotelNotFound_throwsException() {
        when(hotelRepository.existsById((short) 99)).thenReturn(false);

        AppException ex1 = assertThrows(AppException.class, () -> hotelMediaService.getHotelImages((short) 99));
        assertThat(ex1.getErrorCode()).isEqualTo(ErrorCode.HOTEL_NOT_FOUND);

        AppException ex2 = assertThrows(AppException.class, () -> hotelMediaService.deleteImage((short) 99, 1L));
        assertThat(ex2.getErrorCode()).isEqualTo(ErrorCode.HOTEL_NOT_FOUND);
    }

    @Test
    @DisplayName("Ném lỗi MEDIA_NOT_FOUND khi ảnh không thuộc về khách sạn")
    void setPrimaryImage_wrongHotel_throwsException() {
        when(hotelRepository.existsById(hotelId)).thenReturn(true);

        MediaObject mediaOfOtherHotel = MediaObject.builder()
                .id(401L)
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .referenceId(999L) // thuộc hotel khác
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(401L)).thenReturn(Optional.of(mediaOfOtherHotel));

        AppException ex = assertThrows(AppException.class, () -> hotelMediaService.setPrimaryImage(hotelId, 401L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDIA_NOT_FOUND);
    }
}

