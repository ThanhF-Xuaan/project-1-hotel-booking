package vn.edu.utc.hotel_booking.modules.operation.service;

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
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingChargeRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.*;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.CatalogItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderDetailRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.ServiceOrderServiceImpl;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;

import java.math.BigDecimal;
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
    @Mock ServiceOrderDetailRepository serviceOrderDetailRepository;
    @Mock BookingRepository bookingRepository;
    @Mock RoomInstanceRepository roomInstanceRepository;
    @Mock BookingRoomRepository bookingRoomRepository;
    @Mock BookingChargeRepository bookingChargeRepository;
    @Mock MenuRepository menuRepository;
    @Mock CatalogItemRepository catalogItemRepository;
    @Mock VatRuleRepository vatRuleRepository;
    @Mock OperationMapper operationMapper;

    @InjectMocks
    ServiceOrderServiceImpl serviceOrderService;

    private Booking testBooking;
    private RoomInstance testRoom;
    private BookingRoom testBookingRoom;
    private Menu testMenu;
    private CatalogItem testCatalogItem;

    @BeforeEach
    void setUp() {
        testBooking = Booking.builder()
                .id(100L)
                .bookingNumber("BK12345678")
                .build();

        testRoom = RoomInstance.builder()
                .id(50)
                .roomNumber("P.302")
                .build();

        BookingDetail testDetail = BookingDetail.builder()
                .id(15L)
                .booking(testBooking)
                .build();

        testBookingRoom = BookingRoom.builder()
                .id(20L)
                .bookingDetail(testDetail)
                .roomInstance(testRoom)
                .status(BookingRoomStatus.CHECKED_IN)
                .build();

        TaxCategory taxCat = TaxCategory.builder()
                .categoryName("Đồ ăn nhẹ")
                .build();
        taxCat.setId(1);

        testMenu = Menu.builder()
                .id(10)
                .menuType(MenuType.PRODUCT)
                .name("Snack khoai tây Lays")
                .basePrice(BigDecimal.valueOf(25000))
                .taxCategory(taxCat)
                .status("ACTIVE")
                .build();

        testCatalogItem = CatalogItem.builder()
                .menu(testMenu)
                .stockQuantity(20)
                .build();
    }

    @Test
    @DisplayName("Đặt món ăn/dịch vụ thành công: trừ tồn kho, tính thuế & đẩy vào BookingCharge")
    void createOrder_Success() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .bookingId(100L)
                .roomInstanceId(50)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(2)
                                .build()
                ))
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(roomInstanceRepository.findByIdAndIsDeletedFalse(50)).thenReturn(Optional.of(testRoom));
        when(bookingRoomRepository.findByBookingIdAndRoomInstanceId(100L, 50)).thenReturn(Optional.of(testBookingRoom));
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testMenu));
        when(catalogItemRepository.findById(10)).thenReturn(Optional.of(testCatalogItem));
        when(vatRuleRepository.findActiveVatRule(any(), any())).thenReturn(Optional.empty());

        ServiceOrder savedOrder = ServiceOrder.builder()
                .id(1L)
                .orderNumber("SO-12345")
                .booking(testBooking)
                .roomInstance(testRoom)
                .subTotal(BigDecimal.valueOf(50000))
                .totalAmount(BigDecimal.valueOf(52500))
                .build();

        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(savedOrder);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(1L)
                .orderNumber("SO-12345")
                .totalAmount(BigDecimal.valueOf(52500))
                .build());

        ServiceOrderResponse response = serviceOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        // Kiểm tra tồn kho bị trừ từ 20 xuống 18
        assertThat(testCatalogItem.getStockQuantity()).isEqualTo(18);
        verify(catalogItemRepository, times(1)).save(testCatalogItem);

        // Kiểm tra tự động đẩy phụ phí vào BookingCharge
        ArgumentCaptor<BookingCharge> chargeCaptor = ArgumentCaptor.forClass(BookingCharge.class);
        verify(bookingChargeRepository, times(1)).save(chargeCaptor.capture());
        BookingCharge capturedCharge = chargeCaptor.getValue();
        assertThat(capturedCharge.getChargeType()).isEqualTo(BookingChargeType.SERVICE);
        assertThat(capturedCharge.getItemName()).contains("Snack khoai tây Lays");
        assertThat(capturedCharge.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);

        verify(serviceOrderRepository, times(1)).save(any(ServiceOrder.class));
    }

    @Test
    @DisplayName("Đặt món chỉ với ID phòng vật lý (không cần bookingId): tự động tìm đơn đặt phòng đang lưu trú")
    void createOrder_WithoutBookingId_AutoLookupSuccess() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .roomInstanceId(50)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(roomInstanceRepository.findByIdAndIsDeletedFalse(50)).thenReturn(Optional.of(testRoom));
        when(bookingRoomRepository.findByRoomInstanceIdAndStatusIn(eq(50), any())).thenReturn(List.of(testBookingRoom));
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testMenu));
        when(catalogItemRepository.findById(10)).thenReturn(Optional.of(testCatalogItem));
        when(vatRuleRepository.findActiveVatRule(any(), any())).thenReturn(Optional.empty());

        ServiceOrder savedOrder = ServiceOrder.builder()
                .id(2L)
                .orderNumber("SO-99999")
                .booking(testBooking)
                .roomInstance(testRoom)
                .status(ServiceOrderStatus.PENDING)
                .details(new ArrayList<>())
                .build();

        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(savedOrder);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(2L)
                .orderNumber("SO-99999")
                .bookingId(100L)
                .roomInstanceId(50)
                .status(ServiceOrderStatus.PENDING)
                .build());

        ServiceOrderResponse response = serviceOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getBookingId()).isEqualTo(100L);
        verify(bookingChargeRepository, times(1)).save(any(BookingCharge.class));
    }

    @Test
    @DisplayName("Đặt món chỉ với ID phòng: fallback tìm đặt phòng gần nhất khi không có phòng CHECKED_IN")
    void createOrder_WithoutBookingId_FallbackToRecentBooking() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .roomInstanceId(50)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(roomInstanceRepository.findByIdAndIsDeletedFalse(50)).thenReturn(Optional.of(testRoom));
        when(bookingRoomRepository.findByRoomInstanceIdAndStatusIn(eq(50), any())).thenReturn(List.of());
        when(bookingRoomRepository.findActiveOrRecentByRoomInstanceId(50)).thenReturn(List.of(testBookingRoom));
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testMenu));
        when(catalogItemRepository.findById(10)).thenReturn(Optional.of(testCatalogItem));
        when(vatRuleRepository.findActiveVatRule(any(), any())).thenReturn(Optional.empty());

        ServiceOrder savedOrder = ServiceOrder.builder()
                .id(3L)
                .orderNumber("SO-88888")
                .booking(testBooking)
                .roomInstance(testRoom)
                .status(ServiceOrderStatus.PENDING)
                .details(new ArrayList<>())
                .build();

        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(savedOrder);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(3L)
                .orderNumber("SO-88888")
                .bookingId(100L)
                .roomInstanceId(50)
                .status(ServiceOrderStatus.PENDING)
                .build());

        ServiceOrderResponse response = serviceOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(3L);
        verify(bookingRoomRepository, times(1)).findActiveOrRecentByRoomInstanceId(50);
    }

    @Test
    @DisplayName("Báo lỗi OUT_OF_STOCK khi số lượng đặt vượt quá tồn kho khả dụng")
    void createOrder_OutOfStock_ThrowsException() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .bookingId(100L)
                .roomInstanceId(50)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder()
                                .menuId(10)
                                .quantity(50) // Vượt quá tồn kho 20
                                .build()
                ))
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(roomInstanceRepository.findByIdAndIsDeletedFalse(50)).thenReturn(Optional.of(testRoom));
        when(bookingRoomRepository.findByBookingIdAndRoomInstanceId(100L, 50)).thenReturn(Optional.of(testBookingRoom));
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testMenu));
        when(catalogItemRepository.findById(10)).thenReturn(Optional.of(testCatalogItem));

        assertThatThrownBy(() -> serviceOrderService.createOrder(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK));
    }

    @Test
    @DisplayName("Báo lỗi ROOM_NOT_OCCUPIED khi phòng không thuộc về booking")
    void createOrder_RoomNotOccupied_ThrowsException() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .bookingId(100L)
                .roomInstanceId(50)
                .items(List.of(
                        ServiceOrderCreateRequest.OrderItemRequest.builder().menuId(10).quantity(1).build()
                ))
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(roomInstanceRepository.findByIdAndIsDeletedFalse(50)).thenReturn(Optional.of(testRoom));
        when(bookingRoomRepository.findByBookingIdAndRoomInstanceId(100L, 50)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceOrderService.createOrder(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_OCCUPIED));
    }

    @Test
    @DisplayName("Hủy đơn dịch vụ: hoàn trả tồn kho sản phẩm và tạo bút toán bù âm trên Folio")
    void updateStatus_Cancelled_RestoresStockAndCreatesCompensatingCharge() {
        ServiceOrderDetail detail = ServiceOrderDetail.builder()
                .menu(testMenu)
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(25000))
                .subtotal(BigDecimal.valueOf(75000))
                .serviceFeeRate(BigDecimal.valueOf(5))
                .serviceFeeAmount(BigDecimal.valueOf(3750))
                .vatRate(BigDecimal.valueOf(8))
                .vatAmount(BigDecimal.valueOf(6300))
                .totalAmount(BigDecimal.valueOf(85050))
                .itemName("Snack khoai tây Lays")
                .build();

        ServiceOrder order = ServiceOrder.builder()
                .id(1L)
                .orderNumber("SO-12345")
                .booking(testBooking)
                .roomInstance(testRoom)
                .status(ServiceOrderStatus.PENDING)
                .details(new ArrayList<>(List.of(detail)))
                .build();

        testCatalogItem.setStockQuantity(10);

        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(catalogItemRepository.findById(10)).thenReturn(Optional.of(testCatalogItem));
        when(bookingRoomRepository.findByBookingIdAndRoomInstanceId(100L, 50)).thenReturn(Optional.of(testBookingRoom));
        when(serviceOrderRepository.save(any(ServiceOrder.class))).thenReturn(order);
        when(operationMapper.toResponse(any(ServiceOrder.class))).thenReturn(ServiceOrderResponse.builder()
                .id(1L)
                .status(ServiceOrderStatus.CANCELLED)
                .build());

        ServiceOrderResponse response = serviceOrderService.updateStatus(1L, ServiceOrderStatus.CANCELLED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);

        // 1. Tồn kho được hoàn trả từ 10 thành 13
        assertThat(testCatalogItem.getStockQuantity()).isEqualTo(13);
        verify(catalogItemRepository, times(1)).save(testCatalogItem);

        // 2. Tạo bút toán bù (compensating charge) âm trên Folio
        ArgumentCaptor<BookingCharge> refundCaptor = ArgumentCaptor.forClass(BookingCharge.class);
        verify(bookingChargeRepository, times(1)).save(refundCaptor.capture());
        BookingCharge refundCharge = refundCaptor.getValue();
        assertThat(refundCharge.getChargeType()).isEqualTo(BookingChargeType.SERVICE);
        assertThat(refundCharge.getItemName()).isEqualTo("Hủy SO-12345: Snack khoai tây Lays");
        assertThat(refundCharge.getTotalAmount()).isEqualTo(BigDecimal.valueOf(-85050));
        assertThat(refundCharge.getSubtotal()).isEqualTo(BigDecimal.valueOf(-75000));
    }

    @Test
    @DisplayName("Báo lỗi INVALID_SERVICE_ORDER_STATUS khi cố gắng hủy đơn đã bị hủy trước đó")
    void updateStatus_CancelledTwice_ThrowsException() {
        ServiceOrder order = ServiceOrder.builder()
                .id(1L)
                .orderNumber("SO-12345")
                .status(ServiceOrderStatus.CANCELLED)
                .build();

        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> serviceOrderService.updateStatus(1L, ServiceOrderStatus.CANCELLED))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_SERVICE_ORDER_STATUS));
    }
}
