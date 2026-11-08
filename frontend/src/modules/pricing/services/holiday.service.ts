import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import type {
  HolidayCalendarDto,
  HolidayCalendarCreateRequest,
  HolidayCalendarUpdateRequest,
  HolidayCalendarSearchDto,
} from '../types/pricing.types'

const BASE_URL = '/api/v1/pricing/holiday-calendars'

export const holidayService = {
  filter: (params: HolidayCalendarSearchDto) =>
    apiClient.post<PageResponse<HolidayCalendarDto>>(`${BASE_URL}/filter`, params),

  getById: (id: number) => apiClient.get<HolidayCalendarDto>(`${BASE_URL}/${id}`),

  create: (data: HolidayCalendarCreateRequest) =>
    apiClient.post<HolidayCalendarDto>(BASE_URL, data),

  update: (id: number, data: HolidayCalendarUpdateRequest) =>
    apiClient.put<HolidayCalendarDto>(`${BASE_URL}/${id}`, data),

  deleteBatch: (ids: number[]) =>
    apiClient.delete<void>(`${BASE_URL}/delete`, { data: ids }),
}

export default holidayService
