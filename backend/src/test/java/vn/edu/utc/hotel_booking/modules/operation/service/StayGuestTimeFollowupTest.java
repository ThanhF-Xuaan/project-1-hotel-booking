package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.utc.hotel_booking.common.exception.*;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.*;
import vn.edu.utc.hotel_booking.modules.operation.entity.*;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapperImpl;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.StayGuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import java.time.OffsetDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StayGuestTimeFollowupTest {
    final StayGuestRepository guests=mock(StayGuestRepository.class);
    final LodgingQueueRepository queues=mock(LodgingQueueRepository.class);
    final HotelRepository hotels=mock(HotelRepository.class);
    final Hotel hotel=Hotel.builder().id((short)1).name("SYNTHETIC").build();
    final StayGuestServiceImpl service=new StayGuestServiceImpl(guests,queues,hotels,mock(BookingLookupService.class),
            mock(RoomInstanceRepository.class),mock(StaffRepository.class),new StayGuestMapperImpl());
    final OffsetDateTime checkIn=OffsetDateTime.parse("2026-11-03T12:00:00+07:00");
    @BeforeEach void setup() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-admin","",AuthorityUtils.createAuthorityList("ROLE_CHAIN_ADMIN")));
        ReflectionTestUtils.setField(service,"entityManager",mock(jakarta.persistence.EntityManager.class));
        when(hotels.findByIdAndIsDeletedFalse((short)1)).thenReturn(Optional.of(hotel));
        when(guests.save(any())).thenAnswer(call->call.getArgument(0));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    StayGuest guest() {
        var guest=StayGuest.builder().id(11L).hotel(hotel).roomNumber("S101").fullName("SYNTHETIC")
            .documentNumber("SYNTHETIC_ONLY").checkInTime(checkIn).expectedCheckOutTime(checkIn.plusDays(1)).build();
        when(guests.findById(11L)).thenReturn(Optional.of(guest)); return guest;
    }
    void invalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(AppException.class,
                e->assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA));
        verify(guests,never()).save(any()); verify(queues,never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings={"2026-11-03T05:00:00Z","2026-11-03T04:59:59Z"})
    void createRejectsCheckoutAtOrBeforeCheckInInstant(String checkout) {
        invalid(()->service.createOrCheckIn(StayGuestCreateRequest.builder().hotelId((short)1).roomNumber("S101")
                .fullName("SYNTHETIC").documentNumber("SYNTHETIC_ONLY").checkInTime(checkIn)
                .expectedCheckOutTime(OffsetDateTime.parse(checkout)).build()));
    }
    @Test void updateInvalidExpectedDoesNotMutateManagedGuest() {
        var g=guest(); var before=g.getExpectedCheckOutTime();
        invalid(()->service.update(11L,StayGuestUpdateRequest.builder().expectedCheckOutTime(checkIn).build()));
        assertThat(g.getExpectedCheckOutTime()).isEqualTo(before);
    }
    @Test void actualCheckoutCannotPrecedeCheckIn() {
        var g=guest(); invalid(()->service.update(11L,StayGuestUpdateRequest.builder().actualCheckOutTime(checkIn.minusSeconds(1)).build()));
        assertThat(g.getActualCheckOutTime()).isNull();
    }
    @Test void actualMayEqualCheckInOrExceedExpected() {
        var g=guest(); service.update(11L,StayGuestUpdateRequest.builder().actualCheckOutTime(checkIn).build());
        assertThat(g.getActualCheckOutTime()).isEqualTo(checkIn);
        service.update(11L,StayGuestUpdateRequest.builder().actualCheckOutTime(checkIn.plusDays(2)).build());
        assertThat(g.getActualCheckOutTime()).isEqualTo(checkIn.plusDays(2));
    }
    @Test void unrelatedLegacyEditDoesNotRepairInvalidDates() {
        var g=guest();g.setExpectedCheckOutTime(checkIn.minusDays(1));
        service.update(11L,StayGuestUpdateRequest.builder().fullName("SYNTHETIC_EDIT").build());
        assertThat(g.getExpectedCheckOutTime()).isEqualTo(checkIn.minusDays(1));
        assertThat(g.getFullName()).isEqualTo("SYNTHETIC_EDIT");
    }
    @Test void exportedNoopKeepsLegacyDatesAndMarkers() {
        var g=guest();g.setExpectedCheckOutTime(checkIn.minusDays(1));
        var q=LodgingQueue.builder().stayGuest(g).status(LodgingQueueStatus.EXPORTED).batchReference("SYNTHETIC_BATCH").exportedAt(checkIn).build();
        when(queues.findByStayGuestIdAndIsDeletedFalse(11L)).thenReturn(Optional.of(q));
        service.update(11L,StayGuestUpdateRequest.builder().build());
        assertThat(q.getBatchReference()).isEqualTo("SYNTHETIC_BATCH");verify(guests,never()).save(any());
        assertThatThrownBy(()->service.update(11L,StayGuestUpdateRequest.builder().expectedCheckOutTime(checkIn).build()))
                .isInstanceOfSatisfying(AppException.class,e->assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LODGING_EXPORTED_RECORD_LOCKED));
    }
    @Test void checkInSyncValidatesBeforeChangingManagedGuest() {
        var g=guest();g.setCheckInTime(null);g.setExpectedCheckOutTime(checkIn.minusYears(1));
        when(guests.findByBookingRoomId(50L)).thenReturn(List.of(g));
        invalid(()->service.syncLodgingQueueForBookingRoom(50L));assertThat(g.getCheckInTime()).isNull();
    }
    @Test void incompleteLegacyEndpointRemainsEditable() {
        var g=guest();g.setCheckInTime(null);
        service.update(11L,StayGuestUpdateRequest.builder().expectedCheckOutTime(checkIn.minusDays(1)).build());
        assertThat(g.getCheckInTime()).isNull();
    }
}
