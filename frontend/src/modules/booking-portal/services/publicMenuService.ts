import { apiClient } from '../../../core/api/client';
import type { AddonServiceItem } from '../types/portal.types';

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

const BASE_URL = '/api/v1/public/menus';

export const publicMenuService = {
  /**
   * Lấy danh sách dịch vụ bổ sung (SERVICE only) của một khách sạn
   * Tự động ưu tiên dịch vụ cơ sở đã override, nếu chưa override lấy chuẩn toàn chuỗi
   */
  getAddonServicesByHotel: (hotelId: number, page: number = 0, size: number = 50) =>
    apiClient.get<PageResponse<AddonServiceItem>>(`${BASE_URL}/hotel/${hotelId}/services`, {
      params: { page, size },
    }),

  /**
   * Xem chi tiết món ăn / dịch vụ theo ID
   */
  getMenuById: (id: number) =>
    apiClient.get<AddonServiceItem>(`${BASE_URL}/${id}`),
};

export default publicMenuService;
