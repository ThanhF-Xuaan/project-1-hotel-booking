import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';
import type {
  BookingCreateRequest,
  BookingSearchDto,
  BookingResponse,
  BookingStatus,
  BookingChargeCreateRequest,
  BookingChargeResponse,
  RoomingListPreviewResponse,
  RoomingListImportResultResponse,
  RoomingListConfirmRequest,
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

  previewRoomingList: (bookingId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<RoomingListPreviewResponse>(
      `${BASE_URL}/${bookingId}/rooming-list/preview`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  importRoomingList: (bookingId: number, file: File, confirm: boolean = false) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<RoomingListImportResultResponse>(
      `${BASE_URL}/${bookingId}/rooming-list/import?confirm=${confirm}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  confirmRoomingList: (bookingId: number, data: RoomingListConfirmRequest) =>
    apiClient.post<RoomingListImportResultResponse>(
      `${BASE_URL}/${bookingId}/rooming-list/confirm`,
      data
    ),

  getVacantRooms: (hotelId?: number) =>
    apiClient.get<RoomInstanceDto[]>(
      `${BASE_URL}/vacant-rooms${hotelId ? `?hotelId=${hotelId}` : ''}`
    ),
};

export default bookingService;
