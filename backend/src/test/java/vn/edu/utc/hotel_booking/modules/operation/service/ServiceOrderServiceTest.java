package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.*;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;
import vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.ServiceOrderServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.service.VatRuleService;

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
class ServiceOrderServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock BookingLookupService bookingLookupService;
    @Mock StayGuestService stayGuestService;
    @Mock HotelService hotelService;
    @Mock MenuService menuService;
    @Mock VatRuleService vatRuleService;
    @Mock OperationMapper operationMapper;

    @InjectMocks
    ServiceOrderServiceImpl serviceOrderService;

    private Hotel testHotel;
    private Booking testBooking;
    private BookingRoom testBookingRoom;
    private StayGuest testStayGuest;
    private Menu testMenu;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        testHotel.setId((short) 1);

        testBooking = Booking.builder()
                .id(100L)
                .hotel(testHotel)
                .bookingNumber("BK12345678")
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(10L)
                .booking(testBooking)
                .build();

        testBookingRoom = BookingRoom.builder()
                .id(20L)
                .bookingDetail(detail)
                .status(BookingRoomStatus.CHECKED_IN)
                .build();

        testStayGuest = StayGuest.builder()
                .id(30L)
                .bookingRoom(testBookingRoom)
                .hotel(testHotel)
                .fullName("Nguyễn Văn A")
                .build();

        TaxCategory taxCat = TaxCategory.builder()
                .categoryName("Đồ ăn nhẹ")
                .build();
        taxCat.setId(1);

        testMenu = Menu.builder()
                .id(10)
                .hotel(testHotel)
                .menuType(MenuType.PRODUCT)
                .name("Snack khoai tây Lays")
                .basePrice(BigDecimal.valueOf(25000))
                .stockQuantity(20)
                .taxCategory(taxCat)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Đặt món ăn cấp BOOKING thành công: trừ tồn kho trên menu, tính thuế và lưu đơn")
    void createOrder_BookingLevel_Success() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .referenceType(ServiceOrderReferenceType.BOOKING)
                .referenceId(100L)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(2)
                                .build()
                ))
                .build();

        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(testBooking);
        when(menuService.getEntityById(10)).thenReturn(testMenu);
        when(vatRuleService.getActiveVatPercent(any(), any())).thenReturn(BigDecimal.ZERO);

        ServiceOrder savedOrder = ServiceOrder.builder()
                .id(1L)
                .orderNumber("SO-12345")
                .hotel(testHotel)
                .referenceType(ServiceOrderReferenceType.BOOKING)
                .referenceId(100L)
                .subTotal(BigDecimal.valueOf(50000))
                .totalAmount(BigDecimal.valueOf(52500))
                .build();

        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(savedOrder);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(1L)
                .orderNumber("SO-12345")
                .referenceType(ServiceOrderReferenceType.BOOKING)
                .referenceId(100L)
                .totalAmount(BigDecimal.valueOf(52500))
                .build());

        ServiceOrderResponse response = serviceOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getReferenceType()).isEqualTo(ServiceOrderReferenceType.BOOKING);

        // Kiểm tra ủy quyền trừ tồn kho qua MenuService
        verify(menuService, times(1)).deductStock(10, 2);
        verify(serviceOrderRepository, times(1)).save(any(ServiceOrder.class));
    }

    @Test
    @DisplayName("Đặt món ăn cấp BOOKING_ROOM (theo phòng) thành công")
    void createOrder_BookingRoomLevel_Success() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .referenceType(ServiceOrderReferenceType.BOOKING_ROOM)
                .referenceId(20L)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(bookingLookupService.getBookingRoomEntityById(20L)).thenReturn(testBookingRoom);
        when(menuService.getEntityById(10)).thenReturn(testMenu);
        when(vatRuleService.getActiveVatPercent(any(), any())).thenReturn(BigDecimal.ZERO);

        ServiceOrder savedOrder = ServiceOrder.builder()
                .id(2L)
                .orderNumber("SO-ROOM-001")
                .hotel(testHotel)
                .referenceType(ServiceOrderReferenceType.BOOKING_ROOM)
                .referenceId(20L)
                .build();

        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(savedOrder);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(2L)
                .referenceType(ServiceOrderReferenceType.BOOKING_ROOM)
                .referenceId(20L)
                .build());

        ServiceOrderResponse response = serviceOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getReferenceType()).isEqualTo(ServiceOrderReferenceType.BOOKING_ROOM);
        verify(menuService, times(1)).deductStock(10, 1);
    }

    @Test
    @DisplayName("Đặt dịch vụ cấp STAY_GUEST (cá nhân khách) thành công")
    void createOrder_StayGuestLevel_Success() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .referenceType(ServiceOrderReferenceType.STAY_GUEST)
                .referenceId(30L)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(stayGuestService.getEntityById(30L)).thenReturn(testStayGuest);
        when(menuService.getEntityById(10)).thenReturn(testMenu);
        when(vatRuleService.getActiveVatPercent(any(), any())).thenReturn(BigDecimal.ZERO);

        ServiceOrder savedOrder = ServiceOrder.builder()
                .id(3L)
                .orderNumber("SO-GUEST-001")
                .hotel(testHotel)
                .referenceType(ServiceOrderReferenceType.STAY_GUEST)
                .referenceId(30L)
                .build();

        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(savedOrder);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(3L)
                .referenceType(ServiceOrderReferenceType.STAY_GUEST)
                .referenceId(30L)
                .build());

        ServiceOrderResponse response = serviceOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getReferenceType()).isEqualTo(ServiceOrderReferenceType.STAY_GUEST);
        verify(menuService, times(1)).deductStock(10, 1);
    }

    @Test
    @DisplayName("Đặt món cho khách vãng lai WALK_IN thành công với referenceId = NULL")
    void createOrder_WalkIn_Success() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .referenceType(ServiceOrderReferenceType.WALK_IN)
                .hotelId((short) 1)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(2)
                                .build()
                ))
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(menuService.getEntityById(10)).thenReturn(testMenu);
        when(vatRuleService.getActiveVatPercent(any(), any())).thenReturn(BigDecimal.ZERO);

        ServiceOrder savedOrder = ServiceOrder.builder()
                .id(4L)
                .orderNumber("SO-WALKIN-001")
                .hotel(testHotel)
                .referenceType(ServiceOrderReferenceType.WALK_IN)
                .referenceId(null) // NULL chuẩn theo nghiệp vụ
                .build();

        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(savedOrder);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(4L)
                .referenceType(ServiceOrderReferenceType.WALK_IN)
                .referenceId(null)
                .build());

        ServiceOrderResponse response = serviceOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getReferenceType()).isEqualTo(ServiceOrderReferenceType.WALK_IN);
        assertThat(response.getReferenceId()).isNull();
        verify(menuService, times(1)).deductStock(10, 2);
    }

    @Test
    @DisplayName("Báo lỗi OUT_OF_STOCK khi số lượng đặt vượt quá tồn kho khả dụng")
    void createOrder_OutOfStock_ThrowsException() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .referenceType(ServiceOrderReferenceType.BOOKING)
                .referenceId(100L)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(50) // Vượt quá tồn kho 20
                                .build()
                ))
                .build();

        when(bookingLookupService.getBookingEntityById(100L)).thenReturn(testBooking);
        when(menuService.getEntityById(10)).thenReturn(testMenu);
        doThrow(new AppException(ErrorCode.OUT_OF_STOCK, "Sản phẩm Snack khoai tây Lays không đủ tồn kho"))
                .when(menuService).deductStock(10, 50);

        assertThatThrownBy(() -> serviceOrderService.createOrder(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK));
    }

    @Test
    @DisplayName("Hủy đơn dịch vụ hoàn trả tồn kho sản phẩm trực tiếp trên Menu")
    void updateStatus_Cancelled_RestoresStock() {
        ServiceOrderDetail detail = ServiceOrderDetail.builder()
                .menu(testMenu)
                .quantity(3)
                .build();

        ServiceOrder order = ServiceOrder.builder()
                .id(1L)
                .status(ServiceOrderStatus.PENDING)
                .details(new ArrayList<>(List.of(detail)))
                .build();

        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(order);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(1L)
                .status(ServiceOrderStatus.CANCELLED)
                .build());

        ServiceOrderResponse response = serviceOrderService.updateStatus(1L, ServiceOrderStatus.CANCELLED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
        // Tồn kho được hoàn trả qua MenuService
        verify(menuService, times(1)).restoreStock(10, 3);
    }

    @Test
    @DisplayName("getOrdersByBookingId trả về danh sách các đơn không bị CANCELLED khi không có phòng phụ thuộc")
    void getOrdersByBookingId_Success() {
        ServiceOrder activeOrder = ServiceOrder.builder().id(1L).status(ServiceOrderStatus.PENDING).build();
        ServiceOrder cancelledOrder = ServiceOrder.builder().id(2L).status(ServiceOrderStatus.CANCELLED).build();

        when(bookingLookupService.getBookingRoomsByBookingId(100L)).thenReturn(List.of());
        when(serviceOrderRepository.findByBookingIdAndIsDeletedFalse(100L))
                .thenReturn(List.of(activeOrder, cancelledOrder));
        when(operationMapper.toResponse(activeOrder)).thenReturn(ServiceOrderResponse.builder().id(1L).build());

        List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(100L);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getOrdersByBookingId lấy cả đơn đặt phòng cấp BOOKING và cấp BOOKING_ROOM qua hierarchy")
    void getOrdersByBookingId_WithRooms_Success() {
        ServiceOrder bookingOrder = ServiceOrder.builder().id(1L).status(ServiceOrderStatus.PENDING).build();
        ServiceOrder roomOrder = ServiceOrder.builder().id(2L).status(ServiceOrderStatus.COMPLETED).build();

        when(bookingLookupService.getBookingRoomsByBookingId(100L)).thenReturn(List.of(testBookingRoom));
        when(serviceOrderRepository.findAllOrdersForBookingAndRooms(100L, List.of(20L)))
                .thenReturn(List.of(bookingOrder, roomOrder));
        when(operationMapper.toResponse(bookingOrder)).thenReturn(ServiceOrderResponse.builder().id(1L).build());
        when(operationMapper.toResponse(roomOrder)).thenReturn(ServiceOrderResponse.builder().id(2L).build());

        List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(100L);

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getId()).isEqualTo(1L);
        assertThat(orders.get(1).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getDailyServiceRevenue tính tổng doanh thu của các đơn dịch vụ hợp lệ trong ngày")
    void getDailyServiceRevenue_Success() {
        LocalDate date = LocalDate.of(2026, 6, 1);
        ServiceOrder order1 = ServiceOrder.builder().totalAmount(BigDecimal.valueOf(300000)).build();
        ServiceOrder order2 = ServiceOrder.builder().totalAmount(BigDecimal.valueOf(700000)).build();

        when(serviceOrderRepository.findActiveOrdersForHotelAndDate(eq((short) 1), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(order1, order2));

        BigDecimal total = serviceOrderService.getDailyServiceRevenue((short) 1, date);

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        verify(serviceOrderRepository).findActiveOrdersForHotelAndDate(eq((short) 1), any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("getDailyServiceRevenue trả về ZERO khi tham số hotelId hoặc date bị null")
    void getDailyServiceRevenue_NullArgs_ReturnsZero() {
        assertThat(serviceOrderService.getDailyServiceRevenue(null, LocalDate.now())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(serviceOrderService.getDailyServiceRevenue((short) 1, null)).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(serviceOrderRepository);
    }
}
