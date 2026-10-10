import React, { useState, useEffect, useCallback } from 'react';
import {
  BellRing,
  Search,
  ShoppingCart,
  Plus,
  Minus,
  Trash2,
  CheckCircle2,
  Clock,
  Send,
  Utensils,
  Sparkles,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { Modal, ConfirmModal } from '../../../core/components/ui/Modal';
import { operationService } from '../services/operation.service';
import bookingService from '../../booking/services/booking.service';
import type {
  MenuResponse,
  ServiceOrderResponse,
  ServiceOrderStatus,
} from '../types/operation.types';

export interface BookedRoomOption {
  id: number;
  roomNumber: string;
  hotelName: string;
  roomTypeName?: string;
  bookingId: number;
  bookingNumber: string;
  guestName: string;
  currentStatus: string;
}

export const RoomServiceOrderPage: React.FC = () => {
  // Destination: Chỉ các phòng đã được tạo đơn hàng (đang lưu trú / đã đặt)
  const [roomInstanceId, setRoomInstanceId] = useState<number | ''>('');
  const [roomList, setRoomList] = useState<BookedRoomOption[]>([]);
  const [isLoadingRooms, setIsLoadingRooms] = useState(false);

  // Menu items list
  const [menuItems, setMenuItems] = useState<MenuResponse[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | 'PRODUCT' | 'SERVICE'>('ALL');
  const [isLoadingMenu, setIsLoadingMenu] = useState(false);

  // Cart
  const [cart, setCart] = useState<{ item: MenuResponse; quantity: number }[]>([]);
  const [isPlacingOrder, setIsPlacingOrder] = useState(false);
  const [isConfirmOrderModalOpen, setIsConfirmOrderModalOpen] = useState(false);
  const [orderSuccessMessage, setOrderSuccessMessage] = useState('');
  const [orderErrorMessage, setOrderErrorMessage] = useState('');

  // Cancel order modal
  const [orderToCancel, setOrderToCancel] = useState<ServiceOrderResponse | null>(null);
  const [isCancellingOrder, setIsCancellingOrder] = useState(false);

  // Recent Orders list
  const [recentOrders, setRecentOrders] = useState<ServiceOrderResponse[]>([]);
  const [isLoadingOrders, setIsLoadingOrders] = useState(false);

  const fetchMenuItems = useCallback(async () => {
    setIsLoadingMenu(true);
    try {
      const res = await operationService.getMenus({
        name: searchKeyword || undefined,
        menuType: categoryFilter === 'ALL' ? undefined : categoryFilter,
        status: 'ACTIVE',
        pageSize: 50,
      });
      setMenuItems(res.content || []);
    } catch (err) {
      console.error('Failed to load menu items:', err);
    } finally {
      setIsLoadingMenu(false);
    }
  }, [searchKeyword, categoryFilter]);

  const fetchRecentOrders = useCallback(async () => {
    setIsLoadingOrders(true);
    try {
      const res = await operationService.getServiceOrders({
        pageSize: 10,
      });
      setRecentOrders(res.content || []);
    } catch (err) {
      console.error('Failed to fetch recent orders:', err);
    } finally {
      setIsLoadingOrders(false);
    }
  }, []);

  const fetchRooms = useCallback(async () => {
    setIsLoadingRooms(true);
    try {
      const res = await bookingService.filter({ size: 100 });
      const bookings = res?.result?.content || [];

      // Chỉ lấy các phòng vật lý đã được tạo đơn hàng (đang có booking hợp lệ và đã gán phòng)
      const bookedRoomsMap = new Map<number, BookedRoomOption>();

      for (const b of bookings) {
        if (b.status === 'CANCELLED') continue;

        for (const detail of b.bookingDetails || []) {
          for (const room of detail.bookingRooms || []) {
            if (
              room.roomInstanceId &&
              room.status !== 'CANCELLED' &&
              room.status !== 'CHECKED_OUT'
            ) {
              const existing = bookedRoomsMap.get(room.roomInstanceId);
              if (!existing || room.status === 'CHECKED_IN') {
                bookedRoomsMap.set(room.roomInstanceId, {
                  id: room.roomInstanceId,
                  roomNumber: room.roomNumber || String(room.roomInstanceId),
                  hotelName: b.hotelName,
                  roomTypeName: detail.roomTypeName,
                  bookingId: b.id,
                  bookingNumber: b.bookingNumber,
                  guestName: b.guestName || b.guestPhone,
                  currentStatus: room.status === 'CHECKED_IN' ? 'Đang ở' : 'Đã đặt phòng',
                });
              }
            }
          }
        }
      }

      const activeList = Array.from(bookedRoomsMap.values());
      setRoomList(activeList);
      if (activeList.length > 0) {
        setRoomInstanceId((prev) => (activeList.some((r) => r.id === prev) ? prev : activeList[0].id));
      } else {
        setRoomInstanceId('');
      }
    } catch (err) {
      console.error('Failed to load booked rooms:', err);
    } finally {
      setIsLoadingRooms(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    const init = async () => {
      if (isMounted) {
        await fetchRooms();
        await fetchMenuItems();
        await fetchRecentOrders();
      }
    };
    void init();
    return () => {
      isMounted = false;
    };
  }, [fetchRooms, fetchMenuItems, fetchRecentOrders]);

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

  // Subtotal, Service fee (5%), VAT (8%), Grand Total calculation
  const subTotal = cart.reduce((sum, c) => sum + c.item.basePrice * c.quantity, 0);
  const serviceFeeAmount = Math.round(subTotal * 0.05);
  const taxableAmount = subTotal + serviceFeeAmount;
  const vatAmount = Math.round(taxableAmount * 0.08);
  const grandTotal = taxableAmount + vatAmount;

  const selectedRoom = roomList.find((r) => r.id === roomInstanceId);

  const handleConfirmPlaceOrder = async () => {
    if (!roomInstanceId || cart.length === 0) return;

    setIsPlacingOrder(true);
    setOrderSuccessMessage('');
    setOrderErrorMessage('');

    try {
      const res = await operationService.createServiceOrder({
        roomInstanceId: Number(roomInstanceId),
        bookingId: selectedRoom?.bookingId,
        items: cart.map((c) => ({
          menuId: c.item.id,
          quantity: c.quantity,
        })),
      });

      setOrderSuccessMessage(`Đã tạo đơn dịch vụ #${res.orderNumber} thành công và tự động đẩy vào Folio phòng!`);
      setCart([]);
      setIsConfirmOrderModalOpen(false);
      fetchRecentOrders();
      fetchMenuItems(); // Refresh stock
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Có lỗi xảy ra khi tạo đơn dịch vụ';
      setOrderErrorMessage(msg);
      setIsConfirmOrderModalOpen(false);
    } finally {
      setIsPlacingOrder(false);
    }
  };

  const handleConfirmCancelOrder = async () => {
    if (!orderToCancel) return;
    setIsCancellingOrder(true);
    setOrderSuccessMessage('');
    setOrderErrorMessage('');

    try {
      await operationService.updateServiceOrderStatus(orderToCancel.id, 'CANCELLED');
      setOrderSuccessMessage(`Đã hủy đơn ${orderToCancel.orderNumber} thành công! Kho đã được hoàn trả và bút toán bù âm đã được ghi nhận trên Folio.`);
      setOrderToCancel(null);
      fetchRecentOrders();
      fetchMenuItems();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Không thể hủy đơn dịch vụ này';
      setOrderErrorMessage(msg);
      setOrderToCancel(null);
    } finally {
      setIsCancellingOrder(false);
    }
  };

  const handleUpdateOrderStatus = async (orderId: number, status: ServiceOrderStatus) => {
    try {
      await operationService.updateServiceOrderStatus(orderId, status);
      fetchRecentOrders();
      fetchMenuItems();
    } catch (err) {
      console.error('Update status failed:', err);
    }
  };

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <BellRing className="w-7 h-7 text-red-600" />
            Gọi Dịch Vụ Phòng & POS (Room Service Ordering)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Đặt món ăn, đồ uống minibar và dịch vụ phòng — Tự động trừ kho & kết nối hóa đơn Folio
          </p>
        </div>
      </div>

      {/* Destination Selector: Chỉ hiện các phòng đã được tạo đơn hàng */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center gap-4">
        <div className="text-sm font-semibold text-neutral-800">Thông tin phòng nhận dịch vụ:</div>
        <div className="w-80">
          <select
            className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-red-600 focus:border-red-600 font-medium text-neutral-900 cursor-pointer"
            value={roomInstanceId}
            onChange={(e) => setRoomInstanceId(e.target.value ? parseInt(e.target.value) : '')}
            disabled={isLoadingRooms}
          >
            {roomList.length === 0 ? (
              <option value="">-- Chưa có phòng nào có đơn hàng --</option>
            ) : (
              <option value="">-- Chọn phòng đã tạo đơn hàng --</option>
            )}
            {roomList.map((room) => (
              <option key={room.id} value={room.id}>
                Phòng {room.roomNumber} ({room.roomTypeName || 'Tiêu chuẩn'}) - {room.currentStatus} [{room.bookingNumber}]
              </option>
            ))}
          </select>
        </div>
        {selectedRoom ? (
          <div className="text-xs text-neutral-600 bg-neutral-50 px-3.5 py-2 rounded-xl border border-neutral-200 flex flex-wrap items-center gap-3">
            <span>Số phòng: <strong className="text-neutral-900 font-bold">Phòng {selectedRoom.roomNumber}</strong></span>
            <span>•</span>
            <span>Đơn hàng: <strong className="text-neutral-900 font-mono font-bold">{selectedRoom.bookingNumber}</strong></span>
            <span>•</span>
            <span>Khách hàng: <strong className="text-neutral-900">{selectedRoom.guestName}</strong></span>
            <span>•</span>
            <span>Khách sạn: <strong className="text-neutral-900">{selectedRoom.hotelName}</strong></span>
            <span>•</span>
            <span>
              Trạng thái:{' '}
              <strong className={selectedRoom.currentStatus === 'Đang ở' ? 'text-blue-700' : 'text-emerald-700'}>
                {selectedRoom.currentStatus}
              </strong>
            </span>
          </div>
        ) : (
          roomList.length === 0 && (
            <span className="text-xs text-amber-600 italic">
              Hiện chưa có phòng nào có đơn đặt phòng hợp lệ đang lưu trú để gọi món.
            </span>
          )
        )}
      </div>

      {orderSuccessMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          {orderSuccessMessage}
        </div>
      )}

      {orderErrorMessage && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
          {orderErrorMessage}
        </div>
      )}

      {/* Main Grid: Left Catalog, Right Order Cart */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Menu Catalog (2 cols wide) */}
        <div className="lg:col-span-2 space-y-4">
          {/* Filter tabs & Search */}
          <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center justify-between gap-4">
            <div className="flex gap-2">
              <button
                onClick={() => setCategoryFilter('ALL')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${categoryFilter === 'ALL'
                    ? 'bg-red-600 text-white'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                  }`}
              >
                Tất cả danh mục
              </button>
              <button
                onClick={() => setCategoryFilter('PRODUCT')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${categoryFilter === 'PRODUCT'
                    ? 'bg-red-600 text-white'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                  }`}
              >
                <Utensils className="w-3.5 h-3.5" />
                Đồ ăn & Minibar
              </button>
              <button
                onClick={() => setCategoryFilter('SERVICE')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${categoryFilter === 'SERVICE'
                    ? 'bg-red-600 text-white'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                  }`}
              >
                <Sparkles className="w-3.5 h-3.5" />
                Dịch vụ phòng
              </button>
            </div>
            <div className="w-64">
              <Input
                placeholder="Tìm món/dịch vụ..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
              />
            </div>
          </div>

          {/* Catalog Items Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
            {isLoadingMenu ? (
              <div className="col-span-full py-16 text-center text-neutral-400">
                Đang tải thực đơn phục vụ...
              </div>
            ) : menuItems.length === 0 ? (
              <div className="col-span-full py-16 text-center text-neutral-400">
                Không có món hoặc dịch vụ nào trong danh mục này.
              </div>
            ) : (
              menuItems.map((item) => (
                <div
                  key={item.id}
                  className="bg-white p-4 rounded-2xl border border-neutral-200 hover:border-red-300 hover:shadow-sm transition-all flex flex-col justify-between"
                >
                  <div>
                    <div className="flex justify-between items-start mb-2">
                      <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                        {item.menuType === 'PRODUCT' ? 'F&B / Minibar' : 'Dịch vụ'}
                      </span>
                      {item.menuType === 'PRODUCT' ? (
                        <span
                          className={`text-[11px] font-bold px-1.5 py-0.5 rounded ${(item.stockQuantity ?? 0) > 0
                              ? 'bg-emerald-50 text-emerald-700'
                              : 'bg-rose-50 text-rose-700'
                            }`}
                        >
                          Còn {(item.stockQuantity ?? 0)}
                        </span>
                      ) : (
                        <span className="text-[11px] font-medium text-neutral-500 bg-neutral-100 px-1.5 py-0.5 rounded">
                          Lượt
                        </span>
                      )}
                    </div>
                    <h3 className="font-bold text-neutral-900 text-sm line-clamp-1">{item.name}</h3>
                    {item.description && (
                      <p className="text-xs text-neutral-500 mt-1 line-clamp-2">{item.description}</p>
                    )}
                  </div>
                  <div className="mt-4 pt-3 border-t border-neutral-100 flex items-center justify-between">
                    <span className="font-bold text-red-600 text-base">{formatVND(item.basePrice)}</span>
                    <Button
                      onClick={() => addToCart(item)}
                      disabled={item.menuType === 'PRODUCT' && (item.stockQuantity ?? 0) <= 0}
                      className="text-xs py-1.5 px-3 flex items-center gap-1"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      Thêm
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Column: Order Cart (1 col wide) */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs flex flex-col justify-between h-fit sticky top-6">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-neutral-200 mb-4">
              <h2 className="font-bold text-neutral-900 text-lg flex items-center gap-2">
                <ShoppingCart className="w-5 h-5 text-red-600" />
                Đơn dịch vụ phòng
              </h2>
              <span className="text-xs font-semibold px-2 py-0.5 bg-red-50 text-red-700 rounded-full">
                {cart.length} món
              </span>
            </div>

            {cart.length === 0 ? (
              <div className="py-12 text-center text-neutral-400 text-sm">
                Chưa có món nào được chọn. Hãy nhấn nút &quot;Thêm&quot; từ danh mục bên trái.
              </div>
            ) : (
              <div className="space-y-3 max-h-[350px] overflow-y-auto pr-1">
                {cart.map((c) => (
                  <div
                    key={c.item.id}
                    className="flex items-center justify-between p-2.5 bg-neutral-50 rounded-xl border border-neutral-200"
                  >
                    <div className="flex-1 min-w-0 pr-2">
                      <div className="font-semibold text-neutral-900 text-xs truncate">{c.item.name}</div>
                      <div className="text-[11px] text-neutral-500">{formatVND(c.item.basePrice)}</div>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="flex items-center border border-neutral-300 rounded-lg bg-white overflow-hidden">
                        <button
                          onClick={() => updateQuantity(c.item.id, -1)}
                          className="p-1 hover:bg-neutral-100 text-neutral-600"
                        >
                          <Minus className="w-3 h-3" />
                        </button>
                        <span className="px-2 text-xs font-bold text-neutral-900">{c.quantity}</span>
                        <button
                          onClick={() => updateQuantity(c.item.id, 1)}
                          disabled={c.item.menuType === 'PRODUCT' && c.quantity >= (c.item.stockQuantity ?? 0)}
                          className="p-1 hover:bg-neutral-100 text-neutral-600 disabled:opacity-30"
                        >
                          <Plus className="w-3 h-3" />
                        </button>
                      </div>
                      <button
                        onClick={() => removeFromCart(c.item.id)}
                        className="text-neutral-400 hover:text-red-600 p-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Pricing Summary */}
          {cart.length > 0 && (
            <div className="mt-6 pt-4 border-t border-neutral-200 space-y-2 text-sm">
              <div className="flex justify-between text-neutral-600 text-xs">
                <span>Tiền hàng gốc:</span>
                <span>{formatVND(subTotal)}</span>
              </div>
              <div className="flex justify-between text-neutral-600 text-xs">
                <span>Phí dịch vụ khách sạn (5%):</span>
                <span>{formatVND(serviceFeeAmount)}</span>
              </div>
              <div className="flex justify-between text-neutral-600 text-xs">
                <span>Thuế GTGT (VAT 8%):</span>
                <span>{formatVND(vatAmount)}</span>
              </div>
              <div className="flex justify-between font-bold text-neutral-900 pt-2 border-t border-neutral-100 text-base">
                <span>Tổng hạch toán Folio:</span>
                <span className="text-red-600">{formatVND(grandTotal)}</span>
              </div>

              <Button
                onClick={() => setIsConfirmOrderModalOpen(true)}
                disabled={isPlacingOrder || !roomInstanceId || cart.length === 0}
                className="w-full mt-4 flex items-center justify-center gap-2 h-12 rounded-xl bg-red-600 hover:bg-red-700 text-white font-semibold shadow-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Send className="w-4 h-4" />
                Xác nhận đặt đơn & Ghi Folio
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Bottom Section: Active Service Orders List */}
      <div className="bg-white rounded-2xl border border-neutral-200 p-5 shadow-xs space-y-4">
        <div className="flex justify-between items-center">
          <h2 className="text-lg font-bold text-neutral-900 flex items-center gap-2">
            <Clock className="w-5 h-5 text-red-600" />
            Đơn dịch vụ phòng vừa tạo gần đây (Live Service Orders)
          </h2>
          <Button variant="secondary" onClick={fetchRecentOrders} className="text-xs py-1.5 flex items-center gap-1.5">
            <RefreshCw className="w-3.5 h-3.5" />
            Làm mới đơn
          </Button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-neutral-50 border-b border-neutral-200 text-neutral-600 font-semibold text-xs uppercase tracking-wider">
                <th className="py-3 px-4">Mã Đơn</th>
                <th className="py-3 px-4">Booking / Phòng</th>
                <th className="py-3 px-4">Các món gọi</th>
                <th className="py-3 px-4">Tổng tiền (Folio)</th>
                <th className="py-3 px-4">Trạng thái</th>
                <th className="py-3 px-4 text-right">Chuyển trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoadingOrders ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-neutral-400">
                    Đang tải danh sách đơn...
                  </td>
                </tr>
              ) : recentOrders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-neutral-400">
                    Chưa có đơn dịch vụ phòng nào.
                  </td>
                </tr>
              ) : (
                recentOrders.map((order) => (
                  <tr key={order.id} className="hover:bg-neutral-50 transition-colors">
                    <td className="py-3 px-4 font-bold text-neutral-900">{order.orderNumber}</td>
                    <td className="py-3 px-4">
                      <div className="font-semibold text-neutral-800">
                        {order.roomNumber ? `Phòng ${order.roomNumber}` : 'Phòng dịch vụ'}
                      </div>
                      <div className="text-xs text-neutral-500">
                        {order.bookingNumber ? `Booking #${order.bookingNumber}` : `Mã đơn: #${order.orderNumber}`}
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <div className="text-xs text-neutral-700 space-y-0.5">
                        {order.details.map((d) => (
                          <div key={d.id} className="truncate max-w-[200px]">
                            • {d.quantity}x {d.itemName}
                          </div>
                        ))}
                      </div>
                    </td>
                    <td className="py-3 px-4 font-bold text-neutral-900">{formatVND(order.totalAmount)}</td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-bold ${order.status === 'PENDING'
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : order.status === 'PREPARING'
                              ? 'bg-blue-50 text-blue-700 border border-blue-200'
                              : order.status === 'DELIVERED'
                                ? 'bg-purple-50 text-purple-700 border border-purple-200'
                                : order.status === 'COMPLETED'
                                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                  : 'bg-rose-50 text-rose-700 border border-rose-200'
                          }`}
                      >
                        {order.status === 'PENDING' && 'Chờ tiếp nhận'}
                        {order.status === 'PREPARING' && 'Đang chuẩn bị'}
                        {order.status === 'DELIVERED' && 'Đang giao phòng'}
                        {order.status === 'COMPLETED' && 'Hoàn thành'}
                        {order.status === 'CANCELLED' && 'Đã hủy'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right">
                      {order.status !== 'CANCELLED' && order.status !== 'COMPLETED' && (
                        <div className="flex justify-end gap-1.5">
                          {order.status === 'PENDING' && (
                            <button
                              onClick={() => handleUpdateOrderStatus(order.id, 'PREPARING')}
                              className="px-2 py-1 bg-blue-50 text-blue-700 hover:bg-blue-100 rounded text-xs font-semibold"
                            >
                              Chuẩn bị
                            </button>
                          )}
                          {order.status === 'PREPARING' && (
                            <button
                              onClick={() => handleUpdateOrderStatus(order.id, 'DELIVERED')}
                              className="px-2 py-1 bg-purple-50 text-purple-700 hover:bg-purple-100 rounded text-xs font-semibold"
                            >
                              Giao phòng
                            </button>
                          )}
                          {order.status === 'DELIVERED' && (
                            <button
                              onClick={() => handleUpdateOrderStatus(order.id, 'COMPLETED')}
                              className="px-2 py-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded text-xs font-semibold"
                            >
                              Xong
                            </button>
                          )}
                          <button
                            onClick={() => setOrderToCancel(order)}
                            className="px-2 py-1 bg-rose-50 text-rose-700 hover:bg-rose-100 rounded text-xs font-semibold cursor-pointer"
                          >
                            Hủy
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal xác nhận đặt đơn và hạch toán Folio */}
      <Modal
        isOpen={isConfirmOrderModalOpen}
        onClose={() => setIsConfirmOrderModalOpen(false)}
        title="Xác nhận đặt đơn dịch vụ phòng"
        description="Vui lòng kiểm tra lại thông tin đơn hàng trước khi tự động hạch toán vào hóa đơn Folio."
        maxWidth="lg"
      >
        <div className="space-y-4">
          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 text-xs flex justify-between items-center">
            <div>
              <span className="text-neutral-500">Phòng nhận dịch vụ:</span>{' '}
              <strong className="text-neutral-900">
                {selectedRoom ? `Phòng ${selectedRoom.roomNumber}` : 'Phòng đã chọn'}
              </strong>
            </div>
            <div>
              <span className="text-neutral-500">Mã đơn đặt phòng:</span>{' '}
              <strong className="text-neutral-900 font-mono">
                {selectedRoom?.bookingNumber || 'N/A'}
              </strong>
            </div>
            <div>
              <span className="text-neutral-500">Loại phòng:</span>{' '}
              <strong className="text-neutral-900">{selectedRoom?.roomTypeName || 'Tiêu chuẩn'}</strong>
            </div>
          </div>

          <div className="max-h-60 overflow-y-auto divide-y divide-neutral-100 border border-neutral-100 rounded-xl">
            {cart.map(({ item, quantity }) => (
              <div key={item.id} className="p-3 flex justify-between items-center text-sm">
                <div>
                  <div className="font-semibold text-neutral-800">{item.name}</div>
                  <div className="text-xs text-neutral-500">
                    {formatVND(item.basePrice)} × {quantity}
                  </div>
                </div>
                <div className="font-bold text-neutral-900">
                  {formatVND(item.basePrice * quantity)}
                </div>
              </div>
            ))}
          </div>

          <div className="p-3 bg-neutral-50 rounded-xl space-y-1.5 text-xs">
            <div className="flex justify-between text-neutral-600">
              <span>Tiền hàng gốc:</span>
              <span>{formatVND(subTotal)}</span>
            </div>
            <div className="flex justify-between text-neutral-600">
              <span>Phí dịch vụ khách sạn (5%):</span>
              <span>{formatVND(serviceFeeAmount)}</span>
            </div>
            <div className="flex justify-between text-neutral-600">
              <span>Thuế GTGT (VAT 8%):</span>
              <span>{formatVND(vatAmount)}</span>
            </div>
            <div className="flex justify-between font-bold text-neutral-900 text-sm pt-2 border-t border-neutral-200">
              <span>Tổng hạch toán Folio:</span>
              <span className="text-red-600 font-extrabold">{formatVND(grandTotal)}</span>
            </div>
          </div>

          <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 text-xs flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <span>
              <strong>Lưu ý tài chính:</strong> Thao tác này sẽ tự động trừ tồn kho mặt hàng và ghi nhận trực tiếp khoản phí{' '}
              <strong>{formatVND(grandTotal)}</strong> vào Folio của phòng.
            </span>
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-100">
            <Button
              variant="secondary"
              onClick={() => setIsConfirmOrderModalOpen(false)}
              disabled={isPlacingOrder}
            >
              Hủy bỏ
            </Button>
            <Button
              className="bg-red-600 hover:bg-red-700 text-white rounded-xl h-12 px-6 font-semibold flex items-center gap-2 shadow-xs cursor-pointer"
              onClick={handleConfirmPlaceOrder}
              isLoading={isPlacingOrder}
            >
              <Send className="w-4 h-4" />
              Xác nhận đặt & Ghi Folio
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal xác nhận hủy đơn dịch vụ */}
      <ConfirmModal
        isOpen={orderToCancel !== null}
        onClose={() => setOrderToCancel(null)}
        onConfirm={handleConfirmCancelOrder}
        title="Xác nhận hủy đơn dịch vụ"
        description={`Bạn có chắc chắn muốn hủy đơn hàng #${orderToCancel?.orderNumber}? Hệ thống sẽ hoàn trả số lượng tồn kho (đối với hàng hóa) và tự động tạo bút toán bù âm trên Folio của phòng để hoàn tiền cho khách.`}
        confirmText="Xác nhận hủy đơn"
        cancelText="Đóng"
        isLoading={isCancellingOrder}
        variant="danger"
      />
    </div>
  );
};
