import axios from 'axios'
import { apiClient } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import { getAccessToken } from '../../../core/auth/auth'
import { parseJwt } from '../../auth/utils/token.utils'
import type { StaffDto } from '../../identity/types/staff.types'
import type { HotelDto } from '../../organization/types/organization.types'
import type { LodgingExportRequest, LodgingFilter, LodgingQueueItem } from '../types/lodging.types'

type HotelOption = Pick<HotelDto, 'id' | 'name'>

const queue = '/api/v1/lodging-queues'
const hotels = '/api/v1/hotels'
const xlsxMime = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

export async function loadLodgingHotels(isChainAdmin: boolean, signal: AbortSignal): Promise<HotelOption[]> {
  if (!isChainAdmin) {
    const token = getAccessToken()
    const subject = token ? parseJwt(token)?.sub : undefined
    if (!subject || !/^[\da-f]{8}(?:-[\da-f]{4}){3}-[\da-f]{12}$/i.test(subject)) return []
    const { result: staff } = await apiClient.get<StaffDto>('/api/v1/staffs/keycloak/' + subject, { signal })
    const hotelId = staff?.scopeEntityId
    if (staff?.scopeType !== 'PROPERTY' || typeof hotelId !== 'number' ||
        !Number.isInteger(hotelId) || hotelId < 1 || hotelId > 32767) return []
    const { result: hotel } = await apiClient.get<HotelDto>(hotels + '/' + hotelId, { signal })
    return hotel?.id === hotelId ? [{ id: hotel.id, name: hotel.name }] : []
  }

  const options: HotelOption[] = []
  for (let page = 0; ; page += 1) {
    const { result } = await apiClient.post<PageResponse<HotelDto>>(hotels + '/filter',
      { page, pageSize: 50, sortBy: 'id', sortDirection: 'ASC' }, { signal })
    options.push(...result.content.map(({ id, name }) => ({ id, name })))
    if (result.last || result.empty) return options
  }
}

export const lodgingService = {
  filter: (body: LodgingFilter, signal: AbortSignal) =>
    apiClient.post<PageResponse<LodgingQueueItem>>(queue + '/filter', body, { signal }).then(r => r.result),

  retry: (id: number) => apiClient.post<LodgingQueueItem>(queue + '/retry/' + id).then(r => r.result),

  async exportWorkbook(body: LodgingExportRequest) {
    const file = await apiClient.postBlob('/api/v1/lodging-export/download', body)
    if (!file.size || file.type.split(';')[0] !== xlsxMime) {
      throw new Error('Máy chủ không trả về file Excel hợp lệ.')
    }
    return file
  },
}

// postBlob wraps JSON error responses and preserves the original Axios error as the cause.
export function lodgingExportCause(error: unknown): unknown {
  return error instanceof Error && error.cause ? error.cause : error
}

export async function isEmptyLodgingExport(error: unknown): Promise<boolean> {
  if (!axios.isAxiosError(error) || error.response?.status !== 400) return false
  const data: unknown = error.response.data
  try {
    const body = data instanceof Blob ? JSON.parse(await data.text()) : data
    return body?.code === 9072
  } catch {
    return false
  }
}
