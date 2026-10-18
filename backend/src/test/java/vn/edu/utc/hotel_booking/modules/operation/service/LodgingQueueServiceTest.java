package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingQueueSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.LodgingQueueResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.LodgingQueueMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.LodgingQueueServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LodgingQueueServiceTest {

    @Mock
    private LodgingQueueRepository repository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private LodgingQueueMapper mapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LodgingQueueServiceImpl lodgingQueueService;

    private Hotel sampleHotel;

    @BeforeEach
    void setUp() {
        sampleHotel = Hotel.builder().id((short) 1).name("Test Hotel").build();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void search_chainAdmin_searchesAnyHotel() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CHAIN_ADMIN"))).when(authentication).getAuthorities();
        when(securityContext.getAuthentication()).thenReturn(authentication);

        LodgingQueueSearchDto request = new LodgingQueueSearchDto();
        request.setHotelId((short) 1);
        request.setPage(0);
        request.setPageSize(10);

        Page<LodgingQueue> page = new PageImpl<>(Collections.emptyList());
        when(repository.search(eq((short) 1), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        PageResponse<LodgingQueueResponse> response = lodgingQueueService.search(request);
        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void search_propertyManager_crossHotel_throwsUnauthorized() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_PROPERTY_MANAGER"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("pm_user");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Staff staff = Staff.builder()
                .username("pm_user")
                .scopeType("PROPERTY")
                .scopeEntityId(1)
                .build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        LodgingQueueSearchDto request = new LodgingQueueSearchDto();
        request.setHotelId((short) 2); // Requesting different hotel
        request.setPage(0);
        request.setPageSize(10);

        AppException ex = assertThrows(AppException.class, () -> lodgingQueueService.search(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void retryValidate_missingFields_setsErrorStatus() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CHAIN_ADMIN"))).when(authentication).getAuthorities();
        when(securityContext.getAuthentication()).thenReturn(authentication);

        StayGuest guest = StayGuest.builder()
                .id(1L)
                .hotel(sampleHotel)
                .fullName("NGUYEN VAN A")
                .nationality("Việt Nam")
                // Missing permanent address, DOB, gender, docNumber, etc.
                .build();

        LodgingQueue queue = LodgingQueue.builder()
                .id(10L)
                .hotel(sampleHotel)
                .stayGuest(guest)
                .status(LodgingQueueStatus.ERROR)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(queue));
        when(repository.save(any(LodgingQueue.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(LodgingQueueResponse.builder().id(10L).status(LodgingQueueStatus.ERROR).build());

        LodgingQueueResponse response = lodgingQueueService.retryValidate(10L);
        assertNotNull(response);
        assertEquals(LodgingQueueStatus.ERROR, response.getStatus());
        assertEquals(LodgingQueueStatus.ERROR, queue.getStatus());
        assertNotNull(queue.getErrorMessage());
    }

    @Test
    void retryValidate_allValid_setsPendingStatus() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CHAIN_ADMIN"))).when(authentication).getAuthorities();
        when(securityContext.getAuthentication()).thenReturn(authentication);

        StayGuest guest = StayGuest.builder()
                .id(1L)
                .hotel(sampleHotel)
                .fullName("NGUYEN VAN A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .permanentAddress("Ha Noi")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(1))
                .roomNumber("101")
                .build();

        LodgingQueue queue = LodgingQueue.builder()
                .id(10L)
                .hotel(sampleHotel)
                .stayGuest(guest)
                .status(LodgingQueueStatus.ERROR)
                .errorMessage("Old error")
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(queue));
        when(repository.save(any(LodgingQueue.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(LodgingQueueResponse.builder().id(10L).status(LodgingQueueStatus.PENDING).build());

        LodgingQueueResponse response = lodgingQueueService.retryValidate(10L);
        assertNotNull(response);
        assertEquals(LodgingQueueStatus.PENDING, response.getStatus());
        assertEquals(LodgingQueueStatus.PENDING, queue.getStatus());
        assertNull(queue.getErrorMessage());
    }
}
