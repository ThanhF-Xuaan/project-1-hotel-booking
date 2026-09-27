import { apiClient } from '../../../core/api/client';
import type {
  DashboardKpiResponse,
  OccupancyTrendDto,
  RevenueTrendDto,
} from '../types/dashboard.types';

export const dashboardService = {
  getExecutiveKpis: async (hotelId: number, targetDate?: string): Promise<DashboardKpiResponse> => {
    const url = targetDate
      ? `/api/v1/dashboard/kpis?hotelId=${hotelId}&targetDate=${targetDate}`
      : `/api/v1/dashboard/kpis?hotelId=${hotelId}`;
    const res = await apiClient.get<DashboardKpiResponse>(url);
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
    return res.result;
  },
};

export default dashboardService;
