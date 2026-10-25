import React, { useState, useEffect, useCallback } from 'react';
import {
  DollarSign,
  CreditCard,
  CheckCircle,
  AlertCircle,
  User,
  Phone,
  DoorOpen,
  Receipt,
  FileCheck,
  ShieldCheck,
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import bookingService from '../services/booking.service';
import { financeService } from '../../finance/services/finance.service';
import type { CheckoutSettlementResponse, SettlementLineItemDto } from '../types/booking.types';
import type { PaymentMethod } from '../../finance/types/finance.types';

export interface CheckoutSettlementModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingRoomId: number | null;
  onCheckOutSuccess: (bookingNumber: string) => void;
}

export const CheckoutSettlementModal: React.FC<CheckoutSettlementModalProps> = ({
  isOpen,
  onClose,
  bookingRoomId,
  onCheckOutSuccess,
}) => {
  const [settlement, setSettlement] = useState<CheckoutSettlementResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isProcessingCheckout, setIsProcessingCheckout] = useState(false);

  // Form thanh toán ngay
  const [paymentAmount, setPaymentAmount] = useState<number>(0);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [paymentReference, setPaymentReference] = useState('');
  const [isSubmittingPayment, setIsSubmittingPayment] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [paymentSuccessMsg, setPaymentSuccessMsg] = useState<string | null>(null);

  const fetchSettlement = useCallback(async () => {
    if (!bookingRoomId) return;
    setIsLoading(true);
    setPaymentError(null);
    try {
      const res = await bookingService.getCheckoutSettlement(bookingRoomId);
      if (res.result) {
        setSettlement(res.result);
        setPaymentAmount(res.result.balanceDue > 0 ? res.result.balanceDue : 0);
      }
    } catch (err: unknown) {
      console.error('Lỗi lấy thông tin quyết toán check-out:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Không thể tải thông tin quyết toán check-out. Vui lòng thử lại.';
      setPaymentError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [bookingRoomId]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isOpen && bookingRoomId) {
        void fetchSettlement();
        setPaymentSuccessMsg(null);
      } else {
        setSettlement(null);
      }
    }, 0);
    return () => clearTimeout(timer);
  }, [isOpen, bookingRoomId, fetchSettlement]);

  const formatVND = (val?: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

  const handleMakePayment = async () => {
    if (!settlement) return;
    if (paymentAmount <= 0) {
      setPaymentError('Số tiền thanh toán phải lớn hơn 0 đ');
      return;
    }
    setIsSubmittingPayment(true);
    setPaymentError(null);
    setPaymentSuccessMsg(null);
    try {
      await financeService.createPayment({
        bookingId: settlement.bookingId,
        totalAmount: paymentAmount,
        paymentMethod: paymentMethod,
        paymentPurpose: paymentAmount >= settlement.balanceDue ? 'FULL_PAYMENT' : 'DEPOSIT',
        transactionReference: paymentReference.trim() || undefined,
      });

      setPaymentSuccessMsg(`Ghi nhận thanh toán ${formatVND(paymentAmount)} thành công!`);
      setPaymentReference('');
      // Reload settlement data
      await fetchSettlement();
    } catch (err: unknown) {
      console.error('Lỗi ghi nhận thanh toán:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Ghi nhận thanh toán thất bại. Vui lòng kiểm tra lại.';
      setPaymentError(msg);
    } finally {
      setIsSubmittingPayment(false);
    }
  };

  const handleConfirmCheckOut = async () => {
    if (!bookingRoomId || !settlement) return;
    if (settlement.balanceDue > 0) {
      setPaymentError('Chưa thanh toán hết số dư còn nợ. Bắt buộc thanh toán 100% trước khi check-out!');
      return;
    }

    setIsProcessingCheckout(true);
    setPaymentError(null);
    try {
      await bookingService.checkOut(bookingRoomId);
      onCheckOutSuccess(settlement.bookingNumber);
      onClose();
    } catch (err: unknown) {
      console.error('Lỗi khi xác nhận check-out:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Check-out thất bại. Vui lòng thử lại.';
      setPaymentError(msg);
    } finally {
      setIsProcessingCheckout(false);
    }
  };

  const getLineTypeBadge = (lineType: SettlementLineItemDto['lineType']) => {
    switch (lineType) {
      case 'ROOM_RATE':
        return <span className="px-2 py-0.5 text-[11px] font-semibold bg-blue-50 text-blue-700 rounded-md">Tiền phòng</span>;
      case 'SURCHARGE':
        return <span className="px-2 py-0.5 text-[11px] font-semibold bg-amber-50 text-amber-700 rounded-md">Phụ thu</span>;
      case 'PENALTY':
        return <span className="px-2 py-0.5 text-[11px] font-semibold bg-red-50 text-red-700 rounded-md">Phạt phí</span>;
      case 'SERVICE':
        return <span className="px-2 py-0.5 text-[11px] font-semibold bg-emerald-50 text-emerald-700 rounded-md">Dịch vụ phòng</span>;
      case 'PRODUCT':
        return <span className="px-2 py-0.5 text-[11px] font-semibold bg-purple-50 text-purple-700 rounded-md">Sản phẩm/POS</span>;
      default:
        return <span className="px-2 py-0.5 text-[11px] font-semibold bg-neutral-100 text-neutral-600 rounded-md">{lineType}</span>;
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Quyết toán & Đóng sổ Check-out"
      maxWidth="2xl"
    >
      {isLoading ? (
        <div className="py-12 flex flex-col items-center justify-center space-y-3">
          <div className="w-8 h-8 border-3 border-red-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-neutral-600 font-medium">Đang tập hợp bảng kê chi phí và số dư nợ...</p>
        </div>
      ) : !settlement ? (
        <div className="py-8 text-center text-neutral-500 text-sm">
          Không tìm thấy thông tin quyết toán của phòng đặt này.
        </div>
      ) : (
        <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
          {/* Guest & Room Quick Info Header */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 p-3 bg-neutral-50 rounded-xl border border-neutral-200 text-xs">
            <div>
              <span className="text-neutral-500 flex items-center gap-1">
                <Receipt className="w-3.5 h-3.5 text-neutral-400" /> Mã đơn:
              </span>
              <p className="font-bold text-neutral-900 mt-0.5">{settlement.bookingNumber}</p>
            </div>
            <div>
              <span className="text-neutral-500 flex items-center gap-1">
                <DoorOpen className="w-3.5 h-3.5 text-neutral-400" /> Phòng:
              </span>
              <p className="font-bold text-neutral-900 mt-0.5">
                {settlement.roomNumber} ({settlement.roomTypeName})
              </p>
            </div>
            <div>
              <span className="text-neutral-500 flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-neutral-400" /> Khách lưu trú:
              </span>
              <p className="font-bold text-neutral-900 mt-0.5">{settlement.guestName}</p>
            </div>
            <div>
              <span className="text-neutral-500 flex items-center gap-1">
                <Phone className="w-3.5 h-3.5 text-neutral-400" /> Điện thoại:
              </span>
              <p className="font-bold text-neutral-900 mt-0.5">{settlement.guestPhone || 'Chưa cập nhật'}</p>
            </div>
          </div>

          {/* Messages */}
          {paymentError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl flex items-start gap-2.5 text-xs text-red-800">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <span>{paymentError}</span>
            </div>
          )}
          {paymentSuccessMsg && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-start gap-2.5 text-xs text-emerald-800">
              <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span>{paymentSuccessMsg}</span>
            </div>
          )}

          {/* Financial Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200">
              <p className="text-[11px] font-medium text-neutral-500 uppercase tracking-wider">Tổng chi phí phát sinh</p>
              <p className="text-base font-extrabold text-neutral-900 mt-1">{formatVND(settlement.grandTotal)}</p>
              <p className="text-[10px] text-neutral-400 mt-0.5">
                {settlement.noShowPenaltyAmount && settlement.noShowPenaltyAmount > 0
                  ? `Bao gồm phạt No-show: ${formatVND(settlement.noShowPenaltyAmount)}`
                  : 'Tiền phòng + Phụ thu + Dịch vụ'}
              </p>
            </div>
            <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200">
              <p className="text-[11px] font-medium text-neutral-500 uppercase tracking-wider">Đã thanh toán</p>
              <p className="text-base font-extrabold text-emerald-600 mt-1">{formatVND(settlement.totalPaid)}</p>
              <p className="text-[10px] text-neutral-400 mt-0.5">{settlement.paymentHistory?.length || 0} lần giao dịch</p>
            </div>
            <div
              className={`p-3 rounded-xl border ${
                settlement.balanceDue > 0 ? 'bg-amber-50/80 border-amber-300' : 'bg-emerald-50/80 border-emerald-300'
              }`}
            >
              <p className="text-[11px] font-medium text-neutral-600 uppercase tracking-wider">Số dư còn nợ (Balance Due)</p>
              <p
                className={`text-base font-extrabold mt-1 ${
                  settlement.balanceDue > 0 ? 'text-amber-800' : 'text-emerald-700'
                }`}
              >
                {formatVND(settlement.balanceDue)}
              </p>
              <p className="text-[10px] mt-0.5 font-medium">
                {settlement.balanceDue > 0 ? '⚠️ Cần thanh toán trước khi check-out' : '✅ Đã hoàn tất 100% công nợ'}
              </p>
            </div>
          </div>

          {/* Folio Netting Surplus Refund Alert */}
          {settlement.netOverpaymentAmount && settlement.netOverpaymentAmount > 0 && (
            <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl flex items-start gap-2.5 text-xs text-blue-900">
              <CheckCircle className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-blue-800">
                  Dôi dư tiền cọc sau bù trừ chéo (Folio Netting Surplus):{' '}
                  <span className="font-bold text-blue-900">{formatVND(settlement.netOverpaymentAmount)}</span>
                </p>
                <p className="text-[11px] text-blue-700 mt-0.5">
                  Số tiền thanh toán lớn hơn chi phí lưu trú thực tế (do có phòng No-Show hoặc cọc dư). Hệ thống sẽ tự động tạo yêu cầu hoàn tiền (REFUND PENDING) khi tất cả các phòng hoàn tất check-out.
                </p>
              </div>
            </div>
          )}

          {/* Itemized Line Items Table */}
          <div className="border border-neutral-200 rounded-xl overflow-hidden text-xs">
            <div className="bg-neutral-100 px-3 py-2 font-semibold text-neutral-800 flex items-center justify-between">
              <span>Bảng kê chi tiết từng dòng chi phí ({settlement.lineItems?.length || 0})</span>
              <span className="text-[11px] font-normal text-neutral-500">Đã bao gồm phí DV & VAT</span>
            </div>
            <div className="overflow-x-auto max-h-48 overflow-y-auto">
              <table className="w-full text-left border-collapse">
                <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-500 font-medium">
                  <tr>
                    <th className="py-2 px-3">Loại</th>
                    <th className="py-2 px-3">Khoản mục</th>
                    <th className="py-2 px-2 text-center">SL</th>
                    <th className="py-2 px-3 text-right">Đơn giá</th>
                    <th className="py-2 px-3 text-right">Thành tiền</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100">
                  {settlement.lineItems && settlement.lineItems.length > 0 ? (
                    settlement.lineItems.map((item, idx) => (
                      <tr key={idx} className="hover:bg-neutral-50/60 transition-colors">
                        <td className="py-2 px-3">{getLineTypeBadge(item.lineType)}</td>
                        <td className="py-2 px-3 font-medium text-neutral-900">{item.description}</td>
                        <td className="py-2 px-2 text-center text-neutral-600">{item.quantity}</td>
                        <td className="py-2 px-3 text-right text-neutral-600">{formatVND(item.unitPrice)}</td>
                        <td className="py-2 px-3 text-right font-bold text-neutral-900">{formatVND(item.totalAmount)}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={5} className="py-4 text-center text-neutral-400">
                        Không có khoản chi phí nào ghi nhận
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Payment History Section (if any) */}
          {settlement.paymentHistory && settlement.paymentHistory.length > 0 && (
            <div className="border border-neutral-200 rounded-xl overflow-hidden text-xs">
              <div className="bg-neutral-100 px-3 py-2 font-semibold text-neutral-800">
                Lịch sử thanh toán đã ghi nhận ({settlement.paymentHistory.length})
              </div>
              <div className="divide-y divide-neutral-100 max-h-32 overflow-y-auto">
                {settlement.paymentHistory.map((p, idx) => (
                  <div key={idx} className="p-2.5 flex items-center justify-between text-xs hover:bg-neutral-50">
                    <div className="space-y-0.5">
                      <p className="font-semibold text-neutral-800">
                        {p.paymentMethod} • {p.paymentPurpose || 'THANH TOÁN'}
                      </p>
                      <p className="text-[11px] text-neutral-400">
                        Mã GD: {p.transactionReference || p.id} • {p.paidAt ? new Date(p.paidAt).toLocaleString('vi-VN') : 'Thành công'}
                      </p>
                    </div>
                    <span className="font-extrabold text-emerald-600">{formatVND(p.totalAmount)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Inline Payment Box (If balanceDue > 0) */}
          {settlement.balanceDue > 0 ? (
            <div className="p-4 bg-amber-50/60 border border-amber-200 rounded-xl space-y-3">
              <div className="flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-amber-700" />
                <h4 className="text-xs font-bold text-amber-900 uppercase tracking-wide">
                  Thu tiền thanh toán số dư còn nợ
                </h4>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                <div>
                  <label className="block text-[11px] font-semibold text-neutral-700 mb-1">
                    Số tiền thanh toán (VNĐ) <span className="text-red-600">*</span>
                  </label>
                  <Input
                    type="number"
                    min="1000"
                    max={settlement.balanceDue}
                    value={paymentAmount || ''}
                    onChange={(e) => setPaymentAmount(Number(e.target.value))}
                    placeholder="Nhập số tiền"
                  />
                  <div className="flex gap-1.5 mt-1">
                    <button
                      type="button"
                      onClick={() => setPaymentAmount(settlement.balanceDue)}
                      className="text-[10px] text-blue-600 hover:underline font-semibold"
                    >
                      Toàn bộ nợ ({formatVND(settlement.balanceDue)})
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-neutral-700 mb-1">
                    Phương thức thanh toán <span className="text-red-600">*</span>
                  </label>
                  <select
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
                    className="w-full h-10 px-3 text-xs bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-none"
                  >
                    <option value="CASH">Tiền mặt tại quầy (CASH)</option>
                    <option value="CREDIT_CARD">Thẻ POS quầy (CREDIT_CARD)</option>
                    <option value="BANK_TRANSFER">Chuyển khoản NH (BANK_TRANSFER)</option>
                    <option value="VNPAY">Quét mã VNPay QR</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-neutral-700 mb-1">
                    Mã tham chiếu / Biên lai
                  </label>
                  <Input
                    value={paymentReference}
                    onChange={(e) => setPaymentReference(e.target.value)}
                    placeholder="VD: POS-9988, UNC-01..."
                  />
                </div>
              </div>

              <div className="flex justify-end pt-1">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleMakePayment}
                  disabled={isSubmittingPayment || paymentAmount <= 0}
                  className="bg-neutral-900 hover:bg-black text-white"
                >
                  <DollarSign className="w-3.5 h-3.5 mr-1" />
                  {isSubmittingPayment ? 'Đang ghi nhận...' : `Ghi nhận thu tiền (${formatVND(paymentAmount)})`}
                </Button>
              </div>
            </div>
          ) : (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-2.5 text-xs text-emerald-800">
              <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0" />
              <div>
                <p className="font-bold">Đủ điều kiện Check-out và Đóng sổ</p>
                <p className="text-[11px] text-emerald-700">
                  Toàn bộ công nợ đã được tất toán (0 đ). Nhấn nút bên dưới để đóng sổ phòng sang trạng thái <strong>DIRTY</strong> (Chờ dọn dẹp) và quyết toán hóa đơn.
                </p>
              </div>
            </div>
          )}

          {/* Action Footer */}
          <div className="pt-3 border-t border-neutral-200 flex flex-col sm:flex-row items-center justify-between gap-2">
            <div className="text-xs text-neutral-500">
              {settlement.balanceDue > 0 ? (
                <span className="text-amber-700 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" /> Nút Check-out bị khóa do còn số dư nợ {formatVND(settlement.balanceDue)}
                </span>
              ) : (
                <span className="text-emerald-700 font-semibold flex items-center gap-1">
                  <CheckCircle className="w-3.5 h-3.5" /> Sẵn sàng hoàn tất thủ tục
                </span>
              )}
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
              <Button
                variant="outline"
                size="sm"
                onClick={onClose}
                disabled={isProcessingCheckout}
              >
                Đóng / Thoát
              </Button>

              <Button
                variant="primary"
                size="sm"
                onClick={handleConfirmCheckOut}
                disabled={settlement.balanceDue > 0 || isProcessingCheckout}
                className="bg-red-600 hover:bg-red-700 text-white font-bold"
              >
                <FileCheck className="w-4 h-4 mr-1.5" />
                {isProcessingCheckout ? 'Đang đóng sổ...' : 'Xác nhận Check-out & Đóng sổ'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </Modal>
  );
};

export default CheckoutSettlementModal;
