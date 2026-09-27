import type { BaseSearchDto } from '../../../core/api/types'

export interface DailyPriceDto {
  date: string
  baseRate: number
  seasonalAdjustment: number
  holidayAdjustment: number
  adjustedRate: number
  discountAmount: number
  surchargeAmount: number
  netAmount: number
}

export interface PriceBreakdownDto {
  hotelRoomTypeId: number
  roomTypeName: string
  totalNights: number
  dailyPrices: DailyPriceDto[]
  totalBasePrice: number
  totalDiscountAmount: number
  totalSurchargeAmount: number
  preTaxAmount: number
  vatPercent: number
  vatAmount: number
  finalTotalAmount: number
}

export interface PriceCalculationRequest {
  hotelRoomTypeId: number
  checkInDate: string
  checkOutDate: string
  adults?: number
  children?: number
  extraBeds?: number
  campaignCode?: string
}

export interface PricingRuleDto {
  id: number
  hotelRoomTypeId: number
  roomTypeName: string
  holidayCalendarId?: number
  ruleTypeCode: string
  ruleTypeName: string
  adjustmentType: 'PERCENT' | 'FIXED'
  adjustmentValue: number
  startDate: string
  endDate: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface PricingRuleCreateRequest {
  hotelRoomTypeId: number
  holidayCalendarId?: number
  ruleTypeCode: string
  adjustmentType: string
  adjustmentValue: number
  startDate: string
  endDate: string
  status?: string
}

export interface PricingRuleUpdateRequest {
  holidayCalendarId?: number
  adjustmentType: string
  adjustmentValue: number
  startDate: string
  endDate: string
  status?: string
}

export interface PricingRuleSearchDto extends BaseSearchDto {
  hotelRoomTypeId?: number
  ruleTypeCode?: string
  activeOnDate?: string
  status?: string
}

export interface CampaignDto {
  id: number
  hotelId: number
  hotelName: string
  name: string
  description?: string
  startDate: string
  endDate: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface CampaignCreateRequest {
  hotelId: number
  name: string
  description?: string
  startDate: string
  endDate: string
  status?: string
}

export interface CampaignUpdateRequest {
  name: string
  description?: string
  startDate: string
  endDate: string
  status?: string
}

export interface CampaignSearchDto extends BaseSearchDto {
  hotelId?: number
  name?: string
  activeOnDate?: string
  status?: string
}
