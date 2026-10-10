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
  roomNumber?: string;
  bookingType?: BookingType;
  status?: BookingStatus;
  checkInDate?: string;
  checkOutDate?: string;
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
  bookingGuestId?: number;
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
