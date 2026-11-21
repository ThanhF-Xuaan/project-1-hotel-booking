import React, { useState, useEffect, useCallback } from 'react';
import {
  Brush,
  Sparkles,
  BedDouble,
  AlertTriangle,
  RefreshCw,
  CheckCircle2,
  Clock,
  User,
  Calendar,
  Wrench,
  ShieldAlert,
  LayoutGrid,
  ListFilter,
  Plus,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Select from '../../../core/components/ui/Select';
import Modal from '../../../core/components/ui/Modal';
import { getApiErrorMessage } from '../../../core/api/error';
import { operationService } from '../services/operation.service';
import type { RoomHousekeepingStatusResponse } from '../types/operation.types';
import hotelService from '../../organization/services/hotel.service';
import type { HotelDto } from '../../organization/types/organization.types';
import MaintenanceBlockFormModal from '../components/maintenance-block/MaintenanceBlockFormModal';
import MaintenanceBlockList from '../components/maintenance-block/MaintenanceBlockList';
import { getUserProfile } from '../../auth/utils/token.utils';

export function resolveUserHotelId(
  hotels: HotelDto[],
  profile: ReturnType<typeof getUserProfile>
): number {
  if (hotels && hotels.length > 0) {
    if (profile) {
      const text = `${profile.username || ''} ${profile.name || ''} ${profile.email || ''}`.toLowerCase();
      for (const h of hotels) {
        const hName = (h.name || '').toLowerCase();
        const rName = (h.regionName || '').toLowerCase();
        const rCode = (h.regionCode || '').toLowerCase();
        if (text.includes('hà nội') || text.includes('hanoi') || text.includes('_hn')) {
          if (hName.includes('hà nội') || rName.includes('hà nội') || rCode.includes('hanoi')) return h.id;
        }
        if (text.includes('sài gòn') || text.includes('saigon') || text.includes('_sg') || text.includes('hồ chí minh')) {
          if (hName.includes('sài gòn') || hName.includes('hồ chí minh') || rName.includes('nam')) return h.id;
        }
        if (text.includes('đà nẵng') || text.includes('danang') || text.includes('_dn')) {
          if (hName.includes('đà nẵng') || rName.includes('đà nẵng')) return h.id;
        }
        if (text.includes('hội an') || text.includes('hoian')) {
          if (hName.includes('hội an')) return h.id;
        }
        if (text.includes('hạ long') || text.includes('halong')) {
          if (hName.includes('hạ long')) return h.id;
        }
        if (text.includes('phú quốc') || text.includes('phuquoc')) {
          if (hName.includes('phú quốc')) return h.id;
        }
      }
    }
    const active = hotels.find((h) => h.status === 'ACTIVE') || hotels[0];
    if (active) return active.id;
  }

  if (profile) {
    const text = `${profile.username || ''} ${profile.name || ''} ${profile.email || ''}`.toLowerCase();
    if (text.includes('hà nội') || text.includes('hanoi') || text.includes('_hn')) return 7;
    if (text.includes('sài gòn') || text.includes('saigon') || text.includes('_sg')) return 11;
    if (text.includes('đà nẵng') || text.includes('danang') || text.includes('_dn')) return 9;
    if (text.includes('hội an') || text.includes('hoian')) return 10;
    if (text.includes('hạ long') || text.includes('halong')) return 8;
    if (text.includes('phú quốc') || text.includes('phuquoc')) return 12;
  }
  return 7; // Mặc định khách sạn đang hoạt động
}

export const HousekeepingPage: React.FC = () => {
  const [hotels, setHotels] = useState<HotelDto[]>([]);
  const [isLoadingHotels, setIsLoadingHotels] = useState<boolean>(false);
  const [hotelId, setHotelId] = useState<number>(() => resolveUserHotelId([], getUserProfile()));
  const [mainTab, setMainTab] = useState<'GRID' | 'BLOCKS'>('GRID');
  const [selectedStatusTab, setSelectedStatusTab] = useState<string>('ALL');
  const [rooms, setRooms] = useState<RoomHousekeepingStatusResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [actionSuccessMessage, setActionSuccessMessage] = useState('');
  const [refreshBlocksTrigger, setRefreshBlocksTrigger] = useState<number>(0);

  // Modal states
  const [isBlockModalOpen, setIsBlockModalOpen] = useState<boolean>(false);
  const [selectedRoomForBlock, setSelectedRoomForBlock] = useState<number | undefined>(undefined);
  const [occupiedWarningRoom, setOccupiedWarningRoom] = useState<RoomHousekeepingStatusResponse | null>(null);

  // Error modal state
  const [apiErrorModal, setApiErrorModal] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    code?: number;
  } | null>(null);

  const handleApiError = useCallback((err: unknown, defaultTitle = 'Không thể thực hiện thao tác') => {
    const errorInfo = getApiErrorMessage(err);
    setApiErrorModal({
      isOpen: true,
      title: defaultTitle,
      message: errorInfo.message,
      code: errorInfo.code,
    });
  }, []);

  // Tải danh sách khách sạn đang hoạt động từ hệ thống quản lý khách sạn
  const fetchHotels = useCallback(async () => {
    setIsLoadingHotels(true);
    try {
      let list: HotelDto[] = [];
      try {
        const res = await hotelService.filter({ page: 0, pageSize: 100, status: 'ACTIVE' });
        if (res && res.result) {
          list = res.result.content || [];
        }
      } catch (err) {
        console.warn('Lỗi tải danh sách khách sạn qua API nội bộ, thử fallback qua API công khai:', err);
        const fallbackRes = await hotelService.getPublicHotels({ page: 0, pageSize: 100, status: 'ACTIVE' });
        if (fallbackRes && fallbackRes.result) {
          list = fallbackRes.result.content || [];
        }
      }

      if (list.length > 0) {
        setHotels(list);
        const profile = getUserProfile();
        const resolvedId = resolveUserHotelId(list, profile);
        setHotelId((current) => (current && list.some((h) => h.id === current) ? current : resolvedId || list[0].id));
      } else {
        handleApiError(new Error('Không tìm thấy khách sạn cơ sở nào đang hoạt động.'), 'Lỗi tải danh sách khách sạn');
      }
    } catch (err) {
      console.error('Lỗi tải danh sách khách sạn:', err);
      handleApiError(err, 'Lỗi tải danh sách khách sạn');
    } finally {
      setIsLoadingHotels(false);
    }
  }, [handleApiError]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void fetchHotels();
    }, 0);
    return () => clearTimeout(timer);
  }, [fetchHotels]);

  const fetchRooms = useCallback(async () => {
    if (!hotelId) return;
    setIsLoading(true);
    try {
      // Luôn tải toàn bộ danh sách phòng của khách sạn để tính toán chính xác số lượng cho tất cả trạng thái
      const data = await operationService.getHousekeepingRooms(hotelId);
      setRooms(data || []);
    } catch (err) {
      handleApiError(err, 'Lỗi tải danh sách buồng phòng');
    } finally {
      setIsLoading(false);
    }
  }, [hotelId, handleApiError]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void fetchRooms();
    }, 0);
    return () => clearTimeout(timer);
  }, [fetchRooms]);

  const handleUpdateStatus = async (roomInstanceId: number, newStatus: string) => {
    try {
      await operationService.updateRoomHousekeepingStatus(roomInstanceId, { status: newStatus });
      setActionSuccessMessage(`Đã cập nhật trạng thái phòng thành công: ${newStatus}`);
      setTimeout(() => setActionSuccessMessage(''), 3500);
      fetchRooms();
      setRefreshBlocksTrigger((prev) => prev + 1);
    } catch (err) {
      handleApiError(err, 'Lỗi cập nhật trạng thái phòng');
    }
  };

  const handleInitiateBlock = (room: RoomHousekeepingStatusResponse) => {
    if (room.currentStatus === 'OCCUPIED') {
      setOccupiedWarningRoom(room);
    } else {
      setSelectedRoomForBlock(room.roomInstanceId);
      setIsBlockModalOpen(true);
    }
  };

  const handleBlockSuccess = (msg: string) => {
    setActionSuccessMessage(msg);
    setTimeout(() => setActionSuccessMessage(''), 4000);
    fetchRooms();
    setRefreshBlocksTrigger((prev) => prev + 1);
  };

  // Status counters from full rooms list
  const countReady = rooms.filter((r) => r.currentStatus === 'READY').length;
  const countOccupied = rooms.filter((r) => r.currentStatus === 'OCCUPIED').length;
  const countDirty = rooms.filter((r) => r.currentStatus === 'DIRTY').length;
  const countCleaning = rooms.filter((r) => r.currentStatus === 'CLEANING').length;
  const countMaintenance = rooms.filter((r) => r.currentStatus === 'MAINTENANCE').length;

  // Lọc danh sách phòng hiển thị trên sơ đồ theo tab người dùng chọn (chuyển tab tức thì, không làm mất số đếm KPI)
  const displayedRooms = selectedStatusTab === 'ALL'
    ? rooms
    : rooms.filter((r) => r.currentStatus === selectedStatusTab);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 border-b border-neutral-200 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <Brush className="w-6 h-6 sm:w-7 sm:h-7 text-red-600 shrink-0" />
            <span>Sơ đồ Buồng phòng & Khóa bảo trì</span>
          </h1>
          <p className="text-xs sm:text-sm text-neutral-500 mt-1">
            Theo dõi trạng thái buồng phòng, xử lý vệ sinh và quản lý các lệnh khóa phòng bảo trì (OOO/OOS)
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2.5 sm:gap-3 w-full lg:w-auto">
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <label className="text-xs font-semibold text-neutral-600 whitespace-nowrap">Khách sạn cơ sở:</label>
            <div className="flex-1 sm:w-56">
              <Select<number>
                value={hotelId}
                onChange={(val) => setHotelId(Number(val))}
                onOpen={fetchHotels}
                isLoading={isLoadingHotels}
                placeholder="Chọn một tùy chọn..."
                options={hotels.map((h) => ({
                  value: h.id,
                  label: h.name,
                }))}
                size="md"
              />
            </div>
          </div>
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <Button
              variant="primary"
              onClick={() => {
                setSelectedRoomForBlock(undefined);
                setIsBlockModalOpen(true);
              }}
              className="flex-1 sm:flex-none h-10 px-3.5 bg-red-600 hover:bg-red-700 text-xs font-semibold flex items-center justify-center gap-1.5 shadow-sm"
            >
              <Plus className="w-4 h-4" />
              Khóa phòng (OOO/OOS)
            </Button>
            <Button
              variant="secondary"
              onClick={() => {
                void fetchHotels();
                void fetchRooms();
              }}
              className="h-10 flex items-center justify-center gap-1.5 text-xs px-3"
              disabled={isLoading || isLoadingHotels}
              title="Làm mới dữ liệu"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading || isLoadingHotels ? 'animate-spin' : ''}`} />
              <span className="hidden sm:inline">Làm mới</span>
            </Button>
          </div>
        </div>
      </div>

      {/* Thông báo trực quan khi danh sách khách sạn cơ sở rỗng / lỗi tải */}
      {hotels.length === 0 && !isLoadingHotels && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl text-amber-900 text-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs">
          <div className="flex items-center gap-2.5">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0" />
            <div>
              <span className="font-semibold">Không thể tải danh sách khách sạn cơ sở.</span>
              <p className="text-xs text-amber-700 mt-0.5">Vui lòng kiểm tra lại kết nối mạng hoặc liên hệ Quản lý (Property Manager).</p>
            </div>
          </div>
          <Button variant="secondary" size="sm" onClick={() => void fetchHotels()} className="shrink-0 text-xs font-semibold">
            <RefreshCw className="w-3.5 h-3.5 mr-1" />
            Thử lại
          </Button>
        </div>
      )}

      {actionSuccessMessage && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-sm flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          {actionSuccessMessage}
        </div>
      )}

      {/* Main Tab Navigation */}
      <div className="flex items-center gap-2 border-b border-neutral-200">
        <button
          onClick={() => setMainTab('GRID')}
          className={`pb-3 px-4 text-sm font-bold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
            mainTab === 'GRID'
              ? 'border-red-600 text-red-600'
              : 'border-transparent text-neutral-500 hover:text-neutral-800'
          }`}
        >
          <LayoutGrid className="w-4 h-4" />
          Sơ đồ buồng phòng ({rooms.length})
        </button>

        <button
          onClick={() => setMainTab('BLOCKS')}
          className={`pb-3 px-4 text-sm font-bold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
            mainTab === 'BLOCKS'
              ? 'border-red-600 text-red-600'
              : 'border-transparent text-neutral-500 hover:text-neutral-800'
          }`}
        >
          <ListFilter className="w-4 h-4" />
          Danh sách khóa bảo trì (OOO / OOS)
        </button>
      </div>

      {/* Tab Content: Housekeeping Grid */}
      {mainTab === 'GRID' && (
        <div className="space-y-6">
          {/* Summary KPI Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
            <div
              onClick={() => setSelectedStatusTab((prev) => (prev === 'READY' ? 'ALL' : 'READY'))}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'READY'
                  ? 'bg-emerald-50 border-emerald-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-emerald-300'
              }`}
            >
              <div className="text-xs font-semibold text-emerald-700 flex items-center gap-1.5">
                <Sparkles className="w-4 h-4" />
                Sẵn sàng đón khách
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countReady}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab((prev) => (prev === 'OCCUPIED' ? 'ALL' : 'OCCUPIED'))}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'OCCUPIED'
                  ? 'bg-blue-50 border-blue-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-blue-300'
              }`}
            >
              <div className="text-xs font-semibold text-blue-700 flex items-center gap-1.5">
                <BedDouble className="w-4 h-4" />
                Đang có khách ở
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countOccupied}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab((prev) => (prev === 'DIRTY' ? 'ALL' : 'DIRTY'))}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'DIRTY'
                  ? 'bg-rose-50 border-rose-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-rose-300'
              }`}
            >
              <div className="text-xs font-semibold text-rose-700 flex items-center gap-1.5">
                <Brush className="w-4 h-4" />
                Cần dọn dẹp (Dirty)
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countDirty}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab((prev) => (prev === 'CLEANING' ? 'ALL' : 'CLEANING'))}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'CLEANING'
                  ? 'bg-amber-50 border-amber-500 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-amber-300'
              }`}
            >
              <div className="text-xs font-semibold text-amber-700 flex items-center gap-1.5">
                <Clock className="w-4 h-4" />
                Đang dọn dẹp
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countCleaning}</div>
            </div>

            <div
              onClick={() => setSelectedStatusTab((prev) => (prev === 'MAINTENANCE' ? 'ALL' : 'MAINTENANCE'))}
              className={`cursor-pointer p-4 rounded-2xl border transition-all ${
                selectedStatusTab === 'MAINTENANCE'
                  ? 'bg-neutral-100 border-neutral-400 shadow-xs'
                  : 'bg-white border-neutral-200 hover:border-neutral-400'
              }`}
            >
              <div className="text-xs font-semibold text-neutral-700 flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4" />
                Đang bảo trì
              </div>
              <div className="text-2xl font-bold text-neutral-900 mt-2">{countMaintenance}</div>
            </div>
          </div>

          {/* Filter Tabs */}
          <div className="flex flex-wrap gap-2 border-b border-neutral-200 pb-3">
            {[
              { key: 'ALL', label: 'Tất cả phòng', count: rooms.length },
              { key: 'READY', label: 'Sẵn sàng (READY)', count: countReady },
              { key: 'OCCUPIED', label: 'Có khách (OCCUPIED)', count: countOccupied },
              { key: 'DIRTY', label: 'Cần dọn (DIRTY)', count: countDirty },
              { key: 'CLEANING', label: 'Đang dọn (CLEANING)', count: countCleaning },
              { key: 'MAINTENANCE', label: 'Bảo trì (MAINTENANCE)', count: countMaintenance },
            ].map((tab) => (
              <button
                key={tab.key}
                onClick={() => setSelectedStatusTab(tab.key)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer flex items-center gap-1.5 ${
                  selectedStatusTab === tab.key
                    ? 'bg-red-600 text-white shadow-xs'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                }`}
              >
                <span>{tab.label}</span>
                <span
                  className={`px-1.5 py-0.2 rounded-full text-[11px] font-bold ${
                    selectedStatusTab === tab.key
                      ? 'bg-white/25 text-white'
                      : 'bg-neutral-200 text-neutral-700'
                  }`}
                >
                  {tab.count}
                </span>
              </button>
            ))}
          </div>

          {/* Room Grid */}
          {isLoading ? (
            <div className="py-24 text-center text-neutral-400">Đang tải sơ đồ phòng...</div>
          ) : displayedRooms.length === 0 ? (
            <div className="py-24 text-center text-neutral-400">Không có phòng nào với trạng thái này.</div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {displayedRooms.map((room) => {
                const isReady = room.currentStatus === 'READY';
                const isOccupied = room.currentStatus === 'OCCUPIED';
                const isDirty = room.currentStatus === 'DIRTY';
                const isCleaning = room.currentStatus === 'CLEANING';
                const isMaintenance = room.currentStatus === 'MAINTENANCE';

                return (
                  <div
                    key={room.roomInstanceId}
                    className={`bg-white rounded-2xl border p-4.5 flex flex-col justify-between transition-all shadow-xs ${
                      isReady
                        ? 'border-emerald-200 hover:border-emerald-400'
                        : isOccupied
                        ? 'border-blue-200 hover:border-blue-400'
                        : isDirty
                        ? 'border-rose-200 hover:border-rose-400'
                        : isCleaning
                        ? 'border-amber-200 hover:border-amber-400'
                        : 'border-neutral-200 hover:border-neutral-400'
                    }`}
                  >
                    <div>
                      <div className="flex justify-between items-start">
                        <div>
                          <div className="text-xl font-extrabold text-neutral-900 tracking-tight">
                            P.{room.roomNumber}
                          </div>
                          <div className="text-xs font-medium text-neutral-500 mt-0.5">
                            {room.roomTypeName || 'Tiêu chuẩn'}
                          </div>
                        </div>
                        <span
                          className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${
                            isReady
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : isOccupied
                              ? 'bg-blue-50 text-blue-700 border border-blue-200'
                              : isDirty
                              ? 'bg-rose-50 text-rose-700 border border-rose-200'
                              : isCleaning
                              ? 'bg-amber-50 text-amber-700 border border-amber-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-300'
                          }`}
                        >
                          {isReady && 'Sẵn sàng'}
                          {isOccupied && 'Có khách'}
                          {isDirty && 'Cần dọn dẹp'}
                          {isCleaning && 'Đang dọn'}
                          {isMaintenance && 'Bảo trì'}
                        </span>
                      </div>

                      {isOccupied && (
                        <div className="mt-3 pt-3 border-t border-neutral-100 space-y-1 text-xs text-neutral-600">
                          <div className="flex items-center gap-1.5 font-medium text-neutral-800">
                            <User className="w-3.5 h-3.5 text-blue-600" />
                            <span>{room.guestName || 'Khách lưu trú'}</span>
                          </div>
                          {room.checkOutDate && (
                            <div className="flex items-center gap-1.5 text-neutral-500 text-[11px]">
                              <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                              <span>Trả phòng: {room.checkOutDate}</span>
                            </div>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Quick Action Buttons */}
                    <div className="mt-4 pt-3 border-t border-neutral-100 flex flex-wrap gap-1.5">
                      {isDirty && (
                        <button
                          onClick={() => handleUpdateStatus(room.roomInstanceId, 'CLEANING')}
                          className="flex-1 py-1.5 bg-amber-500 hover:bg-amber-600 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-1 transition-colors cursor-pointer"
                        >
                          <Brush className="w-3.5 h-3.5" />
                          Bắt đầu dọn
                        </button>
                      )}
                      {isCleaning && (
                        <button
                          onClick={() => handleUpdateStatus(room.roomInstanceId, 'READY')}
                          className="flex-1 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-1 transition-colors cursor-pointer"
                        >
                          <Sparkles className="w-3.5 h-3.5" />
                          Đã sạch (Ready)
                        </button>
                      )}
                      {!isDirty && !isCleaning && !isOccupied && (
                        <>
                          {!isReady && (
                            <button
                              onClick={() => handleUpdateStatus(room.roomInstanceId, 'READY')}
                              className="px-2.5 py-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded-lg text-xs font-semibold transition-colors cursor-pointer"
                            >
                              Sẵn sàng
                            </button>
                          )}
                          <button
                            onClick={() => handleUpdateStatus(room.roomInstanceId, 'DIRTY')}
                            className="px-2.5 py-1 bg-rose-50 text-rose-700 hover:bg-rose-100 rounded-lg text-xs font-semibold transition-colors cursor-pointer"
                          >
                            Báo dọn
                          </button>
                        </>
                      )}

                      {/* Khóa bảo trì / Xem bảo trì button */}
                      {isMaintenance ? (
                        <button
                          onClick={() => setMainTab('BLOCKS')}
                          className="px-2.5 py-1 bg-neutral-100 text-neutral-700 hover:bg-neutral-200 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1 cursor-pointer"
                        >
                          <Wrench className="w-3.5 h-3.5 text-neutral-500" />
                          Lệnh bảo trì
                        </button>
                      ) : (
                        <button
                          onClick={() => handleInitiateBlock(room)}
                          className="px-2.5 py-1 bg-neutral-50 hover:bg-neutral-100 text-neutral-700 border border-neutral-200 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1 cursor-pointer"
                          title="Tạo lệnh khóa phòng bảo trì (OOO/OOS)"
                        >
                          <Wrench className="w-3.5 h-3.5 text-neutral-500" />
                          Khóa phòng
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Tab Content: Maintenance Blocks List */}
      {mainTab === 'BLOCKS' && (
        <MaintenanceBlockList
          hotelId={hotelId}
          onOpenCreateModal={() => {
            setSelectedRoomForBlock(undefined);
            setIsBlockModalOpen(true);
          }}
          onError={(err) => handleApiError(err, 'Lỗi thao tác khóa bảo trì')}
          onSuccess={(msg) => {
            setActionSuccessMessage(msg);
            setTimeout(() => setActionSuccessMessage(''), 4000);
            fetchRooms();
          }}
          refreshTrigger={refreshBlocksTrigger}
        />
      )}

      {/* Modal: Form tạo lệnh khóa phòng */}
      <MaintenanceBlockFormModal
        isOpen={isBlockModalOpen}
        onClose={() => setIsBlockModalOpen(false)}
        onSuccess={handleBlockSuccess}
        onError={(err) => handleApiError(err, 'Lỗi tạo lệnh khóa phòng')}
        hotelId={hotelId}
        rooms={rooms}
        initialRoomInstanceId={selectedRoomForBlock}
      />

      {/* Modal cảnh báo phòng có khách lưu trú */}
      {occupiedWarningRoom && (
        <Modal
          isOpen={!!occupiedWarningRoom}
          onClose={() => setOccupiedWarningRoom(null)}
          title="Cảnh báo: Phòng đang có khách lưu trú"
          maxWidth="sm"
        >
          <div className="flex flex-col items-center text-center gap-3">
            <div className="w-12 h-12 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center mb-1">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <p className="text-sm text-neutral-700 leading-relaxed">
              Phòng <span className="font-bold text-neutral-900">P.{occupiedWarningRoom.roomNumber}</span> hiện đang có khách lưu trú
              {occupiedWarningRoom.guestName ? ` (${occupiedWarningRoom.guestName})` : ''}.
            </p>
            <p className="text-xs text-neutral-500 leading-relaxed">
              Khóa phòng OOO trong thời gian khách ở sẽ bị từ chối nếu thời gian trùng với booking. Bạn có chắc muốn tiếp tục mở biểu mẫu khóa phòng không?
            </p>
            <div className="flex items-center justify-end gap-3 w-full mt-4 pt-3 border-t border-neutral-100">
              <Button variant="secondary" onClick={() => setOccupiedWarningRoom(null)}>
                Đóng lại
              </Button>
              <Button
                variant="primary"
                onClick={() => {
                  const targetRoomId = occupiedWarningRoom.roomInstanceId;
                  setOccupiedWarningRoom(null);
                  setSelectedRoomForBlock(targetRoomId);
                  setIsBlockModalOpen(true);
                }}
                className="bg-amber-600 hover:bg-amber-700"
              >
                Tiếp tục tạo lệnh
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Modal Popup Lỗi theo chuẩn ApiResponse */}
      {apiErrorModal && (
        <Modal
          isOpen={apiErrorModal.isOpen}
          onClose={() => setApiErrorModal(null)}
          title={apiErrorModal.title}
          maxWidth="sm"
        >
          <div className="flex flex-col items-center text-center gap-3">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mb-1">
              <AlertTriangle className="w-6 h-6" />
            </div>
            {apiErrorModal.code && (
              <span className="text-xs font-mono font-semibold px-2 py-0.5 bg-neutral-100 text-neutral-600 rounded">
                Mã lỗi: {apiErrorModal.code}
              </span>
            )}
            <p className="text-sm text-neutral-700 leading-relaxed">
              {apiErrorModal.message}
            </p>
            <div className="flex justify-end w-full mt-4 pt-3 border-t border-neutral-100">
              <Button
                variant="primary"
                onClick={() => setApiErrorModal(null)}
                className="w-full bg-red-600 hover:bg-red-700"
              >
                Đã hiểu
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default HousekeepingPage;
