package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomSwapRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSwapResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.RoomSwapRecordService;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;
import vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingRoomSwapTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingRoomService bookingRoomService;
    @Mock BookingChargeService bookingChargeService;
    @Mock RoomInstanceService roomInstanceService;
    @Mock RoomSlotService roomSlotService;
    @Mock StayGuestService stayGuestService;
    @Mock RoomSwapRecordService roomSwapRecordService;
    @Mock AuditLogService auditLogService;

    @InjectMocks
    BookingServiceImpl bookingService;

    private Hotel hotel;
    private HotelRoomType standardRoomType;
    private HotelRoomType suiteRoomType;
    private RoomInstance room101;
    private RoomInstance room102;
    private RoomInstance room201Suite;
    private Booking booking;
    private BookingDetail bookingDetail;
    private BookingRoom bookingRoom;
    private StayGuest stayGuest;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder().name("Grand Plaza Hanoi").build();
        hotel.setId((short) 1);

        RoomType standardType = RoomType.builder().code("STD").name("Standard Room").build();
        standardType.setId((short) 1);

        RoomType suiteType = RoomType.builder().code("STE").name("Executive Suite").build();
        suiteType.setId((short) 2);

        standardRoomType = HotelRoomType.builder()
                .hotel(hotel)
                .roomType(standardType)
                .basePrice(BigDecimal.valueOf(1000000))
                .build();
        standardRoomType.setId(10);

        suiteRoomType = HotelRoomType.builder()
                .hotel(hotel)
                .roomType(suiteType)
                .basePrice(BigDecimal.valueOf(1800000))
                .build();
        suiteRoomType.setId(20);

        room101 = RoomInstance.builder()
                .hotel(hotel)
                .hotelRoomType(standardRoomType)
                .roomNumber("101")
                .currentStatus("OCCUPIED")
                .build();
        room101.setId(101);

        room102 = RoomInstance.builder()
                .hotel(hotel)
                .hotelRoomType(standardRoomType)
                .roomNumber("102")
                .currentStatus("READY")
                .build();
        room102.setId(102);

        room201Suite = RoomInstance.builder()
                .hotel(hotel)
                .hotelRoomType(suiteRoomType)
                .roomNumber("201")
                .currentStatus("READY")
                .build();
        room201Suite.setId(201);

        booking = Booking.builder()
                .hotel(hotel)
                .bookingNumber("BK-2026-TEST")
                .status(BookingStatus.CONFIRMED)
                .subtotalAmount(BigDecimal.valueOf(3000000))
                .serviceFeeRate(BigDecimal.valueOf(5))
                .serviceFeeAmount(BigDecimal.valueOf(150000))
                .totalVatAmount(BigDecimal.valueOf(315000))
                .totalAmount(BigDecimal.valueOf(3465000))
                .build();
        booking.setId(1L);

        bookingDetail = BookingDetail.builder()
                .booking(booking)
                .hotelRoomType(standardRoomType)
                .roomTypeName("Standard Room")
                .quantity((short) 1)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(3))
                .build();
        bookingDetail.setId(10L);

        stayGuest = StayGuest.builder()
                .room(room101)
                .roomNumber("101")
                .fullName("Nguyen Van A")
                .build();
        stayGuest.setId(1001L);

        List<StayGuest> guests = new ArrayList<>();
        guests.add(stayGuest);

        bookingRoom = BookingRoom.builder()
                .bookingDetail(bookingDetail)
                .roomInstance(room101)
                .status(BookingRoomStatus.CHECKED_IN)
                .stayGuests(guests)
                .build();
        bookingRoom.setId(500L);
    }

    @Test
    @DisplayName("Kịch bản 1: Đổi cùng hạng phòng (Same room type) thành công khi khách đang in-house")
    void swapRoom_sameRoomType_success() {
        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(102)
                .effectiveDate(LocalDate.now())
                .reason("Điều hòa phòng 101 bị ồn, khách yêu cầu đổi sang phòng 102")
                .isComplimentary(false)
                .build();

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(102)).thenReturn(room102);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(102), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        RoomSwapRecord savedRecord = RoomSwapRecord.builder()
                .id(99L)
                .bookingRoom(bookingRoom)
                .fromRoomInstance(room101)
                .toRoomInstance(room102)
                .fromHotelRoomType(standardRoomType)
                .toHotelRoomType(standardRoomType)
                .swapDate(LocalDate.now())
                .effectiveDate(LocalDate.now())
                .reason(request.getReason())
                .isUpgrade(false)
                .isComplimentary(false)
                .priceDifference(BigDecimal.ZERO)
                .performedBy("RECEPTIONIST")
                .build();
        when(roomSwapRecordService.save(any(RoomSwapRecord.class))).thenReturn(savedRecord);

        RoomSwapResponse response = bookingService.swapRoom(500L, request);

        assertThat(response).isNotNull();
        assertThat(response.getSwapRecordId()).isEqualTo(99L);
        assertThat(response.getPreviousRoomNumber()).isEqualTo("101");
        assertThat(response.getNewRoomNumber()).isEqualTo("102");
        assertThat(response.getIsUpgrade()).isFalse();
        assertThat(response.getIsComplimentary()).isFalse();
        assertThat(response.getPriceDifference()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify phòng cũ chuyển DIRTY, phòng mới chuyển OCCUPIED
        verify(roomInstanceService).updateRoomStatusEntity(101, "DIRTY");
        verify(roomInstanceService).updateRoomStatusEntity(102, "OCCUPIED");

        // Verify đổi slot phòng
        verify(roomSlotService).swapSlots(eq(101), eq(room102), eq(LocalDate.now()), eq(bookingDetail.getCheckOutDate()), eq(500L), eq(true));

        // Verify StayGuest được cập nhật số phòng mới
        assertThat(stayGuest.getRoomNumber()).isEqualTo("102");
        assertThat(stayGuest.getRoom()).isEqualTo(room102);
        verify(stayGuestService).syncLodgingQueueForBookingRoom(500L);

        // Verify không tạo charge vì cùng hạng
        verifyNoInteractions(bookingChargeService);

        // Verify audit log
        verify(auditLogService).recordLog(isNull(), eq("BOOKING_ROOM_SWAP"), eq("BookingRoom"), eq("500"), eq("101"), contains("102"), isNull(), anyString());
    }

    @Test
    @DisplayName("Kịch bản 2: Nâng hạng phòng có trả phí (Paid Upgrade) thành công")
    void swapRoom_paidUpgrade_success() {
        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(201)
                .effectiveDate(LocalDate.now().plusDays(1))
                .reason("Khách yêu cầu nâng hạng phòng lên Suite")
                .isComplimentary(false)
                .build();

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(201)).thenReturn(room201Suite);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(201), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Chênh lệch đơn giá = 1.800.000 - 1.000.000 = 800.000 VNĐ.
        // Còn 2 đêm (từ plusDays(1) đến checkOutDate plusDays(3)) -> 1.600.000 VNĐ subtotal.
        BookingCharge mockCharge = BookingCharge.builder()
                .subtotal(BigDecimal.valueOf(1600000))
                .serviceFeeAmount(BigDecimal.valueOf(80000))
                .vatAmount(BigDecimal.valueOf(168000))
                .totalAmount(BigDecimal.valueOf(1848000))
                .build();
        when(bookingChargeService.createCharge(eq(bookingRoom), any(BookingChargeCreateRequest.class))).thenReturn(mockCharge);

        RoomSwapRecord savedRecord = RoomSwapRecord.builder()
                .id(100L)
                .bookingRoom(bookingRoom)
                .fromRoomInstance(room101)
                .toRoomInstance(room201Suite)
                .fromHotelRoomType(standardRoomType)
                .toHotelRoomType(suiteRoomType)
                .swapDate(LocalDate.now())
                .effectiveDate(request.getEffectiveDate())
                .reason(request.getReason())
                .isUpgrade(true)
                .isComplimentary(false)
                .priceDifference(BigDecimal.valueOf(1600000))
                .performedBy("RECEPTIONIST")
                .build();
        when(roomSwapRecordService.save(any(RoomSwapRecord.class))).thenReturn(savedRecord);

        RoomSwapResponse response = bookingService.swapRoom(500L, request);

        assertThat(response).isNotNull();
        assertThat(response.getSwapRecordId()).isEqualTo(100L);
        assertThat(response.getPreviousRoomNumber()).isEqualTo("101");
        assertThat(response.getNewRoomNumber()).isEqualTo("201");
        assertThat(response.getIsUpgrade()).isTrue();
        assertThat(response.getIsComplimentary()).isFalse();
        assertThat(response.getPriceDifference()).isEqualByComparingTo(BigDecimal.valueOf(1600000));

        // Verify charge phụ thu được tạo
        ArgumentCaptor<BookingChargeCreateRequest> chargeCaptor = ArgumentCaptor.forClass(BookingChargeCreateRequest.class);
        verify(bookingChargeService).createCharge(eq(bookingRoom), chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1600000));

        // Verify booking tổng tiền được cộng dồn
        verify(bookingRepository).save(booking);
        assertThat(booking.getSubtotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(4600000)); // 3M + 1.6M
        assertThat(booking.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5313000)); // 3.465M + 1.848M
    }

    @Test
    @DisplayName("Kịch bản 3: Nâng hạng phòng miễn phí (Complimentary Upgrade) cho khách VIP")
    void swapRoom_complimentaryUpgrade_success() {
        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(201)
                .effectiveDate(LocalDate.now())
                .reason("Khách hàng VIP Platinum - khách sạn tặng nâng hạng Suite miễn phí")
                .isComplimentary(true)
                .build();

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(201)).thenReturn(room201Suite);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(201), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        RoomSwapRecord savedRecord = RoomSwapRecord.builder()
                .id(101L)
                .bookingRoom(bookingRoom)
                .fromRoomInstance(room101)
                .toRoomInstance(room201Suite)
                .fromHotelRoomType(standardRoomType)
                .toHotelRoomType(suiteRoomType)
                .swapDate(LocalDate.now())
                .effectiveDate(request.getEffectiveDate())
                .reason(request.getReason())
                .isUpgrade(true)
                .isComplimentary(true)
                .priceDifference(BigDecimal.ZERO)
                .performedBy("RECEPTIONIST")
                .build();
        when(roomSwapRecordService.save(any(RoomSwapRecord.class))).thenReturn(savedRecord);

        RoomSwapResponse response = bookingService.swapRoom(500L, request);

        assertThat(response).isNotNull();
        assertThat(response.getIsUpgrade()).isTrue();
        assertThat(response.getIsComplimentary()).isTrue();
        assertThat(response.getPriceDifference()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify KHÔNG tạo charge phụ thu khi complimentary
        verifyNoInteractions(bookingChargeService);
        // Verify booking không đổi tổng tiền
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Kịch bản 4: Báo lỗi khi phòng đích đang bẩn (DIRTY) hoặc đang bảo trì (MAINTENANCE)")
    void swapRoom_targetRoomDirty_throwsException() {
        room102.setCurrentStatus("DIRTY");

        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(102)
                .effectiveDate(LocalDate.now())
                .reason("Đổi phòng")
                .build();

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(102)).thenReturn(room102);

        assertThatThrownBy(() -> bookingService.swapRoom(500L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_READY_FOR_CHECKIN);

        verifyNoInteractions(roomSlotService);
        verify(roomSwapRecordService, never()).save(any());
    }

    @Test
    @DisplayName("Kịch bản 5: Báo lỗi khi phòng đích đã có booking khác trùng lịch")
    void swapRoom_targetRoomOverlapping_throwsException() {
        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(102)
                .effectiveDate(LocalDate.now())
                .reason("Đổi phòng")
                .build();

        BookingRoom anotherBookingRoom = BookingRoom.builder().build();
        anotherBookingRoom.setId(999L); // Khác với bookingRoom 500L

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(102)).thenReturn(room102);
        when(bookingRoomService.findOverlappingAssignedRooms(eq(102), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(anotherBookingRoom));

        assertThatThrownBy(() -> bookingService.swapRoom(500L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_ALREADY_ASSIGNED);

        verifyNoInteractions(roomSlotService);
        verify(roomSwapRecordService, never()).save(any());
    }

    @Test
    @DisplayName("Kịch bản 6: Báo lỗi khi lượt đặt phòng đã trả phòng (CHECKED_OUT)")
    void swapRoom_checkedOutBookingRoom_throwsException() {
        bookingRoom.setStatus(BookingRoomStatus.CHECKED_OUT);

        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(102)
                .effectiveDate(LocalDate.now())
                .reason("Đổi phòng sau khi đã check-out")
                .build();

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);

        assertThatThrownBy(() -> bookingService.swapRoom(500L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);

        verifyNoInteractions(roomInstanceService);
        verifyNoInteractions(roomSlotService);
    }

    @Test
    @DisplayName("Kịch bản 7: Báo lỗi khi chọn phòng đích trùng với phòng hiện tại")
    void swapRoom_sameRoomTarget_throwsException() {
        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(101) // Trùng với room101
                .effectiveDate(LocalDate.now())
                .reason("Đổi vào chính phòng này")
                .build();

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);

        assertThatThrownBy(() -> bookingService.swapRoom(500L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("Kịch bản 8: Báo lỗi khi ngày hiệu lực sau ngày check-out")
    void swapRoom_invalidEffectiveDate_throwsException() {
        RoomSwapRequest request = RoomSwapRequest.builder()
                .targetRoomInstanceId(102)
                .effectiveDate(bookingDetail.getCheckOutDate().plusDays(1)) // Sau check-out
                .reason("Đổi phòng quá hạn")
                .build();

        when(bookingRoomService.getEntityById(500L)).thenReturn(bookingRoom);
        when(roomInstanceService.getEntityById(102)).thenReturn(room102);

        assertThatThrownBy(() -> bookingService.swapRoom(500L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }
}
