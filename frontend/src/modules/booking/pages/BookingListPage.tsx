import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Search,
  RefreshCw,
  Eye,
  CheckCircle,
  LogOut,
  Ban,
  DollarSign,
  BedDouble,
  UserCheck,
  Receipt,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import bookingService from '../services/booking.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import type {
  BookingResponse,
  BookingStatus,
  BookingChargeType,
} from '../types/booking.types';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';

export const BookingListPage: React.FC = () => {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(false);

  // Filters
  const [searchBookingNumber, setSearchBookingNumber] = useState('');
  const [searchRoomNumber, setSearchRoomNumber] = useState('');
  const [searchStatus, setSearchStatus] = useState<BookingStatus | ''>('');

  // Modals & Selected Booking
  const [selectedBooking, setSelectedBooking] = useState<BookingResponse | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  // Assign Room Modal
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [targetBookingRoomId, setTargetBookingRoomId] = useState<number | null>(null);
  const [availableRooms, setAvailableRooms] = useState<RoomInstanceDto[]>([]);
  const [selectedRoomInstanceId, setSelectedRoomInstanceId] = useState<number | ''>('');
  const [isAssigning, setIsAssigning] = useState(false);

  // Add Charge Modal
  const [isChargeModalOpen, setIsChargeModalOpen] = useState(false);
  const [chargeType, setChargeType] = useState<BookingChargeType>('EARLY_CHECKIN');
  const [chargeItemName, setChargeItemName] = useState('');
  const [chargeQuantity, setChargeQuantity] = useState(1);
  const [chargeUnitPrice, setChargeUnitPrice] = useState<number>(100000);
  const [chargeServiceFeeRate, setChargeServiceFeeRate] = useState<number>(5);
  const [chargeVatRate, setChargeVatRate] = useState<number>(8);
  const [isSubmittingCharge, setIsSubmittingCharge] = useState(false);

  // Cancel Modal
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancellingBookingId, setCancellingBookingId] = useState<number | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  // Action status message
  const [actionSuccessMessage, setActionSuccessMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const bNum = searchBookingNumber ? searchBookingNumber.trim() : undefined;
      const rNum = searchRoomNumber
        ? searchRoomNumber.trim().replace(/^(phòng|phong|p\.?)\s*/i, '')
        : undefined;

      const res = await bookingService.filter({
        bookingNumber: bNum,
        roomNumber: rNum,
        status: searchStatus || undefined,
        page: page + 1,
        size: pageSize,
      });

      if (res.result) {
        setBookings(res.result.content || []);
        setTotalElements(res.result.totalElements || 0);
        setTotalPages(res.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi tải danh sách đơn đặt phòng:', err);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, searchBookingNumber, searchRoomNumber, searchStatus]);

  useEffect(() => {
    let isMounted = true;
    const init = async () => {
      if (isMounted) {
        await loadData();
      }
    };
    void init();
    return () => {
      isMounted = false;
    };
  }, [loadData]);

  // Load available physical rooms for the booking's hotel and room type
  useEffect(() => {
    if (isAssignModalOpen && selectedBooking) {
      const targetDetail = selectedBooking.bookingDetails?.find((d) =>
        d.bookingRooms?.some((r) => r.id === targetBookingRoomId)
      );

      roomInstanceService
        .filter({
          hotelId: selectedBooking.hotelId,
          hotelRoomTypeId: targetDetail?.hotelRoomTypeId,
          currentStatus: 'READY',
          pageSize: 100,
        })
        .then((res) => {
          if (res?.result) {
            setAvailableRooms(res.result.content || []);
          }
        })
        .catch((err) => console.error('Lỗi tải phòng vật lý:', err));
    }
  }, [isAssignModalOpen, selectedBooking, targetBookingRoomId]);

  // Danh sách các ID phòng và số phòng đã được đặt/gán trong hệ thống (cái nào đã đặt rồi sẽ mất đi trong chỗ chọn phòng)
  const assignedRoomIds = useMemo(() => {
    return new Set(
      bookings
        .filter((b) => b.status !== 'CANCELLED')
        .flatMap((b) => b.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [])
        .filter((r) => r.status !== 'CHECKED_OUT' && r.status !== 'CANCELLED' && r.roomInstanceId)
        .map((r) => r.roomInstanceId)
    );
  }, [bookings]);

  const assignedRoomNumbers = useMemo(() => {
    return new Set(
      bookings
        .filter((b) => b.status !== 'CANCELLED' && b.hotelId === selectedBooking?.hotelId)
        .flatMap((b) => b.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [])
        .filter((r) => r.status !== 'CHECKED_OUT' && r.status !== 'CANCELLED' && r.roomNumber)
        .map((r) => r.roomNumber)
    );
  }, [bookings, selectedBooking]);

  // Lọc chỉ giữ lại các phòng thực sự còn trống (chưa bị ai đặt, không trùng tên phòng đã đặt)
  const selectableRooms = useMemo(() => {
    return availableRooms.filter(
      (r) =>
        r.currentStatus === 'READY' &&
        !assignedRoomIds.has(r.id) &&
        !assignedRoomNumbers.has(r.roomNumber)
    );
  }, [availableRooms, assignedRoomIds, assignedRoomNumbers]);

  const handleOpenAssignModal = (bookingRoomId: number) => {
    setTargetBookingRoomId(bookingRoomId);
    setSelectedRoomInstanceId('');
    setIsAssignModalOpen(true);
  };

  const handleConfirmAssign = async () => {
    if (!targetBookingRoomId || !selectedRoomInstanceId) return;
    setIsAssigning(true);
    try {
      await bookingService.assignRoom(targetBookingRoomId, Number(selectedRoomInstanceId));
      setIsAssignModalOpen(false);
      setActionSuccessMessage('Xếp phòng vật lý thành công!');
      loadData();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Không thể xếp phòng. Vui lòng kiểm tra lại phòng có đang bị trùng lịch hay không.';
      console.error('Lỗi xếp phòng:', err);
      alert(msg);
    } finally {
      setIsAssigning(false);
    }
  };

  const handleOpenChargeModal = (bookingRoomId: number) => {
    setTargetBookingRoomId(bookingRoomId);
    setChargeItemName('Phụ phí phát sinh');
    setChargeQuantity(1);
    setChargeUnitPrice(100000);
    setIsChargeModalOpen(true);
  };

  const handleConfirmAddCharge = async () => {
    if (!targetBookingRoomId) return;
    setIsSubmittingCharge(true);
    try {
      await bookingService.addCharge(targetBookingRoomId, {
        chargeType,
        itemName: chargeItemName,
        quantity: chargeQuantity,
        unitPrice: chargeUnitPrice,
        serviceFeeRate: chargeServiceFeeRate,
        vatRate: chargeVatRate,
      });
      setIsChargeModalOpen(false);
      setActionSuccessMessage('Thêm phụ phí thành công!');
      loadData();
    } catch (err) {
      console.error('Lỗi thêm phụ phí:', err);
      alert('Không thể thêm phụ phí. Vui lòng kiểm tra lại.');
    } finally {
      setIsSubmittingCharge(false);
    }
  };

  const handleCheckIn = async (bookingRoomId: number) => {
    const targetRoom = selectedBooking?.bookingDetails
      ?.flatMap((d) => d.bookingRooms || [])
      ?.find((r) => r.id === bookingRoomId);

    if (!targetRoom?.roomNumber && !targetRoom?.roomInstanceId) {
      alert('Lượt lưu trú này chưa được xếp phòng vật lý. Vui lòng nhấn "Xếp phòng" trước khi Check-in!');
      return;
    }

    try {
      await bookingService.checkIn(bookingRoomId);
      setActionSuccessMessage('Check-in thành công!');
      loadData();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Check-in thất bại.';
      console.error('Lỗi check-in:', err);
      alert(msg);
    }
  };

  const handleCheckOut = async (bookingRoomId: number) => {
    try {
      await bookingService.checkOut(bookingRoomId);
      setActionSuccessMessage('Check-out thành công!');
      loadData();
    } catch (err) {
      console.error('Lỗi check-out:', err);
      alert('Check-out thất bại.');
    }
  };

  const handleConfirmCancel = async () => {
    if (!cancellingBookingId) return;
    setIsCancelling(true);
    try {
      await bookingService.updateStatus(cancellingBookingId, 'CANCELLED');
      setIsCancelModalOpen(false);
      setActionSuccessMessage('Đã hủy đơn đặt phòng thành công!');
      loadData();
    } catch (err) {
      console.error('Lỗi hủy phòng:', err);
      alert('Hủy phòng thất bại.');
    } finally {
      setIsCancelling(false);
    }
  };

  const getStatusBadge = (status: BookingStatus) => {
    switch (status) {
      case 'CONFIRMED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800">Đã xác nhận</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800">Đã hủy</span>;
      case 'NO_SHOW':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-100 text-amber-800">Vắng mặt</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-neutral-100 text-neutral-800">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">Quản lý Đặt phòng (Bookings)</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý vòng đời lưu trú, xếp phòng vật lý, phụ phí dịch vụ và thủ tục Check-in/Check-out
          </p>
        </div>
      </div>

      {actionSuccessMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-xl flex items-center justify-between">
          <span>{actionSuccessMessage}</span>
          <button onClick={() => setActionSuccessMessage(null)} className="text-sm underline font-medium">
            Đóng
          </button>
        </div>
      )}

      {/* SEARCH BAR — TÌM KIẾM THEO MÃ BOOKING (GIỐNG QUẢN LÝ TÀI CHÍNH) */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <label className="block text-xs font-semibold text-neutral-700 mb-1.5 flex items-center gap-1.5">
          <Receipt className="w-3.5 h-3.5 text-red-600" />
          <span>Tra cứu theo mã đơn đặt phòng (Booking Code)</span>
        </label>
        <div className="flex gap-3 items-center">
          <div className="flex-1">
            <Input
              placeholder="Nhập mã đơn đặt phòng (VD: BK-TEST-1001, BK...)..."
              value={searchBookingNumber}
              onChange={(e) => setSearchBookingNumber(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setPage(0);
                  loadData();
                }
              }}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>
          <Button
            variant="primary"
            onClick={() => {
              setPage(0);
              loadData();
            }}
            disabled={isLoading}
          >
            <Search className="w-4 h-4 mr-2" />
            Tra cứu đơn
          </Button>
          {searchBookingNumber && (
            <Button
              variant="outline"
              onClick={() => {
                setSearchBookingNumber('');
                setPage(0);
              }}
            >
              Đặt lại
            </Button>
          )}
        </div>
      </div>

      {/* ACTION TOOLBAR & LỌC THEO SỐ PHÒNG, TRẠNG THÁI */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3 flex-1 min-w-[280px]">
          <div>
            <span className="font-bold text-neutral-900 block sm:inline">Danh sách đơn đặt phòng</span>
            <span className="text-xs text-neutral-500 sm:ml-2 block sm:inline">
              (Tổng số: <strong className="text-neutral-800">{totalElements}</strong> đơn)
            </span>
          </div>
          <div className="relative flex-1 max-w-xs ml-auto sm:ml-2">
            <Input
              placeholder="🔍 Tìm theo số phòng (VD: 101, 201...)"
              value={searchRoomNumber}
              onChange={(e) => setSearchRoomNumber(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setPage(0);
                  loadData();
                }
              }}
              className="h-10 text-xs"
            />
          </div>
          <div className="w-44">
            <select
              className="w-full h-10 px-3 text-xs bg-white border border-neutral-300 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-red-600 focus:border-red-600"
              value={searchStatus}
              onChange={(e) => {
                setSearchStatus(e.target.value as BookingStatus | '');
                setPage(0);
              }}
            >
              <option value="">Tất cả trạng thái</option>
              <option value="CONFIRMED">Đã xác nhận</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="NO_SHOW">Vắng mặt (No-show)</option>
            </select>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={loadData} disabled={isLoading}>
            <RefreshCw className={`w-4 h-4 mr-1.5 ${isLoading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-600 font-medium">
              <tr>
                <th className="p-4">Phòng vật lý / Mã đơn</th>
                <th className="p-4">Khách sạn</th>
                <th className="p-4">Khách hàng</th>
                <th className="p-4">Loại phòng</th>
                <th className="p-4">Thời gian lưu trú</th>
                <th className="p-4 text-right">Tổng tiền (VNĐ)</th>
                <th className="p-4 text-center">Trạng thái</th>
                <th className="p-4 text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang nạp danh sách đặt phòng...
                  </td>
                </tr>
              ) : bookings.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-400">
                    Không tìm thấy đơn đặt phòng nào phù hợp.
                  </td>
                </tr>
              ) : (
                bookings.map((booking) => (
                  <tr key={booking.id} className="hover:bg-neutral-50 transition-colors">
                    <td className="p-4">
                      {(() => {
                        const assignedRooms = booking.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [];
                        const roomsWithInfo = assignedRooms.filter((r) => r.roomNumber || r.roomInstanceId);

                        if (roomsWithInfo.length === 0) {
                          return (
                            <div className="space-y-0.5">
                              <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-neutral-100 text-neutral-600">
                                Chưa xếp phòng
                              </span>
                              <div className="text-[11px] font-mono font-semibold text-neutral-500">
                                {booking.bookingNumber}
                              </div>
                              <div className="text-[10px] text-neutral-400">
                                {booking.bookingType}
                              </div>
                            </div>
                          );
                        }

                        return (
                          <div className="space-y-0.5">
                            <div className="flex flex-wrap gap-1">
                              {roomsWithInfo.map((r, idx) => (
                                <span
                                  key={r.id || idx}
                                  className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold bg-red-50 text-red-700 border border-red-200"
                                >
                                  {r.roomNumber ? `Phòng ${r.roomNumber}` : 'Đã xếp phòng'}
                                </span>
                              ))}
                            </div>
                            <div className="text-[11px] font-mono font-semibold text-neutral-500">
                              {booking.bookingNumber}
                            </div>
                            <div className="text-[10px] text-neutral-400">
                              {booking.bookingType}
                            </div>
                          </div>
                        );
                      })()}
                    </td>
                    <td className="p-4 text-neutral-800">{booking.hotelName}</td>
                    <td className="p-4">
                      <div className="font-medium text-neutral-900">{booking.guestPhone}</div>
                      {booking.companyName && (
                        <div className="text-xs text-neutral-500">{booking.companyName}</div>
                      )}
                    </td>
                    <td className="p-4 text-neutral-700">
                      {booking.bookingDetails && booking.bookingDetails.length > 0 ? (
                        <div className="space-y-0.5">
                          {booking.bookingDetails.map((d, idx) => (
                            <div key={d.id || idx} className="font-medium text-neutral-900">
                              {d.roomTypeName || 'Chưa xác định'}
                              {d.quantity > 1 && (
                                <span className="text-xs text-neutral-500 ml-1">x{d.quantity}</span>
                              )}
                            </div>
                          ))}
                        </div>
                      ) : (
                        <span className="text-neutral-400 italic">N/A</span>
                      )}
                    </td>
                    <td className="p-4 text-neutral-600 text-xs">
                      {booking.bookingDetails?.[0] ? (
                        <>
                          <div>Từ: {booking.bookingDetails[0].checkInDate}</div>
                          <div>Đến: {booking.bookingDetails[0].checkOutDate}</div>
                        </>
                      ) : (
                        'N/A'
                      )}
                    </td>
                    <td className="p-4 text-right font-bold text-neutral-900">
                      {new Intl.NumberFormat('vi-VN').format(booking.totalAmount)} đ
                    </td>
                    <td className="p-4 text-center">{getStatusBadge(booking.status)}</td>
                    <td className="p-4">
                      <div className="flex items-center justify-center gap-1.5">
                        <button
                          onClick={() => {
                            setSelectedBooking(booking);
                            setIsDetailModalOpen(true);
                          }}
                          className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg"
                          title="Xem chi tiết đơn"
                        >
                          <Eye className="w-4 h-4" />
                        </button>

                        {booking.status === 'CONFIRMED' && (
                          <button
                            onClick={() => {
                              setCancellingBookingId(booking.id);
                              setIsCancelModalOpen(true);
                            }}
                            className="p-1.5 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg"
                            title="Hủy đơn đặt phòng"
                          >
                            <Ban className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang */}
        <div className="p-4 border-t border-neutral-200">
          <Pagination
            pageNumber={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </div>
      </div>

      {/* MODAL CHI TIẾT ĐƠN ĐẶT PHÒNG */}
      {selectedBooking && (
        <Modal
          isOpen={isDetailModalOpen}
          onClose={() => setIsDetailModalOpen(false)}
          title={`Chi tiết đơn đặt phòng: ${selectedBooking.bookingNumber}`}
        >
          <div className="space-y-6 max-h-[75vh] overflow-y-auto pr-1">
            {/* Header info */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 bg-neutral-50 rounded-xl">
              <div>
                <span className="text-xs text-neutral-500">Khách sạn</span>
                <p className="font-semibold text-neutral-900 text-sm">{selectedBooking.hotelName}</p>
              </div>
              <div>
                <span className="text-xs text-neutral-500">Khách hàng</span>
                <p className="font-semibold text-neutral-900 text-sm">{selectedBooking.guestPhone}</p>
              </div>
              <div>
                <span className="text-xs text-neutral-500">Trạng thái</span>
                <div className="mt-1">{getStatusBadge(selectedBooking.status)}</div>
              </div>
              <div>
                <span className="text-xs text-neutral-500">Tổng thanh toán</span>
                <p className="font-bold text-red-600 text-base">
                  {new Intl.NumberFormat('vi-VN').format(selectedBooking.totalAmount)} đ
                </p>
              </div>
            </div>

            {/* Chi tiết từng phòng */}
            <div>
              <h3 className="text-sm font-bold text-neutral-800 uppercase tracking-wider mb-3">
                Danh sách phòng đặt ({selectedBooking.bookingDetails?.length || 0})
              </h3>

              {selectedBooking.bookingDetails?.map((detail) => (
                <div key={detail.id} className="border border-neutral-200 rounded-xl p-4 mb-4 space-y-4">
                  <div className="flex justify-between items-center border-b border-neutral-100 pb-2">
                    <span className="font-bold text-neutral-900 flex items-center gap-2">
                      <BedDouble className="w-4 h-4 text-red-600" />
                      {detail.roomTypeName} (SL: {detail.quantity})
                    </span>
                    <span className="text-xs text-neutral-500">
                      {detail.checkInDate} ➔ {detail.checkOutDate}
                    </span>
                  </div>

                  {detail.bookingRooms?.map((room, idx) => (
                    <div key={room.id} className="bg-neutral-50 rounded-lg p-3 space-y-3">
                      <div className="flex justify-between items-center">
                        <div className="text-sm font-semibold text-neutral-800">
                          Phòng #{idx + 1}:{' '}
                          {room.roomNumber ? (
                            <span className="text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg font-mono text-xs font-bold border border-emerald-200">
                              Phòng {room.roomNumber}
                            </span>
                          ) : (
                            <span className="text-amber-600 italic">Chưa xếp phòng vật lý</span>
                          )}
                        </div>

                        <div className="flex gap-2">
                          {!room.roomNumber && selectedBooking.status === 'CONFIRMED' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleOpenAssignModal(room.id)}
                            >
                              <UserCheck className="w-3.5 h-3.5 mr-1" />
                              Xếp phòng
                            </Button>
                          )}

                          {room.status === 'EXPECTED' && (
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() => handleCheckIn(room.id)}
                            >
                              <CheckCircle className="w-3.5 h-3.5 mr-1" />
                              Check-in
                            </Button>
                          )}

                          {room.status === 'CHECKED_IN' && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleCheckOut(room.id)}
                            >
                              <LogOut className="w-3.5 h-3.5 mr-1" />
                              Check-out
                            </Button>
                          )}

                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenChargeModal(room.id)}
                          >
                            <DollarSign className="w-3.5 h-3.5 mr-1" />
                            Phụ phí
                          </Button>
                        </div>
                      </div>

                      {/* Phụ phí của phòng nếu có */}
                      {room.charges && room.charges.length > 0 && (
                        <div className="border-t border-neutral-200 pt-2 text-xs">
                          <span className="font-semibold text-neutral-700">Phụ phí phát sinh:</span>
                          <ul className="list-disc pl-4 mt-1 text-neutral-600 space-y-0.5">
                            {room.charges.map((c) => (
                              <li key={c.id}>
                                {c.itemName} x{c.quantity}: {new Intl.NumberFormat('vi-VN').format(c.totalAmount)} đ ({c.chargeType})
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL XẾP PHÒNG VẬT LÝ */}
      <Modal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        title="Xếp phòng vật lý (Physical Room Assignment)"
      >
        <div className="space-y-4">
          <p className="text-sm text-neutral-600">
            Chọn phòng vật lý đang sẵn sàng để gán cho lượt lưu trú này:
          </p>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Phòng vật lý khả dụng (chưa có khách đặt)
            </label>
            <select
              className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={selectedRoomInstanceId}
              onChange={(e) => setSelectedRoomInstanceId(e.target.value ? Number(e.target.value) : '')}
            >
              <option value="">-- Chọn số phòng trống --</option>
              {selectableRooms.map((r) => (
                <option key={r.id} value={r.id}>
                  Phòng {r.roomNumber} ({r.roomTypeName || 'Tiêu chuẩn'}) - Trạng thái: Trống
                </option>
              ))}
            </select>
            {selectableRooms.length === 0 && (
              <p className="text-xs text-rose-600 mt-2 font-medium">
                ⚠️ Không có phòng trống khả dụng nào cho loại phòng này (các phòng đã có khách đặt hoặc đang ở).
              </p>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button variant="outline" onClick={() => setIsAssignModalOpen(false)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              onClick={handleConfirmAssign}
              disabled={!selectedRoomInstanceId || isAssigning || selectableRooms.length === 0}
            >
              {isAssigning ? 'Đang xếp...' : 'Xác nhận xếp phòng'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL THÊM PHỤ PHÍ */}
      <Modal
        isOpen={isChargeModalOpen}
        onClose={() => setIsChargeModalOpen(false)}
        title="Thêm phụ phí (Early Check-in / Late Check-out / Minibar)"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Loại phụ phí</label>
            <select
              className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={chargeType}
              onChange={(e) => setChargeType(e.target.value as BookingChargeType)}
            >
              <option value="EARLY_CHECKIN">Nhận phòng sớm (Early Check-in)</option>
              <option value="LATE_CHECKOUT">Trả phòng muộn (Late Check-out)</option>
              <option value="PENALTY">Phạt vi phạm / Hỏng hóc</option>
              <option value="OTHER">Dịch vụ khác</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Tên khoản phụ thu</label>
            <Input
              value={chargeItemName}
              onChange={(e) => setChargeItemName(e.target.value)}
              placeholder="VD: Phụ thu nhận phòng sớm 09:00"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Số lượng</label>
              <Input
                type="number"
                value={chargeQuantity}
                onChange={(e) => setChargeQuantity(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Đơn giá (VNĐ)</label>
              <Input
                type="number"
                value={chargeUnitPrice}
                onChange={(e) => setChargeUnitPrice(Number(e.target.value))}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Phí dịch vụ (%)</label>
              <Input
                type="number"
                value={chargeServiceFeeRate}
                onChange={(e) => setChargeServiceFeeRate(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Thuế VAT (%)</label>
              <Input
                type="number"
                value={chargeVatRate}
                onChange={(e) => setChargeVatRate(Number(e.target.value))}
              />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button variant="outline" onClick={() => setIsChargeModalOpen(false)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              onClick={handleConfirmAddCharge}
              disabled={isSubmittingCharge || !chargeItemName}
            >
              {isSubmittingCharge ? 'Đang lưu...' : 'Thêm phụ phí'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* CONFIRM CANCEL MODAL */}
      <ConfirmModal
        isOpen={isCancelModalOpen}
        onClose={() => setIsCancelModalOpen(false)}
        onConfirm={handleConfirmCancel}
        title="Xác nhận hủy đơn đặt phòng"
        description="Bạn có chắc chắn muốn hủy đơn đặt phòng này? Quỹ phòng và các phòng vật lý đã gán sẽ được giải phóng lập tức."
        confirmText={isCancelling ? 'Đang hủy...' : 'Xác nhận hủy'}
        cancelText="Đóng"
        variant="danger"
      />
    </div>
  );
};

export default BookingListPage;
