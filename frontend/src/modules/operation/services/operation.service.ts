import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  MenuCreateRequest,
  MenuResponse,
  MenuSearchDto,
  MenuUpdateRequest,
  NightAuditRequest,
  NightAuditResponse,
  RoomHousekeepingStatusResponse,
  RoomStatusUpdateRequest,
  ServiceOrderCreateRequest,
  ServiceOrderResponse,
  ServiceOrderSearchDto,
  ServiceOrderStatus,
} from '../types/operation.types';

export const operationService = {
  // Menu APIs
  getMenus: async (searchDto: MenuSearchDto): Promise<PageResponse<MenuResponse>> => {
    const res = await apiClient.post<PageResponse<MenuResponse>>('/api/v1/menus/filter', searchDto);
    return res.result;
  },

  getMenuById: async (id: number): Promise<MenuResponse> => {
    const res = await apiClient.get<MenuResponse>(`/api/v1/menus/${id}`);
    return res.result;
  },

  createMenu: async (request: MenuCreateRequest): Promise<MenuResponse> => {
    const res = await apiClient.post<MenuResponse>('/api/v1/menus/create', request);
    return res.result;
  },

  updateMenu: async (id: number, request: MenuUpdateRequest): Promise<MenuResponse> => {
    const res = await apiClient.put<MenuResponse>(`/api/v1/menus/update/${id}`, request);
    return res.result;
  },

  deleteMenus: async (ids: number[]): Promise<void> => {
    await apiClient.delete<void>('/api/v1/menus/delete', { data: ids });
  },

  // Service Order APIs
  getServiceOrders: async (searchDto: ServiceOrderSearchDto): Promise<PageResponse<ServiceOrderResponse>> => {
    const res = await apiClient.post<PageResponse<ServiceOrderResponse>>('/api/v1/service-orders/filter', searchDto);
    return res.result;
  },

  getServiceOrderById: async (id: number): Promise<ServiceOrderResponse> => {
    const res = await apiClient.get<ServiceOrderResponse>(`/api/v1/service-orders/${id}`);
    return res.result;
  },

  createServiceOrder: async (request: ServiceOrderCreateRequest): Promise<ServiceOrderResponse> => {
    const res = await apiClient.post<ServiceOrderResponse>('/api/v1/service-orders/create', request);
    return res.result;
  },

  updateServiceOrderStatus: async (id: number, status: ServiceOrderStatus): Promise<ServiceOrderResponse> => {
    const res = await apiClient.put<ServiceOrderResponse>(`/api/v1/service-orders/${id}/status?status=${status}`);
    return res.result;
  },

  // Housekeeping APIs
  getHousekeepingRooms: async (hotelId: number, status?: string): Promise<RoomHousekeepingStatusResponse[]> => {
    const url = status
      ? `/api/v1/housekeeping/rooms?hotelId=${hotelId}&status=${encodeURIComponent(status)}`
      : `/api/v1/housekeeping/rooms?hotelId=${hotelId}`;
    const res = await apiClient.get<RoomHousekeepingStatusResponse[]>(url);
    return res.result;
  },

  updateRoomHousekeepingStatus: async (roomInstanceId: number, request: RoomStatusUpdateRequest): Promise<RoomHousekeepingStatusResponse> => {
    const res = await apiClient.put<RoomHousekeepingStatusResponse>(`/api/v1/housekeeping/rooms/${roomInstanceId}/status`, request);
    return res.result;
  },

  // Night Audit APIs
  executeNightAudit: async (request: NightAuditRequest): Promise<NightAuditResponse> => {
    const res = await apiClient.post<NightAuditResponse>('/api/v1/night-audit/execute', request);
    return res.result;
  },

  getAuditSummary: async (hotelId: number, auditDate: string): Promise<NightAuditResponse> => {
    const res = await apiClient.get<NightAuditResponse>(`/api/v1/night-audit/summary?hotelId=${hotelId}&auditDate=${auditDate}`);
    return res.result;
  },
};

export default operationService;
