import React, { useState } from 'react';
import { ExternalLink, CreditCard, Copy, Check } from 'lucide-react';
import Modal from '../../../../core/components/ui/Modal';
import Button from '../../../../core/components/ui/Button';
import CountdownBadge from './CountdownBadge';
import { formatCurrency } from '../../../../core/utils/currency';
import type { GatewayMethod, PaymentUrlResponse } from '../../types/finance.types';
import { VNPAY_ATM_TEST_CARD } from '../../utils/vnpay.utils';

interface CheckoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  gateway: GatewayMethod;
  /** Kết quả createVnPayPayment / createMoMoPayment */
  session: PaymentUrlResponse | null;
  /** Số tiền hiển thị (đơn VNĐ) */
  amount?: number;
  /** "Hủy & thanh toán lại" — page hủy payment PENDING → reset → đầu flow */
  onCancelAndRetry: () => void;
  isCancelling?: boolean;
}

const GATEWAY_LABEL: Record<GatewayMethod, string> = {
  VNPAY: 'Thẻ ATM Cổng VNPay',
  MOMO: 'Ví MoMo',
};

/**
 * Modal phiên thanh toán online: countdown theo expiresAt (TTL 10' BE trả về),
 * link ra cổng thanh toán + nút Hủy & thanh toán lại (§6).
 */
export const CheckoutModal: React.FC<CheckoutModalProps> = ({
  isOpen,
  onClose,
  gateway,
  session,
  amount,
  onCancelAndRetry,
  isCancelling = false,
}) => {
  const [copied, setCopied] = useState(false);
  if (!session) return null;
  const label = GATEWAY_LABEL[gateway];

  const handleOpenGateway = () => {
    window.open(session.paymentUrl, '_blank', 'noopener,noreferrer');
  };

  const handleCopyCardInfo = () => {
    const text = `Ngân hàng: ${VNPAY_ATM_TEST_CARD.bankName}\nSố thẻ: ${VNPAY_ATM_TEST_CARD.cardNumber}\nTên chủ thẻ: ${VNPAY_ATM_TEST_CARD.cardHolder}\nNgày phát hành: ${VNPAY_ATM_TEST_CARD.issueDate}\nOTP: ${VNPAY_ATM_TEST_CARD.otp}`;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Thanh toán qua ${label}`}
      description={
        gateway === 'VNPAY'
          ? 'Hoàn tất thanh toán bằng thẻ ATM nội địa NCB trên cổng VNPAY Sandbox'
          : 'Hoàn tất thanh toán trên cổng của ngân hàng / ví điện tử'
      }
      maxWidth="md"
    >
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3 bg-neutral-50 border border-neutral-200 rounded-xl p-4">
          <div>
            <span className="text-xs font-medium text-neutral-500 block">Số tiền thanh toán</span>
            <span className="text-lg font-bold text-red-600">
              {amount !== undefined ? formatCurrency(amount) : '—'}
            </span>
          </div>
          <CountdownBadge expiresAt={session.expiresAt} />
        </div>

        <div className="space-y-1">
          <span className="text-xs font-medium text-neutral-500">Mã giao dịch (txnRef)</span>
          <p className="font-mono text-sm font-semibold text-neutral-900 break-all">
            {session.txnRef}
          </p>
        </div>

        {/* Thông tin thẻ ATM kiểm thử nếu là VNPAY */}
        {gateway === 'VNPAY' && (
          <div className="p-3.5 bg-blue-50/70 border border-blue-200 rounded-xl text-xs space-y-2">
            <div className="flex items-center justify-between text-blue-900 font-bold">
              <span className="flex items-center gap-1.5">
                <CreditCard className="w-4 h-4 text-blue-600" />
                Thông tin thẻ test NCB Sandbox:
              </span>
              <button
                type="button"
                onClick={handleCopyCardInfo}
                className="text-[11px] text-blue-600 hover:underline flex items-center gap-1"
              >
                {copied ? (
                  <span className="text-emerald-600 font-bold flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" />
                    Đã sao chép!
                  </span>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    Sao chép thông tin
                  </>
                )}
              </button>
            </div>
            <div className="grid grid-cols-2 gap-1.5 text-neutral-700 font-medium">
              <div>Ngân hàng: <strong className="text-neutral-900">NCB</strong></div>
              <div>Hạn: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.issueDate}</strong></div>
              <div className="col-span-2">
                Số thẻ: <strong className="font-mono text-neutral-900">{VNPAY_ATM_TEST_CARD.cardNumber}</strong>
              </div>
              <div className="col-span-2">
                Chủ thẻ: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.cardHolder}</strong> • OTP: <strong className="text-emerald-700 font-bold">{VNPAY_ATM_TEST_CARD.otp}</strong>
              </div>
            </div>
          </div>
        )}

        <div className="space-y-1">
          <span className="text-xs font-medium text-neutral-500 flex items-center gap-1">
            <CreditCard className="w-3.5 h-3.5" aria-hidden="true" />
            Link thanh toán {label}
          </span>
          <a
            href={session.paymentUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs text-blue-600 hover:text-blue-800 hover:underline break-all block"
          >
            {session.paymentUrl}
          </a>
        </div>

        <div className="flex flex-col-reverse sm:flex-row items-stretch sm:items-center justify-end gap-3 pt-4 border-t border-neutral-100">
          <Button variant="secondary" onClick={onCancelAndRetry} disabled={isCancelling}>
            Hủy &amp; thanh toán lại
          </Button>
          <Button
            variant="primary"
            onClick={handleOpenGateway}
            rightIcon={<ExternalLink className="w-4 h-4" />}
          >
            {gateway === 'VNPAY' ? 'Mở Cổng VNPAY (Thẻ ATM)' : `Mở trang thanh toán ${label}`}
          </Button>
        </div>

        <p className="text-xs text-neutral-400 text-center">
          Sau khi thanh toán xong trên cổng, quay lại trang này — hệ thống sẽ tự động cập nhật kết quả đối chiếu.
        </p>
      </div>
    </Modal>
  );
};

export default CheckoutModal;

