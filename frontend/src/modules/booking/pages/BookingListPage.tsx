import React, { useState, useEffect, useCallback } from 'react';
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
  AlertTriangle,
  Users,
  Utensils,
  Trash2,
  Receipt,
  UserPlus,
  UserX,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import bookingService from '../services/booking.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import StayGuestsModal from '../components/StayGuestsModal';
import SurchargeProposalModal from '../components/SurchargeProposalModal';
import RoomServiceOrderModal from '../components/RoomServiceOrderModal';
import CheckoutSettlementModal from '../components/CheckoutSettlementModal';
import WalkInBookingModal from '../components/WalkInBookingModal';
import type {
  BookingResponse,
  BookingStatus,
  BookingChargeType,
  BookingRoomResponse,
  SurchargeProposalResponse,
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
  const [searchStatus, setSearchStatus] = useState<BookingStatus | ''>('');
  const [assignedTab, setAssignedTab] = useState<'ALL' | 'UNASSIGNED' | 'ASSIGNED'>('ALL');

  // Modals & Selected Booking
  const [selectedBooking, setSelectedBooking] = useState<BookingResponse | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  // Stay Guests Modal
  const [isStayGuestsModalOpen, setIsStayGuestsModalOpen] = useState(false);
  const [stayGuestsTarget, setStayGuestsTarget] = useState<{
    bookingRoomId: number;
    hotelId: number;
    bookingId?: number;
    roomId?: number;
    roomNumber?: string;
  } | null>(null);

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

  // Surcharge Proposal Modal
  const [isSurchargeProposalModalOpen, setIsSurchargeProposalModalOpen] = useState(false);
  const [surchargeProposal, setSurchargeProposal] = useState<SurchargeProposalResponse | null>(null);
  const [surchargeActionType, setSurchargeActionType] = useState<'CHECK_IN' | 'CHECK_OUT'>('CHECK_IN');
  const [surchargeTargetRoom, setSurchargeTargetRoom] = useState<BookingRoomResponse | null>(null);
  const [isProcessingSurchargeAction, setIsProcessingSurchargeAction] = useState(false);

  // Room Service Order Modal
  const [isServiceOrderModalOpen, setIsServiceOrderModalOpen] = useState(false);
  const [serviceOrderTarget, setServiceOrderTarget] = useState<{
    bookingId: number;
    bookingNumber?: string;
    roomInstanceId: number;
    roomNumber?: string;
    hotelId: number;
  } | null>(null);

  // Delete Charge Confirmation Modal
  const [isDeleteChargeModalOpen, setIsDeleteChargeModalOpen] = useState(false);
  const [chargeToDelete, setChargeToDelete] = useState<{ id: number; name: string } | null>(null);
  const [isDeletingCharge, setIsDeletingCharge] = useState(false);

  // Cancel Modal
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancellingBookingId, setCancellingBookingId] = useState<number | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  // No-Show Modal
  const [isNoShowModalOpen, setIsNoShowModalOpen] = useState(false);
  const [noShowBooking, setNoShowBooking] = useState<BookingResponse | null>(null);
  const [noShowReason, setNoShowReason] = useState('Khách không đến nhận phòng theo lịch (Quá 23:59)');
  const [isSubmittingNoShow, setIsSubmittingNoShow] = useState(false);

  // Room No-Show Modal
  const [isRoomNoShowModalOpen, setIsRoomNoShowModalOpen] = useState(false);
  const [noShowTargetRoom, setNoShowTargetRoom] = useState<{ id: number; label: string } | null>(null);
  const [roomNoShowReason, setRoomNoShowReason] = useState('Khách không đến nhận phòng này');
  const [isSubmittingRoomNoShow, setIsSubmittingRoomNoShow] = useState(false);

  // Action status message
  const [actionSuccessMessage, setActionSuccessMessage] = useState<string | null>(null);

  // Walk-in Booking Modal
  const [isWalkInModalOpen, setIsWalkInModalOpen] = useState(false);

  // Checkout Settlement Modal
  const [isCheckoutSettlementModalOpen, setIsCheckoutSettlementModalOpen] = useState(false);
  const [checkoutSettlementRoomId, setCheckoutSettlementRoomId] = useState<number | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const isAssignedParam =
        assignedTab === 'ALL' ? undefined : assignedTab === 'ASSIGNED';

      const res = await bookingService.filter({
        bookingNumber: searchBookingNumber ? searchBookingNumber.trim() : undefined,
        status: searchStatus || undefined,
        isAssigned: isAssignedParam,
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
  }, [page, pageSize, searchBookingNumber, searchStatus, assignedTab]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Load available physical rooms when assigning
  useEffect(() => {
    if (isAssignModalOpen) {
      roomInstanceService
        .filter({ pageSize: 100 })
        .then((res) => {
          if (res.result) {
            setAvailableRooms(res.result.content || []);
          }
        })
        .catch((err) => console.error('Lỗi tải phòng vật lý:', err));
    }
  }, [isAssignModalOpen]);

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
      if (selectedBooking) {
        const updated = await bookingService.getById(selectedBooking.id);
        if (updated.result) {
          setSelectedBooking(updated.result);
        }
      }
    } catch (err: unknown) {
      console.error('Lỗi xếp phòng:', err);
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Không thể xếp phòng. Vui lòng kiểm tra lại phòng có đang bị trùng lịch hay không.';
      alert(`Lỗi xếp phòng: ${errorMsg}`);
    } finally {
      setIsAssigning(false);
    }
  };

  const handleOpenStayGuestsModal = (booking: BookingResponse, room: BookingRoomResponse) => {
    if (!room.roomInstanceId || !room.roomNumber) {
      alert('Phòng chưa được xếp phòng vật lý. Vui lòng xếp phòng trước khi khai báo khách lưu trú!');
      return;
    }
    setStayGuestsTarget({
      bookingRoomId: room.id,
      hotelId: booking.hotelId,
      bookingId: booking.id,
      roomId: room.roomInstanceId,
      roomNumber: room.roomNumber,
    });
    setIsStayGuestsModalOpen(true);
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
      if (selectedBooking) {
        const updated = await bookingService.getById(selectedBooking.id);
        if (updated.result) {
          setSelectedBooking(updated.result);
        }
      }
    } catch (err) {
      console.error('Lỗi thêm phụ phí:', err);
      alert('Không thể thêm phụ phí. Vui lòng kiểm tra lại.');
    } finally {
      setIsSubmittingCharge(false);
    }
  };

  const handleCheckIn = async (bookingRoomId: number) => {
    try {
      await bookingService.checkIn(bookingRoomId);
      setActionSuccessMessage('Check-in thành công! Phòng đã chuyển sang trạng thái đang ở và đồng bộ hàng chờ lưu trú.');
      loadData();
      if (selectedBooking) {
        const updated = await bookingService.getById(selectedBooking.id);
        if (updated.result) {
          setSelectedBooking(updated.result);
        }
      }
    } catch (err: unknown) {
      console.error('Lỗi check-in:', err);
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Check-in thất bại. Vui lòng kiểm tra phòng đã được gán và ở trạng thái sẵn sàng (READY).';
      alert(`Check-in thất bại: ${errorMsg}`);
    }
  };

  const handleOpenCheckoutSettlement = (bookingRoomId: number) => {
    setCheckoutSettlementRoomId(bookingRoomId);
    setIsCheckoutSettlementModalOpen(true);
  };

  const handleInitiateCheckIn = async (room: BookingRoomResponse) => {
    try {
      const preview = await bookingService.previewSurcharge(room.id, { chargeType: 'EARLY_CHECKIN' });
      if (preview.result && preview.result.applicable) {
        setSurchargeProposal(preview.result);
        setSurchargeActionType('CHECK_IN');
        setSurchargeTargetRoom(room);
        setIsSurchargeProposalModalOpen(true);
        return;
      }
    } catch (err) {
      console.warn('Lỗi kiểm tra phụ thu nhận phòng sớm, tiếp tục check-in thường:', err);
    }
    await handleCheckIn(room.id);
  };

  const handleInitiateCheckOut = async (room: BookingRoomResponse) => {
    try {
      const preview = await bookingService.previewSurcharge(room.id, { chargeType: 'LATE_CHECKOUT' });
      if (preview.result && preview.result.applicable) {
        setSurchargeProposal(preview.result);
        setSurchargeActionType('CHECK_OUT');
        setSurchargeTargetRoom(room);
        setIsSurchargeProposalModalOpen(true);
        return;
      }
    } catch (err) {
      console.warn('Lỗi kiểm tra phụ thu trả phòng muộn, tiếp tục mở bảng quyết toán check-out:', err);
    }
    handleOpenCheckoutSettlement(room.id);
  };

  const handleApplySurchargeProposal = async () => {
    if (!surchargeTargetRoom || !surchargeProposal) return;
    setIsProcessingSurchargeAction(true);
    try {
      await bookingService.addCharge(surchargeTargetRoom.id, {
        chargeType: surchargeActionType === 'CHECK_IN' ? 'EARLY_CHECKIN' : 'LATE_CHECKOUT',
        itemName:
          surchargeProposal.suggestedItemName ||
          surchargeProposal.policyRule ||
          (surchargeActionType === 'CHECK_IN' ? 'Phụ thu nhận phòng sớm' : 'Phụ thu trả phòng muộn'),
        quantity: 1,
        unitPrice: surchargeProposal.suggestedAmount || 0,
        serviceFeeRate: surchargeProposal.defaultServiceFeeRate || 5,
        vatRate: surchargeProposal.defaultVatRate || 8,
      });

      setIsSurchargeProposalModalOpen(false);

      if (surchargeActionType === 'CHECK_IN') {
        await handleCheckIn(surchargeTargetRoom.id);
      } else {
        setActionSuccessMessage('Đã ghi nhận phụ thu trả phòng muộn vào công nợ phòng. Vui lòng thanh toán số dư còn nợ trước khi đóng sổ.');
        handleOpenCheckoutSettlement(surchargeTargetRoom.id);
      }
    } catch (err) {
      console.error('Lỗi khi áp dụng phụ thu:', err);
      alert('Không thể ghi nhận phụ thu.');
    } finally {
      setIsProcessingSurchargeAction(false);
    }
  };

  const handleWaiveSurcharge = async (reason: string) => {
    if (!surchargeTargetRoom) return;
    setIsProcessingSurchargeAction(true);
    try {
      setIsSurchargeProposalModalOpen(false);
      if (surchargeActionType === 'CHECK_IN') {
        await handleCheckIn(surchargeTargetRoom.id);
        setActionSuccessMessage(
          `Đã miễn phụ thu (${reason}) và hoàn tất Check-in!`
        );
      } else {
        setActionSuccessMessage(
          `Đã miễn phụ thu (${reason}). Tiếp tục kiểm tra công nợ để đóng sổ Check-out!`
        );
        handleOpenCheckoutSettlement(surchargeTargetRoom.id);
      }
    } catch (err) {
      console.error('Lỗi khi miễn phụ thu:', err);
    } finally {
      setIsProcessingSurchargeAction(false);
    }
  };

  const handleCustomAdjustSurcharge = () => {
    if (!surchargeTargetRoom || !surchargeProposal) return;
    setIsSurchargeProposalModalOpen(false);
    setTargetBookingRoomId(surchargeTargetRoom.id);
    setChargeType(surchargeActionType === 'CHECK_IN' ? 'EARLY_CHECKIN' : 'LATE_CHECKOUT');
    setChargeItemName(
      surchargeProposal.suggestedItemName ||
      surchargeProposal.policyRule ||
      (surchargeActionType === 'CHECK_IN' ? 'Phụ thu nhận phòng sớm' : 'Phụ thu trả phòng muộn')
    );
    setChargeQuantity(1);
    setChargeUnitPrice(surchargeProposal.suggestedAmount || 100000);
    setChargeServiceFeeRate(surchargeProposal.defaultServiceFeeRate || 5);
    setChargeVatRate(surchargeProposal.defaultVatRate || 8);
    setIsChargeModalOpen(true);
  };

  const handleOpenServiceOrderModal = (booking: BookingResponse, room: BookingRoomResponse) => {
    if (!room.roomInstanceId || !room.roomNumber) {
      alert('Phòng chưa được xếp phòng vật lý. Vui lòng xếp phòng trước khi gọi món POS!');
      return;
    }
    setServiceOrderTarget({
      bookingId: booking.id,
      bookingNumber: booking.bookingNumber,
      roomInstanceId: room.roomInstanceId,
      roomNumber: room.roomNumber,
      hotelId: booking.hotelId,
    });
    setIsServiceOrderModalOpen(true);
  };

  const handlePromptDeleteCharge = (chargeId: number, itemName: string) => {
    setChargeToDelete({ id: chargeId, name: itemName });
    setIsDeleteChargeModalOpen(true);
  };

  const handleConfirmDeleteCharge = async () => {
    if (!chargeToDelete) return;
    setIsDeletingCharge(true);
    try {
      await bookingService.deleteCharge(chargeToDelete.id);
      setIsDeleteChargeModalOpen(false);
      setChargeToDelete(null);
      setActionSuccessMessage('Đã xóa phụ phí thành công!');
      loadData();
      if (selectedBooking) {
        const updated = await bookingService.getById(selectedBooking.id);
        if (updated.result) {
          setSelectedBooking(updated.result);
        }
      }
    } catch (err) {
      console.error('Lỗi xóa phụ phí:', err);
      alert('Không thể xóa phụ phí. Vui lòng thử lại.');
    } finally {
      setIsDeletingCharge(false);
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

  const handleOpenNoShowModal = (booking: BookingResponse) => {
    setNoShowBooking(booking);
    setNoShowReason('Khách không đến nhận phòng theo lịch (Quá 23:59)');
    setIsNoShowModalOpen(true);
  };

  const handleConfirmNoShow = async () => {
    if (!noShowBooking) return;
    if (!noShowReason.trim()) {
      alert('Vui lòng nhập lý do khách không đến (No-Show).');
      return;
    }
    setIsSubmittingNoShow(true);
    try {
      await bookingService.markAsNoShow(noShowBooking.id, { reason: noShowReason.trim() });
      setIsNoShowModalOpen(false);
      setActionSuccessMessage(
        `Đã ghi nhận khách vắng mặt (No-Show) cho đơn ${noShowBooking.bookingNumber}! Đã phạt đêm đầu, giải phóng các đêm còn lại và tạo yêu cầu hoàn cọc dôi dư (nếu có).`
      );
      loadData();
      if (selectedBooking && selectedBooking.id === noShowBooking.id) {
        const updated = await bookingService.getById(selectedBooking.id);
        if (updated.result) {
          setSelectedBooking(updated.result);
        }
      }
    } catch (err: unknown) {
      console.error('Lỗi khi đánh dấu No-Show:', err);
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Đánh dấu No-Show thất bại. Vui lòng kiểm tra lại.';
      alert(`Thao tác thất bại: ${errorMsg}`);
    } finally {
      setIsSubmittingNoShow(false);
    }
  };

  const handlePromptRoomNoShow = (room: BookingRoomResponse) => {
    const label = room.roomNumber ? `Phòng ${room.roomNumber}` : `Phòng ID ${room.id}`;
    setNoShowTargetRoom({ id: room.id, label });
    setRoomNoShowReason('Khách không đến nhận phòng này theo lịch');
    setIsRoomNoShowModalOpen(true);
  };

  const handleConfirmRoomNoShow = async () => {
    if (!noShowTargetRoom) return;
    if (!roomNoShowReason.trim()) {
      alert('Vui lòng nhập lý do khách không đến nhận phòng.');
      return;
    }
    setIsSubmittingRoomNoShow(true);
    try {
      await bookingService.markRoomAsNoShow(noShowTargetRoom.id, { reason: roomNoShowReason.trim() });
      setIsRoomNoShowModalOpen(false);
      setActionSuccessMessage(
        `Đã ghi nhận No-Show cho ${noShowTargetRoom.label}! Hệ thống đã tính phạt đêm đầu cho phòng này và giải phóng phòng vật lý.`
      );
      loadData();
      if (selectedBooking) {
        const updated = await bookingService.getById(selectedBooking.id);
        if (updated.result) {
          setSelectedBooking(updated.result);
        }
      }
    } catch (err: unknown) {
      console.error('Lỗi khi đánh dấu No-Show phòng:', err);
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Đánh dấu No-Show phòng thất bại. Vui lòng kiểm tra lại.';
      alert(`Thao tác thất bại: ${errorMsg}`);
    } finally {
      setIsSubmittingRoomNoShow(false);
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
            Quản lý vòng đời lưu trú, xếp phòng vật lý, khai báo khách lưu trú BCA, phụ phí và Check-in/Check-out
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

      {/* QUICK FILTER TABS: TẤT CẢ / CHƯA XẾP PHÒNG / ĐÃ XẾP PHÒNG */}
      <div className="flex items-center gap-2 border-b border-neutral-200 pb-3">
        <button
          onClick={() => {
            setAssignedTab('ALL');
            setPage(0);
          }}
          className={`px-4 py-2 text-sm font-semibold rounded-xl transition-colors ${
            assignedTab === 'ALL'
              ? 'bg-neutral-900 text-white shadow-xs'
              : 'text-neutral-600 hover:bg-neutral-100'
          }`}
        >
          Tất cả đơn đặt
        </button>
        <button
          onClick={() => {
            setAssignedTab('UNASSIGNED');
            setPage(0);
          }}
          className={`flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl transition-colors ${
            assignedTab === 'UNASSIGNED'
              ? 'bg-amber-500 text-white shadow-xs'
              : 'text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200'
          }`}
        >
          <AlertTriangle className="w-4 h-4" />
          Chưa xếp phòng vật lý
        </button>
        <button
          onClick={() => {
            setAssignedTab('ASSIGNED');
            setPage(0);
          }}
          className={`px-4 py-2 text-sm font-semibold rounded-xl transition-colors ${
            assignedTab === 'ASSIGNED'
              ? 'bg-emerald-600 text-white shadow-xs'
              : 'text-neutral-600 hover:bg-neutral-100'
          }`}
        >
          Đã xếp phòng đầy đủ
        </button>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap gap-4 items-center">
        <div className="flex-1 min-w-[240px]">
          <Input
            placeholder="Tìm theo mã đặt phòng (VD: BK...)"
            value={searchBookingNumber}
            onChange={(e) => setSearchBookingNumber(e.target.value)}
          />
        </div>

        <div className="w-48">
          <select
            className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-red-600 focus:border-red-600"
            value={searchStatus}
            onChange={(e) => setSearchStatus(e.target.value as BookingStatus | '')}
          >
            <option value="">Tất cả trạng thái</option>
            <option value="CONFIRMED">Đã xác nhận</option>
            <option value="CANCELLED">Đã hủy</option>
            <option value="NO_SHOW">Vắng mặt (No-show)</option>
          </select>
        </div>

        <Button variant="primary" onClick={() => { setPage(0); loadData(); }}>
          <Search className="w-4 h-4 mr-2" />
          Tìm kiếm
        </Button>

        <Button
          variant="outline"
          onClick={() => {
            setSearchBookingNumber('');
            setSearchStatus('');
            setAssignedTab('ALL');
            setPage(0);
          }}
        >
          Đặt lại
        </Button>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex justify-between items-center bg-neutral-50 p-3 rounded-2xl border border-neutral-200">
        <div className="text-sm text-neutral-600 font-medium">
          Tổng số: <span className="font-bold text-neutral-900">{totalElements}</span> đơn đặt phòng
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            size="sm"
            onClick={() => setIsWalkInModalOpen(true)}
            className="bg-red-600 hover:bg-red-700 text-white font-semibold shadow-xs"
          >
            <UserPlus className="w-4 h-4 mr-1.5" />
            Nhận khách vãng lai (Walk-in)
          </Button>
          <Button variant="outline" size="sm" onClick={loadData} disabled={isLoading}>
            <RefreshCw className={`w-4 h-4 mr-1 ${isLoading ? 'animate-spin' : ''}`} />
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
                <th className="p-4">Mã đơn</th>
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
                bookings.map((booking) => {
                  const hasUnassignedRoom = booking.bookingDetails?.some((d) =>
                    d.bookingRooms?.some((r) => !r.roomInstanceId)
                  );

                  return (
                    <tr key={booking.id} className="hover:bg-neutral-50 transition-colors">
                      <td className="p-4 font-semibold text-neutral-900">
                        {booking.bookingNumber}
                        <div className="text-xs text-neutral-400 font-normal mt-0.5">
                          {booking.bookingType}
                        </div>
                      </td>
                      <td className="p-4 text-neutral-800">{booking.hotelName}</td>
                      <td className="p-4">
                        <div className="font-medium text-neutral-900">{booking.guestPhone}</div>
                        {booking.companyName && (
                          <div className="text-xs text-neutral-500">{booking.companyName}</div>
                        )}
                      </td>
                      <td className="p-4 text-neutral-700">
                        {booking.bookingDetails?.map((d) => d.roomTypeName).join(', ') || 'N/A'}
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
                      <td className="p-4 text-center space-y-1">
                        <div>{getStatusBadge(booking.status)}</div>
                        {hasUnassignedRoom && booking.status === 'CONFIRMED' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-semibold rounded-full bg-amber-100 text-amber-800 border border-amber-300">
                            <AlertTriangle className="w-3 h-3" /> Chưa xếp phòng
                          </span>
                        )}
                      </td>
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
                            <>
                              <button
                                onClick={() => handleOpenNoShowModal(booking)}
                                className="p-1.5 text-amber-600 hover:text-amber-800 hover:bg-amber-50 rounded-lg"
                                title="Đánh dấu khách không đến (No-Show)"
                              >
                                <UserX className="w-4 h-4" />
                              </button>

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
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
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
          maxWidth="2xl"
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
                <div className="mt-1 flex items-center gap-1.5 flex-wrap">
                  {getStatusBadge(selectedBooking.status)}
                  {selectedBooking.status === 'CONFIRMED' && (
                    <button
                      onClick={() => handleOpenNoShowModal(selectedBooking)}
                      className="px-2 py-0.5 text-[11px] font-semibold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-300 rounded-md transition-colors"
                      title="Xử lý khách vắng mặt (No-Show)"
                    >
                      Xử lý No-Show
                    </button>
                  )}
                </div>
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

                  {detail.bookingRooms?.map((room, idx) => {
                    const isRoomAssigned = Boolean(room.roomInstanceId && room.roomNumber);

                    return (
                      <div key={room.id} className="bg-neutral-50 rounded-lg p-3 space-y-3">
                        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                          <div className="text-sm font-semibold text-neutral-800 flex items-center gap-2">
                            <span>Phòng #{idx + 1}:</span>
                            {room.status === 'NO_SHOW' ? (
                              <span className="text-red-700 bg-red-50 px-2 py-0.5 rounded-md font-semibold text-xs border border-red-200">
                                Vắng mặt (No-Show)
                              </span>
                            ) : isRoomAssigned ? (
                              <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md font-mono font-semibold border border-emerald-200">
                                {room.roomNumber}
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-amber-700 bg-amber-50 px-2.5 py-0.5 rounded-md text-xs font-semibold border border-amber-200">
                                <AlertTriangle className="w-3.5 h-3.5" /> Chưa xếp phòng
                              </span>
                            )}
                          </div>

                          <div className="flex flex-wrap items-center gap-2">
                            {/* Nút Xếp phòng / Đổi phòng */}
                            {selectedBooking.status === 'CONFIRMED' && room.status !== 'NO_SHOW' && (
                              <Button
                                variant={!isRoomAssigned ? 'primary' : 'outline'}
                                size="sm"
                                onClick={() => handleOpenAssignModal(room.id)}
                              >
                                <UserCheck className="w-3.5 h-3.5 mr-1" />
                                {isRoomAssigned ? 'Đổi phòng' : 'Xếp phòng'}
                              </Button>
                            )}

                            {/* Nút Khách lưu trú (Bắt buộc phải gán phòng trước) */}
                            {room.status !== 'NO_SHOW' && (
                              <Button
                                variant="outline"
                                size="sm"
                                disabled={!isRoomAssigned}
                                title={
                                  !isRoomAssigned
                                    ? 'Cần xếp phòng vật lý trước khi khai báo khách lưu trú'
                                    : 'Quản lý thông tin khách lưu trú (CCCD/Hộ chiếu)'
                                }
                                onClick={() => handleOpenStayGuestsModal(selectedBooking, room)}
                                className={
                                  !isRoomAssigned
                                    ? 'opacity-50 cursor-not-allowed'
                                    : 'text-blue-700 hover:text-blue-800 border-blue-200 hover:bg-blue-50'
                                }
                              >
                                <Users className="w-3.5 h-3.5 mr-1" />
                                Khách lưu trú
                              </Button>
                            )}

                            {/* Nút Check-in (Bắt buộc phải gán phòng trước) */}
                            {room.status === 'EXPECTED' && (
                              <Button
                                variant="primary"
                                size="sm"
                                disabled={!isRoomAssigned}
                                title={
                                  !isRoomAssigned
                                    ? 'Cần xếp phòng vật lý trước khi Check-in'
                                    : 'Thực hiện thủ tục Check-in'
                                }
                                onClick={() => handleInitiateCheckIn(room)}
                                className={
                                  !isRoomAssigned
                                    ? 'opacity-50 cursor-not-allowed bg-neutral-400 hover:bg-neutral-400'
                                    : ''
                                }
                              >
                                <CheckCircle className="w-3.5 h-3.5 mr-1" />
                                Check-in
                              </Button>
                            )}

                            {/* Nút No-Show phòng lẻ */}
                            {room.status === 'EXPECTED' && selectedBooking.status !== 'NO_SHOW' && selectedBooking.status !== 'CANCELLED' && (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handlePromptRoomNoShow(room)}
                                className="text-amber-700 hover:text-amber-800 border-amber-300 hover:bg-amber-50"
                                title="Đánh dấu khách không đến nhận phòng này (Phạt đêm đầu và nhả phòng)"
                              >
                                <UserX className="w-3.5 h-3.5 mr-1 text-amber-600" />
                                No-Show
                              </Button>
                            )}

                            {/* Nút Quyết toán & Check-out */}
                            {room.status === 'CHECKED_IN' && (
                              <>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleOpenCheckoutSettlement(room.id)}
                                  className="text-blue-700 border-blue-200 hover:bg-blue-50"
                                  title="Xem bảng kê công nợ và thu tiền tạm ứng / thanh toán"
                                >
                                  <Receipt className="w-3.5 h-3.5 mr-1 text-blue-600" />
                                  Quyết toán
                                </Button>
                                <Button
                                  variant="primary"
                                  size="sm"
                                  onClick={() => handleInitiateCheckOut(room)}
                                  className="bg-red-600 hover:bg-red-700 text-white font-semibold"
                                  title="Kiểm tra phụ thu trả muộn và thực hiện thủ tục Check-out"
                                >
                                  <LogOut className="w-3.5 h-3.5 mr-1" />
                                  Check-out
                                </Button>
                              </>
                            )}

                            {/* Nút Gọi món / POS Dịch vụ phòng */}
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={!isRoomAssigned}
                              title={
                                !isRoomAssigned
                                  ? 'Cần xếp phòng vật lý trước khi gọi món POS'
                                  : 'Đặt món ăn, đồ uống minibar và dịch vụ phòng'
                              }
                              onClick={() => handleOpenServiceOrderModal(selectedBooking, room)}
                              className={
                                !isRoomAssigned
                                  ? 'opacity-50 cursor-not-allowed'
                                  : 'text-emerald-700 hover:text-emerald-800 border-emerald-200 hover:bg-emerald-50'
                              }
                            >
                              <Utensils className="w-3.5 h-3.5 mr-1 text-emerald-600" />
                              Gọi món / POS
                            </Button>

                            {/* Nút Phụ phí */}
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
                            <div className="mt-1 space-y-1">
                              {room.charges.map((c) => (
                                <div
                                  key={c.id}
                                  className="flex items-center justify-between bg-neutral-50 px-2.5 py-1 rounded-md border border-neutral-200"
                                >
                                  <span className="text-neutral-700">
                                    {c.itemName} x{c.quantity}:{' '}
                                    <strong className="text-neutral-900">
                                      {new Intl.NumberFormat('vi-VN').format(c.totalAmount)} đ
                                    </strong>{' '}
                                    <span className="text-neutral-500">({c.chargeType})</span>
                                  </span>
                                  <button
                                    type="button"
                                    onClick={() => handlePromptDeleteCharge(c.id, c.itemName)}
                                    className="p-1 text-neutral-400 hover:text-red-600 rounded hover:bg-red-50 transition"
                                    title="Xóa khoản phụ phí này"
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
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
              ))}
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL KHAI BÁO KHÁCH LƯU TRÚ (STAY GUESTS) */}
      {stayGuestsTarget && (
        <StayGuestsModal
          isOpen={isStayGuestsModalOpen}
          onClose={() => {
            setIsStayGuestsModalOpen(false);
            setStayGuestsTarget(null);
          }}
          bookingRoomId={stayGuestsTarget.bookingRoomId}
          hotelId={stayGuestsTarget.hotelId}
          bookingId={stayGuestsTarget.bookingId}
          roomId={stayGuestsTarget.roomId}
          roomNumber={stayGuestsTarget.roomNumber}
        />
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
              Phòng vật lý khả dụng
            </label>
            <select
              className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={selectedRoomInstanceId}
              onChange={(e) => setSelectedRoomInstanceId(Number(e.target.value))}
            >
              <option value="">-- Chọn số phòng --</option>
              {availableRooms.map((r) => (
                <option key={r.id} value={r.id}>
                  Phòng {r.roomNumber} - {r.currentStatus}
                </option>
              ))}
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button variant="outline" onClick={() => setIsAssignModalOpen(false)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              onClick={handleConfirmAssign}
              disabled={!selectedRoomInstanceId || isAssigning}
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

      {/* NO-SHOW CONFIRMATION & PENALTY PIPELINE MODAL */}
      {noShowBooking && (
        <Modal
          isOpen={isNoShowModalOpen}
          onClose={() => setIsNoShowModalOpen(false)}
          title={`Xử lý khách vắng mặt (No-Show): ${noShowBooking.bookingNumber}`}
          maxWidth="lg"
        >
          <div className="space-y-4">
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
              <div className="text-xs text-amber-800 space-y-1">
                <p className="font-bold text-sm text-amber-900">Quy trình xử lý No-Show theo pipeline chuẩn:</p>
                <ul className="list-disc list-inside space-y-0.5">
                  <li><strong>Phạt đêm đầu:</strong> Hệ thống tự động hạch toán phạt 100% tiền phòng đêm đầu tiên vào Folio.</li>
                  <li><strong>Giải phóng phòng:</strong> Nhả toàn bộ các đêm còn lại về kho bán hàng và đưa phòng vật lý về READY.</li>
                  <li><strong>Hoàn tiền cọc dôi dư:</strong> Nếu khách đã cọc/trả trước nhiều hơn phí phạt, hệ thống sẽ tự động tạo yêu cầu REFUND ở trạng thái PENDING.</li>
                </ul>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 p-3 bg-neutral-50 rounded-xl text-xs">
              <div>
                <span className="text-neutral-500">Mã đơn:</span>
                <p className="font-semibold text-neutral-900 font-mono">{noShowBooking.bookingNumber}</p>
              </div>
              <div>
                <span className="text-neutral-500">Khách hàng:</span>
                <p className="font-semibold text-neutral-900">{noShowBooking.guestPhone || 'N/A'}</p>
              </div>
              <div>
                <span className="text-neutral-500">Thời gian lưu trú:</span>
                <p className="font-semibold text-neutral-900">
                  {noShowBooking.bookingDetails?.[0]?.checkInDate} ➔ {noShowBooking.bookingDetails?.[0]?.checkOutDate}
                </p>
              </div>
              <div>
                <span className="text-neutral-500">Tổng giá trị đơn:</span>
                <p className="font-bold text-red-600">
                  {new Intl.NumberFormat('vi-VN').format(noShowBooking.totalAmount)} đ
                </p>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Lý do khách không đến (No-Show) <span className="text-red-500">*</span>
              </label>
              <textarea
                value={noShowReason}
                onChange={(e) => setNoShowReason(e.target.value)}
                placeholder="Nhập lý do khách vãng mặt (ví dụ: Khách không liên lạc được quá 23:59, khách báo hủy chuyến...)"
                rows={3}
                className="w-full text-xs p-2.5 border border-neutral-300 rounded-xl focus:ring-2 focus:ring-amber-500 focus:outline-none"
              />
            </div>

            <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100">
              <Button variant="outline" onClick={() => setIsNoShowModalOpen(false)}>
                Hủy bỏ
              </Button>
              <Button
                variant="primary"
                onClick={handleConfirmNoShow}
                disabled={isSubmittingNoShow || !noShowReason.trim()}
                className="bg-amber-600 hover:bg-amber-700 text-white"
              >
                {isSubmittingNoShow ? 'Đang xử lý No-Show...' : 'Xác nhận No-Show & Hạch toán phạt'}
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* SURCHARGE PROPOSAL MODAL */}
      <SurchargeProposalModal
        isOpen={isSurchargeProposalModalOpen}
        onClose={() => {
          setIsSurchargeProposalModalOpen(false);
          setSurchargeProposal(null);
          setSurchargeTargetRoom(null);
        }}
        proposal={surchargeProposal}
        actionType={surchargeActionType}
        roomNumber={surchargeTargetRoom?.roomNumber}
        onApplyProposal={handleApplySurchargeProposal}
        onWaiveSurcharge={handleWaiveSurcharge}
        onCustomAdjust={handleCustomAdjustSurcharge}
        isProcessing={isProcessingSurchargeAction}
      />

      {/* ROOM SERVICE ORDER MODAL (POS) */}
      {serviceOrderTarget && (
        <RoomServiceOrderModal
          isOpen={isServiceOrderModalOpen}
          onClose={() => {
            setIsServiceOrderModalOpen(false);
            setServiceOrderTarget(null);
          }}
          bookingId={serviceOrderTarget.bookingId}
          bookingNumber={serviceOrderTarget.bookingNumber}
          roomInstanceId={serviceOrderTarget.roomInstanceId}
          roomNumber={serviceOrderTarget.roomNumber}
          hotelId={serviceOrderTarget.hotelId}
          onOrderSuccess={() => {
            loadData();
            if (selectedBooking) {
              bookingService.getById(selectedBooking.id).then((res) => {
                if (res.result) setSelectedBooking(res.result);
              });
            }
          }}
        />
      )}

      {/* CONFIRM DELETE CHARGE MODAL */}
      <ConfirmModal
        isOpen={isDeleteChargeModalOpen}
        onClose={() => {
          setIsDeleteChargeModalOpen(false);
          setChargeToDelete(null);
        }}
        onConfirm={handleConfirmDeleteCharge}
        title="Xác nhận xóa khoản phụ phí"
        description={`Bạn có chắc chắn muốn xóa khoản phụ phí "${chargeToDelete?.name || ''}" này không? Khoản tiền này sẽ được khấu trừ khỏi hóa đơn phòng.`}
        confirmText={isDeletingCharge ? 'Đang xóa...' : 'Xóa phụ phí'}
        cancelText="Hủy"
        variant="danger"
      />

      {/* CHECKOUT SETTLEMENT & ZERO-BALANCE CLOSURE MODAL */}
      <CheckoutSettlementModal
        isOpen={isCheckoutSettlementModalOpen}
        onClose={() => {
          setIsCheckoutSettlementModalOpen(false);
          setCheckoutSettlementRoomId(null);
        }}
        bookingRoomId={checkoutSettlementRoomId}
        onCheckOutSuccess={async (bookingNumber) => {
          setActionSuccessMessage(
            `Check-out thành công cho đơn ${bookingNumber}! Phòng đã chuyển sang trạng thái DIRTY (Chờ dọn dẹp), hóa đơn và các đơn dịch vụ đã được đóng PAID.`
          );
          loadData();
          if (selectedBooking) {
            const updated = await bookingService.getById(selectedBooking.id);
            if (updated.result) {
              setSelectedBooking(updated.result);
            }
          }
        }}
      />

      {/* WALK-IN BOOKING & INSTANT CHECK-IN MODAL */}
      <WalkInBookingModal
        isOpen={isWalkInModalOpen}
        onClose={() => setIsWalkInModalOpen(false)}
        onSuccess={(bookingNumber) => {
          setIsWalkInModalOpen(false);
          setActionSuccessMessage(
            `Tạo đơn Walk-in thành công cho đơn ${bookingNumber}! Các phòng đã được check-in trực tiếp sang trạng thái In-house.`
          );
          loadData();
        }}
      />

      {/* MODAL XÁC NHẬN ROOM NO-SHOW */}
      <Modal
        isOpen={isRoomNoShowModalOpen}
        onClose={() => setIsRoomNoShowModalOpen(false)}
        title={`Xác nhận Khách vắng mặt (No-Show) — ${noShowTargetRoom?.label || ''}`}
        maxWidth="md"
      >
        <div className="space-y-4">
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 space-y-1">
            <p className="font-bold flex items-center gap-1.5">
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              Quy tắc xử lý No-Show phòng lẻ:
            </p>
            <ul className="list-disc list-inside space-y-0.5 pl-1 text-neutral-600">
              <li>Áp dụng phí phạt 100% đêm đầu tiên cho riêng phòng này vào công nợ đơn.</li>
              <li>Các đêm tiếp theo của phòng sẽ được hủy tính tiền phòng.</li>
              <li>Dịch vụ đã đặt trước (nếu có) được giữ nguyên, không hoàn tiền.</li>
              <li>Phòng vật lý và lịch phòng các đêm còn lại sẽ lập tức được giải phóng về trạng thái Sẵn sàng (READY).</li>
              <li>Các phòng khác trong đơn vẫn tiếp tục lưu trú bình thường (sẽ được bù trừ công nợ chéo khi Check-out).</li>
            </ul>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Lý do không đến <span className="text-red-500">*</span>
            </label>
            <textarea
              className="w-full text-xs p-2.5 border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden min-h-[80px]"
              placeholder="VD: Thành viên này trong đoàn lỡ chuyến bay không đến nhận phòng..."
              value={roomNoShowReason}
              onChange={(e) => setRoomNoShowReason(e.target.value)}
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-neutral-100">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsRoomNoShowModalOpen(false)}
              disabled={isSubmittingRoomNoShow}
            >
              Hủy bỏ
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleConfirmRoomNoShow}
              isLoading={isSubmittingRoomNoShow}
              className="bg-amber-600 hover:bg-amber-700 text-white font-semibold"
            >
              Xác nhận No-Show phòng này
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default BookingListPage;
