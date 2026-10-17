package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.RequiredArgsConstructor;
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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityMeterResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.mapper.UtilityMeterMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityMeterRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.UtilityMeterService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityMeterServiceImpl implements UtilityMeterService {

    private static final Set<String> ALLOWED_METER_SORTS = Set.of("id", "meterCode", "meterType", "locationLabel", "createdAt");

    private final UtilityMeterRepository repository;
    private final UtilityMeterMapper mapper;
    private final HotelRepository hotelRepository;
    private final StaffRepository staffRepository;

    @Override
    public PageResponse<UtilityMeterResponse> search(UtilityMeterSearchDto request) {
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
            effectiveRegionId = userRegionId;
            effectiveHotelId = request.getHotelId();
        } else {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thực hiện hành động này");
        }

        Pageable pageable = buildPageable(request);
        Page<UtilityMeter> page = repository.search(effectiveHotelId, effectiveRegionId, request.getMeterType(), request.getKeyword(), pageable);
        return PageResponse.from(page.map(mapper::toResponse));
    }

    @Override
    public UtilityMeterResponse getById(Integer id) {
        UtilityMeter meter = getMeterOrThrow(id);
        validateReadScope(meter);
        return mapper.toResponse(meter);
    }

    @Override
    @Transactional
    public UtilityMeterResponse create(UtilityMeterCreateRequest request) {
        validateWriteScope(request.getHotelId());

        if (repository.existsByHotelIdAndMeterCodeAndIsDeletedFalse(request.getHotelId(), request.getMeterCode())) {
            throw new AppException(ErrorCode.UTILITY_METER_ALREADY_EXISTS, "Meter code already exists for this hotel");
        }

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Hotel not found"));

        UtilityMeter meter = mapper.toEntity(request);
        meter.setHotel(hotel);
        return mapper.toResponse(repository.save(meter));
    }

    @Override
    @Transactional
    public UtilityMeterResponse update(Integer id, UtilityMeterUpdateRequest request) {
        UtilityMeter meter = getMeterOrThrow(id);
        validateWriteScope(meter.getHotel().getId());

        if (!meter.getMeterCode().equals(request.getMeterCode()) && 
            repository.existsByHotelIdAndMeterCodeAndIsDeletedFalse(meter.getHotel().getId(), request.getMeterCode())) {
            throw new AppException(ErrorCode.UTILITY_METER_ALREADY_EXISTS, "Meter code already exists for this hotel");
        }

        meter.setMeterCode(request.getMeterCode());
        meter.setLocationLabel(request.getLocationLabel());
        return mapper.toResponse(repository.save(meter));
    }

    @Override
    @Transactional
    public void delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (!SecurityUtils.hasRole("CHAIN_ADMIN") && !SecurityUtils.hasRole("PROPERTY_MANAGER") && !SecurityUtils.hasRole("ENGINEERING")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thực hiện hành động này");
        }
        List<UtilityMeter> meters = repository.findAllByIdInWithHotel(ids);
        for (UtilityMeter meter : meters) {
            validateWriteScope(meter.getHotel().getId());
        }
        for (UtilityMeter meter : meters) {
            meter.setIsDeleted(true);
        }
        repository.saveAll(meters);
    }

    private UtilityMeter getMeterOrThrow(Integer id) {
        return repository.findByIdWithHotelAndRegion(id)
                .orElseThrow(() -> new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Utility meter not found"));
    }

    private void validateReadScope(UtilityMeter meter) {
        if (SecurityUtils.hasRole("CHAIN_ADMIN")) {
            return;
        }
        if (SecurityUtils.hasRole("PROPERTY_MANAGER") || SecurityUtils.hasRole("ENGINEERING")) {
            Staff staff = resolveCurrentStaff();
            if (!"PROPERTY".equalsIgnoreCase(staff.getScopeType()) || staff.getScopeEntityId() == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
            }
            if (!meter.getHotel().getId().equals(staff.getScopeEntityId().shortValue())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền truy cập đồng hồ thuộc khách sạn khác");
            }
            return;
        }
        if (SecurityUtils.hasRole("REGION_MANAGER")) {
            Staff staff = resolveCurrentStaff();
            if (!"REGION".equalsIgnoreCase(staff.getScopeType()) || staff.getScopeEntityId() == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Cấu hình phạm vi quản lý của nhân viên không hợp lệ");
            }
            if (!meter.getHotel().getRegion().getId().equals(staff.getScopeEntityId().shortValue())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Đồng hồ không thuộc vùng quản lý");
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
            throw new AppException(ErrorCode.UNAUTHORIZED, "Quản lý vùng không có quyền thêm/sửa/xóa đồng hồ đo");
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

    private Pageable buildPageable(UtilityMeterSearchDto request) {
        int page = request.getPage() != null && request.getPage() >= 0 ? request.getPage() : 0;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 10;

        String requestedSort = request.getSortBy();
        String sortProperty = (requestedSort != null && ALLOWED_METER_SORTS.contains(requestedSort)) ? requestedSort : "id";

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortProperty);
        if (!"id".equals(sortProperty)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }

        return PageRequest.of(page, pageSize, sort);
    }
}
