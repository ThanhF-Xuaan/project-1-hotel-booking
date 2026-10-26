package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomMaintenanceBlockRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomAvailabilityGenerationServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityGenerationServiceTest {

    @Mock
    RoomAvailabilityRepository roomAvailabilityRepository;
    @Mock
    HotelRoomTypeRepository hotelRoomTypeRepository;
    @Mock
    RoomMaintenanceBlockRepository roomMaintenanceBlockRepository;

    RoomAvailabilityGenerationServiceImpl generationService;

    Hotel sampleHotel;
    HotelRoomType sampleRoomType;

    @BeforeEach
    void setUp() {
        generationService = new RoomAvailabilityGenerationServiceImpl(
                roomAvailabilityRepository,
                hotelRoomTypeRepository,
                roomMaintenanceBlockRepository
        );

        sampleHotel = Hotel.builder().id((short) 1).name("Khách sạn Hà Nội Central").build();
        sampleRoomType = HotelRoomType.builder()
                .id(10)
                .hotel(sampleHotel)
                .totalQuantity(5)
                .status("ACTIVE")
                .build();
        sampleRoomType.setIsDeleted(false);
    }

    @Test
    @DisplayName("generateForRoomType creates records for all missing dates within horizon")
    void testGenerateForRoomType_Success() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        int days = 3;

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(sampleRoomType));
        when(roomAvailabilityRepository.findExistingDates(eq(10), eq(start), eq(start.plusDays(2))))
                .thenReturn(Collections.emptySet());
        when(roomMaintenanceBlockRepository.countActiveOooByHotelRoomTypeIdAndDate(eq(10), any(LocalDate.class)))
                .thenReturn(0);

        int created = generationService.generateForRoomType(10, start, days);

        assertThat(created).isEqualTo(3);
        verify(roomAvailabilityRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("generateForRoomType skips already existing dates (Idempotent)")
    void testGenerateForRoomType_Idempotent() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        int days = 3;

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(sampleRoomType));
        when(roomAvailabilityRepository.findExistingDates(eq(10), eq(start), eq(start.plusDays(2))))
                .thenReturn(Set.of(start));
        when(roomMaintenanceBlockRepository.countActiveOooByHotelRoomTypeIdAndDate(eq(10), any(LocalDate.class)))
                .thenReturn(0);

        int created = generationService.generateForRoomType(10, start, days);

        assertThat(created).isEqualTo(2);
        verify(roomAvailabilityRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("generateForRoomType throws HOTEL_ROOM_TYPE_NOT_FOUND when room type not found")
    void testGenerateForRoomType_NotFound() {
        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> generationService.generateForRoomType(99, LocalDate.now(), 30))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND);
    }

    @Test
    @DisplayName("rollHorizonForAllActiveRoomTypes processes all active room types")
    void testRollHorizonForAllActiveRoomTypes() {
        HotelRoomType rt2 = HotelRoomType.builder().id(11).hotel(sampleHotel).totalQuantity(8).status("ACTIVE").build();
        rt2.setIsDeleted(false);
        when(hotelRoomTypeRepository.findByIsDeletedFalseAndStatus("ACTIVE")).thenReturn(List.of(sampleRoomType, rt2));
        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(sampleRoomType));
        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(11)).thenReturn(Optional.of(rt2));
        when(roomAvailabilityRepository.findExistingDates(any(), any(), any())).thenReturn(Collections.emptySet());

        int total = generationService.rollHorizonForAllActiveRoomTypes(2);

        assertThat(total).isEqualTo(4); // 2 ngày x 2 hạng phòng
    }

    @Test
    @DisplayName("rollHorizonForHotel processes all active room types of given hotel")
    void testRollHorizonForHotel() {
        when(hotelRoomTypeRepository.findByHotelIdAndIsDeletedFalse((short) 1)).thenReturn(List.of(sampleRoomType));
        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(sampleRoomType));
        when(roomAvailabilityRepository.findExistingDates(any(), any(), any())).thenReturn(Collections.emptySet());

        int total = generationService.rollHorizonForHotel((short) 1, 5);

        assertThat(total).isEqualTo(5);
    }

    @Test
    @DisplayName("propagateCapacityChange updates future records total_rooms")
    void testPropagateCapacityChange_Success() {
        when(roomAvailabilityRepository.bulkUpdateTotalRooms(eq(10), any(LocalDate.class), eq(8)))
                .thenReturn(365);

        int updated = generationService.propagateCapacityChange(10, 8);

        assertThat(updated).isEqualTo(365);
        verify(roomAvailabilityRepository, times(1)).bulkUpdateTotalRooms(eq(10), any(LocalDate.class), eq(8));
    }

    @Test
    @DisplayName("syncOooRooms updates ooo_rooms on each date in range")
    void testSyncOooRooms() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 2);

        when(roomMaintenanceBlockRepository.countActiveOooByHotelRoomTypeIdAndDate(10, start)).thenReturn(1);
        when(roomMaintenanceBlockRepository.countActiveOooByHotelRoomTypeIdAndDate(10, end)).thenReturn(2);

        generationService.syncOooRooms(10, start, end);

        verify(roomAvailabilityRepository, times(1)).updateOooRooms(10, start, 1);
        verify(roomAvailabilityRepository, times(1)).updateOooRooms(10, end, 2);
    }
}
