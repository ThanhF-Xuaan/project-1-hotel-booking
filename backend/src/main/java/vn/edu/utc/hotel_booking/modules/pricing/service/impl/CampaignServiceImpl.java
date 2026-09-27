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
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.CampaignResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Campaign;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.CampaignMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.CampaignRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.CampaignService;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class CampaignServiceImpl implements CampaignService {

    CampaignRepository campaignRepository;
    HotelRepository hotelRepository;
    CampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn id: " + request.getHotelId()));

        if (campaignRepository.findByHotelIdAndNameAndIsDeletedFalse(request.getHotelId(), request.getName()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Tên chiến dịch đã tồn tại trong khách sạn này");
        }

        Campaign campaign = campaignMapper.toEntity(request);
        campaign.setHotel(hotel);

        Campaign saved = campaignRepository.save(campaign);
        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(Integer id, CampaignUpdateRequest request) {
        Campaign campaign = campaignRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND, "Không tìm thấy chiến dịch id: " + id));

        campaignMapper.updateEntity(campaign, request);
        Campaign updated = campaignRepository.save(campaign);
        return campaignMapper.toResponse(updated);
    }

    @Override
    public CampaignResponse getCampaignById(Integer id) {
        Campaign campaign = campaignRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND, "Không tìm thấy chiến dịch id: " + id));
        return campaignMapper.toResponse(campaign);
    }

    @Override
    public PageResponse<CampaignResponse> filterCampaigns(CampaignSearchDto searchDto) {
        Pageable pageable = PageRequest.of(
                searchDto.getPage(),
                searchDto.getPageSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy())
        );

        Page<Campaign> page = campaignRepository.filterCampaigns(
                searchDto.getHotelId(),
                searchDto.getName(),
                searchDto.getActiveOnDate(),
                searchDto.getStatus(),
                pageable
        );

        return PageResponse.<CampaignResponse>builder()
                .content(campaignMapper.toResponseList(page.getContent()))
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
    public void deleteCampaigns(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            campaignRepository.softDeleteByIds(ids);
        }
    }
}
