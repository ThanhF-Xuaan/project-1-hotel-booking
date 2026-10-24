package vn.edu.utc.hotel_booking.modules.booking.service.impl;

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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingChargeResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomSwapRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSwapResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.mapper.BookingMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.RoomSwapRecordService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingChargeService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingRoomService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.service.CompanyService;
import vn.edu.utc.hotel_booking.modules.identity.service.GuestService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.ServiceOrderResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.SurchargePreviewRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.SurchargeProposalResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.LeadGuestRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CartPriceQuoteResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.ItemizedSurchargeDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomPriceQuoteResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.session.BookingDraftSession;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CheckoutSettlementResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.SettlementLineItemDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.Payment;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.InvoiceService;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.InvoiceCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentPurpose;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.CompanyCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.CompanyResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.WalkInQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.WalkInQuoteResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingNoShowRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.ResolvedBookingPolicies;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingPolicyResolverService;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.WalkInBookingRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCancelRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelPreviewResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelResponse;
import org.springframework.util.StringUtils;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    BookingRepository bookingRepository;
    RoomSwapRecordService roomSwapRecordService;

    BookingRoomService bookingRoomService;
    BookingChargeService bookingChargeService;
    HotelService hotelService;
    GuestService guestService;
    CompanyService companyService;
    HotelRoomTypeService hotelRoomTypeService;
    RoomInstanceService roomInstanceService;
    RoomSlotService roomSlotService;
    RoomAvailabilityService roomAvailabilityService;
    PriceEngine priceEngine;
    TaxCategoryService taxCategoryService;
    VatRuleService vatRuleService;
    VoucherService voucherService;
    ServiceOrderService serviceOrderService;
    StayGuestService stayGuestService;
    InvoiceService invoiceService;
    PaymentRepository paymentRepository;
    SurchargeRuleRepository surchargeRuleRepository;
    AuditLogService auditLogService;
    BookingPolicyResolverService bookingPolicyResolverService;
    BookingMapper bookingMapper;
    FinanceMapper financeMapper;
    StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        Hotel hotel = hotelService.getEntityById(request.getHotelId());
        BookingGuest guest = guestService.getEntityById(request.getGuestId());

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyService.getEntityById(request.getCompanyId());
        }

        String bookingNumber = generateBookingNumber();

        Booking booking = Booking.builder()
                .hotel(hotel)
                .guest(guest)
                .company(company)
                .bookingType(request.getBookingType())
                .bookingNumber(bookingNumber)
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(request.getServiceFeeRate() != null ? request.getServiceFeeRate() : BigDecimal.ZERO)
                .issuedAt(OffsetDateTime.now())
                .subtotalAmount(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .totalVatAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal bookingSubtotal = BigDecimal.ZERO;
        BigDecimal bookingVatTotal = BigDecimal.ZERO;
        List<BookingDetail> details = new ArrayList<>();

        // Sắp xếp danh sách phòng theo hotelRoomTypeId tăng dần để đảm bảo Sequential Lock Ordering tránh Deadlock
        List<BookingCreateRequest.RoomItemRequest> sortedRooms = new ArrayList<>(request.getRooms());
        sortedRooms.sort(Comparator.comparing(BookingCreateRequest.RoomItemRequest::getHotelRoomTypeId));

        for (BookingCreateRequest.RoomItemRequest roomReq : sortedRooms) {
            if (!roomReq.getCheckInDate().isBefore(roomReq.getCheckOutDate())) {
                throw new AppException(ErrorCode.INVALID_BOOKING_DATES,
                        "Ngày nhận phòng (" + roomReq.getCheckInDate() + ") phải trước ngày trả phòng (" + roomReq.getCheckOutDate() + ")");
            }

            HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(roomReq.getHotelRoomTypeId());

            // Kiểm tra ràng buộc sức chứa (Capacity Constraints)
            short reqAdults = roomReq.getAdultCount() != null ? roomReq.getAdultCount() : (short) 1;
            short reqChildren = roomReq.getChildCount() != null ? roomReq.getChildCount() : (short) 0;
            short reqInfants = roomReq.getInfantCount() != null ? roomReq.getInfantCount() : (short) 0;
            short reqExtraBeds = roomReq.getExtraBeds() != null ? roomReq.getExtraBeds() : (short) 0;
            short totalGuests = (short) (reqAdults + reqChildren + reqInfants);

            if (hotelRoomType.getMaxAdults() != null && reqAdults > hotelRoomType.getMaxAdults()) {
                throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY,
                        "Số người lớn (" + reqAdults + ") vượt quá số người lớn tối đa (" + hotelRoomType.getMaxAdults() + ") của hạng phòng " + hotelRoomType.getRoomType().getName());
            }
            if (hotelRoomType.getMaxChildren() != null && reqChildren > hotelRoomType.getMaxChildren()) {
                throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY,
                        "Số trẻ em (" + reqChildren + ") vượt quá số trẻ em tối đa (" + hotelRoomType.getMaxChildren() + ") của hạng phòng " + hotelRoomType.getRoomType().getName());
            }
            if (hotelRoomType.getMaxInfants() != null && reqInfants > hotelRoomType.getMaxInfants()) {
                throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY,
                        "Số em bé (" + reqInfants + ") vượt quá số em bé tối đa (" + hotelRoomType.getMaxInfants() + ") của hạng phòng " + hotelRoomType.getRoomType().getName());
            }
            if (hotelRoomType.getMaxTotalGuests() != null && totalGuests > hotelRoomType.getMaxTotalGuests()) {
                throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY,
                        "Tổng số khách (" + totalGuests + ") vượt quá sức chứa tối đa (" + hotelRoomType.getMaxTotalGuests() + ") của hạng phòng " + hotelRoomType.getRoomType().getName());
            }
            if (hotelRoomType.getExtraBeds() != null && reqExtraBeds > hotelRoomType.getExtraBeds()) {
                throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY,
                        "Số giường phụ (" + reqExtraBeds + ") vượt quá số giường phụ tối đa (" + hotelRoomType.getExtraBeds() + ") của hạng phòng " + hotelRoomType.getRoomType().getName());
            }

            // 1. Giữ/Trừ quỹ phòng tổng hợp (Aggregate Inventory)
            int quantity = roomReq.getQuantity() != null ? roomReq.getQuantity().intValue() : 1;
            roomAvailabilityService.confirmBookingInventory(hotelRoomType.getId(), roomReq.getCheckInDate(), roomReq.getCheckOutDate(), quantity);

            // 2. Tính toán đơn giá qua Pricing Engine
            PriceCalculationRequest priceReq = PriceCalculationRequest.builder()
                    .hotelRoomTypeId(hotelRoomType.getId())
                    .checkInDate(roomReq.getCheckInDate())
                    .checkOutDate(roomReq.getCheckOutDate())
                    .adults(reqAdults)
                    .children(reqChildren)
                    .infants(reqInfants)
                    .extraBeds(reqExtraBeds)
                    .voucherCode(request.getVoucherCode())
                    .build();
            PriceBreakdownDto priceBreakdown = priceEngine.calculatePrice(priceReq);

            ResolvedBookingPolicies resolvedPolicies = bookingPolicyResolverService.resolvePolicyForBooking(
                    hotel.getId(), hotelRoomType.getId(), booking.getBookingType());

            BookingDetail detail = BookingDetail.builder()
                    .booking(booking)
                    .hotelRoomType(hotelRoomType)
                    .roomTypeName(hotelRoomType.getRoomType().getName())
                    .quantity(roomReq.getQuantity())
                    .checkInDate(roomReq.getCheckInDate())
                    .checkOutDate(roomReq.getCheckOutDate())
                    .cancellationPolicy(resolvedPolicies != null ? resolvedPolicies.getCancellationPolicy() : null)
                    .noShowPolicy(resolvedPolicies != null ? resolvedPolicies.getNoShowPolicy() : null)
                    .cancellationPolicySnapshot(resolvedPolicies != null ? resolvedPolicies.getCancellationPolicySnapshotJson() : null)
                    .noShowPolicySnapshot(resolvedPolicies != null ? resolvedPolicies.getNoShowPolicySnapshotJson() : null)
                    .build();

            List<BookingRoom> bookingRooms = new ArrayList<>();

            for (int i = 0; i < quantity; i++) {
                RoomInstance assignedRoom = null;
                // Nếu là walk-in hoặc có chỉ định phòng vật lý
                if (i == 0 && roomReq.getRoomInstanceId() != null) {
                    assignedRoom = roomInstanceService.getEntityById(roomReq.getRoomInstanceId());

                    // Sequential Lock ordering: Kiểm tra xem phòng có bị trùng lịch không
                    List<BookingRoom> overlapping = bookingRoomService.findOverlappingAssignedRooms(
                            assignedRoom.getId(), roomReq.getCheckInDate(), roomReq.getCheckOutDate());
                    if (!overlapping.isEmpty()) {
                        throw new AppException(ErrorCode.ROOM_ALREADY_ASSIGNED,
                                "Phòng " + assignedRoom.getRoomNumber() + " đã có khách đặt trong khoảng thời gian này");
                    }
                }

                short adultCount = reqAdults;
                short childCount = reqChildren;
                short infantCount = reqInfants;
                short guestCount = totalGuests;

                BookingRoom bookingRoom = BookingRoom.builder()
                        .bookingDetail(detail)
                        .roomInstance(assignedRoom)
                        .adultCount(adultCount)
                        .childCount(childCount)
                        .infantCount(infantCount)
                        .guestCount(guestCount)
                        .status(BookingRoomStatus.EXPECTED)
                        .assignedAt(assignedRoom != null ? OffsetDateTime.now() : null)
                        .build();

                // Tạo danh sách khách lưu trú (StayGuest trong bảng stay_guests)
                List<StayGuest> stayGuests = new ArrayList<>();
                if (roomReq.getGuests() != null && !roomReq.getGuests().isEmpty()) {
                    for (BookingCreateRequest.GuestItemRequest gReq : roomReq.getGuests()) {
                        StayGuest sg = StayGuest.builder()
                                .bookingRoom(bookingRoom)
                                .firstName(gReq.getFirstName() != null ? gReq.getFirstName() : "")
                                .lastName(gReq.getLastName() != null ? gReq.getLastName() : "")
                                .fullName(gReq.getFullName() != null ? gReq.getFullName() : (gReq.getFirstName() + " " + gReq.getLastName()).trim())
                                .dateOfBirth(gReq.getBirthDate())
                                .guestType(gReq.getGuestType() != null ? gReq.getGuestType() : BookingGuestType.ADULT)
                                .documentNumber(gReq.getIdentityNumber())
                                .build();
                        if (gReq.getIdentityType() != null) {
                            sg.setIdentityType(gReq.getIdentityType());
                        }
                        stayGuests.add(sg);
                    }
                }
                bookingRoom.setStayGuests(stayGuests);

                // Tạo Snapshot giá chi tiết từng đêm (BookingDailyRate)
                List<BookingDailyRate> dailyRates = new ArrayList<>();
                BigDecimal roomSubtotal = BigDecimal.ZERO;
                BigDecimal roomVat = BigDecimal.ZERO;

                TaxCategory taxCategory = null;
                BigDecimal vatPercent = BigDecimal.valueOf(10); // 10% default
                if (hotelRoomType.getTaxCategoryId() != null) {
                    taxCategory = taxCategoryService.getEntityById(hotelRoomType.getTaxCategoryId());
                    BigDecimal activeVat = vatRuleService.getActiveVatPercent(hotelRoomType.getTaxCategoryId(), roomReq.getCheckInDate());
                    if (activeVat != null && activeVat.compareTo(BigDecimal.ZERO) > 0) {
                        vatPercent = activeVat;
                    }
                }

                for (DailyPriceDto daily : priceBreakdown.getDailyPrices()) {
                    BigDecimal basePrice = daily.getBaseRate();
                    BigDecimal discount = daily.getDiscountAmount() != null ? daily.getDiscountAmount() : BigDecimal.ZERO;
                    BigDecimal surcharge = daily.getSurchargeAmount() != null ? daily.getSurchargeAmount() : BigDecimal.ZERO;
                    BigDecimal netPrice = daily.getNetAmount();

                    BigDecimal serviceFeeRate = booking.getServiceFeeRate();
                    BigDecimal serviceFee = netPrice.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal taxable = netPrice.add(serviceFee);
                    BigDecimal vatAmount = taxable.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    BookingDailyRate dailyRate = BookingDailyRate.builder()
                            .bookingRoom(bookingRoom)
                            .stayDate(daily.getDate())
                            .basePrice(basePrice)
                            .discountAmount(discount)
                            .surchargeAmount(surcharge)
                            .serviceFeeRate(serviceFeeRate)
                            .serviceFeeAmount(serviceFee)
                            .taxCategory(taxCategory)
                            .vatPercent(vatPercent)
                            .vatAmount(vatAmount)
                            .netPrice(netPrice)
                            .status("CONFIRMED")
                            .build();

                    dailyRates.add(dailyRate);
                    roomSubtotal = roomSubtotal.add(netPrice);
                    roomVat = roomVat.add(vatAmount);
                }

                bookingRoom.setDailyRates(dailyRates);
                bookingRooms.add(bookingRoom);

                bookingSubtotal = bookingSubtotal.add(roomSubtotal);
                bookingVatTotal = bookingVatTotal.add(roomVat);
            }

            detail.setBookingRooms(bookingRooms);
            details.add(detail);
        }

        booking.setBookingDetails(details);
        booking.setSubtotalAmount(bookingSubtotal);

        BigDecimal bookingServiceFee = bookingSubtotal.multiply(booking.getServiceFeeRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        booking.setServiceFeeAmount(bookingServiceFee);
        booking.setTotalVatAmount(bookingVatTotal);
        booking.setTotalAmount(bookingSubtotal.add(bookingServiceFee).add(bookingVatTotal));

        Booking saved = bookingRepository.save(booking);

        // Nếu có phòng vật lý được chỉ định, tạo RoomSlot tương ứng
        for (BookingDetail d : saved.getBookingDetails()) {
            for (BookingRoom br : d.getBookingRooms()) {
                if (br.getRoomInstance() != null) {
                    roomSlotService.createOrReserveSlots(br.getRoomInstance(), d.getCheckInDate(), d.getCheckOutDate(), br.getId());
                }
            }
        }

        return bookingMapper.toResponse(saved);
    }

    @Override
    public PageResponse<BookingResponse> filter(BookingSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() > 0 ? searchDto.getPage() - 1 : 0;
        int size = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDto.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), searchDto.getHotelId()));
            }
            if (searchDto.getGuestId() != null) {
                predicates.add(cb.equal(root.get("guest").get("id"), searchDto.getGuestId()));
            }
            if (searchDto.getBookingNumber() != null && !searchDto.getBookingNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("bookingNumber")),
                        "%" + searchDto.getBookingNumber().trim().toLowerCase() + "%"));
            }
            if (searchDto.getBookingType() != null) {
                predicates.add(cb.equal(root.get("bookingType"), searchDto.getBookingType()));
            }
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }

            jakarta.persistence.criteria.Join<Booking, BookingDetail> detailJoin = null;
            if (searchDto.getCheckInDate() != null || searchDto.getCheckOutDate() != null || searchDto.getIsAssigned() != null) {
                detailJoin = root.join("bookingDetails");
                query.distinct(true);
            }
            if (searchDto.getCheckInDate() != null && detailJoin != null) {
                predicates.add(cb.equal(detailJoin.get("checkInDate"), searchDto.getCheckInDate()));
            }
            if (searchDto.getCheckOutDate() != null && detailJoin != null) {
                predicates.add(cb.equal(detailJoin.get("checkOutDate"), searchDto.getCheckOutDate()));
            }
            if (searchDto.getIsAssigned() != null && detailJoin != null) {
                jakarta.persistence.criteria.Join<BookingDetail, BookingRoom> roomJoin = detailJoin.join("bookingRooms");
                if (Boolean.TRUE.equals(searchDto.getIsAssigned())) {
                    predicates.add(cb.isNotNull(roomJoin.get("roomInstance")));
                } else {
                    predicates.add(cb.isNull(roomJoin.get("roomInstance")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Booking> pageResult = bookingRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(pageResult.map(bookingMapper::toResponse));
    }

    @Override
    public BookingResponse getById(Long id) {
        Booking booking = getEntityById(id);
        return bookingMapper.toResponse(booking);
    }

    @Override
    public Booking getEntityById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng ID: " + id));
    }

    @Override
    public BookingRoom getBookingRoomEntityById(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID phòng đặt (BookingRoom) không được để trống");
        }
        return bookingRoomService.getEntityById(id);
    }

    @Override
    public BookingResponse getByBookingNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng: " + bookingNumber));
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng ID: " + id));

        if (status == BookingStatus.NO_SHOW) {
            String username = SecurityUtils.getCurrentUsername().orElse("STAFF");
            Booking saved = executeNoShowPipeline(booking, "Cập nhật thủ công trạng thái No-Show", username);
            return bookingMapper.toResponse(saved);
        }

        if (status == BookingStatus.CANCELLED) {
            String username = SecurityUtils.getCurrentUsername().orElse("STAFF");
            BookingCancelRequest cancelReq = BookingCancelRequest.builder()
                    .reason("Cập nhật thủ công trạng thái Hủy đơn")
                    .build();
            cancelBooking(id, cancelReq, username);
            Booking reloaded = bookingRepository.findById(id).orElse(booking);
            return bookingMapper.toResponse(reloaded);
        }

        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse assignRoom(Long bookingRoomId, Integer roomInstanceId) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);
        RoomInstance roomInstance = roomInstanceService.getEntityById(roomInstanceId);

        BookingDetail detail = bookingRoom.getBookingDetail();

        // Kiểm tra xung đột lịch
        List<BookingRoom> overlapping = bookingRoomService.findOverlappingAssignedRooms(
                roomInstance.getId(), detail.getCheckInDate(), detail.getCheckOutDate());
        for (BookingRoom o : overlapping) {
            if (!o.getId().equals(bookingRoom.getId())) {
                throw new AppException(ErrorCode.ROOM_ALREADY_ASSIGNED,
                        "Phòng " + roomInstance.getRoomNumber() + " đã có khách đặt trong khoảng thời gian này");
            }
        }

        // Hủy liên kết phòng cũ nếu có
        if (bookingRoom.getRoomInstance() != null) {
            roomSlotService.releaseSlots(bookingRoom.getRoomInstance().getId(), detail.getCheckInDate(), detail.getCheckOutDate());
        }

        bookingRoom.setRoomInstance(roomInstance);
        bookingRoom.setAssignedAt(OffsetDateTime.now());
        if (bookingRoom.getStayGuests() != null) {
            for (StayGuest g : bookingRoom.getStayGuests()) {
                g.setRoom(roomInstance);
                g.setRoomNumber(roomInstance.getRoomNumber());
            }
        }
        bookingRoomService.save(bookingRoom);

        roomSlotService.createOrReserveSlots(roomInstance, detail.getCheckInDate(), detail.getCheckOutDate(), bookingRoom.getId());

        Booking booking = detail.getBooking();
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public RoomSwapResponse swapRoom(Long bookingRoomId, RoomSwapRequest request) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);

        // 1. Kiểm tra trạng thái BookingRoom
        if (bookingRoom.getStatus() == BookingRoomStatus.CHECKED_OUT
                || bookingRoom.getStatus() == BookingRoomStatus.CANCELLED
                || bookingRoom.getStatus() == BookingRoomStatus.NO_SHOW) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Không thể đổi phòng cho lượt đặt phòng đang ở trạng thái: " + bookingRoom.getStatus());
        }

        RoomInstance currentRoom = bookingRoom.getRoomInstance();
        if (currentRoom == null) {
            throw new AppException(ErrorCode.ROOM_NOT_YET_ASSIGNED,
                    "Phòng đặt ID " + bookingRoomId + " chưa được gán phòng vật lý ban đầu. Vui lòng xếp phòng trước.");
        }

        if (currentRoom.getId().equals(request.getTargetRoomInstanceId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Phòng đích trùng với phòng hiện tại (" + currentRoom.getRoomNumber() + ")");
        }

        // 2. Kiểm tra phòng đích
        RoomInstance targetRoom = roomInstanceService.getEntityById(request.getTargetRoomInstanceId());
        BookingDetail detail = bookingRoom.getBookingDetail();
        Booking booking = detail.getBooking();

        if (!targetRoom.getHotel().getId().equals(booking.getHotel().getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Phòng đích không thuộc cùng khách sạn với đơn đặt phòng");
        }

        if ("DIRTY".equalsIgnoreCase(targetRoom.getCurrentStatus())
                || "CLEANING".equalsIgnoreCase(targetRoom.getCurrentStatus())
                || "MAINTENANCE".equalsIgnoreCase(targetRoom.getCurrentStatus())) {
            throw new AppException(ErrorCode.ROOM_NOT_READY_FOR_CHECKIN,
                    "Phòng đích " + targetRoom.getRoomNumber() + " đang ở trạng thái '" + targetRoom.getCurrentStatus() + "'. Không thể nhận khách.");
        }

        LocalDate effectiveDate = request.getEffectiveDate() != null ? request.getEffectiveDate() : LocalDate.now();
        if (effectiveDate.isBefore(detail.getCheckInDate())) {
            effectiveDate = detail.getCheckInDate();
        }
        if (!effectiveDate.isBefore(detail.getCheckOutDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Ngày hiệu lực đổi phòng (" + effectiveDate + ") phải trước ngày trả phòng (" + detail.getCheckOutDate() + ")");
        }

        // Kiểm tra xung đột lịch với các booking khác
        List<BookingRoom> overlapping = bookingRoomService.findOverlappingAssignedRooms(
                targetRoom.getId(), effectiveDate, detail.getCheckOutDate());
        for (BookingRoom o : overlapping) {
            if (!o.getId().equals(bookingRoom.getId())) {
                throw new AppException(ErrorCode.ROOM_ALREADY_ASSIGNED,
                        "Phòng đích " + targetRoom.getRoomNumber() + " đã có khách đặt trong khoảng thời gian từ " + effectiveDate + " đến " + detail.getCheckOutDate());
            }
        }

        // 3. Phân biệt nâng hạng / đổi hạng phòng
        HotelRoomType currentRoomType = detail.getHotelRoomType();
        HotelRoomType targetRoomType = targetRoom.getHotelRoomType();

        boolean isSameType = currentRoomType.getId().equals(targetRoomType.getId());
        boolean isUpgrade = !isSameType && targetRoomType.getBasePrice().compareTo(currentRoomType.getBasePrice()) > 0;
        boolean isComplimentary = Boolean.TRUE.equals(request.getIsComplimentary());

        BigDecimal priceDifference = BigDecimal.ZERO;

        // Tính chênh lệch giá nếu đổi khác hạng và không phải complimentary
        if (!isSameType && !isComplimentary) {
            long remainingNights = ChronoUnit.DAYS.between(effectiveDate, detail.getCheckOutDate());
            if (remainingNights > 0) {
                BigDecimal diffPerNight = targetRoomType.getBasePrice().subtract(currentRoomType.getBasePrice());
                priceDifference = diffPerNight.multiply(BigDecimal.valueOf(remainingNights));

                if (priceDifference.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal subtotalDiff = priceDifference;
                    BigDecimal serviceFeeRate = booking.getServiceFeeRate() != null ? booking.getServiceFeeRate() : BigDecimal.ZERO;
                    BigDecimal serviceFeeDiff = subtotalDiff.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal taxableDiff = subtotalDiff.add(serviceFeeDiff);
                    BigDecimal vatRate = BigDecimal.valueOf(10);
                    BigDecimal vatDiff = taxableDiff.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    String targetRoomTypeName = targetRoomType.getRoomType() != null ? targetRoomType.getRoomType().getName() : "Phòng";
                    BookingChargeCreateRequest chargeReq = BookingChargeCreateRequest.builder()
                            .chargeType(BookingChargeType.OTHER)
                            .itemName("Chênh lệch nâng hạng phòng: " + targetRoomTypeName)
                            .quantity(1)
                            .unitPrice(subtotalDiff)
                            .serviceFeeRate(serviceFeeRate)
                            .vatRate(vatRate)
                            .description("Đổi từ " + currentRoom.getRoomNumber() + " sang " + targetRoom.getRoomNumber() + " (" + remainingNights + " đêm)")
                            .build();

                    BookingCharge savedCharge = bookingChargeService.createCharge(bookingRoom, chargeReq);

                    booking.setSubtotalAmount(booking.getSubtotalAmount().add(savedCharge.getSubtotal()));
                    booking.setServiceFeeAmount(booking.getServiceFeeAmount().add(savedCharge.getServiceFeeAmount()));
                    booking.setTotalVatAmount(booking.getTotalVatAmount().add(savedCharge.getVatAmount()));
                    booking.setTotalAmount(booking.getTotalAmount().add(savedCharge.getTotalAmount()));
                    bookingRepository.save(booking);
                }
            }
        }

        // 4. Cập nhật RoomSlot và RoomInstance Status
        boolean isInHouse = (bookingRoom.getStatus() == BookingRoomStatus.CHECKED_IN);
        roomSlotService.swapSlots(currentRoom.getId(), targetRoom, effectiveDate, detail.getCheckOutDate(), bookingRoom.getId(), isInHouse);

        // Phòng cũ lập tức chuyển thành DIRTY
        roomInstanceService.updateRoomStatusEntity(currentRoom.getId(), "DIRTY");

        // Phòng mới chuyển thành OCCUPIED (nếu đang in-house)
        if (isInHouse) {
            roomInstanceService.updateRoomStatusEntity(targetRoom.getId(), "OCCUPIED");
        }

        // 5. Cập nhật BookingRoom & StayGuests
        bookingRoom.setRoomInstance(targetRoom);
        if (bookingRoom.getStayGuests() != null) {
            for (StayGuest sg : bookingRoom.getStayGuests()) {
                sg.setRoom(targetRoom);
                sg.setRoomNumber(targetRoom.getRoomNumber());
            }
        }
        bookingRoomService.save(bookingRoom);

        // 6. Đồng bộ BCA LodgingQueue
        stayGuestService.syncLodgingQueueForBookingRoom(bookingRoom.getId());

        // 7. Lưu vết lịch sử RoomSwapRecord
        String staffUsername = SecurityUtils.getCurrentUsername().orElse("RECEPTIONIST");
        RoomSwapRecord swapRecord = RoomSwapRecord.builder()
                .bookingRoom(bookingRoom)
                .fromRoomInstance(currentRoom)
                .toRoomInstance(targetRoom)
                .fromHotelRoomType(currentRoomType)
                .toHotelRoomType(targetRoomType)
                .swapDate(LocalDate.now())
                .effectiveDate(effectiveDate)
                .reason(request.getReason())
                .isUpgrade(isUpgrade)
                .isComplimentary(isComplimentary)
                .priceDifference(priceDifference)
                .performedBy(staffUsername)
                .build();

        RoomSwapRecord savedRecord = roomSwapRecordService.save(swapRecord);

        // 8. Ghi nhật ký Audit Log
        auditLogService.recordLog(
                null,
                "BOOKING_ROOM_SWAP",
                "BookingRoom",
                bookingRoom.getId().toString(),
                currentRoom.getRoomNumber(),
                targetRoom.getRoomNumber() + " (Lý do: " + request.getReason() + ")",
                null,
                staffUsername
        );

        return RoomSwapResponse.builder()
                .swapRecordId(savedRecord.getId())
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .bookingRoomId(bookingRoom.getId())
                .previousRoomInstanceId(currentRoom.getId())
                .previousRoomNumber(currentRoom.getRoomNumber())
                .previousRoomTypeName(currentRoomType.getRoomType() != null ? currentRoomType.getRoomType().getName() : "Phòng")
                .newRoomInstanceId(targetRoom.getId())
                .newRoomNumber(targetRoom.getRoomNumber())
                .newRoomTypeName(targetRoomType.getRoomType() != null ? targetRoomType.getRoomType().getName() : "Phòng")
                .swapDate(savedRecord.getSwapDate())
                .effectiveDate(savedRecord.getEffectiveDate())
                .reason(savedRecord.getReason())
                .isUpgrade(isUpgrade)
                .isComplimentary(isComplimentary)
                .priceDifference(priceDifference)
                .roomStatus(bookingRoom.getStatus())
                .swappedAt(savedRecord.getCreatedAt())
                .performedBy(staffUsername)
                .build();
    }

    @Override
    @Transactional
    public BookingChargeResponse addCharge(Long bookingRoomId, BookingChargeCreateRequest request) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);
        BookingCharge savedCharge = bookingChargeService.createCharge(bookingRoom, request);

        // Cập nhật tổng tiền đơn đặt phòng
        Booking booking = bookingRoom.getBookingDetail().getBooking();
        booking.setSubtotalAmount(booking.getSubtotalAmount().add(savedCharge.getSubtotal()));
        booking.setServiceFeeAmount(booking.getServiceFeeAmount().add(savedCharge.getServiceFeeAmount()));
        booking.setTotalVatAmount(booking.getTotalVatAmount().add(savedCharge.getVatAmount()));
        booking.setTotalAmount(booking.getTotalAmount().add(savedCharge.getTotalAmount()));
        bookingRepository.save(booking);

        return bookingMapper.toChargeResponse(savedCharge);
    }

    @Override
    @Transactional
    public BookingResponse checkIn(Long bookingRoomId) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);

        if (bookingRoom.getRoomInstance() == null) {
            throw new AppException(ErrorCode.ROOM_NOT_YET_ASSIGNED,
                    "Phòng đặt ID " + bookingRoomId + " chưa được gán phòng vật lý. Vui lòng xếp phòng trước khi Check-in.");
        }

        RoomInstance roomInstance = bookingRoom.getRoomInstance();
        if ("DIRTY".equalsIgnoreCase(roomInstance.getCurrentStatus())
                || "CLEANING".equalsIgnoreCase(roomInstance.getCurrentStatus())
                || "MAINTENANCE".equalsIgnoreCase(roomInstance.getCurrentStatus())) {
            throw new AppException(ErrorCode.ROOM_NOT_READY_FOR_CHECKIN,
                    "Phòng " + roomInstance.getRoomNumber() + " đang ở trạng thái '" + roomInstance.getCurrentStatus() + "'. Vui lòng dọn buồng phòng trước khi Check-in.");
        }

        bookingRoom.setStatus(BookingRoomStatus.CHECKED_IN);
        bookingRoom.setActualCheckInAt(OffsetDateTime.now());
        bookingRoomService.save(bookingRoom);

        BookingDetail detail = bookingRoom.getBookingDetail();
        roomSlotService.occupySlots(roomInstance.getId(), detail.getCheckInDate(), detail.getCheckOutDate());

        stayGuestService.syncLodgingQueueForBookingRoom(bookingRoomId);

        return bookingMapper.toResponse(bookingRoom.getBookingDetail().getBooking());
    }

    @Override
    @Transactional
    public BookingResponse checkOut(Long bookingRoomId) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);

        if (bookingRoom.getStatus() != BookingRoomStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.BOOKING_ROOM_NOT_CHECKED_IN,
                    "Phòng này không ở trạng thái đang lưu trú (CHECKED_IN) để thực hiện check-out.");
        }

        // 1. GATEKEEPER: Kiểm tra công nợ còn lại (Bắt buộc thanh toán hết 100% mới được check-out)
        CheckoutSettlementResponse settlement = getCheckoutSettlement(bookingRoomId);
        if (settlement.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
            throw new AppException(ErrorCode.CHECKOUT_BALANCE_UNPAID,
                    "Không thể check-out: Đơn đặt phòng còn khoản nợ chưa thanh toán ("
                            + String.format("%,.0f", settlement.getBalanceDue())
                            + " đ). Vui lòng thanh toán đủ toàn bộ chi phí trước khi trả phòng.");
        }

        Booking booking = bookingRoom.getBookingDetail().getBooking();

        // 2. Cập nhật trạng thái BookingRoom
        bookingRoom.setStatus(BookingRoomStatus.CHECKED_OUT);
        bookingRoom.setActualCheckOutAt(OffsetDateTime.now());
        bookingRoomService.save(bookingRoom);

        // 3. Cập nhật RoomInstance thành 'DIRTY' trước và đánh dấu RoomSlot dirty
        if (bookingRoom.getRoomInstance() != null) {
            BookingDetail detail = bookingRoom.getBookingDetail();
            roomInstanceService.updateRoomStatusEntity(bookingRoom.getRoomInstance().getId(), "DIRTY");
            roomSlotService.markSlotsDirty(bookingRoom.getRoomInstance().getId(), detail.getCheckInDate(), detail.getCheckOutDate());
        }

        // 4. Đóng toàn bộ đơn dịch vụ POST_TO_ROOM của booking -> PAID
        serviceOrderService.markOrdersPaidForBooking(booking.getId());

        // 5. Quyết toán và đóng Hóa đơn Folio -> PAID
        invoiceService.settleAndCloseInvoice(booking.getId());

        // 6. Đồng bộ hàng chờ lưu trú BCA
        stayGuestService.syncLodgingQueueForBookingRoom(bookingRoomId);

        // 7. Bù trừ công nợ chéo (Folio Netting): Nếu tất cả phòng đã kết thúc và phát sinh khoản cọc thừa -> Tạo REFUND PENDING
        boolean allRoomsDone = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .allMatch(br -> br.getStatus() == BookingRoomStatus.CHECKED_OUT
                        || br.getStatus() == BookingRoomStatus.NO_SHOW
                        || br.getStatus() == BookingRoomStatus.CANCELLED);

        if (allRoomsDone && settlement.getNetOverpaymentAmount() != null && settlement.getNetOverpaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            Payment overpaymentRefund = Payment.builder()
                    .booking(booking)
                    .totalAmount(settlement.getNetOverpaymentAmount())
                    .paymentPurpose(PaymentPurpose.REFUND)
                    .paymentMethod(PaymentMethod.OTHER)
                    .paymentProvider("SYSTEM_FOLIO_NETTING_REFUND")
                    .status(PaymentStatus.PENDING)
                    .transactionReference("REFUND-NETTING-" + booking.getBookingNumber())
                    .gatewayTxnId("PENDING_FINANCE_SETTLEMENT")
                    .rawCallbackPayload("{\"reason\": \"Hoàn tiền cọc dôi dư sau khi bù trừ công nợ chéo check-out\", \"amount\": " + settlement.getNetOverpaymentAmount() + "}")
                    .build();
            paymentRepository.save(overpaymentRefund);
            log.info("Booking {} hoàn tất check-out tất cả các phòng, phát sinh khoản cọc thừa {} đ, đã tạo yêu cầu REFUND ở trạng thái PENDING",
                    booking.getBookingNumber(), settlement.getNetOverpaymentAmount());
        }

        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutSettlementResponse getCheckoutSettlement(Long bookingRoomId) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);
        BookingDetail detail = bookingRoom.getBookingDetail();
        Booking booking = detail.getBooking();

        List<SettlementLineItemDto> lineItems = new ArrayList<>();
        BigDecimal totalRoomCharges = BigDecimal.ZERO;
        BigDecimal totalSurcharges = BigDecimal.ZERO;
        BigDecimal totalServiceCharges = BigDecimal.ZERO;

        BigDecimal bookingServiceFeeRate = booking.getServiceFeeRate() != null ? booking.getServiceFeeRate() : BigDecimal.ZERO;
        BigDecimal totalNoShowPenalty = BigDecimal.ZERO;

        // 1. Tiền phòng (daily_rates) & phụ phí/phạt của toàn bộ các phòng trong booking
        for (BookingDetail bd : booking.getBookingDetails()) {
            for (BookingRoom br : bd.getBookingRooms()) {
                // Nếu phòng bị NO_SHOW: Không tính tiền phòng lưu trú daily_rates các đêm (chỉ tính phí phạt đêm đầu ở mục BookingCharge bên dưới)
                if (br.getStatus() != BookingRoomStatus.NO_SHOW && br.getDailyRates() != null) {
                    for (BookingDailyRate dr : br.getDailyRates()) {
                        BigDecimal lineSubtotal = dr.getNetPrice();
                        BigDecimal lineFee = lineSubtotal.multiply(bookingServiceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        BigDecimal taxable = lineSubtotal.add(lineFee);
                        BigDecimal lineVat = taxable.multiply(dr.getVatPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        BigDecimal lineTotal = taxable.add(lineVat);

                        String roomLabel = (br.getRoomInstance() != null ? br.getRoomInstance().getRoomNumber() : bd.getRoomTypeName());
                        lineItems.add(SettlementLineItemDto.builder()
                                .lineType("ROOM_RATE")
                                .referenceId(dr.getId())
                                .description("Phòng " + roomLabel + " - Đêm ngày " + dr.getStayDate())
                                .quantity(1)
                                .unitPrice(dr.getNetPrice())
                                .subtotal(lineSubtotal)
                                .serviceFeeRate(bookingServiceFeeRate)
                                .serviceFeeAmount(lineFee)
                                .vatRate(dr.getVatPercent())
                                .vatAmount(lineVat)
                                .totalAmount(lineTotal)
                                .build());

                        totalRoomCharges = totalRoomCharges.add(lineTotal);
                    }
                }

                // 2. Phụ phí & Phạt (booking_charges)
                if (br.getCharges() != null) {
                    for (BookingCharge charge : br.getCharges()) {
                        boolean isPenalty = charge.getChargeType() == BookingChargeType.PENALTY;
                        String lineType = isPenalty ? "PENALTY" : "SURCHARGE";
                        String roomLabel = (br.getRoomInstance() != null ? " (Phòng " + br.getRoomInstance().getRoomNumber() + ")" : "");
                        lineItems.add(SettlementLineItemDto.builder()
                                .lineType(lineType)
                                .referenceId(charge.getId())
                                .description(charge.getItemName() + roomLabel)
                                .quantity(charge.getQuantity())
                                .unitPrice(charge.getUnitPrice())
                                .subtotal(charge.getSubtotal())
                                .serviceFeeRate(charge.getServiceFeeRate())
                                .serviceFeeAmount(charge.getServiceFeeAmount())
                                .vatRate(charge.getVatRate())
                                .vatAmount(charge.getVatAmount())
                                .totalAmount(charge.getTotalAmount())
                                .build());

                        if (isPenalty) {
                            totalNoShowPenalty = totalNoShowPenalty.add(charge.getTotalAmount());
                        } else {
                            totalSurcharges = totalSurcharges.add(charge.getTotalAmount());
                        }
                    }
                }
            }
        }

        // 3. Đơn dịch vụ phòng POS (service_orders) dạng POST_TO_ROOM
        List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(booking.getId());
        if (orders != null) {
            for (ServiceOrderResponse order : orders) {
                // Chỉ tính đơn POST_TO_ROOM và chưa thanh toán trực tiếp
                if (order.getPaymentMethod() == vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentMethod.POST_TO_ROOM
                        && order.getPaymentStatus() == vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderPaymentStatus.UNPAID) {
                    if (order.getDetails() != null) {
                        for (var item : order.getDetails()) {
                            lineItems.add(SettlementLineItemDto.builder()
                                    .lineType("SERVICE")
                                    .referenceId(item.getId())
                                    .description(item.getItemName() + " (" + order.getOrderNumber() + ")")
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .subtotal(item.getSubtotal())
                                    .serviceFeeRate(item.getServiceFeeRate())
                                    .serviceFeeAmount(item.getServiceFeeAmount())
                                    .vatRate(item.getVatRate())
                                    .vatAmount(item.getVatAmount())
                                    .totalAmount(item.getTotalAmount())
                                    .build());
                        }
                    }
                    totalServiceCharges = totalServiceCharges.add(order.getTotalAmount());
                }
            }
        }

        BigDecimal grandTotal = totalRoomCharges.add(totalSurcharges).add(totalNoShowPenalty).add(totalServiceCharges);

        // 4. Lấy lịch sử thanh toán thành công (payments)
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        BigDecimal totalPaid = BigDecimal.ZERO;
        List<PaymentResponse> paymentResponses = new ArrayList<>();
        if (payments != null) {
            for (Payment p : payments) {
                if (p.getStatus() == PaymentStatus.SUCCESS) {
                    BigDecimal amt = p.getPaymentPurpose() == PaymentPurpose.REFUND ? p.getTotalAmount().negate() : p.getTotalAmount();
                    totalPaid = totalPaid.add(amt);
                }
                paymentResponses.add(financeMapper.toResponse(p));
            }
        }

        BigDecimal balanceDue = grandTotal.subtract(totalPaid);
        BigDecimal netOverpaymentAmount = BigDecimal.ZERO;
        if (balanceDue.compareTo(BigDecimal.ZERO) < 0) {
            netOverpaymentAmount = balanceDue.abs();
            balanceDue = BigDecimal.ZERO;
        }

        boolean canCheckOut = balanceDue.compareTo(BigDecimal.ZERO) <= 0;

        String roomNumber = bookingRoom.getRoomInstance() != null ? bookingRoom.getRoomInstance().getRoomNumber() : "N/A";
        String guestName = booking.getBookingGuest() != null ? booking.getBookingGuest().getFullName() : "N/A";
        String guestPhone = booking.getBookingGuest() != null && booking.getBookingGuest().getPhone() != null ? booking.getBookingGuest().getPhone() : "";

        return CheckoutSettlementResponse.builder()
                .bookingId(booking.getId())
                .bookingRoomId(bookingRoom.getId())
                .bookingNumber(booking.getBookingNumber())
                .roomNumber(roomNumber)
                .roomTypeName(detail.getRoomTypeName())
                .guestName(guestName)
                .guestPhone(guestPhone)
                .totalRoomCharges(totalRoomCharges)
                .totalSurcharges(totalSurcharges)
                .noShowPenaltyAmount(totalNoShowPenalty)
                .totalServiceCharges(totalServiceCharges)
                .grandTotal(grandTotal)
                .totalPaid(totalPaid)
                .balanceDue(balanceDue)
                .netOverpaymentAmount(netOverpaymentAmount)
                .canCheckOut(canCheckOut)
                .lineItems(lineItems)
                .paymentHistory(paymentResponses)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse confirmOnlineBooking(String sessionId, Long paymentId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mã sessionId không được để trống");
        }

        String redisKey = "booking:draft:" + sessionId.trim();
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null || json.isBlank()) {
            throw new AppException(ErrorCode.BOOKING_SESSION_EXPIRED, "Phiên đặt phòng đã hết hạn hoặc không tồn tại");
        }

        BookingDraftSession draftSession;
        try {
            draftSession = objectMapper.readValue(json, BookingDraftSession.class);
        } catch (Exception e) {
            log.error("Failed to parse draft session JSON for session: {}", sessionId, e);
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không thể đọc thông tin phiên đặt phòng");
        }

        CartPriceQuoteResponse quote = draftSession.getQuoteResponse();
        if (quote == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Phiên đặt phòng không có thông tin báo giá hợp lệ");
        }

        // 1. Snapshot Lead Guest (booking_guests)
        LeadGuestRequest leadReq = draftSession.getLeadGuest();
        if (leadReq == null || leadReq.getPhone() == null || leadReq.getPhone().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Thông tin người đại diện đặt phòng không được để trống");
        }

        String phone = leadReq.getPhone().trim();
        BookingGuest leadGuest = guestService.getOrCreateGuest(phone, leadReq.getFullName(), leadReq.getEmail(), leadReq.getNationality());

        // 2. Snapshot Hotel
        Hotel hotel = hotelService.getEntityById(draftSession.getHotelId());

        // 3. Snapshot Booking (bookings)
        String bookingNumber = generateBookingNumber();
        BigDecimal serviceFeeRate = new BigDecimal("5.00");

        Booking booking = Booking.builder()
                .hotel(hotel)
                .guest(leadGuest)
                .bookingType(BookingType.FIT)
                .bookingNumber(bookingNumber)
                .status(BookingStatus.CONFIRMED)
                .serviceFeeRate(serviceFeeRate)
                .issuedAt(OffsetDateTime.now())
                .notes(leadReq.getNotes())
                .subtotalAmount(quote.getTotalRoomNetAmount() != null ? quote.getTotalRoomNetAmount() : BigDecimal.ZERO)
                .serviceFeeAmount(quote.getTotalRoomServiceFee() != null ? quote.getTotalRoomServiceFee() : BigDecimal.ZERO)
                .totalVatAmount(quote.getTotalRoomVat() != null ? quote.getTotalRoomVat() : BigDecimal.ZERO)
                .totalAmount(quote.getGrandTotal() != null ? quote.getGrandTotal() : BigDecimal.ZERO)
                .build();

        List<BookingDetail> details = new ArrayList<>();
        List<BookingCharge> chargesToSave = new ArrayList<>();

        Map<Integer, RoomPriceQuoteResponse> quoteMap = new HashMap<>();
        if (quote.getItemizedQuotes() != null) {
            for (RoomPriceQuoteResponse rq : quote.getItemizedQuotes()) {
                quoteMap.put(rq.getHotelRoomTypeId(), rq);
            }
        }

        // 4. Snapshot Booking Details, Rooms, StayGuests, DailyRates, Charges
        for (CartPriceQuoteRequest.CartItemQuoteRequest item : draftSession.getItems()) {
            HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(item.getHotelRoomTypeId());

            ResolvedBookingPolicies resolvedPolicies = bookingPolicyResolverService.resolvePolicyForBooking(
                    hotel.getId(), hotelRoomType.getId(), booking.getBookingType());

            BookingDetail detail = BookingDetail.builder()
                    .booking(booking)
                    .hotelRoomType(hotelRoomType)
                    .roomTypeName(hotelRoomType.getRoomType().getName())
                    .quantity(item.getQuantity())
                    .checkInDate(draftSession.getCheckInDate())
                    .checkOutDate(draftSession.getCheckOutDate())
                    .cancellationPolicy(resolvedPolicies != null ? resolvedPolicies.getCancellationPolicy() : null)
                    .noShowPolicy(resolvedPolicies != null ? resolvedPolicies.getNoShowPolicy() : null)
                    .cancellationPolicySnapshot(resolvedPolicies != null ? resolvedPolicies.getCancellationPolicySnapshotJson() : null)
                    .noShowPolicySnapshot(resolvedPolicies != null ? resolvedPolicies.getNoShowPolicySnapshotJson() : null)
                    .build();

            RoomPriceQuoteResponse roomQuote = quoteMap.get(item.getHotelRoomTypeId());
            PriceBreakdownDto breakdown = roomQuote != null ? roomQuote.getPriceBreakdown() : null;

            int qty = item.getQuantity() != null ? item.getQuantity().intValue() : 1;
            List<BookingRoom> bookingRooms = new ArrayList<>();

            for (int i = 0; i < qty; i++) {
                short adults = item.getAdults() != null ? item.getAdults() : (short) 1;
                short children = item.getChildren() != null ? item.getChildren() : (short) 0;
                short infants = item.getInfants() != null ? item.getInfants() : (short) 0;
                short totalGuests = (short) (adults + children + infants);

                BookingRoom bookingRoom = BookingRoom.builder()
                        .bookingDetail(detail)
                        .roomInstance(null)
                        .adultCount(adults)
                        .childCount(children)
                        .infantCount(infants)
                        .guestCount(totalGuests)
                        .status(BookingRoomStatus.EXPECTED)
                        .build();

                List<StayGuest> stayGuests = new ArrayList<>();
                if (i == 0) {
                    StayGuest leadStay = StayGuest.builder()
                            .bookingRoom(bookingRoom)
                            .hotel(hotel)
                            .fullName(leadGuest.getFullName())
                            .guestType(BookingGuestType.ADULT)
                            .build();
                    stayGuests.add(leadStay);
                }
                bookingRoom.setStayGuests(stayGuests);

                List<BookingDailyRate> dailyRates = new ArrayList<>();
                TaxCategory taxCategory = null;
                BigDecimal vatPercent = BigDecimal.valueOf(10);
                if (hotelRoomType.getTaxCategoryId() != null) {
                    taxCategory = taxCategoryService.getEntityById(hotelRoomType.getTaxCategoryId());
                    BigDecimal activeVat = vatRuleService.getActiveVatPercent(hotelRoomType.getTaxCategoryId(), draftSession.getCheckInDate());
                    if (activeVat != null && activeVat.compareTo(BigDecimal.ZERO) > 0) {
                        vatPercent = activeVat;
                    }
                }

                if (breakdown != null && breakdown.getDailyPrices() != null) {
                    for (DailyPriceDto daily : breakdown.getDailyPrices()) {
                        BigDecimal basePrice = daily.getBaseRate();
                        BigDecimal discount = daily.getDiscountAmount() != null ? daily.getDiscountAmount() : BigDecimal.ZERO;
                        BigDecimal surcharge = daily.getSurchargeAmount() != null ? daily.getSurchargeAmount() : BigDecimal.ZERO;
                        BigDecimal netPrice = daily.getNetAmount();

                        BigDecimal roomServiceFee = netPrice.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        BigDecimal taxable = netPrice.add(roomServiceFee);
                        BigDecimal vatAmount = taxable.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                        BookingDailyRate dailyRate = BookingDailyRate.builder()
                                .bookingRoom(bookingRoom)
                                .stayDate(daily.getDate())
                                .basePrice(basePrice)
                                .discountAmount(discount)
                                .surchargeAmount(surcharge)
                                .serviceFeeRate(serviceFeeRate)
                                .serviceFeeAmount(roomServiceFee)
                                .taxCategory(taxCategory)
                                .vatPercent(vatPercent)
                                .vatAmount(vatAmount)
                                .netPrice(netPrice)
                                .status("CONFIRMED")
                                .build();
                        dailyRates.add(dailyRate);
                    }
                }
                bookingRoom.setDailyRates(dailyRates);

                if (roomQuote != null && roomQuote.getSurchargeSummary() != null) {
                    ItemizedSurchargeDto surSummary = roomQuote.getSurchargeSummary();
                    if (surSummary.getExtraBedsSurchargeTotal() != null && surSummary.getExtraBedsSurchargeTotal().compareTo(BigDecimal.ZERO) > 0) {
                        BookingCharge charge = BookingCharge.builder()
                                .bookingRoom(bookingRoom)
                                .chargeType(BookingChargeType.EXTRA_BED)
                                .itemName("Phụ thu giường phụ (Extra Bed)")
                                .quantity(item.getExtraBeds() != null && item.getExtraBeds() > 0 ? (int) item.getExtraBeds() : 1)
                                .unitPrice(surSummary.getExtraBedsSurchargeTotal())
                                .subtotal(surSummary.getExtraBedsSurchargeTotal())
                                .serviceFeeRate(serviceFeeRate)
                                .serviceFeeAmount(surSummary.getExtraBedsSurchargeTotal().multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                                .vatRate(vatPercent)
                                .vatAmount(surSummary.getExtraBedsSurchargeTotal().multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                                .totalAmount(surSummary.getExtraBedsSurchargeTotal())
                                .issuedAt(OffsetDateTime.now())
                                .build();
                        chargesToSave.add(charge);
                    }
                    BigDecimal extraPersonTotal = BigDecimal.ZERO;
                    if (surSummary.getExtraAdultsSurchargeTotal() != null) {
                        extraPersonTotal = extraPersonTotal.add(surSummary.getExtraAdultsSurchargeTotal());
                    }
                    if (surSummary.getExtraChildrenSurchargeTotal() != null) {
                        extraPersonTotal = extraPersonTotal.add(surSummary.getExtraChildrenSurchargeTotal());
                    }
                    if (extraPersonTotal.compareTo(BigDecimal.ZERO) > 0) {
                        BookingCharge charge = BookingCharge.builder()
                                .bookingRoom(bookingRoom)
                                .chargeType(BookingChargeType.EXTRA_PERSON)
                                .itemName("Phụ thu thêm người (Extra Guest)")
                                .quantity(1)
                                .unitPrice(extraPersonTotal)
                                .subtotal(extraPersonTotal)
                                .serviceFeeRate(serviceFeeRate)
                                .serviceFeeAmount(extraPersonTotal.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                                .vatRate(vatPercent)
                                .vatAmount(extraPersonTotal.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                                .totalAmount(extraPersonTotal)
                                .issuedAt(OffsetDateTime.now())
                                .build();
                        chargesToSave.add(charge);
                    }
                }

                bookingRooms.add(bookingRoom);
            }

            detail.setBookingRooms(bookingRooms);
            details.add(detail);
        }

        booking.setBookingDetails(details);
        Booking savedBooking = bookingRepository.save(booking);

        if (!chargesToSave.isEmpty()) {
            bookingChargeService.saveAll(chargesToSave);
        }

        // 5. Snapshot Add-on Service Orders
        if (draftSession.getSelectedServices() != null && !draftSession.getSelectedServices().isEmpty()) {
            List<ServiceOrderCreateRequest.OrderItemRequest> orderItems = draftSession.getSelectedServices().stream()
                    .map(s -> ServiceOrderCreateRequest.OrderItemRequest.builder()
                            .menuId(s.getMenuId())
                            .quantity(s.getQuantity() != null && s.getQuantity() > 0 ? s.getQuantity() : 1)
                            .build())
                    .toList();

            ServiceOrderCreateRequest orderReq = ServiceOrderCreateRequest.builder()
                    .hotelId(hotel.getId())
                    .referenceType(ServiceOrderReferenceType.BOOKING)
                    .referenceId(savedBooking.getId())
                    .items(orderItems)
                    .build();

            serviceOrderService.createOrder(orderReq);
        }

        // 6. Snapshot Voucher Usage & Increment Voucher Count
        if (draftSession.getVoucherCode() != null && !draftSession.getVoucherCode().isBlank()) {
            voucherService.recordVoucherUsage(
                    draftSession.getVoucherCode(),
                    leadGuest,
                    quote.getTotalDiscountAmount() != null ? quote.getTotalDiscountAmount() : BigDecimal.ZERO
            );
        }

        // 7. Commit Room Inventory Holds & Availabilities (Status -> COMMITTED)
        roomAvailabilityService.commitHoldInventory(sessionId);

        // 8. Delete Draft Session from Redis
        redisTemplate.delete(redisKey);

        log.info("Successfully confirmed online booking {} for session {} (Payment ID: {})",
                savedBooking.getBookingNumber(), sessionId, paymentId);

        return bookingMapper.toResponse(savedBooking);
    }

    private String generateBookingNumber() {
        return "BK" + System.currentTimeMillis() + (int) (Math.random() * 900 + 100);
    }

    @Override
    @Transactional
    public int processNoShowBookings(Short hotelId, java.time.LocalDate auditDate) {
        if (hotelId == null || auditDate == null) {
            return 0;
        }

        List<Booking> pendingBookings = bookingRepository.findByHotelIdAndStatusWithDetails(hotelId, BookingStatus.CONFIRMED);
        int noShowCount = 0;

        for (Booking booking : pendingBookings) {
            boolean isNoShow = false;
            if (booking.getBookingDetails() != null) {
                for (BookingDetail detail : booking.getBookingDetails()) {
                    if (!detail.getCheckInDate().isAfter(auditDate)) {
                        boolean anyCheckedIn = detail.getBookingRooms() != null && detail.getBookingRooms().stream()
                                .anyMatch(br -> br.getStatus() == BookingRoomStatus.CHECKED_IN || br.getActualCheckInAt() != null);
                        if (!anyCheckedIn) {
                            isNoShow = true;
                            break;
                        }
                    }
                }
            }

            if (isNoShow) {
                executeNoShowPipeline(booking, "Khách không đến nhận phòng theo lịch (Night Audit)", "SYSTEM_NIGHT_AUDIT");
                noShowCount++;
            }
        }

        log.info("Processed {} No-Show bookings for hotel ID {} on audit date {}", noShowCount, hotelId, auditDate);
        return noShowCount;
    }

    @Override
    @Transactional
    public BookingResponse markBookingAsNoShow(Long id, BookingNoShowRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Lý do khách không đến (No-show) không được để trống");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng ID: " + id));

        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            return bookingMapper.toResponse(booking);
        }

        String username = SecurityUtils.getCurrentUsername().orElse("STAFF");
        Booking saved = executeNoShowPipeline(booking, request.getReason(), username);
        return bookingMapper.toResponse(saved);
    }

    private BigDecimal calculateSingleRoomFirstNightPenalty(BookingRoom room, BookingDetail detail, Booking booking) {
        BigDecimal roomFirstNight = BigDecimal.ZERO;
        LocalDate firstNightDate = detail.getCheckInDate();

        if (room.getDailyRates() != null && !room.getDailyRates().isEmpty()) {
            Optional<BookingDailyRate> firstNightRateOpt = room.getDailyRates().stream()
                    .filter(dr -> dr.getStayDate() != null && dr.getStayDate().equals(firstNightDate))
                    .findFirst();
            BookingDailyRate rate = firstNightRateOpt.orElse(room.getDailyRates().get(0));
            BigDecimal net = rate.getNetPrice() != null ? rate.getNetPrice() : BigDecimal.ZERO;
            BigDecimal fee = rate.getServiceFeeAmount() != null ? rate.getServiceFeeAmount() : BigDecimal.ZERO;
            BigDecimal vat = rate.getVatAmount() != null ? rate.getVatAmount() : BigDecimal.ZERO;
            roomFirstNight = net.add(fee).add(vat);
        } else if (detail.getHotelRoomType() != null && detail.getHotelRoomType().getBasePrice() != null) {
            BigDecimal base = detail.getHotelRoomType().getBasePrice();
            BigDecimal serviceFeeRate = booking.getServiceFeeRate() != null ? booking.getServiceFeeRate() : BigDecimal.ZERO;
            BigDecimal fee = base.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal vatBase = (booking.getSubtotalAmount() != null ? booking.getSubtotalAmount() : BigDecimal.ZERO)
                    .add(booking.getServiceFeeAmount() != null ? booking.getServiceFeeAmount() : BigDecimal.ZERO);
            BigDecimal vatRate = (vatBase.compareTo(BigDecimal.ZERO) > 0 && booking.getTotalVatAmount() != null)
                    ? booking.getTotalVatAmount().divide(vatBase, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal vat = base.add(fee).multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
            roomFirstNight = base.add(fee).add(vat);
        }
        return roomFirstNight;
    }

    private BigDecimal calculateFirstNightPenalty(Booking booking) {
        BigDecimal totalPenalty = BigDecimal.ZERO;
        if (booking.getBookingDetails() == null || booking.getBookingDetails().isEmpty()) {
            return totalPenalty;
        }

        for (BookingDetail detail : booking.getBookingDetails()) {
            if (detail.getBookingRooms() == null) {
                continue;
            }
            for (BookingRoom room : detail.getBookingRooms()) {
                BigDecimal firstNight = calculateSingleRoomFirstNightPenalty(room, detail, booking);
                BigDecimal penalty = bookingPolicyResolverService.calculateSingleRoomNoShowPenalty(
                        room, detail, booking, firstNight);
                totalPenalty = totalPenalty.add(penalty);
            }
        }
        return totalPenalty;
    }

    private Booking executeNoShowPipeline(Booking booking, String reason, String executedBy) {
        // 1. Validation: Không cho phép No-Show nếu đã có phòng CHECKED_IN
        if (booking.getBookingDetails() != null) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                if (detail.getBookingRooms() != null) {
                    boolean anyCheckedIn = detail.getBookingRooms().stream()
                            .anyMatch(br -> br.getStatus() == BookingRoomStatus.CHECKED_IN || br.getActualCheckInAt() != null);
                    if (anyCheckedIn) {
                        throw new AppException(ErrorCode.BOOKING_ALREADY_CHECKED_IN);
                    }
                }
            }
        }

        // 2. Tính tiền phạt đêm đầu tiên (100% first night rate cho tất cả các phòng trong đơn)
        BigDecimal penaltyAmount = calculateFirstNightPenalty(booking);

        // 2.1. Tính tiền dịch vụ đã đặt trước (Strictly Non-refundable và KHÔNG cancel dịch vụ)
        List<ServiceOrderResponse> preorderedOrders = serviceOrderService.getOrdersByBookingId(booking.getId());
        BigDecimal preorderedServicesTotal = BigDecimal.ZERO;
        if (preorderedOrders != null) {
            preorderedServicesTotal = preorderedOrders.stream()
                    .filter(o -> o.getStatus() != ServiceOrderStatus.CANCELLED)
                    .map(ServiceOrderResponse::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Addon charges trên các phòng nếu có (ngoài penalty)
        BigDecimal addonChargesTotal = BigDecimal.ZERO;
        if (booking.getBookingDetails() != null) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                if (detail.getBookingRooms() != null) {
                    for (BookingRoom br : detail.getBookingRooms()) {
                        if (br.getCharges() != null) {
                            for (BookingCharge c : br.getCharges()) {
                                if (c.getChargeType() != BookingChargeType.PENALTY) {
                                    addonChargesTotal = addonChargesTotal.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                                }
                            }
                        }
                    }
                }
            }
        }

        BigDecimal nonRefundableTotal = penaltyAmount.add(preorderedServicesTotal).add(addonChargesTotal);

        // 3. Tạo BookingCharge (charge_type = PENALTY) nếu penaltyAmount > 0
        BookingRoom firstRoom = null;
        if (booking.getBookingDetails() != null) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                if (detail.getBookingRooms() != null && !detail.getBookingRooms().isEmpty()) {
                    firstRoom = detail.getBookingRooms().get(0);
                    break;
                }
            }
        }

        if (firstRoom != null && penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            BookingChargeCreateRequest chargeReq = BookingChargeCreateRequest.builder()
                    .chargeType(BookingChargeType.PENALTY)
                    .itemName(String.format("Phí phạt No-Show (%,.0f đ)", penaltyAmount))
                    .description("Phí phạt khách vắng mặt không nhận phòng: " + reason)
                    .quantity(1)
                    .unitPrice(penaltyAmount)
                    .serviceFeeRate(BigDecimal.ZERO)
                    .vatRate(BigDecimal.ZERO)
                    .build();
            bookingChargeService.createCharge(firstRoom, chargeReq);
        }

        // 4. Release toàn bộ phòng, lịch phòng và hoàn trả kho bán cho TOÀN BỘ các đêm của đơn
        if (booking.getBookingDetails() != null) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                if (detail.getBookingRooms() != null) {
                    for (BookingRoom br : detail.getBookingRooms()) {
                        br.setStatus(BookingRoomStatus.NO_SHOW);
                        bookingRoomService.save(br);

                        // Hủy hàng chờ lưu trú BCA nếu có
                        if (br.getId() != null) {
                            stayGuestService.cancelLodgingQueueForBookingRoom(br.getId());
                        }

                        if (br.getRoomInstance() != null) {
                            // Nhả toàn bộ slots các đêm của đơn
                            roomSlotService.releaseSlots(
                                    br.getRoomInstance().getId(),
                                    detail.getCheckInDate(),
                                    detail.getCheckOutDate()
                            );
                            // Cập nhật trạng thái phòng vật lý về READY
                            roomInstanceService.updateRoomStatusEntity(br.getRoomInstance().getId(), "READY");
                        }
                    }
                }

                // Hoàn trả kho phòng bán hàng cho các đêm của detail
                if (detail.getHotelRoomType() != null) {
                    int roomsCount = detail.getQuantity() != null && detail.getQuantity() > 0 ? detail.getQuantity().intValue() : 1;
                    roomAvailabilityService.releaseBookedInventory(
                            detail.getHotelRoomType().getId(),
                            detail.getCheckInDate(),
                            detail.getCheckOutDate(),
                            roomsCount
                    );
                }
            }
        }

        // 5. Tính tiền dư và tạo bản ghi cần REFUND ở trạng thái PENDING
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (payments != null) {
            totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .map(p -> p.getPaymentPurpose() == PaymentPurpose.REFUND ? p.getTotalAmount().negate() : p.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal surplusAmount = totalPaid.subtract(nonRefundableTotal);
        if (surplusAmount.compareTo(BigDecimal.ZERO) > 0) {
            Payment refundPayment = Payment.builder()
                    .booking(booking)
                    .totalAmount(surplusAmount)
                    .paymentPurpose(PaymentPurpose.REFUND)
                    .paymentMethod(PaymentMethod.OTHER)
                    .paymentProvider("SYSTEM_NO_SHOW_REFUND")
                    .status(PaymentStatus.PENDING)
                    .transactionReference("REFUND-SURPLUS-" + booking.getBookingNumber())
                    .gatewayTxnId("PENDING_FINANCE_SETTLEMENT")
                    .rawCallbackPayload("{\"reason\": \"Hoàn tiền cọc dôi dư do No-Show sau khi trừ phạt đêm đầu và dịch vụ đặt trước\", \"amount\": " + surplusAmount + "}")
                    .build();
            paymentRepository.save(refundPayment);
            log.info("Booking {} có số tiền cọc dư {} đ (đã giữ lại phạt {} đ và dịch vụ {} đ), đã tạo bản ghi REFUND ở trạng thái PENDING",
                    booking.getBookingNumber(), surplusAmount, penaltyAmount, preorderedServicesTotal.add(addonChargesTotal));
        }

        // 6. Cập nhật Booking: status = NO_SHOW và nối thêm ghi chú
        booking.setStatus(BookingStatus.NO_SHOW);
        String noShowNote = String.format("[NO_SHOW - %s] Lý do: %s. Phạt đêm đầu: %,.0f đ. Dịch vụ không hoàn: %,.0f đ. Tiền dư cần refund: %,.0f đ. Thực hiện bởi: %s",
                OffsetDateTime.now().toString(),
                reason,
                penaltyAmount,
                preorderedServicesTotal.add(addonChargesTotal),
                surplusAmount.compareTo(BigDecimal.ZERO) > 0 ? surplusAmount : BigDecimal.ZERO,
                executedBy);
        booking.setNotes(booking.getNotes() != null ? booking.getNotes() + "\n" + noShowNote : noShowNote);
        Booking savedBooking = bookingRepository.save(booking);

        // 7. Ghi nhật ký kiểm toán (AuditLog)
        auditLogService.recordLog(
                null,
                "BOOKING_NO_SHOW",
                "Booking",
                booking.getId().toString(),
                "CONFIRMED",
                noShowNote,
                null,
                executedBy
        );

        return savedBooking;
    }

    @Override
    @Transactional
    public BookingResponse markBookingRoomAsNoShow(Long bookingRoomId, BookingNoShowRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Lý do khách không đến (No-show) không được để trống");
        }

        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);

        // 1. Kiểm tra trạng thái BookingRoom
        if (bookingRoom.getStatus() == BookingRoomStatus.NO_SHOW) {
            Booking booking = bookingRoom.getBookingDetail() != null ? bookingRoom.getBookingDetail().getBooking() : null;
            return booking != null ? bookingMapper.toResponse(booking) : null;
        }
        if (bookingRoom.getStatus() != BookingRoomStatus.EXPECTED) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Chỉ có thể đánh dấu No-Show cho phòng đang ở trạng thái chờ nhận phòng (EXPECTED). Trạng thái hiện tại: " + bookingRoom.getStatus());
        }

        BookingDetail detail = bookingRoom.getBookingDetail();
        Booking booking = detail.getBooking();

        String username = SecurityUtils.getCurrentUsername().orElse("STAFF");

        // 2. Tính tiền phạt No-Show cho riêng phòng này dựa trên chính sách snapshot
        BigDecimal firstNightRate = calculateSingleRoomFirstNightPenalty(bookingRoom, detail, booking);
        BigDecimal penaltyAmount = bookingPolicyResolverService.calculateSingleRoomNoShowPenalty(
                bookingRoom, detail, booking, firstNightRate);

        // 3. Tạo BookingCharge (charge_type = PENALTY) cho phòng này
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            String roomLabel = bookingRoom.getRoomInstance() != null ? bookingRoom.getRoomInstance().getRoomNumber() : detail.getRoomTypeName();
            BookingChargeCreateRequest chargeReq = BookingChargeCreateRequest.builder()
                    .chargeType(BookingChargeType.PENALTY)
                    .itemName(String.format("Phí phạt No-Show phòng %s (%,.0f đ)", roomLabel, penaltyAmount))
                    .description("Phí phạt khách không đến nhận phòng " + roomLabel + ": " + request.getReason())
                    .quantity(1)
                    .unitPrice(penaltyAmount)
                    .serviceFeeRate(BigDecimal.ZERO)
                    .vatRate(BigDecimal.ZERO)
                    .build();
            bookingChargeService.createCharge(bookingRoom, chargeReq);
        }

        // 4. Giải phóng tài nguyên của phòng này
        bookingRoom.setStatus(BookingRoomStatus.NO_SHOW);
        bookingRoomService.save(bookingRoom);

        // Hủy hàng chờ lưu trú BCA nếu có
        stayGuestService.cancelLodgingQueueForBookingRoom(bookingRoom.getId());

        if (bookingRoom.getRoomInstance() != null) {
            // Nhả slots các đêm của phòng
            roomSlotService.releaseSlots(
                    bookingRoom.getRoomInstance().getId(),
                    detail.getCheckInDate(),
                    detail.getCheckOutDate()
            );
            // Cập nhật trạng thái phòng vật lý về READY
            roomInstanceService.updateRoomStatusEntity(bookingRoom.getRoomInstance().getId(), "READY");
        }

        // Hoàn trả kho phòng bán hàng cho 1 phòng của hạng phòng này
        if (detail.getHotelRoomType() != null) {
            roomAvailabilityService.releaseBookedInventory(
                    detail.getHotelRoomType().getId(),
                    detail.getCheckInDate(),
                    detail.getCheckOutDate(),
                    1
            );
        }

        // 5. Kiểm tra trạng thái chung của Booking
        boolean allRoomsNoShowOrCancelled = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .allMatch(br -> br.getStatus() == BookingRoomStatus.NO_SHOW
                        || br.getStatus() == BookingRoomStatus.CANCELLED);

        if (allRoomsNoShowOrCancelled) {
            booking.setStatus(BookingStatus.NO_SHOW);
            // Nếu tất cả các phòng đều không đến, kích hoạt hoàn tiền cọc dôi dư toàn đơn
            List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
            BigDecimal totalPaid = BigDecimal.ZERO;
            if (payments != null) {
                totalPaid = payments.stream()
                        .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                        .map(p -> p.getPaymentPurpose() == PaymentPurpose.REFUND ? p.getTotalAmount().negate() : p.getTotalAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal allPenalties = BigDecimal.ZERO;
            BigDecimal allAddons = BigDecimal.ZERO;
            for (BookingDetail bd : booking.getBookingDetails()) {
                if (bd.getBookingRooms() != null) {
                    for (BookingRoom br : bd.getBookingRooms()) {
                        if (br.getCharges() != null) {
                            for (BookingCharge c : br.getCharges()) {
                                if (c.getChargeType() == BookingChargeType.PENALTY) {
                                    allPenalties = allPenalties.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                                } else {
                                    allAddons = allAddons.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                                }
                            }
                        }
                    }
                }
            }

            List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(booking.getId());
            BigDecimal preorderedTotal = BigDecimal.ZERO;
            if (orders != null) {
                preorderedTotal = orders.stream()
                        .filter(o -> o.getStatus() != ServiceOrderStatus.CANCELLED)
                        .map(ServiceOrderResponse::getTotalAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal nonRefundableTotal = allPenalties.add(preorderedTotal).add(allAddons);
            BigDecimal surplus = totalPaid.subtract(nonRefundableTotal);
            if (surplus.compareTo(BigDecimal.ZERO) > 0) {
                Payment refundPayment = Payment.builder()
                        .booking(booking)
                        .totalAmount(surplus)
                        .paymentPurpose(PaymentPurpose.REFUND)
                        .paymentMethod(PaymentMethod.OTHER)
                        .paymentProvider("SYSTEM_NO_SHOW_REFUND")
                        .status(PaymentStatus.PENDING)
                        .transactionReference("REFUND-SURPLUS-" + booking.getBookingNumber())
                        .gatewayTxnId("PENDING_FINANCE_SETTLEMENT")
                        .rawCallbackPayload("{\"reason\": \"Hoàn tiền cọc dôi dư do tất cả các phòng No-Show\", \"amount\": " + surplus + "}")
                        .build();
                paymentRepository.save(refundPayment);
            }
        }

        String logNote = String.format("[ROOM_NO_SHOW - %s] Phòng ID %s bị No-Show. Lý do: %s. Phạt: %,.0f đ. Bởi: %s",
                OffsetDateTime.now().toString(),
                bookingRoomId,
                request.getReason(),
                penaltyAmount,
                username);
        booking.setNotes(booking.getNotes() != null ? booking.getNotes() + "\n" + logNote : logNote);
        Booking saved = bookingRepository.save(booking);

        auditLogService.recordLog(
                null,
                "BOOKING_ROOM_NO_SHOW",
                "BookingRoom",
                bookingRoomId.toString(),
                "EXPECTED",
                logNote,
                null,
                username
        );

        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SurchargeProposalResponse previewSurcharge(Long bookingRoomId, SurchargePreviewRequest request) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);
        BookingDetail detail = bookingRoom.getBookingDetail();
        BookingChargeType chargeType = request.getChargeType();

        OffsetDateTime actualTime = request.getActualTime() != null ? request.getActualTime() : OffsetDateTime.now();
        LocalTime timeOnly = actualTime.toLocalTime();
        LocalDate dateOnly = actualTime.toLocalDate();

        // 1. Tính đơn giá cơ sở của phòng 1 đêm từ daily rates hoặc net price
        BigDecimal baseRoomRate = BigDecimal.ZERO;
        if (bookingRoom.getDailyRates() != null && !bookingRoom.getDailyRates().isEmpty()) {
            BigDecimal sum = bookingRoom.getDailyRates().stream()
                    .map(BookingDailyRate::getNetPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            baseRoomRate = sum.divide(BigDecimal.valueOf(bookingRoom.getDailyRates().size()), 2, RoundingMode.HALF_UP);
        } else if (bookingRoom.getRoomInstance() != null && bookingRoom.getRoomInstance().getHotelRoomType() != null) {
            baseRoomRate = bookingRoom.getRoomInstance().getHotelRoomType().getBasePrice();
        }
        if (baseRoomRate == null || baseRoomRate.compareTo(BigDecimal.ZERO) <= 0) {
            baseRoomRate = new BigDecimal("1000000.00");
        }

        Integer hotelRoomTypeId = null;
        if (bookingRoom.getRoomInstance() != null && bookingRoom.getRoomInstance().getHotelRoomType() != null) {
            hotelRoomTypeId = bookingRoom.getRoomInstance().getHotelRoomType().getId();
        } else if (detail.getHotelRoomType() != null) {
            hotelRoomTypeId = detail.getHotelRoomType().getId();
        }

        Optional<SurchargeRule> configuredRuleOpt = Optional.empty();
        if (hotelRoomTypeId != null && surchargeRuleRepository != null) {
            configuredRuleOpt = surchargeRuleRepository.findActiveRuleForRoomTypeAndDate(
                    hotelRoomTypeId, chargeType.name(), dateOnly
            );
        }

        BigDecimal percentage = BigDecimal.ZERO;
        BigDecimal fixedAmount = null;
        String policyRule = "";
        String standardTime = chargeType == BookingChargeType.EARLY_CHECKIN ? "14:00" : "12:00";
        long diffMinutes = 0;
        boolean applicable = false;

        // Đọc cấu hình JSON conditions nếu có
        JsonNode conditionsNode = null;
        if (configuredRuleOpt.isPresent() && configuredRuleOpt.get().getConditions() != null) {
            try {
                conditionsNode = objectMapper.readTree(configuredRuleOpt.get().getConditions());
                if (conditionsNode.has("standard_time")) {
                    standardTime = conditionsNode.get("standard_time").asText();
                }
            } catch (Exception e) {
                log.warn("Lỗi đọc conditions JSON của surcharge rule id {}: {}", configuredRuleOpt.get().getId(), e.getMessage());
            }
        }

        LocalTime standardLocalTime;
        try {
            standardLocalTime = LocalTime.parse(standardTime);
        } catch (Exception e) {
            standardLocalTime = chargeType == BookingChargeType.EARLY_CHECKIN ? LocalTime.of(14, 0) : LocalTime.of(12, 0);
        }

        if (chargeType == BookingChargeType.EARLY_CHECKIN) {
            if (dateOnly.isBefore(detail.getCheckInDate()) || (dateOnly.isEqual(detail.getCheckInDate()) && timeOnly.isBefore(standardLocalTime))) {
                applicable = true;
                diffMinutes = Duration.between(actualTime, detail.getCheckInDate().atTime(standardLocalTime).atOffset(actualTime.getOffset())).toMinutes();

                // Kiểm tra xem trong conditions có cấu hình các bậc thời gian (tiers) động không
                boolean tierMatched = false;
                if (conditionsNode != null && conditionsNode.has("tiers") && conditionsNode.get("tiers").isArray()) {
                    for (JsonNode tier : conditionsNode.get("tiers")) {
                        LocalTime from = tier.has("from_time") ? LocalTime.parse(tier.get("from_time").asText()) : LocalTime.MIN;
                        LocalTime to = tier.has("to_time") ? LocalTime.parse(tier.get("to_time").asText()) : standardLocalTime;
                        if (!timeOnly.isBefore(from) && !timeOnly.isAfter(to)) {
                            tierMatched = true;
                            if ("PERCENT".equalsIgnoreCase(tier.path("adjustment_type").asText("PERCENT"))) {
                                percentage = new BigDecimal(tier.path("adjustment_value").asText("30.0"));
                            } else {
                                fixedAmount = new BigDecimal(tier.path("adjustment_value").asText("0"));
                            }
                            policyRule = tier.has("description") ? tier.get("description").asText() : "Phụ thu nhận phòng sớm theo cấu hình hạng phòng";
                            break;
                        }
                    }
                }

                if (!tierMatched) {
                    if (configuredRuleOpt.isPresent() && !"PERCENT".equalsIgnoreCase(configuredRuleOpt.get().getAdjustmentType())) {
                        fixedAmount = configuredRuleOpt.get().getAdjustmentValue();
                        policyRule = "Phụ thu nhận phòng sớm cố định theo cấu hình hạng phòng";
                    } else if (configuredRuleOpt.isPresent()) {
                        percentage = configuredRuleOpt.get().getAdjustmentValue();
                        policyRule = "Phụ thu nhận phòng sớm theo tỷ lệ cấu hình hạng phòng (" + percentage + "%)";
                    } else {
                        // Fallback chuẩn toàn hệ thống
                        if (timeOnly.isBefore(LocalTime.of(6, 0))) {
                            percentage = new BigDecimal("100.0");
                            policyRule = "Nhận phòng trước 06:00 (Phụ thu 100% giá phòng 1 đêm)";
                        } else if (timeOnly.isBefore(LocalTime.of(9, 0))) {
                            percentage = new BigDecimal("50.0");
                            policyRule = "Nhận phòng từ 06:00 đến 09:00 (Phụ thu 50% giá phòng 1 đêm)";
                        } else {
                            percentage = new BigDecimal("30.0");
                            policyRule = "Nhận phòng từ 09:00 đến 14:00 (Phụ thu 30% giá phòng 1 đêm)";
                        }
                    }
                }
            } else {
                policyRule = "Khách nhận phòng đúng hoặc sau giờ quy định (" + standardTime + ") — Không phát sinh phụ thu";
            }
        } else if (chargeType == BookingChargeType.LATE_CHECKOUT) {
            if (dateOnly.isAfter(detail.getCheckOutDate()) || (dateOnly.isEqual(detail.getCheckOutDate()) && timeOnly.isAfter(standardLocalTime))) {
                applicable = true;
                diffMinutes = Duration.between(detail.getCheckOutDate().atTime(standardLocalTime).atOffset(actualTime.getOffset()), actualTime).toMinutes();

                // Kiểm tra xem trong conditions có cấu hình các bậc thời gian (tiers) động không
                boolean tierMatched = false;
                if (conditionsNode != null && conditionsNode.has("tiers") && conditionsNode.get("tiers").isArray()) {
                    for (JsonNode tier : conditionsNode.get("tiers")) {
                        LocalTime from = tier.has("from_time") ? LocalTime.parse(tier.get("from_time").asText()) : standardLocalTime;
                        LocalTime to = tier.has("to_time") ? LocalTime.parse(tier.get("to_time").asText()) : LocalTime.MAX;
                        if (!timeOnly.isBefore(from) && !timeOnly.isAfter(to)) {
                            tierMatched = true;
                            if ("PERCENT".equalsIgnoreCase(tier.path("adjustment_type").asText("PERCENT"))) {
                                percentage = new BigDecimal(tier.path("adjustment_value").asText("30.0"));
                            } else {
                                fixedAmount = new BigDecimal(tier.path("adjustment_value").asText("0"));
                            }
                            policyRule = tier.has("description") ? tier.get("description").asText() : "Phụ thu trả phòng muộn theo cấu hình hạng phòng";
                            break;
                        }
                    }
                }

                if (!tierMatched) {
                    if (configuredRuleOpt.isPresent() && !"PERCENT".equalsIgnoreCase(configuredRuleOpt.get().getAdjustmentType())) {
                        fixedAmount = configuredRuleOpt.get().getAdjustmentValue();
                        policyRule = "Phụ thu trả phòng muộn cố định theo cấu hình hạng phòng";
                    } else if (configuredRuleOpt.isPresent()) {
                        percentage = configuredRuleOpt.get().getAdjustmentValue();
                        policyRule = "Phụ thu trả phòng muộn theo tỷ lệ cấu hình hạng phòng (" + percentage + "%)";
                    } else {
                        // Fallback chuẩn toàn hệ thống
                        if (timeOnly.isAfter(LocalTime.of(18, 0)) || dateOnly.isAfter(detail.getCheckOutDate())) {
                            percentage = new BigDecimal("100.0");
                            policyRule = "Trả phòng sau 18:00 (Phụ thu 100% giá phòng 1 đêm)";
                        } else if (timeOnly.isAfter(LocalTime.of(15, 0))) {
                            percentage = new BigDecimal("50.0");
                            policyRule = "Trả phòng từ 15:00 đến 18:00 (Phụ thu 50% giá phòng 1 đêm)";
                        } else {
                            percentage = new BigDecimal("30.0");
                            policyRule = "Trả phòng từ 12:00 đến 15:00 (Phụ thu 30% giá phòng 1 đêm)";
                        }
                    }
                }
            } else {
                policyRule = "Khách trả phòng đúng hoặc trước giờ quy định (" + standardTime + ") — Không phát sinh phụ thu";
            }
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Chỉ hỗ trợ tính phụ thu tự động cho EARLY_CHECKIN hoặc LATE_CHECKOUT");
        }

        BigDecimal suggestedAmount;
        if (fixedAmount != null) {
            suggestedAmount = fixedAmount;
        } else {
            suggestedAmount = baseRoomRate.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        String suggestedItemName = chargeType == BookingChargeType.EARLY_CHECKIN
                ? "Phụ thu nhận phòng sớm (" + (fixedAmount != null ? (String.format("%,.0f", fixedAmount) + " đ") : (percentage.stripTrailingZeros().toPlainString() + "% giá phòng")) + ")"
                : "Phụ thu trả phòng muộn (" + (fixedAmount != null ? (String.format("%,.0f", fixedAmount) + " đ") : (percentage.stripTrailingZeros().toPlainString() + "% giá phòng")) + ")";

        return SurchargeProposalResponse.builder()
                .applicable(applicable)
                .chargeType(chargeType)
                .standardTime(standardTime)
                .actualTime(actualTime)
                .timeDifferenceMinutes(diffMinutes)
                .policyRule(policyRule)
                .baseRoomRate(baseRoomRate)
                .suggestedPercentage(percentage)
                .suggestedAmount(suggestedAmount)
                .suggestedItemName(suggestedItemName)
                .defaultServiceFeeRate(new BigDecimal("5.00"))
                .defaultVatRate(new BigDecimal("8.00"))
                .build();
    }

    @Override
    @Transactional
    public void deleteCharge(Long chargeId) {
        BookingCharge charge = bookingChargeService.getById(chargeId);
        Booking booking = charge.getBookingRoom().getBookingDetail().getBooking();

        // Trừ tiền khỏi tổng tiền của đơn đặt phòng
        booking.setSubtotalAmount(booking.getSubtotalAmount().subtract(charge.getSubtotal()));
        booking.setServiceFeeAmount(booking.getServiceFeeAmount().subtract(charge.getServiceFeeAmount()));
        booking.setTotalVatAmount(booking.getTotalVatAmount().subtract(charge.getVatAmount()));
        booking.setTotalAmount(booking.getTotalAmount().subtract(charge.getTotalAmount()));
        bookingRepository.save(booking);

        bookingChargeService.deleteById(chargeId);
    }

    @Override
    @Transactional(readOnly = true)
    public WalkInQuoteResponse calculateWalkInQuote(WalkInQuoteRequest request) {
        if (request.getRooms() == null || request.getRooms().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Danh sách phòng không được để trống");
        }
        if (request.getRooms().size() > 5) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_ROOMS);
        }
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_DATES, "Ngày nhận phòng phải trước ngày trả phòng");
        }

        Hotel hotel = hotelService.getEntityById(request.getHotelId());
        LocalTime actualTime = request.getActualCheckInTime() != null ? request.getActualCheckInTime() : LocalTime.now();

        List<WalkInQuoteResponse.WalkInQuoteRoomItemResponse> roomItemResponses = new ArrayList<>();
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalServiceFee = BigDecimal.ZERO;
        BigDecimal totalSurcharges = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (int i = 0; i < request.getRooms().size(); i++) {
            WalkInQuoteRequest.WalkInQuoteRoomItemRequest roomReq = request.getRooms().get(i);
            HotelRoomType roomType = hotelRoomTypeService.getEntityById(roomReq.getHotelRoomTypeId());

            short adults = roomReq.getAdultCount() != null ? roomReq.getAdultCount() : (short) 1;
            short children = roomReq.getChildCount() != null ? roomReq.getChildCount() : (short) 0;
            short infants = roomReq.getInfantCount() != null ? roomReq.getInfantCount() : (short) 0;
            short extraBeds = roomReq.getExtraBeds() != null ? roomReq.getExtraBeds() : (short) 0;

            PriceCalculationRequest priceReq = PriceCalculationRequest.builder()
                    .hotelRoomTypeId(roomType.getId())
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .adults(adults)
                    .children(children)
                    .infants(infants)
                    .extraBeds(extraBeds)
                    .build();

            PriceBreakdownDto breakdown = priceEngine.calculatePrice(priceReq);

            BigDecimal netPrice = breakdown.getTotalRoomNetAmount() != null ? breakdown.getTotalRoomNetAmount() : BigDecimal.ZERO;
            BigDecimal vatAmount = breakdown.getTotalRoomVat() != null ? breakdown.getTotalRoomVat() : BigDecimal.ZERO;
            BigDecimal serviceFeeAmount = breakdown.getTotalRoomServiceFee() != null ? breakdown.getTotalRoomServiceFee() : BigDecimal.ZERO;
            BigDecimal roomTotal = breakdown.getTotalRoomGrossAmount() != null ? breakdown.getTotalRoomGrossAmount() : BigDecimal.ZERO;

            totalSubtotal = totalSubtotal.add(netPrice);
            totalVat = totalVat.add(vatAmount);
            totalServiceFee = totalServiceFee.add(serviceFeeAmount);
            totalAmount = totalAmount.add(roomTotal);

            ItemizedSurchargeDto capSurcharge = ItemizedSurchargeDto.builder()
                    .extraAdultsCount(adults > (roomType.getStandardAdults() != null ? roomType.getStandardAdults() : 2)
                            ? (short) (adults - (roomType.getStandardAdults() != null ? roomType.getStandardAdults() : 2))
                            : (short) 0)
                    .extraAdultsSurchargeTotal(breakdown.getTotalExtraAdultsSurcharge() != null ? breakdown.getTotalExtraAdultsSurcharge() : BigDecimal.ZERO)
                    .extraChildrenCount(children)
                    .extraChildrenSurchargeTotal(breakdown.getTotalExtraChildrenSurcharge() != null ? breakdown.getTotalExtraChildrenSurcharge() : BigDecimal.ZERO)
                    .infantsCount(infants)
                    .infantsSurchargeTotal(BigDecimal.ZERO)
                    .extraBedsCount(extraBeds)
                    .extraBedsSurchargeTotal(breakdown.getTotalExtraBedsSurcharge() != null ? breakdown.getTotalExtraBedsSurcharge() : BigDecimal.ZERO)
                    .totalSurchargeAmount(breakdown.getTotalSurchargeAmount() != null ? breakdown.getTotalSurchargeAmount() : BigDecimal.ZERO)
                    .build();

            if (breakdown.getTotalSurchargeAmount() != null) {
                totalSurcharges = totalSurcharges.add(breakdown.getTotalSurchargeAmount());
            }

            int nights = breakdown.getTotalNights() != null && breakdown.getTotalNights() > 0 ? breakdown.getTotalNights() : 1;
            BigDecimal baseDailyRate = netPrice.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
            SurchargeProposalResponse earlyProposal = evaluateEarlyCheckinProposal(
                    roomType.getId(),
                    request.getCheckInDate(),
                    actualTime,
                    baseDailyRate,
                    hotel
            );

            WalkInQuoteResponse.WalkInQuoteRoomItemResponse roomItem = WalkInQuoteResponse.WalkInQuoteRoomItemResponse.builder()
                    .roomIndex(i)
                    .hotelRoomTypeId(roomType.getId())
                    .roomTypeName(roomType.getRoomType().getName())
                    .adultCount(adults)
                    .childCount(children)
                    .infantCount(infants)
                    .extraBeds(extraBeds)
                    .dailyRates(breakdown.getDailyPrices())
                    .netPrice(netPrice)
                    .vatAmount(vatAmount)
                    .serviceFeeAmount(serviceFeeAmount)
                    .totalPrice(roomTotal)
                    .capacitySurcharge(capSurcharge)
                    .earlyCheckinProposal(earlyProposal)
                    .build();

            roomItemResponses.add(roomItem);
        }

        return WalkInQuoteResponse.builder()
                .hotelId(hotel.getId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .rooms(roomItemResponses)
                .subtotalAmount(totalSubtotal)
                .totalVatAmount(totalVat)
                .totalServiceFeeAmount(totalServiceFee)
                .totalSurchargesAmount(totalSurcharges)
                .totalAmount(totalAmount)
                .build();
    }

    private SurchargeProposalResponse evaluateEarlyCheckinProposal(
            Integer hotelRoomTypeId,
            LocalDate checkInDate,
            LocalTime actualTime,
            BigDecimal baseRoomRate,
            Hotel hotel
    ) {
        String standardTime = "14:00";
        if (hotel.getCheckInTime() != null) {
            standardTime = hotel.getCheckInTime().toString();
        }

        Optional<SurchargeRule> configuredRuleOpt = Optional.empty();
        if (hotelRoomTypeId != null && surchargeRuleRepository != null) {
            configuredRuleOpt = surchargeRuleRepository.findActiveRuleForRoomTypeAndDate(
                    hotelRoomTypeId, BookingChargeType.EARLY_CHECKIN.name(), checkInDate
            );
        }

        JsonNode conditionsNode = null;
        if (configuredRuleOpt.isPresent() && configuredRuleOpt.get().getConditions() != null) {
            try {
                conditionsNode = objectMapper.readTree(configuredRuleOpt.get().getConditions());
                if (conditionsNode.has("standard_time")) {
                    standardTime = conditionsNode.get("standard_time").asText();
                }
            } catch (Exception e) {
                log.warn("Lỗi đọc conditions JSON của surcharge rule: {}", e.getMessage());
            }
        }

        LocalTime standardLocalTime;
        try {
            standardLocalTime = LocalTime.parse(standardTime);
        } catch (Exception e) {
            standardLocalTime = LocalTime.of(14, 0);
        }

        BigDecimal percentage = BigDecimal.ZERO;
        BigDecimal fixedAmount = null;
        String policyRule = "";
        long diffMinutes = 0;
        boolean applicable = false;

        OffsetDateTime actualOffsetTime = checkInDate.atTime(actualTime).atOffset(ZoneOffset.ofHours(7));

        if (actualTime.isBefore(standardLocalTime)) {
            applicable = true;
            diffMinutes = Duration.between(actualTime, standardLocalTime).toMinutes();

            boolean tierMatched = false;
            if (conditionsNode != null && conditionsNode.has("tiers") && conditionsNode.get("tiers").isArray()) {
                for (JsonNode tier : conditionsNode.get("tiers")) {
                    LocalTime from = tier.has("from_time") ? LocalTime.parse(tier.get("from_time").asText()) : LocalTime.MIN;
                    LocalTime to = tier.has("to_time") ? LocalTime.parse(tier.get("to_time").asText()) : standardLocalTime;
                    if (!actualTime.isBefore(from) && !actualTime.isAfter(to)) {
                        tierMatched = true;
                        if ("PERCENT".equalsIgnoreCase(tier.path("adjustment_type").asText("PERCENT"))) {
                            percentage = new BigDecimal(tier.path("adjustment_value").asText("30.0"));
                        } else {
                            fixedAmount = new BigDecimal(tier.path("adjustment_value").asText("0"));
                        }
                        policyRule = tier.has("description") ? tier.get("description").asText() : "Phụ thu nhận phòng sớm theo cấu hình hạng phòng";
                        break;
                    }
                }
            }

            if (!tierMatched) {
                if (configuredRuleOpt.isPresent() && !"PERCENT".equalsIgnoreCase(configuredRuleOpt.get().getAdjustmentType())) {
                    fixedAmount = configuredRuleOpt.get().getAdjustmentValue();
                    policyRule = "Phụ thu nhận phòng sớm cố định theo cấu hình hạng phòng";
                } else if (configuredRuleOpt.isPresent()) {
                    percentage = configuredRuleOpt.get().getAdjustmentValue();
                    policyRule = "Phụ thu nhận phòng sớm theo tỷ lệ cấu hình hạng phòng (" + percentage + "%)";
                } else {
                    if (actualTime.isBefore(LocalTime.of(6, 0))) {
                        percentage = new BigDecimal("100.0");
                        policyRule = "Nhận phòng trước 06:00 (Phụ thu 100% giá phòng 1 đêm)";
                    } else if (actualTime.isBefore(LocalTime.of(9, 0))) {
                        percentage = new BigDecimal("50.0");
                        policyRule = "Nhận phòng từ 06:00 đến 09:00 (Phụ thu 50% giá phòng 1 đêm)";
                    } else {
                        percentage = new BigDecimal("30.0");
                        policyRule = "Nhận phòng từ 09:00 đến 14:00 (Phụ thu 30% giá phòng 1 đêm)";
                    }
                }
            }
        } else {
            policyRule = "Khách nhận phòng đúng hoặc sau giờ quy định (" + standardTime + ") — Không phát sinh phụ thu";
        }

        BigDecimal suggestedAmount = BigDecimal.ZERO;
        String suggestedItemName = "Phụ thu nhận phòng sớm (Early Check-in)";
        if (applicable) {
            if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) > 0) {
                suggestedAmount = fixedAmount;
                suggestedItemName = "Phụ thu nhận phòng sớm (Mức cố định: " + String.format("%,.0f", fixedAmount) + " đ)";
            } else if (percentage.compareTo(BigDecimal.ZERO) > 0) {
                suggestedAmount = baseRoomRate.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                suggestedItemName = "Phụ thu nhận phòng sớm (" + percentage.stripTrailingZeros().toPlainString() + "% giá phòng 1 đêm)";
            }
        }

        return SurchargeProposalResponse.builder()
                .applicable(applicable)
                .chargeType(BookingChargeType.EARLY_CHECKIN)
                .standardTime(standardTime)
                .actualTime(actualOffsetTime)
                .timeDifferenceMinutes(diffMinutes)
                .policyRule(policyRule)
                .baseRoomRate(baseRoomRate)
                .suggestedPercentage(percentage)
                .suggestedAmount(suggestedAmount)
                .suggestedItemName(suggestedItemName)
                .defaultServiceFeeRate(new BigDecimal("5.00"))
                .defaultVatRate(new BigDecimal("8.00"))
                .build();
    }

    @Override
    @Transactional
    public BookingResponse createWalkInBooking(WalkInBookingRequest request) {
        if (request.getRooms() == null || request.getRooms().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Danh sách phòng đặt không được để trống");
        }
        if (request.getRooms().size() > 5) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_ROOMS);
        }
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_DATES, "Ngày nhận phòng phải trước ngày trả phòng");
        }

        Hotel hotel = hotelService.getEntityById(request.getHotelId());

        // 1. Phân loại Company & BookingGuest
        boolean hasCompany = request.getCompanyId() != null
                || (request.getNewCompany() != null && StringUtils.hasText(request.getNewCompany().getName()));
        boolean isSmallOrFit = request.getRooms().size() <= 2 && !hasCompany;

        // Xử lý Company
        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyService.getEntityById(request.getCompanyId());
        } else if (request.getNewCompany() != null && StringUtils.hasText(request.getNewCompany().getName())) {
            CompanyCreateRequest compReq = CompanyCreateRequest.builder()
                    .name(request.getNewCompany().getName())
                    .taxCode(request.getNewCompany().getTaxCode())
                    .address(request.getNewCompany().getAddress())
                    .contactName(request.getNewCompany().getContactName())
                    .contactPhone(request.getNewCompany().getContactPhone())
                    .contactEmail(request.getNewCompany().getContactEmail())
                    .build();
            CompanyResponse compResp = companyService.create(compReq);
            company = companyService.getEntityById(compResp.getId());
        }

        // Xử lý BookingGuest (Người đặt / Đại diện)
        BookingGuest bookingGuest;
        if (request.getGuestId() != null) {
            bookingGuest = guestService.getEntityById(request.getGuestId());
        } else if (request.getNewGuest() != null && StringUtils.hasText(request.getNewGuest().getPhone())) {
            bookingGuest = guestService.getOrCreateGuest(
                    request.getNewGuest().getPhone(),
                    request.getNewGuest().getFullName(),
                    request.getNewGuest().getEmail(),
                    request.getNewGuest().getNationality() != null ? request.getNewGuest().getNationality() : "Việt Nam"
            );
            if (request.getNewGuest().getIdentityNumber() != null) {
                bookingGuest.setIdentityNumber(request.getNewGuest().getIdentityNumber());
            }
            if (request.getNewGuest().getIdentityType() != null) {
                bookingGuest.setIdentityType(request.getNewGuest().getIdentityType());
            }
            if (request.getNewGuest().getBirthDate() != null) {
                bookingGuest.setBirthDate(request.getNewGuest().getBirthDate());
            }
        } else {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Vui lòng cung cấp thông tin người đặt / đại diện thanh toán");
        }

        // 2. Sequential Lock Ordering & Kiểm tra phòng vật lý READY
        List<WalkInBookingRequest.WalkInRoomItemRequest> sortedRooms = new ArrayList<>(request.getRooms());
        sortedRooms.sort(Comparator.comparing(WalkInBookingRequest.WalkInRoomItemRequest::getRoomInstanceId));

        Map<Integer, RoomInstance> roomInstanceMap = new HashMap<>();
        for (WalkInBookingRequest.WalkInRoomItemRequest rReq : sortedRooms) {
            RoomInstance roomInst = roomInstanceService.getEntityById(rReq.getRoomInstanceId());

            if (!roomInst.getHotel().getId().equals(hotel.getId())) {
                throw new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Phòng " + roomInst.getRoomNumber() + " không thuộc khách sạn này");
            }
            if (!roomInst.getHotelRoomType().getId().equals(rReq.getHotelRoomTypeId())) {
                throw new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND, "Phòng " + roomInst.getRoomNumber() + " không đúng hạng phòng yêu cầu");
            }
            if (!"READY".equalsIgnoreCase(roomInst.getCurrentStatus())) {
                throw new AppException(ErrorCode.ROOM_NOT_READY_FOR_CHECKIN,
                        "Phòng vật lý " + roomInst.getRoomNumber() + " đang ở trạng thái '" + roomInst.getCurrentStatus() + "'. Chỉ có thể nhận phòng với phòng sẵn sàng (READY).");
            }

            List<BookingRoom> overlapping = bookingRoomService.findOverlappingAssignedRooms(
                    roomInst.getId(), request.getCheckInDate(), request.getCheckOutDate());
            if (!overlapping.isEmpty()) {
                throw new AppException(ErrorCode.ROOM_ALREADY_ASSIGNED,
                        "Phòng " + roomInst.getRoomNumber() + " đã có khách đặt trong khoảng thời gian này");
            }

            roomInstanceMap.put(roomInst.getId(), roomInst);
        }

        // 3. Tính toán chi phí sơ bộ để kiểm tra quy tắc thanh toán
        BigDecimal calculatedRoomTotal = BigDecimal.ZERO;
        for (WalkInBookingRequest.WalkInRoomItemRequest rReq : sortedRooms) {
            PriceCalculationRequest priceReq = PriceCalculationRequest.builder()
                    .hotelRoomTypeId(rReq.getHotelRoomTypeId())
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .adults(rReq.getAdultCount() != null ? rReq.getAdultCount() : (short) 1)
                    .children(rReq.getChildCount() != null ? rReq.getChildCount() : (short) 0)
                    .infants(rReq.getInfantCount() != null ? rReq.getInfantCount() : (short) 0)
                    .extraBeds(rReq.getExtraBeds() != null ? rReq.getExtraBeds() : (short) 0)
                    .build();
            PriceBreakdownDto breakdown = priceEngine.calculatePrice(priceReq);
            calculatedRoomTotal = calculatedRoomTotal.add(breakdown.getTotalRoomGrossAmount() != null ? breakdown.getTotalRoomGrossAmount() : BigDecimal.ZERO);
        }

        BigDecimal calculatedChargesTotal = BigDecimal.ZERO;
        if (request.getConfirmedCharges() != null) {
            for (WalkInBookingRequest.WalkInConfirmedChargeRequest c : request.getConfirmedCharges()) {
                if (!Boolean.TRUE.equals(c.getIsWaived()) && c.getAmount() != null) {
                    calculatedChargesTotal = calculatedChargesTotal.add(c.getAmount());
                }
            }
        }

        BigDecimal totalExpected = calculatedRoomTotal.add(calculatedChargesTotal);

        // 4. RÀNG BUỘC THANH TOÁN (Payment Enforcement)
        if (request.getPayment() == null || request.getPayment().getAmount() == null) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT, "Thông tin thanh toán không được để trống");
        }

        BigDecimal paidAmount = request.getPayment().getAmount();
        String paymentPurposeStr = request.getPayment().getPaymentPurpose() != null ? request.getPayment().getPaymentPurpose() : "FULL_PAYMENT";

        if (isSmallOrFit) {
            if (!"FULL_PAYMENT".equalsIgnoreCase(paymentPurposeStr) || paidAmount.compareTo(totalExpected) < 0) {
                throw new AppException(ErrorCode.WALK_IN_REQUIRE_FULL_PAYMENT,
                        "Khách vãng lai đặt ít phòng (≤ 2 phòng) bắt buộc thanh toán 100% khi nhận phòng (Cần: " + String.format("%,.0f", totalExpected) + " đ, Thực trả: " + String.format("%,.0f", paidAmount) + " đ)");
            }
        } else {
            if ("DEPOSIT".equalsIgnoreCase(paymentPurposeStr)) {
                BigDecimal minDeposit = totalExpected.multiply(new BigDecimal("0.50"));
                if (paidAmount.compareTo(minDeposit) < 0) {
                    throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT,
                            "Số tiền đặt cọc tối thiểu là 50% tổng chi phí (" + String.format("%,.0f", minDeposit) + " đ)");
                }
            }
        }

        // 5. Khởi tạo Booking Entity
        BookingType bType = hasCompany ? BookingType.CORPORATE : (request.getBookingType() != null ? request.getBookingType() : BookingType.FIT);
        Booking booking = Booking.builder()
                .hotel(hotel)
                .bookingGuest(bookingGuest)
                .company(company)
                .bookingType(bType)
                .bookingNumber(generateBookingNumber())
                .status(BookingStatus.CONFIRMED)
                .issuedAt(OffsetDateTime.now())
                .notes(request.getNotes())
                .subtotalAmount(BigDecimal.ZERO)
                .serviceFeeAmount(BigDecimal.ZERO)
                .totalVatAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal bookingSubtotal = BigDecimal.ZERO;
        BigDecimal bookingVatTotal = BigDecimal.ZERO;
        List<BookingDetail> details = new ArrayList<>();
        List<BookingRoom> allCreatedBookingRooms = new ArrayList<>();

        for (WalkInBookingRequest.WalkInRoomItemRequest rReq : sortedRooms) {
            HotelRoomType roomType = hotelRoomTypeService.getEntityById(rReq.getHotelRoomTypeId());
            RoomInstance roomInst = roomInstanceMap.get(rReq.getRoomInstanceId());

            roomAvailabilityService.confirmBookingInventory(roomType.getId(), request.getCheckInDate(), request.getCheckOutDate(), 1);

            PriceCalculationRequest priceReq = PriceCalculationRequest.builder()
                    .hotelRoomTypeId(roomType.getId())
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .adults(rReq.getAdultCount() != null ? rReq.getAdultCount() : (short) 1)
                    .children(rReq.getChildCount() != null ? rReq.getChildCount() : (short) 0)
                    .infants(rReq.getInfantCount() != null ? rReq.getInfantCount() : (short) 0)
                    .extraBeds(rReq.getExtraBeds() != null ? rReq.getExtraBeds() : (short) 0)
                    .build();
            PriceBreakdownDto breakdown = priceEngine.calculatePrice(priceReq);

            ResolvedBookingPolicies resolvedPolicies = bookingPolicyResolverService.resolvePolicyForBooking(
                    hotel.getId(), roomType.getId(), bType);

            BookingDetail detail = BookingDetail.builder()
                    .booking(booking)
                    .hotelRoomType(roomType)
                    .roomTypeName(roomType.getRoomType().getName())
                    .quantity((short) 1)
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .cancellationPolicy(resolvedPolicies != null ? resolvedPolicies.getCancellationPolicy() : null)
                    .noShowPolicy(resolvedPolicies != null ? resolvedPolicies.getNoShowPolicy() : null)
                    .cancellationPolicySnapshot(resolvedPolicies != null ? resolvedPolicies.getCancellationPolicySnapshotJson() : null)
                    .noShowPolicySnapshot(resolvedPolicies != null ? resolvedPolicies.getNoShowPolicySnapshotJson() : null)
                    .build();

            short adultCount = rReq.getAdultCount() != null ? rReq.getAdultCount() : (short) 1;
            short childCount = rReq.getChildCount() != null ? rReq.getChildCount() : (short) 0;
            short infantCount = rReq.getInfantCount() != null ? rReq.getInfantCount() : (short) 0;
            short totalGuests = (short) (adultCount + childCount + infantCount);

            BookingRoom bookingRoom = BookingRoom.builder()
                    .bookingDetail(detail)
                    .roomInstance(roomInst)
                    .adultCount(adultCount)
                    .childCount(childCount)
                    .infantCount(infantCount)
                    .guestCount(totalGuests)
                    .status(BookingRoomStatus.CHECKED_IN)
                    .assignedAt(OffsetDateTime.now())
                    .actualCheckInAt(OffsetDateTime.now())
                    .build();

            List<BookingDailyRate> dailyRates = new ArrayList<>();
            BigDecimal roomSubtotal = BigDecimal.ZERO;
            BigDecimal roomVat = BigDecimal.ZERO;

            TaxCategory taxCategory = null;
            BigDecimal vatPercent = BigDecimal.valueOf(10);
            if (roomType.getTaxCategoryId() != null) {
                taxCategory = taxCategoryService.getEntityById(roomType.getTaxCategoryId());
                BigDecimal activeVat = vatRuleService.getActiveVatPercent(roomType.getTaxCategoryId(), request.getCheckInDate());
                if (activeVat != null && activeVat.compareTo(BigDecimal.ZERO) > 0) {
                    vatPercent = activeVat;
                }
            }

            for (DailyPriceDto daily : breakdown.getDailyPrices()) {
                BigDecimal netPrice = daily.getNetAmount();
                BigDecimal serviceFee = netPrice.multiply(booking.getServiceFeeRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal taxable = netPrice.add(serviceFee);
                BigDecimal vatAmount = taxable.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BookingDailyRate dr = BookingDailyRate.builder()
                        .bookingRoom(bookingRoom)
                        .stayDate(daily.getDate())
                        .basePrice(daily.getBaseRate())
                        .discountAmount(daily.getDiscountAmount())
                        .surchargeAmount(daily.getSurchargeAmount())
                        .serviceFeeRate(booking.getServiceFeeRate())
                        .serviceFeeAmount(serviceFee)
                        .taxCategory(taxCategory)
                        .vatPercent(vatPercent)
                        .vatAmount(vatAmount)
                        .netPrice(netPrice)
                        .status("CONFIRMED")
                        .build();
                dailyRates.add(dr);
                roomSubtotal = roomSubtotal.add(netPrice);
                roomVat = roomVat.add(vatAmount);
            }

            bookingRoom.setDailyRates(dailyRates);
            detail.setBookingRooms(List.of(bookingRoom));
            details.add(detail);
            allCreatedBookingRooms.add(bookingRoom);

            bookingSubtotal = bookingSubtotal.add(roomSubtotal);
            bookingVatTotal = bookingVatTotal.add(roomVat);
        }

        booking.setBookingDetails(details);
        booking.setSubtotalAmount(bookingSubtotal);
        BigDecimal bookingServiceFee = bookingSubtotal.multiply(booking.getServiceFeeRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        booking.setServiceFeeAmount(bookingServiceFee);
        booking.setTotalVatAmount(bookingVatTotal);
        booking.setTotalAmount(bookingSubtotal.add(bookingServiceFee).add(bookingVatTotal));

        Booking savedBooking = bookingRepository.save(booking);

        // 6. Xếp phòng vật lý & Check-in & StayGuest & RoomSlots
        for (int i = 0; i < sortedRooms.size(); i++) {
            WalkInBookingRequest.WalkInRoomItemRequest rReq = sortedRooms.get(i);
            BookingRoom br = allCreatedBookingRooms.get(i);
            RoomInstance roomInst = roomInstanceMap.get(rReq.getRoomInstanceId());

            // Cập nhật trạng thái phòng -> OCCUPIED
            roomInstanceService.updateRoomStatusEntity(roomInst.getId(), "OCCUPIED");

            // Tạo & Chiếm dụng RoomSlot
            roomSlotService.createOrReserveSlots(roomInst, request.getCheckInDate(), request.getCheckOutDate(), br.getId());
            roomSlotService.occupySlots(roomInst.getId(), request.getCheckInDate(), request.getCheckOutDate());

            // Tạo StayGuests
            if (rReq.getStayGuests() != null && !rReq.getStayGuests().isEmpty()) {
                for (WalkInBookingRequest.WalkInStayGuestRequest sg : rReq.getStayGuests()) {
                    DocumentType docType = DocumentType.CCCD;
                    if (sg.getIdentityType() != null) {
                        try {
                            docType = DocumentType.valueOf(sg.getIdentityType());
                        } catch (Exception ignored) {}
                    }
                    StayGuestCreateRequest sgReq = StayGuestCreateRequest.builder()
                            .hotelId(hotel.getId())
                            .bookingId(savedBooking.getId())
                            .bookingRoomId(br.getId())
                            .roomId(roomInst.getId())
                            .roomNumber(roomInst.getRoomNumber())
                            .fullName(sg.getFullName())
                            .dateOfBirth(sg.getBirthDate())
                            .gender(sg.getGender())
                            .nationality(sg.getNationality() != null ? sg.getNationality() : "Việt Nam")
                            .documentType(docType)
                            .documentNumber(sg.getIdentityNumber() != null ? sg.getIdentityNumber() : "N/A")
                            .checkInTime(OffsetDateTime.now())
                            .expectedCheckOutTime(request.getCheckOutDate().atTime(12, 0).atOffset(ZoneOffset.ofHours(7)))
                            .build();
                    stayGuestService.createOrCheckIn(sgReq);
                }
            }

            stayGuestService.syncLodgingQueueForBookingRoom(br.getId());
        }

        // 7. Ghi nhận Phụ thu đã xác nhận
        if (request.getConfirmedCharges() != null && !request.getConfirmedCharges().isEmpty()) {
            for (WalkInBookingRequest.WalkInConfirmedChargeRequest c : request.getConfirmedCharges()) {
                BookingRoom targetRoom = allCreatedBookingRooms.get(0);
                if (c.getRoomInstanceId() != null) {
                    for (BookingRoom br : allCreatedBookingRooms) {
                        if (br.getRoomInstance() != null && br.getRoomInstance().getId().equals(c.getRoomInstanceId())) {
                            targetRoom = br;
                            break;
                        }
                    }
                }

                boolean isWaived = Boolean.TRUE.equals(c.getIsWaived());
                BigDecimal unitPrice = isWaived ? BigDecimal.ZERO : (c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
                String desc = c.getDescription() != null ? c.getDescription() : "";
                if (isWaived && StringUtils.hasText(c.getWaivedReason())) {
                    desc += " (Miễn phụ thu: " + c.getWaivedReason() + ")";
                }

                BookingChargeCreateRequest chReq = BookingChargeCreateRequest.builder()
                        .chargeType(c.getChargeType())
                        .itemName(c.getDescription() != null ? c.getDescription() : c.getChargeType().name())
                        .description(desc)
                        .quantity(1)
                        .unitPrice(unitPrice)
                        .serviceFeeRate(BigDecimal.ZERO)
                        .vatRate(BigDecimal.ZERO)
                        .build();
                bookingChargeService.createCharge(targetRoom, chReq);
            }
        }

        // 8. Ghi nhận Thanh toán Payment
        PaymentPurpose pPurpose = "DEPOSIT".equalsIgnoreCase(paymentPurposeStr) ? PaymentPurpose.DEPOSIT : PaymentPurpose.FULL_PAYMENT;
        Payment payment = Payment.builder()
                .booking(savedBooking)
                .totalAmount(paidAmount)
                .paymentMethod(request.getPayment().getPaymentMethod())
                .paymentPurpose(pPurpose)
                .status(PaymentStatus.SUCCESS)
                .transactionReference(request.getPayment().getTransactionReference())
                .paidAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(payment);

        // 9. Sinh hóa đơn & Quyết toán nếu đủ tiền
        invoiceService.createInvoice(InvoiceCreateRequest.builder()
                .bookingId(savedBooking.getId())
                .serviceFeeRate(savedBooking.getServiceFeeRate())
                .build());

        if (paidAmount.compareTo(savedBooking.getTotalAmount()) >= 0) {
            invoiceService.settleAndCloseInvoice(savedBooking.getId());
        }

        return bookingMapper.toResponse(savedBooking);
    }

    // =========================================================================
    // FIT BOOKING CANCELLATION PIPELINE (UC-CUST-06, SQ-03, US12, FR-RES-09)
    // =========================================================================

    private BigDecimal calculateSingleRoomTotalAmount(BookingRoom room, BookingDetail detail, Booking booking) {
        BigDecimal total = BigDecimal.ZERO;
        if (room != null && room.getDailyRates() != null && !room.getDailyRates().isEmpty()) {
            for (BookingDailyRate rate : room.getDailyRates()) {
                BigDecimal net = rate.getNetPrice() != null ? rate.getNetPrice() : BigDecimal.ZERO;
                BigDecimal fee = rate.getServiceFeeAmount() != null ? rate.getServiceFeeAmount() : BigDecimal.ZERO;
                BigDecimal vat = rate.getVatAmount() != null ? rate.getVatAmount() : BigDecimal.ZERO;
                total = total.add(net).add(fee).add(vat);
            }
        } else if (detail != null && detail.getHotelRoomType() != null && detail.getHotelRoomType().getBasePrice() != null) {
            long nights = detail.getCheckInDate() != null && detail.getCheckOutDate() != null
                    ? Math.max(1, ChronoUnit.DAYS.between(detail.getCheckInDate(), detail.getCheckOutDate()))
                    : 1;
            BigDecimal base = detail.getHotelRoomType().getBasePrice().multiply(BigDecimal.valueOf(nights));
            BigDecimal serviceFeeRate = booking != null && booking.getServiceFeeRate() != null ? booking.getServiceFeeRate() : BigDecimal.ZERO;
            BigDecimal fee = base.multiply(serviceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal vat = base.add(fee).multiply(BigDecimal.valueOf(10)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            total = base.add(fee).add(vat);
        }
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingCancelPreviewResponse previewCancelBooking(Long id) {
        Booking booking = getEntityById(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        boolean anyCheckedInOrOut = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .anyMatch(br -> br.getStatus() == BookingRoomStatus.CHECKED_IN || br.getStatus() == BookingRoomStatus.CHECKED_OUT);
        if (anyCheckedInOrOut) {
            throw new AppException(ErrorCode.CANCELLATION_NOT_ALLOWED_AFTER_CHECKIN);
        }

        List<BookingRoom> expectedRooms = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .filter(br -> br.getStatus() == BookingRoomStatus.EXPECTED)
                .collect(Collectors.toList());

        if (expectedRooms.isEmpty()) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_BE_CANCELLED, "Không còn phòng nào ở trạng thái chờ nhận phòng để hủy");
        }

        LocalDate cancelDate = LocalDate.now();
        LocalDate earliestCheckIn = booking.getBookingDetails().stream()
                .map(BookingDetail::getCheckInDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(cancelDate);

        long daysUntilCheckIn = ChronoUnit.DAYS.between(cancelDate, earliestCheckIn);

        BigDecimal roomAmountTotal = BigDecimal.ZERO;
        BigDecimal cancellationPenalty = BigDecimal.ZERO;
        String previewPolicyDescription = null;

        for (BookingDetail bd : booking.getBookingDetails()) {
            if (bd.getBookingRooms() != null) {
                for (BookingRoom br : bd.getBookingRooms()) {
                    if (br.getStatus() == BookingRoomStatus.EXPECTED) {
                        BigDecimal roomTotal = calculateSingleRoomTotalAmount(br, bd, booking);
                        roomAmountTotal = roomAmountTotal.add(roomTotal);
                        BigDecimal roomPenaltyRate = bookingPolicyResolverService.calculateRoomCancellationPenaltyRate(bd, cancelDate);
                        cancellationPenalty = cancellationPenalty.add(roomTotal.multiply(roomPenaltyRate).setScale(2, RoundingMode.HALF_UP));
                        if (previewPolicyDescription == null) {
                            previewPolicyDescription = bookingPolicyResolverService.getCancellationPolicyDescription(bd, cancelDate);
                        }
                    }
                }
            }
        }

        BigDecimal penaltyRate = roomAmountTotal.compareTo(BigDecimal.ZERO) > 0
                ? cancellationPenalty.divide(roomAmountTotal, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Preordered services
        List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(booking.getId());
        BigDecimal preorderedServicesTotal = BigDecimal.ZERO;
        if (orders != null) {
            preorderedServicesTotal = orders.stream()
                    .filter(o -> o.getStatus() != ServiceOrderStatus.CANCELLED)
                    .map(ServiceOrderResponse::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Addon charges
        BigDecimal addonChargesTotal = BigDecimal.ZERO;
        for (BookingDetail bd : booking.getBookingDetails()) {
            if (bd.getBookingRooms() != null) {
                for (BookingRoom br : bd.getBookingRooms()) {
                    if (br.getCharges() != null) {
                        for (BookingCharge c : br.getCharges()) {
                            if (c.getChargeType() != BookingChargeType.PENALTY) {
                                addonChargesTotal = addonChargesTotal.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                            }
                        }
                    }
                }
            }
        }

        BigDecimal nonRefundableTotal = cancellationPenalty.add(preorderedServicesTotal).add(addonChargesTotal);

        // Payments total paid
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (payments != null) {
            totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .map(p -> p.getPaymentPurpose() == PaymentPurpose.REFUND ? p.getTotalAmount().negate() : p.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal refundAmount = totalPaid.subtract(nonRefundableTotal);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        String refundRouting = "NO_REFUND";
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundRouting = "WAITING_FINANCE_APPROVAL";
        }

        return BookingCancelPreviewResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .cancelledRoomsCount(expectedRooms.size())
                .checkInDate(earliestCheckIn)
                .cancelDate(cancelDate)
                .daysUntilCheckIn(daysUntilCheckIn)
                .penaltyRatePercentage(penaltyRate.multiply(BigDecimal.valueOf(100)))
                .roomAmount(roomAmountTotal)
                .cancellationPenalty(cancellationPenalty)
                .preorderedServicesTotal(preorderedServicesTotal)
                .addonChargesTotal(addonChargesTotal)
                .nonRefundableTotal(nonRefundableTotal)
                .totalPaid(totalPaid)
                .refundAmount(refundAmount)
                .refundRouting(refundRouting)
                .policyDescription(previewPolicyDescription != null ? previewPolicyDescription : bookingPolicyResolverService.getCancellationPolicyDescription(null, cancelDate))
                .isFullBookingCancellation(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingCancelPreviewResponse previewCancelBookingRoom(Long bookingRoomId) {
        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);
        if (bookingRoom.getStatus() == BookingRoomStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_ROOM_NOT_ELIGIBLE_FOR_CANCELLATION, "Phòng này đã được hủy trước đó");
        }
        if (bookingRoom.getStatus() != BookingRoomStatus.EXPECTED) {
            throw new AppException(ErrorCode.BOOKING_ROOM_NOT_ELIGIBLE_FOR_CANCELLATION,
                    "Chỉ được hủy phòng đang ở trạng thái chờ nhận phòng (EXPECTED). Trạng thái hiện tại: " + bookingRoom.getStatus());
        }

        BookingDetail detail = bookingRoom.getBookingDetail();
        Booking booking = detail.getBooking();
        LocalDate cancelDate = LocalDate.now();
        LocalDate checkInDate = detail.getCheckInDate() != null ? detail.getCheckInDate() : cancelDate;
        long daysUntilCheckIn = ChronoUnit.DAYS.between(cancelDate, checkInDate);
        BigDecimal penaltyRate = bookingPolicyResolverService.calculateRoomCancellationPenaltyRate(detail, cancelDate);
        BigDecimal roomAmount = calculateSingleRoomTotalAmount(bookingRoom, detail, booking);
        BigDecimal cancellationPenalty = roomAmount.multiply(penaltyRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal nonRefundableTotal = cancellationPenalty;

        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (payments != null) {
            totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .map(p -> p.getPaymentPurpose() == PaymentPurpose.REFUND ? p.getTotalAmount().negate() : p.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal refundAmount = roomAmount.subtract(cancellationPenalty);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }
        if (totalPaid.compareTo(BigDecimal.ZERO) > 0 && refundAmount.compareTo(totalPaid) > 0) {
            refundAmount = totalPaid;
        }

        String refundRouting = "NO_REFUND";
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundRouting = "WAITING_FINANCE_APPROVAL";
        }

        String roomNumber = bookingRoom.getRoomInstance() != null ? bookingRoom.getRoomInstance().getRoomNumber() : "Chưa gán";

        return BookingCancelPreviewResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .bookingRoomId(bookingRoom.getId())
                .roomNumber(roomNumber)
                .roomTypeName(detail.getRoomTypeName())
                .cancelledRoomsCount(1)
                .checkInDate(checkInDate)
                .cancelDate(cancelDate)
                .daysUntilCheckIn(daysUntilCheckIn)
                .penaltyRatePercentage(penaltyRate.multiply(BigDecimal.valueOf(100)))
                .roomAmount(roomAmount)
                .cancellationPenalty(cancellationPenalty)
                .preorderedServicesTotal(BigDecimal.ZERO)
                .addonChargesTotal(BigDecimal.ZERO)
                .nonRefundableTotal(nonRefundableTotal)
                .totalPaid(totalPaid)
                .refundAmount(refundAmount)
                .refundRouting(refundRouting)
                .policyDescription(bookingPolicyResolverService.getCancellationPolicyDescription(detail, cancelDate))
                .isFullBookingCancellation(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingCancelPreviewResponse previewPublicCancelBooking(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng với mã: " + bookingNumber));
        return previewCancelBooking(booking.getId());
    }

    @Override
    @Transactional
    public BookingCancelResponse cancelBooking(Long id, BookingCancelRequest request) {
        String username = SecurityUtils.getCurrentUsername().orElse("STAFF");
        return cancelBooking(id, request, username);
    }

    public BookingCancelResponse cancelBooking(Long id, BookingCancelRequest request, String executedBy) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Lý do hủy đặt phòng không được để trống");
        }

        Booking booking = getEntityById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        boolean anyCheckedInOrOut = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .anyMatch(br -> br.getStatus() == BookingRoomStatus.CHECKED_IN || br.getStatus() == BookingRoomStatus.CHECKED_OUT);
        if (anyCheckedInOrOut) {
            throw new AppException(ErrorCode.CANCELLATION_NOT_ALLOWED_AFTER_CHECKIN);
        }

        List<BookingRoom> expectedRooms = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .filter(br -> br.getStatus() == BookingRoomStatus.EXPECTED)
                .collect(Collectors.toList());

        if (expectedRooms.isEmpty()) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_BE_CANCELLED, "Không còn phòng nào ở trạng thái chờ nhận phòng để thực hiện hủy");
        }

        LocalDate cancelDate = LocalDate.now();
        BigDecimal totalPenaltyAmount = BigDecimal.ZERO;

        // 1. Release kho phòng bán lẻ theo từng BookingDetail (đúng số lượng cancelledRoomsCount)
        for (BookingDetail detail : booking.getBookingDetails()) {
            if (detail.getBookingRooms() == null) continue;
            int cancelledRoomsCount = (int) detail.getBookingRooms().stream()
                    .filter(br -> br.getStatus() == BookingRoomStatus.EXPECTED)
                    .count();

            if (cancelledRoomsCount > 0 && detail.getHotelRoomType() != null) {
                roomAvailabilityService.releaseBookedInventory(
                        detail.getHotelRoomType().getId(),
                        detail.getCheckInDate(),
                        detail.getCheckOutDate(),
                        cancelledRoomsCount
                );
            }
        }

        // 2. Xử lý từng phòng EXPECTED
        for (BookingRoom room : expectedRooms) {
            BookingDetail detail = room.getBookingDetail();
            LocalDate roomCheckIn = (detail != null && detail.getCheckInDate() != null) ? detail.getCheckInDate() : cancelDate;
            LocalDate roomCheckOut = (detail != null && detail.getCheckOutDate() != null) ? detail.getCheckOutDate() : cancelDate.plusDays(1);
            BigDecimal penaltyRate = bookingPolicyResolverService.calculateRoomCancellationPenaltyRate(detail, cancelDate);
            BigDecimal roomAmount = calculateSingleRoomTotalAmount(room, detail, booking);
            BigDecimal roomPenalty = roomAmount.multiply(penaltyRate).setScale(2, RoundingMode.HALF_UP);
            totalPenaltyAmount = totalPenaltyAmount.add(roomPenalty);

            // Ghi nhận phụ phí phạt
            if (roomPenalty.compareTo(BigDecimal.ZERO) > 0) {
                String roomLabel = room.getRoomInstance() != null ? room.getRoomInstance().getRoomNumber() : (detail != null ? detail.getRoomTypeName() : "Phòng");
                int pct = penaltyRate.multiply(BigDecimal.valueOf(100)).intValue();
                String bookingTypeLabel = booking.getBookingType() != null ? booking.getBookingType().name() : "FIT";
                BookingChargeCreateRequest chargeReq = BookingChargeCreateRequest.builder()
                        .chargeType(BookingChargeType.PENALTY)
                        .itemName("Phí phạt hủy phòng " + roomLabel + " (" + pct + "%)")
                        .description("Phí phạt hủy đơn " + bookingTypeLabel + ": " + (request != null ? request.getReason() : "Hủy phòng"))
                        .quantity(1)
                        .unitPrice(roomPenalty)
                        .serviceFeeRate(BigDecimal.ZERO)
                        .vatRate(BigDecimal.ZERO)
                        .build();
                bookingChargeService.createCharge(room, chargeReq);
            }

            // Hủy hàng chờ lưu trú BCA
            stayGuestService.cancelLodgingQueueForBookingRoom(room.getId());

            // Nhả RoomSlot và xử lý RoomInstance theo nguyên tắc Tương lai vs Hiện tại
            if (room.getRoomInstance() != null) {
                roomSlotService.releaseSlots(
                        room.getRoomInstance().getId(),
                        roomCheckIn,
                        roomCheckOut
                );

                // Chỉ xem xét cập nhật READY nếu ngày checkIn là hôm nay hoặc trong quá khứ
                if (!roomCheckIn.isAfter(cancelDate)) {
                    RoomInstance inst = room.getRoomInstance();
                    if (!"CLEANING".equalsIgnoreCase(inst.getCurrentStatus()) && !"MAINTENANCE".equalsIgnoreCase(inst.getCurrentStatus())) {
                        roomInstanceService.updateRoomStatusEntity(inst.getId(), "READY");
                    }
                }

                room.setRoomInstance(null);
            }

            room.setStatus(BookingRoomStatus.CANCELLED);
            bookingRoomService.save(room);
        }

        // 3. Kiểm tra điều kiện chuyển Booking sang CANCELLED:
        // CHỈ chuyển CANCELLED khi 100% các phòng trong booking đều đã là CANCELLED
        boolean allRoomsCancelled = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .allMatch(br -> br.getStatus() == BookingRoomStatus.CANCELLED);

        if (allRoomsCancelled) {
            booking.setStatus(BookingStatus.CANCELLED);
        }

        // 4. Tính toán hoàn tiền
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (payments != null) {
            totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .map(p -> p.getPaymentPurpose() == PaymentPurpose.REFUND ? p.getTotalAmount().negate() : p.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal allPenalties = BigDecimal.ZERO;
        BigDecimal allAddons = BigDecimal.ZERO;
        for (BookingDetail bd : booking.getBookingDetails()) {
            if (bd.getBookingRooms() != null) {
                for (BookingRoom br : bd.getBookingRooms()) {
                    if (br.getCharges() != null) {
                        for (BookingCharge c : br.getCharges()) {
                            if (c.getChargeType() == BookingChargeType.PENALTY) {
                                allPenalties = allPenalties.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                            } else {
                                allAddons = allAddons.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                            }
                        }
                    }
                }
            }
        }

        List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(booking.getId());
        BigDecimal preorderedTotal = BigDecimal.ZERO;
        if (orders != null) {
            preorderedTotal = orders.stream()
                    .filter(o -> o.getStatus() != ServiceOrderStatus.CANCELLED)
                    .map(ServiceOrderResponse::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal nonRefundableTotal = allPenalties.add(preorderedTotal).add(allAddons);
        BigDecimal refundAmount = totalPaid.subtract(nonRefundableTotal);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        String refundRouting = "NO_REFUND";
        String refundRef = null;
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundRouting = "WAITING_FINANCE_APPROVAL";
            refundRef = "REFUND-CANCEL-" + booking.getBookingNumber() + "-" + System.currentTimeMillis();
            Payment refundPayment = Payment.builder()
                    .booking(booking)
                    .totalAmount(refundAmount)
                    .paymentPurpose(PaymentPurpose.REFUND)
                    .paymentMethod(PaymentMethod.OTHER)
                    .paymentProvider(refundRouting)
                    .status(PaymentStatus.PENDING)
                    .transactionReference(refundRef)
                    .gatewayTxnId("PENDING_FINANCE_SETTLEMENT")
                    .rawCallbackPayload("{\"reason\": \"" + request.getReason() + "\", \"amount\": " + refundAmount + "}")
                    .build();
            paymentRepository.save(refundPayment);
        }

        String logNote = String.format("[BOOKING_CANCELLED - %s] Đã hủy %d phòng. Lý do: %s. Tổng phạt: %,.0f đ. Tiền hoàn: %,.0f đ. Bởi: %s",
                OffsetDateTime.now().toString(),
                expectedRooms.size(),
                request.getReason(),
                totalPenaltyAmount,
                refundAmount,
                executedBy);
        booking.setNotes(booking.getNotes() != null ? booking.getNotes() + "\n" + logNote : logNote);
        bookingRepository.save(booking);

        auditLogService.recordLog(
                null,
                "BOOKING_CANCELLED",
                "Booking",
                booking.getId().toString(),
                "CONFIRMED",
                logNote,
                null,
                executedBy
        );

        String msg = refundAmount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Hủy đặt phòng thành công. Tiền hoàn %,.0f đ sẽ được xử lý trong 3 - 7 ngày làm việc.", refundAmount)
                : "Hủy đặt phòng thành công.";

        return BookingCancelResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .cancelledRoomsCount(expectedRooms.size())
                .bookingStatus(booking.getStatus().name())
                .roomStatus(BookingRoomStatus.CANCELLED.name())
                .cancellationPenalty(totalPenaltyAmount)
                .refundAmount(refundAmount)
                .refundPaymentReference(refundRef)
                .refundRouting(refundRouting)
                .cancelledBy(executedBy)
                .cancelledAt(OffsetDateTime.now())
                .message(msg)
                .build();
    }

    @Override
    @Transactional
    public BookingCancelResponse cancelBookingRoom(Long bookingRoomId, BookingCancelRequest request) {
        String username = SecurityUtils.getCurrentUsername().orElse("STAFF");
        return cancelBookingRoom(bookingRoomId, request, username);
    }

    public BookingCancelResponse cancelBookingRoom(Long bookingRoomId, BookingCancelRequest request, String executedBy) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Lý do hủy phòng không được để trống");
        }

        BookingRoom bookingRoom = bookingRoomService.getEntityById(bookingRoomId);

        if (bookingRoom.getStatus() == BookingRoomStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_ROOM_NOT_ELIGIBLE_FOR_CANCELLATION, "Phòng này đã được hủy trước đó");
        }
        if (bookingRoom.getStatus() != BookingRoomStatus.EXPECTED) {
            throw new AppException(ErrorCode.BOOKING_ROOM_NOT_ELIGIBLE_FOR_CANCELLATION,
                    "Chỉ được hủy phòng đang ở trạng thái chờ nhận phòng (EXPECTED). Trạng thái hiện tại: " + bookingRoom.getStatus());
        }

        BookingDetail detail = bookingRoom.getBookingDetail();
        Booking booking = detail.getBooking();
        LocalDate cancelDate = LocalDate.now();

        BigDecimal penaltyRate = bookingPolicyResolverService.calculateRoomCancellationPenaltyRate(detail, cancelDate);
        BigDecimal roomAmount = calculateSingleRoomTotalAmount(bookingRoom, detail, booking);
        BigDecimal penaltyAmount = roomAmount.multiply(penaltyRate).setScale(2, RoundingMode.HALF_UP);

        // 1. Ghi nhận BookingCharge(PENALTY) nếu phạt > 0
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            String roomLabel = bookingRoom.getRoomInstance() != null ? bookingRoom.getRoomInstance().getRoomNumber() : detail.getRoomTypeName();
            int pct = penaltyRate.multiply(BigDecimal.valueOf(100)).intValue();
            String bookingTypeLabel = booking.getBookingType() != null ? booking.getBookingType().name() : "FIT";
            BookingChargeCreateRequest chargeReq = BookingChargeCreateRequest.builder()
                    .chargeType(BookingChargeType.PENALTY)
                    .itemName("Phí phạt hủy phòng " + roomLabel + " (" + pct + "%)")
                    .description("Phí phạt hủy phòng " + bookingTypeLabel + ": " + request.getReason())
                    .quantity(1)
                    .unitPrice(penaltyAmount)
                    .serviceFeeRate(BigDecimal.ZERO)
                    .vatRate(BigDecimal.ZERO)
                    .build();
            bookingChargeService.createCharge(bookingRoom, chargeReq);
        }

        // 2. Hủy hàng chờ lưu trú BCA
        stayGuestService.cancelLodgingQueueForBookingRoom(bookingRoom.getId());

        // 3. Hoàn trả kho phòng bán lẻ đúng 1 phòng
        if (detail.getHotelRoomType() != null) {
            roomAvailabilityService.releaseBookedInventory(
                    detail.getHotelRoomType().getId(),
                    detail.getCheckInDate(),
                    detail.getCheckOutDate(),
                    1
            );
        }

        // 4. Nhả RoomSlot & xử lý RoomInstance theo nguyên tắc Tương lai vs Hiện tại
        if (bookingRoom.getRoomInstance() != null) {
            roomSlotService.releaseSlots(
                    bookingRoom.getRoomInstance().getId(),
                    detail.getCheckInDate(),
                    detail.getCheckOutDate()
            );

            // Chỉ cập nhật READY nếu ngày checkIn là hôm nay hoặc trong quá khứ
            if (detail.getCheckInDate() != null && !detail.getCheckInDate().isAfter(cancelDate)) {
                RoomInstance inst = bookingRoom.getRoomInstance();
                if (!"CLEANING".equalsIgnoreCase(inst.getCurrentStatus()) && !"MAINTENANCE".equalsIgnoreCase(inst.getCurrentStatus())) {
                    roomInstanceService.updateRoomStatusEntity(inst.getId(), "READY");
                }
            }

            bookingRoom.setRoomInstance(null);
        }

        bookingRoom.setStatus(BookingRoomStatus.CANCELLED);
        bookingRoomService.save(bookingRoom);

        // 5. Kiểm tra điều kiện chuyển toàn bộ Booking sang CANCELLED:
        // CHỈ chuyển CANCELLED khi 100% các phòng trong booking đều đã là CANCELLED
        boolean allRoomsCancelled = booking.getBookingDetails().stream()
                .flatMap(bd -> bd.getBookingRooms().stream())
                .allMatch(br -> br.getStatus() == BookingRoomStatus.CANCELLED);

        if (allRoomsCancelled) {
            booking.setStatus(BookingStatus.CANCELLED);
        }

        // 6. Tính toán hoàn tiền
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (payments != null) {
            totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .map(p -> p.getPaymentPurpose() == PaymentPurpose.REFUND ? p.getTotalAmount().negate() : p.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal allPenalties = BigDecimal.ZERO;
        BigDecimal allAddons = BigDecimal.ZERO;
        BigDecimal activeRoomsTotal = BigDecimal.ZERO;
        for (BookingDetail bd : booking.getBookingDetails()) {
            if (bd.getBookingRooms() != null) {
                for (BookingRoom br : bd.getBookingRooms()) {
                    if (br.getStatus() == BookingRoomStatus.EXPECTED || br.getStatus() == BookingRoomStatus.CHECKED_IN) {
                        activeRoomsTotal = activeRoomsTotal.add(calculateSingleRoomTotalAmount(br, bd, booking));
                    }
                    if (br.getCharges() != null) {
                        for (BookingCharge c : br.getCharges()) {
                            if (c.getChargeType() == BookingChargeType.PENALTY) {
                                allPenalties = allPenalties.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                            } else {
                                allAddons = allAddons.add(c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO);
                            }
                        }
                    }
                }
            }
        }

        List<ServiceOrderResponse> orders = serviceOrderService.getOrdersByBookingId(booking.getId());
        BigDecimal preorderedTotal = BigDecimal.ZERO;
        if (orders != null) {
            preorderedTotal = orders.stream()
                    .filter(o -> o.getStatus() != ServiceOrderStatus.CANCELLED)
                    .map(ServiceOrderResponse::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalObligations = activeRoomsTotal.add(allPenalties).add(preorderedTotal).add(allAddons);
        BigDecimal refundAmount = totalPaid.subtract(totalObligations);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        String refundRouting = "NO_REFUND";
        String refundRef = null;
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundRouting = "WAITING_FINANCE_APPROVAL";
            refundRef = "REFUND-ROOM-" + bookingRoomId + "-" + System.currentTimeMillis();
            Payment refundPayment = Payment.builder()
                    .booking(booking)
                    .totalAmount(refundAmount)
                    .paymentPurpose(PaymentPurpose.REFUND)
                    .paymentMethod(PaymentMethod.OTHER)
                    .paymentProvider(refundRouting)
                    .status(PaymentStatus.PENDING)
                    .transactionReference(refundRef)
                    .gatewayTxnId("PENDING_FINANCE_SETTLEMENT")
                    .rawCallbackPayload("{\"reason\": \"" + request.getReason() + "\", \"amount\": " + refundAmount + "}")
                    .build();
            paymentRepository.save(refundPayment);
        }

        String logNote = String.format("[ROOM_CANCELLED - %s] Đã hủy phòng ID %d. Lý do: %s. Phạt: %,.0f đ. Tiền hoàn: %,.0f đ. Bởi: %s",
                OffsetDateTime.now().toString(),
                bookingRoomId,
                request.getReason(),
                penaltyAmount,
                refundAmount,
                executedBy);
        booking.setNotes(booking.getNotes() != null ? booking.getNotes() + "\n" + logNote : logNote);
        bookingRepository.save(booking);

        auditLogService.recordLog(
                null,
                "BOOKING_ROOM_CANCELLED",
                "BookingRoom",
                bookingRoomId.toString(),
                "EXPECTED",
                logNote,
                null,
                executedBy
        );

        String msg = refundAmount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Hủy phòng thành công. Tiền hoàn %,.0f đ sẽ được xử lý trong 3 - 7 ngày làm việc.", refundAmount)
                : "Hủy phòng thành công.";

        return BookingCancelResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .bookingRoomId(bookingRoomId)
                .cancelledRoomsCount(1)
                .bookingStatus(booking.getStatus().name())
                .roomStatus(BookingRoomStatus.CANCELLED.name())
                .cancellationPenalty(penaltyAmount)
                .refundAmount(refundAmount)
                .refundPaymentReference(refundRef)
                .refundRouting(refundRouting)
                .cancelledBy(executedBy)
                .cancelledAt(OffsetDateTime.now())
                .message(msg)
                .build();
    }

    @Override
    @Transactional
    public BookingCancelResponse cancelPublicBooking(String bookingNumber, BookingCancelRequest request) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng với mã: " + bookingNumber));
        return cancelBooking(booking.getId(), request, "ONLINE_CUSTOMER");
    }
}
