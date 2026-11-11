import React, { useEffect, useId, useState } from 'react';
import { BookOpen, Building2, AlertCircle, Clock, ChevronDown, ChevronUp } from 'lucide-react';
import type { KnowledgeAnswer } from '../../types/roomAdvisor.types';
import { loadKnowledgeHotels, type KnowledgeHotel } from '../../services/hotelKnowledgeService';

interface KnowledgeAnswerBlockProps {
  knowledge: KnowledgeAnswer;
  onSelectHotelChoice?: (hotelId: number) => void;
  disabled?: boolean;
}

function PublicHotelPicker({ onSelect, disabled }: { onSelect: (hotelId: number) => void; disabled: boolean }) {
  const selectId = useId();
  const [attempt, setAttempt] = useState(0);
  const [hotels, setHotels] = useState<KnowledgeHotel[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [selected, setSelected] = useState('');

  useEffect(() => {
    if (attempt === 0) return;
    const request = new AbortController();
    void loadKnowledgeHotels(request.signal).then(list => {
      if (!request.signal.aborted) setHotels(list);
    }).catch(() => {
      if (!request.signal.aborted) setFailed(true);
    });
    return () => request.abort();
  }, [attempt]);

  if (attempt === 0 || failed) {
    return <div className="space-y-1">
      {failed && <p role="alert">Chưa tải được danh sách khách sạn.</p>}
      <button type="button" className="rounded border px-2.5 py-1.5 text-xs font-semibold" disabled={disabled}
        onClick={() => { setFailed(false); setAttempt(value => value + 1); }}>
        {failed ? 'Tải lại danh sách' : 'Chọn khách sạn'}
      </button>
    </div>;
  }
  if (!hotels) return <p role="status">Đang tải khách sạn…</p>;
  if (hotels.length === 0) return <p>Chưa có khách sạn để tra cứu.</p>;

  return <form className="flex flex-wrap items-end gap-2" onSubmit={event => {
    event.preventDefault();
    if (!disabled && selected) onSelect(Number(selected));
  }}>
    <div className="min-w-0 grow">
      <label htmlFor={selectId} className="text-xs font-medium">Khách sạn tra cứu</label>
      <select id={selectId} className="block w-full rounded border bg-white p-1.5 text-xs" value={selected}
        disabled={disabled} onChange={event => setSelected(event.target.value)}>
        <option value="">Chọn khách sạn</option>
        {hotels.map(hotel => <option key={hotel.id} value={hotel.id}>{hotel.name}</option>)}
      </select>
    </div>
    <button type="submit" disabled={disabled || !selected}
      className="rounded bg-red-700 px-2.5 py-1.5 text-xs font-semibold text-white disabled:opacity-50">
      Tra cứu chính sách
    </button>
  </form>;
}

export const KnowledgeAnswerBlock: React.FC<KnowledgeAnswerBlockProps> = ({
  knowledge,
  onSelectHotelChoice,
  disabled = false,
}) => {
  const [showCitations, setShowCitations] = useState(false);

  if (!knowledge || knowledge.state === 'NOT_ASKED') {
    return null;
  }

  const { state, hotelName, result, hotelChoices, retryAfterSeconds } = knowledge;

  return (
    <div className="mt-3 rounded-xl border border-red-100 bg-red-50/40 p-3 text-sm text-neutral-800 shadow-xs transition-all">
      {/* Header */}
      <div className="flex items-center justify-between pb-2 border-b border-red-100/60 mb-2">
        <div className="flex items-center gap-1.5 font-semibold text-red-700">
          <BookOpen className="h-4 w-4" />
          <span>Thông tin chính sách & tiện ích</span>
        </div>
        {hotelName && (
          <div className="flex items-center gap-1 text-xs text-neutral-500 bg-white px-2 py-0.5 rounded-md border border-neutral-200">
            <Building2 className="h-3 w-3 text-red-600" />
            <span className="font-medium truncate max-w-[160px]">{hotelName}</span>
          </div>
        )}
      </div>

      {/* State-specific Body */}
      {state === 'ANSWERED' && (
        <div className="space-y-2">
          {result?.answer && (
            <p className="leading-relaxed text-neutral-800 whitespace-pre-line">{result.answer}</p>
          )}

          {/* Citations list */}
          {result?.citations && result.citations.length > 0 && (
            <div className="mt-2 pt-2 border-t border-red-100/60">
              <button
                type="button"
                onClick={() => setShowCitations(!showCitations)}
                className="flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-700 hover:underline cursor-pointer"
              >
                {showCitations ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                <span>Nguồn trích dẫn ({result.citations.length})</span>
              </button>

              {showCitations && (
                <div className="mt-2 space-y-1.5">
                  {result.citations.map((cite, idx) => (
                    <div
                      key={`${cite.sourceId}-${idx}`}
                      className="rounded-lg bg-white p-2 text-xs border border-neutral-200/80 shadow-2xs"
                    >
                      <div className="font-medium text-neutral-900">{cite.sourceId}</div>
                      <div className="mt-0.5 text-neutral-600 line-clamp-3">{cite.snippet}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Disclaimers */}
          {result?.disclaimers && result.disclaimers.length > 0 && (
            <div className="mt-1 text-xs text-neutral-400 italic">
              {result.disclaimers.join(' • ')}
            </div>
          )}
        </div>
      )}

      {state === 'SCOPE_REQUIRED' && (
        <div className="space-y-2">
          <p className="text-neutral-700">
            {result?.answer || 'Vui lòng chọn khách sạn để xem chính sách chi tiết:'}
          </p>

          {!hotelChoices?.length && onSelectHotelChoice && (
            <PublicHotelPicker onSelect={onSelectHotelChoice} disabled={disabled} />
          )}
          {hotelChoices && hotelChoices.length > 0 && (
            <div className="flex flex-wrap gap-1.5 pt-1">
              {hotelChoices.map((choice) => (
                <button
                  key={choice.hotelId}
                  type="button"
                  disabled={disabled}
                  onClick={() => onSelectHotelChoice?.(choice.hotelId)}
                  className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-lg bg-white border border-red-300 text-red-700 hover:bg-red-600 hover:text-white hover:border-red-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shadow-2xs"
                >
                  <Building2 className="h-3 w-3" />
                  <span>{choice.hotelName}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {state === 'RATE_LIMITED' && (
        <div className="flex items-center gap-2 text-amber-800 bg-amber-50 p-2 rounded-lg border border-amber-200 text-xs">
          <Clock className="h-4 w-4 text-amber-600 shrink-0" />
          <span>
            Đã vượt giới hạn tra cứu chính sách.{' '}
            {retryAfterSeconds ? `Vui lòng thử lại sau ${retryAfterSeconds} giây.` : 'Vui lòng thử lại sau.'}
          </span>
        </div>
      )}

      {state === 'UNAVAILABLE' && (
        <div className="flex items-center gap-2 text-neutral-600 bg-neutral-100 p-2 rounded-lg border border-neutral-200 text-xs">
          <AlertCircle className="h-4 w-4 text-neutral-500 shrink-0" />
          <span>Tra cứu chính sách tạm thời không khả dụng, kết quả tìm phòng vẫn hiển thị đầy đủ.</span>
        </div>
      )}
    </div>
  );
};
