package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.CampaignResponse;

import java.util.List;

public interface CampaignService {

    CampaignResponse createCampaign(CampaignCreateRequest request);

    CampaignResponse updateCampaign(Integer id, CampaignUpdateRequest request);

    CampaignResponse getCampaignById(Integer id);

    PageResponse<CampaignResponse> filterCampaigns(CampaignSearchDto searchDto);

    void deleteCampaigns(List<Integer> ids);
}
