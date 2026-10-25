package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingQueueSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.mapper.LodgingQueueMapper;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapper;
import vn.edu.utc.hotel_booking.modules.operation.export.LodgingWorkbookWriter;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.LodgingExportServiceImpl;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.LodgingQueueServiceImpl;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.StayGuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class LodgingScopeIsolationTest {
    @Mock private StaffRepository staffRepository;
    @Mock private LodgingQueueRepository queueRepository;
    @Mock private StayGuestRepository guestRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private BookingLookupService bookingLookupService;
    @Mock private RoomInstanceRepository roomRepository;
    @Mock private LodgingQueueMapper queueMapper;
    @Mock private StayGuestMapper guestMapper;
    @Mock private LodgingWorkbookWriter workbookWriter;
    @InjectMocks private LodgingExportServiceImpl exportService;
    @InjectMocks private LodgingQueueServiceImpl queueService;
    @InjectMocks private StayGuestServiceImpl stayService;

    static Stream<Arguments> invalidScopes() {
        return Stream.of("PROPERTY_MANAGER", "RECEPTIONIST").flatMap(role -> Stream.of(
                Arguments.of(role, "PROPERTY", null),
                Arguments.of(role, "REGION", 1),
                Arguments.of(role, "CHAIN", 1),
                Arguments.of(role, null, 1),
                Arguments.of(role, "PROPERTY", 0),
                Arguments.of(role, "PROPERTY", -1),
                Arguments.of(role, "PROPERTY", 32768),
                Arguments.of(role, "PROPERTY", 65537)));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("invalidScopes")
    void invalidStaffScopeCannotReadOrWriteLodgingData(String role, String scopeType, Integer scopeId) {
        authenticateStaff(role, scopeType, scopeId);
        assertAllOperationsDenied((short) 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROPERTY_MANAGER", "RECEPTIONIST"})
    void validPropertyScopeCannotSelectAnotherHotel(String role) {
        authenticateStaff(role, "PROPERTY", 1);
        assertAllOperationsDenied((short) 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROPERTY_MANAGER", "RECEPTIONIST"})
    void validPropertyScopeDefaultsSearchToOwnHotel(String role) {
        authenticateStaff(role, "PROPERTY", 1);
        when(queueRepository.search(eq((short) 1), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(guestRepository.search(eq((short) 1), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        assertThat(queueService.search(new LodgingQueueSearchDto()).getTotalElements()).isZero();
        assertThat(stayService.search(new StayGuestSearchDto()).getTotalElements()).isZero();
        AppException empty = assertThrows(AppException.class, () -> exportService.exportBcaLodgingReport(
                LodgingExportRequest.builder().hotelId((short) 1).date(LocalDate.of(2026, 10, 25)).build()));
        assertThat(empty.getErrorCode()).isEqualTo(ErrorCode.LODGING_EXPORT_EMPTY);
        verify(queueRepository).findPendingForExport(eq((short) 1), any(), eq(true));
    }

    private void authenticateStaff(String role, String scopeType, Integer scopeId) {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("scope-test", null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role)));
        Staff staff = Staff.builder().username("scope-test").scopeType(scopeType).scopeEntityId(scopeId).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("scope-test")).thenReturn(Optional.of(staff));
    }

    private void assertAllOperationsDenied(Short requestedHotel) {
        assertDenied(() -> exportService.exportBcaLodgingReport(LodgingExportRequest.builder()
                .hotelId(requestedHotel).date(LocalDate.of(2026, 10, 25)).build()));
        LodgingQueueSearchDto queues = new LodgingQueueSearchDto();
        queues.setHotelId(requestedHotel);
        assertDenied(() -> queueService.search(queues));
        StayGuestSearchDto stays = new StayGuestSearchDto();
        stays.setHotelId(requestedHotel);
        assertDenied(() -> stayService.search(stays));
        assertDenied(() -> stayService.createOrCheckIn(StayGuestCreateRequest.builder().hotelId(requestedHotel).build()));

        verifyNoInteractions(queueRepository, guestRepository, hotelRepository,
                bookingLookupService, roomRepository, queueMapper, guestMapper);
    }

    private void assertDenied(Runnable action) {
        AppException exception = assertThrows(AppException.class, action::run);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
