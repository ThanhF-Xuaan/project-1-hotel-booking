import type { BaseSearchDto } from '../../../core/api/types'

export interface RoomTypeDto {
  id: number
  code: string
  name: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface RoomTypeCreateRequest {
  code: string
  name: string
  status?: string
}

export interface RoomTypeUpdateRequest {
  name: string
  status?: string
}

export interface RoomTypeSearchDto extends BaseSearchDto {
  keyword?: string
  status?: string
}

export interface HotelRoomTypeDto {
  id: number
  hotelId: number
  hotelName: string
  roomTypeId: number
  roomTypeCode: string
  roomTypeName: string
  taxCategoryId: number
  standardAdults: number
  standardChildren: number
  maxAdults: number
  maxChildren: number
  maxInfants: number
  maxTotalGuests: number
  maxBeds: number
  extraBeds: number
  basePrice: number
  totalQuantity: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface HotelRoomTypeCreateRequest {
  hotelId: number
  roomTypeId: number
  taxCategoryId: number
  standardAdults?: number
  standardChildren?: number
  maxAdults?: number
  maxChildren?: number
  maxInfants?: number
  maxTotalGuests?: number
  maxBeds?: number
  extraBeds?: number
  basePrice: number
  totalQuantity: number
  status?: string
  featureIds?: number[]
}

export interface HotelRoomTypeUpdateRequest {
  taxCategoryId?: number
  standardAdults?: number
  standardChildren?: number
  maxAdults?: number
  maxChildren?: number
  maxInfants?: number
  maxTotalGuests?: number
  maxBeds?: number
  extraBeds?: number
  basePrice?: number
  totalQuantity?: number
  status?: string
  featureIds?: number[]
}

export interface HotelRoomTypeSearchDto extends BaseSearchDto {
  hotelId?: number
  roomTypeId?: number
  adults?: number
  children?: number
  minPrice?: number
  maxPrice?: number
  status?: string
}

export interface RoomInstanceDto {
  id: number
  hotelId: number
  hotelName: string
  hotelRoomTypeId: number
  roomTypeCode: string
  roomTypeName: string
  roomNumber: string
  currentStatus: 'READY' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE'
  createdAt: string
  updatedAt: string
}

export interface RoomInstanceCreateRequest {
  hotelId: number
  hotelRoomTypeId: number
  roomNumber: string
  currentStatus?: string
}

export interface RoomInstanceUpdateRequest {
  hotelRoomTypeId: number
  roomNumber: string
  currentStatus: string
}

export interface RoomInstanceSearchDto extends BaseSearchDto {
  hotelId?: number
  hotelRoomTypeId?: number
  roomNumber?: string
  currentStatus?: string
}

export interface RoomAvailabilityDto {
  id: number
  hotelRoomTypeId: number
  date: string
  totalRooms: number
  bookedRooms: number
  lockedRooms: number
  oooRooms: number
  availableCount: number
  lockedUntil?: string
  version: number
}
