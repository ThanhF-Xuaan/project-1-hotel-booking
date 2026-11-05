import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  CompanyDto,
  CompanyCreateRequest,
  CompanyUpdateRequest,
  CompanySearchDto,
} from '../types/company.types';

const BASE_URL = '/api/v1/companies';

export const companyService = {
  filter: (params: CompanySearchDto) =>
    apiClient.post<PageResponse<CompanyDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<CompanyDto>(`${BASE_URL}/${id}`),

  create: (data: CompanyCreateRequest) => apiClient.post<CompanyDto>(BASE_URL, data),

  update: (id: number, data: CompanyUpdateRequest) =>
    apiClient.put<CompanyDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
};

export default companyService;

