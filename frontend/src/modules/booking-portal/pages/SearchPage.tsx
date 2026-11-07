import React, { useState, useEffect } from 'react';
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
  Users,
  Camera,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Select from '../../../core/components/ui/Select';
import PublicFooter from '../../../core/components/layout/PublicFooter';
import type { CustomerSearchCriteria, AvailableRoomOffer } from '../types/portal.types';
import { RoomAdvisorWidget } from '../components/RoomAdvisor/RoomAdvisorWidget';
import { GroupInquiryModal } from '../components/GroupInquiryModal';
import { HotelGalleryViewerModal } from '../components/HotelGalleryViewerModal';
import hotelService from '../../organization/services/hotel.service';
import type { HotelImageDto } from '../../organization/types/organization.types';
import type { AdvisorRecommendation, AdvisorSearchCriteria } from '../types/roomAdvisor.types';

const DEFAULT_CHECK_IN = new Date(Date.now() + 86400000).toISOString().split('T')[0];
const DEFAULT_CHECK_OUT = new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0];

const INITIAL_CRITERIA: CustomerSearchCriteria = {
  hotelId: 1,
  checkInDate: DEFAULT_CHECK_IN,
  checkOutDate: DEFAULT_CHECK_OUT,
  adultCount: 2,
  childCount: 0,
  roomsCount: 1,
};

