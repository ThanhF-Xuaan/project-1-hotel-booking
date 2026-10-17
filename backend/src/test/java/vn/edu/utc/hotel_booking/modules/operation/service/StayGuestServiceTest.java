package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.StayGuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StayGuestServiceTest {

    @Mock
    private StayGuestRepository stayGuestRepository;

    @Mock
    private LodgingQueueRepository lodgingQueueRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private BookingLookupService bookingLookupService;

    @Mock
    private RoomInstanceRepository roomInstanceRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StayGuestMapper stayGuestMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StayGuestServiceImpl stayGuestService;

    private Hotel sampleHotel;

    @BeforeEach
    void setUp() {
        sampleHotel = Hotel.builder().id((short) 1).name("Test Hotel").build();
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockChainAdminAuth() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CHAIN_ADMIN"))).when(authentication).getAuthorities();
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void createOrCheckIn_validData_createsStayGuestAndPendingQueue() {
        mockChainAdminAuth();

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .permanentAddress("Hà Nội")
                .currentAddress("Hà Nội")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(2))
                .reasonForStay("Du lịch")
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        when(stayGuestRepository.findFirstByHotelIdAndRoomNumberAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                (short) 1, "101", "001200001234")).thenReturn(Optional.empty());

        StayGuest mappedGuest = StayGuest.builder()
                .id(10L)
                .hotel(sampleHotel)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .permanentAddress("Hà Nội")
                .checkInTime(request.getCheckInTime())
                .expectedCheckOutTime(request.getExpectedCheckOutTime())
                .build();

        when(stayGuestMapper.toEntity(request)).thenReturn(mappedGuest);
        when(stayGuestRepository.save(any(StayGuest.class))).thenReturn(mappedGuest);
        when(lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        StayGuestResponse expectedResponse = StayGuestResponse.builder().id(10L).fullName("NGUYỄN VĂN A").build();
        when(stayGuestMapper.toResponse(mappedGuest)).thenReturn(expectedResponse);

        StayGuestResponse response = stayGuestService.createOrCheckIn(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());

        ArgumentCaptor<LodgingQueue> queueCaptor = ArgumentCaptor.forClass(LodgingQueue.class);
        verify(lodgingQueueRepository, times(1)).save(queueCaptor.capture());
        assertEquals(LodgingQueueStatus.PENDING, queueCaptor.getValue().getStatus());
        assertNull(queueCaptor.getValue().getErrorMessage());
    }

    @Test
    void createOrCheckIn_returningGuest_createsNewStayGuestPreservingHistory() {
        mockChainAdminAuth();

        // Guest checked out in a past stay
        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .roomNumber("102")
                .fullName("NGUYỄN VĂN A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .permanentAddress("Hà Nội")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(2))
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        // No active stay (past stay has actualCheckOutTime != null so repository query returns empty)
        when(stayGuestRepository.findFirstByHotelIdAndRoomNumberAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                (short) 1, "102", "001200001234")).thenReturn(Optional.empty());

        StayGuest newStayGuest = StayGuest.builder()
                .id(20L)
                .hotel(sampleHotel)
                .roomNumber("102")
                .fullName("NGUYỄN VĂN A")
                .documentNumber("001200001234")
                .build();

        when(stayGuestMapper.toEntity(request)).thenReturn(newStayGuest);
        when(stayGuestRepository.save(newStayGuest)).thenReturn(newStayGuest);
        when(lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(20L)).thenReturn(Optional.empty());
        when(stayGuestMapper.toResponse(newStayGuest)).thenReturn(StayGuestResponse.builder().id(20L).build());

        StayGuestResponse response = stayGuestService.createOrCheckIn(request);

        assertNotNull(response);
        assertEquals(20L, response.getId());
        // Verify toEntity was called to create a NEW entity rather than updating existing
        verify(stayGuestMapper, times(1)).toEntity(request);
        verify(stayGuestRepository, times(1)).save(newStayGuest);
    }

    @Test
    void createOrCheckIn_activeStaySameBooking_updatesExistingStayGuest() {
        mockChainAdminAuth();

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .bookingId(100L)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN A ĐÃ SỬA")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .permanentAddress("Hà Nội")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(2))
                .build();

        Booking booking = Booking.builder().id(100L).hotel(sampleHotel).build();
        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(booking);

        StayGuest existingActiveStay = StayGuest.builder()
                .id(10L)
                .hotel(sampleHotel)
                .booking(booking)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN A CŨ")
                .documentNumber("001200001234")
                .build();

        when(stayGuestRepository.findFirstByHotelIdAndBookingIdAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                (short) 1, 100L, "001200001234")).thenReturn(Optional.of(existingActiveStay));
        when(stayGuestRepository.save(existingActiveStay)).thenReturn(existingActiveStay);
        when(lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());
        when(stayGuestMapper.toResponse(existingActiveStay)).thenReturn(StayGuestResponse.builder().id(10L).fullName("NGUYỄN VĂN A ĐÃ SỬA").build());

        StayGuestResponse response = stayGuestService.createOrCheckIn(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("NGUYỄN VĂN A ĐÃ SỬA", existingActiveStay.getFullName());
        // Verify mapper.toEntity was NOT called because existing record was updated
        verify(stayGuestMapper, never()).toEntity(request);
    }

    @Test
    void createOrCheckIn_reuseOldCompletedBookingId_createsNewStayAndDoesNotCorruptCompletedStay() {
        mockChainAdminAuth();

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .bookingId(100L)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN A")
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(2))
                .build();

        Booking booking = Booking.builder().id(100L).hotel(sampleHotel).build();
        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(booking);

        // Booking 100 has completed stay (actualCheckOutTime != null), so active stay query returns empty
        when(stayGuestRepository.findFirstByHotelIdAndBookingIdAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                (short) 1, 100L, "001200001234")).thenReturn(Optional.empty());

        StayGuest newGuest = StayGuest.builder()
                .id(99L)
                .hotel(sampleHotel)
                .booking(booking)
                .documentNumber("001200001234")
                .build();

        when(stayGuestMapper.toEntity(request)).thenReturn(newGuest);
        when(stayGuestRepository.save(newGuest)).thenReturn(newGuest);
        when(lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());
        when(stayGuestMapper.toResponse(newGuest)).thenReturn(StayGuestResponse.builder().id(99L).build());

        StayGuestResponse response = stayGuestService.createOrCheckIn(request);

        assertNotNull(response);
        assertEquals(99L, response.getId());
        // Verifies new entity is created, preserving historical completed stay
        verify(stayGuestMapper, times(1)).toEntity(request);
        verify(stayGuestRepository, times(1)).save(newGuest);
    }

    @Test
    void createOrCheckIn_crossHotelBooking_throwsInvalidRequestData() {
        mockChainAdminAuth();

        Hotel otherHotel = Hotel.builder().id((short) 2).name("Other Hotel").build();
        Booking booking = Booking.builder().id(200L).hotel(otherHotel).build();

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .bookingId(200L)
                .documentNumber("001200001234")
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        when(bookingLookupService.getBookingEntityById(200L)).thenReturn(booking);

        AppException ex = assertThrows(AppException.class, () -> stayGuestService.createOrCheckIn(request));
        assertEquals(ErrorCode.INVALID_REQUEST_DATA, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Đơn đặt phòng không thuộc khách sạn này"));
    }

    @Test
    void createOrCheckIn_crossHotelRoom_throwsInvalidRequestData() {
        mockChainAdminAuth();

        Hotel otherHotel = Hotel.builder().id((short) 2).name("Other Hotel").build();
        RoomInstance room = RoomInstance.builder().id(50).hotel(otherHotel).build();

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .roomId(50)
                .documentNumber("001200001234")
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        when(roomInstanceRepository.findById(50)).thenReturn(Optional.of(room));

        AppException ex = assertThrows(AppException.class, () -> stayGuestService.createOrCheckIn(request));
        assertEquals(ErrorCode.INVALID_REQUEST_DATA, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Phòng vật lý không thuộc khách sạn này"));
    }

    @Test
    void createOrCheckIn_crossPropertyStaff_throwsUnauthorized() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_PROPERTY_MANAGER"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("pm_hotel2");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Staff staff = Staff.builder()
                .username("pm_hotel2")
                .scopeType("PROPERTY")
                .scopeEntityId(2) // Belongs to Hotel 2
                .build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_hotel2")).thenReturn(Optional.of(staff));

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1) // Attempting operation on Hotel 1
                .documentNumber("001200001234")
                .build();

        AppException ex = assertThrows(AppException.class, () -> stayGuestService.createOrCheckIn(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("không có quyền thao tác trên khách sạn khác"));
    }

    @Test
    void createOrCheckIn_withBookingRoomId_linksBookingRoomCorrectly() {
        mockChainAdminAuth();

        Booking booking = Booking.builder().id(100L).hotel(sampleHotel).build();
        BookingDetail bookingDetail = BookingDetail.builder().id(1L).booking(booking).build();
        BookingRoom bookingRoom = BookingRoom.builder().id(500L).bookingDetail(bookingDetail).build();

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .bookingId(100L)
                .bookingRoomId(500L)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN A")
                .nationality("Việt Nam")
                .documentNumber("001200001234")
                .documentType(DocumentType.CCCD)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .permanentAddress("Hà Nội")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(2))
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(booking);
        when(bookingLookupService.getBookingRoomEntityById(500L)).thenReturn(bookingRoom);
        when(stayGuestRepository.findFirstByHotelIdAndBookingIdAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                (short) 1, 100L, "001200001234")).thenReturn(Optional.empty());

        StayGuest mappedGuest = StayGuest.builder()
                .id(30L)
                .hotel(sampleHotel)
                .booking(booking)
                .bookingRoom(bookingRoom)
                .documentNumber("001200001234")
                .build();

        when(stayGuestMapper.toEntity(request)).thenReturn(mappedGuest);
        when(stayGuestRepository.save(mappedGuest)).thenReturn(mappedGuest);
        when(lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(30L)).thenReturn(Optional.empty());
        when(stayGuestMapper.toResponse(mappedGuest)).thenReturn(StayGuestResponse.builder().id(30L).bookingRoomId(500L).build());

        StayGuestResponse response = stayGuestService.createOrCheckIn(request);

        assertNotNull(response);
        assertEquals(500L, response.getBookingRoomId());
        assertEquals(bookingRoom, mappedGuest.getBookingRoom());
    }

    @Test
    void createOrCheckIn_missingPermanentAddressForVietnamese_createsErrorQueue() {
        mockChainAdminAuth();

        StayGuestCreateRequest request = StayGuestCreateRequest.builder()
                .hotelId((short) 1)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN B")
                .dateOfBirth(LocalDate.of(1992, 5, 20))
                .gender(Gender.FEMALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200005678")
                .permanentAddress(null) // Thiếu trường bắt buộc cho khách VN
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(1))
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(sampleHotel));
        when(stayGuestRepository.findFirstByHotelIdAndRoomNumberAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(
                (short) 1, "101", "001200005678")).thenReturn(Optional.empty());

        StayGuest mappedGuest = StayGuest.builder()
                .id(11L)
                .hotel(sampleHotel)
                .roomNumber("101")
                .fullName("NGUYỄN VĂN B")
                .dateOfBirth(LocalDate.of(1992, 5, 20))
                .gender(Gender.FEMALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200005678")
                .permanentAddress(null)
                .checkInTime(request.getCheckInTime())
                .expectedCheckOutTime(request.getExpectedCheckOutTime())
                .build();

        when(stayGuestMapper.toEntity(request)).thenReturn(mappedGuest);
        when(stayGuestRepository.save(any(StayGuest.class))).thenReturn(mappedGuest);
        when(lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(11L)).thenReturn(Optional.empty());

        StayGuestResponse expectedResponse = StayGuestResponse.builder().id(11L).fullName("NGUYỄN VĂN B").build();
        when(stayGuestMapper.toResponse(mappedGuest)).thenReturn(expectedResponse);

        StayGuestResponse response = stayGuestService.createOrCheckIn(request);

        assertNotNull(response);

        ArgumentCaptor<LodgingQueue> queueCaptor = ArgumentCaptor.forClass(LodgingQueue.class);
        verify(lodgingQueueRepository, times(1)).save(queueCaptor.capture());
        assertEquals(LodgingQueueStatus.ERROR, queueCaptor.getValue().getStatus());
        assertTrue(queueCaptor.getValue().getErrorMessage().contains("Nơi ĐKTT / Nơi cấp"));
    }

    @Test
    void getById_notFound_throwsException() {
        when(stayGuestRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> stayGuestService.getById(999L));
        assertEquals(ErrorCode.STAY_GUEST_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void delete_softDeletesGuestAndCancelsQueue() {
        mockChainAdminAuth();

        StayGuest guest = StayGuest.builder().id(12L).hotel(sampleHotel).build();
        LodgingQueue queue = LodgingQueue.builder().id(50L).stayGuest(guest).status(LodgingQueueStatus.PENDING).build();

        when(stayGuestRepository.findAllByIdInAndIsDeletedFalse(List.of(12L))).thenReturn(List.of(guest));
        when(lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(12L)).thenReturn(Optional.of(queue));

        stayGuestService.delete(List.of(12L));

        assertTrue(guest.getIsDeleted());
        assertEquals(LodgingQueueStatus.CANCELLED, queue.getStatus());
        assertTrue(queue.getIsDeleted());
        verify(stayGuestRepository, times(1)).saveAll(any());
        verify(lodgingQueueRepository, times(1)).save(queue);
    }

    @Test
    void getEntityById_success() {
        StayGuest guest = StayGuest.builder().id(12L).hotel(sampleHotel).build();
        when(stayGuestRepository.findById(12L)).thenReturn(Optional.of(guest));

        StayGuest result = stayGuestService.getEntityById(12L);

        assertNotNull(result);
        assertEquals(12L, result.getId());
        verify(stayGuestRepository).findById(12L);
    }

    @Test
    void getEntityById_notFound_throwsException() {
        when(stayGuestRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> stayGuestService.getEntityById(999L));
        assertEquals(ErrorCode.GUEST_NOT_FOUND, ex.getErrorCode());
    }
}
