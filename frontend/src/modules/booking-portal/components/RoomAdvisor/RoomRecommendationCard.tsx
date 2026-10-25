import React from 'react';
import {
  Users,
  Sparkles,
  ArrowRight,
  Building,
  Calendar,
} from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import type { AdvisorRecommendation } from '../../types/roomAdvisor.types';

interface RoomRecommendationCardProps {
  recommendation: AdvisorRecommendation;
  onViewDetails: (recommendation: AdvisorRecommendation) => void;
}

export const RoomRecommendationCard: React.FC<RoomRecommendationCardProps> = ({
  recommendation,
  onViewDetails,
}) => {
  const { rank, room, price, reasons, stay } = recommendation;

  const formatCurrency = (amountStr: string, currency: string = 'VND') => {
    const num = parseFloat(amountStr);
    if (isNaN(num)) return amountStr;
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: currency || 'VND',
    }).format(num);
  };

  return (
    <div className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-sm hover:border-red-300 hover:shadow-md transition-all flex flex-col justify-between gap-3 text-left">
      {/* Top Header: Rank & Hotel */}
      <div className="flex items-center justify-between gap-2 border-b border-neutral-100 pb-2">
        <div className="flex items-center gap-1.5 text-xs text-neutral-600 font-medium">
          <Building className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
          <span className="truncate max-w-[200px]" title={room.hotelName}>
            {room.hotelName}
          </span>
        </div>
        <span
          className={`px-2 py-0.5 text-[11px] font-extrabold rounded-full shrink-0 ${
            rank === 1
              ? 'bg-red-600 text-white shadow-2xs'
              : 'bg-neutral-800 text-neutral-200'
          }`}
        >
          #{rank} Gợi ý
        </span>
      </div>

      {/* Room Details */}
      <div className="space-y-2">
        <div className="flex items-start justify-between gap-2">
          <h4 className="font-bold text-neutral-900 text-sm md:text-base leading-snug">
            {room.roomTypeName}
          </h4>
          <span className="px-1.5 py-0.5 bg-neutral-100 text-neutral-700 text-[10px] font-semibold rounded shrink-0">
            {room.roomTypeCode}
          </span>
        </div>

        {/* Capacity & Stay */}
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-neutral-600">
          <span className="inline-flex items-center gap-1">
            <Users className="w-3.5 h-3.5 text-neutral-500" />
            Tối đa {room.maxAdults} Lớn{room.maxChildren > 0 ? `, ${room.maxChildren} Trẻ` : ''}
          </span>
          {stay?.checkInDate && stay?.checkOutDate && (
            <span className="inline-flex items-center gap-1 text-neutral-500">
              <Calendar className="w-3.5 h-3.5" />
              {stay.checkInDate} → {stay.checkOutDate} ({price.totalNights} đêm)
            </span>
          )}
        </div>

        {/* Features / Amenities */}
        {room.features && room.features.length > 0 && (
          <div className="flex flex-wrap gap-1 pt-1">
            {room.features.slice(0, 3).map((f) => (
              <span
                key={f.code}
                className="px-2 py-0.5 bg-neutral-50 text-neutral-600 rounded text-[11px] border border-neutral-200"
              >
                {f.name}
              </span>
            ))}
            {room.features.length > 3 && (
              <span className="px-1.5 py-0.5 text-neutral-400 text-[10px]">
                +{room.features.length - 3}
              </span>
            )}
          </div>
        )}

        {/* Match Reasons (Why AI recommends this) */}
        {reasons && reasons.length > 0 && (
          <div className="space-y-1 pt-1">
            {reasons.map((r, idx) => (
              <div
                key={idx}
                className="flex items-center gap-1.5 text-xs text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200/60 font-medium"
              >
                <Sparkles className="w-3 h-3 text-emerald-600 shrink-0" />
                <span className="leading-tight">{r.text}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Pricing & Call-to-Action */}
      <div className="border-t border-neutral-100 pt-3 flex items-end justify-between gap-3 mt-1">
        <div className="space-y-0.5">
          <div className="text-[11px] text-neutral-500 font-medium">Giá ước tính từ Core</div>
          <div className="text-[10px] text-neutral-500 leading-tight">
            {price.chargeCoverage === 'ALL_MANDATORY_CHARGES'
              ? 'Đã bao gồm thuế và phí cơ bản'
              : 'Chưa gồm một số phụ phí'}
          </div>
          <div className="text-lg md:text-xl font-extrabold text-red-600 leading-tight">
            {formatCurrency(price.finalTotalAmount, price.currency)}
          </div>
          {price.displayLabel && (
            <div className="text-[10px] text-neutral-400 leading-tight max-w-[170px]" title={price.displayLabel}>
              {price.displayLabel}
            </div>
          )}
        </div>

        <Button
          onClick={() => onViewDetails(recommendation)}
          size="sm"
          className="shrink-0 text-xs px-3 py-2 font-bold flex items-center gap-1.5"
          aria-label={`Xem chi tiết phòng ${room.roomTypeName}`}
        >
          Xem chi tiết
          <ArrowRight className="w-3.5 h-3.5" />
        </Button>
      </div>
    </div>
  );
};
