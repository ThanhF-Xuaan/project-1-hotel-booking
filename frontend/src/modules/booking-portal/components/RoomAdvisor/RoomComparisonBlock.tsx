import React from 'react';
import {
  Scale,
  Users,
  BedDouble,
  Check,
  Sparkles,
  ExternalLink,
  TrendingDown,
  Maximize2,
} from 'lucide-react';
import type {
  AdvisorRecommendation,
  ComparisonFocus,
  RoomComparisonItem,
  RoomComparisonMatrix,
} from '../../types/roomAdvisor.types';

interface RoomComparisonBlockProps {
  comparison: RoomComparisonMatrix;
  recommendations?: AdvisorRecommendation[];
  onSelectRecommendation?: (recommendation: AdvisorRecommendation) => void;
  onSelectPath?: (path: string) => void;
}

const FOCUS_LABELS: Record<ComparisonFocus, string> = {
  FAMILY_KIDS: 'Phù hợp Gia đình & Trẻ nhỏ',
  BUDGET_SAVER: 'Tối ưu Ngân sách',
  SPACE_COMFORT: 'Ưu tiên Không gian & Nghỉ dưỡng',
  WORK_TRIP: 'Phù hợp Công tác',
  GENERAL: 'Đối chiếu Đa tiêu chí',
};

