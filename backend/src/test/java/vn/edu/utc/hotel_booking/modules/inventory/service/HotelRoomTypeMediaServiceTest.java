package vn.edu.utc.hotel_booking.modules.inventory.service;

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
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.HotelRoomTypeMediaServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelRoomTypeMediaService — Unit Tests")
class HotelRoomTypeMediaServiceTest {

    @Mock
    private HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    private MediaObjectRepository mediaObjectRepository;

    @Mock
    private InternalMediaDomainService internalMediaDomainService;

    @InjectMocks
    private HotelRoomTypeMediaServiceImpl hotelRoomTypeMediaService;

    private final Integer hotelRoomTypeId = 101;

    @Test
    @DisplayName("getImages - Thành công trả về danh sách ảnh của hạng phòng")
    void getImages_success() {
        when(hotelRoomTypeRepository.existsById(hotelRoomTypeId)).thenReturn(true);

        MediaObject mo1 = MediaObject.builder()
                .id(1L)
                .storageKey("public/hotels/1/rooms/101/room1.jpg")
                .originalFilename("room1.jpg")
                .sizeBytes(1024L)
                .isPrimary(true)
                .build();

        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                StorageCategory.HOTEL_ROOM_PHOTO, hotelRoomTypeId.longValue()))
                .thenReturn(List.of(mo1));
        when(internalMediaDomainService.getDownloadUrl(eq("public/hotels/1/rooms/101/room1.jpg"), isNull()))
                .thenReturn("http://localhost:9000/download/room1.jpg");

        List<HotelImageResponse> result = hotelRoomTypeMediaService.getImages(hotelRoomTypeId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUrl()).isEqualTo("http://localhost:9000/download/room1.jpg");
        assertThat(result.getFirst().isPrimary()).isTrue();
    }

    @Test
    @DisplayName("getImages - Ném HOTEL_ROOM_TYPE_NOT_FOUND khi hạng phòng không tồn tại")
    void getImages_roomTypeNotFound() {
        when(hotelRoomTypeRepository.existsById(999)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> hotelRoomTypeMediaService.getImages(999));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND);
    }

    @Test
    @DisplayName("uploadImages - Tải lên ảnh thành công cho hạng phòng")
    void uploadImages_success() {
        Hotel mockHotel = Hotel.builder().id((short) 1).build();
        HotelRoomType mockHrt = HotelRoomType.builder().id(hotelRoomTypeId).hotel(mockHotel).build();

        when(hotelRoomTypeRepository.findById(hotelRoomTypeId)).thenReturn(Optional.of(mockHrt));
        when(hotelRoomTypeRepository.existsById(hotelRoomTypeId)).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("files", "room.jpg", "image/jpeg", new byte[100]);
        MediaUploadResponse uploadRes = MediaUploadResponse.builder().mediaId(10L).build();

        when(internalMediaDomainService.upload(any(MultipartFile.class), eq(StorageCategory.HOTEL_ROOM_PHOTO), eq((short) 1), eq(101L), any()))
                .thenReturn(uploadRes);

        MediaObject savedMo = MediaObject.builder()
                .id(10L)
                .storageKey("public/hotels/1/rooms/101/room.jpg")
                .originalFilename("room.jpg")
                .sizeBytes(100L)
                .isPrimary(false)
                .build();

        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                eq(StorageCategory.HOTEL_ROOM_PHOTO), eq(101L)))
                .thenReturn(Optional.empty());

        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsDeletedFalseOrderByIsPrimaryDescCreatedAtDesc(
                eq(StorageCategory.HOTEL_ROOM_PHOTO), eq(101L)))
                .thenReturn(List.of(savedMo));

        when(mediaObjectRepository.save(any(MediaObject.class))).thenReturn(savedMo);
        when(internalMediaDomainService.getDownloadUrl(eq("public/hotels/1/rooms/101/room.jpg"), isNull()))
                .thenReturn("http://localhost:9000/download/room.jpg");

        List<HotelImageResponse> result = hotelRoomTypeMediaService.uploadImages(hotelRoomTypeId, List.of(file), "admin");

        assertThat(result).hasSize(1);
        verify(internalMediaDomainService).upload(any(), eq(StorageCategory.HOTEL_ROOM_PHOTO), eq((short) 1), eq(101L), eq("admin"));
    }

    @Test
    @DisplayName("setPrimaryImage - Đặt ảnh đại diện chính thành công")
    void setPrimaryImage_success() {
        when(hotelRoomTypeRepository.existsById(hotelRoomTypeId)).thenReturn(true);

        MediaObject target = MediaObject.builder()
                .id(20L)
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .referenceId(101L)
                .isPrimary(false)
                .build();

        MediaObject oldPrimary = MediaObject.builder()
                .id(10L)
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .referenceId(101L)
                .isPrimary(true)
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(target));
        when(mediaObjectRepository.findByCategoryAndReferenceIdAndIsPrimaryTrueAndIsDeletedFalse(
                StorageCategory.HOTEL_ROOM_PHOTO, 101L))
                .thenReturn(Optional.of(oldPrimary));

        hotelRoomTypeMediaService.setPrimaryImage(hotelRoomTypeId, 20L);

        assertThat(oldPrimary.getIsPrimary()).isFalse();
        assertThat(target.getIsPrimary()).isTrue();
        verify(mediaObjectRepository).save(target);
    }

    @Test
    @DisplayName("deleteImage - Xóa ảnh thành công")
    void deleteImage_success() {
        when(hotelRoomTypeRepository.existsById(hotelRoomTypeId)).thenReturn(true);

        MediaObject target = MediaObject.builder()
                .id(30L)
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .referenceId(101L)
                .storageKey("public/hotels/1/rooms/101/to_delete.jpg")
                .isPrimary(false)
                .build();

        when(mediaObjectRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(target));

        hotelRoomTypeMediaService.deleteImage(hotelRoomTypeId, 30L);

        verify(internalMediaDomainService).deleteMedia("public/hotels/1/rooms/101/to_delete.jpg");
    }
}

