package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.StayGuestRepository;
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
    private BookingRepository bookingRepository;

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

    @Test
    void createOrCheckIn_validData_createsStayGuestAndPendingQueue() {
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
        when(stayGuestRepository.findByHotelIdAndDocumentNumberAndIsDeletedFalse((short) 1, "001200001234"))
                .thenReturn(Optional.empty());

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
    void createOrCheckIn_missingPermanentAddressForVietnamese_createsErrorQueue() {
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
        when(stayGuestRepository.findByHotelIdAndDocumentNumberAndIsDeletedFalse((short) 1, "001200005678"))
                .thenReturn(Optional.empty());

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
}
