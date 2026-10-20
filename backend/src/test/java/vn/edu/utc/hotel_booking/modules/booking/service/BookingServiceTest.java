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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCancelRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelPreviewResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelResponse;
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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.WalkInQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.WalkInQuoteResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.WalkInBookingRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingNoShowRequest;
import vn.edu.utc.hotel_booking.modules.finance.entity.Payment;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentPurpose;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CheckoutSettlementResponse;

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
    @Mock vn.edu.utc.hotel_booking.modules.finance.service.InvoiceService invoiceService;
    @Mock vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository paymentRepository;
    @Mock vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper financeMapper;
    @Mock vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository surchargeRuleRepository;
    @Mock vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService auditLogService;

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
    @DisplayName("checkOut thành công: cập nhật trạng thái CHECKED_OUT, phòng sang DIRTY, đóng hóa đơn & dịch vụ sang PAID")
    void checkOut_Success() {
        BookingDetail detail = BookingDetail.builder().booking(testBooking).checkInDate(LocalDate.now()).checkOutDate(LocalDate.now().plusDays(1)).build();
        RoomInstance roomInstance = RoomInstance.builder().roomNumber("P.101").build();
        roomInstance.setId(101);
        BookingRoom bookingRoom = BookingRoom.builder()
                .id(20L)
                .status(BookingRoomStatus.CHECKED_IN)
                .bookingDetail(detail)
                .roomInstance(roomInstance)
                .build();
        detail.setBookingRooms(List.of(bookingRoom));
        testBooking.setBookingDetails(List.of(detail));

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);
        when(bookingMapper.toResponse(testBooking)).thenReturn(BookingResponse.builder().id(100L).build());

        BookingResponse response = bookingService.checkOut(20L);

        assertThat(response).isNotNull();
        assertThat(bookingRoom.getStatus()).isEqualTo(BookingRoomStatus.CHECKED_OUT);
        verify(bookingRoomService).save(bookingRoom);
        verify(roomInstanceService).updateRoomStatusEntity(eq(101), eq("DIRTY"));
        verify(roomSlotService).markSlotsDirty(eq(101), eq(detail.getCheckInDate()), eq(detail.getCheckOutDate()));
        verify(serviceOrderService).markOrdersPaidForBooking(eq(testBooking.getId()));
        verify(invoiceService).settleAndCloseInvoice(eq(testBooking.getId()));
    }

    @Test
    @DisplayName("checkOut thất bại: Ném BOOKING_ROOM_NOT_CHECKED_IN khi phòng chưa check-in")
    void checkOut_NotCheckedIn_ThrowsException() {
        BookingDetail detail = BookingDetail.builder().booking(testBooking).build();
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).status(BookingRoomStatus.EXPECTED).bookingDetail(detail).build();
        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);

        assertThatThrownBy(() -> bookingService.checkOut(20L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.BOOKING_ROOM_NOT_CHECKED_IN));
    }

    @Test
    @DisplayName("checkOut thất bại: Zero-Balance Gatekeeper chặn khi còn nợ tiền chưa thanh toán")
    void checkOut_UnpaidBalance_ThrowsException() {
        BookingDetail detail = BookingDetail.builder().booking(testBooking).checkInDate(LocalDate.now()).checkOutDate(LocalDate.now().plusDays(1)).build();
        BookingDailyRate dailyRate = BookingDailyRate.builder()
                .id(1L)
                .netPrice(new BigDecimal("500000.00"))
                .vatPercent(new BigDecimal("10.00"))
                .build();
        BookingRoom bookingRoom = BookingRoom.builder()
                .id(20L)
                .status(BookingRoomStatus.CHECKED_IN)
                .bookingDetail(detail)
                .dailyRates(List.of(dailyRate))
                .build();
        detail.setBookingRooms(List.of(bookingRoom));
        testBooking.setBookingDetails(List.of(detail));

        when(bookingRoomService.getEntityById(20L)).thenReturn(bookingRoom);
        // paymentRepository returns empty list -> balanceDue > 0

        assertThatThrownBy(() -> bookingService.checkOut(20L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.CHECKOUT_BALANCE_UNPAID));
    }


    @Test
    @DisplayName("updateStatus huỷ phòng: giải phóng room slots và huỷ các booking rooms")
    void updateStatus_Cancelled_ReleasesSlots() {
        BookingDetail detail = BookingDetail.builder().checkInDate(LocalDate.now()).checkOutDate(LocalDate.now().plusDays(2)).build();
        RoomInstance roomInstance = RoomInstance.builder().roomNumber("P.101").build();
        roomInstance.setId(101);
        BookingRoom bookingRoom = BookingRoom.builder().id(20L).roomInstance(roomInstance).build();
        bookingRoom.setBookingDetail(detail);
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

    @Test
    @DisplayName("Check-out thất bại khi phòng không ở trạng thái CHECKED_IN")
    void checkOut_WhenRoomNotCheckedIn_ShouldThrowException() {
        BookingRoom room = BookingRoom.builder()
                .id(101L)
                .status(BookingRoomStatus.EXPECTED)
                .build();

        when(bookingRoomService.getEntityById(101L)).thenReturn(room);

        assertThatThrownBy(() -> bookingService.checkOut(101L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_ROOM_NOT_CHECKED_IN);
    }

    @Test
    @DisplayName("Zero-Balance Gatekeeper: Chặn Check-out khi còn nợ tiền (balanceDue > 0)")
    void checkOut_WhenBalanceDuePositive_ShouldThrowCheckoutBalanceUnpaid() {
        BookingDailyRate dailyRate = BookingDailyRate.builder()
                .id(1L)
                .stayDate(LocalDate.now())
                .netPrice(new BigDecimal("1000000.00"))
                .vatPercent(new BigDecimal("10.00"))
                .build();

        BookingRoom room = BookingRoom.builder()
                .id(101L)
                .status(BookingRoomStatus.CHECKED_IN)
                .dailyRates(List.of(dailyRate))
                .charges(List.of())
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(201L)
                .roomTypeName("Deluxe")
                .bookingRooms(List.of(room))
                .build();

        Booking booking = Booking.builder()
                .id(301L)
                .bookingNumber("BK-TEST-001")
                .serviceFeeRate(new BigDecimal("5.00"))
                .bookingDetails(List.of(detail))
                .build();

        room.setBookingDetail(detail);
        detail.setBooking(booking);

        when(bookingRoomService.getEntityById(101L)).thenReturn(room);
        when(serviceOrderService.getOrdersByBookingId(301L)).thenReturn(List.of());
        when(paymentRepository.findByBookingId(301L)).thenReturn(List.of()); // Chưa thanh toán đồng nào

        assertThatThrownBy(() -> bookingService.checkOut(101L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHECKOUT_BALANCE_UNPAID);
    }

    @Test
    @DisplayName("Check-out thành công khi đã trả hết nợ: chuyển phòng sang DIRTY, đóng sổ hóa đơn và service orders")
    void checkOut_WhenBalanceDueZero_ShouldSucceedAndMarkRoomDirtyAndCloseLedger() {
        BookingDailyRate dailyRate = BookingDailyRate.builder()
                .id(1L)
                .stayDate(LocalDate.now())
                .netPrice(new BigDecimal("1000000.00"))
                .vatPercent(new BigDecimal("10.00"))
                .build();

        RoomInstance roomInstance = RoomInstance.builder()
                .id(10)
                .roomNumber("101")
                .currentStatus("OCCUPIED")
                .build();

        BookingRoom room = BookingRoom.builder()
                .id(101L)
                .status(BookingRoomStatus.CHECKED_IN)
                .roomInstance(roomInstance)
                .dailyRates(List.of(dailyRate))
                .charges(List.of())
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(201L)
                .roomTypeName("Deluxe")
                .checkInDate(LocalDate.now().minusDays(1))
                .checkOutDate(LocalDate.now())
                .bookingRooms(List.of(room))
                .build();

        Booking booking = Booking.builder()
                .id(301L)
                .bookingNumber("BK-TEST-001")
                .serviceFeeRate(new BigDecimal("5.00"))
                .bookingDetails(List.of(detail))
                .build();

        room.setBookingDetail(detail);
        detail.setBooking(booking);

        // Giả lập tiền phòng = 1000k + 5% phí (50k) = 1050k + 10% VAT (105k) = 1155000.00 đ
        BigDecimal grandTotal = new BigDecimal("1155000.00");
        vn.edu.utc.hotel_booking.modules.finance.entity.Payment paid = vn.edu.utc.hotel_booking.modules.finance.entity.Payment.builder()
                .id(999L)
                .totalAmount(grandTotal)
                .status(vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus.SUCCESS)
                .build();

        when(bookingRoomService.getEntityById(101L)).thenReturn(room);
        when(serviceOrderService.getOrdersByBookingId(301L)).thenReturn(List.of());
        when(paymentRepository.findByBookingId(301L)).thenReturn(List.of(paid));
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(301L).build());

        BookingResponse response = bookingService.checkOut(101L);

        assertThat(response).isNotNull();
        assertThat(room.getStatus()).isEqualTo(BookingRoomStatus.CHECKED_OUT);
        assertThat(room.getActualCheckOutAt()).isNotNull();

        // Kiểm tra chuyển phòng sang DIRTY trước
        verify(roomInstanceService).updateRoomStatusEntity(10, "DIRTY");
        verify(roomSlotService).markSlotsDirty(eq(10), eq(detail.getCheckInDate()), eq(detail.getCheckOutDate()));

        // Kiểm tra đóng sổ liên hoàn
        verify(serviceOrderService).markOrdersPaidForBooking(301L);
        verify(invoiceService).settleAndCloseInvoice(301L);
        verify(stayGuestService).syncLodgingQueueForBookingRoom(101L);
    }

    @Test
    @DisplayName("calculateWalkInQuote - Thành công tính giá và phụ thu cho phòng walk-in")
    void calculateWalkInQuote_Success() {
        Short hotelId = 1;
        Integer roomTypeId = 10;
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);

        WalkInQuoteRequest request = WalkInQuoteRequest.builder()
                .hotelId(hotelId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .rooms(List.of(
                        WalkInQuoteRequest.WalkInQuoteRoomItemRequest.builder()
                                .hotelRoomTypeId(roomTypeId)
                                .adultCount((short) 2)
                                .build()
                ))
                .build();

        Hotel hotel = Hotel.builder().id(hotelId).name("Khách sạn Grand").build();
        RoomType rt = RoomType.builder().id((short) 1).name("Deluxe").build();
        HotelRoomType hrt = HotelRoomType.builder().id(roomTypeId).roomType(rt).hotel(hotel).standardAdults((short) 2).build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .totalRoomNetAmount(new BigDecimal("1000000.00"))
                .totalRoomServiceFee(new BigDecimal("50000.00"))
                .totalRoomVat(new BigDecimal("105000.00"))
                .totalRoomGrossAmount(new BigDecimal("1155000.00"))
                .totalNights(1)
                .dailyPrices(List.of())
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(hotelRoomTypeService.getEntityById(roomTypeId)).thenReturn(hrt);
        when(priceEngine.calculatePrice(any())).thenReturn(breakdown);

        WalkInQuoteResponse response = bookingService.calculateWalkInQuote(request);

        assertThat(response).isNotNull();
        assertThat(response.getRooms()).hasSize(1);
        assertThat(response.getSubtotalAmount()).isEqualByComparingTo("1000000.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1155000.00");
    }

    @Test
    @DisplayName("calculateWalkInQuote - Ném ngoại lệ khi chọn quá 5 phòng")
    void calculateWalkInQuote_ExceedsMaxRooms_ThrowsException() {
        WalkInQuoteRequest request = WalkInQuoteRequest.builder()
                .hotelId((short) 1)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .rooms(List.of(
                        WalkInQuoteRequest.WalkInQuoteRoomItemRequest.builder().hotelRoomTypeId(1).build(),
                        WalkInQuoteRequest.WalkInQuoteRoomItemRequest.builder().hotelRoomTypeId(2).build(),
                        WalkInQuoteRequest.WalkInQuoteRoomItemRequest.builder().hotelRoomTypeId(3).build(),
                        WalkInQuoteRequest.WalkInQuoteRoomItemRequest.builder().hotelRoomTypeId(4).build(),
                        WalkInQuoteRequest.WalkInQuoteRoomItemRequest.builder().hotelRoomTypeId(5).build(),
                        WalkInQuoteRequest.WalkInQuoteRoomItemRequest.builder().hotelRoomTypeId(6).build()
                ))
                .build();

        assertThatThrownBy(() -> bookingService.calculateWalkInQuote(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEEDS_MAX_ROOMS);
    }

    @Test
    @DisplayName("createWalkInBooking - Đặt phòng ít (1 phòng) bắt buộc thanh toán 100%, ném ngoại lệ nếu trả thiếu hoặc chọn đặt cọc")
    void createWalkInBooking_SmallBooking_Requires100PercentPayment() {
        Short hotelId = 1;
        Integer roomTypeId = 10;
        Integer roomInstanceId = 101;
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);

        WalkInBookingRequest request = WalkInBookingRequest.builder()
                .hotelId(hotelId)
                .guestId(1L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .rooms(List.of(
                        WalkInBookingRequest.WalkInRoomItemRequest.builder()
                                .hotelRoomTypeId(roomTypeId)
                                .roomInstanceId(roomInstanceId)
                                .build()
                ))
                .payment(WalkInBookingRequest.WalkInPaymentRequest.builder()
                        .paymentMethod(PaymentMethod.CASH)
                        .paymentPurpose("DEPOSIT") // Chọn deposit cho đơn ít phòng
                        .amount(new BigDecimal("500000.00")) // Trả thiếu so với 1155000
                        .build())
                .build();

        Hotel hotel = Hotel.builder().id(hotelId).build();
        BookingGuest guest = BookingGuest.builder().id(1L).build();
        RoomType rt = RoomType.builder().id((short) 1).name("Deluxe").build();
        HotelRoomType hrt = HotelRoomType.builder().id(roomTypeId).roomType(rt).hotel(hotel).build();
        RoomInstance roomInst = RoomInstance.builder().id(roomInstanceId).hotel(hotel).hotelRoomType(hrt).roomNumber("101").currentStatus("READY").build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .totalRoomGrossAmount(new BigDecimal("1155000.00"))
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(guestService.getEntityById(1L)).thenReturn(guest);
        when(roomInstanceService.getEntityById(roomInstanceId)).thenReturn(roomInst);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(roomInstanceId), any(), any())).thenReturn(List.of());
        when(priceEngine.calculatePrice(any())).thenReturn(breakdown);

        assertThatThrownBy(() -> bookingService.createWalkInBooking(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALK_IN_REQUIRE_FULL_PAYMENT);
    }

    @Test
    @DisplayName("createWalkInBooking - Ném ngoại lệ khi phòng vật lý không ở trạng thái READY")
    void createWalkInBooking_RoomNotReady_ThrowsException() {
        Short hotelId = 1;
        Integer roomTypeId = 10;
        Integer roomInstanceId = 101;
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);

        WalkInBookingRequest request = WalkInBookingRequest.builder()
                .hotelId(hotelId)
                .guestId(1L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .rooms(List.of(
                        WalkInBookingRequest.WalkInRoomItemRequest.builder()
                                .hotelRoomTypeId(roomTypeId)
                                .roomInstanceId(roomInstanceId)
                                .build()
                ))
                .payment(WalkInBookingRequest.WalkInPaymentRequest.builder()
                        .paymentMethod(PaymentMethod.CASH)
                        .paymentPurpose("FULL_PAYMENT")
                        .amount(new BigDecimal("1155000.00"))
                        .build())
                .build();

        Hotel hotel = Hotel.builder().id(hotelId).build();
        BookingGuest guest = BookingGuest.builder().id(1L).build();
        RoomType rt = RoomType.builder().id((short) 1).name("Deluxe").build();
        HotelRoomType hrt = HotelRoomType.builder().id(roomTypeId).roomType(rt).hotel(hotel).build();
        RoomInstance dirtyRoom = RoomInstance.builder().id(roomInstanceId).hotel(hotel).hotelRoomType(hrt).roomNumber("101").currentStatus("DIRTY").build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(guestService.getEntityById(1L)).thenReturn(guest);
        when(roomInstanceService.getEntityById(roomInstanceId)).thenReturn(dirtyRoom);

        assertThatThrownBy(() -> bookingService.createWalkInBooking(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_READY_FOR_CHECKIN);
    }

    @Test
    @DisplayName("createWalkInBooking - Thành công tạo đơn, Check-in sang OCCUPIED/IN-HOUSE và quyết toán")
    void createWalkInBooking_Success_DirectInHouse_AndPaid() {
        Short hotelId = 1;
        Integer roomTypeId = 10;
        Integer roomInstanceId = 101;
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);
        BigDecimal total = new BigDecimal("1155000.00");

        WalkInBookingRequest request = WalkInBookingRequest.builder()
                .hotelId(hotelId)
                .guestId(1L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .rooms(List.of(
                        WalkInBookingRequest.WalkInRoomItemRequest.builder()
                                .hotelRoomTypeId(roomTypeId)
                                .roomInstanceId(roomInstanceId)
                                .stayGuests(List.of(
                                        WalkInBookingRequest.WalkInStayGuestRequest.builder()
                                                .fullName("Nguyễn Văn A")
                                                .identityNumber("001200000123")
                                                .gender(Gender.MALE)
                                                .build()
                                ))
                                .build()
                ))
                .payment(WalkInBookingRequest.WalkInPaymentRequest.builder()
                        .paymentMethod(PaymentMethod.CASH)
                        .paymentPurpose("FULL_PAYMENT")
                        .amount(total)
                        .build())
                .build();

        Hotel hotel = Hotel.builder().id(hotelId).build();
        BookingGuest guest = BookingGuest.builder().id(1L).build();
        RoomType rt = RoomType.builder().id((short) 1).name("Deluxe").build();
        HotelRoomType hrt = HotelRoomType.builder().id(roomTypeId).roomType(rt).hotel(hotel).build();
        RoomInstance readyRoom = RoomInstance.builder().id(roomInstanceId).hotel(hotel).hotelRoomType(hrt).roomNumber("101").currentStatus("READY").build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .totalRoomNetAmount(new BigDecimal("1000000.00"))
                .totalRoomGrossAmount(total)
                .dailyPrices(List.of(
                        DailyPriceDto.builder()
                                .date(checkIn)
                                .baseRate(new BigDecimal("1000000.00"))
                                .netAmount(new BigDecimal("1000000.00"))
                                .build()
                ))
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(guestService.getEntityById(1L)).thenReturn(guest);
        when(roomInstanceService.getEntityById(roomInstanceId)).thenReturn(readyRoom);
        when(hotelRoomTypeService.getEntityById(roomTypeId)).thenReturn(hrt);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(roomInstanceId), any(), any())).thenReturn(List.of());
        when(priceEngine.calculatePrice(any())).thenReturn(breakdown);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(888L);
            return b;
        });
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(888L).status(BookingStatus.CONFIRMED).build());

        BookingResponse response = bookingService.createWalkInBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(888L);

        // Kiểm tra chuyển trạng thái phòng sang OCCUPIED
        verify(roomInstanceService).updateRoomStatusEntity(roomInstanceId, "OCCUPIED");
        verify(roomSlotService).createOrReserveSlots(eq(readyRoom), eq(checkIn), eq(checkOut), any());
        verify(roomSlotService).occupySlots(eq(roomInstanceId), eq(checkIn), eq(checkOut));

        // Kiểm tra tạo stay guest và đồng bộ BCA
        verify(stayGuestService).createOrCheckIn(any());
        verify(stayGuestService).syncLodgingQueueForBookingRoom(any());

        // Kiểm tra ghi nhận thanh toán và hóa đơn
        verify(paymentRepository).save(any());
        verify(invoiceService).createInvoice(any());
        verify(invoiceService).settleAndCloseInvoice(888L);
    }

    @Test
    @DisplayName("createWalkInBooking - Đặt nhiều phòng (3 phòng) cho phép đặt cọc >= 50%")
    void createWalkInBooking_GroupBooking_AllowsDepositPayment() {
        Short hotelId = 1;
        Integer roomTypeId = 10;
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);
        BigDecimal singleGross = new BigDecimal("1000000.00");
        BigDecimal depositAmount = new BigDecimal("1500000.00"); // 50% của 3,000,000 đ

        List<WalkInBookingRequest.WalkInRoomItemRequest> rooms = List.of(
                WalkInBookingRequest.WalkInRoomItemRequest.builder().hotelRoomTypeId(roomTypeId).roomInstanceId(101).build(),
                WalkInBookingRequest.WalkInRoomItemRequest.builder().hotelRoomTypeId(roomTypeId).roomInstanceId(102).build(),
                WalkInBookingRequest.WalkInRoomItemRequest.builder().hotelRoomTypeId(roomTypeId).roomInstanceId(103).build()
        );

        WalkInBookingRequest request = WalkInBookingRequest.builder()
                .hotelId(hotelId)
                .guestId(1L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .rooms(rooms)
                .payment(WalkInBookingRequest.WalkInPaymentRequest.builder()
                        .paymentMethod(PaymentMethod.BANK_TRANSFER)
                        .paymentPurpose("DEPOSIT")
                        .amount(depositAmount)
                        .transactionReference("TXN-DEP-3ROOMS")
                        .build())
                .build();

        Hotel hotel = Hotel.builder().id(hotelId).build();
        BookingGuest guest = BookingGuest.builder().id(1L).build();
        RoomType rt = RoomType.builder().id((short) 1).name("Deluxe").build();
        HotelRoomType hrt = HotelRoomType.builder().id(roomTypeId).roomType(rt).hotel(hotel).build();

        RoomInstance r101 = RoomInstance.builder().id(101).hotel(hotel).hotelRoomType(hrt).roomNumber("101").currentStatus("READY").build();
        RoomInstance r102 = RoomInstance.builder().id(102).hotel(hotel).hotelRoomType(hrt).roomNumber("102").currentStatus("READY").build();
        RoomInstance r103 = RoomInstance.builder().id(103).hotel(hotel).hotelRoomType(hrt).roomNumber("103").currentStatus("READY").build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .totalRoomNetAmount(new BigDecimal("900000.00"))
                .totalRoomGrossAmount(singleGross)
                .dailyPrices(List.of(
                        DailyPriceDto.builder()
                                .date(checkIn)
                                .baseRate(new BigDecimal("900000.00"))
                                .netAmount(new BigDecimal("900000.00"))
                                .build()
                ))
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(guestService.getEntityById(1L)).thenReturn(guest);
        when(roomInstanceService.getEntityById(101)).thenReturn(r101);
        when(roomInstanceService.getEntityById(102)).thenReturn(r102);
        when(roomInstanceService.getEntityById(103)).thenReturn(r103);
        when(hotelRoomTypeService.getEntityById(roomTypeId)).thenReturn(hrt);
        when(bookingRoomService.findOverlappingAssignedRooms(any(), any(), any())).thenReturn(List.of());
        when(priceEngine.calculatePrice(any())).thenReturn(breakdown);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(777L);
            return b;
        });
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(777L).status(BookingStatus.CONFIRMED).build());

        BookingResponse response = bookingService.createWalkInBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(777L);

        // Các phòng đều chuyển sang OCCUPIED
        verify(roomInstanceService).updateRoomStatusEntity(101, "OCCUPIED");
        verify(roomInstanceService).updateRoomStatusEntity(102, "OCCUPIED");
        verify(roomInstanceService).updateRoomStatusEntity(103, "OCCUPIED");

        // Ghi nhận thanh toán và hóa đơn mở (không đóng vì chỉ mới đặt cọc 50%)
        verify(paymentRepository).save(any());
        verify(invoiceService).createInvoice(any());
        verify(invoiceService, never()).settleAndCloseInvoice(777L);
    }

    @Test
    @DisplayName("createWalkInBooking - Đặt nhiều phòng nhưng cọc dưới 50% ném INVALID_PAYMENT_AMOUNT")
    void createWalkInBooking_GroupBooking_DepositLessThan50Percent_ThrowsException() {
        Short hotelId = 1;
        Integer roomTypeId = 10;
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);
        BigDecimal singleGross = new BigDecimal("1000000.00");
        BigDecimal depositAmount = new BigDecimal("1000000.00"); // 1M < 1.5M (50% của 3M)

        List<WalkInBookingRequest.WalkInRoomItemRequest> rooms = List.of(
                WalkInBookingRequest.WalkInRoomItemRequest.builder().hotelRoomTypeId(roomTypeId).roomInstanceId(101).build(),
                WalkInBookingRequest.WalkInRoomItemRequest.builder().hotelRoomTypeId(roomTypeId).roomInstanceId(102).build(),
                WalkInBookingRequest.WalkInRoomItemRequest.builder().hotelRoomTypeId(roomTypeId).roomInstanceId(103).build()
        );

        WalkInBookingRequest request = WalkInBookingRequest.builder()
                .hotelId(hotelId)
                .guestId(1L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .rooms(rooms)
                .payment(WalkInBookingRequest.WalkInPaymentRequest.builder()
                        .paymentMethod(PaymentMethod.CASH)
                        .paymentPurpose("DEPOSIT")
                        .amount(depositAmount)
                        .build())
                .build();

        Hotel hotel = Hotel.builder().id(hotelId).build();
        BookingGuest guest = BookingGuest.builder().id(1L).build();
        RoomType rt = RoomType.builder().id((short) 1).name("Deluxe").build();
        HotelRoomType hrt = HotelRoomType.builder().id(roomTypeId).roomType(rt).hotel(hotel).build();

        RoomInstance r101 = RoomInstance.builder().id(101).hotel(hotel).hotelRoomType(hrt).roomNumber("101").currentStatus("READY").build();
        RoomInstance r102 = RoomInstance.builder().id(102).hotel(hotel).hotelRoomType(hrt).roomNumber("102").currentStatus("READY").build();
        RoomInstance r103 = RoomInstance.builder().id(103).hotel(hotel).hotelRoomType(hrt).roomNumber("103").currentStatus("READY").build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .totalRoomGrossAmount(singleGross)
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(guestService.getEntityById(1L)).thenReturn(guest);
        when(roomInstanceService.getEntityById(101)).thenReturn(r101);
        when(roomInstanceService.getEntityById(102)).thenReturn(r102);
        when(roomInstanceService.getEntityById(103)).thenReturn(r103);
        when(bookingRoomService.findOverlappingAssignedRooms(any(), any(), any())).thenReturn(List.of());
        when(priceEngine.calculatePrice(any())).thenReturn(breakdown);

        assertThatThrownBy(() -> bookingService.createWalkInBooking(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    @Test
    @DisplayName("createWalkInBooking - Khách doanh nghiệp (1 phòng có Company) cho phép đặt cọc >= 50%")
    void createWalkInBooking_CorporateBooking_AllowsDepositPayment() {
        Short hotelId = 1;
        Integer roomTypeId = 10;
        Integer roomInstanceId = 101;
        Long companyId = 50L;
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);
        BigDecimal total = new BigDecimal("1000000.00");
        BigDecimal depositAmount = new BigDecimal("600000.00"); // 60% >= 50%

        WalkInBookingRequest request = WalkInBookingRequest.builder()
                .hotelId(hotelId)
                .guestId(1L)
                .companyId(companyId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .rooms(List.of(
                        WalkInBookingRequest.WalkInRoomItemRequest.builder()
                                .hotelRoomTypeId(roomTypeId)
                                .roomInstanceId(roomInstanceId)
                                .build()
                ))
                .payment(WalkInBookingRequest.WalkInPaymentRequest.builder()
                        .paymentMethod(PaymentMethod.CREDIT_CARD)
                        .paymentPurpose("DEPOSIT")
                        .amount(depositAmount)
                        .transactionReference("CC-REF-CORP")
                        .build())
                .build();

        Hotel hotel = Hotel.builder().id(hotelId).build();
        BookingGuest guest = BookingGuest.builder().id(1L).build();
        Company company = Company.builder().id(companyId).name("FPT Software").build();
        RoomType rt = RoomType.builder().id((short) 1).name("Deluxe").build();
        HotelRoomType hrt = HotelRoomType.builder().id(roomTypeId).roomType(rt).hotel(hotel).build();
        RoomInstance readyRoom = RoomInstance.builder().id(roomInstanceId).hotel(hotel).hotelRoomType(hrt).roomNumber("101").currentStatus("READY").build();

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .totalRoomNetAmount(new BigDecimal("900000.00"))
                .totalRoomGrossAmount(total)
                .dailyPrices(List.of(
                        DailyPriceDto.builder()
                                .date(checkIn)
                                .baseRate(new BigDecimal("900000.00"))
                                .netAmount(new BigDecimal("900000.00"))
                                .build()
                ))
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(companyService.getEntityById(companyId)).thenReturn(company);
        when(guestService.getEntityById(1L)).thenReturn(guest);
        when(roomInstanceService.getEntityById(roomInstanceId)).thenReturn(readyRoom);
        when(hotelRoomTypeService.getEntityById(roomTypeId)).thenReturn(hrt);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(roomInstanceId), any(), any())).thenReturn(List.of());
        when(priceEngine.calculatePrice(any())).thenReturn(breakdown);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(999L);
            return b;
        });
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(999L).status(BookingStatus.CONFIRMED).build());

        BookingResponse response = bookingService.createWalkInBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(999L);
        verify(companyService).getEntityById(companyId);
        verify(roomInstanceService).updateRoomStatusEntity(roomInstanceId, "OCCUPIED");
        verify(invoiceService, never()).settleAndCloseInvoice(999L);
    }

    @Test
    @DisplayName("processNoShowBookings: Đơn 1 đêm không trả trước -> Phạt đêm đầu, giải phóng phòng và kho, không tạo REFUND")
    void processNoShowBookings_SingleNight_AppliesPenalty_AndReleasesAll() {
        Short hotelId = 1;
        LocalDate auditDate = LocalDate.of(2026, 10, 20);
        LocalDate checkIn = LocalDate.of(2026, 10, 20);
        LocalDate checkOut = LocalDate.of(2026, 10, 21);

        RoomInstance roomInstance = RoomInstance.builder()
                .id(101)
                .roomNumber("101")
                .build();

        BookingRoom bookingRoom = BookingRoom.builder()
                .id(501L)
                .status(BookingRoomStatus.EXPECTED)
                .roomInstance(roomInstance)
                .build();

        BookingDailyRate dailyRate = BookingDailyRate.builder()
                .stayDate(checkIn)
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(new BigDecimal("50000.00"))
                .vatAmount(new BigDecimal("105000.00"))
                .build();
        bookingRoom.setDailyRates(List.of(dailyRate));

        BookingDetail detail = BookingDetail.builder()
                .id(301L)
                .hotelRoomType(testHotelRoomType)
                .quantity((short) 1)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .bookingRooms(new ArrayList<>(List.of(bookingRoom)))
                .build();
        bookingRoom.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(1001L)
                .hotel(testHotel)
                .bookingNumber("BK-NOSHOW-1")
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(new BigDecimal("5.00"))
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();

        when(bookingRepository.findByHotelIdAndStatusWithDetails(hotelId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByBookingId(1001L)).thenReturn(List.of());

        int count = bookingService.processNoShowBookings(hotelId, auditDate);

        assertThat(count).isEqualTo(1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(bookingRoom.getStatus()).isEqualTo(BookingRoomStatus.NO_SHOW);

        // 1. Phạt đêm đầu: 1,000,000 + 50,000 + 105,000 = 1,155,000đ
        verify(bookingChargeService).createCharge(eq(bookingRoom), argThat(req ->
                req.getChargeType() == BookingChargeType.PENALTY &&
                        req.getUnitPrice().compareTo(new BigDecimal("1155000.00")) == 0
        ));

        // 2. Release phòng vật lý và room slot
        verify(roomSlotService).releaseSlots(101, checkIn, checkOut);
        verify(roomInstanceService).updateRoomStatusEntity(101, "READY");

        // 3. Hoàn trả kho bán
        verify(roomAvailabilityService).releaseBookedInventory(testHotelRoomType.getId(), checkIn, checkOut, 1);

        // 4. Hủy hàng chờ lưu trú BCA
        verify(stayGuestService).cancelLodgingQueueForBookingRoom(501L);

        // 5. Không tạo Payment refund vì totalPaid = 0
        verify(paymentRepository, never()).save(any(Payment.class));

        // 6. Ghi AuditLog
        verify(auditLogService).recordLog(isNull(), eq("BOOKING_NO_SHOW"), eq("Booking"), eq("1001"), eq("CONFIRMED"), anyString(), isNull(), eq("SYSTEM_NIGHT_AUDIT"));
    }

    @Test
    @DisplayName("processNoShowBookings: Đơn nhiều đêm đã trả trước 100% -> Phạt đêm 1, release toàn bộ đêm, tạo Payment REFUND ở trạng thái PENDING")
    void processNoShowBookings_MultiNight_WithFullPrepayment_CreatesPendingRefund() {
        Short hotelId = 1;
        LocalDate auditDate = LocalDate.of(2026, 10, 20);
        LocalDate checkIn = LocalDate.of(2026, 10, 20);
        LocalDate checkOut = LocalDate.of(2026, 10, 23); // 3 đêm

        RoomInstance roomInstance = RoomInstance.builder()
                .id(202)
                .roomNumber("202")
                .build();

        BookingRoom bookingRoom = BookingRoom.builder()
                .id(601L)
                .status(BookingRoomStatus.EXPECTED)
                .roomInstance(roomInstance)
                .build();

        BookingDailyRate d1 = BookingDailyRate.builder()
                .stayDate(LocalDate.of(2026, 10, 20))
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .build();
        BookingDailyRate d2 = BookingDailyRate.builder()
                .stayDate(LocalDate.of(2026, 10, 21))
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .build();
        BookingDailyRate d3 = BookingDailyRate.builder()
                .stayDate(LocalDate.of(2026, 10, 22))
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .build();
        bookingRoom.setDailyRates(List.of(d1, d2, d3));

        BookingDetail detail = BookingDetail.builder()
                .id(401L)
                .hotelRoomType(testHotelRoomType)
                .quantity((short) 1)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .bookingRooms(new ArrayList<>(List.of(bookingRoom)))
                .build();
        bookingRoom.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(2002L)
                .hotel(testHotel)
                .bookingNumber("BK-MULTINIGHT-1")
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(BigDecimal.ZERO)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();

        // Khách đã trả trước 3,000,000đ qua cổng thanh toán
        Payment payment = Payment.builder()
                .id(901L)
                .booking(booking)
                .totalAmount(new BigDecimal("3000000.00"))
                .status(PaymentStatus.SUCCESS)
                .paymentPurpose(PaymentPurpose.DEPOSIT)
                .build();

        when(bookingRepository.findByHotelIdAndStatusWithDetails(hotelId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByBookingId(2002L)).thenReturn(List.of(payment));

        int count = bookingService.processNoShowBookings(hotelId, auditDate);

        assertThat(count).isEqualTo(1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);

        // 1. Phạt đêm 1 = 1,000,000đ
        verify(bookingChargeService).createCharge(eq(bookingRoom), argThat(req ->
                req.getChargeType() == BookingChargeType.PENALTY &&
                        req.getUnitPrice().compareTo(new BigDecimal("1000000.00")) == 0
        ));

        // 2. Release toàn bộ 3 đêm
        verify(roomSlotService).releaseSlots(202, checkIn, checkOut);
        verify(roomInstanceService).updateRoomStatusEntity(202, "READY");
        verify(roomAvailabilityService).releaseBookedInventory(testHotelRoomType.getId(), checkIn, checkOut, 1);

        // 3. Tạo bản ghi REFUND ở trạng thái PENDING với số tiền dư 2,000,000đ (3tr - 1tr phạt)
        verify(paymentRepository).save(argThat(p ->
                p.getPaymentPurpose() == PaymentPurpose.REFUND &&
                        p.getStatus() == PaymentStatus.PENDING &&
                        p.getTotalAmount().compareTo(new BigDecimal("2000000.00")) == 0
        ));
    }

    @Test
    @DisplayName("markBookingAsNoShow: Lễ tân gọi API thủ công có lý do -> Thực thi pipeline và lưu AuditLog")
    void markBookingAsNoShow_Manual_WithReason_Success() {
        Long bookingId = 3003L;
        LocalDate checkIn = LocalDate.of(2026, 10, 20);
        LocalDate checkOut = LocalDate.of(2026, 10, 21);

        BookingRoom room = BookingRoom.builder()
                .id(701L)
                .status(BookingRoomStatus.EXPECTED)
                .build();
        BookingDailyRate d1 = BookingDailyRate.builder()
                .stayDate(checkIn)
                .netPrice(new BigDecimal("800000.00"))
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .build();
        room.setDailyRates(List.of(d1));

        BookingDetail detail = BookingDetail.builder()
                .id(501L)
                .hotelRoomType(testHotelRoomType)
                .quantity((short) 1)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .bookingRooms(new ArrayList<>(List.of(room)))
                .build();
        room.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .hotel(testHotel)
                .bookingNumber("BK-MANUAL-1")
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(BigDecimal.ZERO)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(List.of());
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(bookingId).status(BookingStatus.NO_SHOW).build());

        BookingNoShowRequest request = new BookingNoShowRequest("Khách gọi điện hủy gấp do lỡ chuyến bay");
        BookingResponse response = bookingService.markBookingAsNoShow(bookingId, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(booking.getNotes()).contains("Khách gọi điện hủy gấp do lỡ chuyến bay");

        verify(bookingChargeService).createCharge(eq(room), any());
        verify(auditLogService).recordLog(isNull(), eq("BOOKING_NO_SHOW"), eq("Booking"), eq("3003"), eq("CONFIRMED"), anyString(), isNull(), anyString());
    }

    @Test
    @DisplayName("markBookingAsNoShow: Đơn đã có phòng CHECKED_IN -> Ném ngoại lệ BOOKING_ALREADY_CHECKED_IN")
    void markBookingAsNoShow_AlreadyCheckedIn_ThrowsException() {
        Long bookingId = 4004L;
        BookingRoom room = BookingRoom.builder()
                .id(801L)
                .status(BookingRoomStatus.CHECKED_IN)
                .actualCheckInAt(OffsetDateTime.now())
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(601L)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .bookingRooms(List.of(room))
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(List.of(detail))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingNoShowRequest request = new BookingNoShowRequest("Khách không đến");

        assertThatThrownBy(() -> bookingService.markBookingAsNoShow(bookingId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_ALREADY_CHECKED_IN);
    }

    @Test
    @DisplayName("markBookingAsNoShow: Lý do rỗng hoặc khoảng trắng -> Ném ngoại lệ INVALID_REQUEST_DATA")
    void markBookingAsNoShow_BlankReason_ThrowsValidationException() {
        Long bookingId = 5005L;
        BookingNoShowRequest request = new BookingNoShowRequest("   ");

        assertThatThrownBy(() -> bookingService.markBookingAsNoShow(bookingId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("markBookingAsNoShow: Đơn có dịch vụ đặt trước -> Giữ lại tiền dịch vụ và tiền phạt đêm đầu, không hoàn tiền dịch vụ")
    void executeNoShowPipeline_WithPreorderedServices_DoesNotRefundServices() {
        Long bookingId = 6006L;
        LocalDate checkIn = LocalDate.of(2026, 10, 20);
        LocalDate checkOut = LocalDate.of(2026, 10, 22);

        BookingRoom room = BookingRoom.builder()
                .id(901L)
                .status(BookingRoomStatus.EXPECTED)
                .build();
        BookingDailyRate d1 = BookingDailyRate.builder()
                .stayDate(checkIn)
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .build();
        room.setDailyRates(List.of(d1));

        BookingDetail detail = BookingDetail.builder()
                .id(701L)
                .hotelRoomType(testHotelRoomType)
                .quantity((short) 1)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .bookingRooms(new ArrayList<>(List.of(room)))
                .build();
        room.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .hotel(testHotel)
                .bookingNumber("BK-SERVICES-1")
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(BigDecimal.ZERO)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();

        // Khách đã trả 2,000,000 đ cọc
        Payment successPayment = Payment.builder()
                .id(1L)
                .booking(booking)
                .status(PaymentStatus.SUCCESS)
                .paymentPurpose(PaymentPurpose.DEPOSIT)
                .totalAmount(new BigDecimal("2000000.00"))
                .build();

        // Khách đã order trước dịch vụ đón sân bay 500,000 đ
        ServiceOrderResponse preorderedService = ServiceOrderResponse.builder()
                .id(55L)
                .status(ServiceOrderStatus.PENDING)
                .totalAmount(new BigDecimal("500000.00"))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(List.of(successPayment));
        when(serviceOrderService.getOrdersByBookingId(bookingId)).thenReturn(List.of(preorderedService));
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(bookingId).status(BookingStatus.NO_SHOW).build());

        BookingResponse response = bookingService.markBookingAsNoShow(bookingId, new BookingNoShowRequest("Khách không đến nhận phòng"));

        assertThat(response).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);

        // Phạt đêm đầu: 1,000,000 đ. Dịch vụ không hoàn: 500,000 đ. NonRefundable = 1,500,000 đ.
        // Cọc đã đóng: 2,000,000 đ -> Tiền dư hoàn trả: 2,000,000 - 1,500,000 = 500,000 đ
        verify(paymentRepository).save(argThat(p ->
                p.getPaymentPurpose() == PaymentPurpose.REFUND
                        && p.getStatus() == PaymentStatus.PENDING
                        && p.getTotalAmount().compareTo(new BigDecimal("500000.00")) == 0
        ));
    }

    @Test
    @DisplayName("markBookingRoomAsNoShow: Khách đoàn 1 phòng ở (CHECKED_IN), 1 phòng không đến -> Đánh dấu phòng đó No-Show, giữ nguyên Booking hoạt động")
    void markBookingRoomAsNoShow_MultiRoom_OneInHouse_OneNoShow_Success() {
        Long bookingId = 7007L;
        LocalDate checkIn = LocalDate.of(2026, 10, 20);
        LocalDate checkOut = LocalDate.of(2026, 10, 22);

        RoomInstance roomInstanceB = RoomInstance.builder().roomNumber("202").build();
        roomInstanceB.setId(202);

        BookingRoom roomA = BookingRoom.builder()
                .id(801L)
                .status(BookingRoomStatus.CHECKED_IN)
                .actualCheckInAt(OffsetDateTime.now())
                .build();

        BookingRoom roomB = BookingRoom.builder()
                .id(802L)
                .status(BookingRoomStatus.EXPECTED)
                .roomInstance(roomInstanceB)
                .build();

        BookingDailyRate d1 = BookingDailyRate.builder()
                .stayDate(checkIn)
                .netPrice(new BigDecimal("1000000.00"))
                .serviceFeeAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .build();
        roomB.setDailyRates(List.of(d1));

        BookingDetail detail = BookingDetail.builder()
                .id(601L)
                .hotelRoomType(testHotelRoomType)
                .quantity((short) 2)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .bookingRooms(new ArrayList<>(List.of(roomA, roomB)))
                .build();

        roomA.setBookingDetail(detail);
        roomB.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .hotel(testHotel)
                .bookingNumber("BK-MULTI-1")
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(BigDecimal.ZERO)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();
        detail.setBooking(booking);

        when(bookingRoomService.getEntityById(802L)).thenReturn(roomB);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any())).thenReturn(BookingResponse.builder().id(bookingId).status(BookingStatus.CONFIRMED).build());

        BookingResponse response = bookingService.markBookingRoomAsNoShow(802L, new BookingNoShowRequest("1 thành viên trong đoàn không đi"));

        assertThat(response).isNotNull();
        assertThat(roomB.getStatus()).isEqualTo(BookingRoomStatus.NO_SHOW);
        // Booking vẫn duy trì CONFIRMED / active vì Room A vẫn đang CHECKED_IN
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        verify(bookingChargeService).createCharge(eq(roomB), any());
        verify(roomSlotService).releaseSlots(eq(202), eq(checkIn), eq(checkOut));
        verify(roomInstanceService).updateRoomStatusEntity(eq(202), eq("READY"));
        verify(roomAvailabilityService).releaseBookedInventory(eq(testHotelRoomType.getId()), eq(checkIn), eq(checkOut), eq(1));
        verify(stayGuestService).cancelLodgingQueueForBookingRoom(eq(802L));
        verify(auditLogService).recordLog(isNull(), eq("BOOKING_ROOM_NO_SHOW"), eq("BookingRoom"), eq("802"), eq("EXPECTED"), anyString(), isNull(), anyString());
    }

    @Test
    @DisplayName("markBookingRoomAsNoShow: Phòng đã CHECKED_IN -> Ném ngoại lệ INVALID_REQUEST_DATA")
    void markBookingRoomAsNoShow_AlreadyCheckedIn_ThrowsException() {
        BookingRoom room = BookingRoom.builder()
                .id(803L)
                .status(BookingRoomStatus.CHECKED_IN)
                .build();

        when(bookingRoomService.getEntityById(803L)).thenReturn(room);

        assertThatThrownBy(() -> bookingService.markBookingRoomAsNoShow(803L, new BookingNoShowRequest("Hủy phòng")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("getCheckoutSettlement: Đơn có phòng No-Show và cọc dư -> Bù trừ Folio Netting tính chính xác NetOverpayment")
    void checkoutSettlement_WithNoShowCredit_FolioNetting() {
        Long bookingId = 8008L;
        LocalDate checkIn = LocalDate.of(2026, 10, 20);

        RoomInstance roomInstanceA = RoomInstance.builder().roomNumber("101").build();
        roomInstanceA.setId(101);

        BookingRoom roomA = BookingRoom.builder()
                .id(801L)
                .status(BookingRoomStatus.CHECKED_IN)
                .roomInstance(roomInstanceA)
                .build();
        BookingDailyRate drA1 = BookingDailyRate.builder()
                .id(1L)
                .stayDate(checkIn)
                .netPrice(new BigDecimal("1000000.00"))
                .vatPercent(BigDecimal.ZERO)
                .build();
        BookingDailyRate drA2 = BookingDailyRate.builder()
                .id(2L)
                .stayDate(checkIn.plusDays(1))
                .netPrice(new BigDecimal("1000000.00"))
                .vatPercent(BigDecimal.ZERO)
                .build();
        roomA.setDailyRates(List.of(drA1, drA2));

        BookingRoom roomB = BookingRoom.builder()
                .id(802L)
                .status(BookingRoomStatus.NO_SHOW)
                .charges(new ArrayList<>(List.of(
                        BookingCharge.builder()
                                .id(10L)
                                .chargeType(BookingChargeType.PENALTY)
                                .itemName("Phí phạt No-Show phòng 102")
                                .quantity(1)
                                .unitPrice(new BigDecimal("1000000.00"))
                                .subtotal(new BigDecimal("1000000.00"))
                                .serviceFeeRate(BigDecimal.ZERO)
                                .serviceFeeAmount(BigDecimal.ZERO)
                                .vatRate(BigDecimal.ZERO)
                                .vatAmount(BigDecimal.ZERO)
                                .totalAmount(new BigDecimal("1000000.00"))
                                .build()
                )))
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(701L)
                .roomTypeName("Deluxe Room")
                .hotelRoomType(testHotelRoomType)
                .bookingRooms(List.of(roomA, roomB))
                .build();
        roomA.setBookingDetail(detail);
        roomB.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingNumber("BK-NETTING-1")
                .serviceFeeRate(BigDecimal.ZERO)
                .bookingDetails(List.of(detail))
                .build();
        detail.setBooking(booking);

        // Khách ban đầu cọc 4,000,000 đ
        Payment deposit = Payment.builder()
                .id(1L)
                .booking(booking)
                .status(PaymentStatus.SUCCESS)
                .paymentPurpose(PaymentPurpose.DEPOSIT)
                .totalAmount(new BigDecimal("4000000.00"))
                .build();

        when(bookingRoomService.getEntityById(801L)).thenReturn(roomA);
        when(serviceOrderService.getOrdersByBookingId(bookingId)).thenReturn(List.of());
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(List.of(deposit));

        CheckoutSettlementResponse settlement = bookingService.getCheckoutSettlement(801L);

        assertThat(settlement).isNotNull();
        // Tiền phòng thực ở của phòng A = 2,000,000 đ
        assertThat(settlement.getTotalRoomCharges()).isEqualByComparingTo("2000000.00");
        // Phạt No-Show phòng B = 1,000,000 đ
        assertThat(settlement.getNoShowPenaltyAmount()).isEqualByComparingTo("1000000.00");
        // Tổng chi phí = 3,000,000 đ
        assertThat(settlement.getGrandTotal()).isEqualByComparingTo("3000000.00");
        // Tổng đã thanh toán = 4,000,000 đ
        assertThat(settlement.getTotalPaid()).isEqualByComparingTo("4000000.00");
        // Dư nợ = 0 đ (cho phép check-out)
        assertThat(settlement.getBalanceDue()).isEqualByComparingTo("0.00");
        assertThat(settlement.isCanCheckOut()).isTrue();
        // Khoản cọc thừa dôi dư chờ hoàn = 1,000,000 đ
        assertThat(settlement.getNetOverpaymentAmount()).isEqualByComparingTo("1000000.00");
    }

    // =========================================================================
    // FIT CANCELLATION TESTS (UC-CUST-06, SQ-03, US12, FR-RES-09)
    // =========================================================================

    @Test
    @DisplayName("Preview hủy FIT: Kiểm tra các mốc chính sách hủy >= 7 ngày, 4 đến < 7 ngày, và < 4 ngày")
    void previewCancelBooking_FitCancellationPolicyTiers() {
        Long bookingId = 901L;

        // Case 1: >= 7 days before checkin -> 0% penalty
        BookingRoom room1 = BookingRoom.builder()
                .id(911L)
                .status(BookingRoomStatus.EXPECTED)
                .dailyRates(new ArrayList<>(List.of(
                        BookingDailyRate.builder().stayDate(LocalDate.now().plusDays(10)).netPrice(BigDecimal.valueOf(1000000)).build()
                )))
                .build();
        BookingDetail detail1 = BookingDetail.builder()
                .id(921L)
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(11))
                .quantity((short) 1)
                .bookingRooms(new ArrayList<>(List.of(room1)))
                .build();
        room1.setBookingDetail(detail1);

        Booking booking1 = Booking.builder()
                .id(bookingId)
                .bookingNumber("BK-CANCEL-TIER1")
                .bookingType(BookingType.FIT)
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(new ArrayList<>(List.of(detail1)))
                .build();
        detail1.setBooking(booking1);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking1));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(List.of(
                Payment.builder().status(PaymentStatus.SUCCESS).totalAmount(BigDecimal.valueOf(1000000)).build()
        ));

        BookingCancelPreviewResponse preview1 = bookingService.previewCancelBooking(bookingId);
        assertThat(preview1.getPenaltyRatePercentage()).isEqualByComparingTo("0.0");
        assertThat(preview1.getCancellationPenalty()).isEqualByComparingTo("0.00");
        assertThat(preview1.getRefundAmount()).isEqualByComparingTo("1000000.00");
        assertThat(preview1.getRefundRouting()).isEqualTo("AUTO_REFUND");

        // Case 2: 4 to < 7 days -> 50% penalty
        detail1.setCheckInDate(LocalDate.now().plusDays(5));
        BookingCancelPreviewResponse preview2 = bookingService.previewCancelBooking(bookingId);
        assertThat(preview2.getPenaltyRatePercentage()).isEqualByComparingTo("50.0");
        assertThat(preview2.getCancellationPenalty()).isEqualByComparingTo("500000.00");
        assertThat(preview2.getRefundAmount()).isEqualByComparingTo("500000.00");

        // Case 3: < 4 days -> 100% penalty
        detail1.setCheckInDate(LocalDate.now().plusDays(2));
        BookingCancelPreviewResponse preview3 = bookingService.previewCancelBooking(bookingId);
        assertThat(preview3.getPenaltyRatePercentage()).isEqualByComparingTo("100.0");
        assertThat(preview3.getCancellationPenalty()).isEqualByComparingTo("1000000.00");
        assertThat(preview3.getRefundAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Hủy toàn bộ đơn FIT: Tất cả phòng đều EXPECTED -> Chuyển Booking sang CANCELLED, nhả tồn kho và lịch giữ phòng")
    void cancelBooking_AllRoomsCancelled_BookingBecomesCancelled() {
        Long bookingId = 902L;
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(12);

        RoomInstance roomInstance = RoomInstance.builder()
                .roomNumber("301")
                .currentStatus("OCCUPIED")
                .build();
        roomInstance.setId(301);

        BookingRoom room = BookingRoom.builder()
                .id(912L)
                .status(BookingRoomStatus.EXPECTED)
                .roomInstance(roomInstance)
                .dailyRates(new ArrayList<>(List.of(
                        BookingDailyRate.builder().stayDate(checkIn).netPrice(BigDecimal.valueOf(1000000)).build(),
                        BookingDailyRate.builder().stayDate(checkIn.plusDays(1)).netPrice(BigDecimal.valueOf(1000000)).build()
                )))
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(922L)
                .hotelRoomType(testHotelRoomType)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .bookingRooms(new ArrayList<>(List.of(room)))
                .build();
        room.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingNumber("BK-CANCEL-ALL")
                .bookingType(BookingType.FIT)
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();
        detail.setBooking(booking);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(List.of(
                Payment.builder().status(PaymentStatus.SUCCESS).totalAmount(BigDecimal.valueOf(2000000)).build()
        ));

        BookingCancelRequest request = BookingCancelRequest.builder()
                .reason("Khách bận việc đột xuất")
                .build();

        BookingCancelResponse response = bookingService.cancelBooking(bookingId, request);

        assertThat(response).isNotNull();
        assertThat(response.getBookingStatus()).isEqualTo("CANCELLED");
        assertThat(response.getCancelledRoomsCount()).isEqualTo(1);
        assertThat(room.getStatus()).isEqualTo(BookingRoomStatus.CANCELLED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // Kho phòng được hoàn trả đúng số lượng phòng hủy (1)
        verify(roomAvailabilityService).releaseBookedInventory(eq(testHotelRoomType.getId()), eq(checkIn), eq(checkOut), eq(1));
        // Lịch giữ phòng bị hủy
        verify(roomSlotService).releaseSlots(eq(roomInstance.getId()), eq(checkIn), eq(checkOut));
        // Với ngày tương lai (> now), roomInstance.currentStatus KHÔNG ĐƯỢC chạm vào!
        verify(roomInstanceService, never()).updateRoomStatus(any(), any());
        // Hàng chờ lưu trú BCA được hủy
        verify(stayGuestService).cancelLodgingQueueForBookingRoom(room.getId());
    }

    @Test
    @DisplayName("Hủy phòng lẻ FIT: 1 phòng EXPECTED và 1 phòng còn lại -> Booking vẫn giữ CONFIRMED")
    void cancelBookingRoom_PartialCancellation_BookingRemainsConfirmed() {
        Long bookingId = 903L;
        Long bookingRoomId = 913L;
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        RoomInstance roomInstanceA = RoomInstance.builder()
                .roomNumber("101")
                .currentStatus("READY")
                .build();
        roomInstanceA.setId(101);

        BookingRoom roomA = BookingRoom.builder()
                .id(bookingRoomId)
                .status(BookingRoomStatus.EXPECTED)
                .roomInstance(roomInstanceA)
                .dailyRates(new ArrayList<>(List.of(
                        BookingDailyRate.builder().stayDate(checkIn).netPrice(BigDecimal.valueOf(1000000)).build()
                )))
                .build();

        BookingRoom roomB = BookingRoom.builder()
                .id(914L)
                .status(BookingRoomStatus.EXPECTED)
                .dailyRates(new ArrayList<>(List.of(
                        BookingDailyRate.builder().stayDate(checkIn).netPrice(BigDecimal.valueOf(1000000)).build()
                )))
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(923L)
                .hotelRoomType(testHotelRoomType)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 2)
                .bookingRooms(new ArrayList<>(List.of(roomA, roomB)))
                .build();
        roomA.setBookingDetail(detail);
        roomB.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingNumber("BK-CANCEL-PARTIAL")
                .bookingType(BookingType.FIT)
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();
        detail.setBooking(booking);

        when(bookingRoomService.getEntityById(bookingRoomId)).thenReturn(roomA);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(List.of(
                Payment.builder().status(PaymentStatus.SUCCESS).totalAmount(BigDecimal.valueOf(2000000)).build()
        ));

        BookingCancelRequest request = BookingCancelRequest.builder()
                .reason("1 người trong nhóm hủy phòng")
                .build();

        BookingCancelResponse response = bookingService.cancelBookingRoom(bookingRoomId, request);

        assertThat(response).isNotNull();
        assertThat(response.getBookingStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getCancelledRoomsCount()).isEqualTo(1);
        assertThat(roomA.getStatus()).isEqualTo(BookingRoomStatus.CANCELLED);
        assertThat(roomB.getStatus()).isEqualTo(BookingRoomStatus.EXPECTED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Kho phòng hoàn trả 1 phòng
        verify(roomAvailabilityService).releaseBookedInventory(eq(testHotelRoomType.getId()), eq(checkIn), eq(checkOut), eq(1));
        // Lịch giữ phòng của riêng phòng A bị hủy
        verify(roomSlotService).releaseSlots(eq(roomInstanceA.getId()), eq(checkIn), eq(checkOut));
        // Hàng chờ lưu trú của riêng phòng A bị hủy
        verify(stayGuestService).cancelLodgingQueueForBookingRoom(bookingRoomId);
    }

    @Test
    @DisplayName("Không cho phép hủy toàn bộ Booking nếu đã có phòng CHECKED_IN")
    void cancelBooking_CannotCancelIfAnyRoomAlreadyCheckedIn() {
        Long bookingId = 904L;

        BookingRoom roomA = BookingRoom.builder()
                .id(915L)
                .status(BookingRoomStatus.CHECKED_IN)
                .build();
        BookingRoom roomB = BookingRoom.builder()
                .id(916L)
                .status(BookingRoomStatus.EXPECTED)
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(924L)
                .bookingRooms(new ArrayList<>(List.of(roomA, roomB)))
                .build();
        roomA.setBookingDetail(detail);
        roomB.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingNumber("BK-CANNOT-CANCEL")
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();
        detail.setBooking(booking);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingCancelRequest request = BookingCancelRequest.builder().reason("Hủy đơn").build();

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANCELLATION_NOT_ALLOWED_AFTER_CHECKIN);
    }

    @Test
    @DisplayName("Không cho phép hủy phòng lẻ nếu phòng đó đang CHECKED_IN")
    void cancelBookingRoom_CannotCancelCheckedInRoom() {
        Long bookingRoomId = 917L;

        BookingRoom room = BookingRoom.builder()
                .id(bookingRoomId)
                .status(BookingRoomStatus.CHECKED_IN)
                .build();

        when(bookingRoomService.getEntityById(bookingRoomId)).thenReturn(room);

        BookingCancelRequest request = BookingCancelRequest.builder().reason("Hủy phòng").build();

        assertThatThrownBy(() -> bookingService.cancelBookingRoom(bookingRoomId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_ROOM_NOT_ELIGIBLE_FOR_CANCELLATION);
    }

    @Test
    @DisplayName("Hoàn tiền > 5.000.000 VNĐ -> Điều hướng sang WAITING_FINANCE_APPROVAL và lưu trạng thái PENDING")
    void cancelBooking_RefundOver5Million_RoutesToWaitingFinanceApproval() {
        Long bookingId = 905L;
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(12);

        BookingRoom room = BookingRoom.builder()
                .id(918L)
                .status(BookingRoomStatus.EXPECTED)
                .dailyRates(new ArrayList<>(List.of(
                        BookingDailyRate.builder().stayDate(checkIn).netPrice(BigDecimal.valueOf(10000000)).build()
                )))
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(925L)
                .hotelRoomType(testHotelRoomType)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .quantity((short) 1)
                .bookingRooms(new ArrayList<>(List.of(room)))
                .build();
        room.setBookingDetail(detail);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingNumber("BK-REFUND-LARGE")
                .bookingType(BookingType.FIT)
                .status(BookingStatus.CONFIRMED)
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();
        detail.setBooking(booking);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(List.of(
                Payment.builder().status(PaymentStatus.SUCCESS).totalAmount(BigDecimal.valueOf(10000000)).build()
        ));

        BookingCancelRequest request = BookingCancelRequest.builder()
                .reason("Khách hủy đặt trước và yêu cầu hoàn về tài khoản VCB")
                .note("Hoàn về tài khoản Vietcombank 001122334455 NGUYEN VAN A")
                .build();

        BookingCancelResponse response = bookingService.cancelBooking(bookingId, request);

        assertThat(response).isNotNull();
        assertThat(response.getRefundAmount()).isEqualByComparingTo("10000000.00");
        assertThat(response.getRefundRouting()).isEqualTo("WAITING_FINANCE_APPROVAL");

        // Kiểm tra Payment record được lưu với PENDING
        verify(paymentRepository).save(argThat(payment ->
                payment.getPaymentPurpose() == PaymentPurpose.REFUND &&
                payment.getStatus() == PaymentStatus.PENDING &&
                payment.getTotalAmount().compareTo(new BigDecimal("10000000.00")) == 0
        ));
    }
}


