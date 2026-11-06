import React, { useState, useMemo } from 'react';
import Select from '../../../core/components/ui/Select';
import {
  Building,
  Calendar,
  ArrowLeftRight,
  X,
  BedDouble,
} from 'lucide-react';
import type {
  BookingResponse,
  BookingRoomResponse,
  BookingDetailResponse,
} from '../types/booking.types';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';

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
  onOpenSwapModal,
  hotelList = [],
  selectedHotelId,
  onSelectHotelId,
}) => {
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

  // Group rooms by Room Type (Hạng phòng)
  const roomGroups = useMemo(() => {
    const groups = new Map<string, RoomInstanceDto[]>();
    const currentHotelId = selectedHotelId || hotelList[0]?.id;
    const displayedRooms = currentHotelId
      ? rooms.filter((r) => !r.hotelId || r.hotelId === currentHotelId)
      : rooms;

    displayedRooms.forEach((room) => {
      const typeName = room.roomTypeName || 'KHÁC';
      if (!groups.has(typeName)) {
        groups.set(typeName, []);
      }
      groups.get(typeName)!.push(room);
    });

    // Sort room numbers within each group
    groups.forEach((roomList) => {
      roomList.sort((a, b) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true }));
    });

    return Array.from(groups.entries());
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

  return (
    <div className="space-y-6">

      {/* 2. Filter & Date Controls Card matching Screenshot */}
      <div className="bg-white border border-neutral-200 rounded-xl p-5 shadow-xs">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-end">
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
          <div className="md:col-span-2 flex items-center gap-2">
            <button
              type="button"
              onClick={() => handleShiftDays(-7)}
              className="flex-1 h-11 px-3 bg-white border border-neutral-300 hover:bg-neutral-50 active:bg-neutral-100 text-neutral-700 text-xs font-bold rounded-lg transition-colors whitespace-nowrap shadow-2xs cursor-pointer flex items-center justify-center gap-1"
            >
              <span>‹ Lùi 7 ngày</span>
            </button>
            <button
              type="button"
              onClick={() => handleShiftDays(7)}
              className="flex-1 h-11 px-3 bg-white border border-neutral-300 hover:bg-neutral-50 active:bg-neutral-100 text-neutral-700 text-xs font-bold rounded-lg transition-colors whitespace-nowrap shadow-2xs cursor-pointer flex items-center justify-center gap-1"
            >
              <span>Tiến 7 ngày ›</span>
            </button>
          </div>
        </div>
      </div>

      {/* 3. Timeline Matrix Grid matching Screenshot */}
      <div className="bg-white border border-neutral-300 rounded-xl shadow-xs overflow-hidden isolate">
        <div className="overflow-x-auto max-h-[640px]">
          <table className="w-full border-separate border-spacing-0 text-left bg-white">
            {/* Table Header: Solid Black matching Screenshot */}
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
                    className={`sticky top-0 z-30 w-[100px] min-w-[100px] max-w-[100px] p-2.5 text-center bg-neutral-900 text-white border-t border-b border-r border-neutral-700 whitespace-nowrap ${day.isToday ? 'bg-neutral-800' : ''
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

            {/* Table Body: Grouped by Room Type (rowSpan) */}
            <tbody className="text-xs bg-white">
              {roomGroups.length === 0 ? (
                <tr>
                  <td
                    colSpan={daysRange.length + 2}
                    className="p-12 text-center text-neutral-400 font-medium border-b border-neutral-300"
                  >
                    {isLoading
                      ? 'Đang tải ma trận phòng...'
                      : 'Không có dữ liệu phòng vật lý cho khách sạn này.'}
                  </td>
                </tr>
              ) : (
                roomGroups.map(([roomTypeName, groupRooms]) => (
                  <React.Fragment key={roomTypeName}>
                    {groupRooms.map((room, idx) => {
                      return (
                        <tr
                          key={room.id}
                          className="hover:bg-neutral-50/70 transition-colors"
                        >
                          {/* Rowspan: LOẠI PHÒNG (Only rendered on the first room of the group) */}
                          {idx === 0 && (
                            <td
                              rowSpan={groupRooms.length}
                              className="sticky left-0 z-20 w-[200px] min-w-[200px] max-w-[200px] p-3 text-center align-middle font-black text-xs uppercase bg-white text-neutral-900 leading-relaxed px-4 border-b border-r border-l border-neutral-300 break-words"
                            >
                              {roomTypeName}
                            </td>
                          )}

                          {/* PHÒNG Number */}
                          <td className="sticky left-[200px] z-20 w-[80px] min-w-[80px] max-w-[80px] p-3 text-center font-black text-xs bg-white text-neutral-900 border-b border-r border-neutral-300 whitespace-nowrap">
                            {room.roomNumber}
                          </td>

                          {/* Days Cells: Flat 1-cell layout without nested button */}
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
                              if (room.currentStatus === 'CLEANING') {
                                statusType = 'CLEANING';
                                cellBgClass = 'bg-[#E9D5FF] text-purple-950 hover:bg-purple-200 font-bold';
                                cellText = 'ĐANG DỌN';
                              } else if (room.currentStatus === 'MAINTENANCE') {
                                statusType = 'MAINTENANCE';
                                cellBgClass = 'bg-[#9CA3AF] text-white hover:bg-neutral-500 font-bold';
                                cellText = 'BẢO TRÌ';
                              } else if ((room.currentStatus as string) === 'BLOCKED' || (room.currentStatus as string) === 'OUT_OF_SERVICE') {
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
                                className={`w-[100px] min-w-[100px] max-w-[100px] h-11 px-2 border-b border-r border-neutral-300 text-center text-[11px] font-bold uppercase tracking-tight transition-colors cursor-pointer select-none truncate ${cellBgClass}`}
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
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 4. Legend Box matching Screenshot 2 */}
      <div className="bg-white border border-neutral-200 rounded-xl p-5 shadow-xs">
        <h3 className="text-xs font-bold text-neutral-800 uppercase tracking-wider mb-3.5">
          CHÚ GIẢI TRẠNG THÁI PHÒNG
        </h3>
        <div className="flex flex-wrap items-center gap-x-8 gap-y-3 text-xs text-neutral-700 font-medium">
          {/* Sẵn sàng */}
          <div className="flex items-center gap-2.5">
            <span className="w-5 h-5 rounded-sm bg-white border border-neutral-300 inline-block shadow-2xs"></span>
            <span>Sẵn sàng (READY)</span>
          </div>

          {/* Đang dọn */}
          <div className="flex items-center gap-2.5">
            <span className="w-5 h-5 rounded-sm bg-[#E9D5FF] border border-purple-300 inline-block"></span>
            <span>Đang dọn (CLEANING)</span>
          </div>

          {/* Đã đặt trước */}
          <div className="flex items-center gap-2.5">
            <span className="w-5 h-5 rounded-sm bg-[#FED7AA] border border-orange-300 inline-block"></span>
            <span>Đã đặt trước (RESERVED)</span>
          </div>

          {/* Đang ở */}
          <div className="flex items-center gap-2.5">
            <span className="w-5 h-5 rounded-sm bg-[#2563EB] inline-block"></span>
            <span>Đang ở (OCCUPIED)</span>
          </div>

          {/* Khóa ảo */}
          <div className="flex items-center gap-2.5">
            <span className="w-5 h-5 rounded-sm bg-[#FDE047] border border-yellow-400 inline-block"></span>
            <span>Khóa ảo (BLOCKED)</span>
          </div>

          {/* Bảo trì */}
          <div className="flex items-center gap-2.5">
            <span className="w-5 h-5 rounded-sm bg-[#9CA3AF] inline-block"></span>
            <span>Bảo trì (MAINTENANCE)</span>
          </div>
        </div>
      </div>

      {/* 5. Cell Detail Popover / Modal with Room Swap Trigger */}
      {activeCellInfo && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl border border-neutral-200 w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            {/* Popover Header */}
            <div className="flex items-center justify-between p-4 border-b border-neutral-100 bg-neutral-50">
              <div className="flex items-center gap-2">
                <BedDouble className="w-5 h-5 text-red-600" />
                <h4 className="font-bold text-neutral-900 text-sm">
                  Thông Tin Phòng {activeCellInfo.room.roomNumber} ({activeCellInfo.dateStr})
                </h4>
              </div>
              <button
                type="button"
                onClick={() => setActiveCellInfo(null)}
                className="text-neutral-400 hover:text-neutral-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Popover Body */}
            <div className="p-5 space-y-4 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-neutral-500">Hạng phòng:</span>
                <span className="font-bold text-neutral-900">
                  {activeCellInfo.room.roomTypeName}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-neutral-500">Trạng thái phòng:</span>
                <span
                  className={`px-2 py-0.5 rounded-full font-bold text-[10px] ${activeCellInfo.statusType === 'OCCUPIED'
                    ? 'bg-blue-100 text-blue-800'
                    : activeCellInfo.statusType === 'RESERVED'
                      ? 'bg-orange-100 text-orange-900'
                      : activeCellInfo.statusType === 'CLEANING'
                        ? 'bg-purple-100 text-purple-900'
                        : activeCellInfo.statusType === 'MAINTENANCE'
                          ? 'bg-neutral-200 text-neutral-700'
                          : activeCellInfo.statusType === 'BLOCKED'
                            ? 'bg-yellow-100 text-yellow-900'
                            : 'bg-emerald-100 text-emerald-800'
                    }`}
                >
                  {activeCellInfo.statusType}
                </span>
              </div>

              {activeCellInfo.booking && activeCellInfo.bookingRoom && (
                <>
                  <div className="pt-2 border-t border-neutral-100">
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-neutral-500">Mã đặt phòng:</span>
                      <span className="font-mono font-bold text-red-600">
                        {activeCellInfo.booking.bookingNumber}
                      </span>
                    </div>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-neutral-500">Khách hàng:</span>
                      <span className="font-bold text-neutral-900">
                        {activeCellInfo.booking.guestName || 'Khách vãng lai'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-neutral-500">Thời gian lưu trú:</span>
                      <span className="font-medium text-neutral-800">
                        {activeCellInfo.bookingDetail?.checkInDate || 'N/A'} ➔{' '}
                        {activeCellInfo.bookingDetail?.checkOutDate || 'N/A'}
                      </span>
                    </div>
                  </div>

                  {/* ROOM SWAP ACTION BUTTON */}
                  <div className="pt-3 border-t border-neutral-100">
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
                      className="w-full py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-xs"
                    >
                      <ArrowLeftRight className="w-4 h-4" />
                      <span>Thực Hiện Đổi Phòng (Room Swap)</span>
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TimelineRackView;
