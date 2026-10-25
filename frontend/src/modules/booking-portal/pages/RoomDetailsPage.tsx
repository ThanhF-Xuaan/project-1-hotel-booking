import React, { useEffect, useState } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import {
  Building2,
  Calendar,
  Users,
  Bed,
  CheckCircle,
  ArrowLeft,
  Sparkles,
  ShieldCheck,
  Info,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import { apiClient } from '../../../core/api/client';

interface HotelDetail {
  id: number;
  name: string;
  address: string;
  phone: string;
  serviceFeePercent?: number;
}

interface RoomAmenity {
  code: string;
  name: string;
  category?: string;
}

interface AvailableRoomType {
  hotelRoomTypeId: number;
  roomTypeCode: string;
  roomTypeName: string;
  maxAdults: number;
  maxChildren: number;
  maxTotalGuests: number;
  availableRooms: number;
  amenities?: RoomAmenity[];
  description?: string;
}

interface RoomPriceQuote {
  hotelRoomTypeId: number;
  priceBreakdown?: {
    finalTotalAmount?: number | string | null;
    totalBasePrice?: number | string | null;
    totalRoomVat?: number | string | null;
    totalRoomServiceFee?: number | string | null;
  };
}

const DEFAULT_CHECK_IN = new Date(Date.now() + 86400000).toISOString().split('T')[0];
const DEFAULT_CHECK_OUT = new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0];

