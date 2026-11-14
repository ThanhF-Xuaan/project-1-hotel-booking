import { apiClient } from '../../../core/api/client';
import type {
  AvailableRoomTypeApiResponse,
  CustomerSearchCriteria,
  DestinationSuggestionDto,
  UnifiedSearchResponse,
  RoomPriceQuoteRequest,
  RoomPriceQuoteResponse,
  CartPriceQuoteRequest,
  CartPriceQuoteResponse,
} from '../types/portal.types';

const BASE_URL = '/api/v1/public/booking-search';

export const bookingSearchService = {
  /**
   * Tìm kiếm hợp nhất (Theo Vùng/Thành phố hoặc Khách sạn cơ sở)
   */
  searchUnified: (criteria: CustomerSearchCriteria) =>
    apiClient.post<UnifiedSearchResponse>(`${BASE_URL}/unified`, {
      destinationType: criteria.destinationType,
      destinationId: criteria.destinationId,
      hotelId: criteria.hotelId,
      regionId: criteria.regionId,
      checkInDate: criteria.checkInDate,
      checkOutDate: criteria.checkOutDate,
      roomsCount: criteria.roomsCount || 1,
      voucherCode: criteria.voucherCode ? criteria.voucherCode.trim().toUpperCase() : undefined,
    }),

  /**
   * Tìm kiếm các hạng phòng khả dụng của khách sạn kèm bảng giá Pricing Pipeline thời gian thực
   */
  searchRooms: (criteria: CustomerSearchCriteria) =>
    apiClient.post<AvailableRoomTypeApiResponse[]>(`${BASE_URL}/rooms`, {
      hotelId: criteria.hotelId,
      checkInDate: criteria.checkInDate,
      checkOutDate: criteria.checkOutDate,
      roomsCount: criteria.roomsCount || 1,
      voucherCode: criteria.voucherCode ? criteria.voucherCode.trim().toUpperCase() : undefined,
    }),

  /**
   * Tính toán lại giá phòng và phụ thu thời gian thực khi chọn số người lớn, trẻ em, giường phụ
   */
  calculateRoomQuote: (request: RoomPriceQuoteRequest) =>
    apiClient.post<RoomPriceQuoteResponse>(`${BASE_URL}/quote`, request),

  /**
   * Tính toán lại báo giá toàn bộ giỏ hàng đa hạng phòng thời gian thực (Multi-room Cart Quote)
   */
  calculateCartQuote: (request: CartPriceQuoteRequest) =>
    apiClient.post<CartPriceQuoteResponse>(`${BASE_URL}/cart-quote`, request),

  /**
   * Lấy danh sách điểm đến gợi ý (Khu vực / Khách sạn)
   */
  getDestinations: (keyword?: string) =>
    apiClient.get<DestinationSuggestionDto[]>(`${BASE_URL}/destinations`, {
      params: keyword ? { keyword } : undefined,
    }),
};

export default bookingSearchService;

