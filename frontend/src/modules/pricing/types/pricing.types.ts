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

// ==========================================
// Tax Category Types
// ==========================================
export interface TaxCategoryDto {
  id: number
  categoryCode: string
  categoryName: string
  description?: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string
  updatedAt?: string
}

export interface TaxCategoryCreateRequest {
  categoryCode: string
  categoryName: string
  description?: string
}

export interface TaxCategoryUpdateRequest {
  categoryName: string
  description?: string
  status: 'ACTIVE' | 'INACTIVE'
}

export interface TaxCategorySearchDto extends BaseSearchDto {
  keyword?: string
  status?: string
}

// ==========================================
// VAT Rule Types
// ==========================================
export interface VatRuleDto {
  id: number
  taxCategoryId: number
  taxCategoryCode?: string
  taxCategoryName?: string
  vatCode: string
  vatName: string
  vatPercent: number
  startDate: string
  endDate?: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string
}

export interface VatRuleCreateRequest {
  taxCategoryId: number
  vatCode: string
  vatName: string
  vatPercent: number
  startDate: string
  endDate?: string
}

export interface VatRuleUpdateRequest {
  vatName: string
  vatPercent: number
  startDate: string
  endDate?: string
  status?: string
}

export interface VatRuleSearchDto extends BaseSearchDto {
  taxCategoryId?: number
  keyword?: string
  activeOnDate?: string
  status?: string
}

// ==========================================
// Hotel Age Policy Types (Fallback Hierarchy)
// ==========================================
export interface HotelAgePolicyDto {
  id: number
  hotelId?: number
  hotelName?: string
  guestType: 'INFANT' | 'CHILD' | 'ADULT'
  minAge: number
  maxAge: number
  status: string
  isChainDefault: boolean
  createdAt?: string
}

export interface HotelAgePolicyCreateRequest {
  hotelId?: number
  guestType: 'INFANT' | 'CHILD' | 'ADULT'
  minAge: number
  maxAge: number
}

export interface HotelAgePolicyUpdateRequest {
  minAge: number
  maxAge: number
  status?: string
}

export interface HotelAgePolicySearchDto extends BaseSearchDto {
  hotelId?: number
  guestType?: string
  status?: string
  chainOnly?: boolean
}

// ==========================================
// Holiday Calendar Types
// ==========================================
export interface HolidayCalendarDto {
  id: number
  name: string
  date: string
  description?: string
  status: string
  createdAt?: string
}

export interface HolidayCalendarCreateRequest {
  name: string
  date: string
  description?: string
}

export interface HolidayCalendarUpdateRequest {
  name: string
  date: string
  description?: string
  status?: string
}

export interface HolidayCalendarSearchDto extends BaseSearchDto {
  keyword?: string
  year?: number
  month?: number
  status?: string
}

// ==========================================
// Pricing Rule Type Types
// ==========================================
export interface PricingRuleTypeDto {
  code: string
  displayName: string
  description?: string
  priority: number
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string
  updatedAt?: string
}

export interface PricingRuleTypeCreateRequest {
  code: string
  displayName: string
  description?: string
  priority: number
  status: 'ACTIVE' | 'INACTIVE'
}

export interface PricingRuleTypeUpdateRequest {
  displayName: string
  description?: string
  priority: number
  status: 'ACTIVE' | 'INACTIVE'
}

// ==========================================
// Discount Rule Type Types
// ==========================================
export interface DiscountRuleTypeDto {
  code: string
  displayName: string
  description?: string
  priority: number
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string
  updatedAt?: string
}

export interface DiscountRuleTypeCreateRequest {
  code: string
  displayName: string
  description?: string
  priority: number
  status: 'ACTIVE' | 'INACTIVE'
}

export interface DiscountRuleTypeUpdateRequest {
  displayName: string
  description?: string
  priority: number
  status: 'ACTIVE' | 'INACTIVE'
}

// ==========================================
// Discount Rule Types
// ==========================================
export interface DiscountTierItem {
  minNights: number
  maxNights?: number | null
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
}

export interface DiscountConditions {
  minNights?: number
  maxNights?: number
  minAdvanceDays?: number
  maxAdvanceDays?: number
  tiers?: DiscountTierItem[]
}

export interface DiscountRuleDto {
  id: number
  hotelRoomTypeId: number
  roomTypeName?: string
  hotelId?: number
  hotelName?: string
  ruleTypeCode: string
  ruleTypeName?: string
  campaignId?: number
  campaignName?: string
  conditions?: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  startDate: string
  endDate: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string
  updatedAt?: string
}

export interface DiscountRuleCreateRequest {
  hotelRoomTypeId: number
  ruleTypeCode: string
  campaignId?: number
  conditions?: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  startDate: string
  endDate: string
  status: 'ACTIVE' | 'INACTIVE'
}

export interface DiscountRuleUpdateRequest {
  campaignId?: number
  conditions?: string
  discountType: 'PERCENT' | 'FIXED'
  discountValue: number
  startDate: string
  endDate: string
  status: 'ACTIVE' | 'INACTIVE'
}

export interface DiscountRuleSearchDto extends BaseSearchDto {
  hotelId?: number
  hotelRoomTypeId?: number
  campaignId?: number
  ruleTypeCode?: string
  activeOnDate?: string
  status?: string
}

