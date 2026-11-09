import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';

export interface StaffDto {
  id: number;
  fullName: string;
  username: string;
  email?: string;
  phone?: string;
  roleName?: string;
  roleCode?: string;
  departmentName?: string;
  status?: string;
}

export interface StaffSearchDto {
  keyword?: string;
  roleId?: number;
  departmentId?: number;
  scopeType?: string;
  scopeEntityId?: number;
  status?: string;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

const BASE_URL = '/api/v1/staffs';

export const staffService = {
  filter: (params: StaffSearchDto = {}) =>
    apiClient.post<PageResponse<StaffDto>>(`${BASE_URL}/filter`, {
      page: 0,
      pageSize: 100,
      status: 'ACTIVE',
      ...params,
    }),

  getById: (id: number) => apiClient.get<StaffDto>(`${BASE_URL}/${id}`),
};

export default staffService;

