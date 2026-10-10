package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
import vn.edu.utc.hotel_booking.modules.booking.repository.*;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.repository.CompanyRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.GuestRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DailyPriceDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    BookingRepository bookingRepository;
    BookingDetailRepository bookingDetailRepository;
    BookingRoomRepository bookingRoomRepository;
    StayGuestRepository stayGuestRepository;
    BookingDailyRateRepository bookingDailyRateRepository;
    BookingChargeRepository bookingChargeRepository;

    HotelRepository hotelRepository;
    GuestRepository guestRepository;
    CompanyRepository companyRepository;
    HotelRoomTypeRepository hotelRoomTypeRepository;
    RoomInstanceRepository roomInstanceRepository;
    RoomSlotRepository roomSlotRepository;

    RoomAvailabilityService roomAvailabilityService;
    PriceEngine priceEngine;
    BookingMapper bookingMapper;
    vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository taxCategoryRepository;
    vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository vatRuleRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn ID: " + request.getHotelId()));

        BookingGuest guest = guestRepository.findByIdAndIsDeletedFalse(request.getGuestId())
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND, "Không tìm thấy thông tin khách hàng ID: " + request.getGuestId()));

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findByIdAndIsDeletedFalse(request.getCompanyId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND, "Không tìm thấy doanh nghiệp đối tác ID: " + request.getCompanyId()));
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

            HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(roomReq.getHotelRoomTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                            "Không tìm thấy cấu hình loại phòng: " + roomReq.getHotelRoomTypeId()));

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
                    assignedRoom = roomInstanceRepository.findByIdAndIsDeletedFalse(roomReq.getRoomInstanceId())
                            .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND,
                                    "Không tìm thấy phòng vật lý: " + roomReq.getRoomInstanceId()));

                    // Sequential Lock ordering: Kiểm tra xem phòng có bị trùng lịch không
                    List<BookingRoom> overlapping = bookingRoomRepository.findOverlappingAssignedRooms(
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

                // Tạo danh sách khách lưu trú
                List<StayGuest> bookingGuests = new ArrayList<>();
                if (roomReq.getGuests() != null && !roomReq.getGuests().isEmpty()) {
                    for (BookingCreateRequest.GuestItemRequest gReq : roomReq.getGuests()) {
                        StayGuest bg = StayGuest.builder()
                                .bookingRoom(bookingRoom)
                                .firstName(gReq.getFirstName() != null ? gReq.getFirstName() : "")
                                .lastName(gReq.getLastName() != null ? gReq.getLastName() : "")
                                .fullName(gReq.getFullName() != null ? gReq.getFullName() : (gReq.getFirstName() + " " + gReq.getLastName()).trim())
                                .dateOfBirth(gReq.getBirthDate())
                                .guestType(gReq.getGuestType() != null ? gReq.getGuestType() : BookingGuestType.ADULT)
                                .documentNumber(gReq.getIdentityNumber())
                                .build();
                        if (gReq.getIdentityType() != null) {
                            bg.setIdentityType(gReq.getIdentityType());
                        }
                        bookingGuests.add(bg);
                    }
                }
                bookingRoom.setStayGuests(bookingGuests);

                // Tạo Snapshot giá chi tiết từng đêm (BookingDailyRate)
                List<BookingDailyRate> dailyRates = new ArrayList<>();
                BigDecimal roomSubtotal = BigDecimal.ZERO;
                BigDecimal roomVat = BigDecimal.ZERO;

                vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory taxCategory = null;
                BigDecimal vatPercent = BigDecimal.valueOf(10); // 10% default
                if (hotelRoomType.getTaxCategoryId() != null) {
                    taxCategory = taxCategoryRepository.findById(hotelRoomType.getTaxCategoryId()).orElse(null);
                    if (taxCategory != null) {
                        Optional<vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule> optVat =
                                vatRuleRepository.findActiveVatRule(taxCategory.getId(), roomReq.getCheckInDate());
                        if (optVat.isPresent()) {
                            vatPercent = optVat.get().getVatPercent();
                        }
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
                    createRoomSlotsForAssignedRoom(br.getRoomInstance(), d.getCheckInDate(), d.getCheckOutDate(), br.getId());
                }
            }
        }

        return bookingMapper.toResponse(saved);
    }

        private void createRoomSlotsForAssignedRoom(RoomInstance roomInstance, LocalDate checkIn, LocalDate checkOut, Long bookingRoomId) {
        LocalDate cur = checkIn;
        while (cur.isBefore(checkOut)) {
            LocalDate slotDate = cur; // biến effectively final để dùng trong lambda
            Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstance.getId(), slotDate);
            RoomSlot slot = optSlot.orElseGet(() -> RoomSlot.builder()
                    .roomInstance(roomInstance)
                    .slotDate(slotDate)
                    .build());
            slot.setStatus("RESERVED");
            slot.setBookingRoomId(bookingRoomId);
            slot.setReservedAt(OffsetDateTime.now());
            roomSlotRepository.save(slot);
            cur = cur.plusDays(1);
        }
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
            if (searchDto.getRoomNumber() != null && !searchDto.getRoomNumber().isBlank()) {
                String cleanRoomNumber = searchDto.getRoomNumber().trim().toLowerCase();
                Join<Booking, BookingDetail> detailsJoin = root.join("bookingDetails", JoinType.LEFT);
                Join<BookingDetail, BookingRoom> roomsJoin = detailsJoin.join("bookingRooms", JoinType.LEFT);
                Join<BookingRoom, RoomInstance> instanceJoin = roomsJoin.join("roomInstance", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(instanceJoin.get("roomNumber")), "%" + cleanRoomNumber + "%"));
                if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                    query.distinct(true);
                }
            }
            if (searchDto.getBookingType() != null) {
                predicates.add(cb.equal(root.get("bookingType"), searchDto.getBookingType()));
            }
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
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
                        releaseRoomSlots(br.getRoomInstance().getId(), detail.getCheckInDate(), detail.getCheckOutDate());
                    }
                }
            }
        }

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    private void releaseRoomSlots(Integer roomInstanceId, LocalDate checkIn, LocalDate checkOut) {
        LocalDate cur = checkIn;
        while (cur.isBefore(checkOut)) {
            Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstanceId, cur);
            if (optSlot.isPresent()) {
                RoomSlot slot = optSlot.get();
                slot.setStatus("READY");
                slot.setBookingRoomId(null);
                slot.setReservedAt(null);
                roomSlotRepository.save(slot);
            }
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public BookingResponse assignRoom(Long bookingRoomId, Integer roomInstanceId) {
        BookingRoom bookingRoom = bookingRoomRepository.findById(bookingRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng đặt ID: " + bookingRoomId));

        RoomInstance roomInstance = roomInstanceRepository.findByIdAndIsDeletedFalse(roomInstanceId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_INSTANCE_NOT_FOUND,
                        "Không tìm thấy phòng vật lý ID: " + roomInstanceId));

        BookingDetail detail = bookingRoom.getBookingDetail();
        Booking booking = detail != null ? detail.getBooking() : null;

        // 1. Kiểm tra khách sạn: phòng vật lý phải thuộc đúng khách sạn của đơn đặt phòng
        if (booking != null && booking.getHotel() != null && roomInstance.getHotel() != null 
                && !roomInstance.getHotel().getId().equals(booking.getHotel().getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Phòng " + roomInstance.getRoomNumber() + " không thuộc khách sạn của đơn đặt phòng này");
        }

        // 2. Không được xếp vào phòng đang có khách ở
        if ("OCCUPIED".equalsIgnoreCase(roomInstance.getCurrentStatus())) {
            throw new AppException(ErrorCode.ROOM_ALREADY_ASSIGNED,
                    "Phòng " + roomInstance.getRoomNumber() + " đang có khách lưu trú, không thể xếp phòng");
        }

        // 3. Kiểm tra xung đột lịch
        List<BookingRoom> overlapping = bookingRoomRepository.findOverlappingAssignedRooms(
                roomInstance.getId(), detail.getCheckInDate(), detail.getCheckOutDate());
        for (BookingRoom o : overlapping) {
            if (!o.getId().equals(bookingRoom.getId())) {
                throw new AppException(ErrorCode.ROOM_ALREADY_ASSIGNED,
                        "Phòng " + roomInstance.getRoomNumber() + " đã có khách đặt trong khoảng thời gian này");
            }
        }

        // Hủy liên kết phòng cũ nếu có
        if (bookingRoom.getRoomInstance() != null) {
            releaseRoomSlots(bookingRoom.getRoomInstance().getId(), detail.getCheckInDate(), detail.getCheckOutDate());
        }

        bookingRoom.setRoomInstance(roomInstance);
        bookingRoom.setAssignedAt(OffsetDateTime.now());
        bookingRoomRepository.save(bookingRoom);

        createRoomSlotsForAssignedRoom(roomInstance, detail.getCheckInDate(), detail.getCheckOutDate(), bookingRoom.getId());

        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingChargeResponse addCharge(Long bookingRoomId, BookingChargeCreateRequest request) {
        BookingRoom bookingRoom = bookingRoomRepository.findById(bookingRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng đặt ID: " + bookingRoomId));

        BigDecimal subtotal = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal serviceFee = subtotal.multiply(request.getServiceFeeRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal taxable = subtotal.add(serviceFee);
        BigDecimal vatAmount = taxable.multiply(request.getVatRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxable.add(vatAmount);

        BookingCharge charge = BookingCharge.builder()
                .bookingRoom(bookingRoom)
                .chargeType(request.getChargeType())
                .itemName(request.getItemName())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .subtotal(subtotal)
                .serviceFeeRate(request.getServiceFeeRate())
                .serviceFeeAmount(serviceFee)
                .vatRate(request.getVatRate())
                .vatAmount(vatAmount)
                .totalAmount(totalAmount)
                .issuedAt(OffsetDateTime.now())
                .build();

        BookingCharge savedCharge = bookingChargeRepository.save(charge);

        // Cập nhật tổng tiền đơn đặt phòng
        Booking booking = bookingRoom.getBookingDetail().getBooking();
        booking.setSubtotalAmount(booking.getSubtotalAmount().add(subtotal));
        booking.setServiceFeeAmount(booking.getServiceFeeAmount().add(serviceFee));
        booking.setTotalVatAmount(booking.getTotalVatAmount().add(vatAmount));
        booking.setTotalAmount(booking.getTotalAmount().add(totalAmount));
        bookingRepository.save(booking);

        return bookingMapper.toChargeResponse(savedCharge);
    }

    @Override
    @Transactional
    public BookingResponse checkIn(Long bookingRoomId) {
        BookingRoom bookingRoom = bookingRoomRepository.findById(bookingRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng đặt ID: " + bookingRoomId));

        if (bookingRoom.getRoomInstance() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Lượt đặt này chưa được xếp phòng vật lý. Vui lòng xếp phòng trước khi Check-in");
        }

        RoomInstance roomInstance = bookingRoom.getRoomInstance();
        if ("OCCUPIED".equalsIgnoreCase(roomInstance.getCurrentStatus())) {
            throw new AppException(ErrorCode.ROOM_ALREADY_ASSIGNED,
                    "Phòng " + roomInstance.getRoomNumber() + " hiện đang có khách lưu trú, không thể check-in trùng phòng");
        }

        bookingRoom.setStatus(BookingRoomStatus.CHECKED_IN);
        bookingRoom.setActualCheckInAt(OffsetDateTime.now());
        bookingRoomRepository.save(bookingRoom);

        // Đổi trạng thái phòng sang OCCUPIED để các lượt khác không thể chọn trùng
        roomInstance.setCurrentStatus("OCCUPIED");
        roomInstanceRepository.save(roomInstance);

        BookingDetail detail = bookingRoom.getBookingDetail();
        LocalDate cur = detail.getCheckInDate();
        while (cur.isBefore(detail.getCheckOutDate())) {
            Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(
                    roomInstance.getId(), cur);
            if (optSlot.isPresent()) {
                RoomSlot slot = optSlot.get();
                slot.setStatus("OCCUPIED");
                roomSlotRepository.save(slot);
            }
            cur = cur.plusDays(1);
        }

        return bookingMapper.toResponse(bookingRoom.getBookingDetail().getBooking());
    }

    @Override
    @Transactional
    public BookingResponse checkOut(Long bookingRoomId) {
        BookingRoom bookingRoom = bookingRoomRepository.findById(bookingRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_ROOM_NOT_FOUND,
                        "Không tìm thấy phòng đặt ID: " + bookingRoomId));

        bookingRoom.setStatus(BookingRoomStatus.CHECKED_OUT);
        bookingRoom.setActualCheckOutAt(OffsetDateTime.now());
        bookingRoomRepository.save(bookingRoom);

        if (bookingRoom.getRoomInstance() != null) {
            RoomInstance roomInstance = bookingRoom.getRoomInstance();
            // Trả phòng: giải phóng trạng thái OCCUPIED sang CLEANING để dọn dẹp
            roomInstance.setCurrentStatus("CLEANING");
            roomInstanceRepository.save(roomInstance);

            BookingDetail detail = bookingRoom.getBookingDetail();
            LocalDate cur = detail.getCheckInDate();
            while (cur.isBefore(detail.getCheckOutDate())) {
                Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(
                        roomInstance.getId(), cur);
                if (optSlot.isPresent()) {
                    RoomSlot slot = optSlot.get();
                    slot.setStatus("DIRTY"); // Cần dọn phòng sau check-out
                    roomSlotRepository.save(slot);
                }
                cur = cur.plusDays(1);
            }
        }

        return bookingMapper.toResponse(bookingRoom.getBookingDetail().getBooking());
    }

    private String generateBookingNumber() {
        return "BK" + System.currentTimeMillis() + (int) (Math.random() * 900 + 100);
    }
}
