package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HolidayCalendarResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HolidayCalendar;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.HolidayCalendarMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.HolidayCalendarRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.HolidayCalendarService;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class HolidayCalendarServiceImpl implements HolidayCalendarService {

    HolidayCalendarRepository holidayCalendarRepository;
    PricingRuleRepository pricingRuleRepository;
    HolidayCalendarMapper holidayCalendarMapper;

    @Override
    public HolidayCalendarResponse getById(Integer id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID ngày lễ không được để trống");
        }
        HolidayCalendar entity = holidayCalendarRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOLIDAY_CALENDAR_NOT_FOUND, "Không tìm thấy ngày lễ ID: " + id));
        return holidayCalendarMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public HolidayCalendarResponse create(HolidayCalendarCreateRequest request) {
        if (holidayCalendarRepository.existsByNameAndDateAndIsDeletedFalse(request.getName(), request.getDate())) {
            throw new AppException(ErrorCode.HOLIDAY_CALENDAR_ALREADY_EXISTS,
                    "Ngày lễ '" + request.getName() + "' vào ngày " + request.getDate() + " đã tồn tại");
        }

        HolidayCalendar entity = holidayCalendarMapper.toEntity(request);
        entity.setStatus("ACTIVE");
        entity.setIsDeleted(false);

        HolidayCalendar saved = holidayCalendarRepository.save(entity);
        return holidayCalendarMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HolidayCalendarResponse update(Integer id, HolidayCalendarUpdateRequest request) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID ngày lễ không được để trống");
        }

        HolidayCalendar entity = holidayCalendarRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.HOLIDAY_CALENDAR_NOT_FOUND, "Không tìm thấy ngày lễ ID: " + id));

        holidayCalendarMapper.updateEntityFromRequest(entity, request);
        entity.setUpdatedAt(OffsetDateTime.now());

        HolidayCalendar updated = holidayCalendarRepository.save(entity);
        return holidayCalendarMapper.toResponse(updated);
    }

    @Override
    public PageResponse<HolidayCalendarResponse> filter(HolidayCalendarSearchDto searchDto) {
        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int pageSize = searchDto.getPageSize() != null ? searchDto.getPageSize() : 10;
        String sortBy = StringUtils.hasText(searchDto.getSortBy()) ? searchDto.getSortBy() : "date";
        String sortDir = StringUtils.hasText(searchDto.getSortDirection()) ? searchDto.getSortDirection() : "ASC";

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.fromString(sortDir), sortBy));

        String keyword = StringUtils.hasText(searchDto.getKeyword()) ? searchDto.getKeyword().trim() : null;
        String status = StringUtils.hasText(searchDto.getStatus()) ? searchDto.getStatus().trim() : null;

        Page<HolidayCalendar> resultPage = holidayCalendarRepository.filter(
                keyword,
                searchDto.getYear(),
                searchDto.getMonth(),
                status,
                pageable
        );

        return PageResponse.<HolidayCalendarResponse>builder()
                .content(holidayCalendarMapper.toResponseList(resultPage.getContent()))
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .first(resultPage.isFirst())
                .last(resultPage.isLast())
                .empty(resultPage.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Integer id : ids) {
            HolidayCalendar entity = holidayCalendarRepository.findByIdAndIsDeletedFalse(id).orElse(null);
            if (entity != null) {
                if (pricingRuleRepository.existsByHolidayCalendarIdAndIsDeletedFalse(id)) {
                    throw new AppException(ErrorCode.HOLIDAY_CALENDAR_IN_USE,
                            "Ngày lễ '" + entity.getName() + "' (ID: " + id + ") đang được sử dụng trong quy tắc giá, không thể xóa");
                }
                entity.setIsDeleted(true);
                entity.setUpdatedAt(OffsetDateTime.now());
                holidayCalendarRepository.save(entity);
            }
        }
    }

    @Override
    public List<HolidayCalendarResponse> getActiveHolidaysInYear(Integer year) {
        HolidayCalendarSearchDto searchDto = new HolidayCalendarSearchDto();
        searchDto.setYear(year);
        searchDto.setStatus("ACTIVE");
        searchDto.setPage(0);
        searchDto.setPageSize(100);
        return filter(searchDto).getContent();
    }
}
