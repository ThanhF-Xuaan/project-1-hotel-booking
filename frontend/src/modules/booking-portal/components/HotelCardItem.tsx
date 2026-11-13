import React from 'react';
import { MapPin, Phone, Star, ArrowRight, Sparkles, Building2 } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import type { AvailableHotelDto } from '../types/portal.types';
import { formatCurrency } from '../../../core/utils/currency';

interface HotelCardItemProps {
  hotel: AvailableHotelDto;
  onSelectHotel: (hotel: AvailableHotelDto) => void;
}

export const HotelCardItem: React.FC<HotelCardItemProps> = ({ hotel, onSelectHotel }) => {
  const fallbackCover =
    hotel.hotelId === 2
      ? 'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?auto=format&fit=crop&w=1200&q=80'
      : hotel.hotelId === 3
      ? 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80'
      : 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=80';

  const imageUrl = hotel.coverImageUrl || fallbackCover;

  return (
    <div
      className={`bg-white rounded-2xl border shadow-xs hover:shadow-md transition-all overflow-hidden flex flex-col md:flex-row group ${
        hotel.isTarget ? 'border-red-300 ring-2 ring-red-500/20 shadow-sm' : 'border-neutral-200'
      }`}
    >
      {/* Cover Image Container */}
      <div className="md:w-72 lg:w-80 h-52 md:h-auto shrink-0 relative overflow-hidden bg-neutral-100">
        <img
          src={imageUrl}
          alt={hotel.hotelName}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />
        <div className="absolute top-3 left-3 flex flex-wrap gap-1.5">
          {hotel.isTarget && (
            <span className="px-2.5 py-1 bg-red-600 text-white text-[11px] font-bold rounded-lg flex items-center gap-1 shadow-md">
              <Star className="w-3 h-3 fill-white text-white" />
              Khách sạn bạn đã chọn
            </span>
          )}
          <span className="px-2.5 py-1 bg-neutral-900/80 backdrop-blur-xs text-white text-[11px] font-semibold rounded-lg flex items-center gap-1 shadow-xs">
            <Building2 className="w-3 h-3 text-red-400" />
            {hotel.regionName || 'Khách sạn'}
          </span>
        </div>
      </div>

      {/* Hotel Info & Pricing Details */}
      <div className="p-5 flex-1 flex flex-col justify-between">
        <div className="space-y-2">
          {/* Rating */}
          <div className="flex items-center gap-1.5">
            <div className="flex text-amber-400">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className="w-4 h-4 fill-amber-400" />
              ))}
            </div>
            <span className="text-xs font-semibold text-neutral-600">5 sao quốc tế</span>
          </div>

          {/* Title */}
          <h3 className="text-lg md:text-xl font-bold text-neutral-900 group-hover:text-red-600 transition-colors">
            {hotel.hotelName}
          </h3>

          {/* Address & Phone */}
          <div className="space-y-1 text-xs text-neutral-600">
            <p className="flex items-start gap-1.5">
              <MapPin className="w-4 h-4 text-neutral-400 shrink-0 mt-0.5" />
              <span>{hotel.address}</span>
            </p>
            {hotel.phone && (
              <p className="flex items-center gap-1.5 text-neutral-500">
                <Phone className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                <span>{hotel.phone}</span>
              </p>
            )}
          </div>

          {/* Voucher Badge (if applied) */}
          {hotel.appliedVoucherCode && (
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-lg text-xs font-semibold">
              <Sparkles className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>
                Đã áp dụng mã: <strong>{hotel.appliedVoucherCode}</strong>
                {hotel.voucherDiscountAmount && hotel.voucherDiscountAmount > 0
                  ? ` (Giảm -${formatCurrency(hotel.voucherDiscountAmount)}/đêm)`
                  : ''}
              </span>
            </div>
          )}
        </div>

        {/* Pricing & CTA */}
        <div className="pt-4 mt-3 border-t border-neutral-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <span className="text-[11px] text-neutral-500 block">Giá phòng từ</span>
            <div className="flex items-baseline gap-1">
              <span className="text-xl md:text-2xl font-black text-red-600">
                {formatCurrency(hotel.minPricePerNight || 0)}
              </span>
              <span className="text-xs text-neutral-500 font-medium">/ đêm</span>
            </div>
          </div>

          <Button
            onClick={() => onSelectHotel(hotel)}
            className="flex items-center justify-center gap-2 bg-red-600 hover:bg-red-700 text-white font-bold h-11 px-5 rounded-xl shadow-xs cursor-pointer"
          >
            <span>Xem phòng trống & Đặt ngay</span>
            <ArrowRight className="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>
  );
};

export default HotelCardItem;

