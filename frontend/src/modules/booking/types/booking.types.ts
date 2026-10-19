export type BookingType = 'FIT' | 'GIT' | 'CORPORATE';

export type BookingStatus = 'CONFIRMED' | 'CANCELLED' | 'NO_SHOW';

export type BookingRoomStatus = 'EXPECTED' | 'CHECKED_IN' | 'CHECKED_OUT' | 'NO_SHOW' | 'CANCELLED';

export type BookingChargeType = 'EARLY_CHECKIN' | 'LATE_CHECKOUT' | 'PENALTY' | 'OTHER';

export type BookingGuestType = 'ADULT' | 'CHILD' | 'INFANT';

export interface BookingGuestItemRequest {
  firstName: string;
  lastName: string;
  fullName?: string;
  birthDate?: string;
  guestType?: BookingGuestType;
  identityType?: string;
  identityNumber?: string;
}

export interface BookingRoomItemRequest {
  hotelRoomTypeId: number;
  checkInDate: string;
  checkOutDate: string;
  quantity?: number;
  adultCount?: number;
  childCount?: number;
  infantCount?: number;
  roomInstanceId?: number;
  guests?: BookingGuestItemRequest[];
}

export interface BookingCreateRequest {
  hotelId: number;
  guestId: number;
  companyId?: number;
  bookingType?: BookingType;
  serviceFeeRate?: number;
  rooms: BookingRoomItemRequest[];
}

export interface BookingSearchDto {
  hotelId?: number;
  guestId?: number;
  bookingNumber?: string;
  bookingType?: BookingType;
  status?: BookingStatus;
  checkInDate?: string;
  checkOutDate?: string;
  isAssigned?: boolean;
  page?: number;
  size?: number;
}

export interface BookingStatusUpdateRequest {
  status: BookingStatus;
}

export interface RoomAssignmentRequest {
  roomInstanceId: number;
}

export interface BookingChargeCreateRequest {
  chargeType: BookingChargeType;
  itemName: string;
  description?: string;
  quantity: number;
  unitPrice: number;
  serviceFeeRate?: number;
  vatRate?: number;
}

export interface BookingGuestResponse {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  birthDate?: string;
  guestType: BookingGuestType;
  identityType?: string;
  identityNumber?: string;
}

export interface BookingDailyRateResponse {
  id: number;
  stayDate: string;
  basePrice: number;
  discountAmount: number;
  surchargeAmount: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  taxCategoryId?: number;
  taxCategoryName?: string;
  vatPercent: number;
  vatAmount: number;
  netPrice: number;
  status: string;
}

export interface BookingChargeResponse {
  id: number;
  bookingRoomId: number;
  chargeType: BookingChargeType;
  itemName: string;
  description?: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  vatRate: number;
  vatAmount: number;
  totalAmount: number;
  issuedAt: string;
}

export interface BookingRoomResponse {
  id: number;
  bookingDetailId: number;
  roomInstanceId?: number;
  roomNumber?: string;
  adultCount: number;
  childCount: number;
  infantCount: number;
  guestCount: number;
  status: BookingRoomStatus;
  actualCheckInAt?: string;
  actualCheckOutAt?: string;
  assignedAt?: string;
  bookingGuests: BookingGuestResponse[];
  dailyRates: BookingDailyRateResponse[];
  charges: BookingChargeResponse[];
}

export interface BookingDetailResponse {
  id: number;
  bookingId: number;
  hotelRoomTypeId: number;
  roomTypeName: string;
  quantity: number;
  checkInDate: string;
  checkOutDate: string;
  bookingRooms: BookingRoomResponse[];
}

export interface BookingResponse {
  id: number;
  hotelId: number;
  hotelName: string;
  guestId: number;
  guestName: string;
  guestPhone: string;
  companyId?: number;
  companyName?: string;
  bookingType: BookingType;
  bookingNumber: string;
  subtotalAmount: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  totalVatAmount: number;
  totalAmount: number;
  status: BookingStatus;
  issuedAt?: string;
  createdAt: string;
  updatedAt: string;
  bookingDetails: BookingDetailResponse[];
}

export interface SurchargePreviewRequest {
  chargeType: BookingChargeType;
  actualTime?: string;
}

export interface SurchargeProposalResponse {
  applicable: boolean;
  chargeType: BookingChargeType;
  standardTime: string;
  actualTime: string;
  timeDifferenceMinutes: number;
  policyRule: string;
  baseRoomRate: number;
  suggestedPercentage: number;
  suggestedAmount: number;
  suggestedItemName: string;
  defaultServiceFeeRate?: number;
  defaultVatRate?: number;
}

