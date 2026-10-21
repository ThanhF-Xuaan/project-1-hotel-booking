export type BookingType = 'FIT' | 'GIT' | 'CORPORATE' | 'ONLINE' | 'WALK_IN' | 'ALL';

export type CancellationFeeType = 'TIERED' | 'PERCENTAGE' | 'FIXED' | 'FULL_DEPOSIT';

export type NoShowPenaltyType = 'PERCENT_FIRST_NIGHT_RATE' | 'PERCENT_TOTAL_BOOKING' | 'FIXED_AMOUNT' | 'FULL_DEPOSIT';

export interface CancellationTierDto {
  minDays: number;
  maxDays?: number | null;
  penaltyPercent: number;
  description?: string;
}

export interface CancellationPolicyResponse {
  id: number;
  policyCode: string;
  name: string;
  bookingType: BookingType;
  description?: string;
  freeCancellationBeforeDays: number;
  cancellationFeeType: CancellationFeeType;
  defaultPenaltyPercent: number;
  tiers: CancellationTierDto[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CancellationPolicyCreateRequest {
  policyCode: string;
  name: string;
  bookingType: BookingType;
  description?: string;
  freeCancellationBeforeDays: number;
  cancellationFeeType: CancellationFeeType;
  defaultPenaltyPercent: number;
  tiers?: CancellationTierDto[];
  isActive?: boolean;
}

export interface CancellationPolicyUpdateRequest {
  name: string;
  bookingType: BookingType;
  description?: string;
  freeCancellationBeforeDays: number;
  cancellationFeeType: CancellationFeeType;
  defaultPenaltyPercent: number;
  tiers?: CancellationTierDto[];
  isActive: boolean;
}

export interface CancellationPolicySearchDto {
  keyword?: string;
  bookingType?: BookingType;
  cancellationFeeType?: CancellationFeeType;
  isActive?: boolean;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface NoShowPolicyResponse {
  id: number;
  policyCode: string;
  name: string;
  bookingType: BookingType;
  description?: string;
  releaseRoomTime: string; // HH:mm:ss
  penaltyType: NoShowPenaltyType;
  penaltyValue: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface NoShowPolicyCreateRequest {
  policyCode: string;
  name: string;
  bookingType: BookingType;
  description?: string;
  releaseRoomTime?: string;
  penaltyType: NoShowPenaltyType;
  penaltyValue: number;
  isActive?: boolean;
}

export interface NoShowPolicyUpdateRequest {
  name: string;
  bookingType: BookingType;
  description?: string;
  releaseRoomTime?: string;
  penaltyType: NoShowPenaltyType;
  penaltyValue: number;
  isActive: boolean;
}

export interface NoShowPolicySearchDto {
  keyword?: string;
  bookingType?: BookingType;
  penaltyType?: NoShowPenaltyType;
  isActive?: boolean;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface PropertyPolicyOverrideResponse {
  id: number;
  hotelId: number;
  hotelName: string;
  hotelRoomTypeId?: number | null;
  roomTypeName?: string | null;
  cancellationPolicyId?: number | null;
  cancellationPolicyCode?: string | null;
  cancellationPolicyName?: string | null;
  noShowPolicyId?: number | null;
  noShowPolicyCode?: string | null;
  noShowPolicyName?: string | null;
  freeCancellationBeforeDaysOverride?: number | null;
  noShowReleaseTimeOverride?: string | null;
  overrideReason: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyPolicyOverrideCreateRequest {
  hotelId: number;
  hotelRoomTypeId?: number | null;
  cancellationPolicyId?: number | null;
  noShowPolicyId?: number | null;
  freeCancellationBeforeDaysOverride?: number | null;
  noShowReleaseTimeOverride?: string | null;
  overrideReason: string;
  isActive?: boolean;
}

export interface PropertyPolicyOverrideUpdateRequest {
  cancellationPolicyId?: number | null;
  noShowPolicyId?: number | null;
  freeCancellationBeforeDaysOverride?: number | null;
  noShowReleaseTimeOverride?: string | null;
  overrideReason: string;
  isActive: boolean;
}

export interface PropertyPolicyOverrideSearchDto {
  hotelId?: number;
  hotelRoomTypeId?: number;
  isActive?: boolean;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}
