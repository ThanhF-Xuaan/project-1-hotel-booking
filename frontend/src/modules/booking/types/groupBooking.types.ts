export type GroupContractStatus =
  | 'DRAFT'
  | 'TENTATIVE'
  | 'DEPOSIT_PAID'
  | 'ROOMING_LIST_ATTACHED'
  | 'IN_HOUSE'
  | 'SETTLED'
  | 'COMPLETED'
  | 'CANCELLED_NO_DEPOSIT'
  | 'CANCELLED';

export interface GroupRoomBlockResponse {
  id: number;
  contractId: number;
  hotelRoomTypeId: number;
  roomTypeName?: string;
  blockedRooms: number;
  pickedUpRooms: number;
  releasedRooms: number;
  negotiatedRatePerNight: number;
  status: string;
}

export interface GroupContractResponse {
  id: number;
  contractNumber: string;
  hotelId: number;
  hotelName?: string;
  companyId: number;
  companyName?: string;
  salesStaffId?: number;
  salesStaffName?: string;
  groupName: string;
  companyTaxCode: string;
  companyLegalRepresentative?: string;
  signerName?: string;
  signerTitle?: string;
  checkInDate: string;
  checkOutDate: string;
  totalRooms: number;
  totalGuests: number;
  subtotalAmount: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  vatRate: number;
  vatAmount: number;
  totalContractValue: number;
  depositRate: number;
  depositAmount: number;
  depositPaidAmount: number;
  depositDueDate?: string;
  depositPaidAt?: string;
  cutoffDate?: string;
  finalPaymentDueDate?: string;
  lockCheckin?: boolean;
  governingLaw?: string;
  status: GroupContractStatus;
  cancellationReason?: string;
  specialRequests?: string;
  masterFolioId?: number;
  masterFolioNumber?: string;
  createdAt: string;
  roomBlocks?: GroupRoomBlockResponse[];
}

export interface GroupContractSearchDto {
  hotelId?: number;
  keyword?: string;
  contractNumber?: string;
  groupName?: string;
  companyName?: string;
  status?: GroupContractStatus;
  checkInDateFrom?: string;
  checkInDateTo?: string;
  page?: number;
  pageSize?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface ContractRoomBlockItemRequest {
  hotelRoomTypeId: number;
  blockedRooms: number;
  negotiatedRatePerNight: number;
}

export interface GroupContractCreateRequest {
  inquiryId?: number;
  hotelId: number;
  companyId: number;
  groupName: string;
  companyTaxCode: string;
  companyLegalRepresentative?: string;
  signerName?: string;
  signerTitle?: string;
  checkInDate: string;
  checkOutDate: string;
  totalRooms: number;
  totalGuests: number;
  serviceFeeRate?: number;
  vatRate?: number;
  depositRate?: number;
  specialRequests?: string;
  governingLaw?: string;
  roomBlocks: ContractRoomBlockItemRequest[];
}

export interface GroupContractDepositRequest {
  amount: number;
  paymentMethodId?: number;
  transactionReference: string;
  notes?: string;
}

export interface GroupContractCancelRequest {
  cancellationReason?: string;
  reason?: string;
}
