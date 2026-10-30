package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingChargeResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomInstanceResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request);

    PageResponse<BookingResponse> filter(BookingSearchDto searchDto);

    BookingResponse getById(Long id);

    vn.edu.utc.hotel_booking.modules.booking.entity.Booking getEntityById(Long id);

    BookingResponse getByBookingNumber(String bookingNumber);

    BookingResponse updateStatus(Long id, BookingStatus status);

    BookingResponse assignRoom(Long bookingRoomId, Integer roomInstanceId);

    BookingChargeResponse addCharge(Long bookingRoomId, BookingChargeCreateRequest request);

    BookingResponse checkIn(Long bookingRoomId);

    BookingResponse checkOut(Long bookingRoomId);

    boolean isRoomBookedBetween(Integer roomInstanceId, LocalDate startDate, LocalDate endDate);

    vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListPreviewResponse previewRoomingList(Long bookingId, org.springframework.web.multipart.MultipartFile file);

    vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListImportResultResponse importRoomingList(Long bookingId, org.springframework.web.multipart.MultipartFile file, boolean confirm);

    vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListImportResultResponse confirmRoomingList(Long bookingId, vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomingListConfirmRequest request);

    List<RoomInstanceResponse> getVacantRooms(Short hotelId);
}
