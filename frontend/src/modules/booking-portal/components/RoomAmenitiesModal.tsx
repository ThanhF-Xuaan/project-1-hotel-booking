import React, { useState } from 'react';
import {
  X,
  ChevronLeft,
  ChevronRight,
  Tv,
  Wifi,
  Wind,
  Coffee,
  Shield,
  Phone,
  Bed,
  Bath,
  Check,
  Sparkles,
} from 'lucide-react';
import type { RoomAmenityDto } from '../types/portal.types';

export interface RoomAmenitiesModalProps {
  isOpen: boolean;
  onClose: () => void;
  roomTypeName: string;
  description?: string | null;
  images: string[];
  amenities: RoomAmenityDto[];
  roomSizeM2?: number;
}

// Hàm chọn icon trực quan theo mã hoặc tên tiện ích
const getAmenityIcon = (name: string, code?: string) => {
  const lower = `${name} ${code || ''}`.toLowerCase();
  if (lower.includes('wifi') || lower.includes('mạng')) return <Wifi className="w-4 h-4 text-neutral-600" />;
  if (lower.includes('tv') || lower.includes('ti vi')) return <Tv className="w-4 h-4 text-neutral-600" />;
  if (lower.includes('điều hòa') || lower.includes('lạnh') || lower.includes('air'))
    return <Wind className="w-4 h-4 text-neutral-600" />;
  if (lower.includes('tắm') || lower.includes('sen') || lower.includes('bath'))
    return <Bath className="w-4 h-4 text-neutral-600" />;
  if (lower.includes('giường') || lower.includes('gối') || lower.includes('bed'))
    return <Bed className="w-4 h-4 text-neutral-600" />;
  if (lower.includes('sáng') || lower.includes('cafe') || lower.includes('coffee') || lower.includes('ăn'))
    return <Coffee className="w-4 h-4 text-neutral-600" />;
  if (lower.includes('thoại') || lower.includes('phone'))
    return <Phone className="w-4 h-4 text-neutral-600" />;
  if (lower.includes('két') || lower.includes('an toàn') || lower.includes('safe'))
    return <Shield className="w-4 h-4 text-neutral-600" />;
  return <Check className="w-4 h-4 text-neutral-600" />;
};

export const RoomAmenitiesModal: React.FC<RoomAmenitiesModalProps> = ({
  isOpen,
  onClose,
  roomTypeName,
  description,
  images,
  amenities,
  roomSizeM2,
}) => {
  const [currentImgIdx, setCurrentImgIdx] = useState(0);

  if (!isOpen) return null;

  const displayImages =
    images.length > 0
      ? images
      : ['https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80'];

  const handlePrev = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentImgIdx((prev) => (prev > 0 ? prev - 1 : displayImages.length - 1));
  };

  const handleNext = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentImgIdx((prev) => (prev < displayImages.length - 1 ? prev + 1 : 0));
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-2xl bg-white rounded-3xl shadow-2xl overflow-hidden border border-neutral-100 max-h-[90vh] flex flex-col animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-100">
          <h3 className="text-lg font-bold text-neutral-900 tracking-tight">{roomTypeName}</h3>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-neutral-100 hover:bg-neutral-200 text-neutral-600 flex items-center justify-center transition-colors cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content body */}
        <div className="p-6 overflow-y-auto space-y-6">
          {/* Photo Carousel */}
          <div className="relative h-64 sm:h-72 w-full rounded-2xl overflow-hidden bg-neutral-900 shadow-inner group">
            <img
              src={displayImages[currentImgIdx]}
              alt={roomTypeName}
              className="w-full h-full object-cover transition-all duration-300"
            />
            {displayImages.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={handlePrev}
                  className="absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 hover:bg-black/75 text-white flex items-center justify-center backdrop-blur-xs transition-colors cursor-pointer"
                  aria-label="Ảnh trước"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <button
                  type="button"
                  onClick={handleNext}
                  className="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 hover:bg-black/75 text-white flex items-center justify-center backdrop-blur-xs transition-colors cursor-pointer"
                  aria-label="Ảnh kế tiếp"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
                <div className="absolute bottom-3 right-3 px-2 py-0.5 rounded-md bg-black/60 text-white text-[11px] font-semibold backdrop-blur-xs">
                  {currentImgIdx + 1} / {displayImages.length}
                </div>
              </>
            )}
          </div>

          {/* Description */}
          <div className="text-xs sm:text-sm text-neutral-600 space-y-1">
            {roomSizeM2 && (
              <span className="font-bold text-neutral-900 mr-2">{roomSizeM2} m² |</span>
            )}
            <span>
              {description ||
                'Căn phòng được thiết kế sang trọng, tiện nghi hiện đại với không gian thoáng đãng, mang đến trải nghiệm nghỉ dưỡng hoàn hảo.'}
            </span>
          </div>

          {/* Amenities Grid */}
          <div className="space-y-3 pt-2 border-t border-neutral-100">
            <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-1.5">
              <Sparkles className="w-4 h-4 text-red-600" />
              <span>Tiện ích trong phòng:</span>
            </h4>

            {amenities.length === 0 ? (
              <p className="text-xs text-neutral-400 italic">Đang cập nhật danh mục tiện ích chi tiết.</p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {amenities.map((item, idx) => (
                  <div key={idx} className="flex items-center gap-2.5 text-xs text-neutral-700">
                    <span className="p-1.5 rounded-lg bg-neutral-100 shrink-0">
                      {getAmenityIcon(item.name, item.code)}
                    </span>
                    <span className="font-medium">{item.name}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-neutral-100 bg-neutral-50 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 text-xs font-bold text-neutral-700 hover:text-neutral-900 bg-white border border-neutral-300 rounded-xl hover:bg-neutral-100 transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};

export default RoomAmenitiesModal;