function formatCurrency(amountStr: string | undefined, currency: string): string {
  if (!amountStr) return 'Liên hệ';
  const numeric = Number(amountStr);
  if (Number.isNaN(numeric)) return `${amountStr} ${currency}`;
  if (currency.toUpperCase() === 'VND') {
    return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(numeric)} VND`;
  }
  return `${new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(numeric)} ${currency}`;
}

export const RoomComparisonBlock: React.FC<RoomComparisonBlockProps> = ({
  comparison,
  recommendations,
  onSelectRecommendation,
  onSelectPath,
}) => {
  if (comparison.status !== 'READY' || !comparison.items || comparison.items.length < 2) {
    return (
      <section
        aria-label="Thông báo so sánh phòng"
        className="mt-3 rounded-xl border border-neutral-200 bg-neutral-50 p-3 text-xs text-neutral-700"
      >
        <p>{comparison.tradeOffSummary}</p>
      </section>
    );
  }

  const handleViewDetails = (item: RoomComparisonItem) => {
    const matched = recommendations?.find(
      (rec) =>
        (item.recommendationId && rec.recommendationId === item.recommendationId) ||
        rec.room.hotelRoomTypeId === item.hotelRoomTypeId
    );
    if (matched && onSelectRecommendation) {
      onSelectRecommendation(matched);
      return;
    }
    if (item.action?.path && onSelectPath) {
      onSelectPath(item.action.path);
    }
  };

  return (
    <section
      aria-label="Bảng so sánh chi tiết các hạng phòng"
      className="mt-3 pt-3 border-t border-neutral-100 space-y-3"
    >
      {/* Header & Focus Badge */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 text-xs font-semibold text-neutral-900">
          <Scale className="w-4 h-4 text-[#EE0000]" aria-hidden="true" />
          <span>So sánh chi tiết ({comparison.items.length} hạng phòng)</span>
        </div>
        <span className="inline-flex items-center rounded-full bg-red-50 border border-red-200 px-2.5 py-0.5 text-[11px] font-medium text-[#EE0000]">
          {FOCUS_LABELS[comparison.focus] ?? FOCUS_LABELS.GENERAL}
        </span>
      </div>

      {/* Horizontal-scroll / Grid comparison columns */}
      <div className="flex gap-2.5 overflow-x-auto pb-1">
        {comparison.items.map((item) => {
          const isCheapest = item.priceDeltaFromCheapest === '0';
          const diffPct = item.priceDiffPercentFromCheapest;
          const canNavigate =
            Boolean(
              recommendations?.some(
                (r) =>
                  (item.recommendationId && r.recommendationId === item.recommendationId) ||
                  r.room.hotelRoomTypeId === item.hotelRoomTypeId
              ) && onSelectRecommendation
            ) || Boolean(item.action?.path && onSelectPath);

          return (
            <article
              key={`${item.hotelRoomTypeId}-${item.recommendationId ?? item.roomTypeCode}`}
              className={`min-w-[205px] flex-1 rounded-xl border p-3 flex flex-col justify-between bg-white shadow-xs ${
                isCheapest ? 'border-emerald-300' : 'border-neutral-200'
              }`}
            >
              <div className="space-y-2.5">
                {/* Title & Badge */}
                <div>
                  <div className="flex items-start justify-between gap-1.5">
                    <h3 className="text-xs font-bold text-neutral-900 leading-snug">
                      {item.roomTypeName}
                    </h3>
                    {isCheapest ? (
                      <span className="shrink-0 inline-flex items-center gap-0.5 rounded-md bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-700">
                        <TrendingDown className="w-3 h-3" aria-hidden="true" />
                        Giá tốt nhất
                      </span>
                    ) : (
                      typeof diffPct === 'number' &&
                      diffPct > 0 && (
                        <span className="shrink-0 rounded-md bg-neutral-100 border border-neutral-200 px-1.5 py-0.5 text-[10px] font-semibold text-neutral-700">
                          +{diffPct}%
                        </span>
                      )
                    )}
                  </div>
                  <p className="text-[11px] text-neutral-500 mt-0.5 truncate" title={item.hotelName}>
                    {item.hotelName}
                  </p>
                </div>

                {/* Price Block */}
                <div className="rounded-lg bg-neutral-50 border border-neutral-100 p-2">
                  <div className="text-sm font-bold text-[#EE0000]">
                    {formatCurrency(item.finalTotalAmount, item.currency)}
                  </div>
                  <div className="text-[11px] text-neutral-600 mt-0.5">
                    {item.totalNights} đêm
                    {item.perNightAverageAmount && (
                      <span> (~{formatCurrency(item.perNightAverageAmount, item.currency)}/đêm)</span>
                    )}
                  </div>
                  {!isCheapest && item.priceDeltaFromCheapest && (
                    <div className="text-[10px] text-neutral-500 mt-0.5">
                      Chênh +{formatCurrency(item.priceDeltaFromCheapest, item.currency)}
                    </div>
                  )}
                  <div className="text-[10px] text-neutral-500 mt-0.5">
                    {item.chargeCoverage === 'ALL_MANDATORY_CHARGES'
                      ? 'Đã gồm thuế & phí bắt buộc'
                      : 'Theo báo giá hệ thống'}
                  </div>
                </div>

                {/* Static Area & Layout */}
                {item.staticAreaAndLayout && (
                  <div className="flex items-start gap-1.5 text-[11px] text-neutral-700">
                    <Maximize2 className="w-3.5 h-3.5 text-neutral-500 shrink-0 mt-0.5" aria-hidden="true" />
                    <span>{item.staticAreaAndLayout}</span>
                  </div>
                )}

                {/* Capacity & Extra Beds */}
                <div className="space-y-1 text-[11px] text-neutral-700">
                  <div className="flex items-center gap-1.5">
                    <Users className="w-3.5 h-3.5 text-neutral-500 shrink-0" aria-hidden="true" />
                    <span>
                      Tối đa {item.maxTotalGuests} khách ({item.maxAdults} NL, {item.maxChildren} TE)
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <BedDouble className="w-3.5 h-3.5 text-neutral-500 shrink-0" aria-hidden="true" />
                    <span>
                      {item.extraBedsAllowed > 0
                        ? `Hỗ trợ kê thêm tối đa ${item.extraBedsAllowed} giường phụ`
                        : 'Không hỗ trợ kê giường phụ'}
                    </span>
                  </div>
                </div>

                {/* Unique Features */}
                {item.uniqueFeatureNames && item.uniqueFeatureNames.length > 0 && (
                  <div>
                    <div className="text-[10px] font-semibold uppercase tracking-wide text-neutral-500 mb-1">
                      Điểm cộng riêng
                    </div>
                    <div className="flex flex-wrap gap-1">
                      {item.uniqueFeatureNames.map((feat) => (
                        <span
                          key={feat}
                          className="inline-flex items-center gap-1 rounded-md bg-red-50/70 border border-red-100 px-1.5 py-0.5 text-[10px] font-medium text-neutral-800"
                        >
                          <Check className="w-2.5 h-2.5 text-[#EE0000]" aria-hidden="true" />
                          {feat}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Action Button */}
              {canNavigate && (
                <button
                  type="button"
                  onClick={() => handleViewDetails(item)}
                  aria-label={`Xem chi tiết ${item.roomTypeName}`}
                  className="mt-3 w-full inline-flex items-center justify-center gap-1.5 rounded-xl bg-[#EE0000] hover:bg-red-700 text-white text-xs font-semibold py-2 px-3 transition-colors focus:outline-none focus:ring-2 focus:ring-red-400"
                >
                  <span>Xem chi tiết</span>
                  <ExternalLink className="w-3.5 h-3.5" aria-hidden="true" />
                </button>
              )}
            </article>
          );
        })}
      </div>

      {/* Shared Features Footer */}
      {comparison.sharedFeatureNames && comparison.sharedFeatureNames.length > 0 && (
        <div className="text-[11px] text-neutral-600 bg-neutral-50 rounded-xl border border-neutral-200 px-3 py-2">
          <span className="font-semibold text-neutral-800">Tiện ích chung ở cả {comparison.items.length} phòng: </span>
          <span>{comparison.sharedFeatureNames.join(', ')}</span>
        </div>
      )}

      {/* Trade-off Recommendation Callout Box */}
      {comparison.tradeOffSummary && (
        <div className="rounded-xl border border-red-200 bg-red-50/50 p-3 text-xs text-neutral-800 leading-relaxed">
          <div className="flex items-center gap-1.5 font-semibold text-[#EE0000] mb-1">
            <Sparkles className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Tư vấn lựa chọn &amp; đánh đổi</span>
          </div>
          <p>{comparison.tradeOffSummary}</p>
        </div>
      )}
    </section>
  );
};
