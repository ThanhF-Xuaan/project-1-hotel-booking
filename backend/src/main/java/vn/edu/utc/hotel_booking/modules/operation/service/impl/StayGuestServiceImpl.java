package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import vn.edu.utc.hotel_booking.modules.operation.service.support.LodgingExportStateGuard;
import vn.edu.utc.hotel_booking.modules.operation.service.support.StayGuestTimeValidator;
import vn.edu.utc.hotel_booking.modules.operation.service.support.ActiveStayDedup;
import java.util.Arrays;
import java.util.Objects;
import java.util.Comparator;
import vn.edu.utc.hotel_booking.modules.operation.security.OperationStaffScopeResolver;
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
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService;
import vn.edu.utc.hotel_booking.modules.operation.service.support.StayGuestAssociationValidator;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StayGuestServiceImpl implements StayGuestService {

    private final StayGuestRepository stayGuestRepository;
    private final LodgingQueueRepository lodgingQueueRepository;
    private final HotelRepository hotelRepository;
    private final BookingLookupService bookingLookupService;
    private final RoomInstanceRepository roomInstanceRepository;
    private final StaffRepository staffRepository;
    private final StayGuestMapper stayGuestMapper;
    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional
    public StayGuestResponse createOrCheckIn(StayGuestCreateRequest request) {
        validateStaffScope(request.getHotelId());

        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));

        Booking booking = request.getBookingId() == null ? null : bookingLookupService.getBookingEntityById(request.getBookingId());
        RoomInstance room = request.getRoomId() == null ? null : roomInstanceRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy phòng vật lý"));
        BookingRoom bookingRoom = request.getBookingRoomId() == null ? null
                : bookingLookupService.getBookingRoomEntityById(request.getBookingRoomId());
        if (request.getBookingRoomId() != null && bookingRoom == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Liên kết phòng đặt không hợp lệ");
        }
        if (bookingRoom == null && booking != null && room != null) {
            bookingRoom = bookingLookupService.findBookingRoomByBookingIdAndRoomInstanceId(booking.getId(), room.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Phòng vật lý không thuộc đơn đặt phòng"));
        }
        var associations = StayGuestAssociationValidator.validate(hotel.getId(), booking, bookingRoom, room, request.getRoomNumber());
        booking = associations.booking(); bookingRoom = associations.bookingRoom(); room = associations.room();

        StayGuest identityCandidate = StayGuest.builder().hotel(hotel).booking(booking).bookingRoom(bookingRoom).room(room)
                .roomNumber(associations.roomNumber()).documentNumber(request.getDocumentNumber())
                .checkInTime(request.getCheckInTime()).build();
        byte[] key=ActiveStayDedup.key(stayGuestRepository, identityCandidate);
        ActiveStayDedup.lock(stayGuestRepository, Arrays.asList(key));
        Optional<StayGuest> existingActiveStay=key==null?Optional.empty():stayGuestRepository.findActiveByDedupKey(key);
        LodgingQueue existingQueue=null;
        if(existingActiveStay.isPresent()) {
            StayGuest existing=existingActiveStay.get();
            existingQueue=lockQueueAndRefresh(existing);
            // A writer outside this service may have checked out/deleted the row while we waited.
            if(!Arrays.equals(key,ActiveStayDedup.key(stayGuestRepository,existing))) existingActiveStay=Optional.empty();
        }

        StayGuest guest;
        if (existingActiveStay.isPresent()) {
            // Cùng một lượt check-in đang hoạt động -> cập nhật thông tin (idempotent retry)
            guest = existingActiveStay.get();
            StayGuest candidate = LodgingExportStateGuard.copy(guest);
            candidate.setFullName(request.getFullName().trim());
            candidate.setDateOfBirth(request.getDateOfBirth());
            candidate.setGender(request.getGender());
            candidate.setNationality(StringUtils.hasText(request.getNationality()) ? request.getNationality().trim() : "Việt Nam");
            candidate.setDocumentType(request.getDocumentType());
            candidate.setDocumentNumber(ActiveStayDedup.normalizeToken(request.getDocumentNumber()));
            candidate.setPermanentAddress(request.getPermanentAddress());
            candidate.setCurrentAddress(request.getCurrentAddress());
            candidate.setCheckInTime(request.getCheckInTime());
            candidate.setExpectedCheckOutTime(request.getExpectedCheckOutTime());
            candidate.setRoomNumber(request.getRoomNumber());
            candidate.setReasonForStay(request.getReasonForStay());
            candidate.setDocumentImageUrl(request.getDocumentImageUrl());
            boolean sameBookingRoom=Objects.equals(guest.getBookingRoom()==null?null:guest.getBookingRoom().getId(),
                    bookingRoom==null?null:bookingRoom.getId());
            // Equivalent legacy representation remains untouched, including EXPORTED retry/noop.
            if(!sameBookingRoom) {
                candidate.setBooking(booking);candidate.setRoom(room);candidate.setBookingRoom(bookingRoom);
            }
            candidate.setRoomNumber(associations.roomNumber());
            LodgingExportStateGuard.verifyChange(existingQueue, guest, candidate);
            if (existingQueue != null && existingQueue.getStatus() == LodgingQueueStatus.EXPORTED
                    && LodgingExportStateGuard.sameMutableValues(guest, candidate)) return stayGuestMapper.toResponse(guest);
            StayGuestTimeValidator.validate(candidate);
            org.springframework.beans.BeanUtils.copyProperties(candidate, guest);
        } else {
            // Lượt lưu trú mới -> luôn tạo mới bản ghi StayGuest, bảo toàn lịch sử lưu trú trước đó
            guest = stayGuestMapper.toEntity(request);
            guest.setHotel(hotel);
            guest.setBooking(booking);
            guest.setRoom(room);
            guest.setBookingRoom(bookingRoom);
            guest.setDocumentNumber(ActiveStayDedup.normalizeToken(request.getDocumentNumber()));
            StayGuestTimeValidator.validate(guest);
        }

        guest.setRoomNumber(associations.roomNumber());
        StayGuest savedGuest = persistGuest(guest);

        // Đánh giá tính đầy đủ theo tiêu chuẩn BCA để đẩy vào LodgingQueue
        syncLodgingQueue(savedGuest);

        return stayGuestMapper.toResponse(savedGuest);
    }

    @Override
    @Transactional
    public StayGuestResponse update(Long id, StayGuestUpdateRequest request) {
        StayGuest guest = stayGuestRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.STAY_GUEST_NOT_FOUND));

        validateStaffScope(guest.getHotel().getId());

        byte[] oldKey=ActiveStayDedup.key(stayGuestRepository,guest);
        byte[] newKey=ActiveStayDedup.key(stayGuestRepository,prepareUpdate(guest,request));
        ActiveStayDedup.lock(stayGuestRepository,Arrays.asList(oldKey,newKey));
        LodgingQueue existingQueue=lockQueueAndRefresh(guest);
        if(Boolean.TRUE.equals(guest.getIsDeleted())) throw new AppException(ErrorCode.STAY_GUEST_NOT_FOUND);
        ActiveStayDedup.verifyUnchanged(oldKey,ActiveStayDedup.key(stayGuestRepository,guest));
        validateStaffScope(guest.getHotel().getId());
        StayGuest candidate=prepareUpdate(guest,request);
        ActiveStayDedup.verifyUnchanged(newKey,ActiveStayDedup.key(stayGuestRepository,candidate));
        LodgingExportStateGuard.verifyChange(existingQueue, guest, candidate);
        if (existingQueue != null && existingQueue.getStatus() == LodgingQueueStatus.EXPORTED
                && LodgingExportStateGuard.sameMutableValues(guest, candidate)) return stayGuestMapper.toResponse(guest);
        if (request.getExpectedCheckOutTime() != null || request.getActualCheckOutTime() != null) {
            StayGuestTimeValidator.validate(candidate);
        }
        if(newKey!=null) stayGuestRepository.findActiveByDedupKey(newKey).ifPresent(other->{
            if(!Objects.equals(other.getId(),guest.getId())) throw new AppException(ErrorCode.STAY_GUEST_CONFLICT);
        });
        org.springframework.beans.BeanUtils.copyProperties(candidate, guest);
        StayGuest savedGuest = persistGuest(guest);

        // Cập nhật lại trạng thái queue nếu trước đó bị thiếu trường
        syncLodgingQueue(savedGuest);

        return stayGuestMapper.toResponse(savedGuest);
    }

    private StayGuest prepareUpdate(StayGuest guest, StayGuestUpdateRequest request) {
        BookingRoom bookingRoom = request.getBookingRoomId() == null ? guest.getBookingRoom()
                : bookingLookupService.getBookingRoomEntityById(request.getBookingRoomId());
        if (request.getBookingRoomId() != null && bookingRoom == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Liên kết phòng đặt không hợp lệ");
        }
        RoomInstance room = request.getRoomId() == null
                ? (request.getBookingRoomId() == null ? guest.getRoom() : bookingRoom.getRoomInstance())
                : roomInstanceRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy phòng vật lý"));
        String number = request.getRoomNumber() != null ? request.getRoomNumber()
                : ((request.getRoomId() != null || request.getBookingRoomId() != null) && room != null ? room.getRoomNumber() : guest.getRoomNumber());
        var associations = StayGuestAssociationValidator.validate(guest.getHotel().getId(), guest.getBooking(), bookingRoom, room, number);
        StayGuest candidate = LodgingExportStateGuard.copy(guest);
        stayGuestMapper.updateEntityFromDto(request, candidate);
        if(request.getDocumentNumber()!=null) candidate.setDocumentNumber(ActiveStayDedup.normalizeToken(request.getDocumentNumber()));
        // Validate legacy associations but do not backfill redundant keys without an association patch.
        if (request.getBookingRoomId() != null || request.getRoomId() != null) {
            candidate.setBooking(associations.booking()); candidate.setBookingRoom(associations.bookingRoom());
            candidate.setRoom(associations.room());
        }
        candidate.setRoomNumber(associations.roomNumber());
        return candidate;
    }

    @Override
    public PageResponse<StayGuestResponse> search(StayGuestSearchDto request) {
        Short effectiveHotelId = resolveEffectiveHotelId(request.getHotelId());
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize(), Sort.by("checkInTime").descending());

        Page<StayGuest> page = stayGuestRepository.search(
                effectiveHotelId,
                request.getBookingId(),
                request.getRoomNumber(),
                request.getDocumentNumber(),
                request.getKeyword(),
                request.getFromDate(),
                request.getToDate(),
                pageable
        );

        return PageResponse.from(page.map(stayGuestMapper::toResponse));
    }

    @Override
    public StayGuestResponse getById(Long id) {
        StayGuest guest = stayGuestRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.STAY_GUEST_NOT_FOUND));

        validateStaffScope(guest.getHotel().getId());
        return stayGuestMapper.toResponse(guest);
    }

    @Override
    public List<StayGuestResponse> getByBookingRoomId(Long bookingRoomId) {
        if (bookingRoomId == null) {
            return List.of();
        }
        BookingRoom bookingRoom = bookingLookupService.getBookingRoomEntityById(bookingRoomId);
        if (bookingRoom == null || bookingRoom.getBookingDetail() == null || bookingRoom.getBookingDetail().getBooking() == null
                || bookingRoom.getBookingDetail().getBooking().getHotel() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Liên kết phòng đặt không hợp lệ");
        }
        Short hotelId = bookingRoom.getBookingDetail().getBooking().getHotel().getId();
        validateStaffScope(hotelId);
        List<StayGuest> list = stayGuestRepository.findByBookingRoomIdAndHotelIdAndIsDeletedFalse(bookingRoomId, hotelId);
        return list.stream()
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .map(stayGuestMapper::toResponse)
                .toList();
    }

    @Override
    public StayGuest getEntityById(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID khách lưu trú không được để trống");
        }
        return stayGuestRepository.findById(id)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.GUEST_NOT_FOUND, "Không tìm thấy khách lưu trú ID: " + id));
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<StayGuest> guests = stayGuestRepository.findAllByIdInAndIsDeletedFalse(ids).stream()
                .sorted(Comparator.comparing(StayGuest::getId)).toList();
        for(StayGuest guest:guests) validateStaffScope(guest.getHotel().getId());
        List<byte[]> keys=guests.stream().map(g->ActiveStayDedup.key(stayGuestRepository,g)).toList();
        ActiveStayDedup.lock(stayGuestRepository,keys);
        for(int i=0;i<guests.size();i++) {
            StayGuest guest=guests.get(i);
            LodgingQueue queue=lockQueueAndRefresh(guest);
            ActiveStayDedup.verifyUnchanged(keys.get(i),ActiveStayDedup.key(stayGuestRepository,guest));
            validateStaffScope(guest.getHotel().getId());
            if(Boolean.TRUE.equals(guest.getIsDeleted())) continue;
            guest.setIsDeleted(true);
            if(queue!=null) {queue.setStatus(LodgingQueueStatus.CANCELLED);queue.setIsDeleted(true);lodgingQueueRepository.save(queue);}
        }
        try {stayGuestRepository.saveAll(guests);stayGuestRepository.flush();}
        catch(RuntimeException error){throw ActiveStayDedup.translate(error);}
    }

    @Override
    @Transactional
    public void syncLodgingQueueForBookingRoom(Long bookingRoomId) {
        if (bookingRoomId == null) {
            return;
        }
        List<StayGuest> guests=stayGuestRepository.findByBookingRoomId(bookingRoomId).stream()
                .sorted(Comparator.comparing(StayGuest::getId)).toList();
        OffsetDateTime now=OffsetDateTime.now();
        List<byte[]> oldKeys=new ArrayList<>(); List<byte[]> keys=new ArrayList<>();
        for(StayGuest guest:guests) {
            byte[] old=ActiveStayDedup.key(stayGuestRepository,guest);oldKeys.add(old);keys.add(old);
            StayGuest candidate=LodgingExportStateGuard.copy(guest);
            if(candidate.getCheckInTime()==null) candidate.setCheckInTime(now);
            keys.add(ActiveStayDedup.key(stayGuestRepository,candidate));
        }
        ActiveStayDedup.lock(stayGuestRepository,keys);
        for(int i=0;i<guests.size();i++) {
            StayGuest guest=guests.get(i);
            LodgingQueue queue=lockQueueAndRefresh(guest);
            ActiveStayDedup.verifyUnchanged(oldKeys.get(i),ActiveStayDedup.key(stayGuestRepository,guest));
            if(Boolean.TRUE.equals(guest.getIsDeleted()) || guest.getActualCheckOutTime()!=null) continue;
            if(queue!=null && LodgingExportStateGuard.preservesQueue(queue)) continue;
            if(queue!=null && queue.getStatus()==LodgingQueueStatus.PROCESSING) throw LodgingExportStateGuard.stateConflict();
            if(guest.getCheckInTime()==null) {
                StayGuest candidate=LodgingExportStateGuard.copy(guest);candidate.setCheckInTime(now);
                StayGuestTimeValidator.validate(candidate);guest.setCheckInTime(now);
            }
            syncLodgingQueue(persistGuest(guest));
        }
    }

    @Override
    @Transactional
    public void cancelLodgingQueueForBookingRoom(Long bookingRoomId) {
        if (bookingRoomId == null) {
            return;
        }
        List<StayGuest> guests=stayGuestRepository.findByBookingRoomId(bookingRoomId).stream()
                .sorted(Comparator.comparing(StayGuest::getId)).toList();
        List<byte[]> keys=guests.stream().map(g->ActiveStayDedup.key(stayGuestRepository,g)).toList();
        ActiveStayDedup.lock(stayGuestRepository,keys);
        for(int i=0;i<guests.size();i++) {
            StayGuest guest=guests.get(i);LodgingQueue queue=lockQueueAndRefresh(guest);
            ActiveStayDedup.verifyUnchanged(keys.get(i),ActiveStayDedup.key(stayGuestRepository,guest));
            if(queue!=null){queue.setStatus(LodgingQueueStatus.CANCELLED);lodgingQueueRepository.save(queue);}
        }
    }

    private LodgingQueue lockQueueAndRefresh(StayGuest guest) {
        try {
            LodgingQueue queue=lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(guest.getId()).orElse(null);
            entityManager.refresh(guest,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            return queue;
        } catch(RuntimeException error){throw ActiveStayDedup.translate(error);}
    }

    private StayGuest persistGuest(StayGuest guest) {
        try {StayGuest saved=stayGuestRepository.save(guest);stayGuestRepository.flush();return saved;}
        catch(RuntimeException error){throw ActiveStayDedup.translate(error);}
    }

    private void syncLodgingQueue(StayGuest guest) {
        List<String> missingFields = validateBcaFields(guest);

        LodgingQueue queue = lodgingQueueRepository.findByStayGuestIdAndIsDeletedFalse(guest.getId())
                .orElseGet(() -> LodgingQueue.builder()
                        .stayGuest(guest)
                        .hotel(guest.getHotel())
                        .build());

        if (LodgingExportStateGuard.preservesQueue(queue)) return;
        if (queue.getStatus() == LodgingQueueStatus.PROCESSING) throw LodgingExportStateGuard.stateConflict();
        if (missingFields.isEmpty()) {
            queue.setStatus(LodgingQueueStatus.PENDING);
            queue.setErrorMessage(null);
        } else {
            queue.setStatus(LodgingQueueStatus.ERROR);
            queue.setErrorMessage("Thiếu thông tin chuẩn BCA: " + String.join(", ", missingFields));
        }

        lodgingQueueRepository.save(queue);
    }

    private List<String> validateBcaFields(StayGuest guest) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(guest.getFullName())) missing.add("Họ và tên");
        if (guest.getDateOfBirth() == null) missing.add("Ngày tháng năm sinh");
        if (guest.getGender() == null) missing.add("Giới tính");
        if (!StringUtils.hasText(guest.getNationality())) missing.add("Quốc tịch");
        if (guest.getDocumentType() == null) missing.add("Loại giấy tờ");
        if (!StringUtils.hasText(guest.getDocumentNumber())) missing.add("Số giấy tờ");
        if (guest.getCheckInTime() == null) missing.add("Thời gian đến");
        if (guest.getExpectedCheckOutTime() == null) missing.add("Thời gian đi dự kiến");
        if (!StringUtils.hasText(guest.getRoomNumber())) missing.add("Số phòng");

        boolean isVietnamese = "Việt Nam".equalsIgnoreCase(guest.getNationality())
                || "Viet Nam".equalsIgnoreCase(guest.getNationality())
                || "Vietnam".equalsIgnoreCase(guest.getNationality());

        if (isVietnamese && !StringUtils.hasText(guest.getPermanentAddress())) {
            missing.add("Nơi ĐKTT / Nơi cấp");
        }

        return missing;
    }

    private void validateStaffScope(Short hotelId) {
        if (hotelId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "hotelId không được để trống");
        }

        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN")) {
            return;
        }

        if (SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") || SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Short staffHotelId = resolvePropertyHotelId();
            if (!staffHotelId.equals(hotelId)) {
                String roleTitle = SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") ? "Quản lý khách sạn" : "Lễ tân";
                throw new AppException(ErrorCode.UNAUTHORIZED, roleTitle + " không có quyền thao tác trên khách sạn khác");
            }
            return;
        }

        throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thao tác trên khách sạn này");
    }

    private Short resolveEffectiveHotelId(Short requestedHotelId) {
        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN")) {
            return requestedHotelId;
        }

        if (SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") || SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Short staffHotelId = resolvePropertyHotelId();
            if (requestedHotelId != null && !requestedHotelId.equals(staffHotelId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Không được phép truy vấn dữ liệu của khách sạn khác");
            }
            return staffHotelId;
        }

        throw new AppException(ErrorCode.UNAUTHORIZED, "Không xác định được phạm vi khách sạn được phép truy cập");
    }

    private Short resolvePropertyHotelId() {
        return OperationStaffScopeResolver.requireScope(resolveCurrentStaff(), "PROPERTY");
    }

    private Staff resolveCurrentStaff() {
        return OperationStaffScopeResolver.resolve(staffRepository);
    }
}
