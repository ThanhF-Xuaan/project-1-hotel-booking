import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  TaxCategoryDto,
  TaxCategoryCreateRequest,
  TaxCategoryUpdateRequest,
  TaxCategorySearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/tax-categories'

export const taxCategoryService = {
  filter: (params: TaxCategorySearchDto) =>
    apiClient.post<PageResponse<TaxCategoryDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<TaxCategoryDto>(`${BASE_URL}/${id}`),

  create: (data: TaxCategoryCreateRequest) =>
    apiClient.post<TaxCategoryDto>(BASE_URL, data),

  update: (id: number, data: TaxCategoryUpdateRequest) =>
    apiClient.put<TaxCategoryDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),

  getAllActive: async () => {
    const res = await apiClient.post<PageResponse<TaxCategoryDto>>(`${BASE_URL}/filter`, {
      status: 'ACTIVE',
      page: 0,
      pageSize: 100,
    })
    return res.result?.content || []
  },
}

export default taxCategoryService
