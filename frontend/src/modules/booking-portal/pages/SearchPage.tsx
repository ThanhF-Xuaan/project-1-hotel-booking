import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Calendar,
  Building2,
  Check,
  Coffee,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  Star,
  FileSearch,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import type { CustomerSearchCriteria, AvailableRoomOffer } from '../types/portal.types';

export const SearchPage: React.FC = () => {
  const navigate = useNavigate();

  const [criteria, setCriteria] = useState<CustomerSearchCriteria>({
    hotelId: 1,
    checkInDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    checkOutDate: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0],
    adultCount: 2,
    childCount: 0,
  });

  const [mockOffers] = useState<AvailableRoomOffer[]>([
    {
      hotelRoomTypeId: 1,
      roomTypeCode: 'STD',
      roomTypeName: 'Phòng Tiêu Chuẩn (Standard Double)',
      basePrice: 850000,
      estimatedTotal: 1700000,
      maxAdults: 2,
      maxChildren: 1,
      roomSizeM2: 28,
      bedDescription: '1 Giường đôi Queen Size',
      amenities: ['Wi-Fi 6 tốc độ cao', 'Điều hòa 2 chiều', 'TV 4K 55 inch', 'Bàn làm việc'],
      freeBreakfast: true,
      freeCancellation: true,
      availableRoomsCount: 5,
    },
    {
      hotelRoomTypeId: 2,
      roomTypeCode: 'DLX',
      roomTypeName: 'Phòng Deluxe Hướng Phố (City View)',
      basePrice: 1250000,
      estimatedTotal: 2500000,
      maxAdults: 2,
      maxChildren: 2,
      roomSizeM2: 36,
      bedDescription: '1 Giường King Size hoặc 2 Giường đơn',
      amenities: ['Ban công ngắm cảnh', 'Bồn tắm nằm', 'Minibar miễn phí', 'Máy pha cà phê'],
      freeBreakfast: true,
      freeCancellation: true,
      availableRoomsCount: 3,
    },
    {
      hotelRoomTypeId: 3,
      roomTypeCode: 'SUT',
      roomTypeName: 'Phòng Suite Hoàng Gia (Executive Suite)',
      basePrice: 2400000,
      estimatedTotal: 4800000,
      maxAdults: 3,
      maxChildren: 2,
      roomSizeM2: 58,
      bedDescription: 'Phòng khách riêng biệt + 1 Giường King Master',
      amenities: ['Dịch vụ quản gia riêng', 'Phòng khách & bếp ăn', 'Miễn phí giặt là', 'Đưa đón sân bay'],
      freeBreakfast: true,
      freeCancellation: false,
      availableRoomsCount: 2,
    },
  ]);

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

  const handleSelectRoom = (offer: AvailableRoomOffer) => {
    navigate('/portal/checkout', {
      state: {
        offer,
        criteria,
      },
    });
  };

  return (
    <div className="min-h-screen bg-neutral-50 pb-16">
      {/* Top Navigation */}
      <header className="bg-white border-b border-neutral-200 sticky top-0 z-30 shadow-2xs">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-red-600 text-white flex items-center justify-center font-extrabold text-lg shadow-xs">
              H
            </div>
            <div>
              <span className="font-extrabold text-neutral-900 tracking-tight text-lg">HOTEL GRAND</span>
              <span className="text-red-600 font-bold ml-1 text-xs uppercase tracking-widest">Portal</span>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/portal/my-bookings')}
              className="px-3.5 py-2 text-xs font-semibold text-neutral-700 hover:text-red-600 hover:bg-neutral-50 rounded-xl transition-colors flex items-center gap-1.5"
            >
              <FileSearch className="w-4 h-4" />
              Tra cứu đơn đặt phòng
            </button>
            <button
              onClick={() => navigate('/booking/list')}
              className="px-3.5 py-2 text-xs font-semibold text-white bg-neutral-900 hover:bg-black rounded-xl transition-colors"
            >
              Cổng Quản Trị Staff
            </button>
          </div>
        </div>
      </header>

      {/* Hero Banner with Search Bar */}
      <div className="bg-neutral-900 text-white py-12 px-4 shadow-inner">
        <div className="max-w-5xl mx-auto space-y-6">
          <div className="text-center space-y-2">
            <span className="px-3 py-1 bg-red-600 text-white text-xs font-bold rounded-full uppercase tracking-wider inline-flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5" />
              Cam kết giá tốt nhất & Đặt phòng trực tiếp
            </span>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
              Kỳ Nghỉ Thượng Lưu Tại Chuỗi Khách Sạn Hotel Grand
            </h1>
            <p className="text-neutral-400 text-sm max-w-xl mx-auto">
              Trải nghiệm dịch vụ 5 sao đẳng cấp quốc tế với công nghệ giữ phòng tức thì và bóc tách giá minh bạch.
            </p>
          </div>

          {/* Search Box */}
          <div className="bg-white p-4 sm:p-5 rounded-3xl shadow-xl border border-neutral-100 text-neutral-900 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-600 mb-1 flex items-center gap-1">
                <Building2 className="w-3.5 h-3.5 text-red-600" />
                Khách sạn cơ sở
              </label>
              <select
                value={criteria.hotelId}
                onChange={(e) => setCriteria({ ...criteria, hotelId: Number(e.target.value) })}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              >
                <option value={1}>Grand Hà Nội Hotel</option>
                <option value={2}>Premier Đà Nẵng Resort</option>
                <option value={3}>Boutique Sài Gòn Hotel</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-600 mb-1 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-red-600" />
                Ngày nhận phòng
              </label>
              <input
                type="date"
                value={criteria.checkInDate}
                onChange={(e) => setCriteria({ ...criteria, checkInDate: e.target.value })}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-red-600"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-600 mb-1 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-red-600" />
                Ngày trả phòng
              </label>
              <input
                type="date"
                value={criteria.checkOutDate}
                onChange={(e) => setCriteria({ ...criteria, checkOutDate: e.target.value })}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-red-600"
              />
            </div>

            <div className="flex items-end">
              <Button className="w-full h-11 flex items-center justify-center gap-2 font-bold text-sm">
                <Search className="w-4 h-4" />
                Tìm Phòng Trống
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Room Offers List */}
      <div className="max-w-5xl mx-auto px-4 mt-8 space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h2 className="text-xl font-bold text-neutral-900 tracking-tight">
              Các Hạng Phòng Khả Dụng ({mockOffers.length} lựa chọn)
            </h2>
            <p className="text-xs text-neutral-500 mt-0.5">
              Từ {criteria.checkInDate} đến {criteria.checkOutDate} • {criteria.adultCount} người lớn
            </p>
          </div>
          <div className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-200 flex items-center gap-1">
            <ShieldCheck className="w-4 h-4" />
            Giữ phòng 15 phút không cần thẻ tín dụng
          </div>
        </div>

        <div className="space-y-4">
          {mockOffers.map((offer) => (
            <div
              key={offer.hotelRoomTypeId}
              className="bg-white rounded-3xl border border-neutral-200 p-6 shadow-xs hover:border-red-300 hover:shadow-md transition-all flex flex-col md:flex-row justify-between gap-6"
            >
              <div className="flex-1 space-y-3">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 bg-neutral-900 text-white text-[11px] font-bold rounded-md">
                    {offer.roomTypeCode}
                  </span>
                  <div className="flex items-center text-amber-500 text-xs gap-0.5">
                    {[...Array(5)].map((_, i) => (
                      <Star key={i} className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                    ))}
                  </div>
                  <span className="text-xs text-neutral-400">• Còn {offer.availableRoomsCount} phòng trống</span>
                </div>

                <h3 className="text-xl font-bold text-neutral-900">{offer.roomTypeName}</h3>

                <div className="text-xs text-neutral-600 flex flex-wrap gap-x-4 gap-y-1">
                  <span>📐 Diện tích: <strong>{offer.roomSizeM2} m²</strong></span>
                  <span>🛏️ {offer.bedDescription}</span>
                  <span>👥 Tối đa {offer.maxAdults} người lớn</span>
                </div>

                {/* Amenities Badges */}
                <div className="flex flex-wrap gap-2 pt-1">
                  {offer.freeBreakfast && (
                    <span className="px-2.5 py-1 bg-emerald-50 text-emerald-800 rounded-lg text-xs font-semibold flex items-center gap-1 border border-emerald-200">
                      <Coffee className="w-3.5 h-3.5 text-emerald-600" />
                      Ăn sáng Buffet miễn phí
                    </span>
                  )}
                  {offer.freeCancellation && (
                    <span className="px-2.5 py-1 bg-blue-50 text-blue-800 rounded-lg text-xs font-semibold flex items-center gap-1 border border-blue-200">
                      <Check className="w-3.5 h-3.5 text-blue-600" />
                      Miễn phí hủy phòng
                    </span>
                  )}
                  {offer.amenities.map((amenity, idx) => (
                    <span key={idx} className="px-2.5 py-1 bg-neutral-100 text-neutral-700 rounded-lg text-xs">
                      {amenity}
                    </span>
                  ))}
                </div>
              </div>

              {/* Price & Action */}
              <div className="md:w-64 md:border-l md:border-neutral-200 md:pl-6 flex flex-col justify-between items-start md:items-end">
                <div className="text-left md:text-right space-y-1">
                  <div className="text-xs text-neutral-400">Giá mỗi đêm từ</div>
                  <div className="text-2xl font-extrabold text-red-600">{formatVND(offer.basePrice)}</div>
                  <div className="text-xs text-neutral-500">Đã bao gồm thuế VAT & phí dịch vụ</div>
                </div>

                <div className="w-full mt-4 md:mt-0">
                  <Button
                    onClick={() => handleSelectRoom(offer)}
                    className="w-full flex items-center justify-center gap-2 py-3 font-bold"
                  >
                    Đặt Phòng Ngay
                    <ArrowRight className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default SearchPage;
