import type { PaymentMethod } from './finance.types';

export type MasterFolioStatus = 'OPEN' | 'LOCKED_FOR_SETTLEMENT' | 'SETTLED' | 'CLOSED';

export type ChargeCategory =
  | 'ROOM_RATE'
  | 'MINIBAR'
  | 'LAUNDRY'
  | 'BREAKFAST'
  | 'BANQUET'
  | 'SPA'
  | 'TRANSPORT'
  | 'CUSTOM';

export type RoutingTarget = 'MASTER' | 'SUB_FOLIO';

export interface BillingRoutingRuleResponse {
  id: number;
  masterFolioId: number;
  chargeCategory: ChargeCategory;
  routeTarget: RoutingTarget;
  maxCoveredAmount?: number;
  isActive: boolean;
  createdAt: string;
}

export interface MasterFolioLineItemDto {
  referenceId: number;
  itemType: string;
  chargeCategory: ChargeCategory;
  description: string;
  itemDate: string;
  roomNumber?: string;
  guestName?: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  vatRate: number;
  vatAmount: number;
  totalAmount: number;
  routingTarget: RoutingTarget;
  isManuallyAdjusted: boolean;
  adjustmentReason?: string;
}

export interface SubFolioSummaryDto {
  bookingId: number;
  bookingNumber: string;
  bookingRoomId: number;
  roomNumber: string;
  roomTypeName: string;
  guestName?: string;
  roomStatus: string;
  coveredByMasterAmount: number;
  personalDebitAmount: number;
  personalPaidAmount: number;
  personalBalance: number;
}

export interface MasterFolioResponse {
  id: number;
  folioNumber: string;
  contractId: number;
  contractNumber: string;
  hotelId: number;
  hotelName: string;
  companyId?: number;
  companyName?: string;
  buyerLegalName?: string;
  buyerTaxCode?: string;
  buyerAddress?: string;
  buyerEmailForInvoice?: string;
  subtotalAmount: number;
  serviceFeeAmount: number;
  totalVatAmount: number;
  totalAmount: number;
  depositCredited: number;
  totalPaid: number;
  balance: number;
  status: MasterFolioStatus;
  closedAt?: string;
  createdAt: string;
  routingRules: BillingRoutingRuleResponse[];
  debits: MasterFolioLineItemDto[];
  subFolios: SubFolioSummaryDto[];
}

export interface GroupCheckoutRequest {
  paymentMethod: PaymentMethod;
  paymentReference?: string;
  waiveUnpaidSubFolios?: boolean;
  notes?: string;
  buyerLegalName?: string;
  buyerTaxCode?: string;
  buyerAddress?: string;
  buyerEmailForInvoice?: string;
}

export interface GroupCheckoutResponse {
  masterFolioId: number;
  folioNumber: string;
  contractId: number;
  contractNumber: string;
  groupName: string;
  status: MasterFolioStatus;
  subtotalAmount: number;
  serviceFeeAmount: number;
  totalVatAmount: number;
  totalChargesAmount: number;
  depositCredited: number;
  finalPaymentAmount: number;
  remainingBalance: number;
  paymentMethod: PaymentMethod;
  paymentReference?: string;
  checkedOutRoomsCount: number;
  checkedOutRoomNumbers: string[];
  settledAt: string;
  settledBy: string;
}

export interface MasterFolioAdjustmentRequest {
  bookingRoomId?: number;
  chargeCategory: ChargeCategory;
  itemType: string;
  referenceId: number;
  itemDescription: string;
  adjustedTarget: RoutingTarget;
  amount: number;
  adjustmentReason: string;
}
