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
import { operationService } from '../services/operation.service';
import type {
  MenuResponse,
  ServiceOrderResponse,
  ServiceOrderStatus,
} from '../types/operation.types';

export const RoomServiceOrderPage: React.FC = () => {
  // Destination
  const [bookingId, setBookingId] = useState<number | ''>(1);
  const [roomInstanceId, setRoomInstanceId] = useState<number | ''>(1);

  // Menu items list
  const [menuItems, setMenuItems] = useState<MenuResponse[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | 'PRODUCT' | 'SERVICE'>('ALL');
  const [isLoadingMenu, setIsLoadingMenu] = useState(false);

  // Cart
  const [cart, setCart] = useState<{ item: MenuResponse; quantity: number }[]>([]);
  const [isPlacingOrder, setIsPlacingOrder] = useState(false);
  const [orderSuccessMessage, setOrderSuccessMessage] = useState('');
  const [orderErrorMessage, setOrderErrorMessage] = useState('');

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

  useEffect(() => {
    fetchMenuItems();
    fetchRecentOrders();
  }, [fetchMenuItems, fetchRecentOrders]);

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

  const handlePlaceOrder = async () => {
    if (!bookingId || !roomInstanceId || cart.length === 0) return;

    setIsPlacingOrder(true);
    setOrderSuccessMessage('');
    setOrderErrorMessage('');

    try {
      const res = await operationService.createServiceOrder({
        bookingId: Number(bookingId),
        roomInstanceId: Number(roomInstanceId),
        items: cart.map((c) => ({
          menuId: c.item.id,
          quantity: c.quantity,
        })),
      });

      setOrderSuccessMessage(`Đã tạo đơn dịch vụ #${res.orderNumber} thành công và tự động đẩy vào Folio phòng!`);
      setCart([]);
      fetchRecentOrders();
      fetchMenuItems(); // Refresh stock
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Có lỗi xảy ra khi tạo đơn dịch vụ';
      setOrderErrorMessage(msg);
    } finally {
      setIsPlacingOrder(false);
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

      {/* Destination Selector: Booking ID & Room Instance ID */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center gap-4">
        <div className="text-sm font-semibold text-neutral-800">Thông tin phòng nhận dịch vụ:</div>
        <div className="w-48">
          <Input
            label="ID Đơn đặt phòng (Booking ID)"
            type="number"
            value={bookingId}
            onChange={(e) => setBookingId(e.target.value ? parseInt(e.target.value) : '')}
            placeholder="VD: 1, 10..."
          />
        </div>
        <div className="w-48">
          <Input
            label="ID Phòng vật lý (Room ID)"
            type="number"
            value={roomInstanceId}
            onChange={(e) => setRoomInstanceId(e.target.value ? parseInt(e.target.value) : '')}
            placeholder="VD: 1, 5, 20..."
          />
        </div>
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
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                  categoryFilter === 'ALL'
                    ? 'bg-red-600 text-white'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                }`}
              >
                Tất cả danh mục
              </button>
              <button
                onClick={() => setCategoryFilter('PRODUCT')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${
                  categoryFilter === 'PRODUCT'
                    ? 'bg-red-600 text-white'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                }`}
              >
                <Utensils className="w-3.5 h-3.5" />
                Đồ ăn & Minibar
              </button>
              <button
                onClick={() => setCategoryFilter('SERVICE')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${
                  categoryFilter === 'SERVICE'
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
                          className={`text-[11px] font-bold px-1.5 py-0.5 rounded ${
                            (item.stockQuantity ?? 0) > 0
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
                onClick={handlePlaceOrder}
                disabled={isPlacingOrder || !bookingId || !roomInstanceId}
                className="w-full mt-4 flex items-center justify-center gap-2 py-3"
              >
                <Send className="w-4 h-4" />
                {isPlacingOrder ? 'Đang gửi đơn...' : 'Gửi Đơn Dịch Vụ'}
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
                        {order.roomNumber ? `Phòng ${order.roomNumber}` : `Phòng ID ${order.roomInstanceId}`}
                      </div>
                      <div className="text-xs text-neutral-500">Booking #{order.bookingNumber || order.bookingId}</div>
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
                        className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-bold ${
                          order.status === 'PENDING'
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
                            onClick={() => handleUpdateOrderStatus(order.id, 'CANCELLED')}
                            className="px-2 py-1 bg-rose-50 text-rose-700 hover:bg-rose-100 rounded text-xs font-semibold"
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
    </div>
  );
};
