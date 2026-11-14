import React from 'react';
import { Calendar, Building2, Trash2, ArrowRight, ShieldCheck } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import type { SelectedRoomCartItem } from '../types/portal.types';

export interface BookingSummarySidebarProps {
  hotelName: string;
  checkInDate: string;
  checkOutDate: string;
  selectedItems: SelectedRoomCartItem[];
  isSubmitting?: boolean;
  onRemoveRoom: (hotelRoomTypeId: number, roomIndex: number) => void;
  onProceedCheckout: () => void;
}

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

export const BookingSummarySidebar: React.FC<BookingSummarySidebarProps> = ({
  hotelName,
  checkInDate,
  checkOutDate,
  selectedItems,
  isSubmitting,
  onRemoveRoom,
  onProceedCheckout,
}) => {
  const start = new Date(checkInDate).getTime();
  const end = new Date(checkOutDate).getTime();
  const totalNights = Math.max(1, Math.round((end - start) / 86400000));
  const totalDays = totalNights + 1;

  // Tổng tiền toàn bộ các phòng đã chọn
  const grandTotal = selectedItems.reduce((sum, item) => sum + item.estimatedTotal, 0);

  // Tổng số lượng phòng
  const totalRoomsCount = selectedItems.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <div className="bg-white rounded-3xl border border-neutral-200 shadow-sm p-5 space-y-5 sticky top-20">
      {/* Header */}
      <div className="border-b border-neutral-100 pb-4 space-y-1">
        <h3 className="text-base font-extrabold text-neutral-900 tracking-tight">
          Thông tin đặt phòng
        </h3>
        <p className="text-xs font-bold text-red-600 flex items-center gap-1.5 truncate">
          <Building2 className="w-3.5 h-3.5 shrink-0" />
          <span className="truncate">{hotelName}</span>
        </p>
        <p className="text-xs text-neutral-500 flex items-center gap-1.5 pt-0.5">
          <Calendar className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
          <span>
            {checkInDate} - {checkOutDate} ({totalDays} ngày {totalNights} đêm)
          </span>
        </p>
      </div>

      {/* Danh sách phòng đã chọn */}
      <div className="space-y-3">
        <div className="flex items-center justify-between text-xs font-bold text-neutral-700">
          <span>Thông tin phòng</span>
          {totalRoomsCount > 0 && (
            <span className="text-[11px] font-semibold text-neutral-500">
              {totalRoomsCount} phòng đã chọn
            </span>
          )}
        </div>

        {totalRoomsCount === 0 ? (
          <div className="p-4 rounded-2xl bg-neutral-50 border border-dashed border-neutral-200 text-center text-xs text-neutral-400 space-y-1">
            <p className="font-semibold text-neutral-600">Chưa có phòng nào được chọn</p>
            <p className="text-[11px] text-neutral-400">
              Vui lòng bấm &quot;Chọn phòng&quot; ở danh sách bên trái để tiếp tục.
            </p>
          </div>
        ) : (
          <div className="space-y-2.5 max-h-[380px] overflow-y-auto pr-1">
            {selectedItems.map((item) =>
              item.rooms.map((cfg) => {
                const roomNightPrice =
                  item.quantity > 0 ? Math.round(item.estimatedTotal / (totalNights * item.quantity)) : 0;

                return (
                  <div
                    key={`${item.hotelRoomTypeId}-${cfg.roomIndex}`}
                    className="p-3 bg-neutral-50 rounded-2xl border border-neutral-200 text-xs space-y-1.5 relative group hover:border-neutral-300 transition-colors"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <p className="font-bold text-neutral-900 leading-snug truncate">
                          Phòng {cfg.roomIndex}: {item.roomTypeName}
                        </p>
                        <p className="text-[11px] text-neutral-500 truncate">
                          {item.roomTypeCode} - Giá tiêu chuẩn
                        </p>
                      </div>

                      {/* Nút hủy phòng */}
                      <button
                        type="button"
                        onClick={() => onRemoveRoom(item.hotelRoomTypeId, cfg.roomIndex)}
                        className="px-2 py-1 text-[11px] font-bold text-red-600 hover:text-red-700 bg-red-50 hover:bg-red-100 rounded-lg flex items-center gap-1 transition-colors cursor-pointer shrink-0"
                        title="Hủy phòng này"
                      >
                        <Trash2 className="w-3 h-3" />
                        <span>Hủy</span>
                      </button>
                    </div>

                    <div className="text-[11px] text-neutral-600 flex items-center justify-between pt-1 border-t border-neutral-200/60">
                      <span>
                        {cfg.adults} Lớn
                        {cfg.children > 0 && ` - ${cfg.children} Trẻ`}
                        {cfg.infants > 0 && ` - ${cfg.infants} Em bé`}
                      </span>
                      <span className="font-bold text-neutral-900">
                        {formatCurrency(roomNightPrice)} /đêm
                      </span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        )}
      </div>

      {/* Tổng cộng & Nút ĐẶT NGAY */}
      <div className="border-t border-neutral-200 pt-4 space-y-3">
        <div className="flex items-baseline justify-between">
          <span className="text-xs font-semibold text-neutral-600">Tổng cộng</span>
          <div className="text-right">
            <span className="text-xl font-extrabold text-neutral-900">
              {formatCurrency(grandTotal)}
            </span>
            <p className="text-[10px] text-neutral-400">Đã bao gồm thuế VAT & phí dịch vụ</p>
          </div>
        </div>

        <Button
          onClick={onProceedCheckout}
          disabled={totalRoomsCount === 0 || isSubmitting}
          className="w-full h-12 flex items-center justify-center gap-2 font-bold text-sm bg-red-600 hover:bg-red-700 disabled:bg-neutral-200 disabled:text-neutral-400 text-white rounded-xl shadow-xs cursor-pointer transition-all disabled:cursor-not-allowed"
        >
          <span>{isSubmitting ? 'ĐANG GIỮ CHỖ...' : 'ĐẶT NGAY'}</span>
          <ArrowRight className="w-4 h-4" />
        </Button>

        <div className="flex items-center justify-center gap-1.5 text-[11px] text-neutral-400 font-medium pt-1">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
          <span>Cam kết giữ phòng tức thì & bảo mật thanh toán</span>
        </div>
      </div>
    </div>
  );
};

export default BookingSummarySidebar;

