import type { BaseSearchDto } from '../../../core/api/types'

export interface RegionDto {
  id: number
  code: string
  name: string
  description?: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface RegionCreateRequest {
  code: string
  name: string
  description?: string
  status?: string
}

export interface RegionUpdateRequest {
  name: string
  description?: string
  status?: string
}

export interface RegionSearchDto extends BaseSearchDto {
  keyword?: string
  status?: string
}

export interface HotelImageDto {
  id: number
  url: string
  originalFilename: string
  sizeBytes: number
  isPrimary: boolean
  createdAt?: string
}

export interface HotelDto {
  id: number
  regionId: number
  regionName: string
  regionCode: string
  name: string
  address: string
  phone?: string
  checkInTime: string
  checkOutTime: string
  serviceFeePercent: number
  status: string
  coverImageUrl?: string
  images?: HotelImageDto[]
  createdAt: string
  updatedAt: string
}

export interface HotelCreateRequest {
  regionId: number
  name: string
  address: string
  phone?: string
  checkInTime?: string
  checkOutTime?: string
  serviceFeePercent?: number
  status?: string
}

export interface HotelUpdateRequest {
  regionId: number
  name: string
  address: string
  phone?: string
  checkInTime?: string
  checkOutTime?: string
  serviceFeePercent?: number
  status?: string
}

export interface HotelSearchDto extends BaseSearchDto {
  regionId?: number
  keyword?: string
  status?: string
}
