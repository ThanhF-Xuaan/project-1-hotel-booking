package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapperImpl;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.*;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.StayGuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StayGuestReviewRegressionTest {
    final StayGuestRepository guests = mock(StayGuestRepository.class);
    final LodgingQueueRepository queues = mock(LodgingQueueRepository.class);
    final HotelRepository hotels = mock(HotelRepository.class);
    final BookingLookupService bookings = mock(BookingLookupService.class);
    final RoomInstanceRepository rooms = mock(RoomInstanceRepository.class);
    final StaffRepository staff = mock(StaffRepository.class);
    final StayGuestServiceImpl service = new StayGuestServiceImpl(guests, queues, hotels, bookings, rooms, staff, new StayGuestMapperImpl());

    @BeforeEach void authenticate() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", mock(jakarta.persistence.EntityManager.class));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-review", "", AuthorityUtils.createAuthorityList("ROLE_RECEPTIONIST")));
        when(staff.findByUsernameAndIsDeletedFalse("synthetic-review")).thenReturn(Optional.of(
                Staff.builder().id(1).scopeType("PROPERTY").scopeEntityId(1).build()));
    }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    BookingRoom bookingRoom(short hotelId) {
        Hotel hotel = Hotel.builder().id(hotelId).name("SYNTHETIC").build();
        return BookingRoom.builder().id(90L)
                .bookingDetail(BookingDetail.builder().booking(Booking.builder().id(50L).hotel(hotel).build()).build())
                .roomInstance(RoomInstance.builder().id(10).hotel(hotel).roomNumber("S101").build()).build();
    }

    @Test void bookingRoomReadMustAuthorizeBeforeReadingGuests() {
        when(bookings.getBookingRoomEntityById(90L)).thenReturn(bookingRoom((short)2));
        assertThatThrownBy(() -> service.getByBookingRoomId(90L)).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verifyNoInteractions(guests, queues);
    }

    @Test void ownBookingRoomUsesScopedActiveQuery() {
        when(bookings.getBookingRoomEntityById(90L)).thenReturn(bookingRoom((short)1));
        when(guests.findByBookingRoomIdAndHotelIdAndIsDeletedFalse(90L, (short)1)).thenReturn(List.of());
        assertThat(service.getByBookingRoomId(90L)).isEmpty();
        verify(guests).findByBookingRoomIdAndHotelIdAndIsDeletedFalse(90L, (short)1);
        verify(guests, never()).findByBookingRoomId(any());
    }

    @Test void administratorStillUsesBookingRoomHotelScope() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-admin", "", AuthorityUtils.createAuthorityList("ROLE_CHAIN_ADMIN")));
        when(bookings.getBookingRoomEntityById(90L)).thenReturn(bookingRoom((short)2));
        when(guests.findByBookingRoomIdAndHotelIdAndIsDeletedFalse(90L, (short)2)).thenReturn(List.of());
        assertThat(service.getByBookingRoomId(90L)).isEmpty();
        verify(guests).findByBookingRoomIdAndHotelIdAndIsDeletedFalse(90L, (short)2);
    }

    @Test void missingBookingRoomDoesNotReadGuests() {
        when(bookings.getBookingRoomEntityById(90L)).thenThrow(new AppException(ErrorCode.BOOKING_ROOM_NOT_FOUND));
        assertThatThrownBy(() -> service.getByBookingRoomId(90L)).isInstanceOf(AppException.class);
        verifyNoInteractions(guests);
    }

    StayGuestCreateRequest createRequest() {
        return StayGuestCreateRequest.builder().hotelId((short)1).bookingRoomId(90L)
                .roomNumber("S101").fullName("SYNTHETIC").documentNumber("SYNTHETIC_ONLY").build();
    }

    void prepareCreate(BookingRoom room) {
        when(hotels.findByIdAndIsDeletedFalse((short)1)).thenReturn(Optional.of(bookingRoom((short)1).getBookingDetail().getBooking().getHotel()));
        when(bookings.getBookingRoomEntityById(90L)).thenReturn(room);
        when(guests.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void createWithOnlyForeignBookingRoomMustFailBeforeSave() {
        prepareCreate(bookingRoom((short)2));
        assertThatThrownBy(() -> service.createOrCheckIn(createRequest())).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA));
        verify(guests, never()).save(any());
        verifyNoInteractions(queues);
    }

    @Test void updateWalkInCannotAttachForeignBookingRoom() {
        StayGuest guest = StayGuest.builder().id(21L).hotel(bookingRoom((short)1).getBookingDetail().getBooking().getHotel()).build();
        when(guests.findById(21L)).thenReturn(Optional.of(guest));
        when(bookings.getBookingRoomEntityById(90L)).thenReturn(bookingRoom((short)2));
        when(guests.save(any())).thenAnswer(call -> call.getArgument(0));
        assertThatThrownBy(() -> service.update(21L, StayGuestUpdateRequest.builder().bookingRoomId(90L).build()))
                .isInstanceOf(AppException.class);
        assertThat(guest.getBookingRoom()).isNull();
        verify(guests, never()).save(any());
    }

    @Test void explicitRoomMustMatchAssignedBookingRoom() {
        prepareCreate(bookingRoom((short)1));
        when(rooms.findById(11)).thenReturn(Optional.of(RoomInstance.builder().id(11).roomNumber("S101")
                .hotel(bookingRoom((short)1).getBookingDetail().getBooking().getHotel()).build()));
        StayGuestCreateRequest request = createRequest(); request.setRoomId(11);
        assertThatThrownBy(() -> service.createOrCheckIn(request)).isInstanceOf(AppException.class);
        verify(guests, never()).save(any());
    }

    @Test void ownBookingRoomAloneResolvesBookingAndPhysicalRoom() {
        prepareCreate(bookingRoom((short)1));
        var response = service.createOrCheckIn(createRequest());
        assertThat(response.getHotelId()).isEqualTo((short)1);
        assertThat(response.getBookingId()).isEqualTo(50L);
        assertThat(response.getRoomId()).isEqualTo(10);
    }

    @Test void ownWalkInUpdateResolvesBookingAndRoomTogether() {
        StayGuest guest = StayGuest.builder().id(21L).hotel(bookingRoom((short)1).getBookingDetail().getBooking().getHotel()).build();
        when(guests.findById(21L)).thenReturn(Optional.of(guest));
        when(bookings.getBookingRoomEntityById(90L)).thenReturn(bookingRoom((short)1));
        when(guests.save(any())).thenAnswer(call -> call.getArgument(0));
        var response = service.update(21L, StayGuestUpdateRequest.builder().bookingRoomId(90L).build());
        assertThat(response.getBookingId()).isEqualTo(50L);
        assertThat(response.getRoomId()).isEqualTo(10);
        assertThat(response.getRoomNumber()).isEqualTo("S101");
    }

    @Test void mismatchedRoomNumberCannotBeSaved() {
        prepareCreate(bookingRoom((short)1));
        var request = createRequest(); request.setRoomNumber("S999");
        assertThatThrownBy(() -> service.createOrCheckIn(request)).isInstanceOf(AppException.class);
        verify(guests, never()).save(any());
    }

    @Test void bookingRoomWithoutParentCannotBeSaved() {
        var room = bookingRoom((short)1); room.setBookingDetail(null); prepareCreate(room);
        assertThatThrownBy(() -> service.createOrCheckIn(createRequest())).isInstanceOf(AppException.class);
        verify(guests, never()).save(any());
    }

    StayGuest completeGuest() {
        return StayGuest.builder().id(21L).hotel(bookingRoom((short)1).getBookingDetail().getBooking().getHotel())
                .roomNumber("S101").fullName("SYNTHETIC").dateOfBirth(LocalDate.of(2000,1,1))
                .gender(Gender.OTHER).nationality("Vietnam").documentType(DocumentType.CCCD)
                .documentNumber("SYNTHETIC_ONLY").permanentAddress("SYNTHETIC")
                .checkInTime(OffsetDateTime.parse("2026-11-03T12:00:00+07:00"))
                .expectedCheckOutTime(OffsetDateTime.parse("2026-11-04T12:00:00+07:00")).build();
    }
    LodgingQueue prepareExported(StayGuest guest) {
        var queue = LodgingQueue.builder().id(31L).stayGuest(guest).hotel(guest.getHotel())
                .status(LodgingQueueStatus.EXPORTED).batchReference("SYNTHETIC_BATCH")
                .exportedAt(OffsetDateTime.parse("2026-11-03T13:00:00+07:00")).build();
        when(guests.findById(21L)).thenReturn(Optional.of(guest));
        when(guests.save(any())).thenAnswer(call -> call.getArgument(0));
        when(queues.findByStayGuestIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(queue));
        return queue;
    }
    @Test void exportedNoopMustNotRequeueOrSave() {
        var queue = prepareExported(completeGuest());
        service.update(21L, StayGuestUpdateRequest.builder().build());
        assertThat(queue.getStatus()).isEqualTo(LodgingQueueStatus.EXPORTED);
        assertThat(queue.getBatchReference()).isEqualTo("SYNTHETIC_BATCH");
        verify(guests, never()).save(any()); verify(queues, never()).save(any());
    }
    @Test void exportedLegacyGuestNoopMustNotBackfillRedundantAssociationKeys() {
        var guest = completeGuest(); guest.setBookingRoom(bookingRoom((short)1));
        prepareExported(guest);
        service.update(21L, StayGuestUpdateRequest.builder().build());
        assertThat(guest.getBooking()).isNull(); assertThat(guest.getRoom()).isNull();
        verify(guests, never()).save(any());
    }
    @Test void exportedContentChangeMustBeRejectedBeforeMutation() {
        var guest = completeGuest(); var queue = prepareExported(guest);
        assertThatThrownBy(() -> service.update(21L, StayGuestUpdateRequest.builder().fullName("SYNTHETIC_CHANGED").build()))
                .isInstanceOf(AppException.class);
        assertThat(guest.getFullName()).isEqualTo("SYNTHETIC");
        assertThat(queue.getStatus()).isEqualTo(LodgingQueueStatus.EXPORTED);
        verify(guests, never()).save(any());
    }
    @Test void nonExportedImageFieldCanChangeWithoutRequeue() {
        var guest = completeGuest(); var queue = prepareExported(guest);
        service.update(21L, StayGuestUpdateRequest.builder().documentImageUrl("/synthetic/image").build());
        assertThat(guest.getDocumentImageUrl()).isEqualTo("/synthetic/image");
        assertThat(queue.getStatus()).isEqualTo(LodgingQueueStatus.EXPORTED);
        verify(queues, never()).save(any());
    }

    static Stream<Arguments> exportFieldChanges() {
        return Stream.of(
            Arguments.of("name", (Consumer<StayGuestUpdateRequest>) r -> r.setFullName("CHANGED")),
            Arguments.of("dob", (Consumer<StayGuestUpdateRequest>) r -> r.setDateOfBirth(LocalDate.of(2001,1,1))),
            Arguments.of("gender", (Consumer<StayGuestUpdateRequest>) r -> r.setGender(Gender.FEMALE)),
            Arguments.of("nationality", (Consumer<StayGuestUpdateRequest>) r -> r.setNationality("SYNTHETIC_OTHER")),
            Arguments.of("documentType", (Consumer<StayGuestUpdateRequest>) r -> r.setDocumentType(DocumentType.PASSPORT)),
            Arguments.of("documentNumber", (Consumer<StayGuestUpdateRequest>) r -> r.setDocumentNumber("SYNTHETIC_CHANGED")),
            Arguments.of("permanentAddress", (Consumer<StayGuestUpdateRequest>) r -> r.setPermanentAddress("CHANGED")),
            Arguments.of("currentAddress", (Consumer<StayGuestUpdateRequest>) r -> r.setCurrentAddress("CHANGED")),
            Arguments.of("checkout", (Consumer<StayGuestUpdateRequest>) r -> r.setExpectedCheckOutTime(OffsetDateTime.parse("2026-11-05T12:00:00+07:00"))),
            Arguments.of("roomNumber", (Consumer<StayGuestUpdateRequest>) r -> r.setRoomNumber("S102")),
            Arguments.of("reason", (Consumer<StayGuestUpdateRequest>) r -> r.setReasonForStay("CHANGED")));
    }
    @ParameterizedTest @MethodSource("exportFieldChanges")
    void everyExportedPatchFieldIsProtected(String field, Consumer<StayGuestUpdateRequest> change) {
        var guest = completeGuest(); var queue = prepareExported(guest);
        var before = queue.getExportedAt(); var request = StayGuestUpdateRequest.builder().build(); change.accept(request);
        assertThatThrownBy(() -> service.update(21L, request)).as(field).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LODGING_EXPORTED_RECORD_LOCKED));
        assertThat(queue.getExportedAt()).isEqualTo(before);
        verify(guests, never()).save(any());
    }
}
