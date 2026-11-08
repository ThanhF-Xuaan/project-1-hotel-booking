export const LODGING_ROLES = ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST']

export const LODGING_STATUS_LABELS = {
  PENDING: 'Chờ xuất',
  ERROR: 'Cần bổ sung',
  FAILED: 'Cần bổ sung',
  PROCESSING: 'Đang xử lý',
  EXPORTED: 'Đã xuất file',
  CANCELLED: 'Đã hủy',
}

export type LodgingStatus = keyof typeof LODGING_STATUS_LABELS
export type LodgingCategory = 'VIETNAMESE' | 'FOREIGNER'

export interface LodgingQueueItem {
  id: number
  stayGuestId: number
  stayGuest?: {
    bookingId?: number
    bookingRoomId?: number
    fullName?: string
    roomNumber?: string
    checkInTime?: string
  }
  hotelId: number
  status: LodgingStatus
  createdAt?: string
  errorMessage?: string
  batchReference?: string
  exportedAt?: string
}

export interface LodgingFilter {
  hotelId: number
  page: number
  pageSize: number
  status?: LodgingStatus
  fromDate?: string
  toDate?: string
}

export interface LodgingExportRequest {
  hotelId: number
  date: string
  cutOffHour: number
  category: LodgingCategory
}
