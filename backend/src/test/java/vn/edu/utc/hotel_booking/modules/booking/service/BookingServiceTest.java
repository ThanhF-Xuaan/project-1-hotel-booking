package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingChargeResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.mapper.BookingMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.*;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.entity.Guest;
import vn.edu.utc.hotel_booking.modules.identity.repository.CompanyRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.GuestRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DailyPriceDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingDetailRepository bookingDetailRepository;
    @Mock BookingRoomRepository bookingRoomRepository;
    @Mock BookingGuestRepository bookingGuestRepository;
    @Mock BookingDailyRateRepository bookingDailyRateRepository;
    @Mock BookingChargeRepository bookingChargeRepository;

    @Mock HotelRepository hotelRepository;
    @Mock GuestRepository guestRepository;
    @Mock CompanyRepository companyRepository;
    @Mock HotelRoomTypeRepository hotelRoomTypeRepository;
    @Mock RoomInstanceRepository roomInstanceRepository;
    @Mock RoomSlotRepository roomSlotRepository;

    @Mock RoomAvailabilityService roomAvailabilityService;
    @Mock PriceEngine priceEngine;
    @Mock BookingMapper bookingMapper;
    @Mock TaxCategoryRepository taxCategoryRepository;
    @Mock VatRuleRepository vatRuleRepository;

    @InjectMocks
    BookingServiceImpl bookingService;

    private Hotel testHotel;
    private Guest testGuest;
    private HotelRoomType testHotelRoomType;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        testHotel.setId((short) 1);

        testGuest = Guest.builder().phone("0987654321").build();
        testGuest.setId(10L);

        RoomType roomType = RoomType.builder().code("DLX").name("Phòng Deluxe").build();
        roomType.setId((short) 1);

        testHotelRoomType = HotelRoomType.builder()
                .hotel(testHotel)
                .roomType(roomType)
                .taxCategoryId(1)
                .basePrice(BigDecimal.valueOf(1000000))
                .build();
        testHotelRoomType.setId(5);

        testBooking = Booking.builder()
                .id(100L)
                .hotel(testHotel)
                .guest(testGuest)
                .bookingNumber("BK12345678")
                .status(BookingStatus.CONFIRMED)
                .subtotalAmount(BigDecimal.valueOf(2000000))
                .serviceFeeRate(BigDecimal.valueOf(5))
                .serviceFeeAmount(BigDecimal.valueOf(100000))
                .totalVatAmount(BigDecimal.valueOf(168000))
                .totalAmount(BigDecimal.valueOf(2268000))
                .bookingDetails(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Tạo đơn đặt phòng thành công với đầy đủ tính giá và tồn kho")
    void createBooking_Success() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(4);

        BookingCreateRequest.RoomItemRequest roomItem = BookingCreateRequest.RoomItemRequest.builder()
                .hotelRoomTypeId(5)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .adultCount((short) 2)
                .build();

        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId((short) 1)
                .guestId(10L)
                .serviceFeeRate(BigDecimal.valueOf(5))
                .rooms(List.of(roomItem))
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(testHotel));
        when(guestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(testGuest));
        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(5)).thenReturn(Optional.of(testHotelRoomType));

        DailyPriceDto daily1 = DailyPriceDto.builder()
                .date(checkIn)
                .baseRate(BigDecimal.valueOf(1000000))
                .netAmount(BigDecimal.valueOf(1000000))
                .build();
        DailyPriceDto daily2 = DailyPriceDto.builder()
                .date(checkIn.plusDays(1))
                .baseRate(BigDecimal.valueOf(1000000))
                .netAmount(BigDecimal.valueOf(1000000))
                .build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .dailyPrices(List.of(daily1, daily2))
                .totalBasePrice(BigDecimal.valueOf(2000000))
                .preTaxAmount(BigDecimal.valueOf(2000000))
                .build();

        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(BookingResponse.builder()
                .id(100L)
                .bookingNumber("BK12345678")
                .totalAmount(BigDecimal.valueOf(2268000))
                .build());

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        verify(roomAvailabilityService, times(1)).confirmBookingInventory(eq(5), eq(checkIn), eq(checkOut), eq(1));
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Ném lỗi khi ngày nhận phòng không trước ngày trả phòng")
    void createBooking_InvalidDates_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(2); // checkOut < checkIn

        BookingCreateRequest.RoomItemRequest roomItem = BookingCreateRequest.RoomItemRequest.builder()
                .hotelRoomTypeId(5)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .build();

        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId((short) 1)
                .guestId(10L)
                .rooms(List.of(roomItem))
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(testHotel));
        when(guestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(testGuest));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_BOOKING_DATES));
    }

    @Test
    @DisplayName("Xếp phòng thất bại khi phòng vật lý đã có người đặt trong thời gian đó")
    void assignRoom_RoomConflict_ThrowsException() {
        BookingDetail detail = BookingDetail.builder()
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .build();
        BookingRoom bookingRoom = BookingRoom.builder()
                .id(20L)
                .bookingDetail(detail)
                .build();

        RoomInstance roomInstance = RoomInstance.builder()
                .roomNumber("P.201")
                .build();
        roomInstance.setId(50);

        BookingRoom conflictingRoom = BookingRoom.builder()
                .id(21L)
                .build();

        when(bookingRoomRepository.findById(20L)).thenReturn(Optional.of(bookingRoom));
        when(roomInstanceRepository.findByIdAndIsDeletedFalse(50)).thenReturn(Optional.of(roomInstance));
        when(bookingRoomRepository.findOverlappingAssignedRooms(eq(50), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(conflictingRoom));

        assertThatThrownBy(() -> bookingService.assignRoom(20L, 50))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_ALREADY_ASSIGNED));
    }

    @Test
    @DisplayName("Thêm phụ phí thành công và cộng dồn vào tổng tiền đơn đặt phòng")
    void addCharge_Success() {
        BookingDetail detail = BookingDetail.builder().booking(testBooking).build();
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).bookingDetail(detail).build();

        BookingChargeCreateRequest request = BookingChargeCreateRequest.builder()
                .chargeType(BookingChargeType.EARLY_CHECKIN)
                .itemName("Phụ thu nhận phòng sớm 08:00")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(200000))
                .serviceFeeRate(BigDecimal.valueOf(5))
                .vatRate(BigDecimal.valueOf(8))
                .build();

        BookingCharge savedCharge = BookingCharge.builder()
                .id(1L)
                .bookingRoom(bookingRoom)
                .totalAmount(BigDecimal.valueOf(226800))
                .build();

        when(bookingRoomRepository.findById(20L)).thenReturn(Optional.of(bookingRoom));
        when(bookingChargeRepository.save(any(BookingCharge.class))).thenReturn(savedCharge);
        when(bookingMapper.toChargeResponse(any(BookingCharge.class))).thenReturn(BookingChargeResponse.builder()
                .id(1L)
                .totalAmount(BigDecimal.valueOf(226800))
                .build());

        BookingChargeResponse response = bookingService.addCharge(20L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(bookingRepository, times(1)).save(testBooking);
    }
}
