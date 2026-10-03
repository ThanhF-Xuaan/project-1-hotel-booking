export type PaymentMethod =
  | 'CASH'
  | 'BANK_TRANSFER'
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'VNPAY'
  | 'MOMO'
  | 'ZALOPAY'
  | 'OTHER';

export type PaymentPurpose = 'DEPOSIT' | 'FULL_PAYMENT' | 'INCIDENTAL_DEPOSIT' | 'REFUND';

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED' | 'CANCELLED';

export type TransactionType = 'PAYMENT' | 'REFUND';

export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'CANCELLED';

export type InvoiceLineType = 'ROOM_RATE' | 'PRODUCT' | 'SERVICE' | 'SURCHARGE' | 'PENALTY';

export interface PaymentCreateRequest {
  bookingId: number;
  totalAmount: number;
  paymentPurpose?: PaymentPurpose;
  paymentMethod: PaymentMethod;
  paymentProvider?: string;
  transactionReference?: string;
}

export interface PaymentSearchDto {
  bookingId?: number;
  paymentMethod?: PaymentMethod;
  paymentPurpose?: PaymentPurpose;
  status?: PaymentStatus;
  page?: number;
  size?: number;
}

export interface PaymentResponse {
  id: number;
  bookingId: number;
  bookingNumber: string;
  totalAmount: number;
  paymentPurpose: PaymentPurpose;
  paymentMethod: PaymentMethod;
  paymentProvider?: string;
  transactionReference?: string;
  status: PaymentStatus;
  paidAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionResponse {
  id: number;
  paymentId: number;
  bookingId: number;
  transactionType: TransactionType;
  amount: number;
  referenceCode?: string;
  status: string;
  issuedAt: string;
  createdAt: string;
}

export interface InvoiceDetailResponse {
  id: number;
  referenceId: number;
  lineType: InvoiceLineType;
  description: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  vatRate: number;
  vatAmount: number;
  totalAmount: number;
  createdAt: string;
}

export interface InvoiceResponse {
  id: number;
  bookingId: number;
  bookingNumber: string;
  invoiceNumber: string;
  subTotal: number;
  serviceFeeRate: number;
  serviceFeeAmount: number;
  vatAmount: number;
  grandTotal: number;
  status: InvoiceStatus;
  issuedAt?: string;
  createdAt: string;
  updatedAt: string;
  details: InvoiceDetailResponse[];
}

export interface InvoiceCreateRequest {
  bookingId: number;
  serviceFeeRate?: number;
}

export interface InvoiceSearchDto {
  bookingId?: number;
  invoiceNumber?: string;
  status?: InvoiceStatus;
  page?: number;
  size?: number;
}

// ==================== Giai đoạn D — Payment Gateway (VNPay/MoMo) ====================

/** Phương thức thanh toán online qua gateway */
export type GatewayMethod = 'VNPAY' | 'MOMO';

/** Request tạo phiên thanh toán online — POST /payments/vnpay/create | /payments/momo/create */
export interface GatewayCreateRequest {
  bookingId: number;
  method: GatewayMethod;
  /** Tùy chọn: tái sử dụng Payment PENDING đã tạo trước đó */
  paymentId?: number;
}

/** Phản hồi tạo phiên — FE redirect khách ra paymentUrl, countdown theo expiresAt (TTL 10') */
export interface PaymentUrlResponse {
  paymentId: number;
  paymentUrl: string;
  txnRef: string;
  expiresAt: string;
}

/** Trạng thái hiển thị ở trang kết quả (poll PENDING → PAID/FAILED) */
export type CallbackStatus = 'PENDING' | 'PAID' | 'FAILED';
