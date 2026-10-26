export type PersonalizationConsentStatus = 'GRANTED' | 'NOT_GRANTED' | 'RECONSENT_REQUIRED'

export interface PersonalizationConsentView {
  hotelId: number
  status: PersonalizationConsentStatus
  granted: boolean
  revision: number
  bindingVersion: number
  purpose: 'ROOM_TYPE_RECOMMENDATION'
  currentPolicyVersion: string
  acceptedPolicyVersion: string | null
  updatedAt: string | null
  historyLookbackMonths: number
  noticeText: string
  privacyPolicyUrl: string
  dataMode: 'LOCAL_SYNTHETIC'
}

export interface UpdatePersonalizationConsentRequest {
  hotelId: number
  granted: boolean
  expectedRevision: number
  expectedBindingVersion: number
  policyVersion?: string
}

export type PersonalizationRecommendationStatus =
  | 'CONSENT_REQUIRED'
  | 'COLD_START_FALLBACK'
  | 'PERSONALIZED_READY'

export interface PersonalizationRoomTypeSuggestion {
  hotelRoomTypeId: number
  roomTypeCode: string
  roomTypeName: string
  affinityScore: number
  reasonCode: 'HISTORICAL_BOOKING_AFFINITY'
}

export interface PersonalizationRecommendationResponse {
  status: PersonalizationRecommendationStatus
  hotelId: number
  dataMode: 'LOCAL_SYNTHETIC'
  roomTypeSuggestions: PersonalizationRoomTypeSuggestion[]
  nonBindingSuggestion: true
  pricingOrPromotionMutated: false
  reasonCode: string | null
}

export interface PersonalizationErrorDetail {
  reasonCode?: string
  retryable?: boolean
}
