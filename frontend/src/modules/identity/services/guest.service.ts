import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  GuestDto,
  GuestCreateRequest,
  GuestUpdateRequest,
  GuestSearchDto,
  GuestStayHistoryResponse,
} from '../types/guest.types';

const BASE_URL = '/api/v1/guests';

export const guestService = {
  filter: (params: GuestSearchDto) =>
    apiClient.post<PageResponse<GuestDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<GuestDto>(`${BASE_URL}/${id}`),

  getByPhone: (phone: string) => apiClient.get<GuestDto>(`${BASE_URL}/phone/${phone}`),

  getByIdentityNumber: (num: string) => apiClient.get<GuestDto>(`${BASE_URL}/identity/${num}`),

  getStayHistory: (id: number) =>
    apiClient.get<GuestStayHistoryResponse>(`${BASE_URL}/${id}/history`),

  create: (data: GuestCreateRequest) => apiClient.post<GuestDto>(BASE_URL, data),

  update: (id: number, data: GuestUpdateRequest) =>
    apiClient.put<GuestDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
};

export default guestService;

