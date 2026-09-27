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
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HolidayCalendar;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.PricingRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.HolidayCalendarRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.PricingRuleService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PricingRuleServiceImpl implements PricingRuleService {

    PricingRuleRepository pricingRuleRepository;
    HotelRoomTypeRepository hotelRoomTypeRepository;
    PricingRuleTypeRepository pricingRuleTypeRepository;
    HolidayCalendarRepository holidayCalendarRepository;
    PricingRuleMapper pricingRuleMapper;

    @Override
    @Transactional
    public PricingRuleResponse createPricingRule(PricingRuleCreateRequest request) {
        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdAndIsDeletedFalse(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                        "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        PricingRuleType ruleType = pricingRuleTypeRepository.findByCode(request.getRuleTypeCode())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Không tìm thấy loại quy tắc: " + request.getRuleTypeCode()));

        HolidayCalendar holidayCalendar = null;
        if (request.getHolidayCalendarId() != null) {
            holidayCalendar = holidayCalendarRepository.findById(request.getHolidayCalendarId())
                    .orElse(null);
        }

        PricingRule pricingRule = pricingRuleMapper.toEntity(request);
        pricingRule.setHotelRoomType(hotelRoomType);
        pricingRule.setRuleType(ruleType);
        pricingRule.setHolidayCalendar(holidayCalendar);

        PricingRule saved = pricingRuleRepository.save(pricingRule);
        return pricingRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PricingRuleResponse updatePricingRule(Integer id, PricingRuleUpdateRequest request) {
        PricingRule pricingRule = pricingRuleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICING_RULE_NOT_FOUND,
                        "Không tìm thấy quy tắc giá id: " + id));

        pricingRuleMapper.updateEntity(pricingRule, request);

        if (request.getHolidayCalendarId() != null) {
            HolidayCalendar holidayCalendar = holidayCalendarRepository.findById(request.getHolidayCalendarId())
                    .orElse(null);
            pricingRule.setHolidayCalendar(holidayCalendar);
        } else {
            pricingRule.setHolidayCalendar(null);
        }

        PricingRule updated = pricingRuleRepository.save(pricingRule);
        return pricingRuleMapper.toResponse(updated);
    }

    @Override
    public PricingRuleResponse getPricingRuleById(Integer id) {
        PricingRule pricingRule = pricingRuleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICING_RULE_NOT_FOUND,
                        "Không tìm thấy quy tắc giá id: " + id));
        return pricingRuleMapper.toResponse(pricingRule);
    }

    @Override
    public PageResponse<PricingRuleResponse> filterPricingRules(PricingRuleSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<PricingRule> page = pricingRuleRepository.filterPricingRules(
                searchDto.getHotelRoomTypeId(),
                searchDto.getRuleTypeCode(),
                searchDto.getActiveOnDate(),
                searchDto.getStatus(),
                pageable
        );

        return PageResponse.<PricingRuleResponse>builder()
                .content(pricingRuleMapper.toResponseList(page.getContent()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    @Override
    @Transactional
    public void deletePricingRules(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            pricingRuleRepository.softDeleteByIds(ids);
        }
    }
}
