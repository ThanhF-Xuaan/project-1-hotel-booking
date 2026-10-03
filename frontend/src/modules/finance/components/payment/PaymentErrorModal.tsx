import React from 'react';
import { AlertTriangle } from 'lucide-react';
import Modal from '../../../../core/components/ui/Modal';
import Button from '../../../../core/components/ui/Button';

interface PaymentErrorModalProps {
  isOpen: boolean;
  /** ErrorCode từ BE (8002 / 8004 / 8007 / 8008...) — hiển thị cho khách dễ báo lỗi */
  code?: number;
  message: string;
  /**
   * Nút "Hủy & thanh toán lại" (§6 bước ②③④):
   * page xử lý hủy payment PENDING (nếu có) → reset state → quay về đầu flow.
   */
  onRetry: () => void;
  /** Nút "Đóng" — giữ nguyên dữ liệu đã nhập (case 8007 focus lại ô reference) */
  onClose: () => void;
  /** Disable nút retry trong lúc page đang gọi hủy */
  isRetrying?: boolean;
}

/**
 * Popup lỗi thanh toán theo §6 — dùng lại core Modal.tsx, KHÔNG thêm thư viện toast.
 */
export const PaymentErrorModal: React.FC<PaymentErrorModalProps> = ({
  isOpen,
  code,
  message,
  onRetry,
  onClose,
  isRetrying = false,
}) => {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Thanh toán thất bại" maxWidth="sm">
      <div className="flex flex-col items-center text-center gap-3">
        <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center">
          <AlertTriangle className="w-6 h-6" aria-hidden="true" />
        </div>
        <p className="text-sm text-neutral-600 leading-relaxed">{message}</p>
        {code !== undefined && (
          <p className="text-xs text-neutral-400">
            Mã lỗi: <b className="font-mono">{code}</b>
          </p>
        )}
        <div className="flex flex-col-reverse sm:flex-row items-stretch sm:items-center justify-end gap-3 w-full mt-4 pt-3 border-t border-neutral-100">
          <Button variant="secondary" onClick={onClose} disabled={isRetrying}>
            Đóng
          </Button>
          <Button variant="primary" onClick={onRetry} isLoading={isRetrying}>
            Hủy &amp; thanh toán lại
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default PaymentErrorModal;
