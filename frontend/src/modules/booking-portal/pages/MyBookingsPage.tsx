import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Calendar,
  Building2,
  FileSearch,
  CheckCircle2,
  AlertCircle,
  XCircle,
  ArrowLeft,
  Phone,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import { bookingService } from '../../booking/services/booking.service';
import type { BookingResponse } from '../../booking/types/booking.types';

export const MyBookingsPage: React.FC = () => {
  const navigate = useNavigate();

  const [bookingNumber, setBookingNumber] = useState('');
  const [phone, setPhone] = useState('');
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelSuccess, setCancelSuccess] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookingNumber.trim()) return;

    setIsSearching(true);
    setSearchError('');
    setBooking(null);
    setCancelSuccess(false);

    try {
      const res = await bookingService.getByNumber(bookingNumber.trim());
      if (res && res.result) {
        setBooking(res.result);
      } else {
        setSearchError('Không tìm thấy thông tin đơn đặt phòng. Vui lòng kiểm tra lại mã booking.');
      }
    } catch (err: any) {
      setSearchError('Không tìm thấy thông tin đơn đặt phòng. Vui lòng kiểm tra lại mã booking.');
    } finally {
      setIsSearching(false);
    }
  };

  const handleCancelBooking = async () => {
    if (!booking) return;

    setIsCancelling(true);
    try {
      await bookingService.updateStatus(booking.id, 'CANCELLED');
      setBooking({ ...booking, status: 'CANCELLED' });
      setIsCancelModalOpen(false);
      setCancelSuccess(true);
    } catch (err) {
      console.error('Cancel booking failed:', err);
    } finally {
      setIsCancelling(false);
    }
  };

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  return (
    <div className="min-h-screen bg-neutral-50 pb-16">
      {/* Top Header */}
      <header className="bg-white border-b border-neutral-200 sticky top-0 z-30 shadow-2xs">
        <div className="max-w-4xl mx-auto px-4 h-16 flex items-center justify-between">
          <button
            onClick={() => navigate('/portal/search')}
            className="flex items-center gap-1.5 text-xs font-semibold text-neutral-600 hover:text-neutral-900"
          >
            <ArrowLeft className="w-4 h-4" />
            Về trang tìm kiếm phòng
          </button>
          <div className="font-bold text-neutral-900 text-sm">Cổng Tra Cứu Khách Hàng</div>
        </div>
      </header>

      <div className="max-w-xl mx-auto px-4 mt-8 space-y-6">
        <div className="text-center space-y-1">
          <div className="w-12 h-12 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-2">
            <FileSearch className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight">
            Tra Cứu Đơn Đặt Phòng
          </h1>
          <p className="text-xs text-neutral-500">
            Nhập mã đặt phòng và số điện thoại để xem chi tiết hoặc quản lý lưu trú
          </p>
        </div>

        {/* Search Box */}
        <form onSubmit={handleSearch} className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
          <div>
            <label className="block text-xs font-medium text-neutral-700 mb-1">
              Mã đặt phòng (Booking Code) <span className="text-red-500">*</span>
            </label>
            <Input
              value={bookingNumber}
              onChange={(e) => setBookingNumber(e.target.value)}
              placeholder="VD: BK12345678"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 mb-1">Số điện thoại đăng ký</label>
            <Input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="VD: 0987654321"
              leftIcon={<Phone className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <Button type="submit" disabled={isSearching} className="w-full py-3 flex items-center justify-center gap-2">
            <Search className="w-4 h-4" />
            {isSearching ? 'Đang tra cứu...' : 'Tìm Kiếm Đơn'}
          </Button>
        </form>

        {searchError && (
          <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl text-rose-800 text-xs flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            {searchError}
          </div>
        )}

        {cancelSuccess && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-800 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            Đã hủy đơn đặt phòng thành công.
          </div>
        )}

        {/* Booking Details Card */}
        {booking && (
          <div className="bg-white rounded-3xl border border-neutral-200 p-6 shadow-md space-y-5">
            <div className="flex justify-between items-start border-b border-neutral-200 pb-4">
              <div>
                <span className="text-xs text-neutral-500 uppercase tracking-wider font-semibold">Mã đơn đặt phòng</span>
                <div className="text-xl font-extrabold text-neutral-900 font-mono">{booking.bookingNumber}</div>
              </div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-bold ${
                  booking.status === 'CONFIRMED'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : booking.status === 'CANCELLED'
                    ? 'bg-rose-50 text-rose-700 border border-rose-200'
                    : 'bg-neutral-100 text-neutral-700'
                }`}
              >
                {booking.status === 'CONFIRMED' && 'Đã xác nhận'}
                {booking.status === 'CANCELLED' && 'Đã hủy'}
                {booking.status === 'NO_SHOW' && 'No-Show'}
              </span>
            </div>

            <div className="space-y-3 text-xs text-neutral-600">
              <div className="flex items-center gap-2">
                <Building2 className="w-4 h-4 text-red-600" />
                <span className="font-bold text-neutral-900 text-sm">
                  {booking.hotelName || 'Khách sạn Hotel Grand'}
                </span>
              </div>

              {booking.bookingDetails && booking.bookingDetails.length > 0 && (
                <div className="p-3 bg-neutral-50 rounded-xl space-y-1">
                  <div className="font-semibold text-neutral-800">{booking.bookingDetails[0].roomTypeName}</div>
                  <div className="flex items-center gap-1.5 text-neutral-500">
                    <Calendar className="w-3.5 h-3.5" />
                    <span>
                      {booking.bookingDetails[0].checkInDate} ➔ {booking.bookingDetails[0].checkOutDate}
                    </span>
                  </div>
                </div>
              )}

              <div className="flex justify-between pt-2 border-t border-neutral-100 text-sm">
                <span>Tổng chi phí đặt phòng:</span>
                <strong className="text-red-600 text-base">{formatVND(booking.totalAmount)}</strong>
              </div>
            </div>

            {booking.status === 'CONFIRMED' && (
              <div className="pt-2 flex justify-end">
                <Button
                  variant="danger"
                  disabled={isCancelling}
                  onClick={() => setIsCancelModalOpen(true)}
                  className="w-full text-xs py-2.5 flex items-center justify-center gap-1.5"
                >
                  <XCircle className="w-4 h-4" />
                  {isCancelling ? 'Đang hủy...' : 'Yêu Cầu Hủy Đặt Phòng'}
                </Button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Cancel Confirmation Modal */}
      <ConfirmModal
        isOpen={isCancelModalOpen}
        onClose={() => setIsCancelModalOpen(false)}
        onConfirm={handleCancelBooking}
        title="Xác nhận hủy đặt phòng"
        description="Bạn có chắc chắn muốn hủy đơn đặt phòng này không? Các phòng đã giữ sẽ được giải phóng cho khách hàng khác."
      />
    </div>
  );
};

export default MyBookingsPage;
