import React, { useState, useMemo, useEffect } from 'react';
import Select from '../../../core/components/ui/Select';
import {
  Building,
  Calendar,
  ArrowLeftRight,
  X,
  BedDouble,
  ChevronDown,
  ChevronRight,
  Search,
  Filter,
  Layers,
  CheckCircle2,
  Wrench,
  Clock,
  LayoutGrid,
  Rows3,
  Utensils,
  DoorOpen,
  Key,
  FileText,
  UserCheck,
  Sparkles,
  Ban,
  PlusCircle,
  Eye,
  RefreshCw,
  AlertTriangle,
} from 'lucide-react';
import type {
  BookingResponse,
  BookingRoomResponse,
  BookingDetailResponse,
} from '../types/booking.types';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';

const ROOM_TYPE_PRIORITY: Record<string, number> = {
  STD: 1,
  STANDARD: 1,
  SUP: 2,
  SUPERIOR: 2,
  DLX: 3,
  DELUXE: 3,
  STE: 4,
  SUITE: 4,
  EXE: 5,
  EXECUTIVE: 5,
  PRE: 6,
  PREMIER: 6,
  FAM: 7,
  FAMILY: 7,
};

export interface TimelineRackViewProps {
  rooms: RoomInstanceDto[];
  bookings: BookingResponse[];
  isLoading: boolean;
  onRefresh: () => void;
  onOpenSwapModal: (target: {
    bookingId: number;
    bookingNumber: string;
    bookingRoomId: number;
    currentRoomInstanceId?: number;
    currentRoomNumber?: string;
    currentRoomTypeName?: string;
    guestName?: string;
    checkInDate?: string;
    checkOutDate?: string;
    hotelId?: number;
  }) => void;
  onOpenDetail?: (booking: BookingResponse) => void;
  onCheckIn?: (bookingRoom: BookingRoomResponse) => void;
  onOpenCheckout?: (bookingRoom: BookingRoomResponse) => void;
  onOpenServiceOrder?: (target: {
    bookingId: number;
    bookingNumber?: string;
    bookingRoomId?: number;
    roomInstanceId: number;
    roomNumber: string;
    hotelId: number;
  }) => void;
  onOpenWalkIn?: (prefill?: {
    hotelId?: number;
    roomInstanceId?: number;
    roomNumber?: string;
    hotelRoomTypeId?: number;
    checkInDate?: string;
  }) => void;
  onOpenCancel?: (target: {
    bookingId: number;
    bookingNumber: string;
    bookingRoomId?: number;
    roomNumber?: string;
    roomTypeName?: string;
  }) => void;
  onOpenRoomNoShow?: (bookingRoom: BookingRoomResponse) => void;
  onOpenStayGuests?: (target: {
    bookingRoomId: number;
    hotelId: number;
    roomNumber?: string;
    guestName?: string;
  }) => void;
  onUpdateRoomStatus?: (roomId: number, status: string) => Promise<void>;
  hotelList?: { id: number; name: string }[];
  selectedHotelId?: number;
  onSelectHotelId?: (id: number) => void;
}

