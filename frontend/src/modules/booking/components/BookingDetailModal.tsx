import React, { useState } from 'react';
import {
  BedDouble,
  UserCheck,
  Users,
  CheckCircle,
  UserX,
  Ban,
  Receipt,
  LogOut,
  ArrowLeftRight,
  Utensils,
  DollarSign,
  Trash2,
  AlertTriangle,
  Building2,
  Phone,
  Calendar,
  DoorClosed,
  Copy,
  Check,
  User,
} from 'lucide-react';
import { Modal } from '../../../core/components/ui/Modal';
import { Button } from '../../../core/components/ui/Button';
import { RoomActionDropdown, type RoomActionItem } from './RoomActionDropdown';
import type { RoomAssignTarget } from './RoomAssignModal';
import type {
  BookingResponse,
  BookingRoomResponse,
  BookingDetailResponse,
  BookingStatus,
} from '../types/booking.types';

export interface BookingDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  booking: BookingResponse | null;
  onAssignRoom: (target: RoomAssignTarget) => void;
  onStayGuests: (booking: BookingResponse, room: BookingRoomResponse) => void;
  onCheckIn: (room: BookingRoomResponse) => void;
  onNoShowRoom: (room: BookingRoomResponse) => void;
  onCancelRoom: (booking: BookingResponse, room: BookingRoomResponse, roomTypeName?: string) => void;
  onCheckoutSettlement: (bookingRoomId: number) => void;
  onCheckOut: (room: BookingRoomResponse) => void;
  onSwapRoom: (params: {
    bookingId: number;
    bookingNumber: string;
    bookingRoomId: number;
    currentRoomInstanceId?: number;
    currentRoomNumber?: string;
    currentRoomTypeName: string;
    guestName: string;
    checkInDate: string;
    checkOutDate: string;
    hotelId: number;
  }) => void;
  onServiceOrder: (booking: BookingResponse, room: BookingRoomResponse) => void;
  onAddCharge: (
    booking: BookingResponse,
    room: BookingRoomResponse,
    detail: BookingDetailResponse
  ) => void;
  onDeleteCharge: (chargeId: number, itemName: string) => void;
  onNoShowBooking: (booking: BookingResponse) => void;
}

