import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import { getAccessToken } from '../../../core/auth/auth'
import { parseJwt } from '../../auth/utils/token.utils'
import type { StaffDto } from '../../identity/types/staff.types'
import type { HotelDto } from '../../organization/types/organization.types'
import type {
  MeterCreate, MeterSearch, MeterUpdate, ReadingCreate, ReadingSearch, ReadingUpdate,
  UtilityMeter, UtilityReading,
} from '../types/utility.types'

const METERS = '/api/v1/utility-meters'
const READINGS = '/api/v1/utility-readings'

export const utilityService = {
  filterMeters: (data: MeterSearch, signal: AbortSignal) =>
    apiClient.post<PageResponse<UtilityMeter>>(METERS + '/filter', data, { signal }).then(res => res.result),
  getMeter: (id: number, signal: AbortSignal) =>
    apiClient.get<UtilityMeter>(METERS + '/' + id, { signal }).then(res => res.result),
  createMeter: (data: MeterCreate) => apiClient.post<UtilityMeter>(METERS, data),
  updateMeter: (id: number, data: MeterUpdate) => apiClient.put<UtilityMeter>(METERS + '/' + id, data),
  deleteMeter: (id: number) => apiClient.delete<void>(METERS + '/delete', { data: [id] }),

  filterReadings: (data: ReadingSearch, signal: AbortSignal) =>
    apiClient.post<PageResponse<UtilityReading>>(READINGS + '/filter', data, { signal }).then(res => res.result),
  getReading: (id: number, signal: AbortSignal) =>
    apiClient.get<UtilityReading>(READINGS + '/' + id, { signal }).then(res => res.result),
  createReading: (data: ReadingCreate) => apiClient.post<UtilityReading>(READINGS, data),
  updateReading: (id: number, data: ReadingUpdate) => apiClient.put<UtilityReading>(READINGS + '/' + id, data),
  deleteReading: (id: number) => apiClient.delete<void>(READINGS + '/delete', { data: [id] }),

  async currentScope(expected: 'PROPERTY' | 'REGION', signal: AbortSignal) {
    const token = getAccessToken()
    const subject = token ? parseJwt(token)?.sub : undefined
    if (!subject || !/^[\da-f]{8}(?:-[\da-f]{4}){3}-[\da-f]{12}$/i.test(subject)) return null
    const { result: staff } = await apiClient.get<StaffDto>('/api/v1/staffs/keycloak/' + subject, { signal })
    const id = staff?.scopeEntityId
    if (staff?.scopeType !== expected || typeof id !== 'number' || !Number.isInteger(id) || id < 1 || id > 32767) {
      return null
    }
    return id
  },

  async hotels(regionId: number | undefined, signal: AbortSignal) {
    const hotels: Pick<HotelDto, 'id' | 'name'>[] = []
    for (let page = 0; ; page += 1) {
      const { result } = await apiClient.post<PageResponse<HotelDto>>('/api/v1/hotels/filter', {
        regionId, page, pageSize: 50, sortBy: 'id', sortDirection: 'ASC',
      }, { signal })
      hotels.push(...result.content.map(({ id, name }) => ({ id, name })))
      if (result.last) return hotels
    }
  },
}
