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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.LeadGuestRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CartPriceQuoteResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.session.BookingDraftSession;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingChargeResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.mapper.BookingMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.service.CompanyService;
import vn.edu.utc.hotel_booking.modules.identity.service.GuestService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DailyPriceDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;
import vn.edu.utc.hotel_booking.modules.pricing.service.VatRuleService;
import vn.edu.utc.hotel_booking.modules.pricing.service.VoucherService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingRoomService bookingRoomService;
    @Mock BookingChargeService bookingChargeService;

    @Mock HotelService hotelService;
    @Mock GuestService guestService;
    @Mock CompanyService companyService;
    @Mock HotelRoomTypeService hotelRoomTypeService;
    @Mock RoomInstanceService roomInstanceService;
    @Mock RoomSlotService roomSlotService;

    @Mock RoomAvailabilityService roomAvailabilityService;
    @Mock PriceEngine priceEngine;
    @Mock BookingMapper bookingMapper;
    @Mock TaxCategoryService taxCategoryService;
    @Mock VatRuleService vatRuleService;
    @Mock VoucherService voucherService;
    @Mock ServiceOrderService serviceOrderService;
    @Mock vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService stayGuestService;

    @Mock org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @Mock org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;
    @Mock com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    BookingServiceImpl bookingService;

    private Hotel testHotel;
    private BookingGuest testGuest;
    private HotelRoomType testHotelRoomType;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        testHotel.setId((short) 1);

        testGuest = BookingGuest.builder().phone("0987654321").build();
        testGuest.setId(10L);

        RoomType roomType = RoomType.builder().code("DLX").name("Phòng Deluxe").build();
        roomType.setId((short) 1);

        testHotelRoomType = HotelRoomType.builder()
                .hotel(testHotel)
                .roomType(roomType)
                .taxCategoryId(1)
                .basePrice(BigDecimal.valueOf(1000000))
                .standardAdults((short) 2)
                .maxAdults((short) 2)
                .maxChildren((short) 1)
                .maxInfants((short) 1)
                .maxTotalGuests((short) 3)
                .extraBeds((short) 0)
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

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(guestService.getEntityById(10L)).thenReturn(testGuest);
        when(hotelRoomTypeService.getEntityById(5)).thenReturn(testHotelRoomType);

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
    @DisplayName("Tạo đơn đặt phòng thành công cho khách hàng công ty")
    void createBooking_Company_Success() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        Company testCompany = Company.builder().name("Tập đoàn ABC").taxCode("0101234567").build();
        testCompany.setId(88L);

        BookingCreateRequest.RoomItemRequest roomItem = BookingCreateRequest.RoomItemRequest.builder()
                .hotelRoomTypeId(5)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .adultCount((short) 2)
                .build();

        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId((short) 1)
                .companyId(88L)
                .rooms(List.of(roomItem))
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(companyService.getEntityById(88L)).thenReturn(testCompany);
        when(hotelRoomTypeService.getEntityById(5)).thenReturn(testHotelRoomType);

        DailyPriceDto daily = DailyPriceDto.builder().date(checkIn).baseRate(BigDecimal.valueOf(1000000)).netAmount(BigDecimal.valueOf(1000000)).build();
        PriceBreakdownDto breakdown = PriceBreakdownDto.builder().dailyPrices(List.of(daily)).totalBasePrice(BigDecimal.valueOf(1000000)).preTaxAmount(BigDecimal.valueOf(1000000)).build();
        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(BookingResponse.builder().id(100L).build());

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        verify(companyService).getEntityById(88L);
        verify(bookingRepository).save(any(Booking.class));
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

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(guestService.getEntityById(10L)).thenReturn(testGuest);

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

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(50)).thenReturn(roomInstance);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(50), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(conflictingRoom));

        assertThatThrownBy(() -> bookingService.assignRoom(20L, 50))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_ALREADY_ASSIGNED));
    }

    @Test
    @DisplayName("Xếp phòng thành công: kiểm tra không trùng lịch, lưu booking room và tạo room slots")
    void assignRoom_Success() {
        BookingDetail detail = BookingDetail.builder()
                .booking(testBooking)
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

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(50)).thenReturn(roomInstance);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(50), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRoomService.save(any(BookingRoom.class))).thenReturn(bookingRoom);
        when(bookingMapper.toResponse(testBooking)).thenReturn(BookingResponse.builder()
                .id(100L)
                .bookingNumber("BK12345678")
                .build());

        BookingResponse response = bookingService.assignRoom(20L, 50);

        assertThat(response).isNotNull();
        verify(bookingRoomService).save(bookingRoom);
        verify(roomSlotService).createOrReserveSlots(eq(roomInstance), eq(detail.getCheckInDate()), eq(detail.getCheckOutDate()), eq(20L));
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
                .subtotal(BigDecimal.valueOf(200000))
                .serviceFeeAmount(BigDecimal.valueOf(10000))
                .vatAmount(BigDecimal.valueOf(16800))
                .totalAmount(BigDecimal.valueOf(226800))
                .build();

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);
        when(bookingChargeService.createCharge(eq(bookingRoom), any(BookingChargeCreateRequest.class))).thenReturn(savedCharge);
        when(bookingMapper.toChargeResponse(any(BookingCharge.class))).thenReturn(BookingChargeResponse.builder()
                .id(1L)
                .totalAmount(BigDecimal.valueOf(226800))
                .build());

        BookingChargeResponse response = bookingService.addCharge(20L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(bookingRepository, times(1)).save(testBooking);
    }

    @Test
    @DisplayName("Tạo đơn đặt nhiều loại phòng cùng lúc thành công, sắp xếp theo ID để chống deadlock")
    void createBooking_MultiRoomTypes_Success() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(4);

        // Hạng phòng thứ hai (ID = 8)
        RoomType rt2 = RoomType.builder().code("STE").name("Phòng Suite").build();
        rt2.setId((short) 2);

        HotelRoomType hotelRoomType2 = HotelRoomType.builder()
                .hotel(testHotel)
                .roomType(rt2)
                .taxCategoryId(1)
                .basePrice(BigDecimal.valueOf(2000000))
                .standardAdults((short) 2)
                .maxAdults((short) 4)
                .maxChildren((short) 2)
                .maxInfants((short) 2)
                .maxTotalGuests((short) 6)
                .extraBeds((short) 2)
                .build();
        hotelRoomType2.setId(8);

        // Danh sách phòng gửi lên không theo thứ tự (ID 8 trước, ID 5 sau)
        BookingCreateRequest.RoomItemRequest roomItem1 = BookingCreateRequest.RoomItemRequest.builder()
                .hotelRoomTypeId(8)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .adultCount((short) 2)
                .extraBeds((short) 1)
                .build();

        BookingCreateRequest.RoomItemRequest roomItem2 = BookingCreateRequest.RoomItemRequest.builder()
                .hotelRoomTypeId(5)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .adultCount((short) 2)
                .build();

        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId((short) 1)
                .guestId(10L)
                .voucherCode("MULTIROOM10")
                .serviceFeeRate(BigDecimal.valueOf(5))
                .rooms(List.of(roomItem1, roomItem2))
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(guestService.getEntityById(10L)).thenReturn(testGuest);
        when(hotelRoomTypeService.getEntityById(5)).thenReturn(testHotelRoomType);
        when(hotelRoomTypeService.getEntityById(8)).thenReturn(hotelRoomType2);

        DailyPriceDto daily1 = DailyPriceDto.builder()
                .date(checkIn)
                .baseRate(BigDecimal.valueOf(1000000))
                .netAmount(BigDecimal.valueOf(1000000))
                .build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .dailyPrices(List.of(daily1))
                .totalBasePrice(BigDecimal.valueOf(1000000))
                .preTaxAmount(BigDecimal.valueOf(1000000))
                .build();

        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(BookingResponse.builder()
                .id(100L)
                .bookingNumber("BK12345678")
                .build());

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        // Kiểm tra giữ tồn kho được gọi cho cả 2 hạng phòng
        verify(roomAvailabilityService, times(1)).confirmBookingInventory(eq(5), eq(checkIn), eq(checkOut), eq(1));
        verify(roomAvailabilityService, times(1)).confirmBookingInventory(eq(8), eq(checkIn), eq(checkOut), eq(1));
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi số người lớn vượt quá maxAdults của hạng phòng")
    void createBooking_ExceedsCapacity_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(4);

        BookingCreateRequest.RoomItemRequest roomItem = BookingCreateRequest.RoomItemRequest.builder()
                .hotelRoomTypeId(5)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .adultCount((short) 4) // maxAdults = 2
                .build();

        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId((short) 1)
                .guestId(10L)
                .rooms(List.of(roomItem))
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(guestService.getEntityById(10L)).thenReturn(testGuest);
        when(hotelRoomTypeService.getEntityById(5)).thenReturn(testHotelRoomType);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.EXCEEDS_MAX_CAPACITY));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi số giường phụ vượt quá extraBeds của hạng phòng")
    void createBooking_ExceedsExtraBeds_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(4);

        BookingCreateRequest.RoomItemRequest roomItem = BookingCreateRequest.RoomItemRequest.builder()
                .hotelRoomTypeId(5)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .adultCount((short) 2)
                .extraBeds((short) 2) // extraBeds = 0
                .build();

        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId((short) 1)
                .guestId(10L)
                .rooms(List.of(roomItem))
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(guestService.getEntityById(10L)).thenReturn(testGuest);
        when(hotelRoomTypeService.getEntityById(5)).thenReturn(testHotelRoomType);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.EXCEEDS_MAX_CAPACITY));
    }

    @Test
    @DisplayName("confirmOnlineBooking thành công: snapshot đầy đủ các bảng dữ liệu, commit inventory holds và xóa Redis key")
    void confirmOnlineBooking_Success() throws Exception {
        String sessionId = "sess-abc-123";
        String redisKey = "booking:draft:" + sessionId;
        String mockJson = "{\"sessionId\":\"sess-abc-123\"}";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockJson);

        BookingDraftSession draftSession = BookingDraftSession.builder()
                .sessionId(sessionId)
                .hotelId((short) 1)
                .checkInDate(LocalDate.of(2026, 12, 1))
                .checkOutDate(LocalDate.of(2026, 12, 3))
                .leadGuest(LeadGuestRequest.builder()
                        .fullName("Trần Thị B")
                        .phone("0912345678")
                        .email("guest@example.com")
                        .nationality("VN")
                        .build())
                .items(List.of(CartPriceQuoteRequest.CartItemQuoteRequest.builder()
                        .hotelRoomTypeId(5)
                        .quantity((short) 1)
                        .adults((short) 2)
                        .build()))
                .quoteResponse(CartPriceQuoteResponse.builder()
                        .hotelId((short) 1)
                        .checkInDate(LocalDate.of(2026, 12, 1))
                        .checkOutDate(LocalDate.of(2026, 12, 3))
                        .totalNights(2)
                        .totalRooms(1)
                        .totalRoomNetAmount(new BigDecimal("2000000.00"))
                        .totalRoomServiceFee(new BigDecimal("100000.00"))
                        .totalRoomVat(new BigDecimal("210000.00"))
                        .grandTotal(new BigDecimal("2310000.00"))
                        .build())
                .build();

        when(objectMapper.readValue(mockJson, BookingDraftSession.class)).thenReturn(draftSession);
        when(guestService.getOrCreateGuest(eq("0912345678"), eq("Trần Thị B"), eq("guest@example.com"), eq("VN"))).thenReturn(testGuest);
        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(hotelRoomTypeService.getEntityById(5)).thenReturn(testHotelRoomType);

        Booking savedBooking = Booking.builder()
                .id(999L)
                .hotel(testHotel)
                .guest(testGuest)
                .bookingNumber("BK99999999")
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("2310000.00"))
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(BookingResponse.builder()
                .id(999L)
                .bookingNumber("BK99999999")
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("2310000.00"))
                .build());

        BookingResponse response = bookingService.confirmOnlineBooking(sessionId, 555L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(999L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Verify committed holds
        verify(roomAvailabilityService, times(1)).commitHoldInventory(sessionId);
        // Verify Redis draft key deleted
        verify(redisTemplate, times(1)).delete(redisKey);
    }

    @Test
    @DisplayName("confirmOnlineBooking ném BOOKING_SESSION_EXPIRED khi phiên trong Redis không tồn tại")
    void confirmOnlineBooking_ExpiredSession_ThrowsException() {
        String sessionId = "non-existent-session";
        String redisKey = "booking:draft:" + sessionId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        assertThatThrownBy(() -> bookingService.confirmOnlineBooking(sessionId, 123L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.BOOKING_SESSION_EXPIRED));
    }


    @Test
    @DisplayName("checkOut thành công: cập nhật trạng thái CHECKED_OUT và đánh dấu room slots DIRTY")
    void checkOut_Success() {
        BookingDetail detail = BookingDetail.builder().booking(testBooking).checkInDate(LocalDate.now()).checkOutDate(LocalDate.now().plusDays(1)).build();
        RoomInstance roomInstance = RoomInstance.builder().roomNumber("P.101").build();
        roomInstance.setId(101);
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).bookingDetail(detail).roomInstance(roomInstance).build();

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);
        when(bookingMapper.toResponse(testBooking)).thenReturn(BookingResponse.builder().id(100L).build());

        BookingResponse response = bookingService.checkOut(20L);

        assertThat(response).isNotNull();
        assertThat(bookingRoom.getStatus()).isEqualTo(BookingRoomStatus.CHECKED_OUT);
        verify(bookingRoomService).save(bookingRoom);
        verify(roomSlotService).markSlotsDirty(eq(101), eq(detail.getCheckInDate()), eq(detail.getCheckOutDate()));
    }

    @Test
    @DisplayName("updateStatus huỷ phòng: giải phóng room slots và huỷ các booking rooms")
    void updateStatus_Cancelled_ReleasesSlots() {
        BookingDetail detail = BookingDetail.builder().checkInDate(LocalDate.now()).checkOutDate(LocalDate.now().plusDays(2)).build();
        RoomInstance roomInstance = RoomInstance.builder().roomNumber("P.101").build();
        roomInstance.setId(101);
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).roomInstance(roomInstance).build();
        detail.setBookingRooms(List.of(bookingRoom));
        testBooking.setBookingDetails(List.of(detail));

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(testBooking)).thenReturn(BookingResponse.builder().id(100L).status(BookingStatus.CANCELLED).build());

        BookingResponse response = bookingService.updateStatus(100L, BookingStatus.CANCELLED);

        assertThat(response).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingRoom.getStatus()).isEqualTo(BookingRoomStatus.CANCELLED);
        verify(roomSlotService).releaseSlots(eq(101), eq(detail.getCheckInDate()), eq(detail.getCheckOutDate()));
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("getBookingRoomEntityById - Thành công khi ID hợp lệ")
    void getBookingRoomEntityById_Success() {
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).build();
        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);

        BookingRoom result = bookingService.getBookingRoomEntityById(20L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
        verify(bookingRoomService).getEntityById(20L);
    }

    @Test
    @DisplayName("getBookingRoomEntityById - Ném ngoại lệ khi ID là null")
    void getBookingRoomEntityById_NullId() {
        assertThatThrownBy(() -> bookingService.getBookingRoomEntityById(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("getBookingRoomEntityById - Ném ngoại lệ khi không tìm thấy")
    void getBookingRoomEntityById_NotFound() {
        when(bookingRoomService.getEntityById(999L))
                .thenThrow(new AppException(ErrorCode.BOOKING_ROOM_NOT_FOUND, "Không tìm thấy phòng"));

        assertThatThrownBy(() -> bookingService.getBookingRoomEntityById(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("processNoShowBookings - Trả về 0 khi hotelId hoặc auditDate là null")
    void processNoShowBookings_NullParams_ReturnsZero() {
        assertThat(bookingService.processNoShowBookings(null, LocalDate.now())).isEqualTo(0);
        assertThat(bookingService.processNoShowBookings((short) 1, null)).isEqualTo(0);
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("processNoShowBookings - Xử lý NO_SHOW thành công, cập nhật trạng thái và giải phóng slots")
    void processNoShowBookings_Success() {
        LocalDate auditDate = LocalDate.of(2026, 10, 17);
        LocalDate checkIn = LocalDate.of(2026, 10, 17);
        LocalDate checkOut = LocalDate.of(2026, 10, 19);

        RoomInstance roomInstance = RoomInstance.builder().roomNumber("P.301").build();
        roomInstance.setId(301);

        BookingRoom bookingRoom = BookingRoom.builder()
                .id(50L)
                .status(BookingRoomStatus.EXPECTED)
                .roomInstance(roomInstance)
                .build();

        BookingDetail detail = BookingDetail.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .bookingRooms(List.of(bookingRoom))
                .build();

        Booking confirmedBooking = Booking.builder()
                .id(200L)
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(List.of(detail))
                .build();

        when(bookingRepository.findByHotelIdAndStatusWithDetails((short) 1, BookingStatus.CONFIRMED))
                .thenReturn(List.of(confirmedBooking));

        int result = bookingService.processNoShowBookings((short) 1, auditDate);

        assertThat(result).isEqualTo(1);
        assertThat(confirmedBooking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(bookingRoom.getStatus()).isEqualTo(BookingRoomStatus.NO_SHOW);
        verify(roomSlotService).releaseSlots(301, checkIn, checkOut);
        verify(bookingRoomService).save(bookingRoom);
        verify(bookingRepository).save(confirmedBooking);
    }

    @Test
    @DisplayName("processNoShowBookings - Bỏ qua đơn nếu có ít nhất 1 phòng đã CHECKED_IN")
    void processNoShowBookings_AnyCheckedIn_DoesNotMarkNoShow() {
        LocalDate auditDate = LocalDate.of(2026, 10, 17);
        LocalDate checkIn = LocalDate.of(2026, 10, 17);
        LocalDate checkOut = LocalDate.of(2026, 10, 19);

        BookingRoom room1 = BookingRoom.builder().id(51L).status(BookingRoomStatus.CHECKED_IN).build();
        BookingRoom room2 = BookingRoom.builder().id(52L).status(BookingRoomStatus.EXPECTED).build();

        BookingDetail detail = BookingDetail.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .bookingRooms(List.of(room1, room2))
                .build();

        Booking partiallyCheckedInBooking = Booking.builder()
                .id(201L)
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(List.of(detail))
                .build();

        when(bookingRepository.findByHotelIdAndStatusWithDetails((short) 1, BookingStatus.CONFIRMED))
                .thenReturn(List.of(partiallyCheckedInBooking));

        int result = bookingService.processNoShowBookings((short) 1, auditDate);

        assertThat(result).isEqualTo(0);
        assertThat(partiallyCheckedInBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(roomSlotService);
    }

    @Test
    @DisplayName("checkIn ném ngoại lệ ROOM_NOT_YET_ASSIGNED khi phòng chưa được gán phòng vật lý")
    void checkIn_WithoutRoomInstance_ThrowsException() {
        BookingRoom unassignedRoom = BookingRoom.builder()
                .id(99L)
                .roomInstance(null)
                .status(BookingRoomStatus.EXPECTED)
                .build();

        when(bookingRoomService.getEntityById(99L)).thenReturn(unassignedRoom);

        assertThatThrownBy(() -> bookingService.checkIn(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_YET_ASSIGNED));
    }

    @Test
    @DisplayName("checkIn ném ngoại lệ ROOM_NOT_READY_FOR_CHECKIN khi phòng vật lý đang ở trạng thái DIRTY")
    void checkIn_WithDirtyRoom_ThrowsException() {
        RoomInstance dirtyRoom = RoomInstance.builder()
                .id(10)
                .roomNumber("P.101")
                .currentStatus("DIRTY")
                .build();

        BookingRoom assignedRoom = BookingRoom.builder()
                .id(99L)
                .roomInstance(dirtyRoom)
                .status(BookingRoomStatus.EXPECTED)
                .build();

        when(bookingRoomService.getEntityById(99L)).thenReturn(assignedRoom);

        assertThatThrownBy(() -> bookingService.checkIn(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_READY_FOR_CHECKIN));
    }

    @Test
    @DisplayName("checkIn thành công: đổi trạng thái CHECKED_IN, chiếm giữ room slots và kích hoạt sync hàng chờ BCA")
    void checkIn_Success() {
        RoomInstance readyRoom = RoomInstance.builder()
                .id(10)
                .roomNumber("P.101")
                .currentStatus("READY")
                .build();

        BookingDetail detail = BookingDetail.builder()
                .booking(testBooking)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .build();

        BookingRoom bookingRoom = BookingRoom.builder()
                .id(99L)
                .bookingDetail(detail)
                .roomInstance(readyRoom)
                .status(BookingRoomStatus.EXPECTED)
                .build();

        when(bookingRoomService.getEntityById(99L)).thenReturn(bookingRoom);
        when(bookingRoomService.save(any(BookingRoom.class))).thenReturn(bookingRoom);
        when(bookingMapper.toResponse(testBooking)).thenReturn(BookingResponse.builder()
                .id(100L)
                .bookingNumber("BK12345678")
                .build());

        BookingResponse response = bookingService.checkIn(99L);

        assertThat(response).isNotNull();
        assertThat(bookingRoom.getStatus()).isEqualTo(BookingRoomStatus.CHECKED_IN);
        assertThat(bookingRoom.getActualCheckInAt()).isNotNull();

        verify(bookingRoomService).save(bookingRoom);
        verify(roomSlotService).occupySlots(eq(10), eq(detail.getCheckInDate()), eq(detail.getCheckOutDate()));
        verify(stayGuestService).syncLodgingQueueForBookingRoom(99L);
    }

    @Test
    @DisplayName("assignRoom tự động cascade roomInstance và roomNumber sang các StayGuest của phòng")
    void assignRoom_CascadesRoomInstanceToExistingStayGuests() {
        StayGuest stayGuest = StayGuest.builder()
                .id(1L)
                .fullName("Khách Đi Kèm")
                .room(null)
                .roomNumber(null)
                .build();

        List<StayGuest> guests = new ArrayList<>(List.of(stayGuest));

        BookingDetail detail = BookingDetail.builder()
                .booking(testBooking)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .build();

        BookingRoom bookingRoom = BookingRoom.builder()
                .id(20L)
                .bookingDetail(detail)
                .stayGuests(guests)
                .build();

        RoomInstance roomInstance = RoomInstance.builder()
                .roomNumber("P.201")
                .build();
        roomInstance.setId(50);

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(50)).thenReturn(roomInstance);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(50), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRoomService.save(any(BookingRoom.class))).thenReturn(bookingRoom);
        when(bookingMapper.toResponse(testBooking)).thenReturn(BookingResponse.builder().id(100L).build());

        bookingService.assignRoom(20L, 50);

        assertThat(stayGuest.getRoom()).isEqualTo(roomInstance);
        assertThat(stayGuest.getRoomNumber()).isEqualTo("P.201");
        verify(bookingRoomService).save(bookingRoom);
    }

    @Test
    @DisplayName("previewSurcharge: Đề xuất đúng 50% khi nhận phòng sớm trong khung 06:00 - 09:00")
    void previewSurcharge_EarlyCheckIn_SuggestsFiftyPercent() {
        LocalDate checkInDate = LocalDate.now();
        LocalDate checkOutDate = checkInDate.plusDays(2);
        BookingDetail detail = BookingDetail.builder()
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .build();

        BookingDailyRate rate1 = BookingDailyRate.builder().stayDate(checkInDate).netPrice(new BigDecimal("1000000.00")).build();
        BookingDailyRate rate2 = BookingDailyRate.builder().stayDate(checkInDate.plusDays(1)).netPrice(new BigDecimal("1000000.00")).build();

        BookingRoom bookingRoom = BookingRoom.builder()
                .id(20L)
                .bookingDetail(detail)
                .dailyRates(List.of(rate1, rate2))
                .build();

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);

        vn.edu.utc.hotel_booking.modules.booking.dto.request.SurchargePreviewRequest req =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.SurchargePreviewRequest.builder()
                        .chargeType(BookingChargeType.EARLY_CHECKIN)
                        .actualTime(checkInDate.atTime(8, 0).atOffset(java.time.ZoneOffset.ofHours(7)))
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.response.SurchargeProposalResponse proposal =
                bookingService.previewSurcharge(20L, req);

        assertThat(proposal.isApplicable()).isTrue();
        assertThat(proposal.getChargeType()).isEqualTo(BookingChargeType.EARLY_CHECKIN);
        assertThat(proposal.getSuggestedPercentage()).isEqualByComparingTo(new BigDecimal("50.0"));
        assertThat(proposal.getSuggestedAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(proposal.getPolicyRule()).contains("06:00 đến 09:00");
    }

    @Test
    @DisplayName("previewSurcharge: Đề xuất đúng 30% khi trả phòng muộn trong khung 12:00 - 15:00")
    void previewSurcharge_LateCheckOut_SuggestsThirtyPercent() {
        LocalDate checkInDate = LocalDate.now().minusDays(2);
        LocalDate checkOutDate = LocalDate.now();
        BookingDetail detail = BookingDetail.builder()
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .build();

        BookingDailyRate rate = BookingDailyRate.builder().stayDate(checkInDate).netPrice(new BigDecimal("2000000.00")).build();

        BookingRoom bookingRoom = BookingRoom.builder()
                .id(20L)
                .bookingDetail(detail)
                .dailyRates(List.of(rate))
                .build();

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);

        vn.edu.utc.hotel_booking.modules.booking.dto.request.SurchargePreviewRequest req =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.SurchargePreviewRequest.builder()
                        .chargeType(BookingChargeType.LATE_CHECKOUT)
                        .actualTime(checkOutDate.atTime(13, 30).atOffset(java.time.ZoneOffset.ofHours(7)))
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.response.SurchargeProposalResponse proposal =
                bookingService.previewSurcharge(20L, req);

        assertThat(proposal.isApplicable()).isTrue();
        assertThat(proposal.getChargeType()).isEqualTo(BookingChargeType.LATE_CHECKOUT);
        assertThat(proposal.getSuggestedPercentage()).isEqualByComparingTo(new BigDecimal("30.0"));
        assertThat(proposal.getSuggestedAmount()).isEqualByComparingTo(new BigDecimal("600000.00"));
    }

    @Test
    @DisplayName("deleteCharge thành công: trừ tiền phụ phí khỏi tổng tiền booking và gọi xóa bản ghi")
    void deleteCharge_Success_RecalculatesBookingTotal() {
        Booking booking = Booking.builder()
                .id(100L)
                .subtotalAmount(new BigDecimal("1500000.00"))
                .serviceFeeAmount(new BigDecimal("75000.00"))
                .totalVatAmount(new BigDecimal("126000.00"))
                .totalAmount(new BigDecimal("1701000.00"))
                .build();

        BookingDetail detail = BookingDetail.builder().booking(booking).build();
        BookingRoom room = BookingRoom.builder().id(20L).bookingDetail(detail).build();

        BookingCharge charge = BookingCharge.builder()
                .id(55L)
                .bookingRoom(room)
                .chargeType(BookingChargeType.EARLY_CHECKIN)
                .subtotal(new BigDecimal("500000.00"))
                .serviceFeeAmount(new BigDecimal("25000.00"))
                .vatAmount(new BigDecimal("42000.00"))
                .totalAmount(new BigDecimal("567000.00"))
                .build();

        when(bookingChargeService.getById(55L)).thenReturn(charge);

        bookingService.deleteCharge(55L);

        assertThat(booking.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(booking.getServiceFeeAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(booking.getTotalVatAmount()).isEqualByComparingTo(new BigDecimal("84000.00"));
        assertThat(booking.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1134000.00"));

        verify(bookingRepository).save(booking);
        verify(bookingChargeService).deleteById(55L);
    }
}
