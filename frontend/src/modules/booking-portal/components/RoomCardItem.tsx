import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Bed,
  Users,
  Sparkles,
  Check,
  Utensils,
  ShieldAlert,
  Loader2,
} from 'lucide-react';
import { cn } from '../../../core/utils/cn';
import { hotelRoomTypeService } from '../../inventory/services/hotelRoomType.service';
import bookingSearchService from '../services/bookingSearchService';
import { RoomAmenitiesModal } from './RoomAmenitiesModal';
import { RoomPolicyModal } from './RoomPolicyModal';
import type {
  AvailableRoomTypeApiResponse,
  SelectedRoomCartItem,
  SelectedRoomConfig,
} from '../types/portal.types';

export interface RoomCardItemProps {
  room: AvailableRoomTypeApiResponse;
  checkInDate: string;
  checkOutDate: string;
  voucherCode?: string;
  checkInTime?: string;
  checkOutTime?: string;
  currentCartItem?: SelectedRoomCartItem;
  onUpdateCartItem: (cartItem: SelectedRoomCartItem | null) => void;
}

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

export const RoomCardItem: React.FC<RoomCardItemProps> = ({
  room,
  checkInDate,
  checkOutDate,
  voucherCode,
  checkInTime,
  checkOutTime,
  currentCartItem,
  onUpdateCartItem,
}) => {
  const [images, setImages] = useState<string[]>([]);
  const [currentImgIdx, setCurrentImgIdx] = useState(0);
  const [isAmenitiesModalOpen, setIsAmenitiesModalOpen] = useState(false);
  const [isPolicyModalOpen, setIsPolicyModalOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(Boolean(currentCartItem && currentCartItem.quantity > 0));
  const [isCalculatingPrice, setIsCalculatingPrice] = useState(false);

  // Load ảnh của hạng phòng từ API public
  useEffect(() => {
    let isCancelled = false;
    hotelRoomTypeService
      .getPublicImages(room.hotelRoomTypeId)
      .then((res) => {
        if (isCancelled) return;
        if (res && res.result && res.result.length > 0) {
          setImages(res.result.map((img) => img.url));
        } else {
          setImages([
            'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80',
          ]);
        }
      })
      .catch(() => {
        if (isCancelled) return;
        setImages([
          'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80',
        ]);
      });

    return () => {
      isCancelled = true;
    };
  }, [room.hotelRoomTypeId]);

  // Cấu hình giới hạn người tối đa theo DB
  const maxAdults = room.maxAdults || 2;
  const maxChildren = room.maxChildren || 0;
  const maxInfants = room.maxInfants || 0;
  const maxTotalGuests = room.maxTotalGuests || maxAdults + maxChildren || 3;

  // Chuỗi mô tả sức chứa chuẩn hóa đồng bộ 100% cho TẤT CẢ các hạng phòng theo trần tổng khách, triệt tiêu hiểu nhầm
  const capacityDescription = useMemo(() => {
    if (maxChildren === 0) {
      return `Tối đa ${maxTotalGuests} khách (${maxAdults} người lớn)`;
    }
    // Mọi hạng phòng có nhận trẻ em đều hiển thị đồng bộ: Trần tổng khách kèm giới hạn người lớn
    return `Tối đa ${maxTotalGuests} khách (tối đa ${maxAdults} người lớn)`;
  }, [maxAdults, maxChildren, maxTotalGuests]);

  // Tính số đêm lưu trú
  const totalNights = useMemo(() => {
    const start = new Date(checkInDate).getTime();
    const end = new Date(checkOutDate).getTime();
    const diff = Math.round((end - start) / 86400000);
    return diff > 0 ? diff : 1;
  }, [checkInDate, checkOutDate]);

  // Giá cơ bản mỗi đêm
  const basePricePerNight = useMemo(() => {
    const dailyPrice = room.priceBreakdown?.dailyPrices?.[0]?.grossDailyTotal;
    if (dailyPrice && Number(dailyPrice) > 0) {
      return Number(dailyPrice);
    }
    const finalTotal = room.priceBreakdown?.finalTotalAmount;
    if (finalTotal && Number(finalTotal) > 0) {
      return Math.round(Number(finalTotal) / totalNights);
    }
    return 1500000;
  }, [room.priceBreakdown, totalNights]);

  // Số lượng phòng đã chọn: lấy trực tiếp từ cart nếu có
  const selectedQuantity = currentCartItem?.quantity || 0;

  // Danh sách cấu hình khách cục bộ khi chưa chọn phòng
  const [localGuests, setLocalGuests] = useState<SelectedRoomConfig[]>([
    {
      roomIndex: 1,
      adults: Math.min(2, maxAdults),
      children: 0,
      infants: 0,
    },
  ]);

  const activeGuests = useMemo(() => {
    if (currentCartItem?.rooms && currentCartItem.rooms.length > 0) {
      return currentCartItem.rooms;
    }
    return localGuests;
  }, [currentCartItem?.rooms, localGuests]);

  // Hàm tính toán báo giá và cập nhật giỏ hàng theo sự kiện người dùng
  const recomputeAndApply = useCallback(
    async (qty: number, guestConfigs: SelectedRoomConfig[]) => {
      if (qty <= 0) {
        onUpdateCartItem(null);
        return;
      }

      setIsCalculatingPrice(true);

      const targetConfigs = guestConfigs.slice(0, qty);
      const quotePromises = targetConfigs.map((guestConfig) =>
        bookingSearchService
          .calculateRoomQuote({
            hotelRoomTypeId: room.hotelRoomTypeId,
            checkInDate,
            checkOutDate,
            adults: guestConfig.adults,
            children: guestConfig.children,
            infants: guestConfig.infants,
            roomsCount: 1,
            voucherCode,
          })
          .then((res) => res?.result)
          .catch((err) => {
            console.warn('Báo giá phòng thất bại, dùng giá cơ bản:', err);
            return null;
          })
      );

      try {
        const quotes = await Promise.all(quotePromises);
        let totalSum = 0;
        quotes.forEach((q) => {
          if (q && q.finalTotalAmount != null) {
            totalSum += Number(q.finalTotalAmount);
          } else {
            totalSum += basePricePerNight * totalNights;
          }
        });

        const updatedItem: SelectedRoomCartItem = {
          hotelRoomTypeId: room.hotelRoomTypeId,
          roomTypeName: room.roomTypeName,
          roomTypeCode: room.roomTypeCode,
          quantity: qty,
          rooms: targetConfigs,
          estimatedTotal: totalSum,
          perNightPrice: Math.round(totalSum / (totalNights * qty)),
          quote: quotes[0] || undefined,
        };

        onUpdateCartItem(updatedItem);
      } finally {
        setIsCalculatingPrice(false);
      }
    },
    [
      onUpdateCartItem,
      room.hotelRoomTypeId,
      room.roomTypeName,
      room.roomTypeCode,
      checkInDate,
      checkOutDate,
      voucherCode,
      basePricePerNight,
      totalNights,
    ]
  );

  // Thay đổi số lượng phòng
  const handleQuantityChange = (newQty: number) => {
    if (newQty <= 0) {
      onUpdateCartItem(null);
      return;
    }
    setIsExpanded(true);
    const next: SelectedRoomConfig[] = [...activeGuests];
    while (next.length < newQty) {
      next.push({
        roomIndex: next.length + 1,
        adults: Math.min(2, maxAdults),
        children: 0,
        infants: 0,
      });
    }
    setLocalGuests(next);
    void recomputeAndApply(newQty, next);
  };

  // Thay đổi số người lớn cho phòng thứ `index`
  const handleAdultChange = (index: number, adults: number) => {
    const next = [...activeGuests];
    const cur = { ...next[index], adults };

    // Ràng buộc động: Tổng khách = adults + children <= maxTotalGuests
    const availableMaxChildren = Math.max(0, Math.min(maxChildren, maxTotalGuests - adults));
    if (cur.children > availableMaxChildren) {
      cur.children = availableMaxChildren;
    }
    next[index] = cur;
    setLocalGuests(next);
    if (selectedQuantity > 0) {
      void recomputeAndApply(selectedQuantity, next);
    }
  };

  // Thay đổi số trẻ em cho phòng thứ `index`
  const handleChildrenChange = (index: number, children: number) => {
    const next = [...activeGuests];
    next[index] = { ...next[index], children };
    setLocalGuests(next);
    if (selectedQuantity > 0) {
      void recomputeAndApply(selectedQuantity, next);
    }
  };

  // Thay đổi số trẻ sơ sinh cho phòng thứ `index`
  const handleInfantsChange = (index: number, infants: number) => {
    const next = [...activeGuests];
    next[index] = { ...next[index], infants };
    setLocalGuests(next);
    if (selectedQuantity > 0) {
      void recomputeAndApply(selectedQuantity, next);
    }
  };

  // Kiểm tra gói có bao gồm ăn sáng thật hay không từ inclusions
  const breakfastInclusion = useMemo(() => {
    return (room.inclusions || []).find((i) =>
      /ăn sáng|buffet|breakfast/i.test(i.name || i.description || '')
    );
  }, [room.inclusions]);

  const maxSelectableRooms = Math.min(room.availableRooms || 1, 10);

  return (
    <div className="bg-white rounded-3xl border border-neutral-200 overflow-hidden shadow-xs hover:border-neutral-300 transition-all">
      {/* KHỐI 1: HEADER THẺ PHÒNG (Ảnh bên trái + Thông tin bên phải) */}
      <div className="p-4 sm:p-5 flex flex-col md:flex-row gap-5">
        {/* Ảnh phòng & Nút lướt ảnh nhanh < > */}
        <div className="relative w-full md:w-72 h-48 sm:h-52 rounded-2xl overflow-hidden bg-neutral-900 shrink-0 group">
          <img
            src={images[currentImgIdx] || images[0]}
            alt={room.roomTypeName}
            className="w-full h-full object-cover transition-all duration-300"
          />
          {images.length > 1 && (
            <>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setCurrentImgIdx((prev) => (prev > 0 ? prev - 1 : images.length - 1));
                }}
                className="absolute left-2 top-1/2 -translate-y-1/2 w-7 h-7 rounded-full bg-black/60 hover:bg-black/90 text-white flex items-center justify-center backdrop-blur-xs transition-colors cursor-pointer"
                aria-label="Ảnh trước"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setCurrentImgIdx((prev) => (prev < images.length - 1 ? prev + 1 : 0));
                }}
                className="absolute right-2 top-1/2 -translate-y-1/2 w-7 h-7 rounded-full bg-black/60 hover:bg-black/90 text-white flex items-center justify-center backdrop-blur-xs transition-colors cursor-pointer"
                aria-label="Ảnh sau"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
              <div className="absolute bottom-2 right-2 px-1.5 py-0.5 rounded-md bg-black/70 text-white text-[10px] font-semibold">
                {currentImgIdx + 1}/{images.length}
              </div>
            </>
          )}
        </div>

        {/* Thông tin phòng */}
        <div className="flex-1 flex flex-col justify-between space-y-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 bg-neutral-900 text-white text-[10px] font-bold rounded-md uppercase">
                {room.roomTypeCode}
              </span>
              <span className="text-xs text-neutral-500">• Còn {room.availableRooms} phòng trống</span>
            </div>

            <h3 className="text-lg sm:text-xl font-bold text-neutral-900 mt-1">
              {room.roomTypeName}
            </h3>

            {/* Giường & Khách */}
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-neutral-600 mt-1.5">
              <span className="flex items-center gap-1 font-medium">
                <Bed className="w-3.5 h-3.5 text-neutral-400" />
                {room.maxBeds ? `${room.maxBeds} Giường đôi` : '1 Giường đôi King/Queen'}
                {room.extraBeds ? ` (+${room.extraBeds} giường phụ)` : ''}
              </span>
              <span className="flex items-center gap-1 font-medium">
                <Users className="w-3.5 h-3.5 text-neutral-400" />
                {capacityDescription}
              </span>
            </div>

            {/* Tiện nghi rút gọn & Nút mở modal tiện ích */}
            <div className="flex flex-wrap items-center gap-2 mt-3">
              {(room.amenities || []).slice(0, 3).map((a, idx) => (
                <span
                  key={idx}
                  className="px-2 py-1 bg-neutral-100 rounded-lg text-[11px] font-medium text-neutral-700 flex items-center gap-1"
                >
                  <Check className="w-3 h-3 text-emerald-600" />
                  {a.name}
                </span>
              ))}
              <button
                type="button"
                onClick={() => setIsAmenitiesModalOpen(true)}
                className="text-[11px] text-red-600 hover:text-red-700 font-bold hover:underline cursor-pointer inline-flex items-center gap-1 ml-1"
              >
                <span>Xem tất cả tiện nghi</span>
                <ChevronRight className="w-3 h-3" />
              </button>
            </div>
          </div>

          {/* Dòng giá & Nút "Chọn phòng" */}
          <div className="pt-3 border-t border-neutral-100 flex items-center justify-between gap-4">
            <div>
              <p className="text-[11px] text-neutral-500">Giá chỉ từ</p>
              <p className="text-lg font-extrabold text-neutral-900 leading-tight">
                {formatCurrency(basePricePerNight)}
                <span className="text-xs font-normal text-neutral-500 ml-1">/ đêm</span>
              </p>
            </div>

            <button
              type="button"
              onClick={() => {
                if (!isExpanded) {
                  setIsExpanded(true);
                  if (selectedQuantity === 0) {
                    handleQuantityChange(1);
                  }
                } else {
                  setIsExpanded(false);
                }
              }}
              className={cn(
                'px-5 py-2.5 rounded-xl font-bold text-xs sm:text-sm flex items-center gap-2 transition-all cursor-pointer shadow-xs',
                isExpanded
                  ? 'bg-neutral-800 text-white hover:bg-neutral-900'
                  : 'bg-red-600 hover:bg-red-700 text-white'
              )}
            >
              <span>{isExpanded ? 'Thu gọn' : 'Chọn phòng'}</span>
              <ChevronDown
                className={cn('w-4 h-4 transition-transform duration-200', isExpanded && 'rotate-180')}
              />
            </button>
          </div>
        </div>
      </div>

      {/* KHỐI 2: KHỐI MỞ RỘNG (GÓI GIÁ & CHỌN SỐ PHÒNG / SỐ NGƯỜI) - GIỐNG ẢNH 5, 6, 7 */}
      {isExpanded && (
        <div className="border-t border-neutral-200 bg-neutral-50/70 p-4 sm:p-5 space-y-4 animate-in fade-in duration-200">
          {/* Rate Plan Card */}
          <div className="bg-white rounded-2xl border border-neutral-200 p-4 sm:p-5 shadow-2xs space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              {/* Tên gói & dịch vụ */}
              <div className="space-y-1.5">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-red-600 shrink-0" />
                  <h4 className="font-bold text-neutral-900 text-sm sm:text-base">
                    {room.roomTypeName} - Giá tiêu chuẩn công khai
                  </h4>
                </div>

                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-neutral-600">
                  {breakfastInclusion ? (
                    <span className="flex items-center gap-1 text-emerald-700 font-semibold">
                      <Utensils className="w-3.5 h-3.5 text-emerald-600" />
                      {breakfastInclusion.name || 'Đã bao gồm ăn sáng'}
                    </span>
                  ) : (
                    <span className="text-neutral-500">Phòng không bao gồm bữa sáng</span>
                  )}
                  <span>•</span>
                  <span className="flex items-center gap-1 text-neutral-600">
                    <ShieldAlert className="w-3.5 h-3.5 text-neutral-400" />
                    Chính sách linh hoạt theo cơ sở
                  </span>
                </div>

                <div>
                  <button
                    type="button"
                    onClick={() => setIsPolicyModalOpen(true)}
                    className="text-xs text-red-600 hover:text-red-700 font-bold hover:underline cursor-pointer inline-flex items-center gap-0.5"
                  >
                    <span>Xem chi tiết chính sách & phụ thu</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Giá & Dropdown chọn số phòng */}
              <div className="flex items-center gap-4 sm:gap-6 justify-between sm:justify-end shrink-0 pt-2 sm:pt-0 border-t sm:border-0 border-neutral-100">
                <div className="text-right">
                  <div className="flex items-center gap-1 text-xs text-neutral-500 justify-end">
                    <Users className="w-3.5 h-3.5" />
                    <span>{capacityDescription}</span>
                  </div>
                  <p className="text-base sm:text-lg font-extrabold text-neutral-900">
                    {formatCurrency(basePricePerNight)}
                    <span className="text-xs font-normal text-neutral-500 ml-1">/ đêm</span>
                  </p>
                </div>

                {/* Dropdown số phòng */}
                <div className="w-32">
                  <select
                    value={selectedQuantity}
                    onChange={(e) => handleQuantityChange(Number(e.target.value))}
                    className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-xl text-xs sm:text-sm font-bold text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600 cursor-pointer shadow-2xs"
                  >
                    <option value={0}>0 Phòng</option>
                    {[...Array(maxSelectableRooms)].map((_, i) => (
                      <option key={i + 1} value={i + 1}>
                        {i + 1} Phòng
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* KHỐI CẤU HÌNH SỐ NGƯỜI TỪNG PHÒNG KHI ĐÃ CHỌN >= 1 PHÒNG (ẢNH 6, 7) */}
            {selectedQuantity > 0 && (
              <div className="pt-4 border-t border-neutral-100 space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
                  <div>
                    <p className="text-xs font-bold text-neutral-700 uppercase tracking-wider">
                      Cấu hình số lượng khách từng phòng ({selectedQuantity} phòng):
                    </p>
                    {maxChildren > 0 && (
                      <p className="text-[11px] text-neutral-500">
                        * Sức chứa tối đa: {maxTotalGuests} khách/phòng (tổng người lớn & trẻ em)
                      </p>
                    )}
                  </div>
                  {isCalculatingPrice && (
                    <span className="text-[11px] text-red-600 flex items-center gap-1 font-semibold animate-pulse shrink-0">
                      <Loader2 className="w-3 h-3 animate-spin" />
                      Đang tính lại giá...
                    </span>
                  )}
                </div>

                <div className="space-y-2.5">
                  {[...Array(selectedQuantity)].map((_, idx) => {
                    const guestCfg = activeGuests[idx] || {
                      adults: Math.min(2, maxAdults),
                      children: 0,
                      infants: 0,
                    };

                    // Ràng buộc động theo yêu cầu của user:
                    // Max Trẻ em = min(maxChildren, maxTotalGuests - adults)
                    const availableChildrenCount = Math.max(
                      0,
                      Math.min(maxChildren, maxTotalGuests - guestCfg.adults)
                    );

                    return (
                      <div
                        key={idx}
                        className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 flex flex-wrap items-center justify-between gap-3 text-xs"
                      >
                        <div className="flex items-center gap-2 min-w-[130px]">
                          <span className="font-bold text-neutral-800">
                            Phòng {idx + 1}:
                          </span>
                          <span
                            className={cn(
                              'px-1.5 py-0.5 rounded-md text-[10px] font-semibold',
                              guestCfg.adults + guestCfg.children >= maxTotalGuests
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-neutral-200 text-neutral-700'
                            )}
                          >
                            {guestCfg.adults + guestCfg.children}/{maxTotalGuests} khách
                          </span>
                        </div>

                        <div className="flex flex-wrap items-center gap-3 flex-1 justify-end">
                          {/* Người lớn */}
                          <div className="flex items-center gap-1.5">
                            <label className="text-[11px] text-neutral-500 font-medium">Người lớn:</label>
                            <select
                              value={guestCfg.adults}
                              onChange={(e) => handleAdultChange(idx, Number(e.target.value))}
                              className="h-8 px-2 bg-white border border-neutral-300 rounded-lg text-xs font-semibold text-neutral-900 focus:outline-none focus:ring-1 focus:ring-red-600 cursor-pointer"
                            >
                              {[...Array(maxAdults)].map((_, i) => (
                                <option key={i + 1} value={i + 1}>
                                  {i + 1}
                                </option>
                              ))}
                            </select>
                          </div>

                          {/* Trẻ em (6 - 11 tuổi) */}
                          <div className="flex items-center gap-1.5">
                            <label className="text-[11px] text-neutral-500 font-medium">
                              Trẻ em (6-11 tuổi):
                            </label>
                            <select
                              value={guestCfg.children}
                              onChange={(e) => handleChildrenChange(idx, Number(e.target.value))}
                              className="h-8 px-2 bg-white border border-neutral-300 rounded-lg text-xs font-semibold text-neutral-900 focus:outline-none focus:ring-1 focus:ring-red-600 cursor-pointer"
                            >
                              {[...Array(availableChildrenCount + 1)].map((_, i) => (
                                <option key={i} value={i}>
                                  {i}
                                </option>
                              ))}
                            </select>
                          </div>

                          {/* Em bé (0 - 5 tuổi) */}
                          <div className="flex items-center gap-1.5">
                            <label className="text-[11px] text-neutral-500 font-medium">
                              Em bé (0-5 tuổi):
                            </label>
                            <select
                              value={guestCfg.infants}
                              onChange={(e) => handleInfantsChange(idx, Number(e.target.value))}
                              className="h-8 px-2 bg-white border border-neutral-300 rounded-lg text-xs font-semibold text-neutral-900 focus:outline-none focus:ring-1 focus:ring-red-600 cursor-pointer"
                            >
                              {[...Array(Math.max(1, maxInfants) + 1)].map((_, i) => (
                                <option key={i} value={i}>
                                  {i}
                                </option>
                              ))}
                            </select>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL TIỆN NGHI */}
      <RoomAmenitiesModal
        isOpen={isAmenitiesModalOpen}
        onClose={() => setIsAmenitiesModalOpen(false)}
        roomTypeName={room.roomTypeName}
        description={room.description}
        images={images}
        amenities={room.amenities || []}
      />

      {/* MODAL CHÍNH SÁCH */}
      <RoomPolicyModal
        isOpen={isPolicyModalOpen}
        onClose={() => setIsPolicyModalOpen(false)}
        roomTypeName={room.roomTypeName}
        checkInTime={checkInTime}
        checkOutTime={checkOutTime}
        surchargePolicies={room.surchargePolicies || []}
      />
    </div>
  );
};

export default RoomCardItem;
