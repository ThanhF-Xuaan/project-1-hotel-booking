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
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.*;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.service.VatRuleService;

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
    BookingLookupService bookingLookupService;
    StayGuestService stayGuestService;
    HotelService hotelService;
    MenuService menuService;
    VatRuleService vatRuleService;
    OperationMapper operationMapper;

    @Override
    @Transactional
    public ServiceOrderResponse createOrder(ServiceOrderCreateRequest request) {
        ServiceOrderReferenceType refType = request.getReferenceType();
        if (refType == null) {
            if (request.getBookingId() != null) {
                refType = ServiceOrderReferenceType.BOOKING;
            } else {
                refType = ServiceOrderReferenceType.WALK_IN;
            }
        }

        Long refId = request.getReferenceId();
        if (refId == null && refType == ServiceOrderReferenceType.BOOKING && request.getBookingId() != null) {
            refId = request.getBookingId();
        }

        Hotel hotel = null;

        switch (refType) {
            case BOOKING:
                if (refId == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID đơn đặt phòng không được để trống khi hạch toán BOOKING");
                }
                final Long bookingId = refId;
                Booking booking = bookingLookupService.getBookingEntityById(bookingId);
                hotel = booking.getHotel();
                break;

            case BOOKING_ROOM:
                if (refId == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID phòng đặt (BookingRoom) không được để trống khi hạch toán BOOKING_ROOM");
                }
                final Long bookingRoomId = refId;
                BookingRoom bookingRoom = bookingLookupService.getBookingRoomEntityById(bookingRoomId);
                if (bookingRoom.getStatus() == BookingRoomStatus.CANCELLED || bookingRoom.getStatus() == BookingRoomStatus.CHECKED_OUT) {
                    throw new AppException(ErrorCode.ROOM_NOT_OCCUPIED, "Phòng này không ở trạng thái lưu trú khả dụng");
                }
                hotel = bookingRoom.getBookingDetail().getBooking().getHotel();
                break;

            case STAY_GUEST:
                if (refId == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID khách lưu trú (StayGuest) không được để trống khi hạch toán STAY_GUEST");
                }
                final Long stayGuestId = refId;
                StayGuest stayGuest = stayGuestService.getEntityById(stayGuestId);
                if (stayGuest.getBookingRoom() != null && stayGuest.getBookingRoom().getBookingDetail() != null) {
                    hotel = stayGuest.getBookingRoom().getBookingDetail().getBooking().getHotel();
                } else {
                    hotel = stayGuest.getHotel();
                }
                break;

            case WALK_IN:
                // Đối với khách vãng lai, reference_id BẮT BUỘC là NULL theo quy chuẩn nghiệp vụ
                refId = null;
                if (request.getHotelId() == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Khách vãng lai bắt buộc phải cung cấp hotelId để hạch toán doanh thu");
                }
                hotel = hotelService.getEntityById(request.getHotelId());
                break;
        }

        if (hotel == null && request.getHotelId() != null) {
            hotel = hotelService.getEntityById(request.getHotelId());
        }

        if (hotel == null) {
            throw new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không xác định được khách sạn hạch toán cho đơn dịch vụ");
        }

        String orderNumber = "SO-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 900 + 100);
        BigDecimal standardServiceFeeRate = new BigDecimal("5.00"); // 5% Hotel Service Charge

        ServiceOrderPaymentMethod paymentMethod = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : (refType == ServiceOrderReferenceType.WALK_IN ? ServiceOrderPaymentMethod.CASH : ServiceOrderPaymentMethod.POST_TO_ROOM);

        ServiceOrderPaymentStatus paymentStatus = request.getPaymentStatus() != null
                ? request.getPaymentStatus()
                : (paymentMethod == ServiceOrderPaymentMethod.POST_TO_ROOM ? ServiceOrderPaymentStatus.UNPAID : ServiceOrderPaymentStatus.PAID);

        ServiceOrder order = ServiceOrder.builder()
                .hotel(hotel)
                .referenceType(refType)
                .referenceId(refId)
                .orderNumber(orderNumber)
                .serviceFeeRate(standardServiceFeeRate)
                .status(ServiceOrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .issuedAt(OffsetDateTime.now())
                .build();

        List<ServiceOrderDetail> details = new ArrayList<>();
        BigDecimal orderSubTotal = BigDecimal.ZERO;
        BigDecimal orderTotalServiceFee = BigDecimal.ZERO;
        BigDecimal orderTotalVat = BigDecimal.ZERO;
        BigDecimal orderGrandTotal = BigDecimal.ZERO;

        for (ServiceOrderCreateRequest.OrderItemRequest itemReq : request.getItems()) {
            Menu menu = menuService.getEntityById(itemReq.getMenuId());

            if (!"ACTIVE".equalsIgnoreCase(menu.getStatus())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Món/dịch vụ tạm ngưng phục vụ: " + menu.getName());
            }

            // Trừ tồn kho nếu là sản phẩm vật lý (Product) qua MenuService
            menuService.deductStock(menu.getId(), itemReq.getQuantity());

            // Ưu tiên giá override tại cơ sở, fallback về giá chuẩn của MasterItem nếu có
            BigDecimal unitPrice = menu.getBasePrice();
            if (unitPrice == null && menu.getMasterItem() != null) {
                unitPrice = menu.getMasterItem().getDefaultBasePrice();
            }
            if (unitPrice == null) {
                unitPrice = BigDecimal.ZERO;
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal lineServiceFee = subtotal.multiply(standardServiceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal taxableAmount = subtotal.add(lineServiceFee);

            BigDecimal vatRate = BigDecimal.ZERO;
            if (menu.getTaxCategory() != null) {
                BigDecimal fetchedVat = vatRuleService.getActiveVatPercent(menu.getTaxCategory().getId(), LocalDate.now());
                if (fetchedVat != null) {
                    vatRate = fetchedVat;
                }
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
    public List<ServiceOrderResponse> getOrdersByBookingId(Long bookingId) {
        if (bookingId == null) {
            return List.of();
        }
        List<BookingRoom> rooms = bookingLookupService.getBookingRoomsByBookingId(bookingId);
        List<Long> roomIds = rooms != null ? rooms.stream().map(BookingRoom::getId).toList() : List.of();

        List<ServiceOrder> orders;
        if (roomIds.isEmpty()) {
            orders = serviceOrderRepository.findByBookingIdAndIsDeletedFalse(bookingId);
        } else {
            orders = serviceOrderRepository.findAllOrdersForBookingAndRooms(bookingId, roomIds);
        }

        return orders.stream()
                .filter(so -> so.getStatus() != ServiceOrderStatus.CANCELLED)
                .map(operationMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<ServiceOrderResponse> filter(ServiceOrderSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() > 0 ? searchDto.getPage() - 1 : 0;
        int size = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        Specification<ServiceOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDto.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), searchDto.getHotelId()));
            }
            if (searchDto.getReferenceType() != null) {
                predicates.add(cb.equal(root.get("referenceType"), searchDto.getReferenceType()));
            }
            if (searchDto.getReferenceId() != null) {
                predicates.add(cb.equal(root.get("referenceId"), searchDto.getReferenceId()));
            }
            if (searchDto.getBookingId() != null) {
                predicates.add(cb.equal(root.get("referenceType"), ServiceOrderReferenceType.BOOKING));
                predicates.add(cb.equal(root.get("referenceId"), searchDto.getBookingId()));
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

        // Hoàn trả tồn kho qua MenuService nếu hủy đơn hàng
        if (status == ServiceOrderStatus.CANCELLED) {
            for (ServiceOrderDetail detail : order.getDetails()) {
                menuService.restoreStock(detail.getMenu().getId(), detail.getQuantity());
            }
        }

        order.setStatus(status);
        ServiceOrder saved = serviceOrderRepository.save(order);
        return operationMapper.toResponse(saved);
    }

    @Override
    public BigDecimal getDailyServiceRevenue(Short hotelId, LocalDate date) {
        if (hotelId == null || date == null) {
            return BigDecimal.ZERO;
        }
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);

        List<ServiceOrder> dailyOrders = serviceOrderRepository.findActiveOrdersForHotelAndDate(hotelId, startOfDay, endOfDay);
        return dailyOrders.stream()
                .map(ServiceOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public void markOrdersPaidForBooking(Long bookingId) {
        if (bookingId == null) {
            return;
        }
        List<BookingRoom> rooms = bookingLookupService.getBookingRoomsByBookingId(bookingId);
        List<Long> roomIds = rooms != null ? rooms.stream().map(BookingRoom::getId).toList() : List.of();

        List<ServiceOrder> orders;
        if (roomIds.isEmpty()) {
            orders = serviceOrderRepository.findByBookingIdAndIsDeletedFalse(bookingId);
        } else {
            orders = serviceOrderRepository.findAllOrdersForBookingAndRooms(bookingId, roomIds);
        }

        for (ServiceOrder order : orders) {
            if (order.getStatus() != ServiceOrderStatus.CANCELLED
                    && order.getPaymentMethod() == ServiceOrderPaymentMethod.POST_TO_ROOM
                    && order.getPaymentStatus() == ServiceOrderPaymentStatus.UNPAID) {
                order.setPaymentStatus(ServiceOrderPaymentStatus.PAID);
                serviceOrderRepository.save(order);
            }
        }
    }

    @Override
    public List<ServiceOrder> getActiveOrdersForHotelsAndDateRange(List<Short> hotelIds, OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        if (hotelIds == null || hotelIds.isEmpty() || startDateTime == null || endDateTime == null) {
            return List.of();
        }
        return serviceOrderRepository.findActiveOrdersForHotelsAndDateRange(hotelIds, startDateTime, endDateTime);
    }
}

