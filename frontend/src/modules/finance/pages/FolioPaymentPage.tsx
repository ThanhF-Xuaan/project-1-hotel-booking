import React, { useState, useCallback, useRef } from 'react';
import {
  DollarSign,
  Search,
  RefreshCw,
  CheckCircle,
  Receipt,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal from '../../../core/components/ui/Modal';
import { getApiErrorMessage, type ApiErrorInfo } from '../../../core/api/error';
import financeService from '../services/finance.service';
import bookingService from '../../booking/services/booking.service';
import CheckoutModal from '../components/payment/CheckoutModal';
import PaymentErrorModal from '../components/payment/PaymentErrorModal';
import PaymentStatusBadge from '../components/payment/PaymentStatusBadge';
import type {
  PaymentResponse,
  InvoiceResponse,
  PaymentMethod,
  PaymentPurpose,
  GatewayMethod,
  PaymentUrlResponse,
} from '../types/finance.types';
import type { BookingResponse } from '../../booking/types/booking.types';

/** Cấu hình ô nhập mã tham chiếu theo từng method (mục 1 — bảng 0.1) */
const REFERENCE_CONFIG: Record<
  PaymentMethod,
  { label: string; required: boolean; placeholder: string }
> = {
  CASH: { label: 'Số phiếu thu (tùy chọn)', required: false, placeholder: 'VD: PT-001' },
  BANK_TRANSFER: { label: 'Mã giao dịch ngân hàng *', required: true, placeholder: 'VD: FT260930001' },
  CREDIT_CARD: { label: 'Approval code (POS) *', required: true, placeholder: 'VD: AP778899' },
  DEBIT_CARD: { label: 'Approval code (POS) *', required: true, placeholder: 'VD: AP778899' },
  VNPAY: { label: 'Mã tham chiếu (tự ghi từ webhook)', required: false, placeholder: '' },
  MOMO: { label: 'Mã tham chiếu (tự ghi từ webhook)', required: false, placeholder: '' },
  ZALOPAY: { label: 'Mã tham chiếu', required: false, placeholder: '' },
  OTHER: { label: 'Mã tham chiếu / Biên lai', required: false, placeholder: 'VD: HD-001' },
};

const isGatewayMethod = (m: PaymentMethod): m is GatewayMethod => m === 'VNPAY' || m === 'MOMO';

export const FolioPaymentPage: React.FC = () => {
  const [bookingNumberInput, setBookingNumberInput] = useState('');
  const [currentBooking, setCurrentBooking] = useState<BookingResponse | null>(null);
  const [payments, setPayments] = useState<PaymentResponse[]>([]);
  const [invoice, setInvoice] = useState<InvoiceResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Payment Modal
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [payAmount, setPayAmount] = useState<number>(0);
  const [payMethod, setPayMethod] = useState<PaymentMethod>('CASH');
  const [payPurpose, setPayPurpose] = useState<PaymentPurpose>('FULL_PAYMENT');
  const [payProvider, setPayProvider] = useState('');
  const [payReference, setPayReference] = useState('');
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);

  // Giai đoạn D — phiên thanh toán online (VNPay/MoMo) + popup lỗi §6
  const [activeGateway, setActiveGateway] = useState<GatewayMethod>('VNPAY');
  const [gatewaySession, setGatewaySession] = useState<PaymentUrlResponse | null>(null);
  const [isCheckoutModalOpen, setIsCheckoutModalOpen] = useState(false);
  const [paymentError, setPaymentError] = useState<ApiErrorInfo | null>(null);
  const [isCancellingPayment, setIsCancellingPayment] = useState(false);
  const referenceInputRef = useRef<HTMLInputElement>(null);

  // Refund Modal
  const [isRefundModalOpen, setIsRefundModalOpen] = useState(false);
  const [refundPaymentId, setRefundPaymentId] = useState<number | null>(null);
  const [refundAmount, setRefundAmount] = useState<number>(0);
  const [refundReason, setRefundReason] = useState('');
  const [isRefunding, setIsRefunding] = useState(false);

  // Invoicing Modal
  const [isGeneratingInvoice, setIsGeneratingInvoice] = useState(false);

  const fetchFolioData = useCallback(async (bookingNum: string) => {
    if (!bookingNum) return;
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const bRes = await bookingService.getByNumber(bookingNum.trim());
      if (bRes.result) {
        const booking = bRes.result;
        setCurrentBooking(booking);

        // Load payments
        const pRes = await financeService.getPaymentsByBookingId(booking.id);
        if (pRes.result) {
          setPayments(pRes.result);
        }

        // Load invoice
        try {
          const invRes = await financeService.getInvoiceByBookingId(booking.id);
          if (invRes.result) {
            setInvoice(invRes.result);
          } else {
            setInvoice(null);
          }
        } catch {
          setInvoice(null);
        }
      }
    } catch (err: unknown) {
      console.error('Lỗi tra cứu Folio:', err);
      setErrorMessage('Không tìm thấy đơn đặt phòng với mã đã nhập');
      setCurrentBooking(null);
      setPayments([]);
      setInvoice(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const totalCharges = currentBooking ? currentBooking.totalAmount : 0;
  const totalPaid = payments
    .filter((p) => p.status === 'SUCCESS')
    .reduce((sum, p) => sum + Number(p.totalAmount), 0);
  const balanceDue = totalCharges - totalPaid;

  const handleOpenPaymentModal = () => {
    if (!currentBooking) return;
    setPayAmount(Math.max(0, balanceDue));
    setIsPaymentModalOpen(true);
  };

  const handleSubmitPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentBooking || payAmount <= 0) return;

    // D2 — validate mã tham chiếu theo method ngay tại FE (BE cũng bắt, code 8007):
    // popup → giữ nguyên dữ liệu đã nhập → focus lại ô (§6.2)
    const refConfig = REFERENCE_CONFIG[payMethod];
    if (refConfig.required && !payReference.trim()) {
      setPaymentError({
        code: 8007,
        message: `Phương thức "${refConfig.label.replace(' *', '')}" bắt buộc nhập mã tham chiếu. Vui lòng nhập để tiếp tục.`,
      });
      referenceInputRef.current?.focus();
      return;
    }

    setIsProcessingPayment(true);
    try {
      if (isGatewayMethod(payMethod)) {
        // D3 — tạo phiên thanh toán online → mở CheckoutModal (countdown + link gateway)
        const create =
          payMethod === 'VNPAY'
            ? financeService.createVnPayPayment
            : financeService.createMoMoPayment;
        const res = await create({ bookingId: currentBooking.id, method: payMethod });
        if (res.result) {
          setActiveGateway(payMethod);
          setGatewaySession(res.result);
          setIsPaymentModalOpen(false);
          setIsCheckoutModalOpen(true);
        }
      } else {
        await financeService.createPayment({
          bookingId: currentBooking.id,
          totalAmount: payAmount,
          paymentMethod: payMethod,
          paymentPurpose: payPurpose,
          paymentProvider: payProvider || undefined,
          transactionReference: payReference || undefined,
        });

        setIsPaymentModalOpen(false);
        setSuccessMessage('Thanh toán đã được ghi nhận thành công!');
        fetchFolioData(currentBooking.bookingNumber);
      }
    } catch (err) {
      // §6 bước ① — popup lỗi (code + message từ ApiResponse), KHÔNG mất dữ liệu đã nhập
      console.error('Lỗi thanh toán:', err);
      setPaymentError(getApiErrorMessage(err, 'Không thể ghi nhận thanh toán'));
    } finally {
      setIsProcessingPayment(false);
    }
  };

  // §6 bước ②③④ — hủy payment dở (PENDING → CANCELLED) → reset state → khách làm lại từ đầu
  const handleCancelAndRestart = async () => {
    setIsCancellingPayment(true);
    try {
      if (gatewaySession?.paymentId) {
        try {
          await financeService.cancelPayment(gatewaySession.paymentId);
        } catch (err) {
          // Best-effort: payment có thể đã xử lý xong qua webhook — vẫn reset FE
          console.warn('Không hủy được payment (có thể đã xử lý):', err);
        }
      }
      setGatewaySession(null);
      setIsCheckoutModalOpen(false);
      setIsPaymentModalOpen(false);
      setPayReference('');
      setPayProvider('');
      setPaymentError(null);
    } finally {
      setIsCancellingPayment(false);
    }
  };

  const handleOpenRefundModal = (payment: PaymentResponse) => {
    setRefundPaymentId(payment.id);
    setRefundAmount(Number(payment.totalAmount));
    setRefundReason('Hoàn tiền trả phòng sớm');
    setIsRefundModalOpen(true);
  };

  const handleConfirmRefund = async () => {
    if (!refundPaymentId || !currentBooking) return;
    setIsRefunding(true);
    try {
      await financeService.refundPayment(refundPaymentId, refundAmount, refundReason);
      setIsRefundModalOpen(false);
      setSuccessMessage('Hoàn tiền thành công!');
      fetchFolioData(currentBooking.bookingNumber);
    } catch (err) {
      console.error('Lỗi hoàn tiền:', err);
      alert('Hoàn tiền thất bại.');
    } finally {
      setIsRefunding(false);
    }
  };

  const handleGenerateInvoice = async () => {
    if (!currentBooking) return;
    setIsGeneratingInvoice(true);
    try {
      const res = await financeService.createInvoice({
        bookingId: currentBooking.id,
        serviceFeeRate: currentBooking.serviceFeeRate,
      });
      if (res.result) {
        setInvoice(res.result);
        setSuccessMessage('Tạo hóa đơn GTGT thành công!');
      }
    } catch (err) {
      console.error('Lỗi tạo hóa đơn:', err);
      alert('Không thể tạo hóa đơn.');
    } finally {
      setIsGeneratingInvoice(false);
    }
  };

  const handleIssueInvoice = async () => {
    if (!invoice || !currentBooking) return;
    try {
      const res = await financeService.issueInvoice(invoice.id);
      if (res.result) {
        setInvoice(res.result);
        setSuccessMessage('Hóa đơn đã được phát hành chính thức!');
      }
    } catch (err) {
      console.error('Lỗi phát hành hóa đơn:', err);
      alert('Phát hành hóa đơn thất bại.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-neutral-900">Quản lý Tài chính & Folio Khách hàng</h1>
        <p className="text-sm text-neutral-500 mt-1">
          Theo dõi công nợ, giao dịch thanh toán, phụ phí và phát hành hóa đơn GTGT (VAT Invoice)
        </p>
      </div>

      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center justify-between">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage(null)} className="text-sm font-semibold underline">
            Đóng
          </button>
        </div>
      )}

      {/* SEARCH FOLIO BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex gap-4 items-center">
        <div className="flex-1">
          <Input
            placeholder="Nhập mã đơn đặt phòng (VD: BK...)"
            value={bookingNumberInput}
            onChange={(e) => setBookingNumberInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') fetchFolioData(bookingNumberInput);
            }}
          />
        </div>
        <Button
          variant="primary"
          onClick={() => fetchFolioData(bookingNumberInput)}
          disabled={isLoading || !bookingNumberInput}
        >
          <Search className="w-4 h-4 mr-2" />
          Tra cứu Folio
        </Button>
      </div>

      {errorMessage && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl">
          {errorMessage}
        </div>
      )}

      {currentBooking && (
        <div className="space-y-6">
          {/* FOLIO SUMMARY CARDS */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
              <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                Tổng phát sinh (Charges)
              </span>
              <p className="text-2xl font-bold text-neutral-900 mt-1">
                {new Intl.NumberFormat('vi-VN').format(totalCharges)} đ
              </p>
              <span className="text-xs text-neutral-400 mt-1 block">
                Bao gồm tiền phòng, phí dịch vụ & VAT
              </span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
              <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                Đã thanh toán (Paid)
              </span>
              <p className="text-2xl font-bold text-emerald-600 mt-1">
                {new Intl.NumberFormat('vi-VN').format(totalPaid)} đ
              </p>
              <span className="text-xs text-neutral-400 mt-1 block">
                {payments.filter((p) => p.status === 'SUCCESS').length} giao dịch thành công
              </span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
              <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                Còn lại cần thu (Balance Due)
              </span>
              <p className={`text-2xl font-bold mt-1 ${balanceDue > 0 ? 'text-red-600' : 'text-neutral-900'}`}>
                {new Intl.NumberFormat('vi-VN').format(balanceDue)} đ
              </p>
              <span className="text-xs text-neutral-400 mt-1 block">
                {balanceDue <= 0 ? 'Đã quyết toán toàn bộ' : 'Chưa tất toán'}
              </span>
            </div>
          </div>

          {/* ACTION BUTTONS */}
          <div className="flex flex-wrap gap-3">
            <Button variant="primary" onClick={handleOpenPaymentModal}>
              <DollarSign className="w-4 h-4 mr-2" />
              Thu tiền / Thanh toán mới
            </Button>

            {!invoice && (
              <Button
                variant="outline"
                onClick={handleGenerateInvoice}
                disabled={isGeneratingInvoice}
              >
                <Receipt className="w-4 h-4 mr-2" />
                {isGeneratingInvoice ? 'Đang tạo...' : 'Tạo hóa đơn GTGT'}
              </Button>
            )}

            {invoice && invoice.status === 'DRAFT' && (
              <Button variant="primary" onClick={handleIssueInvoice}>
                <CheckCircle className="w-4 h-4 mr-2" />
                Phát hành hóa đơn ({invoice.invoiceNumber})
              </Button>
            )}

            <Button
              variant="outline"
              onClick={() => fetchFolioData(currentBooking.bookingNumber)}
              disabled={isLoading}
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
              Làm mới Folio
            </Button>
          </div>

          {/* BẢNG LỊCH SỬ THANH TOÁN */}
          <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
            <div className="p-4 border-b border-neutral-200 font-bold text-neutral-900 flex justify-between items-center">
              <span>Lịch sử Giao dịch & Thanh toán</span>
              <span className="text-xs font-normal text-neutral-500">{payments.length} khoản</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-600 font-medium">
                  <tr>
                    <th className="p-4">Mã giao dịch</th>
                    <th className="p-4">Phương thức</th>
                    <th className="p-4">Mục đích</th>
                    <th className="p-4 text-right">Số tiền (VNĐ)</th>
                    <th className="p-4 text-center">Trạng thái</th>
                    <th className="p-4">Thời gian</th>
                    <th className="p-4 text-center">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {payments.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="p-6 text-center text-neutral-400">
                        Chưa có giao dịch thanh toán nào được ghi nhận cho Folio này.
                      </td>
                    </tr>
                  ) : (
                    payments.map((p) => (
                      <tr key={p.id} className="hover:bg-neutral-50">
                        <td className="p-4 font-mono font-semibold text-neutral-900">
                          {p.transactionReference || `PAY-${p.id}`}
                        </td>
                        <td className="p-4 text-neutral-700">{p.paymentMethod}</td>
                        <td className="p-4 text-neutral-600">{p.paymentPurpose}</td>
                        <td className="p-4 text-right font-bold text-neutral-900">
                          {new Intl.NumberFormat('vi-VN').format(p.totalAmount)} đ
                        </td>
                        <td className="p-4 text-center">
                          <PaymentStatusBadge status={p.status} />
                        </td>
                        <td className="p-4 text-xs text-neutral-500">
                          {p.paidAt ? new Date(p.paidAt).toLocaleString('vi-VN') : 'N/A'}
                        </td>
                        <td className="p-4 text-center">
                          {p.status === 'SUCCESS' && (
                            <button
                              onClick={() => handleOpenRefundModal(p)}
                              className="text-xs text-amber-600 hover:text-amber-800 font-semibold px-2 py-1 rounded-md hover:bg-amber-50"
                            >
                              Hoàn tiền
                            </button>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* CHI TIẾT HÓA ĐƠN GTGT (NẾU ĐÃ TẠO) */}
          {invoice && (
            <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs p-6 space-y-4">
              <div className="flex justify-between items-start border-b border-neutral-100 pb-4">
                <div>
                  <h3 className="text-lg font-bold text-neutral-900">Hóa đơn GTGT: {invoice.invoiceNumber}</h3>
                  <span className="text-xs text-neutral-500">
                    Trạng thái: <b className="text-neutral-800">{invoice.status}</b> | Ngày lập:{' '}
                    {new Date(invoice.createdAt).toLocaleDateString('vi-VN')}
                  </span>
                </div>
                <div className="text-right">
                  <span className="text-xs text-neutral-500">Tổng thanh toán hóa đơn</span>
                  <p className="text-xl font-bold text-red-600">
                    {new Intl.NumberFormat('vi-VN').format(invoice.grandTotal)} đ
                  </p>
                </div>
              </div>

              {/* Chi tiết từng dòng hóa đơn */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-neutral-50 text-neutral-600 font-semibold">
                    <tr>
                      <th className="p-3">Khoản mục</th>
                      <th className="p-3 text-center">SL</th>
                      <th className="p-3 text-right">Đơn giá</th>
                      <th className="p-3 text-right">VAT (%)</th>
                      <th className="p-3 text-right">Tiền thuế</th>
                      <th className="p-3 text-right">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100">
                    {invoice.details?.map((d) => (
                      <tr key={d.id}>
                        <td className="p-3 font-medium text-neutral-800">{d.description}</td>
                        <td className="p-3 text-center">{d.quantity}</td>
                        <td className="p-3 text-right">{new Intl.NumberFormat('vi-VN').format(d.unitPrice)} đ</td>
                        <td className="p-3 text-right">{d.vatRate}%</td>
                        <td className="p-3 text-right">{new Intl.NumberFormat('vi-VN').format(d.vatAmount)} đ</td>
                        <td className="p-3 text-right font-bold text-neutral-900">
                          {new Intl.NumberFormat('vi-VN').format(d.totalAmount)} đ
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* MODAL THANH TOÁN */}
      <Modal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        title="Ghi nhận Thanh toán Folio"
      >
        <form onSubmit={handleSubmitPayment} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Số tiền thanh toán (VNĐ) *</label>
            <Input
              type="number"
              value={payAmount}
              onChange={(e) => setPayAmount(Number(e.target.value))}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Phương thức thanh toán</label>
              <select
                className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={payMethod}
                onChange={(e) => setPayMethod(e.target.value as PaymentMethod)}
              >
                <option value="CASH">Tiền mặt (Cash)</option>
                <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
                <option value="CREDIT_CARD">Thẻ tín dụng (Credit Card)</option>
                <option value="DEBIT_CARD">Thẻ ghi nợ (Debit Card)</option>
                <option value="VNPAY">VNPAY QR</option>
                <option value="MOMO">Ví MoMo</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Mục đích thu</label>
              <select
                className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={payPurpose}
                onChange={(e) => setPayPurpose(e.target.value as PaymentPurpose)}
              >
                <option value="FULL_PAYMENT">Thanh toán toàn bộ</option>
                <option value="DEPOSIT">Đặt cọc tiền phòng</option>
                <option value="INCIDENTAL_DEPOSIT">Đặt cọc phát sinh</option>
              </select>
            </div>
          </div>

          {isGatewayMethod(payMethod) ? (
            // D2 — VNPAY/MOMO: KHÔNG nhập tay mã tham chiếu (webhook tự ghi) + hiện số tiền theo tổng đơn
            <div className="bg-blue-50 border border-blue-200 text-blue-700 rounded-2xl p-4 text-xs leading-relaxed">
              Thanh toán qua <b>{payMethod === 'VNPAY' ? 'VNPay' : 'MoMo'}</b>: mã tham chiếu sẽ
              <b> tự động ghi từ webhook</b> của cổng thanh toán (không nhập tay).
              <br />
              Số tiền thu theo <b>tổng tiền đơn</b>: <b className="text-blue-900">{new Intl.NumberFormat('vi-VN').format(Number(currentBooking?.totalAmount))} đ</b>{' '}
              — hệ thống tự mở phiên thanh toán có hiệu lực 10 phút.
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">Cổng / Đơn vị thanh toán</label>
                <Input
                  placeholder="VD: Vietcombank, Techcombank, VNPAY"
                  value={payProvider}
                  onChange={(e) => setPayProvider(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  {REFERENCE_CONFIG[payMethod].label}
                </label>
                <Input
                  ref={referenceInputRef}
                  placeholder={REFERENCE_CONFIG[payMethod].placeholder}
                  value={payReference}
                  onChange={(e) => setPayReference(e.target.value)}
                  error={
                    paymentError?.code === 8007 ? 'Mã tham chiếu là bắt buộc với phương thức này' : undefined
                  }
                />
              </div>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button variant="outline" type="button" onClick={() => setIsPaymentModalOpen(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={isProcessingPayment || payAmount <= 0}>
              {isProcessingPayment
                ? 'Đang xử lý...'
                : isGatewayMethod(payMethod)
                ? 'Tạo phiên thanh toán'
                : 'Xác nhận thu tiền'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL HOÀN TIỀN */}
      <Modal
        isOpen={isRefundModalOpen}
        onClose={() => setIsRefundModalOpen(false)}
        title="Xác nhận Hoàn tiền (Refund)"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Số tiền hoàn (VNĐ) *</label>
            <Input
              type="number"
              value={refundAmount}
              onChange={(e) => setRefundAmount(Number(e.target.value))}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Lý do hoàn trả</label>
            <Input
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
              placeholder="VD: Khách trả phòng sớm, hoàn cọc phòng..."
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button variant="outline" onClick={() => setIsRefundModalOpen(false)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              onClick={handleConfirmRefund}
              disabled={isRefunding || refundAmount <= 0}
            >
              {isRefunding ? 'Đang hoàn...' : 'Xác nhận hoàn tiền'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL PHIÊN THANH TOÁN GATEWAY (D3) — countdown + link + hủy/làm lại (§6) */}
      <CheckoutModal
        isOpen={isCheckoutModalOpen}
        onClose={() => setIsCheckoutModalOpen(false)}
        gateway={activeGateway}
        session={gatewaySession}
        amount={payAmount}
        onCancelAndRetry={handleCancelAndRestart}
        isCancelling={isCancellingPayment}
      />

      {/* POPUP LỖI THANH TOÁN (§6 — dùng core Modal, không toast) */}
      <PaymentErrorModal
        isOpen={paymentError !== null}
        code={paymentError?.code}
        message={paymentError?.message ?? ''}
        onRetry={handleCancelAndRestart}
        onClose={() => setPaymentError(null)}
        isRetrying={isCancellingPayment}
      />
    </div>
  );
};

export default FolioPaymentPage;
