import React from 'react';
import { X, Clock, ShieldAlert, CreditCard, Users, AlertCircle } from 'lucide-react';
import type { RoomSurchargePolicyDto } from '../types/portal.types';

export interface RoomPolicyModalProps {
  isOpen: boolean;
  onClose: () => void;
  roomTypeName: string;
  checkInTime?: string;
  checkOutTime?: string;
  surchargePolicies?: RoomSurchargePolicyDto[];
}

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

export const RoomPolicyModal: React.FC<RoomPolicyModalProps> = ({
  isOpen,
  onClose,
  roomTypeName,
  checkInTime = '14:00',
  checkOutTime = '12:00',
  surchargePolicies = [],
}) => {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl overflow-hidden border border-neutral-100 max-h-[85vh] flex flex-col animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-100">
          <h3 className="text-sm sm:text-base font-extrabold text-neutral-900 tracking-tight uppercase">
            {roomTypeName}
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-neutral-100 hover:bg-neutral-200 text-neutral-600 flex items-center justify-center transition-colors cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content body */}
        <div className="p-6 overflow-y-auto space-y-5 text-xs sm:text-sm text-neutral-700">
          {/* 1. Chính sách hoàn hủy */}
          <div className="space-y-1.5">
            <h4 className="font-bold text-neutral-900 flex items-center gap-1.5">
              <ShieldAlert className="w-4 h-4 text-red-600" />
              <span>Chính sách hoàn hủy</span>
            </h4>
            <p className="text-neutral-600 pl-5 leading-relaxed">
              Nếu hủy, thay đổi hoặc không đến nhận phòng (No-show), toàn bộ chi phí đặt phòng sẽ được tính theo quy định của khách sạn. Quý khách vui lòng kiểm tra kỹ lịch trình trước khi xác nhận.
            </p>
          </div>

          {/* 2. Thanh toán */}
          <div className="space-y-1.5">
            <h4 className="font-bold text-neutral-900 flex items-center gap-1.5">
              <CreditCard className="w-4 h-4 text-blue-600" />
              <span>Thanh toán</span>
            </h4>
            <p className="text-neutral-600 pl-5 leading-relaxed">
              Thanh toán trực tuyến 100% qua cổng thanh toán bảo mật VNPAY để kích hoạt trạng thái giữ phòng tức thì trên hệ thống.
            </p>
          </div>

          {/* 3. Giờ nhận & trả phòng */}
          <div className="space-y-2 pt-1 border-t border-neutral-100">
            <h4 className="font-bold text-neutral-900 flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-amber-600" />
              <span>Thời gian lưu trú</span>
            </h4>
            <div className="pl-5 grid grid-cols-2 gap-4">
              <div className="p-2.5 rounded-xl bg-neutral-50 border border-neutral-200">
                <p className="text-[11px] text-neutral-500 font-medium">Nhận phòng (Check-in):</p>
                <p className="text-sm font-bold text-neutral-900">{checkInTime}</p>
              </div>
              <div className="p-2.5 rounded-xl bg-neutral-50 border border-neutral-200">
                <p className="text-[11px] text-neutral-500 font-medium">Trả phòng (Check-out):</p>
                <p className="text-sm font-bold text-neutral-900">{checkOutTime}</p>
              </div>
            </div>
          </div>

          {/* 4. Các loại phụ thu */}
          <div className="space-y-2 pt-1 border-t border-neutral-100">
            <h4 className="font-bold text-neutral-900 flex items-center gap-1.5">
              <Users className="w-4 h-4 text-emerald-600" />
              <span>Chính sách phụ thu phát sinh</span>
            </h4>
            {surchargePolicies.length === 0 ? (
              <p className="text-neutral-500 pl-5 italic text-xs">
                Không có chính sách phụ thu bổ sung áp dụng cho hạng phòng này.
              </p>
            ) : (
              <div className="pl-5 space-y-1.5">
                {surchargePolicies.map((p, idx) => (
                  <div
                    key={idx}
                    className="flex items-center justify-between p-2 rounded-xl bg-neutral-50 border border-neutral-100 text-xs"
                  >
                    <span className="font-medium text-neutral-800">
                      {p.formattedDescription ||
                        (p.ruleType === 'EXTRA_PERSON'
                          ? 'Phụ thu thêm người'
                          : p.ruleType === 'EXTRA_BED'
                          ? 'Phụ thu giường phụ'
                          : p.ruleType)}
                    </span>
                    <span className="font-bold text-neutral-900">
                      {formatCurrency(Number(p.adjustmentValue))} / đêm
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 5. Chính sách khác */}
          <div className="space-y-1.5 pt-1 border-t border-neutral-100">
            <h4 className="font-bold text-neutral-900 flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4 text-neutral-600" />
              <span>Chính sách khác</span>
            </h4>
            <p className="text-neutral-500 pl-5 text-xs leading-relaxed">
              Thông tin đặt phòng sẽ được gửi qua email cho Quý khách. Quý khách vui lòng xuất trình CMND/CCCD hoặc Hộ chiếu gốc trùng khớp với tên người đặt khi làm thủ tục tại quầy lễ tân.
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-neutral-100 bg-neutral-50 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 text-xs font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl transition-colors cursor-pointer shadow-xs"
          >
            Đã hiểu
          </button>
        </div>
      </div>
    </div>
  );
};

export default RoomPolicyModal;

