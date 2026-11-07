import type { BaseSearchDto } from '../../../core/api/types'

export const UTILITY_WRITE_ROLES = ['CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING']
export const UTILITY_READ_ROLES = [...UTILITY_WRITE_ROLES, 'REGION_MANAGER']

export type MeterType = 'ELECTRICITY' | 'WATER'

export interface MeterUpdate {
  meterCode: string
  locationLabel: string | null
}

export interface MeterCreate extends MeterUpdate {
  hotelId: number
  meterType: MeterType
}

export interface UtilityMeter extends Omit<MeterCreate, 'locationLabel'> {
  id: number
  locationLabel?: string | null
  isDeleted: boolean
}

export interface MeterSearch extends BaseSearchDto {
  hotelId: number
  meterType?: MeterType
  keyword?: string
}

export interface ReadingUpdate {
  readingValue: number
  isMeterReset: boolean
}

export interface ReadingCreate extends ReadingUpdate {
  meterId: number
  readingDate: string
}

export interface UtilityReading extends ReadingCreate {
  id: number
  meterCode?: string
  usage?: number | null
  recordedBy?: number
  updatedBy?: number
}

export interface ReadingSearch extends BaseSearchDto {
  hotelId: number
  meterId: number
  fromDate?: string
  toDate?: string
}
