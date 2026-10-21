export type DocumentType = 'CCCD' | 'PASSPORT' | 'CMND' | 'DRIVER_LICENSE' | 'OTHER';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface StayGuestResponse {
  id: number;
  hotelId: number;
  hotelName?: string;
  bookingId?: number;
  roomId: number;
  roomNumber: string;
  fullName: string;
  dateOfBirth?: string;
  gender?: Gender;
  nationality?: string;
  documentType?: DocumentType;
  documentNumber?: string;
  permanentAddress?: string;
  currentAddress?: string;
  checkInTime?: string;
  expectedCheckOutTime?: string;
  actualCheckOutTime?: string;
  reasonForStay?: string;
  documentImageUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StayGuestCreateRequest {
  hotelId: number;
  bookingId?: number;
  bookingRoomId?: number;
  roomId: number;
  roomNumber: string;
  fullName: string;
  dateOfBirth?: string;
  gender?: Gender;
  nationality?: string;
  documentType?: DocumentType;
  documentNumber?: string;
  permanentAddress?: string;
  currentAddress?: string;
  checkInTime?: string;
  expectedCheckOutTime?: string;
  reasonForStay?: string;
  documentImageUrl?: string;
}

export interface StayGuestUpdateRequest {
  fullName?: string;
  dateOfBirth?: string;
  gender?: Gender;
  nationality?: string;
  documentType?: DocumentType;
  documentNumber?: string;
  permanentAddress?: string;
  currentAddress?: string;
  checkInTime?: string;
  expectedCheckOutTime?: string;
  actualCheckOutTime?: string;
  reasonForStay?: string;
  documentImageUrl?: string;
}
