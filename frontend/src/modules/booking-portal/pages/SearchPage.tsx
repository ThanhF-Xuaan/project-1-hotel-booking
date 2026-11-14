import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Search,
  Calendar,
  Building2,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  Star,
  FileSearch,
  Users,
  Camera,
  Loader2,
  AlertTriangle,
  Ticket,
  MapPin,
  ArrowLeft,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Select, { type SelectOption } from '../../../core/components/ui/Select';
import PublicFooter from '../../../core/components/layout/PublicFooter';
import type {
  CustomerSearchCriteria,
  AvailableRoomOffer,
  AvailableRoomTypeApiResponse,
  AvailableHotelDto,
  DestinationSuggestionDto,
} from '../types/portal.types';
import { RoomAdvisorWidget, ADVISOR_AUTO_REOPEN_KEY } from '../components/RoomAdvisor/RoomAdvisorWidget';
import { GroupInquiryModal } from '../components/GroupInquiryModal';
import { HotelGalleryViewerModal } from '../components/HotelGalleryViewerModal';
import { HotelCardItem } from '../components/HotelCardItem';
import { RoomCardItem } from '../components/RoomCardItem';
import { BookingSummarySidebar } from '../components/BookingSummarySidebar';
import { DestinationCombobox } from '../components/DestinationCombobox';
import type { SelectedRoomCartItem } from '../types/portal.types';
import hotelService from '../../organization/services/hotel.service';
import bookingSearchService from '../services/bookingSearchService';
import { bookingSessionService } from '../services/bookingSessionService';
import type { HotelImageDto } from '../../organization/types/organization.types';
import type { AdvisorRecommendation, AdvisorSearchCriteria } from '../types/roomAdvisor.types';

const DEFAULT_CHECK_IN = new Date(Date.now() + 86400000).toISOString().split('T')[0];
const DEFAULT_CHECK_OUT = new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0];

const INITIAL_CRITERIA: CustomerSearchCriteria = {
  checkInDate: DEFAULT_CHECK_IN,
  checkOutDate: DEFAULT_CHECK_OUT,
  adultCount: 2,
  childCount: 0,
  roomsCount: 1,
  voucherCode: '',
};

const calculateNights = (checkIn: string, checkOut: string): number => {
  const start = new Date(checkIn).getTime();
  const end = new Date(checkOut).getTime();
  const diff = Math.round((end - start) / 86400000);
  return diff > 0 ? diff : 1;
};

const mapApiResponseToOffer = (
  item: AvailableRoomTypeApiResponse,
  nights: number
): AvailableRoomOffer => {
  const breakdown = item.priceBreakdown;
  const totalNights = breakdown?.totalNights && breakdown.totalNights > 0 ? breakdown.totalNights : nights;

  const finalTotal = Number(
    breakdown?.finalTotalAmount ?? breakdown?.totalRoomGrossAmount ?? breakdown?.totalBasePrice ?? 0
  );

  // Nightly price: preferably gross per night, or average
  const firstDailyGross = breakdown?.dailyPrices?.[0]?.grossDailyTotal
    ? Number(breakdown.dailyPrices[0].grossDailyTotal)
    : breakdown?.dailyPrices?.[0]?.baseRate
      ? Number(breakdown.dailyPrices[0].baseRate)
      : 0;

  const perNightPrice = firstDailyGross > 0
    ? Math.round(firstDailyGross)
    : totalNights > 0
      ? Math.round(finalTotal / totalNights)
      : Math.round(finalTotal);

  const amenities = (item.amenities || []).map((a) => a.name);
  const inclusions = (item.inclusions || []).map((i) => i.name);

  // Chỉ đánh dấu ăn sáng khi inclusions thực tế từ backend có chứa dịch vụ ăn sáng
  const hasFreeBreakfast = inclusions.some((name) => /buffet|sáng|breakfast/i.test(name));

  const bedDescription = item.maxBeds
    ? `${item.maxBeds} Giường đôi ${item.extraBeds ? `(+${item.extraBeds} giường phụ)` : ''}`
    : '1 Giường đôi Queen/King Size';

  return {
    hotelRoomTypeId: item.hotelRoomTypeId,
    roomTypeCode: item.roomTypeCode,
    roomTypeName: item.roomTypeName,
    basePrice: perNightPrice,
    estimatedTotal: finalTotal || perNightPrice * totalNights,
    maxAdults: item.maxAdults || 2,
    maxChildren: item.maxChildren || 0,
    roomSizeM2: 0,
    bedDescription,
    amenities,
    freeBreakfast: hasFreeBreakfast,
    freeCancellation: false, // Xóa bỏ hardcode true
    availableRoomsCount: item.availableRooms,
  };
};

