import React from 'react';
import { Printer, X, ShieldCheck, CheckCircle2, Receipt } from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import { formatCurrency } from '../../../../core/utils/currency';
import type { BookingResponse } from '../../../booking/types/booking.types';
import type { InvoiceResponse, PaymentResponse } from '../../types/finance.types';

export interface HotelFolioReceiptProps {
  isOpen: boolean;
  onClose: () => void;
  booking: BookingResponse | null;
  invoice: InvoiceResponse | null;
  payment?: PaymentResponse | null;
  appliedVoucher?: {
    code: string;
    discountPercent?: number;
    discountAmount?: number;
    description?: string;
  } | null;
  customAmountPaid?: number;
}

export const HotelFolioReceipt: React.FC<HotelFolioReceiptProps> = ({
  isOpen,
  onClose,
  booking,
  invoice,
  payment,
  appliedVoucher,
  customAmountPaid,
}) => {
  if (!isOpen || !booking) return null;

  // ── 1. TRÍCH XUẤT THÔNG TIN PHÒNG & KHÁCH HÀNG ─────────────────────────────
  const assignedRooms = booking.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [];
  const roomNumbers = assignedRooms
    .map((r) => r.roomNumber)
    .filter(Boolean);

  const roomTypeNames = Array.from(
    new Set(booking.bookingDetails?.map((d) => d.roomTypeName).filter(Boolean) || [])
  );

  // Trích xuất giấy tờ tùy thân CCCD / Passport của khách
  const allGuests = assignedRooms.flatMap((r) => r.bookingGuests || []);
  const guestWithIdentity = allGuests.find((g) => g.identityNumber);
  const identityDisplay = guestWithIdentity
    ? `${guestWithIdentity.identityType || 'CCCD/Passport'}: ${guestWithIdentity.identityNumber} (${guestWithIdentity.fullName || guestWithIdentity.firstName + ' ' + guestWithIdentity.lastName})`
    : 'Đã đối soát tại quầy lễ tân';

  const checkInDate = booking.bookingDetails?.[0]?.checkInDate || 'N/A';
  const checkOutDate = booking.bookingDetails?.[0]?.checkOutDate || 'N/A';

  // Tính số đêm lưu trú ước tính
  let stayNights = 1;
  try {
    const ci = new Date(checkInDate).getTime();
    const co = new Date(checkOutDate).getTime();
    const diff = Math.round((co - ci) / (1000 * 60 * 60 * 24));
    if (diff > 0) stayNights = diff;
  } catch {
    stayNights = 1;
  }

  // ── 2. BẢNG KÊ TẤT CẢ CÁC DỊCH VỤ SỬ DỤNG THEO TỪNG PHÒNG ─────────────────
  interface FolioItem {
    id: number | string;
    roomNumber?: string;
    roomTypeName?: string;
    name: string;
    category: string;
    quantity: number;
    unitPrice: number;
    vatRate: number;
    vatAmount: number;
    subtotal: number;
    totalAmount: number;
  }

  interface RoomFolioGroup {
    roomKey: string;
    roomNumber?: string;
    roomTypeName?: string;
    guestNames?: string[];
    items: FolioItem[];
    subtotal: number;
    vatAmount: number;
    totalAmount: number;
  }

  const serviceItems: FolioItem[] = [];

  if (invoice?.details && invoice.details.length > 0) {
    // Nếu đã có chi tiết hóa đơn chính thức, lấy chính xác từ hóa đơn
    invoice.details.forEach((d, idx) => {
      const match = d.description.match(/(?:Phòng|P\.)\s*([A-Za-z0-9\-]+)/i);
      const extractedRoom = match ? match[1] : undefined;

      serviceItems.push({
        id: d.id || idx + 1,
        roomNumber: extractedRoom,
        name: d.description,
        category:
          d.lineType === 'ROOM_RATE'
            ? 'Tiền phòng'
            : d.lineType === 'PRODUCT'
              ? 'Ẩm thực / Minibar'
              : d.lineType === 'SERVICE'
                ? 'Dịch vụ / Ăn uống'
                : 'Phụ phí',
        quantity: d.quantity,
        unitPrice: Number(d.unitPrice),
        vatRate: Number(d.vatRate || 8),
        vatAmount: Number(d.vatAmount),
        subtotal: Number(d.subtotal),
        totalAmount: Number(d.totalAmount),
      });
    });
  } else {
    // Phân tích chi tiết từ booking theo từng phòng
    let counter = 1;
    booking.bookingDetails?.forEach((detail) => {
      const roomTypeName = detail.roomTypeName || 'Phòng Khách sạn';
      const assignedRoomsInDetail = detail.bookingRooms || [];

      if (assignedRoomsInDetail.length > 0) {
        assignedRoomsInDetail.forEach((rm) => {
          const roomNum = rm.roomNumber;

          // Tiền phòng cho phòng vật lý này
          if (rm.dailyRates && rm.dailyRates.length > 0) {
            rm.dailyRates.forEach((dr) => {
              const sub = Number(dr.netPrice) || 0;
              const vat = Math.round(sub * (Number(dr.vatPercent || 8) / 100));
              serviceItems.push({
                id: counter++,
                roomNumber: roomNum,
                roomTypeName: roomTypeName,
                name: `Tiền phòng: ${roomTypeName} (${dr.stayDate})`,
                category: 'Tiền phòng',
                quantity: 1,
                unitPrice: sub,
                vatRate: Number(dr.vatPercent || 8),
                vatAmount: vat,
                subtotal: sub,
                totalAmount: sub + vat,
              });
            });
          } else {
            const roomCount = assignedRoomsInDetail.length || 1;
            const baseSubtotal = Number(booking.subtotalAmount) || 1250000;
            const unit = Math.round(baseSubtotal / roomCount);
            const vat = Math.round(unit * 0.08);

            serviceItems.push({
              id: counter++,
              roomNumber: roomNum,
              roomTypeName: roomTypeName,
              name: `Tiền phòng: ${roomTypeName} (${detail.checkInDate} ➔ ${detail.checkOutDate})`,
              category: 'Tiền phòng',
              quantity: 1,
              unitPrice: unit,
              vatRate: 8,
              vatAmount: vat,
              subtotal: unit,
              totalAmount: unit + vat,
            });
          }

          // Các khoản dịch vụ ăn uống, minibar, giặt là của phòng này
          rm.charges?.forEach((ch) => {
            const chType = String(ch.chargeType);
            const cat =
              chType === 'RESTAURANT' || chType === 'MINIBAR'
                ? 'Ẩm thực / Minibar'
                : chType === 'LAUNDRY' || chType === 'SPA'
                  ? 'Dịch vụ phòng'
                  : 'Phụ phí';

            serviceItems.push({
              id: counter++,
              roomNumber: roomNum,
              roomTypeName: roomTypeName,
              name: ch.itemName || 'Dịch vụ phòng',
              category: cat,
              quantity: ch.quantity || 1,
              unitPrice: Number(ch.unitPrice),
              vatRate: Number(ch.vatRate || 8),
              vatAmount: Number(ch.vatAmount || 0),
              subtotal: Number(ch.subtotal || ch.unitPrice),
              totalAmount: Number(ch.totalAmount),
            });
          });
        });
      } else {
        // Chưa xếp phòng cụ thể
        const sub = Number(booking.subtotalAmount || booking.totalAmount || 1000000);
        const vat = Math.round(sub * 0.08);
        serviceItems.push({
          id: counter++,
          name: `Tiền phòng: ${roomTypeName} (${detail.checkInDate} ➔ ${detail.checkOutDate})`,
          category: 'Tiền phòng',
          quantity: detail.quantity || 1,
          unitPrice: sub,
          vatRate: 8,
          vatAmount: vat,
          subtotal: sub,
          totalAmount: sub + vat,
        });
      }
    });

    if (serviceItems.length === 0) {
      const sub = Number(booking.subtotalAmount || booking.totalAmount || 1000000);
      const vat = Math.round(sub * 0.08);
      serviceItems.push({
        id: 1,
        name: 'Dịch vụ lưu trú khách sạn trọn gói',
        category: 'Lưu trú',
        quantity: 1,
        unitPrice: sub,
        vatRate: 8,
        vatAmount: vat,
        subtotal: sub,
        totalAmount: sub + vat,
      });
    }
  }

  // Nhóm các dịch vụ theo từng phòng
  const roomGroupsMap = new Map<string, RoomFolioGroup>();

  serviceItems.forEach((item) => {
    const rKey = item.roomNumber ? `Phòng ${item.roomNumber}` : 'Dịch vụ chung đoàn / Khác';
    if (!roomGroupsMap.has(rKey)) {
      const guestsInRoom = item.roomNumber
        ? booking.bookingDetails
            ?.flatMap((d) => d.bookingRooms || [])
            .find((r) => r.roomNumber === item.roomNumber)
            ?.bookingGuests?.map((g) => g.fullName || `${g.firstName} ${g.lastName}`.trim())
            .filter(Boolean) || []
        : [];

      const matchedRoomTypeName = item.roomTypeName || (item.roomNumber
        ? booking.bookingDetails
            ?.find((d) => d.bookingRooms?.some((r) => r.roomNumber === item.roomNumber))
            ?.roomTypeName
        : undefined);

      roomGroupsMap.set(rKey, {
        roomKey: rKey,
        roomNumber: item.roomNumber,
        roomTypeName: matchedRoomTypeName,
        guestNames: guestsInRoom,
        items: [],
        subtotal: 0,
        vatAmount: 0,
        totalAmount: 0,
      });
    }

    const g = roomGroupsMap.get(rKey)!;
    g.items.push(item);
    g.subtotal += item.subtotal;
    g.vatAmount += item.vatAmount;
    g.totalAmount += item.totalAmount;
  });

  const roomGroups = Array.from(roomGroupsMap.values());

  // ── 3. TÍNH TOÁN DÒNG TIỀN VÀ THUẾ PHÍ ──────────────────────────────────────
  const baseSubtotal =
    invoice?.subTotal !== undefined
      ? Number(invoice.subTotal)
      : serviceItems.reduce((acc, it) => acc + it.subtotal, 0);

  const serviceFeeAmount =
    invoice?.serviceFeeAmount !== undefined
      ? Number(invoice.serviceFeeAmount)
      : Math.round(baseSubtotal * 0.05);

  const totalVatAmount =
    invoice?.vatAmount !== undefined
      ? Number(invoice.vatAmount)
      : serviceItems.reduce((acc, it) => acc + it.vatAmount, 0);

  const totalBeforeVoucher = baseSubtotal + serviceFeeAmount + totalVatAmount;

  let voucherDiscount = 0;
  if (appliedVoucher) {
    if (appliedVoucher.discountPercent) {
      voucherDiscount = Math.round((totalBeforeVoucher * appliedVoucher.discountPercent) / 100);
    } else if (appliedVoucher.discountAmount) {
      voucherDiscount = Math.min(totalBeforeVoucher, appliedVoucher.discountAmount);
    }
  }

  const grandTotal =
    invoice?.grandTotal !== undefined
      ? Number(invoice.grandTotal)
      : Math.max(0, totalBeforeVoucher - voucherDiscount);

  // Số tiền thanh toán kỳ này
  const paidThisTime =
    payment?.totalAmount !== undefined
      ? Number(payment.totalAmount)
      : customAmountPaid !== undefined
        ? customAmountPaid
        : grandTotal;

  // Xác định phương thức thanh toán
  const paymentMethodDisplay =
    payment?.paymentMethod === 'VNPAY'
      ? 'Thẻ ATM nội địa (Cổng VNPAY Sandbox)'
      : payment?.paymentMethod === 'CASH'
        ? 'Tiền mặt tại quầy (Trực tiếp)'
        : payment?.paymentMethod || 'Tiền mặt tại quầy';

  const receiptNumber =
    invoice?.invoiceNumber ||
    (payment?.transactionReference
      ? `BL-${payment.transactionReference}`
      : `BL-${booking.bookingNumber}-${new Date().getFullYear()}`);

  const receiptDate = payment?.paidAt
    ? new Date(payment.paidAt).toLocaleString('vi-VN')
    : new Date().toLocaleString('vi-VN');

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 overflow-y-auto">
      {/* Backdrop (ẩn khi in) */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity no-print"
        onClick={onClose}
      />

      {/* Container (Có viền & shadow trên màn hình, khi in chuyển thành trang A4) */}
      <div className="relative w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-neutral-200 z-10 overflow-hidden my-auto flex flex-col max-h-[94vh] animate-in zoom-in-95 duration-200">
        {/* THANH CÔNG CỤ ĐIỀU KHIỂN (ẨN KHI IN) */}
        <div className="flex items-center justify-between px-6 py-3.5 bg-neutral-900 text-white border-b border-neutral-800 no-print">
          <div className="flex items-center gap-2.5">
            <ShieldCheck className="w-5 h-5 text-emerald-400" />
            <div>
              <h3 className="text-sm font-bold tracking-wide">Xem trước Biên lai Thu tiền Dịch vụ (Folio Receipt)</h3>
              <p className="text-[11px] text-neutral-400">Đầy đủ thông tin khách hàng, số phòng, chi tiết từng dịch vụ và thuế phí</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="primary"
              size="sm"
              onClick={handlePrint}
              leftIcon={<Printer className="w-4 h-4" />}
              className="bg-red-600 hover:bg-red-700 text-white font-bold h-9 px-4 rounded-xl shadow-xs"
            >
              In Biên lai Ngay
            </Button>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-xl transition-colors cursor-pointer"
              aria-label="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* NỘI DUNG TỜ BIÊN LAI (IN ẤN CHUẨN A4) */}
        <div className="p-8 sm:p-10 overflow-y-auto flex-1 bg-white text-neutral-900 font-sans hotel-receipt-printable">
          {/* HEADER BIÊN LAI KHÁCH SẠN */}
          <div className="border-b-2 border-neutral-900 pb-5 mb-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-black tracking-widest uppercase px-2 py-0.5 bg-red-600 text-white rounded">
                    HOTEL FOLIO
                  </span>
                  <span className="text-xs font-semibold text-neutral-500">HỆ THỐNG QUẢN LÝ KHÁCH SẠN TẬP TRUNG</span>
                </div>
                <h1 className="text-xl sm:text-2xl font-black text-neutral-900 mt-1 uppercase tracking-tight">
                  {booking.hotelName || 'Khách sạn UTC Grand Hotel & Resort'}
                </h1>
                <p className="text-xs text-neutral-600 mt-0.5">
                  Địa chỉ: Số 3 Phố Cầu Giấy, P. Láng Thượng, Q. Đống Đa, TP. Hà Nội
                </p>
                <p className="text-xs text-neutral-600">
                  Hotline Lễ tân: <strong className="font-semibold text-neutral-800">1900 6868</strong> • Email: <strong className="font-semibold text-neutral-800">reception@hotelbooking.utc.edu.vn</strong>
                </p>
              </div>

              <div className="sm:text-right border-t sm:border-t-0 pt-2 sm:pt-0 border-neutral-200 w-full sm:w-auto">
                <h2 className="text-base sm:text-lg font-black text-neutral-900 uppercase tracking-wider text-red-600">
                  PHIẾU THU &amp; BIÊN LAI DỊCH VỤ
                </h2>
                <p className="text-xs font-mono font-bold text-neutral-800 mt-0.5">
                  Số biên lai: <span className="text-neutral-900 bg-neutral-100 px-2 py-0.5 rounded">{receiptNumber}</span>
                </p>
                <p className="text-xs text-neutral-500 mt-0.5">
                  Mã đặt phòng: <strong className="font-mono text-neutral-900">{booking.bookingNumber}</strong>
                </p>
                <p className="text-[11px] text-neutral-500 mt-0.5">
                  Thời gian in: <span>{receiptDate}</span>
                </p>
              </div>
            </div>
          </div>

          {/* KHỐI THÔNG TIN KHÁCH HÀNG & LƯU TRÚ */}
          <div className="bg-neutral-50 rounded-2xl p-4 border border-neutral-200 mb-6">
            <div className="text-xs font-bold uppercase tracking-wider text-neutral-500 border-b border-neutral-200 pb-2 mb-3 flex items-center justify-between">
              <span>Thông tin Khách lưu trú &amp; Phòng nghỉ</span>
              <span className="text-[11px] font-semibold text-neutral-700">
                Trạng thái: <strong className="text-emerald-700">{booking.status}</strong>
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 text-xs">
              <div>
                <span className="text-neutral-500 block">Họ và tên khách hàng:</span>
                <strong className="text-sm font-bold text-neutral-900 block truncate">
                  {booking.guestName || 'Khách vãng lai'}
                </strong>
              </div>

              <div>
                <span className="text-neutral-500 block">Số điện thoại liên hệ:</span>
                <strong className="text-sm font-bold text-neutral-900 block">
                  {booking.guestPhone || 'Chưa cung cấp'}
                </strong>
              </div>

              <div>
                <span className="text-neutral-500 block">CCCD / Hộ chiếu (Passport):</span>
                <strong className="text-neutral-900 block font-semibold truncate" title={identityDisplay}>
                  {identityDisplay}
                </strong>
              </div>

              <div>
                <span className="text-neutral-500 block">Phòng lưu trú (Room No):</span>
                <div className="flex flex-wrap gap-1 mt-0.5">
                  {roomNumbers.length > 0 ? (
                    roomNumbers.map((rn, idx) => (
                      <span key={idx} className="font-bold text-neutral-900 bg-white px-2 py-0.5 rounded border border-neutral-300">
                        P.{rn}
                      </span>
                    ))
                  ) : (
                    <span className="font-medium text-neutral-600">Chưa xếp phòng</span>
                  )}
                </div>
              </div>

              <div>
                <span className="text-neutral-500 block">Hạng phòng (Room Type):</span>
                <strong className="text-neutral-800 block truncate">
                  {roomTypeNames.join(', ') || 'Tiêu chuẩn'}
                </strong>
              </div>

              <div>
                <span className="text-neutral-500 block">Ngày nhận phòng (Check-in):</span>
                <strong className="text-neutral-800 block">{checkInDate} (14:00)</strong>
              </div>

              <div>
                <span className="text-neutral-500 block">Ngày trả phòng (Check-out):</span>
                <strong className="text-neutral-800 block">{checkOutDate} (12:00)</strong>
              </div>

              <div>
                <span className="text-neutral-500 block">Thời gian lưu trú:</span>
                <strong className="text-neutral-900 block">{stayNights} đêm</strong>
              </div>
            </div>
          </div>

          {/* BẢNG KÊ CHI TIẾT TẤT CẢ DỊCH VỤ ĐƯỢC HIỂN THỊ THEO TỪNG PHÒNG */}
          <div className="mb-6 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-neutral-200 pb-2">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-neutral-800 flex items-center gap-1.5">
                  <Receipt className="w-4 h-4 text-red-600" />
                  Bảng kê chi tiết theo từng phòng ({roomGroups.length} nhóm phòng/khu vực • {serviceItems.length} hạng mục)
                </h3>
                <p className="text-[11px] text-neutral-500 mt-0.5">
                  Chi phí phòng và các dịch vụ ăn uống, phụ phí được phân bổ hiển thị chi tiết theo từng phòng.
                </p>
              </div>
              <span className="text-[11px] font-semibold text-emerald-800 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
                1 Hóa đơn Master Folio gom về khách đại diện
              </span>
            </div>

            {roomGroups.map((group) => (
              <div key={group.roomKey} className="border border-neutral-300 rounded-xl overflow-hidden shadow-2xs bg-white">
                <div className="bg-neutral-100/90 px-3.5 py-2 border-b border-neutral-300 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-bold font-mono text-xs px-2.5 py-0.5 rounded bg-neutral-900 text-white">
                      {group.roomNumber ? `PHÒNG ${group.roomNumber}` : group.roomKey}
                    </span>
                    {group.roomTypeName && (
                      <span className="text-xs font-semibold text-neutral-800">
                        • {group.roomTypeName}
                      </span>
                    )}
                    {group.guestNames && group.guestNames.length > 0 && (
                      <span className="text-[11px] text-neutral-600 font-medium">
                        (Khách: <strong className="text-neutral-900">{group.guestNames.join(', ')}</strong>)
                      </span>
                    )}
                  </div>
                  <div className="text-xs font-semibold text-neutral-600">
                    Cộng tiền phòng này: <strong className="text-red-600 font-bold">{formatCurrency(group.totalAmount)}</strong>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead className="bg-neutral-50 text-neutral-600 font-semibold border-b border-neutral-200 text-[11px]">
                      <tr>
                        <th className="p-2 w-10 text-center">STT</th>
                        <th className="p-2">Hạng mục / Dịch vụ</th>
                        <th className="p-2 text-center w-32">Phân loại</th>
                        <th className="p-2 text-center w-12">SL</th>
                        <th className="p-2 text-right w-24">Đơn giá</th>
                        <th className="p-2 text-center w-14">VAT</th>
                        <th className="p-2 text-right w-24">Tiền thuế</th>
                        <th className="p-2 text-right w-28">Thành tiền</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {group.items.map((item, idx) => (
                        <tr key={item.id} className="hover:bg-neutral-50/50">
                          <td className="p-2 text-center font-medium text-neutral-400">{idx + 1}</td>
                          <td className="p-2 font-medium text-neutral-900">{item.name}</td>
                          <td className="p-2 text-center">
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-neutral-100 text-neutral-700">
                              {item.category}
                            </span>
                          </td>
                          <td className="p-2 text-center font-bold text-neutral-800">{item.quantity}</td>
                          <td className="p-2 text-right text-neutral-600">{formatCurrency(item.unitPrice)}</td>
                          <td className="p-2 text-center text-neutral-500">{item.vatRate}%</td>
                          <td className="p-2 text-right text-neutral-500">{formatCurrency(item.vatAmount)}</td>
                          <td className="p-2 text-right font-bold text-neutral-900">{formatCurrency(item.totalAmount)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>

          {/* BẢNG TỔNG KẾT TÀI CHÍNH & PHƯƠNG THỨC THANH TOÁN */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
            {/* THÔNG TIN THANH TOÁN & ĐỐI SOÁT VNPAY / TIỀN MẶT */}
            <div className="border border-neutral-200 rounded-2xl p-4 bg-neutral-50 flex flex-col justify-between">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-neutral-600 block mb-2">
                  Xác nhận Hình thức &amp; Giao dịch Thanh toán
                </span>
                <div className="space-y-1.5 text-xs text-neutral-700">
                  <div className="flex justify-between">
                    <span>Hình thức:</span>
                    <strong className="text-neutral-900">{paymentMethodDisplay}</strong>
                  </div>
                  <div className="flex justify-between">
                    <span>Mã tham chiếu / Phiếu thu:</span>
                    <strong className="font-mono text-neutral-900">
                      {payment?.transactionReference || `PT-${booking.bookingNumber}`}
                    </strong>
                  </div>
                  <div className="flex justify-between">
                    <span>Thời gian giao dịch:</span>
                    <span className="font-medium text-neutral-800">{receiptDate}</span>
                  </div>
                  <div className="flex justify-between items-center pt-1 border-t border-neutral-200">
                    <span>Trạng thái thanh toán:</span>
                    <span className="inline-flex items-center gap-1 font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded text-[11px]">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      ĐÃ THANH TOÁN HOÀN TẤT
                    </span>
                  </div>
                </div>

                {/* THÔNG TIN ĐỐI SOÁT VNPAY SANDBOX NẾU CÓ */}
                {payment?.paymentMethod === 'VNPAY' && (
                  <div className="mt-3 p-2.5 bg-blue-50/80 border border-blue-200 rounded-xl text-[11px] space-y-1 text-blue-900">
                    <div className="font-bold flex justify-between text-blue-950">
                      <span>Đối chiếu Kỹ thuật VNPAY Gateway:</span>
                      <span className="bg-blue-200 text-blue-900 px-1.5 py-0.2 rounded font-semibold">NCB ATM</span>
                    </div>
                    <div className="grid grid-cols-2 gap-x-2 gap-y-0.5 text-neutral-700 pt-0.5">
                      <div>Ngân hàng: <strong>NCB (Ngân hàng Quốc Dân)</strong></div>
                      <div>Loại thẻ: <strong>Thẻ ATM Nội Địa Sandbox</strong></div>
                      <div>Mã GD VNPAY: <strong className="font-mono">{payment.transactionReference || 'VNPAY...'}</strong></div>
                      <div>Mã phản hồi: <strong className="font-mono text-emerald-700">00 (Thành công)</strong></div>
                    </div>
                  </div>
                )}
              </div>

              <div className="text-[11px] text-neutral-500 italic mt-3 pt-2 border-t border-neutral-200">
                Ghi chú: Khách hàng vui lòng giữ lại biên lai này để đối chiếu khi hoàn tất trả phòng hoặc xuất hóa đơn GTGT điện tử.
              </div>
            </div>

            {/* BẢNG TÍNH TIỀN QUYẾT TOÁN */}
            <div className="border border-neutral-200 rounded-2xl p-4 bg-white space-y-2 text-xs">
              <span className="text-xs font-bold uppercase tracking-wider text-neutral-700 block mb-1">
                Tổng hợp Chi phí &amp; Quyết toán
              </span>

              <div className="flex justify-between text-neutral-600">
                <span>Cộng tiền dịch vụ (Chưa thuế):</span>
                <strong className="text-neutral-900">{formatCurrency(baseSubtotal)}</strong>
              </div>

              <div className="flex justify-between text-neutral-600">
                <span>Phí dịch vụ khách sạn (Service Charge 5%):</span>
                <span>+{formatCurrency(serviceFeeAmount)}</span>
              </div>

              <div className="flex justify-between text-neutral-600">
                <span>Thuế giá trị gia tăng (VAT 8%):</span>
                <span>+{formatCurrency(totalVatAmount)}</span>
              </div>

              {voucherDiscount > 0 && (
                <div className="flex justify-between text-emerald-700 font-semibold bg-emerald-50 px-2 py-1 rounded border border-emerald-200">
                  <span>Giảm trừ Voucher ({appliedVoucher?.code || 'Khuyến mãi'}):</span>
                  <span>-{formatCurrency(voucherDiscount)}</span>
                </div>
              )}

              <div className="border-t border-neutral-200 pt-2 flex justify-between text-sm font-bold text-neutral-900">
                <span>TỔNG CỘNG TIỀN PHÁT SINH:</span>
                <span className="text-neutral-900 text-base">{formatCurrency(grandTotal)}</span>
              </div>

              <div className="flex justify-between text-sm font-bold text-emerald-700 bg-emerald-50/70 p-2 rounded-xl border border-emerald-200">
                <span>SỐ TIỀN THANH TOÁN KỲ NÀY:</span>
                <span className="text-base">{formatCurrency(paidThisTime)}</span>
              </div>

              <div className="flex justify-between text-xs font-semibold text-neutral-600 pt-1">
                <span>Số dư công nợ còn lại:</span>
                <span className={grandTotal - paidThisTime > 0 ? 'text-red-600 font-bold' : 'text-neutral-800'}>
                  {formatCurrency(Math.max(0, grandTotal - paidThisTime))}
                </span>
              </div>
            </div>
          </div>

          {/* CHỮ KÝ XÁC NHẬN HAI BÊN */}
          <div className="grid grid-cols-2 gap-8 text-center text-xs mt-10 pt-4 border-t border-neutral-300">
            <div>
              <p className="font-bold text-neutral-900 uppercase">Khách hàng / Người nộp tiền</p>
              <p className="text-[11px] text-neutral-500 italic">(Ký và ghi rõ họ tên)</p>
              <div className="h-20" />
              <p className="font-semibold text-neutral-800">{booking.guestName}</p>
            </div>

            <div>
              <p className="font-bold text-neutral-900 uppercase">Đại diện Khách sạn / Nhân viên Thu ngân</p>
              <p className="text-[11px] text-neutral-500 italic">(Ký, đóng dấu và ghi rõ họ tên)</p>
              <div className="h-20" />
              <p className="font-semibold text-neutral-800">Lễ tân Quầy Thu Ngân</p>
            </div>
          </div>

          {/* CHÂN TRANG CẢM ƠN */}
          <div className="text-center text-[11px] text-neutral-500 mt-10 pt-4 border-t border-neutral-200">
            <p className="font-semibold text-neutral-700">Cảm ơn Quý khách đã lựa chọn nghỉ dưỡng tại {booking.hotelName || 'Khách sạn UTC Grand Hotel'}!</p>
            <p className="mt-0.5">Mọi thắc mắc về hóa đơn xin vui lòng liên hệ Bộ phận Tài chính - Thu ngân qua hotline 1900 6868.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HotelFolioReceipt;
