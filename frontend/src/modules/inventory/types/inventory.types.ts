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

// ==================== ROOM BED ====================
export interface RoomBedDto {
  id: number
  name: string
  size?: string
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface RoomBedCreateRequest {
  name: string
  size?: string
  status?: string
}

export interface RoomBedUpdateRequest {
  name: string
  size?: string
  status?: string
}

export interface RoomBedSearchDto extends BaseSearchDto {
  keyword?: string
  status?: string
}

// ==================== ROOM FEATURE ====================
export interface RoomFeatureDto {
  id: number
  code: string
  name: string
  category?: string
  status: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface RoomFeatureCreateRequest {
  code: string
  name: string
  category?: string
  status?: string
}

export interface RoomFeatureUpdateRequest {
  name: string
  category?: string
  status?: string
}

export interface RoomFeatureSearchDto extends BaseSearchDto {
  keyword?: string
  category?: string
  status?: string
}

// ==================== HOTEL ROOM TYPE BED ====================
export interface HotelRoomTypeBedDto {
  roomBedId: number
  roomBedName: string
  roomBedSize?: string
  baseQuantity: number
}

export interface HotelRoomTypeBedItemRequest {
  roomBedId: number
  baseQuantity: number
}

// ==================== HOTEL ROOM TYPE ====================
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
  coverImageUrl?: string
  beds?: HotelRoomTypeBedDto[]
  features?: RoomFeatureDto[]
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
  beds?: HotelRoomTypeBedItemRequest[]
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
  beds?: HotelRoomTypeBedItemRequest[]
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

// ==================== ROOM INSTANCE ====================
export interface RoomInstanceDto {
  id: number
  hotelId: number
  hotelName: string
  hotelRoomTypeId: number
  roomTypeCode: string
  roomTypeName: string
  roomNumber: string
  currentStatus: 'READY' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE'
  maxAdults?: number
  maxChildren?: number
  maxTotalGuests?: number
  maxBeds?: number
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
