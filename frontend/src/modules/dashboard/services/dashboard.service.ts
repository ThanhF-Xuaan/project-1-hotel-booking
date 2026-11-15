import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type { HotelDto } from '../../organization/types/organization.types';
import type {
  DashboardKpiResponse,
  OccupancyTrendDto,
  RevenueTrendDto,
} from '../types/dashboard.types';

export const dashboardService = {
  getHotels: async (signal: AbortSignal): Promise<Pick<HotelDto, 'id' | 'name'>[]> => {
    const hotels: Pick<HotelDto, 'id' | 'name'>[] = [];
    for (let page = 0; ; page += 1) {
      const { result } = await apiClient.post<PageResponse<HotelDto>>('/api/v1/hotels/filter',
        { page, pageSize: 50, sortBy: 'id', sortDirection: 'ASC' }, { signal });
      if (!result || !Array.isArray(result.content)) throw new Error('Invalid hotel response');
      hotels.push(...result.content.map(({ id, name }) => ({ id, name })));
      if (result.last || result.empty) return hotels;
    }
  },
  getExecutiveKpis: async (hotelId: number, targetDate?: string): Promise<DashboardKpiResponse> => {
    const url = targetDate
      ? `/api/v1/dashboard/kpis?hotelId=${hotelId}&targetDate=${targetDate}`
      : `/api/v1/dashboard/kpis?hotelId=${hotelId}`;
    const res = await apiClient.get<DashboardKpiResponse>(url);
    if (res.code !== 200 || !res.result || res.result.hotelId !== hotelId) {
      throw new Error('Invalid dashboard KPI response');
    }
    return res.result;
  },

  getRevenueTrends: async (
    hotelId: number,
    startDate?: string,
    endDate?: string
  ): Promise<RevenueTrendDto[]> => {
    let url = `/api/v1/dashboard/revenue-trends?hotelId=${hotelId}`;
    if (startDate) url += `&startDate=${startDate}`;
    if (endDate) url += `&endDate=${endDate}`;
    const res = await apiClient.get<RevenueTrendDto[]>(url);
    if (res.code !== 200 || !Array.isArray(res.result)) throw new Error('Invalid revenue response');
    return res.result;
  },

  getOccupancyTrends: async (
    hotelId: number,
    startDate?: string,
    endDate?: string
  ): Promise<OccupancyTrendDto[]> => {
    let url = `/api/v1/dashboard/occupancy-trends?hotelId=${hotelId}`;
    if (startDate) url += `&startDate=${startDate}`;
    if (endDate) url += `&endDate=${endDate}`;
    const res = await apiClient.get<OccupancyTrendDto[]>(url);
    if (res.code !== 200 || !Array.isArray(res.result)) throw new Error('Invalid occupancy response');
    return res.result;
  },
};

export default dashboardService;
