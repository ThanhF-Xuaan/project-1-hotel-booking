package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
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
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Campaign;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.DiscountRuleMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.CampaignService;
import vn.edu.utc.hotel_booking.modules.pricing.service.DiscountRuleService;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class DiscountRuleServiceImpl implements DiscountRuleService {

    DiscountRuleRepository discountRuleRepository;
    DiscountRuleTypeRepository discountRuleTypeRepository;
    HotelRoomTypeService hotelRoomTypeService;
    CampaignService campaignService;
    DiscountRuleMapper discountRuleMapper;

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public DiscountRuleResponse createDiscountRule(DiscountRuleCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày kết thúc quy tắc giảm giá phải sau hoặc bằng ngày bắt đầu");
        }
        if ("PERCENT".equalsIgnoreCase(request.getDiscountType()) && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mức giảm giá theo phần trăm không được vượt quá 100%");
        }

        HotelRoomType hotelRoomType = hotelRoomTypeService.getEntityById(request.getHotelRoomTypeId());

        DiscountRuleType ruleType = discountRuleTypeRepository.findByCode(request.getRuleTypeCode())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        "Không tìm thấy loại quy tắc giảm giá: " + request.getRuleTypeCode()));

        Campaign campaign = null;
        boolean isCampaignType = "SPECIAL_CAMPAIGN".equalsIgnoreCase(request.getRuleTypeCode()) || request.getCampaignId() != null;
        if (isCampaignType) {
            if (request.getCampaignId() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Quy tắc giảm giá theo chiến dịch bắt buộc phải chọn Chiến dịch áp dụng");
            }
            campaign = campaignService.getEntityById(request.getCampaignId());
            if (request.getStartDate().isBefore(campaign.getStartDate()) || request.getEndDate().isAfter(campaign.getEndDate())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        String.format("Thời gian hiệu lực của quy tắc giảm giá (%s đến %s) phải nằm trong khoảng thời gian diễn ra chiến dịch '%s' (%s đến %s)",
                                request.getStartDate(), request.getEndDate(), campaign.getName(), campaign.getStartDate(), campaign.getEndDate()));
            }
        }

        DiscountRule discountRule = discountRuleMapper.toEntity(request);
        discountRule.setHotelRoomType(hotelRoomType);
        discountRule.setRuleType(ruleType);
        discountRule.setCampaign(campaign);

        DiscountRule saved = discountRuleRepository.save(discountRule);
        return discountRuleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ratePlans", allEntries = true)
    public DiscountRuleResponse updateDiscountRule(Integer id, DiscountRuleUpdateRequest request) {
        DiscountRule discountRule = discountRuleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy quy tắc giảm giá ID: " + id));

        if (request.getEndDate() != null && request.getStartDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày kết thúc quy tắc giảm giá phải sau hoặc bằng ngày bắt đầu");
        }
        if ("PERCENT".equalsIgnoreCase(request.getDiscountType()) && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mức giảm giá theo phần trăm không được vượt quá 100%");
        }

        discountRuleMapper.updateEntity(discountRule, request);

        boolean isCampaignType = "SPECIAL_CAMPAIGN".equalsIgnoreCase(discountRule.getRuleType().getCode()) || request.getCampaignId() != null;
        if (isCampaignType) {
            if (request.getCampaignId() == null && discountRule.getCampaign() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Quy tắc giảm giá theo chiến dịch bắt buộc phải chọn Chiến dịch áp dụng");
            }
            Integer targetCampaignId = request.getCampaignId() != null ? request.getCampaignId() : discountRule.getCampaign().getId();
            Campaign campaign = campaignService.getEntityById(targetCampaignId);
            if (discountRule.getStartDate().isBefore(campaign.getStartDate()) || discountRule.getEndDate().isAfter(campaign.getEndDate())) {
                throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                        String.format("Thời gian hiệu lực của quy tắc giảm giá (%s đến %s) phải nằm trong khoảng thời gian diễn ra chiến dịch '%s' (%s đến %s)",
                                discountRule.getStartDate(), discountRule.getEndDate(), campaign.getName(), campaign.getStartDate(), campaign.getEndDate()));
            }
            discountRule.setCampaign(campaign);
        } else {
            discountRule.setCampaign(null);
        }

        DiscountRule updated = discountRuleRepository.save(discountRule);
        return discountRuleMapper.toResponse(updated);
    }

    @Override
    public DiscountRuleResponse getDiscountRuleById(Integer id) {
        DiscountRule discountRule = discountRuleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy quy tắc giảm giá ID: " + id));
        return discountRuleMapper.toResponse(discountRule);
    }

    @Override
    public PageResponse<DiscountRuleResponse> filterDiscountRules(DiscountRuleSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<DiscountRule> page = discountRuleRepository.filterDiscountRules(
                searchDto.getHotelRoomTypeId(),
                searchDto.getCampaignId(),
                searchDto.getRuleTypeCode(),
                searchDto.getStatus(),
                searchDto.getActiveOnDate(),
                pageable
        );

        return PageResponse.<DiscountRuleResponse>builder()
                .content(discountRuleMapper.toResponseList(page.getContent()))
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
    @CacheEvict(value = "ratePlans", allEntries = true)
    public void deleteDiscountRules(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            discountRuleRepository.softDeleteByIds(ids);
        }
    }
}
