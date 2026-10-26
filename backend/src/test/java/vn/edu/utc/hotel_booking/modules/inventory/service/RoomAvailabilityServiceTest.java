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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchHoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHold;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHoldStatus;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomAvailabilityMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInventoryHoldService;
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
    HotelRoomTypeService hotelRoomTypeService;

    @Mock
    RoomInventoryHoldService roomInventoryHoldService;

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

        when(hotelRoomTypeService.getEntityById(10))
                .thenReturn(hotelRoomType);
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

        when(hotelRoomTypeService.getEntityById(10))
                .thenReturn(hotelRoomType);
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

        when(hotelRoomTypeService.getEntityById(10))
                .thenReturn(hotelRoomType);
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

        when(hotelRoomTypeService.getEntityById(10))
                .thenReturn(hotelRoomType);
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(availability));

        roomAvailabilityService.confirmBookingInventory(10, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), 2);

        assertThat(availability.getLockedRooms()).isEqualTo(0);
        assertThat(availability.getBookedRooms()).isEqualTo(3);
        verify(roomAvailabilityRepository, times(1)).save(availability);
    }

    @Test
    @DisplayName("batchHoldInventory sorts items by hotelRoomTypeId ASC and holds inventory for all items")
    void testBatchHoldInventory_Success() {
        HotelRoomType hrt20 = HotelRoomType.builder()
                .id(20)
                .basePrice(BigDecimal.valueOf(2000000))
                .totalQuantity(3)
                .build();

        when(hotelRoomTypeService.getEntityById(10)).thenReturn(hotelRoomType);
        when(hotelRoomTypeService.getEntityById(20)).thenReturn(hrt20);

        // Unordered items: 20 comes before 10
        BatchHoldInventoryRequest.RoomTypeHoldItem item1 = BatchHoldInventoryRequest.RoomTypeHoldItem.builder()
                .hotelRoomTypeId(20)
                .roomsCount(1)
                .build();
        BatchHoldInventoryRequest.RoomTypeHoldItem item2 = BatchHoldInventoryRequest.RoomTypeHoldItem.builder()
                .hotelRoomTypeId(10)
                .roomsCount(2)
                .build();

        BatchHoldInventoryRequest request = BatchHoldInventoryRequest.builder()
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 2))
                .items(new java.util.ArrayList<>(List.of(item1, item2)))
                .build();

        roomAvailabilityService.batchHoldInventory(request);

        // Verify that inventory was held for both
        verify(roomAvailabilityRepository, times(2)).save(any(RoomAvailability.class));
        verify(roomInventoryHoldService, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("batchReleaseInventory releases locked rooms for multiple items")
    void testBatchReleaseInventory_Success() {
        RoomAvailability av10 = RoomAvailability.builder()
                .hotelRoomType(hotelRoomType)
                .date(LocalDate.of(2026, 10, 1))
                .totalRooms(5)
                .lockedRooms(2)
                .build();

        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(av10));

        BatchHoldInventoryRequest.RoomTypeHoldItem item = BatchHoldInventoryRequest.RoomTypeHoldItem.builder()
                .hotelRoomTypeId(10)
                .roomsCount(1)
                .build();

        BatchReleaseInventoryRequest request = BatchReleaseInventoryRequest.builder()
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 2))
                .items(List.of(item))
                .build();

        roomAvailabilityService.batchReleaseInventory(request);

        assertThat(av10.getLockedRooms()).isEqualTo(1);
        verify(roomAvailabilityRepository, times(1)).save(av10);
    }

    @Test
    @DisplayName("batchReleaseInventory with sessionId releases all granular holds for that session and marks RELEASED")
    void testBatchReleaseInventory_BySessionId() {
        String sessionId = "session-test-123";
        RoomAvailability av10 = RoomAvailability.builder()
                .hotelRoomType(hotelRoomType)
                .date(LocalDate.of(2026, 10, 1))
                .totalRooms(5)
                .lockedRooms(2)
                .build();

        RoomInventoryHold hold = RoomInventoryHold.builder()
                .id(1L)
                .sessionId(sessionId)
                .hotelRoomType(hotelRoomType)
                .holdDate(LocalDate.of(2026, 10, 1))
                .quantity(2)
                .status(RoomInventoryHoldStatus.HELD)
                .build();

        when(roomInventoryHoldService.findActiveHoldsBySessionId(sessionId))
                .thenReturn(List.of(hold));
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(av10));

        BatchReleaseInventoryRequest request = BatchReleaseInventoryRequest.builder()
                .sessionId(sessionId)
                .releaseReason(HoldReleaseReason.USER_CANCELLED)
                .build();

        roomAvailabilityService.batchReleaseInventory(request);

        assertThat(av10.getLockedRooms()).isEqualTo(0);
        verify(roomInventoryHoldService, times(1)).markHoldsReleased(List.of(hold), HoldReleaseReason.USER_CANCELLED);
    }

    @Test
    @DisplayName("commitHoldInventory transfers locked rooms to booked rooms and marks COMMITTED")
    void testCommitHoldInventory_Success() {
        String sessionId = "session-commit-123";
        RoomAvailability av10 = RoomAvailability.builder()
                .hotelRoomType(hotelRoomType)
                .date(LocalDate.of(2026, 10, 1))
                .totalRooms(5)
                .lockedRooms(2)
                .bookedRooms(1)
                .build();

        RoomInventoryHold hold = RoomInventoryHold.builder()
                .id(1L)
                .sessionId(sessionId)
                .hotelRoomType(hotelRoomType)
                .holdDate(LocalDate.of(2026, 10, 1))
                .quantity(2)
                .status(RoomInventoryHoldStatus.HELD)
                .build();

        when(roomInventoryHoldService.findActiveHoldsBySessionId(sessionId))
                .thenReturn(List.of(hold));
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(av10));

        roomAvailabilityService.commitHoldInventory(sessionId);

        assertThat(av10.getLockedRooms()).isEqualTo(0);
        assertThat(av10.getBookedRooms()).isEqualTo(3);
        verify(roomInventoryHoldService, times(1)).markHoldsCommitted(List.of(hold));
    }

    @Test
    @DisplayName("releaseExpiredHolds finds expired holds and releases locked rooms with REASON_EXPIRED")
    void testReleaseExpiredHolds_Success() {
        RoomAvailability av10 = RoomAvailability.builder()
                .hotelRoomType(hotelRoomType)
                .date(LocalDate.of(2026, 10, 1))
                .totalRooms(5)
                .lockedRooms(1)
                .build();

        RoomInventoryHold expiredHold = RoomInventoryHold.builder()
                .id(1L)
                .sessionId("expired-session")
                .hotelRoomType(hotelRoomType)
                .holdDate(LocalDate.of(2026, 10, 1))
                .quantity(1)
                .status(RoomInventoryHoldStatus.HELD)
                .build();

        when(roomInventoryHoldService.findExpiredHolds(any()))
                .thenReturn(List.of(expiredHold));
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(av10));

        int released = roomAvailabilityService.releaseExpiredHolds();

        assertThat(released).isEqualTo(1);
        assertThat(av10.getLockedRooms()).isEqualTo(0);
        verify(roomInventoryHoldService, times(1)).markHoldsReleased(List.of(expiredHold), HoldReleaseReason.EXPIRED);
    }

    @Test
    @DisplayName("getAvailability deducts group blocked rooms when BlockedInventoryProvider is present")
    void testGetAvailability_DeductsGroupBlockedRooms() {
        BlockedInventoryProvider provider = mock(BlockedInventoryProvider.class);
        when(provider.getActiveBlockedRooms(eq(10), any(LocalDate.class))).thenReturn(2);

        RoomAvailabilityServiceImpl serviceWithProvider = new RoomAvailabilityServiceImpl(
                roomAvailabilityRepository,
                hotelRoomTypeService,
                roomInventoryHoldService,
                roomAvailabilityMapper,
                Optional.of(provider)
        );

        RoomAvailabilitySearchDto searchDto = RoomAvailabilitySearchDto.builder()
                .hotelRoomTypeId(10)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 1))
                .build();

        when(hotelRoomTypeService.getEntityById(10)).thenReturn(hotelRoomType);
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDateBetweenOrderByDateAsc(eq(10), any(), any()))
                .thenReturn(Collections.emptyList());

        List<RoomAvailabilityResponse> list = serviceWithProvider.getAvailability(searchDto);

        assertThat(list).hasSize(1);
        // Total = 5, Group Blocked = 2 => Available = 3
        assertThat(list.get(0).getAvailableCount()).isEqualTo(3);
    }
}
