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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityReadingServiceImpl implements UtilityReadingService {

    private final UtilityReadingRepository repository;
    private final UtilityMeterRepository meterRepository;
    private final UtilityReadingMapper mapper;

    @Override
    public PageResponse<UtilityReadingResponse> search(UtilityReadingSearchDto request) {
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), Sort.by("readingDate").descending());
        Page<UtilityReading> page = repository.search(request.getMeterId(), request.getFromDate(), request.getToDate(), pageable);
        return PageResponse.from(page.map(this::mapToResponseWithUsage));
    }

    @Override
    public UtilityReadingResponse getById(Long id) {
        return mapToResponseWithUsage(getReadingOrThrow(id));
    }

    @Override
    @Transactional
    public UtilityReadingResponse create(UtilityReadingCreateRequest request) {
        if (repository.existsByMeterIdAndReadingDateAndIsDeletedFalse(request.getMeterId(), request.getReadingDate())) {
            throw new AppException(ErrorCode.UTILITY_READING_ALREADY_EXISTS, "Reading already exists for this date");
        }

        UtilityMeter meter = meterRepository.findById(request.getMeterId())
                .orElseThrow(() -> new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Meter not found"));
                
        if (Boolean.TRUE.equals(meter.getIsDeleted())) {
            throw new AppException(ErrorCode.UTILITY_METER_NOT_FOUND, "Cannot add reading to a deleted meter");
        }

        UtilityReading reading = mapper.toEntity(request);
        reading.setMeter(meter);
        
        // TODO: Get actual user ID from SecurityContext
        reading.setRecordedBy(1); 
        
        return mapToResponseWithUsage(repository.save(reading));
    }

    @Override
    @Transactional
    public UtilityReadingResponse update(Long id, UtilityReadingUpdateRequest request) {
        UtilityReading reading = getReadingOrThrow(id);
        
        reading.setReadingValue(request.getReadingValue());
        reading.setIsMeterReset(request.getIsMeterReset());
        
        // TODO: Get actual user ID from SecurityContext
        reading.setUpdatedBy(1);
        
        return mapToResponseWithUsage(repository.save(reading));
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        List<UtilityReading> readings = repository.findAllById(ids);
        readings.forEach(r -> r.setIsDeleted(true));
        repository.saveAll(readings);
    }

    private UtilityReading getReadingOrThrow(Long id) {
        UtilityReading reading = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UTILITY_READING_NOT_FOUND, "Reading not found"));
        if (Boolean.TRUE.equals(reading.getIsDeleted())) {
            throw new AppException(ErrorCode.UTILITY_READING_NOT_FOUND, "Reading not found");
        }
        return reading;
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
                        // If negative (meter went backwards without reset flag), just use 0 or current value
                        response.setUsage(usage.compareTo(BigDecimal.ZERO) >= 0 ? usage : BigDecimal.ZERO);
                    },
                    () -> response.setUsage(reading.getReadingValue())
                );
        }
        return response;
    }
}
