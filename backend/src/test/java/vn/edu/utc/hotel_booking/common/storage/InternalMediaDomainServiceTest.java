package vn.edu.utc.hotel_booking.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.hotel_booking.common.storage.domain.MediaLifecycleStatus;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaUploadResponse;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicyRegistry;
import vn.edu.utc.hotel_booking.common.storage.repository.MediaObjectRepository;
import vn.edu.utc.hotel_booking.common.storage.service.InternalMediaDomainService;
import vn.edu.utc.hotel_booking.common.storage.spi.ObjectStorageService;
import vn.edu.utc.hotel_booking.common.storage.validator.FileSecurityPipeline;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("InternalMediaDomainService — Unit Tests")
class InternalMediaDomainServiceTest {

    private ObjectStorageService objectStorageService;
    private MediaObjectRepository mediaObjectRepository;
    private StoragePolicyRegistry storagePolicyRegistry;
    private StorageKeyResolver storageKeyResolver;
    private FileSecurityPipeline fileSecurityPipeline;
    private InternalMediaDomainService service;

    @BeforeEach
    void setUp() {
        objectStorageService = mock(ObjectStorageService.class);
        mediaObjectRepository = mock(MediaObjectRepository.class);
        storagePolicyRegistry = new StoragePolicyRegistry();
        storageKeyResolver = new StorageKeyResolver(storagePolicyRegistry);
        fileSecurityPipeline = mock(FileSecurityPipeline.class);

        when(objectStorageService.getBucketName()).thenReturn("hotel-booking-storage");
        when(objectStorageService.getPublicUrl(any())).thenReturn("http://localhost:9000/public/test.jpg");
        when(objectStorageService.generatePresignedUrl(any(), any())).thenReturn("http://localhost:9000/private/test.pdf?sign=1");

        service = new InternalMediaDomainService(
                objectStorageService,
                mediaObjectRepository,
                storagePolicyRegistry,
                storageKeyResolver,
                fileSecurityPipeline
        );
    }

    @Test
    @DisplayName("Upload tệp thành công: ghi metadata vào DB và đẩy nhị phân lên S3")
    void upload_shouldSaveToDbAndS3() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "room_deluxe.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        when(mediaObjectRepository.save(any(MediaObject.class))).thenAnswer(invocation -> {
            MediaObject mo = invocation.getArgument(0);
            mo.setId(100L);
            return mo;
        });

        MediaUploadResponse response = service.upload(
                file,
                StorageCategory.HOTEL_ROOM_PHOTO,
                (short) 1,
                202L,
                "staff-123"
        );

        assertThat(response).isNotNull();
        assertThat(response.getMediaId()).isEqualTo(100L);
        assertThat(response.getObjectKey()).startsWith("public/hotels/1/rooms/202/");
        assertThat(response.getChecksumSha256()).isNotBlank();
        assertThat(response.getCategory()).isEqualTo(StorageCategory.HOTEL_ROOM_PHOTO);
        assertThat(response.getPublicUrl()).isNotBlank();

        verify(fileSecurityPipeline, times(1)).validate(any(), any());
        verify(mediaObjectRepository, times(1)).save(any(MediaObject.class));
        verify(objectStorageService, times(1)).upload(any(), any(InputStream.class), anyLong(), any());
    }

    @Test
    @DisplayName("Publish tệp media: chuyển trạng thái sang PUBLISHED và quyền sang PUBLIC_READ")
    void publishMedia_shouldUpdateStatusAndAccessLevel() {
        MediaObject mo = MediaObject.builder()
                .id(50L)
                .storageKey("private/reviews/rev_1.jpg")
                .lifecycleStatus(MediaLifecycleStatus.PENDING_MODERATION)
                .accessLevel(StorageAccessLevel.PRIVATE_RESTRICTED)
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(mo));

        service.publishMedia(50L);

        assertThat(mo.getLifecycleStatus()).isEqualTo(MediaLifecycleStatus.PUBLISHED);
        assertThat(mo.getAccessLevel()).isEqualTo(StorageAccessLevel.PUBLIC_READ);
        verify(mediaObjectRepository, times(1)).save(mo);
    }

    @Test
    @DisplayName("Xóa tệp media: đánh dấu soft delete trong DB và xóa vật lý trên S3")
    void deleteMedia_shouldSoftDeleteInDbAndRemoveFromS3() {
        MediaObject mo = MediaObject.builder()
                .id(77L)
                .storageKey("public/rooms/old.jpg")
                .build();
        mo.setIsDeleted(false);

        when(mediaObjectRepository.findByStorageKeyAndIsDeletedFalse("public/rooms/old.jpg"))
                .thenReturn(Optional.of(mo));

        service.deleteMedia("public/rooms/old.jpg");

        assertThat(mo.getIsDeleted()).isTrue();
        verify(mediaObjectRepository, times(1)).save(mo);
        verify(objectStorageService, times(1)).delete("public/rooms/old.jpg");
    }
}
