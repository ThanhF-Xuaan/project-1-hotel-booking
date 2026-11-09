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
  SurchargePreviewRequest,
  SurchargeProposalResponse,
  CheckoutSettlementResponse,
  BookingNoShowRequest,
  BookingCancelPreviewResponse,
  BookingCancelRequest,
  BookingCancelResponse,
  RoomSwapRequest,
  RoomSwapResponse,
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

  getById: (id: number, signal?: AbortSignal) =>
    apiClient.get<BookingResponse>(`${BASE_URL}/${id}`, { signal }),

  getByNumber: (bookingNumber: string) =>
    apiClient.get<BookingResponse>(`${BASE_URL}/number/${bookingNumber}`),

  updateStatus: (id: number, status: BookingStatus) =>
    apiClient.put<BookingResponse>(`${BASE_URL}/${id}/status`, { status }),

  previewCancelBooking: (id: number) =>
    apiClient.get<BookingCancelPreviewResponse>(`${BASE_URL}/${id}/cancel-preview`),

  cancelBooking: (id: number, data: BookingCancelRequest) =>
    apiClient.post<BookingCancelResponse>(`${BASE_URL}/${id}/cancel`, data),

  previewCancelBookingRoom: (bookingRoomId: number) =>
    apiClient.get<BookingCancelPreviewResponse>(`${BASE_URL}/rooms/${bookingRoomId}/cancel-preview`),

  cancelBookingRoom: (bookingRoomId: number, data: BookingCancelRequest) =>
    apiClient.post<BookingCancelResponse>(`${BASE_URL}/rooms/${bookingRoomId}/cancel`, data),

  markAsNoShow: (id: number, data: BookingNoShowRequest) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/${id}/no-show`, data),

  markRoomAsNoShow: (bookingRoomId: number, data: BookingNoShowRequest) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/rooms/${bookingRoomId}/no-show`, data),

  assignRoom: (bookingRoomId: number, roomInstanceId: number) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/rooms/${bookingRoomId}/assign`, { roomInstanceId }),

  addCharge: (bookingRoomId: number, data: BookingChargeCreateRequest) =>
    apiClient.post<BookingChargeResponse>(`${BASE_URL}/rooms/${bookingRoomId}/charges`, data),

  deleteCharge: (chargeId: number) =>
    apiClient.delete<void>(`${BASE_URL}/charges/${chargeId}`),

  previewSurcharge: (bookingRoomId: number, data: SurchargePreviewRequest) =>
    apiClient.post<SurchargeProposalResponse>(`${BASE_URL}/rooms/${bookingRoomId}/preview-surcharge`, data),

  checkIn: (bookingRoomId: number) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/rooms/${bookingRoomId}/check-in`, {}),

  getCheckoutSettlement: (bookingRoomId: number) =>
    apiClient.get<CheckoutSettlementResponse>(`${BASE_URL}/rooms/${bookingRoomId}/checkout-settlement`),

  checkOut: (bookingRoomId: number) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/rooms/${bookingRoomId}/check-out`, {}),

  calculateWalkInQuote: (data: import('../types/booking.types').WalkInQuoteRequest) =>
    apiClient.post<import('../types/booking.types').WalkInQuoteResponse>(`${BASE_URL}/walk-in/quote`, data),

  createWalkInBooking: (data: import('../types/booking.types').WalkInBookingRequest) =>
    apiClient.post<BookingResponse>(`${BASE_URL}/walk-in`, data),

  previewPublicCancelBooking: (bookingNumber: string) =>
    apiClient.get<BookingCancelPreviewResponse>(`/api/v1/public/bookings/${bookingNumber}/cancel-preview`),

  cancelPublicBooking: (bookingNumber: string, data: BookingCancelRequest) =>
    apiClient.post<BookingCancelResponse>(`/api/v1/public/bookings/${bookingNumber}/cancel`, data),

  swapRoom: (bookingRoomId: number, data: RoomSwapRequest) =>
    apiClient.post<RoomSwapResponse>(`${BASE_URL}/rooms/${bookingRoomId}/swap-room`, data),

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
