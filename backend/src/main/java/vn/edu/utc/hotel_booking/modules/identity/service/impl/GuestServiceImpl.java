package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestStayHistoryResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.mapper.GuestMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.BookingGuestRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.GuestService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GuestServiceImpl implements GuestService {

    private final BookingGuestRepository guestRepository;
    private final BookingLookupService bookingLookupService;
    private final GuestMapper guestMapper;

    @Override
    public PageResponse<GuestResponse> filter(GuestSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : "";
        String identityType = StringUtils.hasText(searchDto.getIdentityType()) ? searchDto.getIdentityType().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<BookingGuest> resultPage = guestRepository.searchGuests(keyword, identityType, status, pageable);
        return PageResponse.from(resultPage.map(guestMapper::toResponse));
    }

    @Override
    public GuestResponse getById(Long id) {
        BookingGuest guest = getEntityById(id);
        return guestMapper.toResponse(guest);
    }

    @Override
    public BookingGuest getEntityById(Long id) {
        return guestRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND, "Không tìm thấy thông tin khách hàng ID: " + id));
    }

    @Override
    @Transactional
    public BookingGuest getOrCreateGuest(String phone, String fullName, String email, String nationality) {
        return getOrCreateGuest(phone, fullName, email, nationality, null, null, null);
    }

    @Override
    @Transactional
    public BookingGuest getOrCreateGuest(String phone, String fullName, String email, String nationality,
                                         String identityType, String identityNumber, java.time.LocalDate birthDate) {
        String cleanPhone = StringUtils.hasText(phone) ? phone.trim() : "";
        String cleanFullName = StringUtils.hasText(fullName) ? fullName.trim() : "";
        String cleanEmail = StringUtils.hasText(email) ? email.trim() : null;
        String cleanIdNumber = StringUtils.hasText(identityNumber) ? identityNumber.trim() : null;
        String cleanIdType = StringUtils.hasText(identityType) ? identityType.trim() : null;
        String cleanNationality = StringUtils.hasText(nationality) ? nationality.trim() : "VN";

        List<BookingGuest> candidates = guestRepository.findAllByPhoneAndIsDeletedFalseOrderByCreatedAtDesc(cleanPhone);

        // So sánh các thông tin khác: nếu trùng khớp thông tin thì GIỮ NGUYÊN (không update)
        for (BookingGuest candidate : candidates) {
            if (isCandidateMatching(candidate, cleanFullName, cleanIdNumber)) {
                log.info("Tìm thấy hồ sơ khách hàng trùng khớp (ID: {}, Tên: {}, SĐT: {}). Giữ nguyên bản ghi.",
                        candidate.getId(), candidate.getFullName(), candidate.getPhone());
                return candidate;
            }
        }

        // Nếu khác hoặc chưa từng tồn tại: cho phép trùng SĐT, INSERT thêm vào booking_guests, KHÔNG update bản ghi cũ
        log.info("Không trùng khớp với hồ sơ cũ hoặc là SĐT mới. Tạo mới hồ sơ BookingGuest: Tên={}, SĐT={}, CCCD={}",
                cleanFullName, cleanPhone, cleanIdNumber);

        BookingGuest newGuest = BookingGuest.builder()
                .fullName(cleanFullName)
                .email(cleanEmail)
                .phone(cleanPhone)
                .nationality(cleanNationality)
                .identityNumber(cleanIdNumber)
                .identityType(cleanIdType != null ? cleanIdType : detectIdentityType(cleanIdNumber))
                .birthDate(birthDate)
                .status("ACTIVE")
                .build();

        return guestRepository.save(newGuest);
    }

    private boolean isCandidateMatching(BookingGuest candidate, String targetFullName, String targetIdNumber) {
        if (!StringUtils.hasText(targetFullName)) {
            return false;
        }

        // 1. So khớp họ tên (không phân biệt hoa thường, bỏ khoảng trắng thừa)
        String candidateName = candidate.getFullName() != null ? candidate.getFullName().trim() : "";
        if (!candidateName.equalsIgnoreCase(targetFullName)) {
            return false;
        }

        // 2. So khớp số giấy tờ tùy thân (nếu có thông tin ở cả 2 bên)
        String candidateIdNum = candidate.getIdentityNumber() != null ? candidate.getIdentityNumber().trim() : "";
        if (StringUtils.hasText(targetIdNumber) && StringUtils.hasText(candidateIdNum)) {
            return candidateIdNum.equalsIgnoreCase(targetIdNumber);
        }

        // Nếu một trong 2 bên không có số giấy tờ nhưng họ tên khớp chính xác => coi như cùng 1 người
        return true;
    }

    private String detectIdentityType(String idNum) {
        if (!StringUtils.hasText(idNum)) {
            return null;
        }
        String trimmed = idNum.trim();
        if (trimmed.matches("^\\d{12}$")) {
            return "CCCD";
        } else if (trimmed.matches("^[A-Za-z0-9]{7,9}$")) {
            return "PASSPORT";
        } else if (trimmed.matches("^\\d{9}$")) {
            return "CMND";
        }
        return "OTHER";
    }

    @Override
    public GuestResponse getByPublicId(UUID publicId) {
        BookingGuest guest = guestRepository.findByPublicIdAndIsDeletedFalse(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND));
        return guestMapper.toResponse(guest);
    }

    @Override
    public GuestResponse getByPhone(String phone) {
        BookingGuest guest = guestRepository.findFirstByPhoneAndIsDeletedFalseOrderByCreatedAtDesc(phone.trim())
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND));
        return guestMapper.toResponse(guest);
    }

    @Override
    public GuestResponse getByIdentityNumber(String identityNumber) {
        if (!StringUtils.hasText(identityNumber)) {
            throw new AppException(ErrorCode.GUEST_NOT_FOUND, "Số giấy tờ tùy thân không được để trống");
        }
        BookingGuest guest = guestRepository.findByIdentityNumberAndIsDeletedFalse(identityNumber.trim())
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND, "Không tìm thấy hồ sơ khách hàng với số giấy tờ: " + identityNumber));
        return guestMapper.toResponse(guest);
    }

    @Override
    public GuestStayHistoryResponse getStayHistory(Long guestId) {
        BookingGuest guest = getEntityById(guestId);
        List<Booking> bookings = bookingLookupService.getBookingsByGuestId(guestId);

        BigDecimal totalSpent = BigDecimal.ZERO;
        List<GuestStayHistoryResponse.GuestBookingSummaryDto> summaries = new ArrayList<>();

        for (Booking b : bookings) {
            BigDecimal price = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
            if (b.getStatus() != BookingStatus.CANCELLED) {
                totalSpent = totalSpent.add(price);
            }
            summaries.add(GuestStayHistoryResponse.GuestBookingSummaryDto.builder()
                    .bookingId(b.getId())
                    .bookingNumber(b.getBookingNumber())
                    .hotelName(b.getHotel() != null ? b.getHotel().getName() : "")
                    .status(b.getStatus() != null ? b.getStatus().name() : "")
                    .totalPrice(price)
                    .bookingDate(b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate() : null)
                    .build());
        }

        return GuestStayHistoryResponse.builder()
                .guestId(guest.getId())
                .fullName(guest.getFullName())
                .phone(guest.getPhone())
                .totalStays(bookings.size())
                .totalSpent(totalSpent)
                .bookings(summaries)
                .build();
    }

    @Override
    @Transactional
    public GuestResponse create(GuestCreateRequest request) {
        String phone = request.getPhone().trim();
        // Một số khách sạn cho phép khách đặt lại theo số điện thoại hoặc tìm khách cũ,
        // nếu đã tồn tại thì trả về khách đã có hoặc cập nhật, nhưng ở đây tuân thủ tạo mới:
        BookingGuest guest = guestMapper.toEntity(request);
        guest.setPhone(phone);
        if (request.getEmail() != null) {
            guest.setEmail(request.getEmail().trim().toLowerCase());
        }

        BookingGuest saved = guestRepository.save(guest);
        log.info("Đã tạo mới hồ sơ khách hàng: id={}, phone={}, publicId={}", saved.getId(), saved.getPhone(), saved.getPublicId());
        return guestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public GuestResponse update(Long id, GuestUpdateRequest request) {
        BookingGuest guest = guestRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND));

        guestMapper.updateEntity(guest, request);
        guest.setPhone(request.getPhone().trim());
        if (request.getEmail() != null) {
            guest.setEmail(request.getEmail().trim().toLowerCase());
        }

        BookingGuest updated = guestRepository.save(guest);
        log.info("Đã cập nhật hồ sơ khách hàng: id={}, phone={}", updated.getId(), updated.getPhone());
        return guestMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = guestRepository.softDeleteBatch(ids);
        log.info("Đã xóa mềm {} hồ sơ khách hàng với danh sách IDs: {}", count, ids);
    }
}
