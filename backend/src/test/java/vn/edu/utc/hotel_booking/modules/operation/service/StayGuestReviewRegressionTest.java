package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.StayGuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.Optional;
import java.util.List;

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
}
