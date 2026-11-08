package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeBedItemRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeBed;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeBedId;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.HotelRoomTypeMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.HotelRoomTypeServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelRoomTypeServiceTest {

    @Mock
    HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    HotelService hotelService;

    @Mock
    RoomTypeService roomTypeService;

    @Mock
    RoomBedService roomBedService;

    @Mock
    RoomFeatureService roomFeatureService;

    @Mock
    HotelRoomTypeMapper hotelRoomTypeMapper;

    @Mock
    RoomAvailabilityGenerationService roomAvailabilityGenerationService;

    @InjectMocks
    HotelRoomTypeServiceImpl hotelRoomTypeService;

    @Test
    @DisplayName("getEntityById: Trả về HotelRoomType entity khi tồn tại và chưa bị xóa mềm")
    void testGetEntityById_Success() {
        HotelRoomType entity = HotelRoomType.builder().id(10).totalQuantity(5).build();

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(entity));

        HotelRoomType result = hotelRoomTypeService.getEntityById(10);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
    }

    @Test
    @DisplayName("getEntityById: Ném ngoại lệ HOTEL_ROOM_TYPE_NOT_FOUND khi không tìm thấy")
    void testGetEntityById_NotFound() {
        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(99))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelRoomTypeService.getEntityById(99))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND);
    }

    @Test
    @DisplayName("createHotelRoomType: Thành công khi hợp lệ")
    void testCreateHotelRoomType_Success() {
        HotelRoomTypeCreateRequest request = HotelRoomTypeCreateRequest.builder()
                .hotelId((short) 1)
                .roomTypeId((short) 2)
                .taxCategoryId(1)
                .basePrice(BigDecimal.valueOf(1000000))
                .totalQuantity(10)
                .featureIds(Set.of((short) 1))
                .beds(List.of(HotelRoomTypeBedItemRequest.builder().roomBedId((short) 1).baseQuantity((short) 2).build()))
                .build();

        Hotel hotel = Hotel.builder().id((short) 1).name("Grand Hotel").build();
        RoomType roomType = RoomType.builder().id((short) 2).name("Deluxe").build();
        RoomFeature feature = RoomFeature.builder().id((short) 1).code("BALCONY").build();
        RoomBed bed = RoomBed.builder().id((short) 1).name("King Bed").build();

        HotelRoomType entity = HotelRoomType.builder()
                .id(10)
                .maxBeds((short) 2)
                .beds(new HashSet<>())
                .features(new HashSet<>())
                .build();
        HotelRoomTypeResponse response = HotelRoomTypeResponse.builder().id(10).build();

        when(hotelService.getEntityById((short) 1)).thenReturn(hotel);
        when(roomTypeService.getEntityById((short) 2)).thenReturn(roomType);
        when(hotelRoomTypeRepository.existsByHotelIdAndRoomTypeIdAndIsDeletedFalse((short) 1, (short) 2)).thenReturn(false);
        when(hotelRoomTypeMapper.toEntity(request)).thenReturn(entity);
        when(roomFeatureService.getEntitiesByIds(Set.of((short) 1))).thenReturn(Set.of(feature));
        when(roomBedService.getEntityById((short) 1)).thenReturn(bed);
        when(hotelRoomTypeRepository.save(any(HotelRoomType.class))).thenReturn(entity);
        when(hotelRoomTypeMapper.toResponse(entity)).thenReturn(response);

        HotelRoomTypeResponse result = hotelRoomTypeService.createHotelRoomType(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
        verify(roomAvailabilityGenerationService).generateForRoomType(eq(10), any(LocalDate.class), eq(365));
    }

    @Test
    @DisplayName("updateBeds: Cập nhật thành công danh sách giường")
    void testUpdateBeds_Success() {
        HotelRoomType entity = HotelRoomType.builder().id(10).beds(new HashSet<>()).build();
        RoomBed bed = RoomBed.builder().id((short) 1).name("King Bed").build();
        HotelRoomTypeResponse response = HotelRoomTypeResponse.builder().id(10).build();

        when(hotelRoomTypeRepository.findByIdWithDetails(10)).thenReturn(Optional.of(entity));
        when(roomBedService.getEntityById((short) 1)).thenReturn(bed);
        when(hotelRoomTypeRepository.save(entity)).thenReturn(entity);
        when(hotelRoomTypeMapper.toResponse(entity)).thenReturn(response);

        List<HotelRoomTypeBedItemRequest> bedRequests = List.of(
                HotelRoomTypeBedItemRequest.builder().roomBedId((short) 1).baseQuantity((short) 1).build()
        );

        HotelRoomTypeResponse result = hotelRoomTypeService.updateBeds(10, bedRequests);
        assertThat(result).isNotNull();
        assertThat(entity.getBeds()).hasSize(1);
    }

    @Test
    @DisplayName("updateBeds: Cập nhật tại chỗ (In-Place Update) số lượng của giường đã có sẵn")
    void testUpdateBeds_InPlaceUpdate_PreservesExistingEntity() {
        RoomBed bed1 = RoomBed.builder().id((short) 1).name("Double Bed").build();
        HotelRoomType entity = HotelRoomType.builder()
                .id(10)
                .maxBeds((short) 2)
                .beds(new HashSet<>())
                .build();

        HotelRoomTypeBed existingBed = HotelRoomTypeBed.builder()
                .id(HotelRoomTypeBedId.builder().hotelRoomTypeId(10).roomBedId((short) 1).build())
                .hotelRoomType(entity)
                .roomBed(bed1)
                .baseQuantity((short) 1)
                .build();
        entity.getBeds().add(existingBed);

        HotelRoomTypeResponse response = HotelRoomTypeResponse.builder().id(10).build();

        when(hotelRoomTypeRepository.findByIdWithDetails(10)).thenReturn(Optional.of(entity));
        when(hotelRoomTypeRepository.save(entity)).thenReturn(entity);
        when(hotelRoomTypeMapper.toResponse(entity)).thenReturn(response);

        // Update quantity from 1 to 2 for existing bed1
        List<HotelRoomTypeBedItemRequest> requests = List.of(
                HotelRoomTypeBedItemRequest.builder().roomBedId((short) 1).baseQuantity((short) 2).build()
        );

        HotelRoomTypeResponse result = hotelRoomTypeService.updateBeds(10, requests);
        assertThat(result).isNotNull();
        assertThat(entity.getBeds()).hasSize(1);
        HotelRoomTypeBed updatedBed = entity.getBeds().iterator().next();
        assertThat(updatedBed.getBaseQuantity()).isEqualTo((short) 2);
        // Verify roomBedService.getEntityById was not called because it already existed in-place
        verify(roomBedService, never()).getEntityById(any());
    }

    @Test
    @DisplayName("updateBeds: Ném lỗi khi tổng số giường vượt quá maxBeds")
    void testUpdateBeds_ExceedsMaxBeds_ThrowsException() {
        HotelRoomType entity = HotelRoomType.builder()
                .id(10)
                .maxBeds((short) 1)
                .beds(new HashSet<>())
                .build();

        when(hotelRoomTypeRepository.findByIdWithDetails(10)).thenReturn(Optional.of(entity));

        List<HotelRoomTypeBedItemRequest> requests = List.of(
                HotelRoomTypeBedItemRequest.builder().roomBedId((short) 1).baseQuantity((short) 1).build(),
                HotelRoomTypeBedItemRequest.builder().roomBedId((short) 2).baseQuantity((short) 1).build()
        );

        assertThatThrownBy(() -> hotelRoomTypeService.updateBeds(10, requests))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("vượt quá số giường tối đa");
    }

    @Test
    @DisplayName("updateBeds: Ném lỗi khi trùng lặp roomBedId trong danh sách yêu cầu")
    void testUpdateBeds_DuplicateRoomBedId_ThrowsException() {
        HotelRoomType entity = HotelRoomType.builder()
                .id(10)
                .maxBeds((short) 5)
                .beds(new HashSet<>())
                .build();

        when(hotelRoomTypeRepository.findByIdWithDetails(10)).thenReturn(Optional.of(entity));

        List<HotelRoomTypeBedItemRequest> requests = List.of(
                HotelRoomTypeBedItemRequest.builder().roomBedId((short) 1).baseQuantity((short) 1).build(),
                HotelRoomTypeBedItemRequest.builder().roomBedId((short) 1).baseQuantity((short) 2).build()
        );

        assertThatThrownBy(() -> hotelRoomTypeService.updateBeds(10, requests))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("bị chọn trùng lặp");
    }

    @Test
    @DisplayName("updateFeatures: Cập nhật thành công danh sách tiện ích")
    void testUpdateFeatures_Success() {
        HotelRoomType entity = HotelRoomType.builder().id(10).features(new HashSet<>()).build();
        RoomFeature feature = RoomFeature.builder().id((short) 2).code("BATHTUB").build();
        HotelRoomTypeResponse response = HotelRoomTypeResponse.builder().id(10).build();

        when(hotelRoomTypeRepository.findByIdWithDetails(10)).thenReturn(Optional.of(entity));
        when(roomFeatureService.getEntitiesByIds(Set.of((short) 2))).thenReturn(Set.of(feature));
        when(hotelRoomTypeRepository.save(entity)).thenReturn(entity);
        when(hotelRoomTypeMapper.toResponse(entity)).thenReturn(response);

        HotelRoomTypeResponse result = hotelRoomTypeService.updateFeatures(10, Set.of((short) 2));
        assertThat(result).isNotNull();
        assertThat(entity.getFeatures()).hasSize(1);
    }
}
