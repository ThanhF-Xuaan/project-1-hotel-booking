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
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.identity.mapper.GuestMapper;
import vn.edu.utc.hotel_booking.modules.identity.repository.GuestRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.GuestService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;
    private final GuestMapper guestMapper;

    @Override
    public PageResponse<GuestResponse> filter(GuestSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String identityType = StringUtils.hasText(searchDto.getIdentityType()) ? searchDto.getIdentityType().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<BookingGuest> resultPage = guestRepository.searchGuests(keyword, identityType, status, pageable);
        return PageResponse.from(resultPage.map(guestMapper::toResponse));
    }

    @Override
    public GuestResponse getById(Long id) {
        BookingGuest guest = guestRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND));
        return guestMapper.toResponse(guest);
    }

    @Override
    public GuestResponse getByPublicId(UUID publicId) {
        BookingGuest guest = guestRepository.findByPublicIdAndIsDeletedFalse(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND));
        return guestMapper.toResponse(guest);
    }

    @Override
    public GuestResponse getByPhone(String phone) {
        BookingGuest guest = guestRepository.findByPhoneAndIsDeletedFalse(phone.trim())
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND));
        return guestMapper.toResponse(guest);
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
