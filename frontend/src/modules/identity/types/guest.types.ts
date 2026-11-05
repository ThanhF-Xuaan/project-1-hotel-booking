export interface GuestDto {
  id: number;
  publicId?: string;
  fullName: string;
  birthDate?: string;
  identityType?: string;
  identityNumber?: string;
  nationality?: string;
  email?: string;
  phone?: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
}

export interface GuestCreateRequest {
  fullName: string;
  phone: string;
  email?: string;
  identityType?: string;
  identityNumber?: string;
  birthDate?: string;
  nationality?: string;
}

export interface GuestUpdateRequest {
  fullName: string;
  phone: string;
  email?: string;
  identityType?: string;
  identityNumber?: string;
  birthDate?: string;
  nationality?: string;
  status?: string;
}

export interface GuestSearchDto {
  page?: number;
  pageSize?: number;
  keyword?: string;
  phone?: string;
  identityNumber?: string;
  status?: string;
}

export interface GuestBookingSummaryDto {
  bookingId: number;
  bookingNumber: string;
  hotelName: string;
  status: string;
  totalPrice: number;
  bookingDate: string;
}

export interface GuestStayHistoryResponse {
  guestId: number;
  fullName: string;
  phone: string;
  totalStays: number;
  totalSpent: number;
  bookings: GuestBookingSummaryDto[];
}

