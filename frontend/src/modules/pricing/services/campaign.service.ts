import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  CampaignDto,
  CampaignCreateRequest,
  CampaignUpdateRequest,
  CampaignSearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/campaigns'

export const campaignService = {
  filter: (params: CampaignSearchDto) =>
    apiClient.post<PageResponse<CampaignDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<CampaignDto>(`${BASE_URL}/${id}`),

  create: (data: CampaignCreateRequest) => apiClient.post<CampaignDto>(BASE_URL, data),

  update: (id: number, data: CampaignUpdateRequest) =>
    apiClient.put<CampaignDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default campaignService
