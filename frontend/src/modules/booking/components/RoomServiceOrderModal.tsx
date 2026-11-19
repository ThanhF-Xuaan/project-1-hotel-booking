import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import {
  Utensils,
  Search,
  ShoppingCart,
  Plus,
  Minus,
  Trash2,
  Banknote,
  Building,
  AlertCircle,
  CheckCircle,
  Coffee,
  Sparkles,
  Layers,
  ExternalLink,
  CreditCard,
  Loader2,
  Copy,
  Check,
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import { operationService } from '../../operation/services/operation.service';
import { financeService } from '../../finance/services/finance.service';
import { VNPAY_ATM_TEST_CARD } from '../../finance/utils/vnpay.utils';
import type {
  MenuResponse,
  ServiceOrderPaymentMethod,
} from '../../operation/types/operation.types';

export interface RoomServiceOrderModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingId: number;
  bookingNumber?: string;
  bookingRoomId?: number;
  roomInstanceId: number;
  roomNumber?: string;
  hotelId: number;
  onOrderSuccess?: (msg?: string) => void;
}

type MenuCategoryTab = 'ALL' | 'DRINK' | 'FOOD' | 'SERVICE';

export const RoomServiceOrderModal: React.FC<RoomServiceOrderModalProps> = ({
  isOpen,
  onClose,
  bookingId,
  bookingNumber,
  bookingRoomId,
  roomInstanceId,
  roomNumber,
  hotelId,
  onOrderSuccess,
}) => {
  const [menuItems, setMenuItems] = useState<MenuResponse[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [activeTab, setActiveTab] = useState<MenuCategoryTab>('ALL');
  const [isLoadingMenu, setIsLoadingMenu] = useState(false);

  // Cart
  const [cart, setCart] = useState<{ item: MenuResponse; quantity: number }[]>([]);
  const [paymentMethod, setPaymentMethod] = useState<ServiceOrderPaymentMethod>('POST_TO_ROOM');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // VNPay Sandbox Waiting & Sync State
  const [isWaitingVnPay, setIsWaitingVnPay] = useState(false);
  const [vnpayPaymentId, setVnpayPaymentId] = useState<number | null>(null);
  const [vnpayPaymentUrl, setVnpayPaymentUrl] = useState<string | null>(null);
  const [vnpayOrderNumber, setVnpayOrderNumber] = useState<string | null>(null);
  const [vnpayCopiedField, setVnpayCopiedField] = useState<string | null>(null);

  const vnpayPollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const vnpayChannelRef = useRef<BroadcastChannel | null>(null);

  // Cleanup on unmount or when modal closes
  useEffect(() => {
    return () => {
      if (vnpayPollRef.current) {
        clearInterval(vnpayPollRef.current);
        vnpayPollRef.current = null;
      }
      if (vnpayChannelRef.current) {
        vnpayChannelRef.current.close();
        vnpayChannelRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!isOpen) {
      setIsWaitingVnPay(false);
      setVnpayPaymentId(null);
      setVnpayPaymentUrl(null);
      setVnpayOrderNumber(null);
      if (vnpayPollRef.current) {
        clearInterval(vnpayPollRef.current);
        vnpayPollRef.current = null;
      }
      if (vnpayChannelRef.current) {
        vnpayChannelRef.current.close();
        vnpayChannelRef.current = null;
      }
    }
  }, [isOpen]);

  const fetchMenuItems = useCallback(async () => {
    if (!isOpen) return;
    setIsLoadingMenu(true);
    try {
      const res = await operationService.getMenus({
        hotelId: hotelId || undefined,
        name: searchKeyword || undefined,
        status: 'ACTIVE',
        pageSize: 100,
      });
      setMenuItems(res.content || []);
    } catch (err) {
      console.error('Lỗi tải danh mục dịch vụ/món ăn:', err);
    } finally {
      setIsLoadingMenu(false);
    }
  }, [isOpen, hotelId, searchKeyword]);

  useEffect(() => {
    if (!isOpen) return;
    let ignore = false;
    const initModal = async () => {
      await Promise.resolve();
      if (!ignore) {
        fetchMenuItems();
        setCart([]);
        setPaymentMethod('POST_TO_ROOM');
        setErrorMessage('');
        setSuccessMessage('');
      }
    };
    initModal();
    return () => {
      ignore = true;
    };
  }, [isOpen, fetchMenuItems]);

  // Client-side category filtering
  const filteredMenuItems = useMemo(() => {
    return menuItems.filter((item) => {
      if (activeTab === 'ALL') return true;
      if (activeTab === 'SERVICE') {
        return item.menuType === 'SERVICE';
      }
      if (activeTab === 'DRINK') {
        if (item.menuType !== 'PRODUCT') return false;
        const code = (item.taxCategoryName || '').toLowerCase();
        const name = item.name.toLowerCase();
        return (
          code.includes('drink') ||
          code.includes('beverage') ||
          name.includes('nước') ||
          name.includes('bia') ||
          name.includes('rượu') ||
          name.includes('coca') ||
          name.includes('cola') ||
          name.includes('lavie') ||
          name.includes('heineken') ||
          name.includes('trà') ||
          name.includes('coffee') ||
          name.includes('cà phê')
        );
      }
      if (activeTab === 'FOOD') {
        if (item.menuType !== 'PRODUCT' && item.menuType !== 'SERVICE') return false;
        const code = (item.taxCategoryName || '').toLowerCase();
        const name = item.name.toLowerCase();
        return (
          code.includes('food') ||
          name.includes('buffet') ||
          name.includes('cơm') ||
          name.includes('bánh') ||
          name.includes('phở') ||
          name.includes('bún') ||
          name.includes('mì') ||
          name.includes('snack') ||
          name.includes('đặc sản')
        );
      }
      return true;
    });
  }, [menuItems, activeTab]);

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
  const vatAmount = cart.reduce((sum, c) => {
    const lineSubtotal = c.item.basePrice * c.quantity;
    const lineServiceFee = Math.round(lineSubtotal * 0.05);
    const lineTaxable = lineSubtotal + lineServiceFee;
    const rate = Number(c.item.vatRate ?? 8);
    return sum + Math.round(lineTaxable * (rate / 100));
  }, 0);
  const grandTotal = taxableAmount + vatAmount;

  const vatRates = Array.from(new Set(cart.map((c) => Number(c.item.vatRate ?? 8)))).sort((a, b) => a - b);
  const vatRateLabel =
    vatRates.length === 1
      ? `Thuế VAT (${vatRates[0]}%):`
      : vatRates.length > 1
      ? `Thuế VAT (${vatRates.join('%, ')}%):`
      : 'Thuế GTGT / VAT:';

  const handleVnPayPaymentSuccess = useCallback(
    (orderNum?: string) => {
      if (vnpayPollRef.current) {
        clearInterval(vnpayPollRef.current);
        vnpayPollRef.current = null;
      }
      if (vnpayChannelRef.current) {
        vnpayChannelRef.current.close();
        vnpayChannelRef.current = null;
      }
      try {
        localStorage.removeItem('vnpay_active_session');
        localStorage.removeItem('vnpay_payment_sync_event');
      } catch (e) {}

      setIsWaitingVnPay(false);
      setCart([]);
      onClose();

      const successMsg = `Thanh toán VNPay thành công cho đơn phòng ${
        roomNumber || roomInstanceId
      } (Đơn #${orderNum || vnpayOrderNumber})! Số tiền: ${formatVND(grandTotal)}`;
      if (onOrderSuccess) {
        onOrderSuccess(successMsg);
      }
    },
    [grandTotal, onClose, onOrderSuccess, roomInstanceId, roomNumber, vnpayOrderNumber]
  );

  const startVnPaySync = (paymentId: number, orderNum?: string) => {
    if (vnpayPollRef.current) clearInterval(vnpayPollRef.current);
    if (vnpayChannelRef.current) vnpayChannelRef.current.close();

    // 1. BroadcastChannel đồng bộ liên tab
    try {
      const bc = new BroadcastChannel('vnpay_payment_sync');
      vnpayChannelRef.current = bc;
      bc.onmessage = (event) => {
        if (
          event.data?.type === 'VNPAY_PAYMENT_COMPLETED' &&
          (event.data.status === 'PAID' || event.data.status === 'SUCCESS') &&
          (!event.data.paymentId || event.data.paymentId === paymentId)
        ) {
          handleVnPayPaymentSuccess(orderNum);
        }
      };
    } catch (e) {
      console.warn('BroadcastChannel error:', e);
    }

    // 2. Window message listener
    const handleWindowMessage = (event: MessageEvent) => {
      if (
        event.data?.type === 'VNPAY_PAYMENT_COMPLETED' &&
        (event.data.status === 'PAID' || event.data.status === 'SUCCESS') &&
        (!event.data.paymentId || event.data.paymentId === paymentId)
      ) {
        window.removeEventListener('message', handleWindowMessage);
        handleVnPayPaymentSuccess(orderNum);
      }
    };
    window.addEventListener('message', handleWindowMessage);

    // 3. Polling dự phòng mỗi 2 giây
    vnpayPollRef.current = setInterval(async () => {
      try {
        const pRes = await financeService.getPaymentById(paymentId);
        if (pRes.result && pRes.result.status === 'SUCCESS') {
          window.removeEventListener('message', handleWindowMessage);
          handleVnPayPaymentSuccess(orderNum);
        } else if (
          pRes.result &&
          (pRes.result.status === 'FAILED' || pRes.result.status === 'CANCELLED')
        ) {
          if (vnpayPollRef.current) {
            clearInterval(vnpayPollRef.current);
            vnpayPollRef.current = null;
          }
          window.removeEventListener('message', handleWindowMessage);
          setIsWaitingVnPay(false);
          setErrorMessage(
            pRes.result.gatewayResponseDescription ||
              'Giao dịch VNPay Sandbox không thành công hoặc đã bị hủy.'
          );
        }
      } catch (err) {
        // Tiếp tục poll ở chu kỳ sau
      }
    }, 2000);
  };

  const handleCancelVnPayWaiting = async () => {
    if (vnpayPaymentId) {
      try {
        await financeService.cancelPayment(vnpayPaymentId);
      } catch (e) {
        console.warn('Cancel payment error:', e);
      }
    }
    if (vnpayPollRef.current) {
      clearInterval(vnpayPollRef.current);
      vnpayPollRef.current = null;
    }
    if (vnpayChannelRef.current) {
      vnpayChannelRef.current.close();
      vnpayChannelRef.current = null;
    }
    try {
      localStorage.removeItem('vnpay_active_session');
    } catch {}
    setIsWaitingVnPay(false);
    setErrorMessage('Đã hủy phiên thanh toán VNPay Sandbox. Bạn có thể chọn phương thức khác.');
  };

  const handleReopenVnPayTab = () => {
    if (vnpayPaymentUrl) {
      window.open(vnpayPaymentUrl, '_blank');
    }
  };

  const handleCheckVnPayStatusNow = async () => {
    if (!vnpayPaymentId) return;
    try {
      const pRes = await financeService.getPaymentById(vnpayPaymentId);
      if (pRes.result && pRes.result.status === 'SUCCESS') {
        handleVnPayPaymentSuccess(vnpayOrderNumber || undefined);
      } else {
        setErrorMessage(
          `Giao dịch hiện tại đang ở trạng thái: ${pRes.result?.status || 'Đang xử lý'}. Hãy hoàn tất thanh toán trên cổng VNPay.`
        );
      }
    } catch {
      setErrorMessage('Chưa thể lấy thông tin thanh toán từ máy chủ.');
    }
  };

  const handleCopyAllTestCard = () => {
    const text = `Ngân hàng: ${VNPAY_ATM_TEST_CARD.bankName}\nSố thẻ: ${VNPAY_ATM_TEST_CARD.cardNumber}\nTên chủ thẻ: ${VNPAY_ATM_TEST_CARD.cardHolder}\nNgày phát hành: ${VNPAY_ATM_TEST_CARD.issueDate}\nMã OTP: ${VNPAY_ATM_TEST_CARD.otp}`;
    navigator.clipboard.writeText(text);
    setVnpayCopiedField('all');
    setTimeout(() => setVnpayCopiedField(null), 2000);
  };

  const handleCreateOrder = async () => {
    if (!bookingId || !roomInstanceId || cart.length === 0) return;
    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      // 1. Tạo ServiceOrder với paymentStatus: CASH là PAID, còn VNPAY & POST_TO_ROOM là UNPAID
      const res = await operationService.createServiceOrder({
        hotelId,
        referenceType: bookingRoomId ? 'BOOKING_ROOM' : 'BOOKING',
        referenceId: bookingRoomId ?? bookingId,
        bookingId,
        roomInstanceId,
        paymentMethod,
        paymentStatus: paymentMethod === 'CASH' ? 'PAID' : 'UNPAID',
        items: cart.map((c) => ({
          menuId: c.item.id,
          quantity: c.quantity,
        })),
      });

      // 2. Nếu chọn VNPAY, gọi API tạo phiên Sandbox Gateway gắn đích danh serviceOrderId
      if (paymentMethod === 'VNPAY') {
        try {
          const vnpayRes = await financeService.createVnPayPayment({
            bookingId,
            serviceOrderId: res.id,
            method: 'VNPAY',
            bankCode: 'NCB',
            amount: grandTotal,
          });

          if (vnpayRes.result && vnpayRes.result.paymentUrl) {
            const targetUrl = vnpayRes.result.paymentUrl;
            const paymentId = vnpayRes.result.paymentId;

            // 1. Lưu active session để tab kết quả nhận diện
            try {
              localStorage.setItem(
                'vnpay_active_session',
                JSON.stringify({
                  paymentId,
                  serviceOrderId: res.id,
                  type: 'ROOM_SERVICE',
                  time: Date.now(),
                })
              );
            } catch (e) {
              console.warn('Cannot set localStorage', e);
            }

            // 2. Mở tab mới độc lập
            window.open(targetUrl, '_blank');

            // 3. Chuyển modal sang giao diện chờ
            setIsWaitingVnPay(true);
            setVnpayPaymentId(paymentId);
            setVnpayPaymentUrl(targetUrl);
            setVnpayOrderNumber(res.orderNumber);
            setSuccessMessage(
              `Đã tạo đơn dịch vụ #${res.orderNumber}! Cổng thanh toán VNPay Sandbox đã mở tại tab mới.`
            );

            // 4. Bắt đầu lắng nghe và polling đồng bộ
            startVnPaySync(paymentId, res.orderNumber);
            return;
          } else {
            throw new Error('Cổng VNPay không trả về đường dẫn thanh toán hợp lệ');
          }
        } catch (vnpErr: unknown) {
          console.error('Lỗi khởi tạo cổng VNPay Sandbox:', vnpErr);
          const vnpMsg =
            (vnpErr as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            (vnpErr as Error)?.message ||
            'Không thể kết nối cổng VNPay Sandbox';
          setErrorMessage(
            `Đã tạo đơn dịch vụ #${res.orderNumber} thành công nhưng không thể mở VNPay: ${vnpMsg}. Trạng thái đơn: CHƯA THANH TOÁN (UNPAID).`
          );
          if (onOrderSuccess) onOrderSuccess();
          return;
        }
      }

      const methodNote =
        paymentMethod === 'POST_TO_ROOM'
          ? 'Đã ghi nợ vào Hồ sơ phòng (Room Folio) để thanh toán khi trả phòng.'
          : 'Đã thu tiền mặt trực tiếp tại phòng (không tính lại khi check-out).';

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
        title={`Gọi Món & Dịch Vụ: Phòng ${roomNumber || roomInstanceId} (Đơn #${bookingNumber || bookingId})`}
        maxWidth="5xl"
      >
        <div className="space-y-4">
          {/* Status Alerts */}
          {errorMessage && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2.5 rounded-xl text-xs flex items-center gap-2 shadow-2xs">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
              <span className="font-medium">{errorMessage}</span>
            </div>
          )}
          {successMessage && (
            <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-4 py-2.5 rounded-xl text-xs flex items-center gap-2 shadow-2xs">
              <CheckCircle className="w-4 h-4 shrink-0 text-emerald-600" />
              <span className="font-medium">{successMessage}</span>
            </div>
          )}

          {/* Room Header Strip */}
          <div className="flex flex-wrap items-center justify-between gap-3 bg-neutral-50 border border-neutral-200/90 rounded-2xl px-4 py-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-red-600 text-white flex items-center justify-center font-black text-sm shadow-xs">
                {roomNumber || 'RM'}
              </div>
              <div>
                <div className="text-xs font-bold text-neutral-900 flex items-center gap-2">
                  <span>Phòng {roomNumber || roomInstanceId}</span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200">
                    Đang ở (In-House)
                  </span>
                  {bookingRoomId && (
                    <span className="text-[10px] text-neutral-400 font-mono">
                      (Room ID: #{bookingRoomId})
                    </span>
                  )}
                </div>
                <div className="text-[11px] text-neutral-500 font-medium">
                  Hạch toán: <b className="text-neutral-800 font-semibold">BOOKING_ROOM</b> • Khách sạn: Viettel Boutique Sapa
                </div>
              </div>
            </div>

            <div className="text-right">
              <div className="text-[10px] uppercase font-bold text-neutral-400">Đơn đặt phòng</div>
              <div className="text-xs font-mono font-bold text-neutral-800">{bookingNumber || `#${bookingId}`}</div>
            </div>
          </div>

          {isWaitingVnPay ? (
            <div className="py-6 px-4 space-y-6">
              <div className="text-center space-y-3">
                <div className="w-16 h-16 rounded-full bg-blue-50 text-blue-600 mx-auto flex items-center justify-center animate-pulse shadow-inner ring-8 ring-blue-50/50">
                  <Loader2 className="w-8 h-8 animate-spin" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-neutral-900">
                    Đang Chờ Thanh Toán Qua VNPay Sandbox...
                  </h3>
                  <p className="text-xs text-neutral-500 mt-1 max-w-md mx-auto">
                    Cổng thanh toán VNPay đã được mở trong tab mới. Sau khi khách hàng hoặc nhân viên hoàn tất thanh toán thẻ demo, hệ thống sẽ tự động đồng bộ và đóng cửa sổ này.
                  </p>
                </div>
              </div>

              {/* Chi tiết đơn */}
              <div className="bg-neutral-50 border border-neutral-200/80 rounded-2xl p-4 max-w-lg mx-auto space-y-2.5 text-xs">
                <div className="flex justify-between items-center text-neutral-600">
                  <span>Đơn dịch vụ phòng:</span>
                  <span className="font-bold text-neutral-900">#{vnpayOrderNumber}</span>
                </div>
                <div className="flex justify-between items-center text-neutral-600">
                  <span>Phòng nhận món:</span>
                  <span className="font-bold text-neutral-900">Phòng {roomNumber || roomInstanceId}</span>
                </div>
                <div className="flex justify-between items-center text-neutral-600">
                  <span>Phương thức thanh toán:</span>
                  <span className="font-semibold text-blue-600">Cổng VNPay Sandbox (NCB)</span>
                </div>
                <div className="border-t border-neutral-200/80 pt-2 flex justify-between items-center text-sm font-black text-neutral-900">
                  <span>Tổng số tiền:</span>
                  <span className="text-red-600 text-base">{formatVND(grandTotal)}</span>
                </div>
              </div>

              {/* Box thông tin thẻ test NCB demo */}
              <div className="bg-amber-50/70 border border-amber-200 rounded-2xl p-4 max-w-lg mx-auto space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-amber-900 flex items-center gap-1.5">
                    <CreditCard className="w-4 h-4 text-amber-700" />
                    Thông tin thẻ ATM kiểm thử NCB Demo
                  </span>
                  <button
                    type="button"
                    onClick={handleCopyAllTestCard}
                    className="text-2xs text-amber-800 hover:text-amber-950 font-bold underline flex items-center gap-1 cursor-pointer"
                  >
                    {vnpayCopiedField === 'all' ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-600" />
                        Đã sao chép!
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        Sao chép thông tin thẻ
                      </>
                    )}
                  </button>
                </div>

                <div className="grid grid-cols-2 gap-2 text-2xs text-neutral-700 font-mono">
                  <div className="bg-white/90 border border-amber-100 rounded-lg p-2">
                    <span className="text-neutral-400 block text-3xs font-sans">SỐ THẺ:</span>
                    <span className="font-bold text-neutral-900 select-all">{VNPAY_ATM_TEST_CARD.cardNumber}</span>
                  </div>
                  <div className="bg-white/90 border border-amber-100 rounded-lg p-2">
                    <span className="text-neutral-400 block text-3xs font-sans">TÊN CHỦ THẺ:</span>
                    <span className="font-bold text-neutral-900 select-all">{VNPAY_ATM_TEST_CARD.cardHolder}</span>
                  </div>
                  <div className="bg-white/90 border border-amber-100 rounded-lg p-2">
                    <span className="text-neutral-400 block text-3xs font-sans">NGÀY PHÁT HÀNH:</span>
                    <span className="font-bold text-neutral-900 select-all">{VNPAY_ATM_TEST_CARD.issueDate}</span>
                  </div>
                  <div className="bg-white/90 border border-amber-100 rounded-lg p-2">
                    <span className="text-neutral-400 block text-3xs font-sans">MÃ OTP:</span>
                    <span className="font-bold text-neutral-900 select-all">{VNPAY_ATM_TEST_CARD.otp}</span>
                  </div>
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex flex-wrap items-center justify-center gap-3 pt-2 max-w-lg mx-auto">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCancelVnPayWaiting}
                  className="rounded-xl text-neutral-600 hover:text-red-600 cursor-pointer"
                >
                  Hủy Phiên Thanh Toán
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleReopenVnPayTab}
                  className="rounded-xl border-blue-200 text-blue-700 hover:bg-blue-50 cursor-pointer"
                >
                  <ExternalLink className="w-4 h-4 mr-1.5" />
                  Mở Lại Tab VNPay
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleCheckVnPayStatusNow}
                  className="rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold cursor-pointer"
                >
                  Kiểm Tra Trạng Thái Ngay
                </Button>
              </div>
            </div>
          ) : (
            /* Main 2-Column Grid */
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            {/* Left column: Menu Explorer (7 cols) */}
            <div className="lg:col-span-7 flex flex-col space-y-3">
              {/* Search & Category Tabs */}
              <div className="space-y-2">
                <div className="relative">
                  <Search className="w-4 h-4 absolute left-3.5 top-3 text-neutral-400" />
                  <input
                    type="text"
                    placeholder="Tìm theo tên món ăn, bia, rượu vang, nước suối, dịch vụ giặt là..."
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                    className="w-full h-10 pl-10 pr-3.5 text-xs bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden transition-all shadow-2xs"
                  />
                </div>

                {/* Category Filter Pills */}
                <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs no-scrollbar">
                  <button
                    type="button"
                    onClick={() => setActiveTab('ALL')}
                    className={`px-3 py-1.5 rounded-lg font-bold text-xs shrink-0 transition cursor-pointer ${
                      activeTab === 'ALL'
                        ? 'bg-neutral-900 text-white shadow-2xs'
                        : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                    }`}
                  >
                    <Layers className="w-3.5 h-3.5 inline mr-1" />
                    Tất cả ({menuItems.length})
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveTab('DRINK')}
                    className={`px-3 py-1.5 rounded-lg font-bold text-xs shrink-0 transition cursor-pointer ${
                      activeTab === 'DRINK'
                        ? 'bg-blue-600 text-white shadow-2xs'
                        : 'bg-blue-50 text-blue-700 hover:bg-blue-100'
                    }`}
                  >
                    <Coffee className="w-3.5 h-3.5 inline mr-1" />
                    Đồ uống & Minibar
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveTab('FOOD')}
                    className={`px-3 py-1.5 rounded-lg font-bold text-xs shrink-0 transition cursor-pointer ${
                      activeTab === 'FOOD'
                        ? 'bg-amber-600 text-white shadow-2xs'
                        : 'bg-amber-50 text-amber-800 hover:bg-amber-100'
                    }`}
                  >
                    <Utensils className="w-3.5 h-3.5 inline mr-1" />
                    Món ăn & Bếp
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveTab('SERVICE')}
                    className={`px-3 py-1.5 rounded-lg font-bold text-xs shrink-0 transition cursor-pointer ${
                      activeTab === 'SERVICE'
                        ? 'bg-purple-600 text-white shadow-2xs'
                        : 'bg-purple-50 text-purple-700 hover:bg-purple-100'
                    }`}
                  >
                    <Sparkles className="w-3.5 h-3.5 inline mr-1" />
                    Dịch vụ & Spa
                  </button>
                </div>
              </div>

              {/* Menu List Container */}
              <div className="h-[380px] overflow-y-auto space-y-2 pr-1.5 border border-neutral-200 rounded-2xl p-2.5 bg-neutral-50/40">
                {isLoadingMenu ? (
                  <div className="py-16 text-center text-xs text-neutral-400 flex flex-col items-center justify-center gap-2">
                    <div className="w-6 h-6 border-2 border-red-600 border-t-transparent rounded-full animate-spin" />
                    <span>Đang tải thực đơn và dịch vụ...</span>
                  </div>
                ) : filteredMenuItems.length === 0 ? (
                  <div className="py-16 text-center text-xs text-neutral-400">
                    Không tìm thấy món hoặc dịch vụ phù hợp
                  </div>
                ) : (
                  filteredMenuItems.map((item) => {
                    const cartItem = cart.find((c) => c.item.id === item.id);
                    const qtyInCart = cartItem ? cartItem.quantity : 0;
                    const isOutOfStock = item.menuType === 'PRODUCT' && (item.stockQuantity ?? 0) <= 0;

                    return (
                      <div
                        key={item.id}
                        className={`p-3 rounded-xl border transition flex items-center justify-between gap-3 ${
                          qtyInCart > 0
                            ? 'bg-red-50/20 border-red-200 shadow-2xs'
                            : 'bg-white border-neutral-200/80 hover:border-neutral-300 shadow-2xs'
                        }`}
                      >
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-xs text-neutral-900 truncate">
                              {item.name}
                            </span>
                            <span
                              className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-tight shrink-0 ${
                                item.menuType === 'PRODUCT'
                                  ? 'bg-blue-50 text-blue-700 border border-blue-200'
                                  : 'bg-purple-50 text-purple-700 border border-purple-200'
                              }`}
                            >
                              {item.menuType === 'PRODUCT' ? 'Món / Đồ' : 'Dịch vụ'}
                            </span>
                            {item.vatRate && (
                              <span className="text-[10px] font-semibold text-neutral-400">
                                (VAT {item.vatRate}%)
                              </span>
                            )}
                          </div>

                          <div className="flex items-center gap-3 text-[11px] text-neutral-500 mt-1">
                            <span className="font-bold text-neutral-900 text-xs">
                              {formatVND(item.basePrice)}
                            </span>
                            {/* Chữa triệt để lỗi (Còn 0) trên dịch vụ */}
                            {item.menuType === 'PRODUCT' ? (
                              (item.stockQuantity ?? 0) > 0 ? (
                                <span className="text-[10px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                                  Còn {item.stockQuantity} {item.unit || 'mặt hàng'}
                                </span>
                              ) : (
                                <span className="text-[10px] font-semibold text-red-600 bg-red-50 px-2 py-0.5 rounded-md border border-red-200">
                                  Hết hàng
                                </span>
                              )
                            ) : (
                              <span className="text-[10px] text-neutral-400">
                                Phục vụ theo lượt yêu cầu
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Quick Stepper on Card */}
                        {qtyInCart > 0 ? (
                          <div className="flex items-center gap-1 bg-white border border-red-200 rounded-xl p-1 shadow-2xs shrink-0">
                            <button
                              type="button"
                              onClick={() => updateQuantity(item.id, -1)}
                              className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-neutral-100 text-neutral-700 transition cursor-pointer"
                            >
                              <Minus className="w-3.5 h-3.5" />
                            </button>
                            <span className="w-6 text-center font-bold text-xs text-neutral-900">
                              {qtyInCart}
                            </span>
                            <button
                              type="button"
                              onClick={() => addToCart(item)}
                              disabled={item.menuType === 'PRODUCT' && qtyInCart >= (item.stockQuantity ?? 0)}
                              className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-neutral-100 text-red-600 disabled:opacity-30 transition cursor-pointer"
                            >
                              <Plus className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        ) : (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => addToCart(item)}
                            disabled={isOutOfStock}
                            className={`shrink-0 h-8 px-3 text-xs font-bold rounded-xl transition ${
                              isOutOfStock
                                ? 'opacity-40 cursor-not-allowed border-neutral-200'
                                : 'text-red-600 border-red-200 hover:bg-red-600 hover:text-white cursor-pointer'
                            }`}
                          >
                            <Plus className="w-3.5 h-3.5 mr-1" />
                            Chọn
                          </Button>
                        )}
                      </div>
                    );
                  })
                )}
              </div>
            </div>

            {/* Right column: Cart & Settlement Sidebar (5 cols) */}
            <div className="lg:col-span-5 bg-white border border-neutral-200 rounded-2xl p-4 flex flex-col justify-between shadow-2xs space-y-4">
              <div>
                <div className="flex items-center justify-between border-b border-neutral-100 pb-2.5 mb-2.5">
                  <span className="font-black text-xs text-neutral-900 flex items-center gap-2">
                    <ShoppingCart className="w-4 h-4 text-red-600" />
                    <span>Đơn Hàng Phòng ({cart.reduce((s, c) => s + c.quantity, 0)})</span>
                  </span>
                  {cart.length > 0 && (
                    <button
                      type="button"
                      onClick={() => setCart([])}
                      className="text-[11px] font-semibold text-neutral-400 hover:text-red-600 transition cursor-pointer"
                    >
                      Xóa tất cả
                    </button>
                  )}
                </div>

                {/* Cart items list */}
                <div className="max-h-[160px] overflow-y-auto space-y-2 pr-1 text-xs">
                  {cart.length === 0 ? (
                    <div className="py-10 text-center text-neutral-400 text-xs flex flex-col items-center justify-center gap-1.5">
                      <ShoppingCart className="w-6 h-6 text-neutral-300 stroke-[1.5]" />
                      <span>Chưa có món nào trong đơn</span>
                      <span className="text-[10px] text-neutral-400">Chọn món ở cột bên trái để thêm vào phòng</span>
                    </div>
                  ) : (
                    cart.map((c) => (
                      <div
                        key={c.item.id}
                        className="flex items-center justify-between py-1.5 border-b border-neutral-100 last:border-b-0"
                      >
                        <div className="min-w-0 pr-2">
                          <div className="font-bold text-neutral-800 truncate text-xs">
                            {c.item.name}
                          </div>
                          <div className="text-[10px] text-neutral-500">
                            {formatVND(c.item.basePrice)} × {c.quantity}
                          </div>
                        </div>

                        <div className="flex items-center gap-1 shrink-0">
                          <button
                            type="button"
                            onClick={() => updateQuantity(c.item.id, -1)}
                            className="w-5 h-5 flex items-center justify-center rounded-md border border-neutral-200 hover:bg-neutral-100 text-neutral-700 cursor-pointer"
                          >
                            <Minus className="w-2.5 h-2.5" />
                          </button>
                          <span className="w-5 text-center font-bold text-xs">
                            {c.quantity}
                          </span>
                          <button
                            type="button"
                            onClick={() => updateQuantity(c.item.id, 1)}
                            className="w-5 h-5 flex items-center justify-center rounded-md border border-neutral-200 hover:bg-neutral-100 text-neutral-700 cursor-pointer"
                          >
                            <Plus className="w-2.5 h-2.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => removeFromCart(c.item.id)}
                            className="w-5 h-5 flex items-center justify-center text-neutral-400 hover:text-red-600 ml-1 cursor-pointer"
                          >
                            <Trash2 className="w-3 h-3" />
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Payment Method Selector (Cải thiện triệt để chống vỡ chữ) */}
              <div className="space-y-2.5 pt-3 border-t border-neutral-100">
                <div className="text-[11px] font-bold text-neutral-700 uppercase tracking-wider">
                  Phương thức thanh toán:
                </div>

                <div className="space-y-1.5">
                  {/* OPTION 1: GHI NỢ PHÒNG (POST_TO_ROOM) */}
                  <label
                    onClick={() => setPaymentMethod('POST_TO_ROOM')}
                    className={`flex items-start gap-3 p-2.5 rounded-xl border transition cursor-pointer select-none ${
                      paymentMethod === 'POST_TO_ROOM'
                        ? 'border-red-600 bg-red-50/30 ring-1 ring-red-600'
                        : 'border-neutral-200 hover:bg-neutral-50'
                    }`}
                  >
                    <div className="mt-0.5">
                      <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                        paymentMethod === 'POST_TO_ROOM' ? 'border-red-600 bg-red-600 text-white' : 'border-neutral-300'
                      }`}>
                        {paymentMethod === 'POST_TO_ROOM' && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <Building className="w-3.5 h-3.5 text-neutral-700" />
                        <span className="text-xs font-bold text-neutral-900">Ghi Nợ Phòng (Room Folio)</span>
                        <span className="text-[9px] px-1.5 py-0.2 rounded font-black uppercase bg-emerald-100 text-emerald-800">
                          Khuyên dùng
                        </span>
                      </div>
                      <div className="text-[10px] text-neutral-500 mt-0.5">
                        Gộp chung vào hóa đơn tiền phòng để quyết toán khi Check-out
                      </div>
                    </div>
                  </label>

                  {/* OPTION 2: TIỀN MẶT (CASH) */}
                  <label
                    onClick={() => setPaymentMethod('CASH')}
                    className={`flex items-start gap-3 p-2.5 rounded-xl border transition cursor-pointer select-none ${
                      paymentMethod === 'CASH'
                        ? 'border-red-600 bg-red-50/30 ring-1 ring-red-600'
                        : 'border-neutral-200 hover:bg-neutral-50'
                    }`}
                  >
                    <div className="mt-0.5">
                      <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                        paymentMethod === 'CASH' ? 'border-red-600 bg-red-600 text-white' : 'border-neutral-300'
                      }`}>
                        {paymentMethod === 'CASH' && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <Banknote className="w-3.5 h-3.5 text-emerald-600" />
                        <span className="text-xs font-bold text-neutral-900">Tiền Mặt Tại Chỗ</span>
                      </div>
                      <div className="text-[10px] text-neutral-500 mt-0.5">
                        Nhân viên thu tiền mặt trực tiếp khi giao đồ (Đánh dấu đã thanh toán)
                      </div>
                    </div>
                  </label>

                  {/* OPTION 3: VNPAY SANDBOX */}
                  <label
                    onClick={() => setPaymentMethod('VNPAY')}
                    className={`flex items-start gap-3 p-2.5 rounded-xl border transition cursor-pointer select-none ${
                      paymentMethod === 'VNPAY'
                        ? 'border-red-600 bg-red-50/30 ring-1 ring-red-600'
                        : 'border-neutral-200 hover:bg-neutral-50'
                    }`}
                  >
                    <div className="mt-0.5">
                      <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                        paymentMethod === 'VNPAY' ? 'border-red-600 bg-red-600 text-white' : 'border-neutral-300'
                      }`}>
                        {paymentMethod === 'VNPAY' && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <CreditCard className="w-3.5 h-3.5 text-blue-600" />
                        <span className="text-xs font-bold text-neutral-900">Cổng VN-Pay Sandbox</span>
                        <span className="text-[9px] px-1.5 py-0.2 rounded font-black uppercase bg-blue-100 text-blue-800">
                          Gateway
                        </span>
                      </div>
                      <div className="text-[10px] text-neutral-500 mt-0.5">
                        Chuyển hướng cổng VNPay Sandbox (Thẻ ATM ngân hàng NCB demo)
                      </div>
                    </div>
                  </label>
                </div>

                {/* Price Breakdown */}
                <div className="bg-neutral-50 rounded-xl p-3 space-y-1.5 text-xs border border-neutral-200/80">
                  <div className="flex justify-between text-neutral-600 text-[11px]">
                    <span>Tiền hàng gốc:</span>
                    <span className="font-semibold text-neutral-800">{formatVND(subTotal)}</span>
                  </div>
                  <div className="flex justify-between text-neutral-600 text-[11px]">
                    <span>Phí dịch vụ khách sạn (5%):</span>
                    <span className="font-semibold text-neutral-800">{formatVND(serviceFeeAmount)}</span>
                  </div>
                  <div className="flex justify-between text-neutral-600 text-[11px]">
                    <span>{vatRateLabel}</span>
                    <span className="font-semibold text-neutral-800">{formatVND(vatAmount)}</span>
                  </div>
                  <div className="flex justify-between font-bold text-neutral-900 border-t border-neutral-200 pt-2 text-xs">
                    <span>Tổng thanh toán:</span>
                    <span className="text-red-600 font-black text-sm">{formatVND(grandTotal)}</span>
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
                    className="flex-1 justify-center h-10 font-bold bg-red-600 hover:bg-red-700 text-white rounded-xl cursor-pointer"
                  >
                    {isSubmitting ? (
                      'Đang xử lý...'
                    ) : paymentMethod === 'VNPAY' ? (
                      <>
                        <ExternalLink className="w-4 h-4 mr-1.5" />
                        Tạo Đơn & Mở Cổng VNPay Sandbox
                      </>
                    ) : (
                      <>
                        <Utensils className="w-4 h-4 mr-1.5" />
                        Xác Nhận Tạo Đơn
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
          )}
        </div>
      </Modal>
  );
};
export default RoomServiceOrderModal;
