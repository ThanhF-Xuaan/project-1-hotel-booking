export interface CustomerSearchCriteria {
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  adultCount: number;
  childCount: number;
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
