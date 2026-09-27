import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  BookingCreateRequest,
  BookingSearchDto,
  BookingResponse,
  BookingStatus,
  BookingChargeCreateRequest,
  BookingChargeResponse,
} from '../types/booking.types';

const BASE_URL = '/api/v1/bookings';

export const bookingService = {
  create: (data: BookingCreateRequest) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/create`, data),

  filter: (params: BookingSearchDto) =>
    apiClient.post<PageResponse<BookingResponse>>(`${BASE_URL}/filter`, params),

  getById: (id: number) =>
    apiClient.get<BookingResponse>(`${BASE_URL}/${id}`),

  getByNumber: (bookingNumber: string) =>
    apiClient.get<BookingResponse>(`${BASE_URL}/number/${bookingNumber}`),

  updateStatus: (id: number, status: BookingStatus) =>
    apiClient.put<BookingResponse>(`${BASE_URL}/${id}/status`, { status }),

  assignRoom: (bookingRoomId: number, roomInstanceId: number) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/rooms/${bookingRoomId}/assign`, { roomInstanceId }),

  addCharge: (bookingRoomId: number, data: BookingChargeCreateRequest) =>
    apiClient.post<BookingChargeResponse>(`${BASE_URL}/rooms/${bookingRoomId}/charges`, data),

  checkIn: (bookingRoomId: number) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/rooms/${bookingRoomId}/check-in`, {}),

  checkOut: (bookingRoomId: number) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/rooms/${bookingRoomId}/check-out`, {}),
};

export default bookingService;