export interface SettlementLineItemDto {
  lineType: 'ROOM_RATE' | 'SURCHARGE' | 'PENALTY' | 'SERVICE' | 'PRODUCT';
  referenceId?: number;
  description: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  serviceFeeRate?: number;
  serviceFeeAmount?: number;
  vatRate?: number;
  vatAmount?: number;
  totalAmount: number;
}

export interface CheckoutSettlementResponse {
  bookingId: number;
  bookingRoomId: number;
  bookingNumber: string;
  roomNumber: string;
  roomTypeName: string;
  guestName: string;
  guestPhone: string;
  totalRoomCharges: number;
  totalSurcharges: number;
  noShowPenaltyAmount?: number;
  totalServiceCharges: number;
  grandTotal: number;
  totalPaid: number;
  balanceDue: number;
  netOverpaymentAmount?: number;
  canCheckOut: boolean;
  lineItems: SettlementLineItemDto[];
  paymentHistory: any[];
}

export interface ItemizedSurchargeDto {
  extraAdultsCount: number;
  extraAdultsSurchargeTotal: number;
  extraChildrenCount: number;
  extraChildrenSurchargeTotal: number;
  infantsCount: number;
  infantsSurchargeTotal: number;
  extraBedsCount: number;
  extraBedsSurchargeTotal: number;
  totalSurchargeAmount: number;
  surchargeNotes?: string[];
}

export interface WalkInQuoteRoomItemRequest {
  hotelRoomTypeId: number;
  adultCount?: number;
  childCount?: number;
  infantCount?: number;
  extraBeds?: number;
}

export interface WalkInQuoteRequest {
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  actualCheckInTime?: string;
  rooms: WalkInQuoteRoomItemRequest[];
}

export interface WalkInQuoteRoomItemResponse {
  roomIndex: number;
  hotelRoomTypeId: number;
  roomTypeName: string;
  adultCount: number;
  childCount: number;
  infantCount: number;
  extraBeds: number;
  dailyRates?: any[];
  netPrice: number;
  vatAmount: number;
  serviceFeeAmount: number;
  totalPrice: number;
  capacitySurcharge?: ItemizedSurchargeDto;
  earlyCheckinProposal?: SurchargeProposalResponse;
}

export interface WalkInQuoteResponse {
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  rooms: WalkInQuoteRoomItemResponse[];
  subtotalAmount: number;
  totalServiceFeeAmount: number;
  totalVatAmount: number;
  totalSurchargesAmount: number;
  totalAmount: number;
}

export interface NewCompanyRequest {
  name: string;
  taxCode?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
}

export interface NewGuestRequest {
  fullName: string;
  phone: string;
  email?: string;
  identityType?: string;
  identityNumber?: string;
  birthDate?: string;
  nationality?: string;
}

export interface WalkInStayGuestRequest {
  fullName: string;
  birthDate?: string;
  guestType?: BookingGuestType;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  identityType?: string;
  identityNumber?: string;
  nationality?: string;
  isPrimaryGuest?: boolean;
}

export interface WalkInRoomItemRequest {
  hotelRoomTypeId: number;
  roomInstanceId: number;
  adultCount?: number;
  childCount?: number;
  infantCount?: number;
  extraBeds?: number;
  stayGuests: WalkInStayGuestRequest[];
}

export interface WalkInConfirmedChargeRequest {
  roomInstanceId?: number;
  chargeType: BookingChargeType;
  amount: number;
  description: string;
  isWaived?: boolean;
  waivedReason?: string;
}

export interface WalkInServiceItemRequest {
  roomInstanceId?: number;
  menuItemId: number;
  quantity: number;
  note?: string;
}

export interface WalkInPaymentRequest {
  paymentMethod: 'CASH' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'VNPAY';
  paymentPurpose: 'FULL_PAYMENT' | 'DEPOSIT';
  amount: number;
  transactionReference?: string;
}

export interface WalkInBookingRequest {
  hotelId: number;
  bookingType?: BookingType;
  companyId?: number;
  newCompany?: NewCompanyRequest;
  guestId?: number;
  newGuest?: NewGuestRequest;
  checkInDate: string;
  checkOutDate: string;
  notes?: string;
  rooms: WalkInRoomItemRequest[];
  confirmedCharges?: WalkInConfirmedChargeRequest[];
  serviceItems?: WalkInServiceItemRequest[];
  payment: WalkInPaymentRequest;
}

export interface BookingNoShowRequest {
  reason: string;
}