const toLocalDateStr = (d: Date): string => {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const parseLocalDate = (str: string): Date => {
  const parts = str.split('T')[0].split('-');
  return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
};

export const TimelineRackView: React.FC<TimelineRackViewProps> = ({
  rooms,
  bookings,
  isLoading,
  onRefresh,
  onOpenSwapModal,
  onOpenDetail,
  onCheckIn,
  onOpenCheckout,
  onOpenServiceOrder,
  onOpenWalkIn,
  onOpenCancel,
  onOpenRoomNoShow,
  onOpenStayGuests,
  onUpdateRoomStatus,
  hotelList = [],
  selectedHotelId,
  onSelectHotelId,
}) => {
  // Auto-refresh timer states (options: 0 = Off, 30s, 60s, 120s; default: 60s)
  const [autoRefreshInterval, setAutoRefreshInterval] = useState<number>(60);
  const [secondsRemaining, setSecondsRemaining] = useState<number>(60);

  // Date range state (default 14 days, e.g., today to +13 days)
  const [startDateStr, setStartDateStr] = useState<string>(() => {
    return toLocalDateStr(new Date());
  });

  const [endDateStr, setEndDateStr] = useState<string>(() => {
    const end = new Date();
    end.setDate(end.getDate() + 13);
    return toLocalDateStr(end);
  });

  // Selected cell modal/drawer state
  const [activeCellInfo, setActiveCellInfo] = useState<{
    room: RoomInstanceDto;
    dateStr: string;
    booking?: BookingResponse;
    bookingRoom?: BookingRoomResponse;
    bookingDetail?: BookingDetailResponse;
    statusType: 'READY' | 'OCCUPIED' | 'RESERVED' | 'CLEANING' | 'MAINTENANCE' | 'BLOCKED';
  } | null>(null);

  // UX Optimization States for Large-scale Room Counts
  const [collapsedGroups, setCollapsedGroups] = useState<Set<string>>(new Set());
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRoomTypeFilter, setSelectedRoomTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'AVAILABLE' | 'OCCUPIED' | 'OUT_OF_SERVICE'>('ALL');
  const [density, setDensity] = useState<'STANDARD' | 'COMPACT'>('STANDARD');

  // Compute days in the selected date range
  const daysRange = useMemo(() => {
    const days: { dateStr: string; dayName: string; dateDisplay: string; isToday: boolean }[] = [];
    const start = parseLocalDate(startDateStr);
    const end = parseLocalDate(endDateStr);
    const todayStr = toLocalDateStr(new Date());

    // If start > end, fallback to start
    const validEnd = end >= start ? end : start;

    for (let d = new Date(start); d <= validEnd; d.setDate(d.getDate() + 1)) {
      const dateStr = toLocalDateStr(d);
      const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
      const dayName = dayNames[d.getDay()];
      const day = d.getDate().toString().padStart(2, '0');
      const month = (d.getMonth() + 1).toString().padStart(2, '0');
      const dateDisplay = `${day}/${month}`;

      days.push({
        dateStr,
        dayName,
        dateDisplay,
        isToday: dateStr === todayStr,
      });

      // Limit to 31 days max to prevent memory overload
      if (days.length >= 31) break;
    }

    return days;
  }, [startDateStr, endDateStr]);

  // Navigate date range by +/- days
  const handleShiftDays = (days: number) => {
    const start = parseLocalDate(startDateStr);
    start.setDate(start.getDate() + days);
    const newStartStr = toLocalDateStr(start);

    const end = parseLocalDate(endDateStr);
    end.setDate(end.getDate() + days);
    const newEndStr = toLocalDateStr(end);

    setStartDateStr(newStartStr);
    setEndDateStr(newEndStr);
  };

  // Quick Date Presets
  const setDatePreset = (preset: 'TODAY' | '7_DAYS' | '14_DAYS' | '30_DAYS') => {
    const now = new Date();
    const startStr = toLocalDateStr(now);
    const end = new Date(now);

    if (preset === 'TODAY') {
      // Single day
    } else if (preset === '7_DAYS') {
      end.setDate(end.getDate() + 6);
    } else if (preset === '14_DAYS') {
      end.setDate(end.getDate() + 13);
    } else if (preset === '30_DAYS') {
      end.setDate(end.getDate() + 29);
    }

    setStartDateStr(startStr);
    setEndDateStr(toLocalDateStr(end));
  };

  // Auto-refresh countdown effect (with auto-pause during modal operations)
  useEffect(() => {
    if (autoRefreshInterval <= 0) return;

    const timer = setInterval(() => {
      // Pause countdown when user has quick operations modal open
      if (activeCellInfo) return;

      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          onRefresh();
          return autoRefreshInterval;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [autoRefreshInterval, activeCellInfo, onRefresh]);

  // Filter rooms by selected hotel
  const currentHotelRooms = useMemo(() => {
    const currentHotelId = selectedHotelId || hotelList[0]?.id;
    return currentHotelId
      ? rooms.filter((r) => !r.hotelId || r.hotelId === currentHotelId)
      : rooms;
  }, [rooms, selectedHotelId, hotelList]);

  // Active bookings mapped by roomInstanceId and date
  const roomBookingMap = useMemo(() => {
    const map = new Map<
      string,
      {
        booking: BookingResponse;
        bookingRoom: BookingRoomResponse;
        bookingDetail: BookingDetailResponse;
      }
    >();

    bookings.forEach((b) => {
      if (b.status === 'CANCELLED' || b.status === 'NO_SHOW') return;

      b.bookingDetails?.forEach((bd) => {
        bd.bookingRooms?.forEach((br: BookingRoomResponse) => {
          if (!br.roomInstanceId) return;
          if (br.status === 'NO_SHOW' || br.status === 'CANCELLED') return;

          const checkIn = parseLocalDate(bd.checkInDate);
          const checkOut = parseLocalDate(bd.checkOutDate);

          // Map dates in stay
          for (let d = new Date(checkIn); d < checkOut; d.setDate(d.getDate() + 1)) {
            const dateStr = toLocalDateStr(d);
            const key = `${br.roomInstanceId}_${dateStr}`;
            map.set(key, { booking: b, bookingRoom: br, bookingDetail: bd });
          }
        });
      });
    });

    return map;
  }, [bookings]);

  // Mini KPI Occupancy Dashboard Calculations
  const kpiStats = useMemo(() => {
    const totalRooms = currentHotelRooms.length;
    if (totalRooms === 0) {
      return { total: 0, available: 0, occupied: 0, reserved: 0, outOfService: 0, rate: 0 };
    }

    const targetDateStr = daysRange[0]?.dateStr || toLocalDateStr(new Date());
    let occupiedCount = 0;
    let reservedCount = 0;
    let outOfServiceCount = 0;

    currentHotelRooms.forEach((r) => {
      const cellKey = `${r.id}_${targetDateStr}`;
      const bookingInfo = roomBookingMap.get(cellKey);
      if (bookingInfo) {
        if (bookingInfo.bookingRoom.status === 'CHECKED_IN') {
          occupiedCount++;
        } else {
          reservedCount++;
        }
      } else if (
        r.currentStatus === 'MAINTENANCE' ||
        (r.currentStatus as string) === 'BLOCKED' ||
        r.currentStatus === 'CLEANING' ||
        r.currentStatus === 'DIRTY'
      ) {
        outOfServiceCount++;
      }
    });

    const availableCount = Math.max(0, totalRooms - occupiedCount - reservedCount - outOfServiceCount);
    const occupancyRate = Math.round(((occupiedCount + reservedCount) / totalRooms) * 100);

    return {
      total: totalRooms,
      available: availableCount,
      occupied: occupiedCount,
      reserved: reservedCount,
      outOfService: outOfServiceCount,
      rate: occupancyRate,
    };
  }, [currentHotelRooms, daysRange, roomBookingMap]);

  // Room type filter options
  const roomTypeOptions = useMemo(() => {
    const types = new Set<string>();
    currentHotelRooms.forEach((r) => {
      if (r.roomTypeName) types.add(r.roomTypeName);
    });
    return [
      { value: 'ALL', label: `Tất cả hạng phòng (${types.size})` },
      ...Array.from(types).map((t) => ({ value: t, label: t })),
    ];
  }, [currentHotelRooms]);

  // Filtered rooms grouped by Room Type (Hạng phòng)
  const filteredRoomGroups = useMemo(() => {
    const groups = new Map<string, RoomInstanceDto[]>();
    const query = searchQuery.trim().toLowerCase();

    currentHotelRooms.forEach((room) => {
      const typeName = room.roomTypeName || 'KHÁC';

      // 1. Lọc theo Hạng phòng
      if (selectedRoomTypeFilter !== 'ALL' && typeName !== selectedRoomTypeFilter) {
        return;
      }

      // 2. Lọc theo Search Query (Số phòng, Hạng phòng, hoặc Tên khách trong dải ngày)
      if (query) {
        const matchNumber = room.roomNumber.toLowerCase().includes(query);
        const matchType = typeName.toLowerCase().includes(query);
        let matchGuest = false;
        for (const day of daysRange) {
          const bookingInfo = roomBookingMap.get(`${room.id}_${day.dateStr}`);
          if (bookingInfo?.booking?.guestName?.toLowerCase().includes(query)) {
            matchGuest = true;
            break;
          }
        }
        if (!matchNumber && !matchType && !matchGuest) {
          return;
        }
      }

      // 3. Lọc theo Trạng thái phòng (Occupancy / Status)
      if (statusFilter !== 'ALL') {
        let hasOccupied = false;
        let hasAvailable = false;
        const isOutOfService =
          room.currentStatus === 'MAINTENANCE' ||
          room.currentStatus === 'CLEANING' ||
          room.currentStatus === 'DIRTY' ||
          (room.currentStatus as string) === 'BLOCKED' ||
          (room.currentStatus as string) === 'OUT_OF_SERVICE';

        for (const day of daysRange) {
          const cellKey = `${room.id}_${day.dateStr}`;
          if (roomBookingMap.has(cellKey)) {
            hasOccupied = true;
          } else {
            hasAvailable = true;
          }
        }

        if (statusFilter === 'AVAILABLE' && !hasAvailable) return;
        if (statusFilter === 'OCCUPIED' && !hasOccupied) return;
        if (statusFilter === 'OUT_OF_SERVICE' && !isOutOfService) return;
      }

      if (!groups.has(typeName)) {
        groups.set(typeName, []);
      }
      groups.get(typeName)!.push(room);
    });

    // Sort room numbers within each group
    groups.forEach((roomList) => {
      roomList.sort((a, b) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true }));
    });

    // Sort room type groups deterministically (STD -> SUP -> DLX -> STE ... then min room number)
    return Array.from(groups.entries()).sort(([typeA, roomsA], [typeB, roomsB]) => {
      const codeA = (roomsA[0]?.roomTypeCode || '').toUpperCase();
      const codeB = (roomsB[0]?.roomTypeCode || '').toUpperCase();
      const nameA = typeA.toUpperCase();
      const nameB = typeB.toUpperCase();

      const getPrio = (code: string, name: string) => {
        if (ROOM_TYPE_PRIORITY[code]) return ROOM_TYPE_PRIORITY[code];
        for (const [key, prio] of Object.entries(ROOM_TYPE_PRIORITY)) {
          if (name.includes(key)) return prio;
        }
        return 99;
      };

      const prioA = getPrio(codeA, nameA);
      const prioB = getPrio(codeB, nameB);
      if (prioA !== prioB) return prioA - prioB;

      const minRoomA = roomsA[0]?.roomNumber || '';
      const minRoomB = roomsB[0]?.roomNumber || '';
      const roomCompare = minRoomA.localeCompare(minRoomB, undefined, { numeric: true });
      if (roomCompare !== 0) return roomCompare;

      return typeA.localeCompare(typeB);
    });
  }, [currentHotelRooms, selectedRoomTypeFilter, searchQuery, statusFilter, daysRange, roomBookingMap]);

  // Dynamic Occupancy Rollup per Room Type per Date
  const groupOccupancy = useMemo(() => {
    const map = new Map<string, Map<string, { total: number; busy: number; available: number }>>();

    filteredRoomGroups.forEach(([typeName, gRooms]) => {
      const dateMap = new Map<string, { total: number; busy: number; available: number }>();
      const total = gRooms.length;

      daysRange.forEach((day) => {
        let busy = 0;
        gRooms.forEach((r) => {
          const cellKey = `${r.id}_${day.dateStr}`;
          if (roomBookingMap.has(cellKey)) {
            busy++;
          } else if (
            day.isToday &&
            (r.currentStatus === 'MAINTENANCE' ||
              r.currentStatus === 'CLEANING' ||
              r.currentStatus === 'DIRTY' ||
              (r.currentStatus as string) === 'BLOCKED' ||
              (r.currentStatus as string) === 'OUT_OF_SERVICE')
          ) {
            busy++;
          }
        });
        dateMap.set(day.dateStr, { total, busy, available: Math.max(0, total - busy) });
      });

      map.set(typeName, dateMap);
    });

    return map;
  }, [filteredRoomGroups, daysRange, roomBookingMap]);

  // Accordion toggle handlers
  const toggleGroupCollapse = (roomTypeName: string) => {
    setCollapsedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(roomTypeName)) {
        next.delete(roomTypeName);
      } else {
        next.add(roomTypeName);
      }
      return next;
    });
  };

  const expandAllGroups = () => setCollapsedGroups(new Set());
  const collapseAllGroups = () => {
    const allTypes = new Set(filteredRoomGroups.map(([name]) => name));
    setCollapsedGroups(allTypes);
  };

  // Check if all groups are currently expanded or collapsed for active button feedback
  const allGroupsCount = filteredRoomGroups.length;
  const isAllExpanded =
    allGroupsCount > 0 && (!searchQuery.trim() ? collapsedGroups.size === 0 : true);
  const isAllCollapsed =
    allGroupsCount > 0 && !searchQuery.trim() && collapsedGroups.size >= allGroupsCount;

  // Row and cell styling according to Density
  const isCompact = density === 'COMPACT';
  const rowHeightClass = isCompact ? 'h-8' : 'h-11';
  const cellFontSizeClass = isCompact ? 'text-[10px]' : 'text-[11px]';
  const cellPaddingClass = isCompact ? 'px-1' : 'px-2';

  return (
    <div className="space-y-4">
      {/* 1. Mini KPI Dashboard */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5">
        {/* Tổng số phòng */}
        <div className="bg-white border border-neutral-200 rounded-xl p-3 shadow-2xs flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-neutral-100 text-neutral-800 flex items-center justify-center shrink-0">
            <Building className="w-4 h-4 text-neutral-700" />
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-medium text-neutral-500 block truncate">Tổng số phòng</span>
            <span className="text-base font-black text-neutral-900">{kpiStats.total}</span>
          </div>
        </div>

        {/* Sẵn sàng (Trống) */}
        <div
          onClick={() => setStatusFilter(statusFilter === 'AVAILABLE' ? 'ALL' : 'AVAILABLE')}
          className={`border rounded-xl p-3 shadow-2xs flex items-center gap-3 cursor-pointer transition-all ${
            statusFilter === 'AVAILABLE'
              ? 'bg-emerald-50 border-emerald-500 ring-2 ring-emerald-500'
              : 'bg-white border-neutral-200 hover:border-emerald-300'
          }`}
          title="Bấm để lọc phòng sẵn sàng"
        >
          <div className="w-9 h-9 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-medium text-neutral-500 block truncate">Sẵn sàng (Trống)</span>
            <span className="text-base font-black text-emerald-700">{kpiStats.available}</span>
          </div>
        </div>

        {/* Đang ở */}
        <div
          onClick={() => setStatusFilter(statusFilter === 'OCCUPIED' ? 'ALL' : 'OCCUPIED')}
          className={`border rounded-xl p-3 shadow-2xs flex items-center gap-3 cursor-pointer transition-all ${
            statusFilter === 'OCCUPIED'
              ? 'bg-blue-50 border-blue-500 ring-2 ring-blue-500'
              : 'bg-white border-neutral-200 hover:border-blue-300'
          }`}
          title="Bấm để lọc phòng đang có khách"
        >
          <div className="w-9 h-9 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center shrink-0">
            <BedDouble className="w-4 h-4 text-blue-600" />
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-medium text-neutral-500 block truncate">Đang ở (Occupied)</span>
            <span className="text-base font-black text-blue-700">{kpiStats.occupied}</span>
          </div>
        </div>

        {/* Đã đặt trước */}
        <div
          onClick={() => setStatusFilter(statusFilter === 'OCCUPIED' ? 'ALL' : 'OCCUPIED')}
          className={`border rounded-xl p-3 shadow-2xs flex items-center gap-3 cursor-pointer transition-all ${
            statusFilter === 'OCCUPIED'
              ? 'bg-orange-50 border-orange-500 ring-2 ring-orange-500'
              : 'bg-white border-neutral-200 hover:border-orange-300'
          }`}
          title="Bấm để lọc phòng đã đặt"
        >
          <div className="w-9 h-9 rounded-lg bg-orange-100 text-orange-700 flex items-center justify-center shrink-0">
            <Clock className="w-4 h-4 text-orange-600" />
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-medium text-neutral-500 block truncate">Đã đặt trước</span>
            <span className="text-base font-black text-orange-700">{kpiStats.reserved}</span>
          </div>
        </div>

        {/* Bảo trì / Dọn dẹp */}
        <div
          onClick={() => setStatusFilter(statusFilter === 'OUT_OF_SERVICE' ? 'ALL' : 'OUT_OF_SERVICE')}
          className={`border rounded-xl p-3 shadow-2xs flex items-center gap-3 cursor-pointer transition-all ${
            statusFilter === 'OUT_OF_SERVICE'
              ? 'bg-purple-50 border-purple-500 ring-2 ring-purple-500'
              : 'bg-white border-neutral-200 hover:border-purple-300'
          }`}
          title="Bấm để lọc phòng đang dọn / bảo trì"
        >
          <div className="w-9 h-9 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center shrink-0">
            <Wrench className="w-4 h-4 text-purple-600" />
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-medium text-neutral-500 block truncate">Dọn / Bảo trì</span>
            <span className="text-base font-black text-purple-700">{kpiStats.outOfService}</span>
          </div>
        </div>

        {/* Tỉ lệ lấp đầy */}
        <div className="bg-white border border-neutral-200 rounded-xl p-3 shadow-2xs flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-red-100 text-red-700 flex items-center justify-center shrink-0">
            <span className="text-xs font-black text-red-600">%</span>
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-medium text-neutral-500 block truncate">Công suất phòng</span>
            <span className="text-base font-black text-red-600">{kpiStats.rate}%</span>
          </div>
        </div>
      </div>

      {/* 2. Filter & Date Controls Card */}
      <div className="bg-white border border-neutral-200 rounded-xl p-4 shadow-xs space-y-3.5">
        {/* ROW 1: Hotel + Dates + Presets + Shift */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-3.5 items-end">
          {/* KHÁCH SẠN */}
          <div className="md:col-span-4">
            <label className="block text-xs font-bold text-red-600 uppercase mb-1.5 flex items-center gap-1.5">
              <Building className="w-4 h-4 text-red-600 shrink-0" />
              <span>KHÁCH SẠN *</span>
            </label>
            <Select<number>
              value={selectedHotelId || (hotelList[0]?.id ?? 0)}
              onChange={(val) => onSelectHotelId && onSelectHotelId(Number(val))}
              options={
                hotelList.length > 0
                  ? hotelList.map((h) => ({ value: h.id, label: h.name }))
                  : [
                      {
                        value: 0,
                        label: isLoading ? 'Đang tải danh sách khách sạn...' : 'Chưa có dữ liệu khách sạn',
                        disabled: true,
                      },
                    ]
              }
              size="md"
            />
          </div>

          {/* TỪ NGÀY */}
          <div className="md:col-span-3">
            <label className="block text-xs font-bold text-red-600 uppercase mb-1.5 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-red-600 shrink-0" />
              <span>TỪ NGÀY *</span>
            </label>
            <input
              type="date"
              value={startDateStr}
              onChange={(e) => setStartDateStr(e.target.value)}
              className="w-full h-11 px-3.5 bg-white border border-neutral-300 rounded-lg text-sm font-medium text-neutral-800 focus:ring-2 focus:ring-red-500 focus:border-red-500 focus:outline-none transition-all cursor-pointer"
            />
          </div>

          {/* ĐẾN NGÀY */}
          <div className="md:col-span-3">
            <label className="block text-xs font-bold text-red-600 uppercase mb-1.5 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-red-600 shrink-0" />
              <span>ĐẾN NGÀY *</span>
            </label>
            <input
              type="date"
              value={endDateStr}
              onChange={(e) => setEndDateStr(e.target.value)}
              className="w-full h-11 px-3.5 bg-white border border-neutral-300 rounded-lg text-sm font-medium text-neutral-800 focus:ring-2 focus:ring-red-500 focus:border-red-500 focus:outline-none transition-all cursor-pointer"
            />
          </div>

          {/* QUICK SHIFT BUTTONS */}
          <div className="md:col-span-2 flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => handleShiftDays(-7)}
              className="flex-1 h-11 px-2.5 bg-white border border-neutral-300 hover:bg-neutral-50 active:bg-neutral-100 text-neutral-700 text-xs font-bold rounded-lg transition-colors whitespace-nowrap shadow-2xs cursor-pointer flex items-center justify-center gap-1"
              title="Lùi 7 ngày"
            >
              <span>‹ Lùi 7 ngày</span>
            </button>
            <button
              type="button"
              onClick={() => handleShiftDays(7)}
              className="flex-1 h-11 px-2.5 bg-white border border-neutral-300 hover:bg-neutral-50 active:bg-neutral-100 text-neutral-700 text-xs font-bold rounded-lg transition-colors whitespace-nowrap shadow-2xs cursor-pointer flex items-center justify-center gap-1"
              title="Tiến 7 ngày"
            >
              <span>Tiến 7 ngày ›</span>
            </button>
          </div>
        </div>

        {/* ROW 2: Date Presets & Matrix Quick Filters & Density Switcher */}
        <div className="pt-2.5 border-t border-neutral-200 flex flex-wrap items-center justify-between gap-3 text-xs">
          {/* Left: Quick Date Presets */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-[11px] font-semibold text-neutral-500 mr-1 flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5 text-neutral-400" />
              Xem nhanh:
            </span>
            <button
              type="button"
              onClick={() => setDatePreset('TODAY')}
              className="px-2.5 py-1 rounded-lg border border-neutral-200 bg-neutral-50 hover:bg-white text-neutral-700 hover:text-red-600 font-semibold cursor-pointer transition-colors shadow-2xs"
            >
              Hôm nay
            </button>
            <button
              type="button"
              onClick={() => setDatePreset('7_DAYS')}
              className="px-2.5 py-1 rounded-lg border border-neutral-200 bg-neutral-50 hover:bg-white text-neutral-700 hover:text-red-600 font-semibold cursor-pointer transition-colors shadow-2xs"
            >
              7 ngày
            </button>
            <button
              type="button"
              onClick={() => setDatePreset('14_DAYS')}
              className="px-2.5 py-1 rounded-lg border border-neutral-200 bg-neutral-50 hover:bg-white text-neutral-700 hover:text-red-600 font-semibold cursor-pointer transition-colors shadow-2xs"
            >
              14 ngày
            </button>
            <button
              type="button"
              onClick={() => setDatePreset('30_DAYS')}
              className="px-2.5 py-1 rounded-lg border border-neutral-200 bg-neutral-50 hover:bg-white text-neutral-700 hover:text-red-600 font-semibold cursor-pointer transition-colors shadow-2xs"
            >
              30 ngày
            </button>
          </div>

          {/* Right: Expand / Collapse All & Density Switcher */}
          <div className="flex items-center gap-2 flex-wrap ml-auto">
            {/* Accordion Controls with Visual Active Feedback */}
            <div className="inline-flex rounded-lg p-0.5 bg-neutral-100 border border-neutral-200">
              <button
                type="button"
                onClick={expandAllGroups}
                className={`px-2 py-1 text-[11px] font-semibold rounded cursor-pointer transition-all flex items-center gap-1 ${
                  isAllExpanded
                    ? 'bg-white text-emerald-700 shadow-2xs font-bold border border-neutral-200/80'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
                title="Mở rộng tất cả các hạng phòng"
              >
                <Layers
                  className={`w-3.5 h-3.5 ${
                    isAllExpanded ? 'text-emerald-600' : 'text-neutral-500'
                  }`}
                />
                <span>Mở tất cả</span>
                {isAllExpanded && (
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 ml-0.5" />
                )}
              </button>
              <button
                type="button"
                onClick={collapseAllGroups}
                className={`px-2 py-1 text-[11px] font-semibold rounded cursor-pointer transition-all flex items-center gap-1 ${
                  isAllCollapsed
                    ? 'bg-white text-red-600 shadow-2xs font-bold border border-neutral-200/80'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
                title="Thu gọn tất cả các hạng phòng"
              >
                <Rows3
                  className={`w-3.5 h-3.5 ${isAllCollapsed ? 'text-red-600' : 'text-neutral-500'}`}
                />
                <span>Thu gọn tất cả</span>
                {isAllCollapsed && <span className="w-1.5 h-1.5 rounded-full bg-red-600 ml-0.5" />}
              </button>
            </div>

            {/* Density Switcher */}
            <div className="inline-flex rounded-lg p-0.5 bg-neutral-100 border border-neutral-200">
              <button
                type="button"
                onClick={() => setDensity('STANDARD')}
                className={`px-2 py-1 text-[11px] font-semibold rounded cursor-pointer transition-all flex items-center gap-1 ${
                  density === 'STANDARD'
                    ? 'bg-white text-neutral-900 shadow-2xs font-bold'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
                title="Chế độ hiển thị tiêu chuẩn (hàng 44px)"
              >
                <LayoutGrid className="w-3.5 h-3.5" />
                Chuẩn
              </button>
              <button
                type="button"
                onClick={() => setDensity('COMPACT')}
                className={`px-2 py-1 text-[11px] font-semibold rounded cursor-pointer transition-all flex items-center gap-1 ${
                  density === 'COMPACT'
                    ? 'bg-white text-red-600 shadow-2xs font-bold'
                    : 'text-neutral-600 hover:text-red-600'
                }`}
                title="Chế độ thu nhỏ mật độ cao (hàng 32px - xem nhiều phòng cùng lúc)"
              >
                <Rows3 className="w-3.5 h-3.5" />
                Thu gọn
              </button>
            </div>

            {/* Auto-Refresh Control */}
            <div className="inline-flex items-center gap-1.5 pl-2 border-l border-neutral-200">
              <div className="inline-flex rounded-lg p-0.5 bg-neutral-100 border border-neutral-200 text-[11px] font-semibold">
                <button
                  type="button"
                  onClick={() => {
                    setAutoRefreshInterval(0);
                    setSecondsRemaining(0);
                  }}
                  className={`px-2 py-1 rounded cursor-pointer transition-all ${
                    autoRefreshInterval === 0
                      ? 'bg-white text-neutral-900 shadow-2xs font-bold'
                      : 'text-neutral-500 hover:text-neutral-800'
                  }`}
                  title="Tắt tự động làm mới"
                >
                  Tắt
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setAutoRefreshInterval(30);
                    setSecondsRemaining(30);
                  }}
                  className={`px-2 py-1 rounded cursor-pointer transition-all ${
                    autoRefreshInterval === 30
                      ? 'bg-white text-red-600 shadow-2xs font-bold'
                      : 'text-neutral-500 hover:text-neutral-800'
                  }`}
                  title="Tự động làm mới mỗi 30 giây"
                >
                  30s
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setAutoRefreshInterval(60);
                    setSecondsRemaining(60);
                  }}
                  className={`px-2 py-1 rounded cursor-pointer transition-all ${
                    autoRefreshInterval === 60
                      ? 'bg-white text-red-600 shadow-2xs font-bold'
                      : 'text-neutral-500 hover:text-neutral-800'
                  }`}
                  title="Tự động làm mới mỗi 60 giây (Khuyên dùng)"
                >
                  60s
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setAutoRefreshInterval(120);
                    setSecondsRemaining(120);
                  }}
                  className={`px-2 py-1 rounded cursor-pointer transition-all ${
                    autoRefreshInterval === 120
                      ? 'bg-white text-red-600 shadow-2xs font-bold'
                      : 'text-neutral-500 hover:text-neutral-800'
                  }`}
                  title="Tự động làm mới mỗi 2 phút"
                >
                  2p
                </button>
              </div>

              {autoRefreshInterval > 0 && (
                <span
                  className="px-2 py-1 rounded-lg bg-red-50 border border-red-200 text-red-700 text-[11px] font-bold flex items-center gap-1 shadow-2xs"
                  title="Thời gian đến lượt làm mới tiếp theo (tự động tạm dừng khi mở modal)"
                >
                  <Clock className="w-3 h-3 text-red-600 animate-pulse" />
                  <span>{secondsRemaining}s</span>
                </span>
              )}

              <button
                type="button"
                onClick={() => {
                  setSecondsRemaining(autoRefreshInterval);
                  onRefresh();
                }}
                disabled={isLoading}
                className="p-1.5 rounded-lg border border-neutral-200 bg-white hover:bg-neutral-50 text-neutral-700 hover:text-red-600 transition-all cursor-pointer shadow-2xs disabled:opacity-50"
                title="Làm mới ma trận ngay lập tức"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-red-600' : ''}`} />
              </button>
            </div>
          </div>
        </div>

        {/* ROW 3: Matrix Search & Filter Bar */}
        <div className="pt-2.5 border-t border-neutral-200 flex flex-wrap items-center justify-between gap-3">
          {/* Ô tìm kiếm số phòng hoặc tên khách */}
          <div className="relative flex-1 min-w-[220px] max-w-sm">
            <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo số phòng (101C) hoặc tên khách..."
              className="w-full pl-9 pr-8 py-1.5 text-xs bg-neutral-50 hover:bg-white focus:bg-white border border-neutral-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500 transition-all font-medium text-neutral-800"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600 p-0.5 cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Lọc theo Hạng phòng Dropdown */}
          <div className="w-56 shrink-0">
            <Select<string>
              value={selectedRoomTypeFilter}
              onChange={(val) => setSelectedRoomTypeFilter(String(val))}
              options={roomTypeOptions}
              size="sm"
            />
          </div>

          {/* Lọc theo Trạng thái Pills */}
          <div className="flex items-center gap-1.5 flex-wrap ml-auto">
            <span className="text-[11px] font-semibold text-neutral-500 mr-1 flex items-center gap-1">
              <Filter className="w-3.5 h-3.5 text-neutral-400" />
              Lọc phòng:
            </span>
            <button
              type="button"
              onClick={() => setStatusFilter('ALL')}
              className={`px-2.5 py-1 text-xs rounded-lg font-semibold transition-all cursor-pointer ${
                statusFilter === 'ALL'
                  ? 'bg-neutral-900 text-white shadow-2xs'
                  : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
              }`}
            >
              Tất cả
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('AVAILABLE')}
              className={`px-2.5 py-1 text-xs rounded-lg font-semibold transition-all cursor-pointer flex items-center gap-1 ${
                statusFilter === 'AVAILABLE'
                  ? 'bg-emerald-600 text-white shadow-2xs'
                  : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
              Sẵn sàng
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('OCCUPIED')}
              className={`px-2.5 py-1 text-xs rounded-lg font-semibold transition-all cursor-pointer flex items-center gap-1 ${
                statusFilter === 'OCCUPIED'
                  ? 'bg-blue-600 text-white shadow-2xs'
                  : 'bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
              Đang bận
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('OUT_OF_SERVICE')}
              className={`px-2.5 py-1 text-xs rounded-lg font-semibold transition-all cursor-pointer flex items-center gap-1 ${
                statusFilter === 'OUT_OF_SERVICE'
                  ? 'bg-purple-600 text-white shadow-2xs'
                  : 'bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
              Bảo trì / Dọn
            </button>
          </div>
        </div>
      </div>

      {/* 3. Timeline Matrix Grid with Collapsible Groups & Occupancy Rollup */}
      <div className="bg-white border border-neutral-300 rounded-xl shadow-xs overflow-hidden isolate">
        <div className="overflow-x-auto max-h-[640px] scrollbar-thin">
          <table className="w-full border-separate border-spacing-0 text-left bg-white">
            {/* Table Header: Solid Black matching Brand Standards */}
            <thead className="sticky top-0 z-30 bg-neutral-900 text-white">
              <tr className="bg-neutral-900">
                <th
                  scope="col"
                  className="sticky top-0 left-0 z-40 w-[200px] min-w-[200px] max-w-[200px] p-3 text-center text-xs font-black uppercase tracking-wider bg-neutral-900 text-white border-t border-b border-r border-l border-neutral-700 whitespace-nowrap"
                >
                  LOẠI PHÒNG
                </th>
                <th
                  scope="col"
                  className="sticky top-0 left-[200px] z-40 w-[80px] min-w-[80px] max-w-[80px] p-3 text-center text-xs font-black uppercase tracking-wider bg-neutral-900 text-white border-t border-b border-r border-neutral-700 whitespace-nowrap"
                >
                  PHÒNG
                </th>
                {daysRange.map((day) => (
                  <th
                    key={day.dateStr}
                    scope="col"
                    className={`sticky top-0 z-30 w-[100px] min-w-[100px] max-w-[100px] p-2 text-center bg-neutral-900 text-white border-t border-b border-r border-neutral-700 whitespace-nowrap ${
                      day.isToday ? 'bg-neutral-800 ring-1 ring-red-500/50' : ''
                    }`}
                  >
                    <div className="text-[11px] font-bold text-neutral-300 uppercase">
                      {day.dayName}
                    </div>
                    <div className="text-xs font-black tracking-tight text-white mt-0.5">
                      {day.dateDisplay}
                    </div>
                  </th>
                ))}
              </tr>
            </thead>

            {/* Table Body: Collapsible Grouped by Room Type */}
            <tbody className="text-xs bg-white">
              {filteredRoomGroups.length === 0 ? (
                <tr>
                  <td
                    colSpan={daysRange.length + 2}
                    className="p-12 text-center text-neutral-400 font-medium border-b border-neutral-300"
                  >
                    {isLoading
                      ? 'Đang tải ma trận phòng...'
                      : 'Không tìm thấy phòng vật lý nào phù hợp với bộ lọc hiện tại.'}
                  </td>
                </tr>
              ) : (
                filteredRoomGroups.map(([roomTypeName, groupRooms]) => {
                  const isCollapsed = !searchQuery.trim() && collapsedGroups.has(roomTypeName);

                  return (
                    <React.Fragment key={roomTypeName}>
                      {/* GROUP HEADER ROW (Accordion with Dynamic Occupancy Rollup) */}
                      <tr className="bg-neutral-100 hover:bg-neutral-200/90 transition-colors border-b border-neutral-300">
                        {/* Colspan 2: Sticky Left Group Header Label */}
                        <td
                          colSpan={2}
                          onClick={() => toggleGroupCollapse(roomTypeName)}
                          className="sticky left-0 z-25 w-[280px] min-w-[280px] max-w-[280px] px-3.5 py-2 font-black text-xs uppercase bg-neutral-100 hover:bg-neutral-200 text-neutral-900 border-b border-r border-l border-neutral-300 cursor-pointer select-none transition-colors"
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2 min-w-0">
                              {isCollapsed ? (
                                <ChevronRight className="w-4 h-4 text-neutral-600 shrink-0" />
                              ) : (
                                <ChevronDown className="w-4 h-4 text-neutral-600 shrink-0" />
                              )}
                              <span className="truncate" title={roomTypeName}>
                                {roomTypeName}
                              </span>
                            </div>
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-neutral-200 text-neutral-700 ml-2 shrink-0">
                              {groupRooms.length} phòng
                            </span>
                          </div>
                        </td>

                        {/* Rollup Summary Cells across all date columns */}
                        {daysRange.map((day) => {
                          const stats = groupOccupancy.get(roomTypeName)?.get(day.dateStr) || {
                            total: groupRooms.length,
                            busy: 0,
                            available: groupRooms.length,
                          };
                          const isSoldOut = stats.available === 0;
                          const isLow = stats.available > 0 && stats.available <= 3;

                          return (
                            <td
                              key={day.dateStr}
                              onClick={() => toggleGroupCollapse(roomTypeName)}
                              className="w-[100px] min-w-[100px] max-w-[100px] h-9 px-1 border-b border-r border-neutral-300 text-center cursor-pointer select-none bg-neutral-50 hover:bg-neutral-100 transition-colors"
                              title={`${roomTypeName} ngày ${day.dateDisplay}: Trống ${stats.available}/${stats.total} phòng (Bấm để ${isCollapsed ? 'mở rộng' : 'thu gọn'})`}
                            >
                              <span
                                className={`inline-block px-1.5 py-0.5 rounded text-[10px] font-bold truncate max-w-full ${
                                  isSoldOut
                                    ? 'bg-rose-100 text-rose-800 border border-rose-300 font-black'
                                    : isLow
                                    ? 'bg-amber-100 text-amber-900 border border-amber-300'
                                    : 'bg-emerald-100 text-emerald-900 border border-emerald-300'
                                }`}
                              >
                                {isSoldOut
                                  ? `Hết (0/${stats.total})`
                                  : `${stats.available}/${stats.total} trống`}
                              </span>
                            </td>
                          );
                        })}
                      </tr>

                      {/* PHYSICAL ROOM ROWS (Rendered when group is expanded) */}
                      {!isCollapsed &&
                        groupRooms.map((room) => {
                          return (
                            <tr
                              key={room.id}
                              className="hover:bg-neutral-50/70 transition-colors"
                            >
                              {/* Col 1: Room Type (Subtle tree branch indicator) */}
                              <td
                                className="sticky left-0 z-20 w-[200px] min-w-[200px] max-w-[200px] p-2 pl-6 font-medium text-neutral-500 text-[11px] bg-white border-b border-r border-l border-neutral-200 truncate"
                                title={roomTypeName}
                              >
                                <span className="text-neutral-400 mr-1.5">↳</span>
                                {roomTypeName}
                              </td>

                              {/* Col 2: PHÒNG Number */}
                              <td className="sticky left-[200px] z-20 w-[80px] min-w-[80px] max-w-[80px] p-2 text-center font-black text-xs bg-white text-neutral-900 border-b border-r border-neutral-300 whitespace-nowrap">
                                {room.roomNumber}
                              </td>

                              {/* Days Cells: Flat 1-cell layout matching density */}
                              {daysRange.map((day) => {
                                const cellKey = `${room.id}_${day.dateStr}`;
                                const bookingInfo = roomBookingMap.get(cellKey);

                                // Determine status type and styling
                                let statusType: 'READY' | 'OCCUPIED' | 'RESERVED' | 'CLEANING' | 'MAINTENANCE' | 'BLOCKED' = 'READY';
                                let cellBgClass = 'bg-white text-neutral-700 hover:bg-neutral-100';
                                let cellText = 'SẴN SÀNG';

                                if (bookingInfo) {
                                  const rStatus = bookingInfo.bookingRoom.status;
                                  if (rStatus === 'CHECKED_IN') {
                                    statusType = 'OCCUPIED';
                                    cellBgClass = 'bg-[#2563EB] text-white hover:bg-blue-700 font-bold';
                                    cellText = bookingInfo.booking.guestName || 'ĐANG Ở';
                                  } else {
                                    statusType = 'RESERVED';
                                    cellBgClass = 'bg-[#FED7AA] text-orange-950 hover:bg-orange-200 font-bold';
                                    cellText = bookingInfo.booking.guestName || 'ĐÃ ĐẶT';
                                  }
                                } else if (day.isToday) {
                                  // Only apply current physical room status for TODAY
                                  if (room.currentStatus === 'CLEANING' || room.currentStatus === 'DIRTY') {
                                    statusType = 'CLEANING';
                                    cellBgClass = 'bg-[#E9D5FF] text-purple-950 hover:bg-purple-200 font-bold';
                                    cellText = room.currentStatus === 'DIRTY' ? 'CHỜ DỌN' : 'ĐANG DỌN';
                                  } else if (room.currentStatus === 'MAINTENANCE') {
                                    statusType = 'MAINTENANCE';
                                    cellBgClass = 'bg-[#9CA3AF] text-white hover:bg-neutral-500 font-bold';
                                    cellText = 'BẢO TRÌ';
                                  } else if (
                                    (room.currentStatus as string) === 'BLOCKED' ||
                                    (room.currentStatus as string) === 'OUT_OF_SERVICE'
                                  ) {
                                    statusType = 'BLOCKED';
                                    cellBgClass = 'bg-[#FDE047] text-yellow-950 hover:bg-yellow-400 font-bold';
                                    cellText = 'KHÓA ẢO';
                                  }
                                }

                                return (
                                  <td
                                    key={day.dateStr}
                                    onClick={() =>
                                      setActiveCellInfo({
                                        room,
                                        dateStr: day.dateStr,
                                        booking: bookingInfo?.booking,
                                        bookingRoom: bookingInfo?.bookingRoom,
                                        bookingDetail: bookingInfo?.bookingDetail,
                                        statusType,
                                      })
                                    }
                                    className={`w-[100px] min-w-[100px] max-w-[100px] ${rowHeightClass} ${cellPaddingClass} border-b border-r border-neutral-300 text-center ${cellFontSizeClass} font-bold uppercase tracking-tight transition-colors cursor-pointer select-none truncate ${cellBgClass}`}
                                    title={`Phòng ${room.roomNumber} - ${day.dateDisplay}: ${cellText}`}
                                  >
                                    <span className="block truncate">{cellText}</span>
                                  </td>
                                );
                              })}
                            </tr>
                          );
                        })}
                    </React.Fragment>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 4. Legend Box */}
      <div className="bg-white border border-neutral-200 rounded-xl p-4 shadow-xs">
        <h3 className="text-xs font-bold text-neutral-800 uppercase tracking-wider mb-3">
          CHÚ GIẢI TRẠNG THÁI PHÒNG
        </h3>
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2.5 text-xs text-neutral-700 font-medium">
          {/* Sẵn sàng */}
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-sm bg-white border border-neutral-300 inline-block shadow-2xs"></span>
            <span>Sẵn sàng (READY)</span>
          </div>

          {/* Đang dọn */}
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-sm bg-[#E9D5FF] border border-purple-300 inline-block"></span>
            <span>Đang dọn (CLEANING)</span>
          </div>

          {/* Đã đặt trước */}
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-sm bg-[#FED7AA] border border-orange-300 inline-block"></span>
            <span>Đã đặt trước (RESERVED)</span>
          </div>

          {/* Đang ở */}
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-sm bg-[#2563EB] inline-block"></span>
            <span>Đang ở (OCCUPIED)</span>
          </div>

          {/* Khóa ảo */}
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-sm bg-[#FDE047] border border-yellow-400 inline-block"></span>
            <span>Khóa ảo (BLOCKED)</span>
          </div>

          {/* Bảo trì */}
          <div className="flex items-center gap-2">
            <span className="w-4 h-4 rounded-sm bg-[#9CA3AF] inline-block"></span>
            <span>Bảo trì (MAINTENANCE)</span>
          </div>
        </div>
      </div>

      {/* 5. Cell Detail & Quick Operations Action Panel Modal */}
      {activeCellInfo && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl border border-neutral-200 w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            {/* Popover Header */}
            <div className="flex items-center justify-between p-4 border-b border-neutral-100 bg-neutral-50/80">
              <div className="flex items-center gap-2.5 min-w-0">
                <div
                  className={`w-9 h-9 rounded-xl flex items-center justify-center font-black text-xs shrink-0 shadow-2xs ${
                    activeCellInfo.statusType === 'OCCUPIED'
                      ? 'bg-blue-600 text-white'
                      : activeCellInfo.statusType === 'RESERVED'
                      ? 'bg-orange-500 text-white'
                      : activeCellInfo.statusType === 'CLEANING'
                      ? 'bg-purple-600 text-white'
                      : activeCellInfo.statusType === 'MAINTENANCE'
                      ? 'bg-neutral-600 text-white'
                      : activeCellInfo.statusType === 'BLOCKED'
                      ? 'bg-yellow-500 text-neutral-900'
                      : 'bg-emerald-600 text-white'
                  }`}
                >
                  {activeCellInfo.room.roomNumber}
                </div>
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h4 className="font-black text-neutral-900 text-sm truncate">
                      Phòng {activeCellInfo.room.roomNumber}
                    </h4>
                    <span
                      className={`px-2 py-0.5 rounded-full font-bold text-[10px] shrink-0 ${
                        activeCellInfo.statusType === 'OCCUPIED'
                          ? 'bg-blue-100 text-blue-800 border border-blue-200'
                          : activeCellInfo.statusType === 'RESERVED'
                          ? 'bg-orange-100 text-orange-900 border border-orange-200'
                          : activeCellInfo.statusType === 'CLEANING'
                          ? 'bg-purple-100 text-purple-900 border border-purple-200'
                          : activeCellInfo.statusType === 'MAINTENANCE'
                          ? 'bg-neutral-200 text-neutral-700 border border-neutral-300'
                          : activeCellInfo.statusType === 'BLOCKED'
                          ? 'bg-yellow-100 text-yellow-900 border border-yellow-300'
                          : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                      }`}
                    >
                      {activeCellInfo.statusType === 'OCCUPIED'
                        ? 'Đang ở'
                        : activeCellInfo.statusType === 'RESERVED'
                        ? 'Đã đặt trước'
                        : activeCellInfo.statusType === 'CLEANING'
                        ? (activeCellInfo.room.currentStatus === 'DIRTY' ? 'Chờ dọn' : 'Đang dọn')
                        : activeCellInfo.statusType === 'MAINTENANCE'
                        ? 'Bảo trì'
                        : activeCellInfo.statusType === 'BLOCKED'
                        ? 'Khóa ảo'
                        : 'Sẵn sàng'}
                    </span>
                  </div>
                  <p className="text-[11px] text-neutral-500 font-medium truncate">
                    {activeCellInfo.room.roomTypeName} • Ngày {activeCellInfo.dateStr}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setActiveCellInfo(null)}
                className="text-neutral-400 hover:text-neutral-600 p-1.5 rounded-lg hover:bg-neutral-100 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Popover Body */}
            <div className="p-5 space-y-4 text-xs">
              {/* Customer / Booking Summary Card (If Booking exists) */}
              {activeCellInfo.booking && activeCellInfo.bookingRoom ? (
                <div className="bg-neutral-50 border border-neutral-200/80 rounded-xl p-3.5 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-bold text-neutral-500 uppercase tracking-wider">
                      Thông Tin Lưu Trú
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        const booking = activeCellInfo.booking!;
                        setActiveCellInfo(null);
                        onOpenDetail?.(booking);
                      }}
                      className="text-red-600 hover:text-red-700 font-bold text-[11px] flex items-center gap-1 cursor-pointer"
                    >
                      <span>Xem hồ sơ đơn</span>
                      <Eye className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-xs pt-1">
                    <div>
                      <div className="text-neutral-400 text-[10px]">MÃ ĐẶT PHÒNG</div>
                      <div className="font-mono font-bold text-neutral-900">
                        {activeCellInfo.booking.bookingNumber}
                      </div>
                    </div>
                    <div>
                      <div className="text-neutral-400 text-[10px]">KHÁCH HÀNG</div>
                      <div className="font-bold text-neutral-900 truncate">
                        {activeCellInfo.booking.guestName || 'Khách vãng lai'}
                      </div>
                    </div>
                    <div>
                      <div className="text-neutral-400 text-[10px]">NGÀY NHẬN / TRẢ</div>
                      <div className="font-medium text-neutral-800">
                        {activeCellInfo.bookingDetail?.checkInDate || 'N/A'} ➔{' '}
                        {activeCellInfo.bookingDetail?.checkOutDate || 'N/A'}
                      </div>
                    </div>
                    <div>
                      <div className="text-neutral-400 text-[10px]">SỐ ĐIỆN THOẠI</div>
                      <div className="font-medium text-neutral-800 truncate">
                        {activeCellInfo.booking.guestPhone || 'Chưa cung cấp'}
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="bg-neutral-50 border border-neutral-200/80 rounded-xl p-3 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <BedDouble className="w-4 h-4 text-neutral-500" />
                    <span className="font-bold text-neutral-800">
                      Phòng trống, sẵn sàng đón khách
                    </span>
                  </div>
                  <span className="text-[11px] text-neutral-500">
                    Chưa có lượt đặt ngày này
                  </span>
                </div>
              )}

              {/* ACTION BUTTONS GRID */}
              <div>
                <div className="text-[11px] font-bold text-neutral-600 uppercase tracking-wider mb-2.5 flex items-center gap-1.5">
                  <Layers className="w-3.5 h-3.5 text-neutral-500" />
                  <span>Thao Tác Trực Tiếp (Quick Operations)</span>
                </div>

                <div className="grid grid-cols-2 gap-2.5">
                  {/* CASE 1: OCCUPIED (ĐANG Ở) */}
                  {activeCellInfo.statusType === 'OCCUPIED' && (
                    <>
                      {/* Gọi đồ ăn / POS */}
                      <button
                        type="button"
                        onClick={() => {
                          const target = {
                            bookingId: activeCellInfo.booking!.id,
                            bookingNumber: activeCellInfo.booking!.bookingNumber,
                            bookingRoomId: activeCellInfo.bookingRoom?.id,
                            roomInstanceId: activeCellInfo.room.id,
                            roomNumber: activeCellInfo.room.roomNumber,
                            hotelId:
                              activeCellInfo.booking!.hotelId || selectedHotelId || 1,
                          };
                          setActiveCellInfo(null);
                          onOpenServiceOrder?.(target);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-amber-200 bg-amber-50 hover:bg-amber-100 text-amber-950 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-amber-500 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <Utensils className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Gọi Đồ Ăn / Dịch Vụ
                          </div>
                          <div className="text-[10px] text-amber-700 font-normal truncate">
                            Menu POS, đồ uống, minibar
                          </div>
                        </div>
                      </button>

                      {/* Check-out & Quyết toán */}
                      <button
                        type="button"
                        onClick={() => {
                          const bookingRoom = activeCellInfo.bookingRoom!;
                          setActiveCellInfo(null);
                          onOpenCheckout?.(bookingRoom);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-red-200 bg-red-50 hover:bg-red-100 text-red-950 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-red-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <DoorOpen className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Check-out & Quyết Toán
                          </div>
                          <div className="text-[10px] text-red-700 font-normal truncate">
                            Đóng sổ công nợ về 0đ
                          </div>
                        </div>
                      </button>

                      {/* Đổi phòng đang ở (Room Move) */}
                      <button
                        type="button"
                        onClick={() => {
                          const target = {
                            bookingId: activeCellInfo.booking!.id,
                            bookingNumber: activeCellInfo.booking!.bookingNumber,
                            bookingRoomId: activeCellInfo.bookingRoom!.id,
                            currentRoomInstanceId: activeCellInfo.room.id,
                            currentRoomNumber: activeCellInfo.room.roomNumber,
                            currentRoomTypeName:
                              activeCellInfo.bookingDetail?.roomTypeName ||
                              activeCellInfo.room.roomTypeName,
                            guestName: activeCellInfo.booking!.guestName,
                            checkInDate: activeCellInfo.bookingDetail?.checkInDate,
                            checkOutDate: activeCellInfo.bookingDetail?.checkOutDate,
                            hotelId: activeCellInfo.booking!.hotelId || selectedHotelId,
                          };
                          setActiveCellInfo(null);
                          onOpenSwapModal(target);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-blue-200 bg-blue-50 hover:bg-blue-100 text-blue-950 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <ArrowLeftRight className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Đổi Phòng (Room Move)
                          </div>
                          <div className="text-[10px] text-blue-700 font-normal truncate">
                            Chuyển sang phòng trống khác
                          </div>
                        </div>
                      </button>

                      {/* Khai báo lưu trú BCA */}
                      <button
                        type="button"
                        onClick={() => {
                          const target = {
                            bookingRoomId: activeCellInfo.bookingRoom!.id,
                            hotelId:
                              activeCellInfo.booking!.hotelId || selectedHotelId || 1,
                            roomNumber: activeCellInfo.room.roomNumber,
                            guestName: activeCellInfo.booking!.guestName,
                          };
                          setActiveCellInfo(null);
                          onOpenStayGuests?.(target);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-950 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-purple-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <UserCheck className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Khai Báo Lưu Trú
                          </div>
                          <div className="text-[10px] text-purple-700 font-normal truncate">
                            CCCD/Hộ chiếu khách ở
                          </div>
                        </div>
                      </button>

                      {/* Xem chi tiết đơn */}
                      <button
                        type="button"
                        onClick={() => {
                          const booking = activeCellInfo.booking!;
                          setActiveCellInfo(null);
                          onOpenDetail?.(booking);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-neutral-200 bg-neutral-50 hover:bg-neutral-100 text-neutral-800 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-neutral-700 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <FileText className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Hồ Sơ Đơn Đặt
                          </div>
                          <div className="text-[10px] text-neutral-500 font-normal truncate">
                            Xem hóa đơn & phụ phí
                          </div>
                        </div>
                      </button>

                      {/* Yêu cầu dọn phòng */}
                      <button
                        type="button"
                        onClick={async () => {
                          const roomId = activeCellInfo.room.id;
                          setActiveCellInfo(null);
                          await onUpdateRoomStatus?.(roomId, 'CLEANING');
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-amber-200 bg-amber-50 hover:bg-amber-100 text-amber-950 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-amber-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <Sparkles className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Yêu Cầu Dọn Phòng
                          </div>
                          <div className="text-[10px] text-amber-700 font-normal truncate">
                            Báo buồng phòng dọn dẹp
                          </div>
                        </div>
                      </button>
                    </>
                  )}

                  {/* CASE 2: RESERVED (ĐÃ ĐẶT TRƯỚC) */}
                  {activeCellInfo.statusType === 'RESERVED' && (
                    <>
                      {/* Nhận phòng ngay (Check-in) - Prominent Banner */}
                      <button
                        type="button"
                        onClick={() => {
                          const bookingRoom = activeCellInfo.bookingRoom!;
                          setActiveCellInfo(null);
                          onCheckIn?.(bookingRoom);
                        }}
                        className="flex items-center gap-3.5 p-3.5 rounded-xl border border-emerald-300 bg-emerald-600 hover:bg-emerald-700 text-white transition-all font-bold text-left cursor-pointer group shadow-sm col-span-2"
                      >
                        <div className="w-9 h-9 rounded-lg bg-white/20 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                          <Key className="w-5 h-5" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-sm font-black leading-tight">
                            Nhận Phòng Ngay (Check-in)
                          </div>
                          <div className="text-[11px] text-emerald-100 font-medium">
                            Kích hoạt lưu trú & bàn giao phòng cho khách
                          </div>
                        </div>
                      </button>

                      {/* Đổi phòng vật lý */}
                      <button
                        type="button"
                        onClick={() => {
                          const target = {
                            bookingId: activeCellInfo.booking!.id,
                            bookingNumber: activeCellInfo.booking!.bookingNumber,
                            bookingRoomId: activeCellInfo.bookingRoom!.id,
                            currentRoomInstanceId: activeCellInfo.room.id,
                            currentRoomNumber: activeCellInfo.room.roomNumber,
                            currentRoomTypeName:
                              activeCellInfo.bookingDetail?.roomTypeName ||
                              activeCellInfo.room.roomTypeName,
                            guestName: activeCellInfo.booking!.guestName,
                            checkInDate: activeCellInfo.bookingDetail?.checkInDate,
                            checkOutDate: activeCellInfo.bookingDetail?.checkOutDate,
                            hotelId: activeCellInfo.booking!.hotelId || selectedHotelId,
                          };
                          setActiveCellInfo(null);
                          onOpenSwapModal(target);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-blue-200 bg-blue-50 hover:bg-blue-100 text-blue-950 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <ArrowLeftRight className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Đổi Phòng Vật Lý
                          </div>
                          <div className="text-[10px] text-blue-700 font-normal truncate">
                            Gán phòng khác cùng hạng
                          </div>
                        </div>
                      </button>

                      {/* Xem chi tiết đơn */}
                      <button
                        type="button"
                        onClick={() => {
                          const booking = activeCellInfo.booking!;
                          setActiveCellInfo(null);
                          onOpenDetail?.(booking);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-neutral-200 bg-neutral-50 hover:bg-neutral-100 text-neutral-800 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-neutral-700 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <FileText className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Xem Đơn Đặt Phòng
                          </div>
                          <div className="text-[10px] text-neutral-500 font-normal truncate">
                            Xem thông tin & tiền cọc
                          </div>
                        </div>
                      </button>

                      {/* Hủy phòng (Chính sách hủy linh hoạt) */}
                      <button
                        type="button"
                        onClick={() => {
                          const target = {
                            bookingId: activeCellInfo.booking!.id,
                            bookingNumber: activeCellInfo.booking!.bookingNumber,
                            bookingRoomId: activeCellInfo.bookingRoom!.id,
                            roomNumber: activeCellInfo.room.roomNumber,
                            roomTypeName: activeCellInfo.room.roomTypeName,
                          };
                          setActiveCellInfo(null);
                          onOpenCancel?.(target);
                        }}
                        className="flex items-center gap-2.5 p-3 rounded-xl border border-rose-200 bg-rose-50 hover:bg-rose-100 text-rose-950 transition-all font-bold text-left cursor-pointer group shadow-2xs col-span-1"
                      >
                        <div className="w-8 h-8 rounded-lg bg-rose-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <Ban className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Hủy Phòng
                          </div>
                          <div className="text-[10px] text-rose-700 font-normal truncate">
                            Theo chính sách hủy
                          </div>
                        </div>
                      </button>

                      {/* Báo No-Show (Khách không đến) */}
                      <button
                        type="button"
                        onClick={() => {
                          const room = activeCellInfo.bookingRoom!;
                          setActiveCellInfo(null);
                          onOpenRoomNoShow?.(room);
                        }}
                        className="flex items-center gap-2.5 p-3 rounded-xl border border-amber-200 bg-amber-50 hover:bg-amber-100 text-amber-950 transition-all font-bold text-left cursor-pointer group shadow-2xs col-span-1"
                      >
                        <div className="w-8 h-8 rounded-lg bg-amber-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <AlertTriangle className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Khách Vắng Mặt
                          </div>
                          <div className="text-[10px] text-amber-700 font-normal truncate">
                            Phạt 100% đêm đầu (No-Show)
                          </div>
                        </div>
                      </button>
                    </>
                  )}

                  {/* CASE 3: READY (SẴN SÀNG / PHÒNG TRỐNG) */}
                  {activeCellInfo.statusType === 'READY' && (
                    <>
                      {/* Tạo đơn Walk-in nhận phòng ngay */}
                      <button
                        type="button"
                        onClick={() => {
                          const prefill = {
                            hotelId:
                              selectedHotelId || activeCellInfo.room.hotelId || 1,
                            roomInstanceId: activeCellInfo.room.id,
                            roomNumber: activeCellInfo.room.roomNumber,
                            hotelRoomTypeId: activeCellInfo.room.hotelRoomTypeId,
                            checkInDate: activeCellInfo.dateStr,
                          };
                          setActiveCellInfo(null);
                          onOpenWalkIn?.(prefill);
                        }}
                        className="flex items-center gap-3.5 p-3.5 rounded-xl border border-red-300 bg-red-600 hover:bg-red-700 text-white transition-all font-bold text-left cursor-pointer group shadow-sm col-span-2"
                      >
                        <div className="w-9 h-9 rounded-lg bg-white/20 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                          <PlusCircle className="w-5 h-5" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-sm font-black leading-tight">
                            Đặt Phòng Nhanh Tại Quầy (Walk-in)
                          </div>
                          <div className="text-[11px] text-red-100 font-medium">
                            Tạo đơn cho khách vãng lai & nhận phòng ngay
                          </div>
                        </div>
                      </button>

                      {/* Báo cần dọn dẹp */}
                      <button
                        type="button"
                        onClick={async () => {
                          const roomId = activeCellInfo.room.id;
                          setActiveCellInfo(null);
                          await onUpdateRoomStatus?.(roomId, 'CLEANING');
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-950 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-purple-600 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <Sparkles className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Báo Cần Dọn Dẹp
                          </div>
                          <div className="text-[10px] text-purple-700 font-normal truncate">
                            Chuyển sang Đang dọn
                          </div>
                        </div>
                      </button>

                      {/* Khóa bảo trì */}
                      <button
                        type="button"
                        onClick={async () => {
                          const roomId = activeCellInfo.room.id;
                          setActiveCellInfo(null);
                          await onUpdateRoomStatus?.(roomId, 'MAINTENANCE');
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-neutral-300 bg-neutral-100 hover:bg-neutral-200 text-neutral-900 transition-all font-bold text-left cursor-pointer group shadow-2xs"
                      >
                        <div className="w-8 h-8 rounded-lg bg-neutral-700 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <Wrench className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Khóa Phòng Bảo Trì
                          </div>
                          <div className="text-[10px] text-neutral-600 font-normal truncate">
                            Tạm dừng đón khách sửa chữa
                          </div>
                        </div>
                      </button>
                    </>
                  )}

                  {/* CASE 4: CLEANING / DIRTY / MAINTENANCE / BLOCKED */}
                  {(activeCellInfo.statusType === 'CLEANING' ||
                    activeCellInfo.statusType === 'MAINTENANCE' ||
                    activeCellInfo.statusType === 'BLOCKED') && (
                    <>
                      {/* Nếu là DIRTY (Chờ dọn) -> Có nút Chuyển sang Đang dọn dẹp (CLEANING) */}
                      {activeCellInfo.room.currentStatus === 'DIRTY' && (
                        <button
                          type="button"
                          onClick={async () => {
                            const roomId = activeCellInfo.room.id;
                            setActiveCellInfo(null);
                            await onUpdateRoomStatus?.(roomId, 'CLEANING');
                          }}
                          className="flex items-center gap-3 p-3.5 rounded-xl border border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-950 transition-all font-bold text-left cursor-pointer group shadow-2xs col-span-1"
                        >
                          <div className="w-9 h-9 rounded-lg bg-purple-600 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                            <Sparkles className="w-5 h-5" />
                          </div>
                          <div className="min-w-0">
                            <div className="text-sm font-black leading-tight">
                              Bắt Đầu Dọn Dẹp
                            </div>
                            <div className="text-[11px] text-purple-700 font-medium truncate">
                              Chuyển sang Đang dọn (CLEANING)
                            </div>
                          </div>
                        </button>
                      )}

                      {/* Xác nhận phòng sạch / READY */}
                      <button
                        type="button"
                        onClick={async () => {
                          const roomId = activeCellInfo.room.id;
                          setActiveCellInfo(null);
                          await onUpdateRoomStatus?.(roomId, 'READY');
                        }}
                        className={`flex items-center gap-3.5 p-3.5 rounded-xl border border-emerald-300 bg-emerald-600 hover:bg-emerald-700 text-white transition-all font-bold text-left cursor-pointer group shadow-sm ${activeCellInfo.room.currentStatus === 'DIRTY' ? 'col-span-1' : 'col-span-2'}`}
                      >
                        <div className="w-9 h-9 rounded-lg bg-white/20 text-white flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                          <CheckCircle2 className="w-5 h-5" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-sm font-black leading-tight">
                            Xác Nhận Phòng Sẵn Sàng (READY)
                          </div>
                          <div className="text-[11px] text-emerald-100 font-medium">
                            Hoàn tất dọn dẹp/bảo trì và mở lại phòng đón khách
                          </div>
                        </div>
                      </button>

                      {/* Đặt trước cho ngày tới */}
                      <button
                        type="button"
                        onClick={() => {
                          const prefill = {
                            hotelId:
                              selectedHotelId || activeCellInfo.room.hotelId || 1,
                            roomInstanceId: activeCellInfo.room.id,
                            roomNumber: activeCellInfo.room.roomNumber,
                            hotelRoomTypeId: activeCellInfo.room.hotelRoomTypeId,
                            checkInDate: activeCellInfo.dateStr,
                          };
                          setActiveCellInfo(null);
                          onOpenWalkIn?.(prefill);
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl border border-neutral-200 bg-neutral-50 hover:bg-neutral-100 text-neutral-800 transition-all font-bold text-left cursor-pointer group shadow-2xs col-span-2"
                      >
                        <div className="w-8 h-8 rounded-lg bg-neutral-700 text-white flex items-center justify-center shrink-0 shadow-2xs group-hover:scale-105 transition-transform">
                          <PlusCircle className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold leading-tight">
                            Đặt Phòng Cho Ngày Tới (Walk-in)
                          </div>
                          <div className="text-[10px] text-neutral-500 font-normal">
                            Tạo đơn trước cho khách đến sau khi hoàn tất dọn/sửa
                          </div>
                        </div>
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TimelineRackView;