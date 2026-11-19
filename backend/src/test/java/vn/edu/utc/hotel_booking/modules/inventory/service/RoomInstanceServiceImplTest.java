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
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAssignmentCandidateSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAssignmentCandidateResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomInstanceMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomInstanceServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomInstanceServiceImplTest {

    @Mock RoomInstanceRepository roomInstanceRepository;
    @Mock HotelRepository hotelRepository;
    @Mock HotelRoomTypeRepository hotelRoomTypeRepository;
    @Mock RoomSlotRepository roomSlotRepository;
    @Mock BookingRoomRepository bookingRoomRepository;
    @Mock RoomInstanceMapper roomInstanceMapper;

    @InjectMocks
    RoomInstanceServiceImpl roomInstanceService;

    RoomInstance sampleRoom;

    @BeforeEach
    void setUp() {
        sampleRoom = RoomInstance.builder()
                .id(101)
                .roomNumber("101")
                .currentStatus("DIRTY")
                .build();
    }

    @Test
    @DisplayName("getRoomsByHotelAndStatus - Trả về danh sách phòng theo hotelId và status")
    void getRoomsByHotelAndStatus_Success() {
        when(roomInstanceRepository.findByHotelIdAndCurrentStatus((short) 1, "DIRTY"))
                .thenReturn(List.of(sampleRoom));

        List<RoomInstance> rooms = roomInstanceService.getRoomsByHotelAndStatus((short) 1, "DIRTY");

        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getRoomNumber()).isEqualTo("101");
        verify(roomInstanceRepository).findByHotelIdAndCurrentStatus((short) 1, "DIRTY");
    }

    @Test
    @DisplayName("getRoomsByHotelAndStatus - Trả về rỗng khi hotelId null")
    void getRoomsByHotelAndStatus_NullHotelId_ReturnsEmpty() {
        List<RoomInstance> rooms = roomInstanceService.getRoomsByHotelAndStatus(null, "DIRTY");
        assertThat(rooms).isEmpty();
        verifyNoInteractions(roomInstanceRepository);
    }

    @Test
    @DisplayName("updateRoomStatusEntity - Cập nhật trạng thái thành công")
    void updateRoomStatusEntity_Success() {
        when(roomInstanceRepository.findByIdWithDetails(101)).thenReturn(Optional.of(sampleRoom));
        when(roomInstanceRepository.save(sampleRoom)).thenReturn(sampleRoom);

        RoomInstance updated = roomInstanceService.updateRoomStatusEntity(101, "CLEANING");

        assertThat(updated).isNotNull();
        assertThat(updated.getCurrentStatus()).isEqualTo("CLEANING");
        verify(roomInstanceRepository).save(sampleRoom);
    }

    @Test
    @DisplayName("updateRoomStatusEntity - Báo lỗi khi phòng không tồn tại")
    void updateRoomStatusEntity_NotFound_ThrowsException() {
        when(roomInstanceRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomInstanceService.updateRoomStatusEntity(999, "CLEANING"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_INSTANCE_NOT_FOUND));
    }

    @Test
    @DisplayName("countOccupiedRooms - Đếm số lượng phòng OCCUPIED thành công")
    void countOccupiedRooms_Success() {
        when(roomInstanceRepository.countOccupiedRoomsByHotelId((short) 1)).thenReturn(15L);

        int count = roomInstanceService.countOccupiedRooms((short) 1);

        assertThat(count).isEqualTo(15);
        verify(roomInstanceRepository).countOccupiedRoomsByHotelId((short) 1);
    }

    @Test
    @DisplayName("countOccupiedRooms - Trả về 0 khi hotelId là null")
    void countOccupiedRooms_NullHotelId_ReturnsZero() {
        int count = roomInstanceService.countOccupiedRooms(null);

        assertThat(count).isEqualTo(0);
        verifyNoInteractions(roomInstanceRepository);
    }

    @Test
    @DisplayName("findAvailableRoomsForAssignment - Phòng không bị khóa lịch trả về khả dụng")
    void findAvailableRoomsForAssignment_WhenNoBusySlots_ReturnsAvailable() {
        Short hotelId = 3;
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(2);

        Hotel hotel = Hotel.builder().id(hotelId).name("Viettel Boutique Sapa").build();
        RoomType roomType = RoomType.builder().id((short) 1).code("STD").name("Standard").build();
        HotelRoomType hrt = HotelRoomType.builder().id(7).hotel(hotel).roomType(roomType).build();

        RoomInstance room102 = RoomInstance.builder()
                .id(2)
                .hotel(hotel)
                .roomNumber("102A")
                .currentStatus("READY")
                .hotelRoomType(hrt)
                .build();

        when(roomInstanceRepository.findByHotelIdAndIsDeletedFalseWithDetails(hotelId))
                .thenReturn(List.of(room102));
        when(roomSlotRepository.findBusySlotsBetween(hotelId, checkIn, checkOut))
                .thenReturn(List.of());
        when(bookingRoomRepository.findOverlappingAssignedRoomsInHotel(hotelId, checkIn, checkOut))
                .thenReturn(List.of());

        RoomAssignmentCandidateSearchDto request = RoomAssignmentCandidateSearchDto.builder()
                .hotelId(hotelId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .targetHotelRoomTypeId(7)
                .build();

        List<RoomAssignmentCandidateResponse> results = roomInstanceService.findAvailableRoomsForAssignment(request);

        assertThat(results).hasSize(1);
        RoomAssignmentCandidateResponse candidate = results.get(0);
        assertThat(candidate.getId()).isEqualTo(2);
        assertThat(candidate.getRoomNumber()).isEqualTo("102A");
        assertThat(candidate.isAvailable()).isTrue();
        assertThat(candidate.getConflictReason()).isNull();
    }

    @Test
    @DisplayName("findAvailableRoomsForAssignment - Phòng có slot bận RESERVED bị đánh dấu không khả dụng")
    void findAvailableRoomsForAssignment_WhenSlotReserved_ReturnsUnavailable() {
        Short hotelId = 3;
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(2);

        Hotel hotel = Hotel.builder().id(hotelId).name("Viettel Boutique Sapa").build();
        RoomType roomType = RoomType.builder().id((short) 1).code("STD").name("Standard").build();
        HotelRoomType hrt = HotelRoomType.builder().id(7).hotel(hotel).roomType(roomType).build();

        RoomInstance room101 = RoomInstance.builder()
                .id(1)
                .hotel(hotel)
                .roomNumber("101A")
                .currentStatus("READY")
                .hotelRoomType(hrt)
                .build();

        RoomSlot busySlot = RoomSlot.builder()
                .id(2L)
                .roomInstance(room101)
                .slotDate(checkIn)
                .status("RESERVED")
                .bookingRoomId(16L)
                .build();

        when(roomInstanceRepository.findByHotelIdAndIsDeletedFalseWithDetails(hotelId))
                .thenReturn(List.of(room101));
        when(roomSlotRepository.findBusySlotsBetween(hotelId, checkIn, checkOut))
                .thenReturn(List.of(busySlot));
        when(bookingRoomRepository.findOverlappingAssignedRoomsInHotel(hotelId, checkIn, checkOut))
                .thenReturn(List.of());

        RoomAssignmentCandidateSearchDto request = RoomAssignmentCandidateSearchDto.builder()
                .hotelId(hotelId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .targetHotelRoomTypeId(7)
                .build();

        List<RoomAssignmentCandidateResponse> results = roomInstanceService.findAvailableRoomsForAssignment(request);

        assertThat(results).hasSize(1);
        RoomAssignmentCandidateResponse candidate = results.get(0);
        assertThat(candidate.getId()).isEqualTo(1);
        assertThat(candidate.isAvailable()).isFalse();
        assertThat(candidate.getConflictReason()).isEqualTo("Đã có khách đặt / lưu trú");
    }

    @Test
    @DisplayName("updateRoomStatusEntity - Khi chuyển sang READY, đồng bộ slot hôm nay về READY và giải phóng booking")
    void updateRoomStatusEntity_WhenReady_SyncsTodaySlot() {
        LocalDate today = LocalDate.now();
        RoomSlot dirtySlot = RoomSlot.builder()
                .id(4L)
                .roomInstance(sampleRoom)
                .slotDate(today)
                .status("DIRTY")
                .bookingRoomId(20L)
                .build();

        when(roomInstanceRepository.findByIdWithDetails(101)).thenReturn(Optional.of(sampleRoom));
        when(roomInstanceRepository.save(sampleRoom)).thenReturn(sampleRoom);
        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(101, today)).thenReturn(Optional.of(dirtySlot));

        RoomInstance updated = roomInstanceService.updateRoomStatusEntity(101, "READY");

        assertThat(updated.getCurrentStatus()).isEqualTo("READY");
        assertThat(dirtySlot.getStatus()).isEqualTo("READY");
        assertThat(dirtySlot.getBookingRoomId()).isNull();
        verify(roomSlotRepository).save(dirtySlot);
    }

    @Test
    @DisplayName("findAvailableRoomsForAssignment - Self healing: Slot hôm nay bị DIRTY nhưng phòng vật lý đã là READY thì giải phóng slot và cho phép xếp")
    void findAvailableRoomsForAssignment_SelfHealsDirtySlotWhenPhysicalRoomIsReady() {
        Short hotelId = 3;
        LocalDate today = LocalDate.now();
        LocalDate checkIn = today;
        LocalDate checkOut = today.plusDays(1);

        Hotel hotel = Hotel.builder().name("Sapa Hotel").build();
        hotel.setId(hotelId);
        RoomType roomType = RoomType.builder().code("STD").name("Standard").build();
        roomType.setId((short) 1);
        HotelRoomType hrt = HotelRoomType.builder().hotel(hotel).roomType(roomType).build();
        hrt.setId(7);

        RoomInstance room102 = RoomInstance.builder()
                .id(2)
                .hotel(hotel)
                .roomNumber("102A")
                .currentStatus("READY")
                .hotelRoomType(hrt)
                .build();

        RoomSlot staleDirtySlot = RoomSlot.builder()
                .id(4L)
                .roomInstance(room102)
                .slotDate(today)
                .status("DIRTY")
                .bookingRoomId(20L)
                .build();

        when(roomInstanceRepository.findByHotelIdAndIsDeletedFalseWithDetails(hotelId))
                .thenReturn(List.of(room102));
        when(roomSlotRepository.findBusySlotsBetween(hotelId, checkIn, checkOut))
                .thenReturn(List.of(staleDirtySlot));
        when(bookingRoomRepository.findOverlappingAssignedRoomsInHotel(hotelId, checkIn, checkOut))
                .thenReturn(List.of());

        RoomAssignmentCandidateSearchDto request = RoomAssignmentCandidateSearchDto.builder()
                .hotelId(hotelId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .targetHotelRoomTypeId(7)
                .build();

        List<RoomAssignmentCandidateResponse> results = roomInstanceService.findAvailableRoomsForAssignment(request);

        assertThat(results).hasSize(1);
        RoomAssignmentCandidateResponse candidate = results.get(0);
        assertThat(candidate.getId()).isEqualTo(2);
        assertThat(candidate.isAvailable()).isTrue();
        assertThat(candidate.getConflictReason()).isNull();
        assertThat(staleDirtySlot.getStatus()).isEqualTo("READY");
        assertThat(staleDirtySlot.getBookingRoomId()).isNull();
        verify(roomSlotRepository).save(staleDirtySlot);
    }
}
