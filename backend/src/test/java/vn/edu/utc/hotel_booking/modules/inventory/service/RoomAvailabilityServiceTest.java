package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomAvailabilityMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomAvailabilityServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityServiceTest {

    @Mock
    RoomAvailabilityRepository roomAvailabilityRepository;

    @Mock
    HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    RoomAvailabilityMapper roomAvailabilityMapper;

    @InjectMocks
    RoomAvailabilityServiceImpl roomAvailabilityService;

    HotelRoomType hotelRoomType;

    @BeforeEach
    void setUp() {
        RoomType roomType = RoomType.builder()
                .id((short) 1)
                .code("DELUXE")
                .name("Phòng Deluxe")
                .build();

        hotelRoomType = HotelRoomType.builder()
                .id(10)
                .roomType(roomType)
                .basePrice(BigDecimal.valueOf(1000000))
                .totalQuantity(5)
                .standardAdults((short) 2)
                .maxAdults((short) 3)
                .maxTotalGuests((short) 4)
                .build();
    }

    @Test
    @DisplayName("getAvailability returns sparse default data when no records exist")
    void testGetAvailability_SparseDefault() {
        RoomAvailabilitySearchDto searchDto = RoomAvailabilitySearchDto.builder()
                .hotelRoomTypeId(10)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 2))
                .build();

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(hotelRoomType));
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDateBetweenOrderByDateAsc(eq(10), any(), any()))
                .thenReturn(Collections.emptyList());

        List<RoomAvailabilityResponse> list = roomAvailabilityService.getAvailability(searchDto);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getAvailableCount()).isEqualTo(5);
        assertThat(list.get(0).getBookedRooms()).isEqualTo(0);
        assertThat(list.get(0).getLockedRooms()).isEqualTo(0);
    }

    @Test
    @DisplayName("holdInventory successfully creates new availability row when sparse")
    void testHoldInventory_SparseSuccess() {
        HoldInventoryRequest request = HoldInventoryRequest.builder()
                .hotelRoomTypeId(10)
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 2))
                .roomsCount(2)
                .holdMinutes(15)
                .build();

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(hotelRoomType));
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.empty());

        roomAvailabilityService.holdInventory(request);

        verify(roomAvailabilityRepository, times(1)).save(any(RoomAvailability.class));
    }

    @Test
    @DisplayName("holdInventory throws Exception when requested rooms exceed availability")
    void testHoldInventory_ExceedsCapacity() {
        HoldInventoryRequest request = HoldInventoryRequest.builder()
                .hotelRoomTypeId(10)
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 2))
                .roomsCount(10) // Total is only 5
                .holdMinutes(15)
                .build();

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(hotelRoomType));
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomAvailabilityService.holdInventory(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_AVAILABLE);

        verify(roomAvailabilityRepository, never()).save(any(RoomAvailability.class));
    }

    @Test
    @DisplayName("releaseInventory successfully decreases locked rooms")
    void testReleaseInventory_Success() {
        ReleaseInventoryRequest request = ReleaseInventoryRequest.builder()
                .hotelRoomTypeId(10)
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 2))
                .roomsCount(2)
                .build();

        RoomAvailability availability = RoomAvailability.builder()
                .hotelRoomType(hotelRoomType)
                .date(LocalDate.of(2026, 10, 1))
                .totalRooms(5)
                .bookedRooms(0)
                .lockedRooms(3)
                .oooRooms(0)
                .build();

        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(availability));

        roomAvailabilityService.releaseInventory(request);

        assertThat(availability.getLockedRooms()).isEqualTo(1);
        verify(roomAvailabilityRepository, times(1)).save(availability);
    }

    @Test
    @DisplayName("confirmBookingInventory moves locked rooms to booked rooms")
    void testConfirmBookingInventory_Success() {
        RoomAvailability availability = RoomAvailability.builder()
                .hotelRoomType(hotelRoomType)
                .date(LocalDate.of(2026, 10, 1))
                .totalRooms(5)
                .bookedRooms(1)
                .lockedRooms(2)
                .oooRooms(0)
                .build();

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(hotelRoomType));
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(availability));

        roomAvailabilityService.confirmBookingInventory(10, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), 2);

        assertThat(availability.getLockedRooms()).isEqualTo(0);
        assertThat(availability.getBookedRooms()).isEqualTo(3);
        verify(roomAvailabilityRepository, times(1)).save(availability);
    }
}
