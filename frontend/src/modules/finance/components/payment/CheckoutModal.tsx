import React from 'react';
import { ExternalLink, QrCode } from 'lucide-react';
import Modal from '../../../../core/components/ui/Modal';
import Button from '../../../../core/components/ui/Button';
import CountdownBadge from './CountdownBadge';
import type { GatewayMethod, PaymentUrlResponse } from '../../types/finance.types';

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
  VNPAY: 'VNPay',
  MOMO: 'MoMo',
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
  if (!session) return null;
  const label = GATEWAY_LABEL[gateway];

  const handleOpenGateway = () => {
    window.open(session.paymentUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Thanh toán qua ${label}`}
      description="Hoàn tất thanh toán trên cổng của ngân hàng / ví điện tử"
      maxWidth="md"
    >
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3 bg-neutral-50 border border-neutral-200 rounded-xl p-4">
          <div>
            <span className="text-xs font-medium text-neutral-500 block">Số tiền thanh toán</span>
            <span className="text-lg font-bold text-red-600">
              {amount !== undefined ? new Intl.NumberFormat('vi-VN').format(amount) : '—'} đ
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

        <div className="space-y-1">
          <span className="text-xs font-medium text-neutral-500 flex items-center gap-1">
            <QrCode className="w-3.5 h-3.5" aria-hidden="true" />
            Link thanh toán {label}
          </span>
          <a
            href={session.paymentUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs text-blue-600 hover:text-blue-800 hover:underline break-all"
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
            Mở trang thanh toán {label}
          </Button>
        </div>

        <p className="text-xs text-neutral-400 text-center">
          Sau khi thanh toán xong, quay lại trang này — trạng thái sẽ tự cập nhật.
        </p>
      </div>
    </Modal>
  );
};

export default CheckoutModal;
