import { apiClient } from '../../../core/api/client';
import type {
  StayGuestResponse,
  StayGuestCreateRequest,
  StayGuestUpdateRequest,
} from '../types/stayGuest.types';

const BASE_URL = '/api/v1/stay-guests';

export const stayGuestService = {
  getByBookingRoomId: (bookingRoomId: number) =>
    apiClient.get<StayGuestResponse[]>(`${BASE_URL}/by-booking-room/${bookingRoomId}`),

  getById: (id: number) =>
    apiClient.get<StayGuestResponse>(`${BASE_URL}/${id}`),

  create: (data: StayGuestCreateRequest) =>
    apiClient.post<StayGuestResponse>(`${BASE_URL}/create`, data),

  update: (id: number, data: StayGuestUpdateRequest) =>
    apiClient.put<StayGuestResponse>(`${BASE_URL}/update/${id}`, data),

  delete: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
};

export default stayGuestService;
