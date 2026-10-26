import apiClient from '../../../core/api/client'
import type {
  PersonalizationConsentView,
  PersonalizationRecommendationResponse,
  UpdatePersonalizationConsentRequest,
} from '../types/personalization.types'

const CONSENT_PATH = '/api/v1/me/personalization-consent'
const RECOMMENDATIONS_PATH = '/api/v1/me/personalization/recommendations'

export const personalizationService = {
  async getConsent(hotelId: number, signal?: AbortSignal): Promise<PersonalizationConsentView> {
    const response = await apiClient.get<PersonalizationConsentView>(CONSENT_PATH, {
      params: { hotelId },
      signal,
    })
    return response.result
  },

  async updateConsent(
    request: UpdatePersonalizationConsentRequest,
    signal?: AbortSignal,
  ): Promise<PersonalizationConsentView> {
    const response = await apiClient.put<PersonalizationConsentView>(CONSENT_PATH, request, { signal })
    return response.result
  },

  async getRecommendations(
    hotelId: number,
    maxSuggestions = 3,
    signal?: AbortSignal,
  ): Promise<PersonalizationRecommendationResponse> {
    const response = await apiClient.get<PersonalizationRecommendationResponse>(RECOMMENDATIONS_PATH, {
      params: { hotelId, maxSuggestions },
      signal,
    })
    return response.result
  },
}
