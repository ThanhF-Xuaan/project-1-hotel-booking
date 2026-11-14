import { apiClient } from '../../../core/api/client';
import type {
  InitiateDraftSessionRequest,
  DraftSessionResponse,
  CancelSessionRequest,
} from '../types/portal.types';

const BASE_URL = '/api/v1/public/booking-sessions';

export const bookingSessionService = {
  /**
   * Khởi tạo hoặc cập nhật phiên giữ phòng nháp (15 phút)
   * Tự động tạo RoomInventoryHold và tăng locked_rooms trong DB
   */
  initiateDraftSession: (request: InitiateDraftSessionRequest) =>
    apiClient.post<DraftSessionResponse>(`${BASE_URL}/draft`, request),

  /**
   * Lấy thông tin phiên đặt phòng nháp và số giây còn lại
   */
  getDraftSession: (sessionId: string) =>
    apiClient.get<DraftSessionResponse>(`${BASE_URL}/draft/${encodeURIComponent(sessionId)}`),

  /**
   * Hủy phiên đặt phòng chủ động (Giải phóng kho phòng và xóa session Redis)
   */
  cancelSession: (sessionId: string, reason: 'USER_CANCELLED' | 'EXPIRED' | 'SESSION_SUPERSEDED' = 'USER_CANCELLED') => {
    const payload: CancelSessionRequest = { sessionId, reason };
    return apiClient.post<void>(`${BASE_URL}/cancel`, payload);
  },
};
