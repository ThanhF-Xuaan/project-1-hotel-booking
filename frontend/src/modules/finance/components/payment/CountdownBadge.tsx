import React, { useEffect, useRef, useState } from 'react';
import { Clock } from 'lucide-react';

interface CountdownBadgeProps {
  /** expiresAt BE trả về (ISO string) — đồng bộ TTL lock Redis 10', KHÔNG hardcode thời lượng */
  expiresAt: string;
  onExpire?: () => void;
}

/**
 * Countdown phiên thanh toán — nhãn 12px, số 16px (design tokens),
 * đỏ khi còn dưới 2 phút hoặc đã hết hạn.
 */
export const CountdownBadge: React.FC<CountdownBadgeProps> = ({ expiresAt, onExpire }) => {
  const [remainingMs, setRemainingMs] = useState(
    () => new Date(expiresAt).getTime() - Date.now()
  );
  const expireFiredRef = useRef(false);

  useEffect(() => {
    expireFiredRef.current = false;
    const interval = setInterval(() => {
      setRemainingMs(new Date(expiresAt).getTime() - Date.now());
    }, 1000);
    return () => clearInterval(interval);
  }, [expiresAt]);

  useEffect(() => {
    if (remainingMs <= 0 && !expireFiredRef.current) {
      expireFiredRef.current = true;
      onExpire?.();
    }
  }, [remainingMs, onExpire]);

  const expired = remainingMs <= 0;
  const totalSeconds = Math.max(0, Math.floor(remainingMs / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  const timeText = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  const isUrgent = !expired && totalSeconds <= 120;

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-2 ${
        expired || isUrgent
          ? 'bg-red-50 border-red-200 text-red-700'
          : 'bg-neutral-50 border-neutral-200 text-neutral-700'
      }`}
    >
      <Clock className="w-4 h-4 shrink-0" aria-hidden="true" />
      <span className="text-xs font-medium">
        {expired ? 'Phiên đã hết hạn' : 'Phiên hết hạn sau'}
      </span>
      {!expired && (
        <span className="text-base font-bold tabular-nums" aria-live="polite">
          {timeText}
        </span>
      )}
    </span>
  );
};

export default CountdownBadge;