export const BookingDetailModal: React.FC<BookingDetailModalProps> = ({
  isOpen,
  onClose,
  booking,
  onAssignRoom,
  onStayGuests,
  onCheckIn,
  onNoShowRoom,
  onCancelRoom,
  onCheckoutSettlement,
  onCheckOut,
  onSwapRoom,
  onServiceOrder,
  onAddCharge,
  onDeleteCharge,
  onNoShowBooking,
}) => {
  const [copiedPhone, setCopiedPhone] = useState(false);

  if (!booking) return null;

  const handleCopyPhone = () => {
    if (booking.guestPhone) {
      navigator.clipboard.writeText(booking.guestPhone);
      setCopiedPhone(true);
      setTimeout(() => setCopiedPhone(false), 2000);
    }
  };

  const getStatusBadge = (status: BookingStatus) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
            Đã xác nhận
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full bg-red-50 text-red-700 border border-red-200">
            Đã hủy
          </span>
        );
      case 'NO_SHOW':
        return (
          <span className="inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-50 text-amber-700 border border-amber-200">
            Vắng mặt (No-Show)
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full bg-neutral-100 text-neutral-700">
            {status}
          </span>
        );
    }
  };

  // Tính tổng số lượng phòng
  const totalRoomsCount =
    booking.bookingDetails?.reduce((acc, detail) => acc + (detail.bookingRooms?.length || 0), 0) || 0;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Chi tiết đơn đặt phòng: ${booking.bookingNumber}`}
      maxWidth="4xl"
    >
      <div className="space-y-6 pb-24">
        {/* EXECUTIVE SUMMARY BANNER */}
        <div className="bg-gradient-to-br from-neutral-50 to-white rounded-2xl border border-neutral-200/90 p-5 shadow-xs">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 md:gap-6 divide-y md:divide-y-0 md:divide-x divide-neutral-200/80">
            {/* Cột 1: Khách sạn & Mã đơn */}
            <div className="space-y-1.5">
              <span className="text-xs font-medium text-neutral-500 uppercase tracking-wider flex items-center gap-1.5">
                <Building2 className="w-3.5 h-3.5 text-neutral-400" />
                Khách sạn lưu trú
              </span>
              <p className="font-bold text-neutral-900 text-sm leading-snug">{booking.hotelName}</p>
              <div className="pt-1 flex items-center gap-2">
                <span className="text-[11px] font-mono text-neutral-500 bg-neutral-100 px-2 py-0.5 rounded-md border border-neutral-200">
                  {booking.bookingType}
                </span>
                {booking.companyName && (
                  <span className="text-[11px] text-neutral-600 truncate max-w-[130px]" title={booking.companyName}>
                    🏢 {booking.companyName}
                  </span>
                )}
              </div>
            </div>

            {/* Cột 2: Thông tin khách hàng đặt phòng */}
            <div className="pt-3 md:pt-0 md:pl-5 space-y-1.5">
              <span className="text-xs font-medium text-neutral-500 uppercase tracking-wider flex items-center gap-1.5">
                <User className="w-3.5 h-3.5 text-neutral-400" />
                Người đặt phòng / Đại diện
              </span>
              <p className="font-semibold text-neutral-900 text-sm truncate">{booking.guestName || 'Khách vãng lai'}</p>
              <div className="flex items-center gap-1.5 text-xs text-neutral-700">
                <Phone className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                <span className="font-mono font-medium">{booking.guestPhone}</span>
                <button
                  type="button"
                  onClick={handleCopyPhone}
                  className="p-1 text-neutral-400 hover:text-neutral-700 hover:bg-neutral-100 rounded transition"
                  title="Sao chép số điện thoại"
                >
                  {copiedPhone ? (
                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>
              </div>
            </div>

            {/* Cột 3: Trạng thái & Thao tác tổng */}
            <div className="pt-3 md:pt-0 md:pl-5 space-y-2">
              <span className="text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Trạng thái đơn
              </span>
              <div>{getStatusBadge(booking.status)}</div>
              {booking.status === 'CONFIRMED' && (
                <button
                  type="button"
                  onClick={() => onNoShowBooking(booking)}
                  className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-300 rounded-lg transition-colors cursor-pointer"
                  title="Đánh dấu toàn bộ đơn đặt phòng là vắng mặt (No-Show)"
                >
                  <UserX className="w-3.5 h-3.5 text-amber-600" />
                  Xử lý No-Show toàn đơn
                </button>
              )}
            </div>

            {/* Cột 4: Tổng thanh toán */}
            <div className="pt-3 md:pt-0 md:pl-5 space-y-1">
              <span className="text-xs font-medium text-neutral-500 uppercase tracking-wider">
                Tổng thanh toán
              </span>
              <p className="font-bold text-red-600 text-xl tracking-tight">
                {new Intl.NumberFormat('vi-VN').format(booking.totalAmount)} đ
              </p>
              <div className="text-[11px] text-neutral-500">
                {booking.serviceFeeRate > 0 && `(Phí DV: ${booking.serviceFeeRate}%) `}
                {booking.totalVatAmount > 0 && `(VAT: ${new Intl.NumberFormat('vi-VN').format(booking.totalVatAmount)} đ)`}
              </div>
            </div>
          </div>
        </div>

        {/* DANH SÁCH PHÒNG LƯU TRÚ */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-neutral-800 uppercase tracking-wider flex items-center gap-2">
              <DoorClosed className="w-4 h-4 text-red-600" />
              Danh sách phòng lưu trú ({totalRoomsCount} phòng)
            </h3>
          </div>

          {booking.bookingDetails?.map((detail) => (
            <div
              key={detail.id}
              className="bg-white border border-neutral-200 rounded-2xl p-5 space-y-4 shadow-2xs hover:border-neutral-300 transition-colors"
            >
              {/* Header của loại phòng */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-neutral-100 gap-2">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0 border border-red-100">
                    <BedDouble className="w-4 h-4" />
                  </div>
                  <div>
                    <h4 className="font-bold text-neutral-900 text-sm md:text-base">
                      {detail.roomTypeName}
                    </h4>
                    <span className="text-xs text-neutral-500 font-medium">
                      Số lượng đặt: <strong className="text-neutral-700">{detail.quantity} phòng</strong>
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 text-xs font-medium text-neutral-600 bg-neutral-50 px-3 py-1.5 rounded-xl border border-neutral-200 shrink-0 self-start sm:self-auto">
                  <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                  <span>
                    {detail.checkInDate} <span className="text-neutral-400">➔</span> {detail.checkOutDate}
                  </span>
                </div>
              </div>

              {/* Danh sách từng phòng cụ thể */}
              <div className="space-y-3">
                {detail.bookingRooms?.map((room, idx) => {
                  const isRoomAssigned = Boolean(room.roomInstanceId && room.roomNumber);

                  // Xây dựng danh sách action cho Menu Thao tác khác (...)
                  const moreActions: RoomActionItem[] = [];

                  if (room.status === 'EXPECTED') {
                    // Nếu đã xếp phòng -> có thể đổi phòng
                    if (isRoomAssigned) {
                      moreActions.push({
                        key: 'swap',
                        label: 'Đổi phòng',
                        icon: <ArrowLeftRight className="w-4 h-4" />,
                        onClick: () =>
                          onSwapRoom({
                            bookingId: booking.id,
                            bookingNumber: booking.bookingNumber,
                            bookingRoomId: room.id,
                            currentRoomInstanceId: room.roomInstanceId,
                            currentRoomNumber: room.roomNumber,
                            currentRoomTypeName: detail.roomTypeName,
                            guestName: booking.guestName,
                            checkInDate: detail.checkInDate,
                            checkOutDate: detail.checkOutDate,
                            hotelId: booking.hotelId,
                          }),
                        title: 'Đổi sang phòng khác cùng hạng hoặc nâng hạng',
                      });

                      moreActions.push({
                        key: 'service-order',
                        label: 'Gọi món / POS',
                        icon: <Utensils className="w-4 h-4" />,
                        onClick: () => onServiceOrder(booking, room),
                        title: 'Đặt dịch vụ phòng, minibar',
                      });
                    }

                    moreActions.push({
                      key: 'add-charge',
                      label: 'Thêm phụ phí',
                      icon: <DollarSign className="w-4 h-4" />,
                      onClick: () => onAddCharge(booking, room, detail),
                      title: 'Thêm phụ phí nhận phòng sớm, thêm người...',
                    });

                    // Divider trước nhóm nguy hiểm
                    moreActions.push({
                      key: 'divider-danger',
                      label: '',
                      isDivider: true,
                      onClick: () => {},
                    });

                    // No-show
                    if (booking.status !== 'NO_SHOW' && booking.status !== 'CANCELLED') {
                      moreActions.push({
                        key: 'no-show',
                        label: 'Đánh dấu No-Show',
                        icon: <UserX className="w-4 h-4" />,
                        warning: true,
                        onClick: () => onNoShowRoom(room),
                        title: 'Đánh dấu khách không đến (Phạt đêm đầu)',
                      });
                    }

                    // Hủy phòng
                    if (booking.status !== 'CANCELLED') {
                      moreActions.push({
                        key: 'cancel-room',
                        label: 'Hủy phòng lẻ',
                        icon: <Ban className="w-4 h-4" />,
                        danger: true,
                        onClick: () => onCancelRoom(booking, room, detail.roomTypeName),
                        title: 'Hủy phòng này theo chính sách FIT',
                      });
                    }
                  } else if (room.status === 'CHECKED_IN') {
                    moreActions.push({
                      key: 'service-order',
                      label: 'Gọi món / POS',
                      icon: <Utensils className="w-4 h-4" />,
                      onClick: () => onServiceOrder(booking, room),
                      title: 'Đặt dịch vụ phòng, minibar',
                    });

                    moreActions.push({
                      key: 'stay-guests',
                      label: 'Cập nhật khách lưu trú',
                      icon: <Users className="w-4 h-4" />,
                      onClick: () => onStayGuests(booking, room),
                      title: 'Quản lý thông tin CCCD/Hộ chiếu khách lưu trú',
                    });

                    moreActions.push({
                      key: 'swap',
                      label: 'Đổi phòng',
                      icon: <ArrowLeftRight className="w-4 h-4" />,
                      onClick: () =>
                        onSwapRoom({
                          bookingId: booking.id,
                          bookingNumber: booking.bookingNumber,
                          bookingRoomId: room.id,
                          currentRoomInstanceId: room.roomInstanceId,
                          currentRoomNumber: room.roomNumber,
                          currentRoomTypeName: detail.roomTypeName,
                          guestName: booking.guestName,
                          checkInDate: detail.checkInDate,
                          checkOutDate: detail.checkOutDate,
                          hotelId: booking.hotelId,
                        }),
                      title: 'Đổi sang phòng khác',
                    });

                    moreActions.push({
                      key: 'add-charge',
                      label: 'Thêm phụ phí',
                      icon: <DollarSign className="w-4 h-4" />,
                      onClick: () => onAddCharge(booking, room, detail),
                      title: 'Thêm phụ phí phát sinh',
                    });
                  }

                  return (
                    <div
                      key={room.id}
                      className="bg-neutral-50/70 border border-neutral-200/80 rounded-xl p-3.5 space-y-3 transition-colors hover:bg-neutral-50"
                    >
                      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
                        {/* THÔNG TIN PHÒNG VÀ TRẠNG THÁI */}
                        <div className="flex items-center gap-3">
                          <span className="text-xs font-bold text-neutral-500 uppercase tracking-wider">
                            Phòng #{idx + 1}
                          </span>

                          {room.status === 'NO_SHOW' ? (
                            <span className="inline-flex items-center gap-1 text-red-700 bg-red-50 px-2.5 py-1 rounded-lg font-semibold text-xs border border-red-200">
                              <UserX className="w-3.5 h-3.5" /> Vắng mặt (No-Show)
                            </span>
                          ) : room.status === 'CHECKED_IN' ? (
                            <span className="inline-flex items-center gap-1.5 text-emerald-800 bg-emerald-100/80 px-2.5 py-1 rounded-lg font-mono font-bold text-xs border border-emerald-300">
                              <DoorClosed className="w-3.5 h-3.5 text-emerald-600" />
                              Phòng {room.roomNumber} (Đang ở)
                            </span>
                          ) : isRoomAssigned ? (
                            <span className="inline-flex items-center gap-1.5 text-blue-800 bg-blue-50 px-2.5 py-1 rounded-lg font-mono font-bold text-xs border border-blue-200">
                              <DoorClosed className="w-3.5 h-3.5 text-blue-600" />
                              Phòng {room.roomNumber}
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 text-amber-800 bg-amber-50 px-2.5 py-1 rounded-lg text-xs font-semibold border border-amber-200">
                              <AlertTriangle className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                              Chưa xếp phòng
                            </span>
                          )}

                          {/* Thông tin số khách */}
                          <span className="text-xs text-neutral-500 hidden sm:inline-flex items-center gap-1">
                            <Users className="w-3.5 h-3.5 text-neutral-400" />
                            {room.guestCount || 1} khách
                          </span>
                        </div>

                        {/* CỤM NÚT HÀNH ĐỘNG TINH GỌN (LIFECYCLE-DRIVEN) */}
                        <div className="flex items-center gap-2 self-end md:self-auto flex-wrap">
                          {/* TRƯỜNG HỢP 1: CHƯA XẾP PHÒNG */}
                          {!isRoomAssigned && booking.status === 'CONFIRMED' && room.status !== 'NO_SHOW' && (
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() =>
                                onAssignRoom({
                                  bookingId: booking.id,
                                  bookingNumber: booking.bookingNumber,
                                  bookingRoomId: room.id,
                                  hotelId: booking.hotelId,
                                  hotelName: booking.hotelName,
                                  guestName: booking.guestName,
                                  roomTypeName: detail.roomTypeName,
                                  hotelRoomTypeId: detail.hotelRoomTypeId,
                                  checkInDate: detail.checkInDate,
                                  checkOutDate: detail.checkOutDate,
                                })
                              }
                              className="bg-red-600 hover:bg-red-700 text-white font-semibold shadow-xs"
                            >
                              <UserCheck className="w-3.5 h-3.5 mr-1" />
                              Xếp phòng
                            </Button>
                          )}

                          {/* TRƯỜNG HỢP 2: ĐÃ XẾP PHÒNG VÀ CHỜ KHÁCH (EXPECTED) */}
                          {isRoomAssigned && room.status === 'EXPECTED' && (
                            <>
                              <Button
                                variant="primary"
                                size="sm"
                                onClick={() => onCheckIn(room)}
                                className="bg-red-600 hover:bg-red-700 text-white font-semibold shadow-xs"
                                title="Thực hiện thủ tục Check-in"
                              >
                                <CheckCircle className="w-3.5 h-3.5 mr-1" />
                                Check-in
                              </Button>

                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => onStayGuests(booking, room)}
                                className="text-blue-700 hover:text-blue-800 border-blue-200 hover:bg-blue-50"
                                title="Quản lý thông tin khách lưu trú (CCCD/Hộ chiếu)"
                              >
                                <Users className="w-3.5 h-3.5 mr-1" />
                                Khách lưu trú
                              </Button>
                            </>
                          )}

                          {/* TRƯỜNG HỢP 3: ĐANG Ở (CHECKED_IN) */}
                          {room.status === 'CHECKED_IN' && (
                            <>
                              <Button
                                variant="primary"
                                size="sm"
                                onClick={() => onCheckOut(room)}
                                className="bg-red-600 hover:bg-red-700 text-white font-semibold shadow-xs"
                                title="Kiểm tra phụ thu trả muộn và thực hiện thủ tục Check-out"
                              >
                                <LogOut className="w-3.5 h-3.5 mr-1" />
                                Check-out
                              </Button>

                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => onStayGuests(booking, room)}
                                className="text-blue-700 hover:text-blue-800 border-blue-200 hover:bg-blue-50"
                                title="Quản lý thông tin khách lưu trú (CCCD/Hộ chiếu)"
                              >
                                <Users className="w-3.5 h-3.5 mr-1" />
                                Khách lưu trú ({room.bookingGuests?.length || 0})
                              </Button>

                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => onCheckoutSettlement(room.id)}
                                className="text-blue-700 border-blue-200 hover:bg-blue-50"
                                title="Xem bảng kê công nợ và thu tiền tạm ứng / thanh toán"
                              >
                                <Receipt className="w-3.5 h-3.5 mr-1 text-blue-600" />
                                Quyết toán
                              </Button>
                            </>
                          )}

                          {/* DROPDOWN MENU THAO TÁC PHỤ */}
                          {moreActions.length > 0 && <RoomActionDropdown items={moreActions} />}
                        </div>
                      </div>

                      {/* KHÁCH LƯU TRÚ THỰC TẾ TRONG PHÒNG */}
                      <div className="border-t border-neutral-100 pt-2 text-xs flex items-center justify-between flex-wrap gap-2">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className="font-semibold text-neutral-600 flex items-center gap-1">
                            <Users className="w-3.5 h-3.5 text-neutral-400" />
                            Khách lưu trú:
                          </span>
                          {room.bookingGuests && room.bookingGuests.length > 0 ? (
                            room.bookingGuests.map((g, idx) => (
                              <span
                                key={idx}
                                className="inline-flex items-center gap-1 bg-white px-2 py-0.5 rounded-md border border-neutral-200 text-neutral-800 font-medium text-[11px]"
                              >
                                <span>{g.fullName}</span>
                                {g.identityNumber && (
                                  <span className="text-neutral-400 font-mono text-[10px]">
                                    ({g.identityNumber})
                                  </span>
                                )}
                              </span>
                            ))
                          ) : room.status === 'CHECKED_IN' ? (
                            <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-[11px] font-medium">
                              <AlertTriangle className="w-3 h-3 text-amber-600 shrink-0" />
                              Chưa bổ sung CCCD khách lưu trú (Cần bổ sung trước 23h)
                              <button
                                type="button"
                                onClick={() => onStayGuests(booking, room)}
                                className="ml-1 underline text-amber-900 font-bold hover:text-amber-950 cursor-pointer"
                              >
                                Khai báo ngay
                              </button>
                            </span>
                          ) : (
                            <span className="text-[11px] text-neutral-400 italic">
                              Chưa khai báo khách lưu trú (Có thể bổ sung trước hoặc sau Check-in)
                            </span>
                          )}
                        </div>
                      </div>

                      {/* DANH SÁCH PHỤ PHÍ NẾU CÓ */}
                      {room.charges && room.charges.length > 0 && (
                        <div className="border-t border-neutral-200/80 pt-2 text-xs space-y-1.5">
                          <span className="font-semibold text-neutral-700">Phụ phí phát sinh:</span>
                          <div className="flex flex-wrap gap-2">
                            {room.charges.map((c) => (
                              <div
                                key={c.id}
                                className="inline-flex items-center gap-1.5 bg-white px-2.5 py-1 rounded-lg border border-neutral-200 shadow-2xs"
                              >
                                <span className="text-neutral-700 font-medium">
                                  {c.itemName} <span className="text-neutral-400">x{c.quantity}</span>:{' '}
                                  <strong className="text-neutral-900">
                                    {new Intl.NumberFormat('vi-VN').format(c.totalAmount)} đ
                                  </strong>
                                </span>
                                <button
                                  type="button"
                                  onClick={() => onDeleteCharge(c.id, c.itemName)}
                                  className="p-0.5 text-neutral-400 hover:text-red-600 rounded transition cursor-pointer"
                                  title="Xóa khoản phụ phí này"
                                >
                                  <Trash2 className="w-3 h-3" />
                                </button>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </Modal>
  );
};

export default BookingDetailModal;

