import { apiClient } from '../../../core/api/client';
import type { PageResponse } from '../../../core/api/types';
import type {
  PaymentCreateRequest,
  PaymentSearchDto,
  PaymentResponse,
  InvoiceCreateRequest,
  InvoiceSearchDto,
  InvoiceResponse,
  GatewayCreateRequest,
  PaymentUrlResponse,
} from '../types/finance.types';

const PAYMENT_URL = '/api/v1/payments';
const INVOICE_URL = '/api/v1/invoices';

export const financeService = {
  // Payments
  createPayment: (data: PaymentCreateRequest) =>
    apiClient.post<PaymentResponse>(`${PAYMENT_URL}/create`, data),

  // Giai đoạn D — tạo phiên thanh toán online (trả paymentUrl + txnRef + expiresAt)
  createVnPayPayment: (data: GatewayCreateRequest) =>
    apiClient.post<PaymentUrlResponse>(`${PAYMENT_URL}/vnpay/create`, data),

  createMoMoPayment: (data: GatewayCreateRequest) =>
    apiClient.post<PaymentUrlResponse>(`${PAYMENT_URL}/momo/create`, data),

  // Hủy thanh toán đang dở (PENDING → CANCELLED) — flow lỗi §6
  cancelPayment: (id: number) =>
    apiClient.post<PaymentResponse>(`${PAYMENT_URL}/${id}/cancel`, {}),

  filterPayments: (params: PaymentSearchDto) =>
    apiClient.post<PageResponse<PaymentResponse>>(`${PAYMENT_URL}/filter`, params),

  getPaymentById: (id: number) =>
    apiClient.get<PaymentResponse>(`${PAYMENT_URL}/${id}`),

  getPaymentsByBookingId: (bookingId: number) =>
    apiClient.get<PaymentResponse[]>(`${PAYMENT_URL}/booking/${bookingId}`),

  completePayment: (id: number) =>
    apiClient.post<PaymentResponse>(`${PAYMENT_URL}/${id}/complete`, {}),

  refundPayment: (id: number, refundAmount: number, reason?: string) =>
    apiClient.post<PaymentResponse>(`${PAYMENT_URL}/${id}/refund`, null, {
      params: { refundAmount, reason },
    }),

  // Invoices
  createInvoice: (data: InvoiceCreateRequest) =>
    apiClient.post<InvoiceResponse>(`${INVOICE_URL}/create`, data),

  filterInvoices: (params: InvoiceSearchDto) =>
    apiClient.post<PageResponse<InvoiceResponse>>(`${INVOICE_URL}/filter`, params),

  getInvoiceById: (id: number) =>
    apiClient.get<InvoiceResponse>(`${INVOICE_URL}/${id}`),

  getInvoiceByBookingId: (bookingId: number) =>
    apiClient.get<InvoiceResponse>(`${INVOICE_URL}/booking/${bookingId}`),

  issueInvoice: (id: number) =>
    apiClient.put<InvoiceResponse>(`${INVOICE_URL}/${id}/issue`, {}),
};

export default financeService;
