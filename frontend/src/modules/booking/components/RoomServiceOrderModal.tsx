import React, { useState, useEffect, useCallback } from 'react';
import {
  Utensils,
  Search,
  ShoppingCart,
  Plus,
  Minus,
  Trash2,
  Banknote,
  Building,
  QrCode,
  AlertCircle,
  CheckCircle,
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import { operationService } from '../../operation/services/operation.service';
import type {
  MenuResponse,
  MenuType,
  ServiceOrderPaymentMethod,
} from '../../operation/types/operation.types';

export interface RoomServiceOrderModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingId: number;
  bookingNumber?: string;
  roomInstanceId: number;
  roomNumber?: string;
  hotelId: number;
  onOrderSuccess?: () => void;
}

export const RoomServiceOrderModal: React.FC<RoomServiceOrderModalProps> = ({
  isOpen,
  onClose,
  bookingId,
  bookingNumber,
  roomInstanceId,
  roomNumber,
  hotelId,
  onOrderSuccess,
}) => {
  const [menuItems, setMenuItems] = useState<MenuResponse[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | MenuType>('ALL');
  const [isLoadingMenu, setIsLoadingMenu] = useState(false);

  // Cart
  const [cart, setCart] = useState<{ item: MenuResponse; quantity: number }[]>([]);
  const [paymentMethod, setPaymentMethod] = useState<ServiceOrderPaymentMethod>('POST_TO_ROOM');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const fetchMenuItems = useCallback(async () => {
    if (!isOpen) return;
    setIsLoadingMenu(true);
    try {
      const res = await operationService.getMenus({
        hotelId: hotelId || undefined,
        name: searchKeyword || undefined,
        menuType: categoryFilter === 'ALL' ? undefined : categoryFilter,
        status: 'ACTIVE',
        pageSize: 100,
      });
      setMenuItems(res.content || []);
    } catch (err) {
      console.error('Lỗi tải danh mục dịch vụ/món ăn:', err);
    } finally {
      setIsLoadingMenu(false);
    }
  }, [isOpen, hotelId, searchKeyword, categoryFilter]);

  useEffect(() => {
    if (isOpen) {
      fetchMenuItems();
      setCart([]);
      setPaymentMethod('POST_TO_ROOM');
      setErrorMessage('');
      setSuccessMessage('');
    }
  }, [isOpen, fetchMenuItems]);

  const addToCart = (item: MenuResponse) => {
    setCart((prev) => {
      const existing = prev.find((c) => c.item.id === item.id);
      if (existing) {
        return prev.map((c) =>
          c.item.id === item.id ? { ...c, quantity: c.quantity + 1 } : c
        );
      }
      return [...prev, { item, quantity: 1 }];
    });
  };

  const updateQuantity = (itemId: number, delta: number) => {
    setCart((prev) =>
      prev
        .map((c) => {
          if (c.item.id === itemId) {
            const newQty = c.quantity + delta;
            return newQty > 0 ? { ...c, quantity: newQty } : null;
          }
          return c;
        })
        .filter(Boolean) as { item: MenuResponse; quantity: number }[]
    );
  };

  const removeFromCart = (itemId: number) => {
    setCart((prev) => prev.filter((c) => c.item.id !== itemId));
  };

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

  // Financial calculations
  const subTotal = cart.reduce((sum, c) => sum + c.item.basePrice * c.quantity, 0);
  const serviceFeeAmount = Math.round(subTotal * 0.05);
  const taxableAmount = subTotal + serviceFeeAmount;
  const vatAmount = Math.round(taxableAmount * 0.08);
  const grandTotal = taxableAmount + vatAmount;

  const handleCreateOrder = async () => {
    if (!bookingId || !roomInstanceId || cart.length === 0) return;
    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const res = await operationService.createServiceOrder({
        bookingId,
        roomInstanceId,
        paymentMethod,
        items: cart.map((c) => ({
          menuId: c.item.id,
          quantity: c.quantity,
        })),
      });

      const methodNote =
        paymentMethod === 'POST_TO_ROOM'
          ? 'Đã ghi nợ vào hóa đơn phòng (Folio) để thanh toán khi trả phòng.'
          : 'Đã thanh toán ngay qua ' +
            (paymentMethod === 'CASH' ? 'Tiền mặt' : 'VN-Pay QR') +
            ' (không tính lại khi check-out).';

      setSuccessMessage(`Đã tạo đơn dịch vụ #${res.orderNumber} thành công! ${methodNote}`);
      setCart([]);

      if (onOrderSuccess) {
        onOrderSuccess();
      }

      setTimeout(() => {
        onClose();
      }, 1500);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Có lỗi xảy ra khi tạo đơn dịch vụ phòng';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Gọi món & Dịch vụ phòng: Phòng ${roomNumber || roomInstanceId} (Đơn #${bookingNumber || bookingId})`}
      maxWidth="2xl"
    >
      <div className="space-y-4">
        {/* Alerts */}
        {errorMessage && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-xl text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}
        {successMessage && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-3 py-2 rounded-xl text-xs flex items-center gap-2">
            <CheckCircle className="w-4 h-4 shrink-0 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
          {/* Left column: Menu Selector (7 cols) */}
          <div className="lg:col-span-7 space-y-3">
            {/* Filters */}
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="w-4 h-4 absolute left-3 top-3 text-neutral-400" />
                <input
                  type="text"
                  placeholder="Tìm món ăn, đồ uống, minibar, giặt là..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  className="w-full h-10 pl-9 pr-3 text-xs bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                />
              </div>

              <select
                className="h-10 px-3 text-xs bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value as 'ALL' | MenuType)}
              >
                <option value="ALL">Tất cả</option>
                <option value="PRODUCT">Sản phẩm / Món ăn</option>
                <option value="SERVICE">Dịch vụ</option>
              </select>
            </div>

            {/* Menu List */}
            <div className="max-h-[360px] overflow-y-auto space-y-2 pr-1 border border-neutral-200 rounded-xl p-2 bg-neutral-50/50">
              {isLoadingMenu ? (
                <div className="py-12 text-center text-xs text-neutral-500">Đang tải danh mục...</div>
              ) : menuItems.length === 0 ? (
                <div className="py-12 text-center text-xs text-neutral-400">Không tìm thấy món ăn/dịch vụ nào</div>
              ) : (
                menuItems.map((item) => (
                  <div
                    key={item.id}
                    className="bg-white p-2.5 rounded-lg border border-neutral-200 hover:border-neutral-300 flex items-center justify-between gap-3 transition shadow-2xs"
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="font-semibold text-xs text-neutral-900 truncate">
                          {item.name}
                        </span>
                        <span
                          className={`text-[10px] px-1.5 py-0.2 rounded font-medium ${
                            item.menuType === 'PRODUCT'
                              ? 'bg-blue-50 text-blue-700'
                              : 'bg-purple-50 text-purple-700'
                          }`}
                        >
                          {item.menuType === 'PRODUCT' ? 'Món/Đồ' : 'Dịch vụ'}
                        </span>
                      </div>
                      <div className="text-[11px] text-neutral-500 mt-0.5">
                        {formatVND(item.basePrice)}
                        {item.stockQuantity !== undefined && (
                          <span className="ml-2 text-neutral-400">
                            (Còn {item.stockQuantity})
                          </span>
                        )}
                      </div>
                    </div>

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => addToCart(item)}
                      className="shrink-0 h-8 px-2.5 text-xs text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700"
                    >
                      <Plus className="w-3.5 h-3.5 mr-1" />
                      Chọn
                    </Button>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Right column: Cart & Payment method (5 cols) */}
          <div className="lg:col-span-5 bg-white border border-neutral-200 rounded-xl p-3.5 flex flex-col justify-between space-y-3">
            <div>
              <div className="flex items-center justify-between border-b border-neutral-200 pb-2 mb-2">
                <span className="font-bold text-xs text-neutral-900 flex items-center gap-1.5">
                  <ShoppingCart className="w-4 h-4 text-red-600" />
                  Đơn hàng phòng ({cart.reduce((s, c) => s + c.quantity, 0)})
                </span>
                {cart.length > 0 && (
                  <button
                    onClick={() => setCart([])}
                    className="text-[11px] text-neutral-400 hover:text-red-600 transition"
                  >
                    Xóa tất cả
                  </button>
                )}
              </div>

              {/* Cart items list */}
              <div className="max-h-[160px] overflow-y-auto space-y-1.5 pr-1 text-xs">
                {cart.length === 0 ? (
                  <div className="py-8 text-center text-neutral-400 text-xs">
                    Chưa có món nào trong đơn
                  </div>
                ) : (
                  cart.map((c) => (
                    <div
                      key={c.item.id}
                      className="flex items-center justify-between py-1 border-b border-neutral-100"
                    >
                      <div className="min-w-0 pr-2">
                        <div className="font-medium text-neutral-800 truncate text-xs">
                          {c.item.name}
                        </div>
                        <div className="text-[10px] text-neutral-500">
                          {formatVND(c.item.basePrice)}
                        </div>
                      </div>

                      <div className="flex items-center gap-1 shrink-0">
                        <button
                          onClick={() => updateQuantity(c.item.id, -1)}
                          className="w-5 h-5 flex items-center justify-center rounded border border-neutral-300 hover:bg-neutral-100"
                        >
                          <Minus className="w-3 h-3 text-neutral-600" />
                        </button>
                        <span className="w-5 text-center font-semibold text-xs">
                          {c.quantity}
                        </span>
                        <button
                          onClick={() => updateQuantity(c.item.id, 1)}
                          className="w-5 h-5 flex items-center justify-center rounded border border-neutral-300 hover:bg-neutral-100"
                        >
                          <Plus className="w-3 h-3 text-neutral-600" />
                        </button>
                        <button
                          onClick={() => removeFromCart(c.item.id)}
                          className="w-5 h-5 flex items-center justify-center text-neutral-400 hover:text-red-600 ml-1"
                        >
                          <Trash2 className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Payment Method Selector */}
            <div className="space-y-2 pt-2 border-t border-neutral-200">
              <label className="block text-[11px] font-semibold text-neutral-700">
                Phương thức thanh toán POS:
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-1.5 text-xs">
                <button
                  type="button"
                  onClick={() => setPaymentMethod('POST_TO_ROOM')}
                  className={`p-2 rounded-lg border text-left flex items-start gap-1.5 transition ${
                    paymentMethod === 'POST_TO_ROOM'
                      ? 'border-red-600 bg-red-50 text-red-700 font-semibold ring-1 ring-red-600'
                      : 'border-neutral-200 hover:bg-neutral-50 text-neutral-700'
                  }`}
                >
                  <Building className="w-4 h-4 shrink-0 mt-0.5 text-red-600" />
                  <div>
                    <div className="text-xs font-semibold">Ghi nợ phòng</div>
                    <div className="text-[10px] text-neutral-500 font-normal leading-tight">
                      Gộp Folio trả sau
                    </div>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setPaymentMethod('CASH')}
                  className={`p-2 rounded-lg border text-left flex items-start gap-1.5 transition ${
                    paymentMethod === 'CASH'
                      ? 'border-red-600 bg-red-50 text-red-700 font-semibold ring-1 ring-red-600'
                      : 'border-neutral-200 hover:bg-neutral-50 text-neutral-700'
                  }`}
                >
                  <Banknote className="w-4 h-4 shrink-0 mt-0.5 text-emerald-600" />
                  <div>
                    <div className="text-xs font-semibold">Tiền mặt</div>
                    <div className="text-[10px] text-neutral-500 font-normal leading-tight">
                      Thu tại phòng
                    </div>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setPaymentMethod('VNPAY')}
                  className={`p-2 rounded-lg border text-left flex items-start gap-1.5 transition ${
                    paymentMethod === 'VNPAY'
                      ? 'border-red-600 bg-red-50 text-red-700 font-semibold ring-1 ring-red-600'
                      : 'border-neutral-200 hover:bg-neutral-50 text-neutral-700'
                  }`}
                >
                  <QrCode className="w-4 h-4 shrink-0 mt-0.5 text-blue-600" />
                  <div>
                    <div className="text-xs font-semibold">VN-Pay QR</div>
                    <div className="text-[10px] text-neutral-500 font-normal leading-tight">
                      Quét mã ngay
                    </div>
                  </div>
                </button>
              </div>

              {/* Price Breakdown */}
              <div className="bg-neutral-50 rounded-lg p-2 space-y-1 text-xs border border-neutral-100">
                <div className="flex justify-between text-neutral-600 text-[11px]">
                  <span>Tiền hàng:</span>
                  <span>{formatVND(subTotal)}</span>
                </div>
                <div className="flex justify-between text-neutral-600 text-[11px]">
                  <span>Phí phục vụ (5%):</span>
                  <span>{formatVND(serviceFeeAmount)}</span>
                </div>
                <div className="flex justify-between text-neutral-600 text-[11px]">
                  <span>VAT (8%):</span>
                  <span>{formatVND(vatAmount)}</span>
                </div>
                <div className="flex justify-between font-bold text-neutral-900 border-t border-neutral-200 pt-1 text-xs">
                  <span>Tổng thanh toán:</span>
                  <span className="text-red-600">{formatVND(grandTotal)}</span>
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex gap-2 pt-1">
                <Button variant="outline" size="sm" onClick={onClose} disabled={isSubmitting}>
                  Đóng
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleCreateOrder}
                  disabled={cart.length === 0 || isSubmitting}
                  className="flex-1 justify-center"
                >
                  <Utensils className="w-3.5 h-3.5 mr-1" />
                  {isSubmitting ? 'Đang tạo...' : 'Tạo đơn dịch vụ'}
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
};

export default RoomServiceOrderModal;
