package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.HotelRoomTypeMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomFeatureRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.HotelRoomTypeServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelRoomTypeServiceTest {

    @Mock
    HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    HotelRepository hotelRepository;

    @Mock
    RoomTypeRepository roomTypeRepository;

    @Mock
    RoomFeatureRepository roomFeatureRepository;

    @Mock
    HotelRoomTypeMapper hotelRoomTypeMapper;

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
}
