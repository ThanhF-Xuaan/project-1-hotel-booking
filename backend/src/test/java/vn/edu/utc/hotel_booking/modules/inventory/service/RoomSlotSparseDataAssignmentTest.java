package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAssignmentCandidateSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAssignmentCandidateResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomInstanceMapper;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomInstanceServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomSlotSparseDataAssignmentTest {

    @Mock
    RoomInstanceRepository roomInstanceRepository;

    @Mock
    HotelRepository hotelRepository;

    @Mock
    HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    RoomSlotRepository roomSlotRepository;

    @Mock
    BookingRoomRepository bookingRoomRepository;

    @Mock
    RoomInstanceMapper roomInstanceMapper;

    @InjectMocks
    RoomInstanceServiceImpl roomInstanceService;

    Hotel testHotel;
    HotelRoomType stdRoomType;
    RoomInstance room101;
    RoomInstance room102;
    RoomInstance room103;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().id((short) 1).name("Viettel Boutique Sapa").build();

        RoomType rt = RoomType.builder().id((short) 10).code("STD").name("Standard Room").build();
        stdRoomType = HotelRoomType.builder().id(1).hotel(testHotel).roomType(rt).build();

        room101 = RoomInstance.builder().id(101).roomNumber("101A").hotel(testHotel).hotelRoomType(stdRoomType).currentStatus("READY").build();
        room102 = RoomInstance.builder().id(102).roomNumber("102A").hotel(testHotel).hotelRoomType(stdRoomType).currentStatus("READY").build();
        room103 = RoomInstance.builder().id(103).roomNumber("103A").hotel(testHotel).hotelRoomType(stdRoomType).currentStatus("READY").build();
    }

    @Test
    @DisplayName("Sparse Data: Phòng không có bản ghi nào trong room_slots thì mặc định là READY (isAvailable = true)")
    void findAvailableRooms_SparseData_NoRecords_DefaultsToReady() {
        Short hotelId = 1;
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(2);

        when(roomInstanceRepository.findByHotelIdAndIsDeletedFalseWithDetails(hotelId))
                .thenReturn(List.of(room101, room102));

        // Không có bản ghi busy nào trong room_slots
        when(roomSlotRepository.findBusySlotsBetween(hotelId, checkIn, checkOut))
                .thenReturn(List.of());

        // Không có booking_rooms trùng
        when(bookingRoomRepository.findOverlappingAssignedRoomsInHotel(hotelId, checkIn, checkOut))
                .thenReturn(List.of());

        RoomAssignmentCandidateSearchDto request = RoomAssignmentCandidateSearchDto.builder()
                .hotelId(hotelId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .targetHotelRoomTypeId(1)
                .build();

        List<RoomAssignmentCandidateResponse> candidates = roomInstanceService.findAvailableRoomsForAssignment(request);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).isAvailable()).isTrue();
        assertThat(candidates.get(0).getConflictReason()).isNull();
        assertThat(candidates.get(1).isAvailable()).isTrue();
        assertThat(candidates.get(1).getConflictReason()).isNull();
    }

    @Test
    @DisplayName("Sparse Data: Phòng có slot RESERVED hoặc BLOCKED trong dải ngày thì isAvailable = false kèm lý do")
    void findAvailableRooms_WithBusySlots_MarksUnavailable() {
        Short hotelId = 1;
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = checkIn.plusDays(3);

        when(roomInstanceRepository.findByHotelIdAndIsDeletedFalseWithDetails(hotelId))
                .thenReturn(List.of(room101, room102, room103));

        // room102 có slot RESERVED, room103 có slot BLOCKED
        RoomSlot slot102 = RoomSlot.builder().roomInstance(room102).slotDate(checkIn).status("RESERVED").bookingRoomId(999L).build();
        RoomSlot slot103 = RoomSlot.builder().roomInstance(room103).slotDate(checkIn.plusDays(1)).status("BLOCKED").build();

        when(roomSlotRepository.findBusySlotsBetween(hotelId, checkIn, checkOut))
                .thenReturn(List.of(slot102, slot103));

        when(bookingRoomRepository.findOverlappingAssignedRoomsInHotel(hotelId, checkIn, checkOut))
                .thenReturn(List.of());

        RoomAssignmentCandidateSearchDto request = RoomAssignmentCandidateSearchDto.builder()
                .hotelId(hotelId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .targetHotelRoomTypeId(1)
                .build();

        List<RoomAssignmentCandidateResponse> candidates = roomInstanceService.findAvailableRoomsForAssignment(request);

        assertThat(candidates).hasSize(3);

        // room101 không có bản ghi -> mặc định READY
        RoomAssignmentCandidateResponse r101 = candidates.stream().filter(c -> c.getId().equals(101)).findFirst().orElseThrow();
        assertThat(r101.isAvailable()).isTrue();
        assertThat(r101.getConflictReason()).isNull();

        // room102 có slot RESERVED
        RoomAssignmentCandidateResponse r102 = candidates.stream().filter(c -> c.getId().equals(102)).findFirst().orElseThrow();
        assertThat(r102.isAvailable()).isFalse();
        assertThat(r102.getConflictReason()).contains("Đã có khách đặt");

        // room103 có slot BLOCKED
        RoomAssignmentCandidateResponse r103 = candidates.stream().filter(c -> c.getId().equals(103)).findFirst().orElseThrow();
        assertThat(r103.isAvailable()).isFalse();
        assertThat(r103.getConflictReason()).contains("khóa bảo trì");
    }

    @Test
    @DisplayName("Sparse Data: excludeBookingRoomId bỏ qua slot của chính bookingRoom khi đổi hoặc gán lại phòng")
    void findAvailableRooms_ExcludeBookingRoomId_IgnoresSelf() {
        Short hotelId = 1;
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);
        Long myBookingRoomId = 55L;

        when(roomInstanceRepository.findByHotelIdAndIsDeletedFalseWithDetails(hotelId))
                .thenReturn(List.of(room101));

        // Slot của room101 thuộc chính myBookingRoomId
        RoomSlot mySlot = RoomSlot.builder().roomInstance(room101).slotDate(checkIn).status("RESERVED").bookingRoomId(myBookingRoomId).build();

        when(roomSlotRepository.findBusySlotsBetween(hotelId, checkIn, checkOut))
                .thenReturn(List.of(mySlot));

        BookingRoom myBookingRoom = BookingRoom.builder().id(myBookingRoomId).roomInstance(room101).build();
        when(bookingRoomRepository.findOverlappingAssignedRoomsInHotel(hotelId, checkIn, checkOut))
                .thenReturn(List.of(myBookingRoom));

        RoomAssignmentCandidateSearchDto request = RoomAssignmentCandidateSearchDto.builder()
                .hotelId(hotelId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .targetHotelRoomTypeId(1)
                .excludeBookingRoomId(myBookingRoomId)
                .build();

        List<RoomAssignmentCandidateResponse> candidates = roomInstanceService.findAvailableRoomsForAssignment(request);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Ngày check-in không trước ngày check-out ném ngoại lệ INVALID_BOOKING_DATES")
    void findAvailableRooms_InvalidDates_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = checkIn; // Trùng ngày

        RoomAssignmentCandidateSearchDto request = RoomAssignmentCandidateSearchDto.builder()
                .hotelId((short) 1)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .build();

        assertThatThrownBy(() -> roomInstanceService.findAvailableRoomsForAssignment(request))
                .isInstanceOf(AppException.class);
    }
}
