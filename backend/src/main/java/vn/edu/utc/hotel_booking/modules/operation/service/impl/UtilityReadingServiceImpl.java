package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityReadingResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityReading;
import vn.edu.utc.hotel_booking.modules.operation.mapper.UtilityReadingMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityMeterRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityReadingRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.UtilityReadingService;

import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityReadingServiceImpl implements UtilityReadingService {

    private static final Set<String> ALLOWED_READING_SORTS = Set.of("readingDate", "readingValue", "id", "createdAt");

    private final UtilityReadingRepository repository;
    private final UtilityMeterRepository meterRepository;
    private final StaffRepository staffRepository;
    private final HotelRepository hotelRepository;
    private final UtilityReadingMapper mapper;

    @Override
    public PageResponse<UtilityReadingResponse> search(UtilityReadingSearchDto request) {
        Short effectiveHotelId = request.getHotelId();
        Short effectiveRegionId = request.getRegionId();

        if (SecurityUtils.hasRole("CHAIN_ADMIN")) {
            // Full chain-wide access
        } else if (SecurityUtils.hasRole("PROPERTY_MANAGER") || SecurityUtils.hasRole("ENGINEERING")) {
            Staff staff = resolveCurrentStaff();
            if (!"PROPERTY".equalsIgnoreCase(staff.getScopeType()) || staff.getScopeEntityId() == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
            }
            Short userHotelId = staff.getScopeEntityId().shortValue();
            if (request.getHotelId() != null && !request.getHotelId().equals(userHotelId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền truy cập dữ liệu của khách sạn khác");
            }
            effectiveHotelId = userHotelId;
            effectiveRegionId = null;

            if (request.getMeterId() != null) {
                UtilityMeter meter = meterRepository.findById(request.getMeterId())
                        .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                        .orElseThrow(() -> new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Meter not found"));
                if (!meter.getHotel().getId().equals(userHotelId)) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Đồng hồ không thuộc khách sạn của bạn");
                }
            }
        } else if (SecurityUtils.hasRole("REGION_MANAGER")) {
            Staff staff = resolveCurrentStaff();
            if (!"REGION".equalsIgnoreCase(staff.getScopeType()) || staff.getScopeEntityId() == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
            }
            Short userRegionId = staff.getScopeEntityId().shortValue();
            if (request.getRegionId() != null && !request.getRegionId().equals(userRegionId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền truy cập dữ liệu của vùng khác");
            }
            if (request.getHotelId() != null) {
                Hotel hotel = hotelRepository.findById(request.getHotelId())
                        .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Hotel not found"));
                if (!hotel.getRegion().getId().equals(userRegionId)) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Khách sạn không thuộc vùng quản lý");
                }
            }
            if (request.getMeterId() != null) {
                UtilityMeter meter = meterRepository.findById(request.getMeterId())
                        .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                        .orElseThrow(() -> new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Meter not found"));
                if (!meter.getHotel().getRegion().getId().equals(userRegionId)) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Đồng hồ không thuộc vùng quản lý");
                }
            }
            effectiveRegionId = userRegionId;
            effectiveHotelId = request.getHotelId();
        } else {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thực hiện hành động này");
        }

        Pageable pageable = buildPageable(request);
        Page<UtilityReading> page = repository.search(request.getMeterId(), effectiveHotelId, effectiveRegionId, request.getFromDate(), request.getToDate(), pageable);
        
        List<Long> readingIds = page.getContent().stream().map(UtilityReading::getId).toList();
        Map<Long, BigDecimal> previousValues = new HashMap<>();
        if (!readingIds.isEmpty()) {
            List<Object[]> results = repository.findPreviousValues(readingIds);
            for (Object[] result : results) {
                if (result[1] != null) {
                    previousValues.put(((Number) result[0]).longValue(), new BigDecimal(result[1].toString()));
                }
            }
        }
        
        return PageResponse.from(page.map(reading -> mapToResponseWithUsageBulk(reading, previousValues.get(reading.getId()))));
    }

    @Override
    public UtilityReadingResponse getById(Long id) {
        UtilityReading reading = getReadingOrThrow(id);
        validateReadScope(reading.getMeter().getHotel());
        return mapToResponseWithUsage(reading);
    }

    @Override
    @Transactional
    public UtilityReadingResponse create(UtilityReadingCreateRequest request) {
        if (!SecurityUtils.hasRole("CHAIN_ADMIN") && !SecurityUtils.hasRole("PROPERTY_MANAGER") && !SecurityUtils.hasRole("ENGINEERING")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thực hiện hành động này");
        }

        UtilityMeter meter = meterRepository.findById(request.getMeterId())
                .orElseThrow(() -> new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Meter not found"));
                
        if (Boolean.TRUE.equals(meter.getIsDeleted())) {
            throw new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Cannot add reading to a deleted meter");
        }

        validateWriteScope(meter.getHotel().getId());

        if (repository.existsByMeterIdAndReadingDateAndIsDeletedFalse(request.getMeterId(), request.getReadingDate())) {
            throw new AppException(ErrorCode.UTILITY_READING_ALREADY_EXISTS, "Reading already exists for this date");
        }

        repository.findPreviousReading(meter.getId(), request.getReadingDate())
                .ifPresent(prev -> {
                    if (!Boolean.TRUE.equals(request.getIsMeterReset()) &&
                        request.getReadingValue().compareTo(prev.getReadingValue()) < 0) {
                        throw new AppException(ErrorCode.INVALID_UTILITY_READING_VALUE,
                                "Chỉ số đọc (" + request.getReadingValue() + ") không được nhỏ hơn chỉ số trước đó (" + prev.getReadingValue() + ") khi không có cờ reset đồng hồ");
                    }
                });

        repository.findNextReading(meter.getId(), request.getReadingDate())
                .ifPresent(next -> {
                    if (!Boolean.TRUE.equals(next.getIsMeterReset()) &&
                        request.getReadingValue().compareTo(next.getReadingValue()) > 0) {
                        throw new AppException(ErrorCode.INVALID_UTILITY_READING_VALUE,
                                "Chỉ số đọc (" + request.getReadingValue() + ") không được lớn hơn chỉ số tiếp theo (" + next.getReadingValue() + ") khi chỉ số tiếp theo không có cờ reset đồng hồ");
                    }
                });

        UtilityReading reading = mapper.toEntity(request);
        reading.setMeter(meter);
        reading.setRecordedBy(resolveCurrentStaffId()); 
        
        UtilityReading savedReading;
        try {
            savedReading = repository.saveAndFlush(reading);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueReadingViolation(ex)) {
                throw new AppException(ErrorCode.UTILITY_READING_ALREADY_EXISTS,
                        "Đã có chỉ số ghi nhận cho đồng hồ này trong ngày (xung đột dữ liệu đồng thời)");
            }
            throw ex;
        }

        return mapToResponseWithUsage(savedReading);
    }

    @Override
    @Transactional
    public UtilityReadingResponse update(Long id, UtilityReadingUpdateRequest request) {
        UtilityReading reading = getReadingOrThrow(id);
        validateWriteScope(reading.getMeter().getHotel().getId());
        
        repository.findPreviousReading(reading.getMeter().getId(), reading.getReadingDate())
                .ifPresent(prev -> {
                    if (!Boolean.TRUE.equals(request.getIsMeterReset()) &&
                        request.getReadingValue().compareTo(prev.getReadingValue()) < 0) {
                        throw new AppException(ErrorCode.INVALID_UTILITY_READING_VALUE,
                                "Chỉ số đọc (" + request.getReadingValue() + ") không được nhỏ hơn chỉ số trước đó (" + prev.getReadingValue() + ") khi không có cờ reset đồng hồ");
                    }
                });

        repository.findNextReading(reading.getMeter().getId(), reading.getReadingDate())
                .ifPresent(next -> {
                    if (!Boolean.TRUE.equals(next.getIsMeterReset()) &&
                        request.getReadingValue().compareTo(next.getReadingValue()) > 0) {
                        throw new AppException(ErrorCode.INVALID_UTILITY_READING_VALUE,
                                "Chỉ số đọc (" + request.getReadingValue() + ") không được lớn hơn chỉ số tiếp theo (" + next.getReadingValue() + ") khi chỉ số tiếp theo không có cờ reset đồng hồ");
                    }
                });

        reading.setReadingValue(request.getReadingValue());
        reading.setIsMeterReset(request.getIsMeterReset());
        reading.setUpdatedBy(resolveCurrentStaffId());
        
        return mapToResponseWithUsage(repository.save(reading));
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (!SecurityUtils.hasRole("CHAIN_ADMIN") && !SecurityUtils.hasRole("PROPERTY_MANAGER") && !SecurityUtils.hasRole("ENGINEERING")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thực hiện hành động này");
        }
        List<UtilityReading> readings = repository.findAllByIdInWithMeterAndHotel(ids);
        for (UtilityReading reading : readings) {
            validateWriteScope(reading.getMeter().getHotel().getId());
        }
        for (UtilityReading reading : readings) {
            reading.setIsDeleted(true);
        }
        repository.saveAll(readings);
    }

    private UtilityReading getReadingOrThrow(Long id) {
        return repository.findByIdWithMeterAndHotelAndRegion(id)
                .orElseThrow(() -> new AppException(ErrorCode.UTILITY_READING_NOT_FOUND, "Reading not found"));
    }
    
    private UtilityReadingResponse mapToResponseWithUsage(UtilityReading reading) {
        UtilityReadingResponse response = mapper.toResponse(reading);
        
        if (Boolean.TRUE.equals(reading.getIsMeterReset())) {
            response.setUsage(reading.getReadingValue());
        } else {
            repository.findPreviousReading(reading.getMeter().getId(), reading.getReadingDate())
                .ifPresentOrElse(
                    prev -> {
                        BigDecimal usage = reading.getReadingValue().subtract(prev.getReadingValue());
                        response.setUsage(usage.compareTo(BigDecimal.ZERO) >= 0 ? usage : BigDecimal.ZERO);
                    },
                    () -> response.setUsage(reading.getReadingValue())
                );
        }
        return response;
    }

    private UtilityReadingResponse mapToResponseWithUsageBulk(UtilityReading reading, BigDecimal previousValue) {
        UtilityReadingResponse response = mapper.toResponse(reading);
        
        if (Boolean.TRUE.equals(reading.getIsMeterReset()) || previousValue == null) {
            response.setUsage(reading.getReadingValue());
        } else {
            BigDecimal usage = reading.getReadingValue().subtract(previousValue);
            response.setUsage(usage.compareTo(BigDecimal.ZERO) >= 0 ? usage : BigDecimal.ZERO);
        }
        return response;
    }

    private void validateReadScope(Hotel hotel) {
        if (SecurityUtils.hasRole("CHAIN_ADMIN")) {
            return;
        }
        if (SecurityUtils.hasRole("PROPERTY_MANAGER") || SecurityUtils.hasRole("ENGINEERING")) {
            Staff staff = resolveCurrentStaff();
            if (!"PROPERTY".equalsIgnoreCase(staff.getScopeType()) || staff.getScopeEntityId() == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
            }
            if (!hotel.getId().equals(staff.getScopeEntityId().shortValue())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền truy cập chỉ số thuộc khách sạn khác");
            }
            return;
        }
        if (SecurityUtils.hasRole("REGION_MANAGER")) {
            Staff staff = resolveCurrentStaff();
            if (!"REGION".equalsIgnoreCase(staff.getScopeType()) || staff.getScopeEntityId() == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
            }
            if (!hotel.getRegion().getId().equals(staff.getScopeEntityId().shortValue())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ số không thuộc vùng quản lý");
            }
            return;
        }
        throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thực hiện hành động này");
    }

    private void validateWriteScope(Short hotelId) {
        if (SecurityUtils.hasRole("CHAIN_ADMIN")) {
            return;
        }
        if (SecurityUtils.hasRole("PROPERTY_MANAGER") || SecurityUtils.hasRole("ENGINEERING")) {
            Staff staff = resolveCurrentStaff();
            if (!"PROPERTY".equalsIgnoreCase(staff.getScopeType()) || staff.getScopeEntityId() == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
            }
            if (!staff.getScopeEntityId().equals(hotelId.intValue())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền thao tác trên khách sạn khác");
            }
            return;
        }
        if (SecurityUtils.hasRole("REGION_MANAGER")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Quản lý vùng không có quyền thêm/sửa/xóa chỉ số");
        }
        throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thực hiện hành động này");
    }

    private Staff resolveCurrentStaff() {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(staffRepository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername()
                        .flatMap(staffRepository::findByUsernameAndIsDeletedFalse))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy thông tin nhân viên từ phiên đăng nhập"));
    }

    private Integer resolveCurrentStaffId() {
        return resolveCurrentStaff().getId();
    }

    private Pageable buildPageable(UtilityReadingSearchDto request) {
        int page = request.getPage() != null && request.getPage() >= 0 ? request.getPage() : 0;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 10;

        String requestedSort = request.getSortBy();
        String sortProperty = (requestedSort != null && ALLOWED_READING_SORTS.contains(requestedSort)) ? requestedSort : "readingDate";

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortProperty);
        if (!"id".equals(sortProperty)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }

        return PageRequest.of(page, pageSize, sort);
    }

    private boolean isUniqueReadingViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        Throwable root = ex.getRootCause();
        String rootMsg = root != null ? root.getMessage() : "";
        String full = ((msg != null ? msg : "") + " " + (rootMsg != null ? rootMsg : "")).toLowerCase();
        return full.contains("uq_utility_reading")
                || (full.contains("23505") && full.contains("utility_reading"));
    }
}
