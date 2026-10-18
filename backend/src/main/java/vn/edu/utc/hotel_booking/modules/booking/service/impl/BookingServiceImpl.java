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
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.mapper.BookingMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
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
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;

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
    BookingMapper bookingMapper;
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

            BookingDetail detail = BookingDetail.builder()
                    .booking(booking)
                    .hotelRoomType(hotelRoomType)
                    .roomTypeName(hotelRoomType.getRoomType().getName())
                    .quantity(roomReq.getQuantity())
                    .checkInDate(roomReq.getCheckInDate())
                    .checkOutDate(roomReq.getCheckOutDate())
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

        booking.setStatus(status);

        if (status == BookingStatus.CANCELLED) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                for (BookingRoom br : detail.getBookingRooms()) {
                    br.setStatus(BookingRoomStatus.CANCELLED);
                    if (br.getRoomInstance() != null) {
                        roomSlotService.releaseSlots(br.getRoomInstance().getId(), detail.getCheckInDate(), detail.getCheckOutDate());
                    }
                }
            }
        }

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

        bookingRoom.setStatus(BookingRoomStatus.CHECKED_OUT);
        bookingRoom.setActualCheckOutAt(OffsetDateTime.now());
        bookingRoomService.save(bookingRoom);

        if (bookingRoom.getRoomInstance() != null) {
            BookingDetail detail = bookingRoom.getBookingDetail();
            roomSlotService.markSlotsDirty(bookingRoom.getRoomInstance().getId(), detail.getCheckInDate(), detail.getCheckOutDate());
        }

        return bookingMapper.toResponse(bookingRoom.getBookingDetail().getBooking());
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

            BookingDetail detail = BookingDetail.builder()
                    .booking(booking)
                    .hotelRoomType(hotelRoomType)
                    .roomTypeName(hotelRoomType.getRoomType().getName())
                    .quantity(item.getQuantity())
                    .checkInDate(draftSession.getCheckInDate())
                    .checkOutDate(draftSession.getCheckOutDate())
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
            for (BookingDetail detail : booking.getBookingDetails()) {
                if (!detail.getCheckInDate().isAfter(auditDate)) {
                    boolean anyCheckedIn = detail.getBookingRooms().stream()
                            .anyMatch(br -> br.getStatus() == BookingRoomStatus.CHECKED_IN || br.getActualCheckInAt() != null);
                    if (!anyCheckedIn) {
                        isNoShow = true;
                        break;
                    }
                }
            }

            if (isNoShow) {
                booking.setStatus(BookingStatus.NO_SHOW);

                for (BookingDetail detail : booking.getBookingDetails()) {
                    for (BookingRoom br : detail.getBookingRooms()) {
                        br.setStatus(BookingRoomStatus.NO_SHOW);
                        bookingRoomService.save(br);

                        if (br.getRoomInstance() != null) {
                            roomSlotService.releaseSlots(
                                    br.getRoomInstance().getId(),
                                    detail.getCheckInDate(),
                                    detail.getCheckOutDate()
                            );
                        }
                    }
                }

                bookingRepository.save(booking);
                noShowCount++;
            }
        }

        log.info("Processed {} No-Show bookings for hotel ID {} on audit date {}", noShowCount, hotelId, auditDate);
        return noShowCount;
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

        BigDecimal percentage = BigDecimal.ZERO;
        String policyRule = "";
        String standardTime = "";
        long diffMinutes = 0;
        boolean applicable = false;

        if (chargeType == BookingChargeType.EARLY_CHECKIN) {
            standardTime = "14:00";
            LocalTime standardCheckIn = LocalTime.of(14, 0);

            // Kiểm tra xem khách có nhận sớm hay không
            if (dateOnly.isBefore(detail.getCheckInDate()) || (dateOnly.isEqual(detail.getCheckInDate()) && timeOnly.isBefore(standardCheckIn))) {
                applicable = true;
                diffMinutes = Duration.between(actualTime, detail.getCheckInDate().atTime(standardCheckIn).atOffset(actualTime.getOffset())).toMinutes();

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
            } else {
                policyRule = "Khách nhận phòng đúng hoặc sau giờ quy định (14:00) — Không phát sinh phụ thu";
            }
        } else if (chargeType == BookingChargeType.LATE_CHECKOUT) {
            standardTime = "12:00";
            LocalTime standardCheckOut = LocalTime.of(12, 0);

            // Kiểm tra xem khách có trả muộn hay không
            if (dateOnly.isAfter(detail.getCheckOutDate()) || (dateOnly.isEqual(detail.getCheckOutDate()) && timeOnly.isAfter(standardCheckOut))) {
                applicable = true;
                diffMinutes = Duration.between(detail.getCheckOutDate().atTime(standardCheckOut).atOffset(actualTime.getOffset()), actualTime).toMinutes();

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
            } else {
                policyRule = "Khách trả phòng đúng hoặc trước giờ quy định (12:00) — Không phát sinh phụ thu";
            }
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Chỉ hỗ trợ tính phụ thu tự động cho EARLY_CHECKIN hoặc LATE_CHECKOUT");
        }

        BigDecimal suggestedAmount = baseRoomRate.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String suggestedItemName = chargeType == BookingChargeType.EARLY_CHECKIN
                ? "Phụ thu nhận phòng sớm (" + percentage.stripTrailingZeros().toPlainString() + "% giá phòng)"
                : "Phụ thu trả phòng muộn (" + percentage.stripTrailingZeros().toPlainString() + "% giá phòng)";

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
}
