import React, { useState, useEffect } from 'react';
import {
  Users,
  Building,
  CheckCircle2,
  Bed,
  CreditCard,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import hotelService from '../../organization/services/hotel.service';
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import pricingService from '../../pricing/services/pricing.service';
import bookingService from '../services/booking.service';
import type { HotelDto } from '../../organization/types/organization.types';
import type { HotelRoomTypeDto, RoomInstanceDto } from '../../inventory/types/inventory.types';
import type { PriceBreakdownDto } from '../../pricing/types/pricing.types';

export const WalkInBookingPage: React.FC = () => {
  // Hotels & Configurations
  const [hotels, setHotels] = useState<HotelDto[]>([]);
  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>('');
  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([]);
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState<number | ''>('');
  const [physicalRooms, setPhysicalRooms] = useState<RoomInstanceDto[]>([]);
  const [selectedRoomInstanceId, setSelectedRoomInstanceId] = useState<number | ''>('');

  // Dates & Guests
  const today = new Date().toISOString().split('T')[0];
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
  const [checkInDate, setCheckInDate] = useState(today);
  const [checkOutDate, setCheckOutDate] = useState(tomorrow);
  const [adults, setAdults] = useState(1);
  const [children, setChildren] = useState(0);

  // Guest Details
  const [guestPhone, setGuestPhone] = useState('');
  const [guestFirstName, setGuestFirstName] = useState('');
  const [guestLastName, setGuestLastName] = useState('');
  const [guestIdentityType, setGuestIdentityType] = useState('CCCD');
  const [guestIdentityNumber, setGuestIdentityNumber] = useState('');

  // Pricing breakdown preview
  const [priceBreakdown, setPriceBreakdown] = useState<PriceBreakdownDto | null>(null);
  const [isCalculatingPrice, setIsCalculatingPrice] = useState(false);

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState<string | null>(null);
  const [bookingError, setBookingError] = useState<string | null>(null);

  // Load Hotels
  useEffect(() => {
    hotelService
      .filter({ pageSize: 50, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setHotels(res.result.content || []);
      })
      .catch((err) => console.error('Lỗi tải khách sạn:', err));
  }, []);

  // When hotel changes, load hotel room types
  useEffect(() => {
    if (selectedHotelId) {
      hotelRoomTypeService
        .filter({ hotelId: Number(selectedHotelId), pageSize: 50, status: 'ACTIVE' })
        .then((res) => {
          if (res.result) setRoomTypes(res.result.content || []);
        })
        .catch((err) => console.error('Lỗi tải loại phòng:', err));
    } else {
      setRoomTypes([]);
    }
  }, [selectedHotelId]);

  // When room type changes, load available physical rooms
  useEffect(() => {
    if (selectedRoomTypeId) {
      roomInstanceService
        .filter({
          hotelRoomTypeId: Number(selectedRoomTypeId),
          pageSize: 50,
          currentStatus: 'READY',
        })
        .then((res) => {
          if (res.result) setPhysicalRooms(res.result.content || []);
        })
        .catch((err) => console.error('Lỗi tải phòng vật lý:', err));
    } else {
      setPhysicalRooms([]);
    }
  }, [selectedRoomTypeId]);

  // Live Price Calculation preview via Pricing Engine
  useEffect(() => {
    if (selectedRoomTypeId && checkInDate && checkOutDate && checkInDate < checkOutDate) {
      setIsCalculatingPrice(true);
      pricingService
        .calculate({
          hotelRoomTypeId: Number(selectedRoomTypeId),
          checkInDate,
          checkOutDate,
          adults,
          children,
        })
        .then((res) => {
          if (res.result) setPriceBreakdown(res.result);
        })
        .catch((err) => console.error('Lỗi tính giá:', err))
        .finally(() => setIsCalculatingPrice(false));
    } else {
      setPriceBreakdown(null);
    }
  }, [selectedRoomTypeId, checkInDate, checkOutDate, adults, children]);

  const handleSubmitWalkIn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedHotelId || !selectedRoomTypeId || !guestPhone) {
      setBookingError('Vui lòng điền đầy đủ thông tin bắt buộc');
      return;
    }

    setBookingError(null);
    setIsSubmitting(true);

    try {
      // In walk-in POS, guest is registered or linked by staff
      const res = await bookingService.create({
        hotelId: Number(selectedHotelId),
        guestId: 1, // Fallback/Walk-in guest ID or dynamic registration
        bookingType: 'FIT',
        serviceFeeRate: 5,
        rooms: [
          {
            hotelRoomTypeId: Number(selectedRoomTypeId),
            checkInDate,
            checkOutDate,
            quantity: 1,
            adultCount: adults,
            childCount: children,
            roomInstanceId: selectedRoomInstanceId ? Number(selectedRoomInstanceId) : undefined,
            guests: [
              {
                firstName: guestFirstName,
                lastName: guestLastName,
                fullName: `${guestFirstName} ${guestLastName}`.trim(),
                identityType: guestIdentityType,
                identityNumber: guestIdentityNumber,
              },
            ],
          },
        ],
      });

      if (res.result) {
        setBookingSuccess(`Tạo đơn đặt phòng thành công! Mã đơn: ${res.result.bookingNumber}`);
      }
    } catch (err: unknown) {
      console.error('Lỗi tạo đơn đặt phòng walk-in:', err);
      setBookingError('Tạo đơn thất bại. Vui lòng kiểm tra lại quỹ phòng hoặc thông tin nhập.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-neutral-900">Tiếp nhận Khách Vãng lai (Walk-In Booking POS)</h1>
        <p className="text-sm text-neutral-500 mt-1">
          Hỗ trợ lễ tân tạo đơn đặt phòng trực tiếp tại quầy, tính giá tự động và chỉ định phòng vật lý tức thì
        </p>
      </div>

      {bookingSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          <span className="font-medium">{bookingSuccess}</span>
        </div>
      )}

      {bookingError && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-2xl">
          {bookingError}
        </div>
      )}

      <form onSubmit={handleSubmitWalkIn} className="space-y-6">
        {/* BƯỚC 1: CHỌN KHÁCH SẠN VÀ THỜI GIAN LƯU TRÚ */}
        <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
          <h2 className="text-base font-bold text-neutral-900 flex items-center gap-2">
            <Building className="w-5 h-5 text-red-600" />
            1. Khách sạn & Thời gian lưu trú
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Khách sạn cơ sở *</label>
              <select
                className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={selectedHotelId}
                onChange={(e) => setSelectedHotelId(Number(e.target.value))}
                required
              >
                <option value="">-- Chọn khách sạn --</option>
                {hotels.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name} - {h.address}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Cấu hình loại phòng *</label>
              <select
                className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={selectedRoomTypeId}
                onChange={(e) => setSelectedRoomTypeId(Number(e.target.value))}
                disabled={!selectedHotelId}
                required
              >
                <option value="">-- Chọn loại phòng --</option>
                {roomTypes.map((rt) => (
                  <option key={rt.id} value={rt.id}>
                    {rt.roomTypeName} ({new Intl.NumberFormat('vi-VN').format(rt.basePrice)} đ/đêm)
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Ngày nhận phòng *</label>
              <Input
                type="date"
                value={checkInDate}
                onChange={(e) => setCheckInDate(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Ngày trả phòng *</label>
              <Input
                type="date"
                value={checkOutDate}
                onChange={(e) => setCheckOutDate(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Người lớn</label>
              <Input
                type="number"
                min="1"
                max="10"
                value={adults}
                onChange={(e) => setAdults(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Trẻ em</label>
              <Input
                type="number"
                min="0"
                max="5"
                value={children}
                onChange={(e) => setChildren(Number(e.target.value))}
              />
            </div>
          </div>

          {/* Gán phòng vật lý trực tiếp (Walk-in Double Pessimistic Lock) */}
          {physicalRooms.length > 0 && (
            <div className="p-4 bg-red-50 border border-red-100 rounded-xl">
              <label className="block text-xs font-bold text-red-900 mb-1 flex items-center gap-1.5">
                <Bed className="w-4 h-4 text-red-600" />
                Chỉ định phòng vật lý ngay tại quầy (Tùy chọn)
              </label>
              <select
                className="w-full h-12 px-3 text-sm bg-white border border-red-200 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={selectedRoomInstanceId}
                onChange={(e) => setSelectedRoomInstanceId(Number(e.target.value))}
              >
                <option value="">-- Để hệ thống tự động gán sau --</option>
                {physicalRooms.map((pr) => (
                  <option key={pr.id} value={pr.id}>
                    Phòng {pr.roomNumber} - Sẵn sàng đón khách
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* BƯỚC 2: THÔNG TIN KHÁCH HÀNG */}
        <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
          <h2 className="text-base font-bold text-neutral-900 flex items-center gap-2">
            <Users className="w-5 h-5 text-red-600" />
            2. Thông tin khách hàng lưu trú
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Số điện thoại *</label>
              <Input
                placeholder="0912345678"
                value={guestPhone}
                onChange={(e) => setGuestPhone(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Họ đệm</label>
              <Input
                placeholder="Nguyễn Văn"
                value={guestLastName}
                onChange={(e) => setGuestLastName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Tên chính</label>
              <Input
                placeholder="An"
                value={guestFirstName}
                onChange={(e) => setGuestFirstName(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Loại giấy tờ</label>
              <select
                className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={guestIdentityType}
                onChange={(e) => setGuestIdentityType(e.target.value)}
              >
                <option value="CCCD">Căn cước công dân (CCCD)</option>
                <option value="PASSPORT">Hộ chiếu (Passport)</option>
                <option value="DRIVER_LICENSE">Bằng lái xe</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Số giấy tờ</label>
              <Input
                placeholder="001099xxxxxx"
                value={guestIdentityNumber}
                onChange={(e) => setGuestIdentityNumber(e.target.value)}
              />
            </div>
          </div>
        </div>

        {/* BƯỚC 3: DỰ TOÁN BẢNG GIÁ REAL-TIME (DYNAMIC PRICING BREAKDOWN) */}
        {priceBreakdown && (
          <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
            <h2 className="text-base font-bold text-neutral-900 flex items-center gap-2">
              <CreditCard className="w-5 h-5 text-red-600" />
              3. Dự toán tiền phòng (Pricing Engine Snapshot)
            </h2>

            <div className="divide-y divide-neutral-100 text-sm">
              <div className="py-2 flex justify-between">
                <span className="text-neutral-600">Tổng số đêm lưu trú:</span>
                <span className="font-semibold text-neutral-900">{priceBreakdown.totalNights} đêm</span>
              </div>
              <div className="py-2 flex justify-between">
                <span className="text-neutral-600">Tổng giá gốc các đêm:</span>
                <span className="text-neutral-900">
                  {new Intl.NumberFormat('vi-VN').format(priceBreakdown.totalBasePrice)} đ
                </span>
              </div>
              {priceBreakdown.totalDiscountAmount > 0 && (
                <div className="py-2 flex justify-between text-emerald-600">
                  <span>Giảm giá chiến dịch:</span>
                  <span>-{new Intl.NumberFormat('vi-VN').format(priceBreakdown.totalDiscountAmount)} đ</span>
                </div>
              )}
              {priceBreakdown.totalSurchargeAmount > 0 && (
                <div className="py-2 flex justify-between text-amber-600">
                  <span>Phụ thu người/giường:</span>
                  <span>+{new Intl.NumberFormat('vi-VN').format(priceBreakdown.totalSurchargeAmount)} đ</span>
                </div>
              )}
              <div className="py-3 flex justify-between font-bold text-base text-neutral-900 border-t border-neutral-200">
                <span>Dự toán thanh toán (Pre-tax):</span>
                <span className="text-red-600 text-lg">
                  {new Intl.NumberFormat('vi-VN').format(priceBreakdown.preTaxAmount)} đ
                </span>
              </div>
            </div>
          </div>
        )}

        <div className="flex justify-end gap-4">
          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={isSubmitting || !selectedRoomTypeId || isCalculatingPrice}
          >
            {isSubmitting ? (
              'Đang xác nhận đặt phòng...'
            ) : (
              <>
                <CheckCircle2 className="w-5 h-5 mr-2" />
                Xác nhận Nhận khách tại quầy
              </>
            )}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default WalkInBookingPage;