export const RoomDetailsPage: React.FC = () => {
  const { hotelId, hotelRoomTypeId } = useParams<{
    hotelId: string;
    hotelRoomTypeId: string;
  }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const checkInDate = searchParams.get('checkInDate') || DEFAULT_CHECK_IN;
  const checkOutDate = searchParams.get('checkOutDate') || DEFAULT_CHECK_OUT;
  const adults = parseInt(searchParams.get('adults') || '2', 10);
  const children = parseInt(searchParams.get('children') || '0', 10);
  const infants = parseInt(searchParams.get('infants') || '0', 10);
  const extraBeds = parseInt(searchParams.get('extraBeds') || '0', 10);
  const roomsCount = parseInt(searchParams.get('roomsCount') || '1', 10);

  const [hotel, setHotel] = useState<HotelDetail | null>(null);
  const [room, setRoom] = useState<AvailableRoomType | null>(null);
  const [quote, setQuote] = useState<RoomPriceQuote | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let isCancelled = false;

    const fetchData = async () => {
      setIsLoading(true);
      setLoadError(null);
      try {
        const parsedHotelId = parseInt(hotelId || '1', 10);
        const parsedRoomTypeId = parseInt(hotelRoomTypeId || '1', 10);

        // Fetch hotel details
        const hotelPromise = apiClient
          .get<HotelDetail>(`/api/v1/public/hotels/${parsedHotelId}`)
          .then((res) => res.result)
          .catch(() => ({
            id: parsedHotelId,
            name: `Khách sạn #${parsedHotelId}`,
            address: 'Địa chỉ đang cập nhật',
            phone: '1900 xxxx',
          }));

        // Load the exact selected room type from the current availability response.
        const roomsPromise = apiClient
          .post<AvailableRoomType[]>('/api/v1/public/booking-search/rooms', {
            hotelId: parsedHotelId,
            checkInDate,
            checkOutDate,
            roomsCount,
          })
          .then((res) => res.result || [])
          .catch(() => null);

        const [hotelData, roomList] = await Promise.all([hotelPromise, roomsPromise]);
        const roomData = roomList?.find((r) => r.hotelRoomTypeId === parsedRoomTypeId) || null;
        let quoteData: RoomPriceQuote | null = null;

        if (roomData) {
          try {
            const quoteResponse = await apiClient.post<RoomPriceQuote>(
              '/api/v1/public/booking-search/quote',
              {
                hotelRoomTypeId: parsedRoomTypeId,
                checkInDate,
                checkOutDate,
                adults,
                children,
                infants,
                extraBeds,
              }
            );
            quoteData = quoteResponse.result;
          } catch {
            quoteData = null;
          }
        }

        if (!isCancelled) {
          setHotel(hotelData);
          setRoom(roomData);
          setQuote(quoteData);
          if (roomList === null) {
            setLoadError('Không thể kiểm tra tình trạng phòng từ hệ thống Core. Vui lòng thử lại.');
          } else if (!roomData) {
            setLoadError('Hạng phòng đã chọn hiện không còn khả dụng cho ngày lưu trú này.');
          }
          setIsLoading(false);
        }
      } catch (err: unknown) {
        if (!isCancelled) {
          setLoadError(
            err instanceof Error
              ? err.message
              : 'Không thể tải thông tin chi tiết phòng.'
          );
          setIsLoading(false);
        }
      }
    };

    void fetchData();

    return () => {
      isCancelled = true;
    };
  }, [hotelId, hotelRoomTypeId, checkInDate, checkOutDate, adults, children, infants, extraBeds, roomsCount]);

  const formatVND = (amount: number | string) => {
    const numericAmount = typeof amount === 'number' ? amount : Number(amount);
    if (!Number.isFinite(numericAmount)) {
      return 'Chưa có báo giá';
    }
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(numericAmount);
  };

  const calculateNights = () => {
    try {
      const start = new Date(checkInDate).getTime();
      const end = new Date(checkOutDate).getTime();
      const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
      return diff > 0 ? diff : 1;
    } catch {
      return 1;
    }
  };

  const nights = calculateNights();
  const quoteTotal = quote?.priceBreakdown?.finalTotalAmount;

  return (
    <main className="min-h-screen bg-neutral-50 pb-16">
      {/* Top Header */}
      <header className="bg-white border-b border-neutral-200 sticky top-0 z-20 shadow-2xs">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <button
            onClick={() => navigate('/portal/search')}
            aria-label="Quay lại trang tìm kiếm"
            className="flex items-center gap-2 text-sm font-semibold text-neutral-700 hover:text-red-600 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại tìm kiếm</span>
          </button>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-red-600 text-white flex items-center justify-center font-extrabold text-sm shadow-xs">
              H
            </div>
            <span className="font-bold text-neutral-900 text-sm">HOTEL GRAND</span>
          </div>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-4 pt-6">
        {isLoading && (
          <div className="py-20 text-center">
            <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-sm text-neutral-600 font-medium">
              Đang tải chi tiết hạng phòng và kiểm tra báo giá Core...
            </p>
          </div>
        )}

        {loadError && !isLoading && (
          <div className="bg-red-50 border border-red-200 rounded-2xl p-6 text-center max-w-lg mx-auto my-12">
            <p className="text-sm font-semibold text-red-800 mb-4">{loadError}</p>
            <Button
              onClick={() => navigate('/portal/search')}
              variant="outline"
              size="sm"
            >
              Quay lại trang chủ
            </Button>
          </div>
        )}

        {!isLoading && !loadError && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Main Content (2 cols) */}
            <div className="lg:col-span-2 space-y-6">
              {/* Hotel & Room Title Banner */}
              <section className="bg-white rounded-2xl border border-neutral-200 p-6 shadow-xs">
                <div className="flex items-center gap-2 text-xs font-semibold text-red-600 mb-2">
                  <Building2 className="w-4 h-4" />
                  <span>{hotel?.name || 'Khách sạn liên kết'}</span>
                </div>
                <h1 className="text-2xl md:text-3xl font-extrabold text-neutral-900 leading-tight">
                  {room?.roomTypeName || 'Hạng phòng cao cấp'}
                </h1>
                <p className="text-xs text-neutral-500 mt-1">
                  Mã hạng phòng: <span className="font-semibold text-neutral-700">{room?.roomTypeCode || 'ROOM'}</span> • {hotel?.address}
                </p>

                {/* Key specs badge row */}
                <div className="grid grid-cols-3 gap-3 mt-6 pt-6 border-t border-neutral-100">
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0">
                      <Users className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-[11px] text-neutral-500">Sức chứa</div>
                      <div className="text-xs font-bold text-neutral-800">
                        {room?.maxAdults ?? adults} Lớn, {room?.maxChildren ?? children} Trẻ
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0">
                      <Bed className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-[11px] text-neutral-500">Loại giường</div>
                      <div className="text-xs font-bold text-neutral-800">Core chưa cung cấp thông tin loại giường</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0">
                      <Calendar className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-[11px] text-neutral-500">Thời gian</div>
                      <div className="text-xs font-bold text-neutral-800">{nights} đêm lưu trú</div>
                    </div>
                  </div>
                </div>
              </section>

              {/* Amenities & Features */}
              <section className="bg-white rounded-2xl border border-neutral-200 p-6 shadow-xs space-y-4">
                <h2 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-red-600" />
                  Tiện nghi & Dịch vụ đi kèm
                </h2>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-2">
                  {room?.amenities && room.amenities.length > 0 ? (
                    room.amenities.map((amenity) => (
                      <div
                        key={amenity.code}
                        className="flex items-center gap-2 text-xs text-neutral-700 bg-neutral-50 p-2.5 rounded-xl border border-neutral-200"
                      >
                        <CheckCircle className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span className="font-medium truncate">{amenity.name}</span>
                      </div>
                    ))
                  ) : (
                    <p className="col-span-full text-xs text-neutral-500">Chưa có thông tin tiện nghi cho hạng phòng này.</p>
                  )}
                </div>
              </section>

              {/* Policies & Invariants Notice */}
              <section className="bg-amber-50/70 border border-amber-200 rounded-2xl p-5 flex items-start gap-3">
                <Info className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
                <div className="text-xs text-amber-900 space-y-1">
                  <div className="font-bold">Lưu ý về quy trình kiểm tra báo giá</div>
                  <p className="text-amber-800 leading-relaxed">
                    Trang này cung cấp thông tin chi tiết và tính toán báo giá thực tế từ hệ thống Core.
                    Không có hành động giữ phòng, đặt phòng hoặc thanh toán nào được thực hiện tại bước này.
                  </p>
                </div>
              </section>
            </div>

            {/* Price Quote Sidebar (1 col) */}
            <aside className="space-y-6">
              <div className="bg-white rounded-2xl border border-neutral-200 p-6 shadow-sm sticky top-24 space-y-5">
                <div className="border-b border-neutral-100 pb-4">
                  <div className="text-xs text-neutral-500 font-medium">Báo giá xác thực từ Core</div>
                  <div className="text-2xl font-extrabold text-red-600 mt-1">
                    {quoteTotal !== undefined && quoteTotal !== null && Number.isFinite(Number(quoteTotal))
                      ? formatVND(quoteTotal)
                      : 'Chưa có báo giá'}
                  </div>
                  <div className="text-[11px] text-neutral-500 mt-1">
                    Cho {nights} đêm ({checkInDate} → {checkOutDate})
                  </div>
                </div>

                {/* Stay criteria summary */}
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between text-neutral-600">
                    <span>Nhận phòng:</span>
                    <span className="font-semibold text-neutral-900">{checkInDate}</span>
                  </div>
                  <div className="flex justify-between text-neutral-600">
                    <span>Trả phòng:</span>
                    <span className="font-semibold text-neutral-900">{checkOutDate}</span>
                  </div>
                  <div className="flex justify-between text-neutral-600">
                    <span>Khách lưu trú:</span>
                    <span className="font-semibold text-neutral-900">
                    {adults} Người lớn{children > 0 ? `, ${children} Trẻ em` : ''}{infants > 0 ? `, ${infants} Trẻ sơ sinh` : ''}
                    </span>
                  </div>
                  <div className="flex justify-between text-neutral-600">
                    <span>Số lượng phòng:</span>
                    <span className="font-semibold text-neutral-900">{roomsCount} phòng</span>
                  </div>
                </div>

                {/* Breakdown */}
                {quote && (
                  <div className="pt-3 border-t border-neutral-100 space-y-1.5 text-xs">
                    {quote.priceBreakdown?.totalBasePrice !== undefined && quote.priceBreakdown.totalBasePrice !== null && (
                      <div className="flex justify-between text-neutral-500">
                        <span>Giá gốc:</span>
                        <span>{formatVND(quote.priceBreakdown.totalBasePrice)}</span>
                      </div>
                    )}
                    {quote.priceBreakdown?.totalRoomVat !== undefined && quote.priceBreakdown.totalRoomVat !== null && (
                      <div className="flex justify-between text-neutral-500">
                        <span>Thuế VAT:</span>
                        <span>{formatVND(quote.priceBreakdown.totalRoomVat)}</span>
                      </div>
                    )}
                    {quote.priceBreakdown?.totalRoomServiceFee !== undefined && quote.priceBreakdown.totalRoomServiceFee !== null && (
                      <div className="flex justify-between text-neutral-500">
                        <span>Phí dịch vụ:</span>
                        <span>{formatVND(quote.priceBreakdown.totalRoomServiceFee)}</span>
                      </div>
                    )}
                  </div>
                )}

                <div className="pt-2">
                  <Button
                    onClick={() => navigate('/portal/search')}
                    variant="outline"
                    className="w-full font-bold text-xs"
                    aria-label="Chọn phòng khác tại trang tìm kiếm"
                  >
                    Chọn phòng khác
                  </Button>
                </div>

                <div className="flex items-center gap-1.5 text-[11px] text-neutral-500 justify-center">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Dữ liệu phòng được xác thực trực tiếp</span>
                </div>
              </div>
            </aside>
          </div>
        )}
      </div>
    </main>
  );
};

export default RoomDetailsPage;
