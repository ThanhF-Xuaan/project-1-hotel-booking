export interface CustomerSearchCriteria {
  destinationType?: 'REGION' | 'HOTEL';
  destinationId?: number;
  hotelId?: number;
  regionId?: number;
  checkInDate: string;
  checkOutDate: string;
  adultCount: number;
  childCount: number;
  roomsCount?: number;
  voucherCode?: string;
}

export interface AvailableHotelDto {
  hotelId: number;
  hotelName: string;
  address: string;
  phone?: string;
  regionName?: string;
  availableRoomTypesCount: number;
  minPricePerNight: number;
  currency: string;
  coverImageUrl?: string;
  appliedVoucherCode?: string;
  voucherDiscountAmount?: number;
}

export interface UnifiedSearchResponse {
  searchType: 'HOTELS' | 'ROOMS';
  hotels?: {
    content: AvailableHotelDto[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
  };
  rooms?: AvailableRoomTypeApiResponse[];
  selectedHotel?: AvailableHotelDto;
}

export interface AvailableRoomOffer {
  hotelRoomTypeId: number;
  roomTypeCode: string;
  roomTypeName: string;
  basePrice: number;
  estimatedTotal: number;
  maxAdults: number;
  maxChildren: number;
  roomSizeM2: number;
  bedDescription: string;
  amenities: string[];
  freeBreakfast: boolean;
  freeCancellation: boolean;
  availableRoomsCount: number;
}

export interface BookingLookupResult {
  bookingId: number;
  bookingNumber: string;
  hotelName: string;
  guestName: string;
  guestPhone: string;
  checkInDate: string;
  checkOutDate: string;
  status: string;
  totalAmount: number;
  roomTypeName: string;
}

export interface DestinationSuggestionDto {
  id: number;
  type: 'HOTEL' | 'REGION';
  title: string;
  subTitle: string;
  badge?: string;
}

export interface RoomAmenityDto {
  id: number;
  code: string;
  name: string;
  category?: string | null;
}

export interface RoomInclusionDto {
  id: number;
  referenceType?: string;
  referenceId?: number;
  name: string;
  quantity?: number;
  description?: string;
}

export interface RoomSurchargePolicyDto {
  ruleType: string;
  adjustmentValue: number;
  formattedDescription?: string;
}

export interface DailyPriceDto {
  date: string;
  baseRate: number;
  surchargeAmount?: number;
  rateAfterSurcharge?: number;
  rateAdjustmentAmount?: number;
  grossDailyTotal?: number;
}

export interface PriceBreakdownDto {
  hotelRoomTypeId?: number;
  roomTypeName?: string;
  totalNights?: number;
  totalBasePrice?: number;
  totalRoomGrossAmount?: number;
  finalTotalAmount?: number;
  dailyPrices?: DailyPriceDto[];
}

export interface AvailableRoomTypeApiResponse {
  hotelRoomTypeId: number;
  roomTypeCode: string;
  roomTypeName: string;
  description?: string | null;
  standardAdults?: number;
  standardChildren?: number;
  maxAdults: number;
  maxChildren: number;
  maxInfants?: number;
  maxTotalGuests?: number;
  maxBeds?: number;
  extraBeds?: number;
  availableRooms: number;
  priceBreakdown?: PriceBreakdownDto;
  amenities?: RoomAmenityDto[];
  inclusions?: RoomInclusionDto[];
  surchargePolicies?: RoomSurchargePolicyDto[];
}
