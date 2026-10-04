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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityMeterServiceImpl implements UtilityMeterService {

    private final UtilityMeterRepository repository;
    private final UtilityMeterMapper mapper;
    private final HotelRepository hotelRepository;

    @Override
    public PageResponse<UtilityMeterResponse> search(UtilityMeterSearchDto request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize(), Sort.by("id").descending());
        Page<UtilityMeter> page = repository.search(request.getHotelId(), request.getMeterType(), request.getKeyword(), pageable);
        return PageResponse.from(page.map(mapper::toResponse));
    }

    @Override
    public UtilityMeterResponse getById(Integer id) {
        return mapper.toResponse(getMeterOrThrow(id));
    }

    @Override
    @Transactional
    public UtilityMeterResponse create(UtilityMeterCreateRequest request) {
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
        List<UtilityMeter> meters = repository.findAllById(ids);
        meters.forEach(m -> m.setIsDeleted(true));
        repository.saveAll(meters);
    }

    private UtilityMeter getMeterOrThrow(Integer id) {
        UtilityMeter meter = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Utility meter not found"));
        if (Boolean.TRUE.equals(meter.getIsDeleted())) {
            throw new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Utility meter not found");
        }
        return meter;
    }
}
