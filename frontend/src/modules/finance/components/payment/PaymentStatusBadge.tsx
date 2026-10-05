import React from 'react';
import type { PaymentStatus } from '../../types/finance.types';

interface PaymentStatusBadgeProps {
  status: PaymentStatus;
}

const STATUS_STYLES: Record<PaymentStatus, { label: string; className: string }> = {
  SUCCESS: { label: 'Thành công', className: 'bg-emerald-100 text-emerald-800' },
  PENDING: { label: 'Đang chờ', className: 'bg-amber-100 text-amber-800' },
  FAILED: { label: 'Thất bại', className: 'bg-red-100 text-red-800' },
  REFUNDED: { label: 'Đã hoàn tiền', className: 'bg-violet-100 text-violet-800' },
  CANCELLED: { label: 'Đã hủy', className: 'bg-neutral-100 text-neutral-700' },
};

/** Chip trạng thái thanh toán — dùng ở bảng Folio + trang kết quả */
export const PaymentStatusBadge: React.FC<PaymentStatusBadgeProps> = ({ status }) => {
  const style = STATUS_STYLES[status] ?? {
    label: status,
    className: 'bg-neutral-100 text-neutral-700',
  };
  return (
    <span
      className={`inline-block px-2.5 py-1 text-xs font-semibold rounded-full ${style.className}`}
    >
      {style.label}
    </span>
  );
};

export default PaymentStatusBadge;
