import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  RegionDto,
  RegionCreateRequest,
  RegionUpdateRequest,
  RegionSearchDto,
} from '../types/organization.types'

const BASE_URL = '/api/v1/regions'

export const regionService = {
  filter: (params: RegionSearchDto) =>
    apiClient.post<PageResponse<RegionDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<RegionDto>(`${BASE_URL}/${id}`),

  create: (data: RegionCreateRequest) => apiClient.post<RegionDto>(BASE_URL, data),

  update: (id: number, data: RegionUpdateRequest) =>
    apiClient.put<RegionDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) => apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default regionService
