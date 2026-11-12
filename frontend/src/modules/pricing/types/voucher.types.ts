export interface VoucherDto {
  id: number
  hotelId?: number | null
  hotelName?: string | null
  voucherCode: string
  title: string
  description?: string | null
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  applyScope: 'ROOM_ONLY' | 'TOTAL_BOOKING' | 'SERVICE_ONLY'
  minOrderAmount?: number | null
  maxDiscountAmount?: number | null
  totalQuantity: number
  usedQuantity: number
  remainingQuantity: number
  maxUsagePerUser: number
  validFrom: string
  validTo: string
  status: 'ACTIVE' | 'INACTIVE' | 'EXPIRED'
  createdAt?: string
  updatedAt?: string
}

export interface VoucherCreateRequest {
  hotelId?: number | null
  voucherCode: string
  title: string
  description?: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  applyScope: 'ROOM_ONLY' | 'TOTAL_BOOKING' | 'SERVICE_ONLY'
  minOrderAmount?: number
  maxDiscountAmount?: number | null
  totalQuantity: number
  maxUsagePerUser: number
  validFrom: string
  validTo: string
  status?: string
}

export interface VoucherUpdateRequest {
  hotelId?: number | null
  title: string
  description?: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  applyScope: 'ROOM_ONLY' | 'TOTAL_BOOKING' | 'SERVICE_ONLY'
  minOrderAmount?: number
  maxDiscountAmount?: number | null
  totalQuantity: number
  maxUsagePerUser: number
  validFrom: string
  validTo: string
  status: string
}

export interface VoucherSearchDto {
  page?: number
  pageSize?: number
  keyword?: string
  hotelId?: number | null
  discountType?: string
  applyScope?: string
  status?: string
  activeAt?: string
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface VoucherValidateRequest {
  voucherCode: string
  hotelId?: number | null
  orderAmount: number
  applyScope?: string
  bookingGuestId?: number | null
}

export interface VoucherValidationResponse {
  valid: boolean
  message: string
  voucherCode?: string
  title?: string
  description?: string
  discountType?: 'PERCENT' | 'FIXED'
  discountValue?: number
  discountAmount?: number
  finalAmount?: number
  applyScope?: string
}