export const SearchPage: React.FC = () => {
  const navigate = useNavigate();

  const [isAdvisorOpen, setIsAdvisorOpen] = useState(false);
  const [isGroupModalOpen, setIsGroupModalOpen] = useState(false);
  const [isGalleryOpen, setIsGalleryOpen] = useState(false);
  const [hotelImages, setHotelImages] = useState<HotelImageDto[]>([]);

  const [criteria, setCriteria] = useState<CustomerSearchCriteria>(INITIAL_CRITERIA);

  useEffect(() => {
    if (!criteria.hotelId) return;
    hotelService
      .getPublicHotelImages(criteria.hotelId)
      .then((res) => {
        if (res && res.result) {
          setHotelImages(res.result);
        } else {
          setHotelImages([]);
        }
      })
      .catch((err) => {
        console.error('Lỗi khi tải ảnh khách sạn:', err);
        setHotelImages([]);
      });
  }, [criteria.hotelId]);

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

  const selectedHotelName =
    criteria.hotelId === 2
      ? 'Premier Đà Nẵng Resort'
      : criteria.hotelId === 3
      ? 'Boutique Sài Gòn Hotel'
      : 'Grand Hà Nội Hotel';

  const primaryCoverImage =
    hotelImages.find((img) => img.isPrimary)?.url ||
    hotelImages[0]?.url ||
    (criteria.hotelId === 2
      ? 'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?auto=format&fit=crop&w=1200&q=80'
      : criteria.hotelId === 3
      ? 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80'
      : 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=80');

  const handleSearchRooms = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if ((criteria.roomsCount ?? 1) >= 6) {
      setIsGroupModalOpen(true);
      return;
    }
    const el = document.getElementById('room-offers-list');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

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

  const handleAdvisorViewDetails = (rec: AdvisorRecommendation, advisorCriteria?: AdvisorSearchCriteria) => {
    const adults =
      advisorCriteria?.adults && advisorCriteria.adults >= 1
        ? advisorCriteria.adults
        : criteria.adultCount || 2;

    const hotelId = rec.room.hotelId;
    const hotelRoomTypeId = rec.room.hotelRoomTypeId;
    const checkIn = rec.stay?.checkInDate || criteria.checkInDate;
    const checkOut = rec.stay?.checkOutDate || criteria.checkOutDate;

    const children = advisorCriteria?.children ?? 0;
    const infants = advisorCriteria?.infants ?? 0;
    const extraBeds = advisorCriteria?.extraBeds ?? 0;
    const roomsCount = rec.stay?.roomsCount ?? 1;
    const query = new URLSearchParams({
      checkInDate: checkIn,
      checkOutDate: checkOut,
      adults: String(adults),
      children: String(children),
      infants: String(infants),
      extraBeds: String(extraBeds),
      roomsCount: String(roomsCount),
    });

    navigate(`/portal/hotels/${hotelId}/room-types/${hotelRoomTypeId}?${query.toString()}`);
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
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => navigate(`/portal/personalization?hotelId=${criteria.hotelId}`)}
              className="px-3.5 py-2 text-xs font-semibold text-neutral-700 hover:text-red-600 hover:bg-neutral-50 rounded-xl transition-colors flex items-center gap-1.5"
            >
              <ShieldCheck className="w-4 h-4" />
              Consent & gợi ý cá nhân hóa
            </button>
            <button
              onClick={() => setIsAdvisorOpen(true)}
              className="px-3.5 py-2 text-xs font-bold text-red-600 bg-red-50 hover:bg-red-100 rounded-xl transition-colors flex items-center gap-1.5 border border-red-200 shadow-2xs cursor-pointer"
            >
              <Sparkles className="w-4 h-4 fill-red-500 text-red-600" />
              Trợ lý AI Tìm Phòng
            </button>
            <button
              onClick={() => navigate('/portal/my-bookings')}
              className="px-3.5 py-2 text-xs font-semibold text-neutral-700 hover:text-red-600 hover:bg-neutral-50 rounded-xl transition-colors flex items-center gap-1.5"
            >
              <FileSearch className="w-4 h-4" />
              Tra cứu đơn đặt phòng
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
            <div className="pt-2 flex justify-center">
              <button
                onClick={() => setIsAdvisorOpen(true)}
                className="px-4 py-2 bg-neutral-800/90 hover:bg-neutral-800 text-white text-xs font-bold rounded-2xl border border-neutral-700 flex items-center gap-2 transition-all hover:scale-105 shadow-sm cursor-pointer"
              >
                <Sparkles className="w-4 h-4 text-red-400 fill-red-400" />
                Hỏi Trợ lý AI gợi ý phòng theo nhu cầu & ngân sách
                <ArrowRight className="w-3.5 h-3.5 text-neutral-400" />
              </button>
            </div>
          </div>

          {/* Search Box */}
          <div className="bg-white p-4 sm:p-5 rounded-3xl shadow-xl border border-neutral-100 text-neutral-900 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-600 mb-1 flex items-center gap-1">
                <Building2 className="w-3.5 h-3.5 text-red-600" />
                Khách sạn cơ sở
              </label>
              <Select<number>
                value={criteria.hotelId}
                onChange={(val) => setCriteria({ ...criteria, hotelId: Number(val) })}
                options={[
                  { value: 1, label: 'Grand Hà Nội Hotel' },
                  { value: 2, label: 'Premier Đà Nẵng Resort' },
                  { value: 3, label: 'Boutique Sài Gòn Hotel' },
                ]}
                size="md"
              />
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

            <div>
              <label className="block text-xs font-bold text-neutral-600 mb-1 flex items-center gap-1">
                <Users className="w-3.5 h-3.5 text-red-600" />
                Số lượng phòng
              </label>
              <Select<number>
                value={criteria.roomsCount ?? 1}
                onChange={(val) => {
                  const numVal = Number(val);
                  setCriteria({ ...criteria, roomsCount: numVal });
                  if (numVal >= 6) {
                    setIsGroupModalOpen(true);
                  }
                }}
                options={[
                  { value: 1, label: '1 Phòng (Khách lẻ)' },
                  { value: 2, label: '2 Phòng (Khách lẻ)' },
                  { value: 3, label: '3 Phòng (Khách lẻ)' },
                  { value: 4, label: '4 Phòng (Khách lẻ)' },
                  { value: 5, label: '5 Phòng (Tối đa FIT)' },
                  { value: 6, label: '6+ Phòng (Khách đoàn GIT)' },
                ]}
                size="md"
              />
            </div>

            <div className="flex items-end">
              <Button
                onClick={handleSearchRooms}
                className="w-full h-11 flex items-center justify-center gap-2 font-bold text-sm shadow-xs cursor-pointer"
              >
                <Search className="w-4 h-4" />
                Tìm Phòng Trống
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Hotel Spotlight Banner */}
      <div className="max-w-5xl mx-auto px-4 mt-8">
        <div className="relative rounded-3xl overflow-hidden border border-neutral-200 shadow-md group">
          <div className="h-52 sm:h-64 w-full overflow-hidden bg-neutral-900 relative">
            <img
              src={primaryCoverImage}
              alt={selectedHotelName}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 brightness-90"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent" />

            <div className="absolute bottom-5 left-5 right-5 flex flex-col sm:flex-row sm:items-end justify-between gap-4 text-white">
              <div>
                <div className="flex items-center gap-1.5 text-amber-400 mb-1">
                  {[...Array(5)].map((_, i) => (
                    <Star key={i} className="w-4 h-4 fill-amber-400 text-amber-400" />
                  ))}
                  <span className="text-xs font-semibold text-white/90 ml-1">Tiêu chuẩn 5 sao quốc tế</span>
                </div>
                <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">{selectedHotelName}</h2>
                <p className="text-xs sm:text-sm text-neutral-300 mt-1 flex items-center gap-1.5">
                  <Building2 className="w-3.5 h-3.5 text-red-500" />
                  Chuỗi khách sạn & nghỉ dưỡng Hotel Grand Collection
                </p>
              </div>

              <button
                type="button"
                onClick={() => setIsGalleryOpen(true)}
                className="px-4 py-2.5 bg-white/95 hover:bg-white text-neutral-900 rounded-2xl font-bold text-xs flex items-center gap-2 shadow-lg backdrop-blur-xs transition-all hover:scale-105 cursor-pointer shrink-0"
              >
                <Camera className="w-4 h-4 text-red-600" />
                <span>Xem hình ảnh khách sạn ({hotelImages.length > 0 ? `${hotelImages.length} ảnh` : 'Khám phá'})</span>
              </button>
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

        <div id="room-offers-list" className="space-y-4">
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

      {/* Group Booking Inquiry Modal (FR-SRCH-05: Blocks FIT >= 6 rooms) */}
      <GroupInquiryModal
        isOpen={isGroupModalOpen}
        onClose={() => setIsGroupModalOpen(false)}
        initialRoomsCount={criteria.roomsCount ?? 6}
        hotelName={selectedHotelName}
        checkInDate={criteria.checkInDate}
        checkOutDate={criteria.checkOutDate}
        onSwitchToFit={() => {
          setCriteria((prev) => ({ ...prev, roomsCount: 5 }));
          setIsGroupModalOpen(false);
        }}
      />

      {/* Floating AI Room Advisor Widget */}
      <RoomAdvisorWidget
        isOpenExternal={isAdvisorOpen}
        onCloseExternal={() => setIsAdvisorOpen(false)}
        onOpenExternal={() => setIsAdvisorOpen(true)}
        onViewRoomDetails={handleAdvisorViewDetails}
      />

      {/* Hotel Photo Gallery Modal */}
      <HotelGalleryViewerModal
        isOpen={isGalleryOpen}
        onClose={() => setIsGalleryOpen(false)}
        hotelName={selectedHotelName}
        images={hotelImages}
      />

      <PublicFooter />
    </div>
  );
};

export default SearchPage;
