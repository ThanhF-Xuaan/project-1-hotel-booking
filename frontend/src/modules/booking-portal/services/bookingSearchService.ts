import { apiClient } from '../../../core/api/client';
import type {
  AvailableRoomTypeApiResponse,
  CustomerSearchCriteria,
  DestinationSuggestionDto,
} from '../types/portal.types';

const BASE_URL = '/api/v1/public/booking-search';

export const bookingSearchService = {
  /**
   * Tìm kiếm các hạng phòng khả dụng của khách sạn kèm bảng giá Pricing Pipeline thời gian thực
   */
  searchRooms: (criteria: CustomerSearchCriteria) =>
    apiClient.post<AvailableRoomTypeApiResponse[]>(`${BASE_URL}/rooms`, {
      hotelId: criteria.hotelId,
      checkInDate: criteria.checkInDate,
      checkOutDate: criteria.checkOutDate,
      roomsCount: criteria.roomsCount || 1,
    }),

  /**
   * Lấy danh sách điểm đến gợi ý (Khu vực / Khách sạn)
   */
  getDestinations: (keyword?: string) =>
    apiClient.get<DestinationSuggestionDto[]>(`${BASE_URL}/destinations`, {
      params: keyword ? { keyword } : undefined,
    }),
};

export default bookingSearchService;

