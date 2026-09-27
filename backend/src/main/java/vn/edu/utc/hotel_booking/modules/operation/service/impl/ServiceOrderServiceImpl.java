package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingChargeRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.*;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.CatalogItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderDetailRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class ServiceOrderServiceImpl implements ServiceOrderService {

    ServiceOrderRepository serviceOrderRepository;
    ServiceOrderDetailRepository serviceOrderDetailRepository;
    BookingRepository bookingRepository;
    RoomInstanceRepository roomInstanceRepository;
    BookingRoomRepository bookingRoomRepository;
    BookingChargeRepository bookingChargeRepository;
    MenuRepository menuRepository;
    CatalogItemRepository catalogItemRepository;
    VatRuleRepository vatRuleRepository;
    OperationMapper operationMapper;

    @Override
    @Transactional
    public ServiceOrderResponse createOrder(ServiceOrderCreateRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng: " + request.getBookingId()));

        RoomInstance roomInstance = roomInstanceRepository.findByIdAndIsDeletedFalse(request.getRoomInstanceId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Không tìm thấy phòng: " + request.getRoomInstanceId()));

        BookingRoom bookingRoom = bookingRoomRepository.findByBookingIdAndRoomInstanceId(booking.getId(), roomInstance.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_OCCUPIED, "Phòng này chưa được gán cho đơn đặt phòng: " + booking.getBookingNumber()));

        if (bookingRoom.getStatus() == BookingRoomStatus.CANCELLED || bookingRoom.getStatus() == BookingRoomStatus.CHECKED_OUT) {
            throw new AppException(ErrorCode.ROOM_NOT_OCCUPIED, "Phòng này không ở trạng thái lưu trú khả dụng");
        }

        String orderNumber = "SO-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 900 + 100);

        BigDecimal standardServiceFeeRate = new BigDecimal("5.00"); // 5% Hotel Service Charge

        ServiceOrder order = ServiceOrder.builder()
                .booking(booking)
                .roomInstance(roomInstance)
                .orderNumber(orderNumber)
                .serviceFeeRate(standardServiceFeeRate)
                .status(ServiceOrderStatus.PENDING)
                .issuedAt(OffsetDateTime.now())
                .build();

        List<ServiceOrderDetail> details = new ArrayList<>();
        BigDecimal orderSubTotal = BigDecimal.ZERO;
        BigDecimal orderTotalServiceFee = BigDecimal.ZERO;
        BigDecimal orderTotalVat = BigDecimal.ZERO;
        BigDecimal orderGrandTotal = BigDecimal.ZERO;

        for (ServiceOrderCreateRequest.OrderItemRequest itemReq : request.getItems()) {
            Menu menu = menuRepository.findByIdAndIsDeletedFalse(itemReq.getMenuId())
                    .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND, "Không tìm thấy món/dịch vụ ID: " + itemReq.getMenuId()));

            if (!"ACTIVE".equalsIgnoreCase(menu.getStatus())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Món/dịch vụ tạm ngưng phục vụ: " + menu.getName());
            }

            // Trừ tồn kho nếu là sản phẩm vật lý (CatalogItem / Product)
            if (menu.getMenuType() == MenuType.PRODUCT) {
                CatalogItem catalogItem = catalogItemRepository.findById(menu.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK, "Không tìm thấy kho cho sản phẩm: " + menu.getName()));

                if (catalogItem.getStockQuantity() < itemReq.getQuantity()) {
                    throw new AppException(ErrorCode.OUT_OF_STOCK, "Sản phẩm " + menu.getName() + " không đủ tồn kho (còn " + catalogItem.getStockQuantity() + ")");
                }

                catalogItem.setStockQuantity(catalogItem.getStockQuantity() - itemReq.getQuantity());
                catalogItemRepository.save(catalogItem);
            }

            BigDecimal unitPrice = menu.getBasePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal lineServiceFee = subtotal.multiply(standardServiceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal taxableAmount = subtotal.add(lineServiceFee);

            BigDecimal vatRate = BigDecimal.ZERO;
            if (menu.getTaxCategory() != null) {
                vatRate = vatRuleRepository.findActiveVatRule(menu.getTaxCategory().getId(), LocalDate.now())
                        .map(VatRule::getVatPercent)
                        .orElse(BigDecimal.ZERO);
            }
            BigDecimal lineVat = taxableAmount.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = taxableAmount.add(lineVat);

            ServiceOrderDetail detail = ServiceOrderDetail.builder()
                    .serviceOrder(order)
                    .menu(menu)
                    .itemType(menu.getMenuType().name())
                    .itemName(menu.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .serviceFeeRate(standardServiceFeeRate)
                    .serviceFeeAmount(lineServiceFee)
                    .vatRate(vatRate)
                    .vatAmount(lineVat)
                    .totalAmount(lineTotal)
                    .build();

            details.add(detail);

            orderSubTotal = orderSubTotal.add(subtotal);
            orderTotalServiceFee = orderTotalServiceFee.add(lineServiceFee);
            orderTotalVat = orderTotalVat.add(lineVat);
            orderGrandTotal = orderGrandTotal.add(lineTotal);

            // Tự động đẩy phụ phí vào BookingCharge (loại SERVICE) của BookingRoom để tích hợp Folio
            BookingCharge charge = BookingCharge.builder()
                    .bookingRoom(bookingRoom)
                    .chargeType(BookingChargeType.SERVICE)
                    .itemName("Order " + orderNumber + ": " + menu.getName())
                    .description("Chi tiết: " + itemReq.getQuantity() + "x " + menu.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .serviceFeeRate(standardServiceFeeRate)
                    .serviceFeeAmount(lineServiceFee)
                    .vatRate(vatRate)
                    .vatAmount(lineVat)
                    .totalAmount(lineTotal)
                    .issuedAt(OffsetDateTime.now())
                    .build();

            bookingChargeRepository.save(charge);
        }

        order.setDetails(details);
        order.setSubTotal(orderSubTotal);
        order.setServiceFeeAmount(orderTotalServiceFee);
        order.setVatAmount(orderTotalVat);
        order.setTotalAmount(orderGrandTotal);

        ServiceOrder saved = serviceOrderRepository.save(order);
        return operationMapper.toResponse(saved);
    }

    @Override
    public ServiceOrderResponse getById(Long id) {
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_ORDER_NOT_FOUND, "Không tìm thấy đơn dịch vụ ID: " + id));
        return operationMapper.toResponse(order);
    }

    @Override
    public PageResponse<ServiceOrderResponse> filter(ServiceOrderSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() > 0 ? searchDto.getPage() - 1 : 0;
        int size = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        Specification<ServiceOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDto.getBookingId() != null) {
                predicates.add(cb.equal(root.get("booking").get("id"), searchDto.getBookingId()));
            }
            if (searchDto.getRoomInstanceId() != null) {
                predicates.add(cb.equal(root.get("roomInstance").get("id"), searchDto.getRoomInstanceId()));
            }
            if (searchDto.getOrderNumber() != null && !searchDto.getOrderNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("orderNumber")), "%" + searchDto.getOrderNumber().trim().toLowerCase() + "%"));
            }
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ServiceOrder> pageResult = serviceOrderRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(pageResult.map(operationMapper::toResponse));
    }

    @Override
    @Transactional
    public ServiceOrderResponse updateStatus(Long id, ServiceOrderStatus status) {
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_ORDER_NOT_FOUND, "Không tìm thấy đơn dịch vụ ID: " + id));

        if (order.getStatus() == ServiceOrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_SERVICE_ORDER_STATUS, "Đơn dịch vụ đã bị hủy trước đó");
        }

        // Hoàn trả tồn kho nếu hủy đơn hàng
        if (status == ServiceOrderStatus.CANCELLED) {
            for (ServiceOrderDetail detail : order.getDetails()) {
                if (detail.getMenu().getMenuType() == MenuType.PRODUCT) {
                    catalogItemRepository.findById(detail.getMenu().getId()).ifPresent(item -> {
                        item.setStockQuantity(item.getStockQuantity() + detail.getQuantity());
                        catalogItemRepository.save(item);
                    });
                }
            }
        }

        order.setStatus(status);
        ServiceOrder saved = serviceOrderRepository.save(order);
        return operationMapper.toResponse(saved);
    }
}
