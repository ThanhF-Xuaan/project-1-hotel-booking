import React, { useState, useEffect } from 'react';
import { AlertTriangle, Clock, ArrowRight } from 'lucide-react';
import Button from '../../../core/components/ui/Button';

export interface SessionExpiredModalProps {
  isOpen: boolean;
  onRedirect: () => void;
  autoRedirectSeconds?: number;
}

export const SessionExpiredModal: React.FC<SessionExpiredModalProps> = ({
  isOpen,
  onRedirect,
  autoRedirectSeconds = 5,
}) => {
  const [countdown, setCountdown] = useState<number>(autoRedirectSeconds);

  useEffect(() => {
    if (!isOpen) return;

    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          onRedirect();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isOpen, onRedirect]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl border border-neutral-200 shadow-2xl p-6 sm:p-8 max-w-md w-full text-center space-y-5 animate-in zoom-in-95 duration-200">
        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto shadow-inner">
          <AlertTriangle className="w-8 h-8 text-red-600 animate-pulse" />
        </div>

        <div className="space-y-2">
          <h3 className="text-xl font-extrabold text-neutral-900 tracking-tight">
            Đã Hết Thời Gian Giữ Phòng!
          </h3>
          <p className="text-xs sm:text-sm text-neutral-600 leading-relaxed">
            Thời gian khóa giữ phòng độc quyền 15 phút của quý khách đã kết thúc. Phòng đã được hoàn trả về kho chung để đảm bảo công bằng cho các khách hàng khác.
          </p>
          <p className="text-xs text-neutral-500 font-medium pt-1">
            Quý khách vui lòng quay lại tìm kiếm để cập nhật lại tình trạng phòng và bảng giá mới nhất.
          </p>
        </div>

        <div className="p-3 bg-neutral-50 border border-neutral-200 rounded-2xl flex items-center justify-center gap-2 text-xs text-neutral-600">
          <Clock className="w-4 h-4 text-neutral-400 shrink-0" />
          <span>
            Tự động chuyển về tìm kiếm sau{' '}
            <strong className="text-red-600 font-mono font-bold text-sm">{countdown}s</strong>...
          </span>
        </div>

        <Button
          onClick={onRedirect}
          className="w-full py-3.5 bg-red-600 hover:bg-red-700 text-white font-bold text-sm rounded-xl flex items-center justify-center gap-2 shadow-sm cursor-pointer transition-all"
        >
          <span>Quay lại tìm kiếm phòng ngay</span>
          <ArrowRight className="w-4 h-4" />
        </Button>
      </div>
    </div>
  );
};

export default SessionExpiredModal;
