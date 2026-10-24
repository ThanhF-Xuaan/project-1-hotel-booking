/**
 * Room Advisor Type Definitions
 * Conforming strictly to frozen T12 R1 contract v2.0.0-draft.1
 */

export interface AdvisorSearchCriteria {
  locationKeyword?: string;
  checkInDate?: string;
  checkOutDate?: string;
  adults?: number;
  children?: number;
  extraBeds?: number;
  roomsCount?: number;
  budget?: {
    amount: string;
    currency: string;
    basis: string;
    coverage: string;
    hard: boolean;
  };
  requiredFeatureCodes?: string[];
  preferredFeatureCodes?: string[];
}

export interface AdvisorFeature {
  code: string;
  name: string;
}

export interface AdvisorRoom {
  hotelId: number;
  hotelName: string;
  hotelRoomTypeId: number;
  roomTypeId: number;
  roomTypeCode: string;
  roomTypeName: string;
  maxAdults: number;
  maxChildren: number;
  maxTotalGuests: number;
  extraBedsAllowed: number;
  features?: AdvisorFeature[];
}

export interface AdvisorPriceQuote {
  hotelRoomTypeId: number;
  source: string;
  quoteId?: string;
  observedAt?: string;
  totalNights: number;
  currency: string;
  preTaxAmount?: string;
  vatPercent?: string;
  vatAmount?: string;
  finalTotalAmount: string;
  serviceFeeStatus: 'INCLUDED' | 'EXCLUDED' | 'UNKNOWN';
  serviceFeeAmount?: string;
  chargeCoverage: 'ALL_MANDATORY_CHARGES' | 'ROOM_RATE_ONLY' | 'TAX_EXCLUDED' | 'UNKNOWN';
  budgetCheck: 'PASS' | 'FAIL' | 'NOT_REQUESTED';
  displayLabel?: string;
}

export interface AdvisorMatchReason {
  code: string;
  text: string;
  evidenceFields?: string[];
}

export interface AdvisorAction {
  type: 'VIEW_ROOM_DETAILS';
  path: string;
}

export interface AdvisorStay {
  checkInDate: string;
  checkOutDate: string;
  roomsCount: number;
}

export interface AdvisorRecommendation {
  recommendationId: string;
  rank: number;
  room: AdvisorRoom;
  price: AdvisorPriceQuote;
  reasons: AdvisorMatchReason[];
  action: AdvisorAction;
  stay: AdvisorStay;
}

export interface AdvisorCoverage {
  status: 'COMPLETE' | 'PARTIAL' | 'NOT_RUN';
  catalogComplete: boolean;
  discoveredCount: number;
  evaluatedCount: number;
  failedCount: number;
  remainingCount: number;
  verifiedMatchCount: number;
  reasons?: string[];
}

export interface AdvisorResultDto {
  contractVersion: string;
  requestId: string;
  sessionId?: string;
  sessionVersion?: number;
  status: 'RESULTS' | 'NEED_INFO' | 'NO_MATCH' | 'OUT_OF_SCOPE';
  reply: string;
  criteria?: AdvisorSearchCriteria;
  missingFields?: string[];
  recommendations?: AdvisorRecommendation[];
  coverage?: AdvisorCoverage;
  warnings?: string[];
}

export interface AdvisorChatRequestDto {
  contractVersion: '2.0';
  message: string;
  sessionId?: string;
  sessionVersion?: number;
}

export interface AdvisorRefreshRequestDto {
  contractVersion: '2.0';
  sessionId: string;
  sessionVersion?: number;
}

export interface AdvisorClientError {
  code?: number;
  message: string;
  reason?: string;
  isRateLimited?: boolean;
  retryAfterSeconds?: number;
  isSessionExpired?: boolean;
  isSessionConflict?: boolean;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant' | 'system';
  text: string;
  timestamp: Date;
  result?: AdvisorResultDto;
  isError?: boolean;
  rateLimitCountdown?: number;
}