export const SearchPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const [isAdvisorOpen, setIsAdvisorOpen] = useState(() => {
    if (typeof window === 'undefined') return false;
    const hasRouterState = Boolean(location.state?.autoOpenAdvisor);
    try {
      const hasStorageFlag = sessionStorage.getItem(ADVISOR_AUTO_REOPEN_KEY) === 'true';
      return hasRouterState || hasStorageFlag;
    } catch {
      return hasRouterState;
    }
  });

  // Dọn sạch router state & storage flag sau khi tiếp nhận cờ tự động mở Chatbot
  useEffect(() => {
    try {
      if (sessionStorage.getItem(ADVISOR_AUTO_REOPEN_KEY) === 'true') {
        sessionStorage.removeItem(ADVISOR_AUTO_REOPEN_KEY);
      }
    } catch {
      // ignore
    }

    if (location.state?.autoOpenAdvisor) {
      navigate(location.pathname + location.search, { replace: true, state: {} });
    }
  }, [location.state, location.pathname, location.search, navigate]);

  // Lắng nghe sự kiện popstate (Browser Back/Forward) khi component vẫn còn mounted hoặc phục hồi từ cache
  useEffect(() => {
    const handlePopState = () => {
      try {
        if (sessionStorage.getItem(ADVISOR_AUTO_REOPEN_KEY) === 'true') {
          setIsAdvisorOpen(true);
          sessionStorage.removeItem(ADVISOR_AUTO_REOPEN_KEY);
        }
      } catch {
        // ignore
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);
  const [isGroupModalOpen, setIsGroupModalOpen] = useState(false);
  const [isGalleryOpen, setIsGalleryOpen] = useState(false);
  const [isHoldingSession, setIsHoldingSession] = useState(false);
  const [hotelImages, setHotelImages] = useState<HotelImageDto[]>([]);

  const [criteria, setCriteria] = useState<CustomerSearchCriteria>(INITIAL_CRITERIA);
  const [searchMode, setSearchMode] = useState<'HOTELS' | 'ROOMS'>('HOTELS');
  const [roomOffers, setRoomOffers] = useState<AvailableRoomOffer[]>([]);
  const [rawRooms, setRawRooms] = useState<AvailableRoomTypeApiResponse[]>([]);
  const [selectedCartItems, setSelectedCartItems] = useState<SelectedRoomCartItem[]>([]);
  const [availableHotels, setAvailableHotels] = useState<AvailableHotelDto[]>([]);
  const [isLoadingRooms, setIsLoadingRooms] = useState<boolean>(true);
  const [roomError, setRoomError] = useState<string | null>(null);

  const targetHotel = useMemo(() => availableHotels.find((h) => h.isTarget), [availableHotels]);
  const otherHotels = useMemo(
    () => (targetHotel ? availableHotels.filter((h) => !h.isTarget) : availableHotels),
    [availableHotels, targetHotel]
  );

  // Điểm đến: Khu vực/Thành phố hoặc Khách sạn cơ sở
  const [destinations, setDestinations] = useState<DestinationSuggestionDto[]>([]);
  const [selectedDestKey, setSelectedDestKey] = useState<string>('');

  // Map danh sách điểm đến thành SelectOption có phân nhóm và icon
  const destinationOptions: SelectOption<string>[] = useMemo(() => {
    if (destinations.length === 0) {
      return [];
    }

    const regions = destinations.filter((d) => d.type === 'REGION');
    const hotels = destinations.filter((d) => d.type === 'HOTEL');

    return [
      ...regions.map((r) => ({
        value: `REGION:${r.id}`,
        label: r.title,
        badge: 'Điểm đến',
        icon: <MapPin className="w-3.5 h-3.5 text-red-500" />,
      })),
      ...hotels.map((h) => ({
        value: `HOTEL:${h.id}`,
        label: h.title,
        badge: 'Khách sạn',
        icon: <Building2 className="w-3.5 h-3.5 text-blue-500" />,
      })),
    ];
  }, [destinations]);

  // Thay đổi điểm đến khi người dùng chọn Dropdown
  const handleDestinationChange = (key: string) => {
    setSelectedDestKey(key);
    const [type, idStr] = key.split(':');
    const numId = Number(idStr);
    if (type === 'REGION') {
      setCriteria((prev) => ({
        ...prev,
        destinationType: 'REGION',
        destinationId: numId,
        regionId: numId,
        hotelId: undefined,
      }));
    } else {
      setCriteria((prev) => ({
        ...prev,
        destinationType: 'HOTEL',
        destinationId: numId,
        hotelId: numId,
        regionId: undefined,
      }));
    }
  };

  // Load public hotel images for spotlight banner khi đang chọn 1 khách sạn
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

  // Hàm thực thi tìm kiếm hợp nhất (Unified Search)
  const executeUnifiedSearch = (targetCriteria: CustomerSearchCriteria) => {
    if (!targetCriteria.destinationId && !targetCriteria.hotelId && !targetCriteria.regionId) {
      setIsLoadingRooms(false);
      return;
    }

    setIsLoadingRooms(true);
    setRoomError(null);

    bookingSearchService
      .searchUnified(targetCriteria)
      .then((res) => {
        if (!res?.result) {
          setAvailableHotels([]);
          setRoomOffers([]);
          return;
        }

        if (res.result.searchType === 'HOTELS') {
          setSearchMode('HOTELS');
          setAvailableHotels(res.result.hotels?.content || []);
          setRoomOffers([]);
          setRawRooms([]);
          setSelectedCartItems([]);
        } else {
          setSearchMode('ROOMS');
          const roomList = res.result.rooms || [];
          setRawRooms(roomList);
          const nights = calculateNights(targetCriteria.checkInDate, targetCriteria.checkOutDate);
          const mapped = roomList.map((item) => mapApiResponseToOffer(item, nights));
          setRoomOffers(mapped);
          setAvailableHotels([]);
          setSelectedCartItems([]);
          if (res.result.selectedHotel?.hotelId) {
            setCriteria((prev) => ({ ...prev, hotelId: res.result.selectedHotel?.hotelId }));
          }
        }
      })
      .catch((err: unknown) => {
        console.error('Lỗi khi tìm kiếm:', err);
        const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
        const msg =
          axiosErr?.response?.data?.message ||
          (err instanceof Error ? err.message : 'Không thể kết nối đến hệ thống tìm kiếm phòng.');
        setRoomError(msg);
        setAvailableHotels([]);
        setRoomOffers([]);
      })
      .finally(() => {
        setIsLoadingRooms(false);
      });
  };

  // Load danh sách điểm đến gợi ý từ API và khởi tạo tìm kiếm ban đầu với điểm đến thực tế của DB
  useEffect(() => {
    let isCancelled = false;
    bookingSearchService
      .getDestinations()
      .then((res) => {
        if (isCancelled) return;
        if (!res?.result || res.result.length === 0) {
          setIsLoadingRooms(false);
          return;
        }

        const dests = res.result;
        setDestinations(dests);

        // Ưu tiên chọn khách sạn đầu tiên hoặc điểm đến đầu tiên có sẵn trên hệ thống
        const defaultDest = dests.find((d) => d.type === 'HOTEL') || dests[0];
        const destKey = `${defaultDest.type}:${defaultDest.id}`;
        setSelectedDestKey(destKey);

        const initialTargetCriteria: CustomerSearchCriteria = {
          ...INITIAL_CRITERIA,
          destinationType: defaultDest.type,
          destinationId: defaultDest.id,
          hotelId: defaultDest.type === 'HOTEL' ? defaultDest.id : undefined,
          regionId: defaultDest.type === 'REGION' ? defaultDest.id : undefined,
        };
        setCriteria(initialTargetCriteria);

        executeUnifiedSearch(initialTargetCriteria);
      })
      .catch((err) => {
        if (isCancelled) return;
        console.warn('Lấy danh sách điểm đến gợi ý thất bại:', err);
        setIsLoadingRooms(false);
        setRoomError('Không thể kết nối đến hệ thống điểm đến. Vui lòng thử lại sau.');
      });
    return () => {
      isCancelled = true;
    };
  }, []);

  const handleSearch = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if ((criteria.roomsCount ?? 1) >= 6) {
      setIsGroupModalOpen(true);
      return;
    }
    if (criteria.checkInDate >= criteria.checkOutDate) {
      setRoomError('Ngày nhận phòng phải trước ngày trả phòng ít nhất 1 đêm.');
      return;
    }
    if (!criteria.destinationId && !criteria.hotelId && !criteria.regionId) {
      setRoomError('Vui lòng chọn thành phố hoặc khách sạn cần tìm.');
      return;
    }
    executeUnifiedSearch(criteria);
    const el = document.getElementById('search-results-section');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  // Chọn 1 khách sạn từ danh sách kết quả khách sạn của Vùng
  const handleSelectHotelFromList = (hotel: AvailableHotelDto) => {
    const nextCriteria: CustomerSearchCriteria = {
      ...criteria,
      destinationType: 'HOTEL',
      destinationId: hotel.hotelId,
      hotelId: hotel.hotelId,
      regionId: undefined,
    };
    setCriteria(nextCriteria);
    setSelectedDestKey(`HOTEL:${hotel.hotelId}`);
    setIsLoadingRooms(true);
    setRoomError(null);

    bookingSearchService
      .searchRooms(nextCriteria)
      .then((res) => {
        if (res?.result) {
          setRawRooms(res.result);
          const nights = calculateNights(nextCriteria.checkInDate, nextCriteria.checkOutDate);
          const mapped = res.result.map((item) => mapApiResponseToOffer(item, nights));
          setRoomOffers(mapped);
          setSearchMode('ROOMS');
          setSelectedCartItems([]);
        } else {
          setRawRooms([]);
          setRoomOffers([]);
          setSelectedCartItems([]);
        }
      })
      .catch((err: unknown) => {
        console.error('Lỗi khi tải phòng của khách sạn:', err);
        setRoomError('Không thể tải danh sách phòng của khách sạn này.');
      })
      .finally(() => {
        setIsLoadingRooms(false);
        const el = document.getElementById('search-results-section');
        if (el) el.scrollIntoView({ behavior: 'smooth' });
      });
  };

  const selectedDestinationTitle =
    destinationOptions.find((d) => d.value === selectedDestKey)?.label ||
    (criteria.hotelId ? `Khách sạn #${criteria.hotelId}` : 'Chọn điểm đến');

  const selectedHotelName =
    destinationOptions.find((d) => d.value === `HOTEL:${criteria.hotelId}`)?.label ||
    (criteria.hotelId === 2
      ? 'Premier Đà Nẵng Resort'
      : criteria.hotelId === 3
      ? 'Boutique Sài Gòn Hotel'
      : criteria.hotelId
      ? `Khách sạn #${criteria.hotelId}`
      : 'Khách sạn cơ sở');

  const primaryCoverImage =
    hotelImages.find((img) => img.isPrimary)?.url ||
    hotelImages[0]?.url ||
    (criteria.hotelId === 2
      ? 'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?auto=format&fit=crop&w=1200&q=80'
      : criteria.hotelId === 3
      ? 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80'
      : 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=80');

  const handleUpdateCartItem = (updatedItem: SelectedRoomCartItem | null) => {
    if (!updatedItem || updatedItem.quantity === 0) {
      if (!updatedItem) return;
      setSelectedCartItems((prev) =>
        prev.filter((it) => it.hotelRoomTypeId !== updatedItem.hotelRoomTypeId)
      );
      return;
    }

    setSelectedCartItems((prev) => {
      const idx = prev.findIndex((it) => it.hotelRoomTypeId === updatedItem.hotelRoomTypeId);
      if (idx >= 0) {
        const copy = [...prev];
        copy[idx] = updatedItem;
        return copy;
      }
      return [...prev, updatedItem];
    });
  };

  const handleRemoveRoomFromCart = (hotelRoomTypeId: number, roomIndex: number) => {
    setSelectedCartItems((prev) => {
      const target = prev.find((it) => it.hotelRoomTypeId === hotelRoomTypeId);
      if (!target) return prev;

      if (target.quantity <= 1) {
        return prev.filter((it) => it.hotelRoomTypeId !== hotelRoomTypeId);
      }

      const remainingRooms = target.rooms
        .filter((r) => r.roomIndex !== roomIndex)
        .map((r, i) => ({ ...r, roomIndex: i + 1 }));

      const updatedTarget: SelectedRoomCartItem = {
        ...target,
        quantity: target.quantity - 1,
        rooms: remainingRooms,
        estimatedTotal: Math.round((target.estimatedTotal / target.quantity) * (target.quantity - 1)),
      };

      return prev.map((it) => (it.hotelRoomTypeId === hotelRoomTypeId ? updatedTarget : it));
    });
  };

  const handleProceedCheckout = async () => {
    if (selectedCartItems.length === 0 || isHoldingSession) return;

    setIsHoldingSession(true);
    try {
      const itemsPayload = selectedCartItems.map((it) => {
        const firstRoom = it.rooms[0];
        return {
          hotelRoomTypeId: it.hotelRoomTypeId,
          quantity: it.quantity,
          adults: firstRoom?.adults ?? 2,
          children: firstRoom?.children ?? 0,
          infants: firstRoom?.infants ?? 0,
          extraBeds: 0,
        };
      });

      const effectiveHotelId = criteria.hotelId || 1;

      const sessionReq = {
        hotelId: effectiveHotelId,
        checkInDate: criteria.checkInDate,
        checkOutDate: criteria.checkOutDate,
        voucherCode: criteria.voucherCode ? criteria.voucherCode.trim().toUpperCase() : undefined,
        items: itemsPayload,
      };

      const sessionRes = await bookingSessionService.initiateDraftSession(sessionReq);
      const sessionData = sessionRes.result;

      const primaryItem = selectedCartItems[0];
      const totalNights = calculateNights(criteria.checkInDate, criteria.checkOutDate);
      const matchedRoom = rawRooms.find((r) => r.hotelRoomTypeId === primaryItem.hotelRoomTypeId);
      const fallbackOffer = matchedRoom
        ? mapApiResponseToOffer(matchedRoom, totalNights)
        : roomOffers[0] || {
            hotelRoomTypeId: primaryItem.hotelRoomTypeId,
            roomTypeCode: primaryItem.roomTypeCode,
            roomTypeName: primaryItem.roomTypeName,
            basePrice: primaryItem.perNightPrice,
            estimatedTotal: primaryItem.estimatedTotal,
            maxAdults: 2,
            maxChildren: 0,
            roomSizeM2: 0,
            bedDescription: '1 Giường đôi',
            amenities: [],
            freeBreakfast: false,
            freeCancellation: false,
            availableRoomsCount: 1,
          };

      const totalRooms = selectedCartItems.reduce((sum, it) => sum + it.quantity, 0);
      const totalAdults = selectedCartItems.reduce(
        (sum, it) => sum + it.rooms.reduce((rSum, r) => rSum + r.adults, 0),
        0
      );
      const totalChildren = selectedCartItems.reduce(
        (sum, it) => sum + it.rooms.reduce((rSum, r) => rSum + r.children, 0),
        0
      );
      const grandTotal = sessionData?.quote?.grandTotal || selectedCartItems.reduce((sum, it) => sum + it.estimatedTotal, 0);

      navigate('/portal/checkout', {
        state: {
          sessionId: sessionData?.sessionId,
          remainingSeconds: sessionData?.remainingSeconds || 900,
          hotelName: selectedHotelName,
          hotelId: effectiveHotelId,
          offer: {
            ...fallbackOffer,
            hotelRoomTypeId: primaryItem.hotelRoomTypeId,
            roomTypeName: primaryItem.roomTypeName,
            roomTypeCode: primaryItem.roomTypeCode,
            estimatedTotal: grandTotal,
            basePrice: primaryItem.perNightPrice,
          },
          criteria: {
            ...criteria,
            hotelId: effectiveHotelId,
            roomsCount: totalRooms,
            adultCount: totalAdults,
            childCount: totalChildren,
          },
          selectedCartItems,
        },
      });
    } catch (err: unknown) {
      console.error('Lỗi khi khởi tạo phiên giữ phòng:', err);
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
      const msg = axiosErr.response?.data?.message || axiosErr.message || 'Không thể khóa giữ phòng vào lúc này. Vui lòng kiểm tra lại số lượng phòng trống!';
      alert(msg);
    } finally {
      setIsHoldingSession(false);
    }
  };

  const handleAdvisorViewDetails = (rec: AdvisorRecommendation, advisorCriteria?: AdvisorSearchCriteria) => {
    try {
      sessionStorage.setItem(ADVISOR_AUTO_REOPEN_KEY, 'true');
    } catch (err) {
      console.warn('Không thể lưu cờ tự động mở advisor vào sessionStorage:', err);
    }

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
      <header className="bg-white border-b border-neutral-200 relative z-20 shadow-2xs">
        <div className="max-w-6xl mx-auto px-4 min-h-16 flex flex-wrap items-center justify-between gap-2 py-2 md:h-16 md:flex-nowrap md:py-0">
          <div className="flex shrink-0 items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-red-600 text-white flex items-center justify-center font-extrabold text-lg shadow-xs">
              H
            </div>
            <div>
              <span className="font-extrabold text-neutral-900 tracking-tight text-lg">HOTEL GRAND</span>
              <span className="text-red-600 font-bold ml-1 text-xs uppercase tracking-widest">Portal</span>
            </div>
          </div>
          <div className="flex w-full min-w-0 flex-wrap items-center gap-2 md:w-auto md:flex-nowrap">
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

      {/* Hero Banner Intro (Scrolls naturally) */}
      <div className="bg-neutral-900 text-white pt-8 pb-4 px-4 shadow-inner">
        <div className="max-w-6xl mx-auto space-y-3 text-center">
          <span className="px-3 py-1 bg-red-600 text-white text-xs font-bold rounded-full uppercase tracking-wider inline-flex items-center gap-1">
            <Sparkles className="w-3.5 h-3.5" />
            Cam kết giá tốt nhất & Đặt phòng trực tiếp
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Kỳ Nghỉ Thượng Lưu Tại Chuỗi Khách Sạn Hotel Grand
          </h1>
          <p className="text-neutral-400 text-xs sm:text-sm max-w-xl mx-auto">
            Trải nghiệm dịch vụ 5 sao đẳng cấp quốc tế với công nghệ giữ phòng tức thì và bóc tách giá minh bạch.
          </p>
          <div className="pt-1 flex justify-center">
            <button
              onClick={() => setIsAdvisorOpen(true)}
              className="px-4 py-1.5 bg-neutral-800/90 hover:bg-neutral-800 text-white text-xs font-bold rounded-2xl border border-neutral-700 flex items-center gap-2 transition-all hover:scale-105 shadow-sm cursor-pointer"
            >
              <Sparkles className="w-3.5 h-3.5 text-red-400 fill-red-400" />
              Hỏi Trợ lý AI gợi ý phòng theo nhu cầu & ngân sách
              <ArrowRight className="w-3 h-3 text-neutral-400" />
            </button>
          </div>
        </div>
      </div>

      {/* Sticky Search Bar Section (Pins at top-0 on Desktop like Mường Thanh) */}
      <div className="lg:sticky lg:top-0 z-30 bg-neutral-900/95 backdrop-blur-md border-b border-neutral-800 shadow-lg py-3 px-4 transition-all">
        <div className="max-w-6xl mx-auto">
          {/* Search Box */}
          <div className="bg-white p-3.5 sm:p-4 rounded-2xl shadow-md border border-neutral-100 text-neutral-900 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-[2.4fr_1.3fr_1.3fr_1.1fr_1.1fr_135px] gap-2.5 items-start">
            {/* 1. Điểm Đến (Hỗ trợ nhập liệu trực tiếp và tự động gợi ý) */}
            <div className="w-full">
              <label className="h-5 flex items-center gap-1 text-xs font-bold text-neutral-600 mb-1.5 whitespace-nowrap">
                <MapPin className="w-3.5 h-3.5 text-red-600 shrink-0" />
                <span>Điểm đến</span>
                <span className="text-red-500">*</span>
              </label>
              <DestinationCombobox
                destinations={destinations}
                value={selectedDestKey}
                onChange={handleDestinationChange}
                placeholder="Nhập thành phố hoặc khách sạn..."
              />
            </div>

            {/* 2. Ngày nhận phòng */}
            <div className="w-full">
              <label className="h-5 flex items-center gap-1 text-xs font-bold text-neutral-600 mb-1.5 whitespace-nowrap">
                <Calendar className="w-3.5 h-3.5 text-red-600 shrink-0" />
                <span>Ngày nhận phòng</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                min={new Date().toISOString().split('T')[0]}
                value={criteria.checkInDate}
                onChange={(e) => setCriteria({ ...criteria, checkInDate: e.target.value })}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
                required
              />
            </div>

            {/* 3. Ngày trả phòng */}
            <div className="w-full">
              <label className="h-5 flex items-center gap-1 text-xs font-bold text-neutral-600 mb-1.5 whitespace-nowrap">
                <Calendar className="w-3.5 h-3.5 text-red-600 shrink-0" />
                <span>Ngày trả phòng</span>
                <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                min={criteria.checkInDate || new Date().toISOString().split('T')[0]}
                value={criteria.checkOutDate}
                onChange={(e) => setCriteria({ ...criteria, checkOutDate: e.target.value })}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
                required
              />
            </div>

            {/* 4. Số lượng phòng */}
            <div className="w-full">
              <label className="h-5 flex items-center gap-1 text-xs font-bold text-neutral-600 mb-1.5 whitespace-nowrap">
                <Users className="w-3.5 h-3.5 text-red-600 shrink-0" />
                <span>Số lượng phòng</span>
                <span className="text-red-500">*</span>
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

            {/* 5. Mã Voucher (Optional) */}
            <div className="w-full">
              <label className="h-5 flex items-center gap-1 text-xs font-bold text-neutral-600 mb-1.5 whitespace-nowrap">
                <Ticket className="w-3.5 h-3.5 text-red-600 shrink-0" />
                <span>Mã Voucher</span>
              </label>
              <input
                type="text"
                placeholder="VD: SUMMER2026"
                value={criteria.voucherCode || ''}
                onChange={(e) =>
                  setCriteria({
                    ...criteria,
                    voucherCode: e.target.value.toUpperCase().replace(/\s+/g, ''),
                  })
                }
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm font-semibold uppercase placeholder:normal-case placeholder:font-normal focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              />
            </div>

            {/* 6. Nút Tìm Kiếm */}
            <div className="w-full">
              <div className="h-5 mb-1.5 hidden lg:block" aria-hidden="true" />
              <Button
                onClick={handleSearch}
                className="w-full h-11 flex items-center justify-center gap-2 font-bold text-sm shadow-xs cursor-pointer bg-red-600 hover:bg-red-700 text-white rounded-xl"
              >
                <Search className="w-4 h-4" />
                <span>Tìm Kiếm</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* SEARCH RESULTS SECTION */}
      <div id="search-results-section" className="max-w-6xl mx-auto px-4 mt-8 space-y-6">
        {/* Banner Voucher Đang Áp Dụng (nếu khách nhập mã) */}
        {criteria.voucherCode && (
          <div className="p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center justify-between text-xs sm:text-sm shadow-2xs">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>
                Đang kích hoạt mã voucher: <strong>{criteria.voucherCode}</strong> — Giá hiển thị bên dưới đã được áp dụng chiết khấu ưu đãi.
              </span>
            </div>
            <button
              onClick={() => {
                const nextCriteria = { ...criteria, voucherCode: '' };
                setCriteria(nextCriteria);
                executeUnifiedSearch(nextCriteria);
              }}
              className="text-xs text-emerald-700 underline font-semibold hover:text-emerald-900 cursor-pointer shrink-0 ml-3"
            >
              Hủy áp dụng
            </button>
          </div>
        )}

        {/* LOADING STATE */}
        {isLoadingRooms ? (
          <div className="bg-white rounded-3xl border border-neutral-200 p-12 text-center space-y-4 shadow-xs">
            <Loader2 className="w-8 h-8 text-red-600 animate-spin mx-auto" />
            <div className="space-y-1">
              <p className="text-sm font-bold text-neutral-800">
                Đang tìm kiếm điểm đến & tính toán giá thời gian thực...
              </p>
              <p className="text-xs text-neutral-500">
                Dữ liệu được cập nhật trực tiếp qua Pricing Pipeline và tình trạng phòng trống của khách sạn.
              </p>
            </div>
          </div>
        ) : roomError ? (
          <div className="bg-red-50 border border-red-200 rounded-3xl p-8 text-center space-y-3">
            <AlertTriangle className="w-8 h-8 text-red-600 mx-auto" />
            <p className="text-sm font-semibold text-red-700">{roomError}</p>
            <Button
              variant="outline"
              size="sm"
              onClick={handleSearch}
              className="mt-2"
            >
              Thử lại
            </Button>
          </div>
        ) : searchMode === 'HOTELS' ? (
          /* ========================================================== */
          /* CHẾ ĐỘ 1: KẾT QUẢ TÌM KIẾM THEO VÙNG / THÀNH PHỐ (HOTELS)  */
          /* ========================================================== */
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-neutral-200 pb-4">
              <div>
                <h2 className="text-xl font-bold text-neutral-900 tracking-tight">
                  Khách Sạn Khả Dụng ({availableHotels.length} khách sạn)
                </h2>
                <p className="text-xs text-neutral-500 mt-0.5">
                  Điểm đến: <strong>{selectedDestinationTitle}</strong> • Từ {criteria.checkInDate} đến {criteria.checkOutDate} • {criteria.roomsCount ?? 1} phòng
                </p>
              </div>
              <div className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-200 flex items-center gap-1 self-start sm:self-auto">
                <ShieldCheck className="w-4 h-4" />
                Giữ phòng tức thì • Giá chuẩn chuỗi
              </div>
            </div>

            {availableHotels.length === 0 ? (
              <div className="bg-white rounded-3xl border border-neutral-200 p-10 text-center space-y-4 shadow-xs">
                <div className="w-14 h-14 bg-neutral-100 rounded-2xl flex items-center justify-center mx-auto text-neutral-400">
                  <Building2 className="w-7 h-7" />
                </div>
                <div className="space-y-1.5">
                  <h3 className="text-lg font-bold text-neutral-900">Không tìm thấy khách sạn còn phòng trống</h3>
                  <p className="text-xs text-neutral-500 max-w-md mx-auto">
                    Hiện tại tất cả khách sạn tại khu vực này đã hết phòng trống cho khoảng thời gian {criteria.checkInDate} đến {criteria.checkOutDate}. Quý khách vui lòng thử chọn ngày khác hoặc tham khảo trợ lý AI.
                  </p>
                </div>
                <div className="flex justify-center gap-3 pt-2">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => setIsAdvisorOpen(true)}
                    className="bg-red-600 hover:bg-red-700 text-white font-bold inline-flex items-center gap-1.5"
                  >
                    <Sparkles className="w-4 h-4" />
                    Nhờ Trợ lý AI Tìm Phòng
                  </Button>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {/* 1. Khách sạn mục tiêu người dùng đã chọn (nếu tìm theo Khách sạn cơ sở) */}
                {targetHotel && (
                  <HotelCardItem
                    key={targetHotel.hotelId}
                    hotel={targetHotel}
                    onSelectHotel={handleSelectHotelFromList}
                  />
                )}

                {/* 2. Dải phân cách các khách sạn khác cùng khu vực (chỉ hiển thị khi có khách sạn khác cùng khu vực) */}
                {targetHotel && otherHotels.length > 0 && (
                  <div className="pt-6 pb-2 border-t border-neutral-200">
                    <div className="flex items-center gap-2">
                      <Building2 className="w-5 h-5 text-red-600" />
                      <h3 className="text-sm sm:text-base font-extrabold uppercase tracking-wide text-neutral-800">
                        Khách Sạn Khác Trong Khu Vực Cùng Khả Dụng ({otherHotels.length})
                      </h3>
                    </div>
                    <p className="text-xs text-neutral-500 mt-1">
                      Gợi ý các khách sạn lân cận trong cùng khu vực còn phòng trống cho khoảng thời gian của quý khách
                    </p>
                  </div>
                )}

                {/* 3. Danh sách các khách sạn khác cùng khu vực (hoặc toàn bộ danh sách khi tìm theo Vùng) */}
                {(!targetHotel ? availableHotels : otherHotels).map((hotel) => (
                  <HotelCardItem
                    key={hotel.hotelId}
                    hotel={hotel}
                    onSelectHotel={handleSelectHotelFromList}
                  />
                ))}
              </div>
            )}
          </div>
        ) : (
          /* ========================================================== */
          /* CHẾ ĐỘ 2: KẾT QUẢ TÌM KIẾM THEO KHÁCH SẠN CỤ THỂ (ROOMS)   */
          /* ========================================================== */
          <div className="space-y-6">
            {/* Nút quay lại danh sách khách sạn nếu tìm từ khu vực */}
            {availableHotels.length > 0 && (
              <div>
                <button
                  type="button"
                  onClick={() => setSearchMode('HOTELS')}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-700 hover:text-red-600 hover:bg-neutral-100 px-3 py-1.5 rounded-xl border border-neutral-200 transition-colors cursor-pointer"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Quay lại danh sách khách sạn ({availableHotels.length})</span>
                </button>
              </div>
            )}

            {/* Hotel Spotlight Banner */}
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

            {/* Room Offers & Booking Summary: 2-Column Layout (Chuẩn Mường Thanh) */}
            {rawRooms.length === 0 ? (
              <div className="bg-white rounded-3xl border border-neutral-200 p-10 text-center space-y-4 shadow-xs">
                <div className="w-14 h-14 bg-neutral-100 rounded-2xl flex items-center justify-center mx-auto text-neutral-400">
                  <Building2 className="w-7 h-7" />
                </div>
                <div className="space-y-1.5">
                  <h3 className="text-lg font-bold text-neutral-900">Không tìm thấy phòng trống phù hợp</h3>
                  <p className="text-xs text-neutral-500 max-w-md mx-auto">
                    Hiện tại khách sạn đã hết phòng trống cho khoảng thời gian {criteria.checkInDate} đến {criteria.checkOutDate} hoặc số lượng phòng yêu cầu ({criteria.roomsCount ?? 1} phòng). Quý khách vui lòng thử chọn ngày khác hoặc liên hệ Trợ lý AI.
                  </p>
                </div>
                <div className="flex justify-center gap-3 pt-2">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => setIsAdvisorOpen(true)}
                    className="bg-red-600 hover:bg-red-700 text-white font-bold inline-flex items-center gap-1.5"
                  >
                    <Sparkles className="w-4 h-4" />
                    Nhờ Trợ lý AI Tìm Phòng
                  </Button>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-6 items-start">
                {/* CỘT TRÁI: DANH SÁCH CÁC HẠNG PHÒNG KHẢ DỤNG */}
                <div className="space-y-4">
                  <div className="flex flex-wrap justify-between items-center pb-1 gap-2">
                    <h3 className="text-base font-bold text-neutral-900 tracking-tight">
                      Vui lòng chọn phòng (Có {rawRooms.length} loại phòng theo tìm kiếm)
                    </h3>
                    <div className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5" />
                      Giữ phòng 15 phút không cần thẻ tín dụng
                    </div>
                  </div>

                  <div id="room-offers-list" className="space-y-4">
                    {rawRooms.map((room) => (
                      <RoomCardItem
                        key={room.hotelRoomTypeId}
                        room={room}
                        checkInDate={criteria.checkInDate}
                        checkOutDate={criteria.checkOutDate}
                        voucherCode={criteria.voucherCode}
                        checkInTime="14:00"
                        checkOutTime="12:00"
                        currentCartItem={selectedCartItems.find(
                          (it) => it.hotelRoomTypeId === room.hotelRoomTypeId
                        )}
                        onUpdateCartItem={handleUpdateCartItem}
                      />
                    ))}
                  </div>
                </div>

                {/* CỘT PHẢI: PHIẾU THÔNG TIN ĐẶT PHÒNG GHIM CỐ ĐỊNH */}
                <div className="w-full">
                  <BookingSummarySidebar
                    hotelName={selectedHotelName}
                    checkInDate={criteria.checkInDate}
                    checkOutDate={criteria.checkOutDate}
                    selectedItems={selectedCartItems}
                    isSubmitting={isHoldingSession}
                    onRemoveRoom={handleRemoveRoomFromCart}
                    onProceedCheckout={handleProceedCheckout}
                  />
                </div>
              </div>
            )}
          </div>
        )}
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
        onOpenGroupInquiry={() => setIsGroupModalOpen(true)}
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
