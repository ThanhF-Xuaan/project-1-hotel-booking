import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  VoucherDto,
  VoucherCreateRequest,
  VoucherUpdateRequest,
  VoucherSearchDto,
  VoucherValidateRequest,
  VoucherValidationResponse,
} from '../types/voucher.types'

const BASE_URL = '/api/v1/pricing/vouchers'
const PUBLIC_BASE_URL = '/api/v1/public/pricing/vouchers'

export const voucherService = {
  filter: (params: VoucherSearchDto) =>
    apiClient.post<PageResponse<VoucherDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<VoucherDto>(`${BASE_URL}/${id}`),

  create: (data: VoucherCreateRequest) =>
    apiClient.post<VoucherDto>(BASE_URL, data),

  update: (id: number, data: VoucherUpdateRequest) =>
    apiClient.put<VoucherDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),

  validateVoucher: (data: VoucherValidateRequest) =>
    apiClient.post<VoucherValidationResponse>(`${PUBLIC_BASE_URL}/validate`, data),
}

export default voucherService

