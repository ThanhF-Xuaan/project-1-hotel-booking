/**
 * Room Advisor Type Definitions
 * Conforming strictly to frozen T12 R1 contract v2.0.0-draft.1 and additive v2.1 unified conversation
 */

export interface AdvisorSearchCriteria {
  locationKeyword?: string;
  checkInDate?: string;
  checkOutDate?: string;
  adults?: number;
  children?: number;
  infants?: number;
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
  status: 'RESULTS' | 'NEED_INFO' | 'NO_MATCH' | 'INCOMPLETE_SEARCH' | 'TEMP_UNAVAILABLE' | 'OUT_OF_SCOPE';
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
  expectedSessionVersion?: number;
}

export interface AdvisorRefreshRequestDto {
  contractVersion: '2.0';
  sessionId: string;
  expectedSessionVersion?: number;
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

export interface HotelChoice {
  hotelId: number;
  hotelName: string;
}

export interface KnowledgeCitation {
  sourceId: string;
  version: number;
  chunkId: string;
  category: string;
  hotelId?: number | null;
  effectiveFrom?: string | null;
  snippet: string;
  similarityScore: number;
}

export interface KnowledgeAnswer {
  state: 'ANSWERED' | 'SCOPE_REQUIRED' | 'RATE_LIMITED' | 'UNAVAILABLE' | 'NOT_ASKED';
  hotelId?: number;
  hotelName?: string;
  policyQuestion?: string;
  result?: {
    status: 'GROUNDED_ANSWER' | 'INSUFFICIENT_EVIDENCE' | 'SCOPE_REQUIRED' | 'CONFLICTING_SOURCES' | 'RAG_TEMP_UNAVAILABLE' | 'OUT_OF_SCOPE';
    answer?: string;
    citations?: KnowledgeCitation[];
    warnings?: string[];
    disclaimers?: string[];
  };
  retryAfterSeconds?: number;
  hotelChoices?: HotelChoice[];
}

export type ComparisonFocus =
  | 'FAMILY_KIDS'
  | 'BUDGET_SAVER'
  | 'SPACE_COMFORT'
  | 'WORK_TRIP'
  | 'GENERAL';

export type ComparisonStatus = 'READY' | 'INSUFFICIENT_CANDIDATES' | 'NEED_SEARCH_FIRST';

export interface RoomComparisonItem {
  recommendationId?: string;
  hotelRoomTypeId: number;
  hotelId: number;
  hotelName: string;
  roomTypeCode: string;
  roomTypeName: string;
  quoteId?: string;
  observedAt?: string;
  finalTotalAmount: string;
  perNightAverageAmount?: string;
  currency: string;
  totalNights: number;
  chargeCoverage?: AdvisorPriceQuote['chargeCoverage'];
  serviceFeeStatus?: AdvisorPriceQuote['serviceFeeStatus'];
  budgetCheck?: AdvisorPriceQuote['budgetCheck'];
  priceDeltaFromCheapest?: string;
  priceDiffPercentFromCheapest?: number;
  maxAdults: number;
  maxChildren: number;
  maxTotalGuests: number;
  extraBedsAllowed: number;
  staticAreaAndLayout?: string;
  uniqueFeatureNames: string[];
  bestForBullets: string[];
  action?: AdvisorAction;
}

export interface RoomComparisonMatrix {
  status: ComparisonStatus;
  focus: ComparisonFocus;
  items: RoomComparisonItem[];
  sharedFeatureNames: string[];
  maxSavingsAmount?: string;
  maxSavingsPercent?: number;
  tradeOffSummary: string;
  staticDescriptionCitations?: KnowledgeCitation[];
}

export interface UnifiedAdvisorRequest {
  message: string;
  sessionId?: string;
  expectedSessionVersion?: number;
  policyHotelId?: number;
  compareRoomTypeIds?: number[];
}

export interface UnifiedAdvisorResult {
  contractVersion: string;
  requestId: string;
  intent:
    | 'SEARCH_ONLY'
    | 'POLICY_ONLY'
    | 'SEARCH_AND_POLICY'
    | 'ROOM_COMPARISON'
    | 'SEARCH_AND_COMPARISON'
    | 'OUT_OF_SCOPE';
  sessionId?: string;
  sessionVersion?: number;
  advisor?: AdvisorResultDto;
  knowledge?: KnowledgeAnswer;
  comparison?: RoomComparisonMatrix;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant' | 'system';
  text: string;
  timestamp: Date;
  result?: AdvisorResultDto;
  knowledge?: KnowledgeAnswer;
  comparison?: RoomComparisonMatrix;
  intent?: UnifiedAdvisorResult['intent'];
  isError?: boolean;
  rateLimitCountdown?: number;
}
