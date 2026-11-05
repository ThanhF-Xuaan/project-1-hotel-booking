import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  StaffDto,
  StaffCreateRequest,
  StaffUpdateRequest,
  StaffSearchDto,
  StaffStatusUpdateRequest,
  StaffPasswordResetRequest,
} from '../types/staff.types';

const BASE_URL = '/api/v1/staffs';

export const staffService = {
  filter: (params: StaffSearchDto) =>
    apiClient.post<PageResponse<StaffDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<StaffDto>(`${BASE_URL}/${id}`),

  create: (data: StaffCreateRequest) => apiClient.post<StaffDto>(BASE_URL, data),

  update: (id: number, data: StaffUpdateRequest) =>
    apiClient.put<StaffDto>(`${BASE_URL}/${id}`, data),

  updateStatus: (id: number, data: StaffStatusUpdateRequest) =>
    apiClient.patch<StaffDto>(`${BASE_URL}/${id}/status`, data),

  resetPassword: (id: number, data: StaffPasswordResetRequest) =>
    apiClient.post<void>(`${BASE_URL}/${id}/reset-password`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
};

export default staffService;

