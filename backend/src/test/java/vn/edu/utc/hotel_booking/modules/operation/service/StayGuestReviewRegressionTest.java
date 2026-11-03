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
}
