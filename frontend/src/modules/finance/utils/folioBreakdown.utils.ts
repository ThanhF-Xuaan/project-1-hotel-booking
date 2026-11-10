import type { BookingResponse } from '../../booking/types/booking.types';
import type { InvoiceLineType, InvoiceResponse } from '../types/finance.types';

export interface FolioBreakdownItem {
  id: number | string;
  roomNumber?: string;
  roomTypeName?: string;
  name: string;
  category: string;
  quantity: number;
  unitPrice: number;
  serviceFeeAmount: number;
  vatRate: number;
  vatAmount: number;
  subtotal: number;
  totalAmount: number;
}

export interface RoomFolioBreakdownGroup {
  roomKey: string;
  roomNumber?: string;
  roomTypeName?: string;
  guestNames: string[];
  items: FolioBreakdownItem[];
  subtotal: number;
  serviceFeeAmount: number;
  vatAmount: number;
  totalAmount: number;
}

export interface FolioBreakdownResult {
  items: FolioBreakdownItem[];
  roomGroups: RoomFolioBreakdownGroup[];
  hasValidInvoice: boolean;
  baseSubtotal: number;
  serviceFeeAmount: number;
  totalVatAmount: number;
  grandTotalBeforeVoucher: number;
}

const mapLineCategory = (lineType: InvoiceLineType): string => {
  switch (lineType) {
    case 'ROOM_RATE':
      return 'Tiền phòng';
    case 'PRODUCT':
      return 'Ẩm thực / Minibar';
    case 'SERVICE':
      return 'Dịch vụ / Ăn uống';
    default:
      return 'Phụ phí';
  }
};

/** Chia một số tiền nguyên theo từng phần sao cho tổng các phần luôn khớp tuyệt đối */
const splitAmountAcross = (total: number, parts: number, index: number): number => {
  if (parts <= 1) return Math.round(total);
  const roundedTotal = Math.round(total);
  const base = Math.floor(roundedTotal / parts);
  if (index === parts - 1) {
    return roundedTotal - base * (parts - 1);
  }
  return base;
};

const escapeRegExp = (value: string): string => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const resolveRoomTokensFromDescription = (
  description: string,
  assignedRooms: string[]
): string[] => {
  const matchedKnown = assignedRooms.filter((rn) => {
    const pattern = new RegExp(`(^|[^A-Za-z0-9])${escapeRegExp(rn)}([^A-Za-z0-9]|$)`, 'i');
    return pattern.test(description);
  });
  if (matchedKnown.length > 0) {
    return matchedKnown;
  }

  const extracted: string[] = [];
  const regex = /(?:Phòng|P\.)\s*([A-Za-z0-9-]+)/gi;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(description)) !== null) {
    const token = match[1];
    if (!token) continue;
    const prefixMatches = assignedRooms.filter((rn) =>
      rn.toLowerCase().startsWith(token.toLowerCase())
    );
    const resolved = prefixMatches.length === 1 ? prefixMatches[0] : token;
    if (!extracted.includes(resolved)) {
      extracted.push(resolved);
    }
  }

  if (extracted.length > 0) {
    return extracted;
  }

  if (assignedRooms.length === 1) {
    return [assignedRooms[0]];
  }

  return [];
};

/**
 * Xây dựng bảng phân rã chi phí theo từng phòng từ Invoice hợp lệ hoặc Booking gốc,
 * đảm bảo không tính trùng thuế/phí lên netPrice và phân tách đúng các dòng gộp nhiều phòng.
 */
export function buildFolioBreakdown(
  booking: BookingResponse | null,
  invoice: InvoiceResponse | null
): FolioBreakdownResult {
  const hasValidInvoice = Boolean(
    invoice &&
      invoice.details &&
      invoice.details.length > 0 &&
      Number(invoice.grandTotal) > 0
  );

  if (!booking && !hasValidInvoice) {
    return {
      items: [],
      roomGroups: [],
      hasValidInvoice: false,
      baseSubtotal: 0,
      serviceFeeAmount: 0,
      totalVatAmount: 0,
      grandTotalBeforeVoucher: 0,
    };
  }

  const orderedRooms: string[] = [];
  const roomMeta = new Map<string, { roomTypeName?: string; guestNames: string[] }>();
  const rateIdToRoom = new Map<number, { roomNumber?: string; roomTypeName?: string }>();
  const chargeIdToRoom = new Map<number, { roomNumber?: string; roomTypeName?: string }>();
  const detailIdToRooms = new Map<number, { roomTypeName?: string; roomNumbers: string[] }>();

  booking?.bookingDetails?.forEach((detail) => {
    const detailRooms: string[] = [];
    (detail.bookingRooms || []).forEach((rm) => {
      if (rm.roomNumber) {
        if (!orderedRooms.includes(rm.roomNumber)) {
          orderedRooms.push(rm.roomNumber);
        }
        detailRooms.push(rm.roomNumber);
        const guestNames = (rm.bookingGuests || [])
          .map((g) => g.fullName || `${g.firstName || ''} ${g.lastName || ''}`.trim())
          .filter(Boolean);
        roomMeta.set(rm.roomNumber, {
          roomTypeName: detail.roomTypeName,
          guestNames,
        });
      }

      rm.dailyRates?.forEach((dr) => {
        if (dr.id !== undefined) {
          rateIdToRoom.set(dr.id, {
            roomNumber: rm.roomNumber,
            roomTypeName: detail.roomTypeName,
          });
        }
      });

      rm.charges?.forEach((ch) => {
        if (ch.id !== undefined) {
          chargeIdToRoom.set(ch.id, {
            roomNumber: rm.roomNumber,
            roomTypeName: detail.roomTypeName,
          });
        }
      });
    });

    if (detail.id !== undefined) {
      detailIdToRooms.set(detail.id, {
        roomTypeName: detail.roomTypeName,
        roomNumbers: detailRooms,
      });
    }
  });

  const items: FolioBreakdownItem[] = [];

  if (hasValidInvoice && invoice) {
    invoice.details.forEach((d, idx) => {
      const refId = d.referenceId;
      let targetRooms: string[] = [];
      let matchedRoomTypeName: string | undefined;

      if (d.lineType === 'ROOM_RATE' && refId !== undefined) {
        const byRate = rateIdToRoom.get(refId);
        if (byRate?.roomNumber) {
          targetRooms = [byRate.roomNumber];
          matchedRoomTypeName = byRate.roomTypeName;
        } else {
          const byDetail = detailIdToRooms.get(refId);
          if (byDetail && byDetail.roomNumbers.length > 0) {
            targetRooms = byDetail.roomNumbers;
            matchedRoomTypeName = byDetail.roomTypeName;
          }
        }
      } else if (
        (d.lineType === 'SURCHARGE' || d.lineType === 'PENALTY') &&
        refId !== undefined
      ) {
        const byCharge = chargeIdToRoom.get(refId);
        if (byCharge?.roomNumber) {
          targetRooms = [byCharge.roomNumber];
          matchedRoomTypeName = byCharge.roomTypeName;
        }
      }

      if (targetRooms.length === 0) {
        targetRooms = resolveRoomTokensFromDescription(d.description, orderedRooms);
      }

      const dSub = Number(d.subtotal || 0);
      const dFee = Number(d.serviceFeeAmount || 0);
      const dVat = Number(d.vatAmount || 0);
      const dTotal = Number(d.totalAmount || 0);
      const dQty = d.quantity || 1;
      const dVatRate = Number(d.vatRate ?? 0);

      if (targetRooms.length > 1) {
        const count = targetRooms.length;
        const canSplitQty = dQty >= count && dQty % count === 0;
        targetRooms.forEach((rn, rIdx) => {
          const splitQty = canSplitQty ? dQty / count : 1;
          const splitSub = splitAmountAcross(dSub, count, rIdx);
          const splitFee = splitAmountAcross(dFee, count, rIdx);
          const splitVat = splitAmountAcross(dVat, count, rIdx);
          const splitTotal = splitAmountAcross(dTotal, count, rIdx);

          items.push({
            id: `${d.id || idx + 1}-${rn}`,
            roomNumber: rn,
            roomTypeName: matchedRoomTypeName || roomMeta.get(rn)?.roomTypeName,
            name: `${d.description} — Phòng ${rn}`,
            category: mapLineCategory(d.lineType),
            quantity: splitQty,
            unitPrice:
              canSplitQty && splitQty > 0 ? Number(d.unitPrice) : splitSub,
            serviceFeeAmount: splitFee,
            vatRate: dVatRate,
            vatAmount: splitVat,
            subtotal: splitSub,
            totalAmount: splitTotal,
          });
        });
      } else {
        const rn = targetRooms[0];
        items.push({
          id: d.id || idx + 1,
          roomNumber: rn,
          roomTypeName: matchedRoomTypeName || (rn ? roomMeta.get(rn)?.roomTypeName : undefined),
          name: d.description,
          category: mapLineCategory(d.lineType),
          quantity: dQty,
          unitPrice: Number(d.unitPrice),
          serviceFeeAmount: dFee,
          vatRate: dVatRate,
          vatAmount: dVat,
          subtotal: dSub,
          totalAmount: dTotal,
        });
      }
    });
  } else if (booking) {
    let counter = 1;
    const details = booking.bookingDetails || [];
    const totalDetailsCount = details.length || 1;
    const bookingSub = Number(booking.subtotalAmount || 0);
    const bookingFee = Number(booking.serviceFeeAmount || 0);
    const bookingVat = Number(booking.totalVatAmount || 0);
    const bookingVatRate =
      bookingSub + bookingFee > 0 && bookingVat > 0
        ? Math.round((bookingVat / (bookingSub + bookingFee)) * 100)
        : 8;

    details.forEach((detail, dIdx) => {
      const roomTypeName = detail.roomTypeName || 'Phòng Khách sạn';
      const assignedRoomsInDetail = detail.bookingRooms || [];
      const detailSub = splitAmountAcross(bookingSub, totalDetailsCount, dIdx);
      const detailFee = splitAmountAcross(bookingFee, totalDetailsCount, dIdx);
      const detailVat = splitAmountAcross(bookingVat, totalDetailsCount, dIdx);

      if (assignedRoomsInDetail.length > 0) {
        const roomCount = assignedRoomsInDetail.length;
        assignedRoomsInDetail.forEach((rm, rIdx) => {
          const roomNum = rm.roomNumber;

          if (rm.dailyRates && rm.dailyRates.length > 0) {
            rm.dailyRates.forEach((dr) => {
              const hasBreakdown = dr.basePrice !== undefined && dr.basePrice !== null;
              const sub = hasBreakdown
                ? Math.max(
                    0,
                    Number(dr.basePrice) -
                      Number(dr.discountAmount || 0) +
                      Number(dr.surchargeAmount || 0)
                  )
                : Number(dr.netPrice) || 0;
              const fee = hasBreakdown ? Number(dr.serviceFeeAmount || 0) : 0;
              const vatRate = Number(dr.vatPercent ?? bookingVatRate);
              const vat =
                dr.vatAmount !== undefined && dr.vatAmount !== null
                  ? Number(dr.vatAmount)
                  : Math.round((sub + fee) * (vatRate / 100));
              const lineTotal =
                hasBreakdown && Number(dr.netPrice) > 0
                  ? Number(dr.netPrice)
                  : sub + fee + vat;

              items.push({
                id: counter++,
                roomNumber: roomNum,
                roomTypeName,
                name: `Tiền phòng: ${roomTypeName} (${dr.stayDate})`,
                category: 'Tiền phòng',
                quantity: 1,
                unitPrice: sub,
                serviceFeeAmount: fee,
                vatRate,
                vatAmount: vat,
                subtotal: sub,
                totalAmount: lineTotal,
              });
            });
          } else {
            const lineSub = splitAmountAcross(detailSub, roomCount, rIdx);
            const lineFee = splitAmountAcross(detailFee, roomCount, rIdx);
            const lineVat = splitAmountAcross(detailVat, roomCount, rIdx);

            items.push({
              id: counter++,
              roomNumber: roomNum,
              roomTypeName,
              name: `Tiền phòng: ${roomTypeName} (${detail.checkInDate} ➔ ${detail.checkOutDate})`,
              category: 'Tiền phòng',
              quantity: 1,
              unitPrice: lineSub,
              serviceFeeAmount: lineFee,
              vatRate: bookingVatRate,
              vatAmount: lineVat,
              subtotal: lineSub,
              totalAmount: lineSub + lineFee + lineVat,
            });
          }

          rm.charges?.forEach((ch) => {
            const chType = String(ch.chargeType);
            const cat =
              chType === 'RESTAURANT' || chType === 'MINIBAR'
                ? 'Ẩm thực / Minibar'
                : chType === 'LAUNDRY' || chType === 'SPA'
                  ? 'Dịch vụ phòng'
                  : 'Phụ phí';

            items.push({
              id: counter++,
              roomNumber: roomNum,
              roomTypeName,
              name: `${ch.itemName || 'Dịch vụ phòng'}${roomNum ? ` (P.${roomNum})` : ''}`,
              category: cat,
              quantity: ch.quantity || 1,
              unitPrice: Number(ch.unitPrice),
              serviceFeeAmount: Number(ch.serviceFeeAmount || 0),
              vatRate: Number(ch.vatRate ?? bookingVatRate),
              vatAmount: Number(ch.vatAmount || 0),
              subtotal: Number(ch.subtotal ?? ch.unitPrice),
              totalAmount: Number(ch.totalAmount),
            });
          });
        });
      } else {
        const qty = detail.quantity || 1;
        items.push({
          id: counter++,
          roomTypeName,
          name: `Tiền phòng: ${roomTypeName} (${detail.checkInDate} ➔ ${detail.checkOutDate})`,
          category: 'Tiền phòng',
          quantity: qty,
          unitPrice: qty > 0 ? Math.round(detailSub / qty) : detailSub,
          serviceFeeAmount: detailFee,
          vatRate: bookingVatRate,
          vatAmount: detailVat,
          subtotal: detailSub,
          totalAmount: detailSub + detailFee + detailVat,
        });
      }
    });

    if (items.length === 0) {
      const sub = bookingSub || Number(booking.totalAmount || 0);
      items.push({
        id: 1,
        name: 'Dịch vụ lưu trú khách sạn trọn gói',
        category: 'Lưu trú',
        quantity: 1,
        unitPrice: sub,
        serviceFeeAmount: bookingFee,
        vatRate: bookingVat > 0 ? bookingVatRate : 0,
        vatAmount: bookingVat,
        subtotal: sub,
        totalAmount: Number(booking.totalAmount || sub + bookingFee + bookingVat),
      });
    }
  }

  // Nhóm theo đúng thứ tự phòng vật lý trước, sau đó đến nhóm khác nếu có
  const roomGroupsMap = new Map<string, RoomFolioBreakdownGroup>();

  items.forEach((item) => {
    const rKey = item.roomNumber ? `Phòng ${item.roomNumber}` : 'Dịch vụ chung đoàn / Khác';
    if (!roomGroupsMap.has(rKey)) {
      const meta = item.roomNumber ? roomMeta.get(item.roomNumber) : undefined;
      roomGroupsMap.set(rKey, {
        roomKey: rKey,
        roomNumber: item.roomNumber,
        roomTypeName: item.roomTypeName || meta?.roomTypeName,
        guestNames: meta?.guestNames || [],
        items: [],
        subtotal: 0,
        serviceFeeAmount: 0,
        vatAmount: 0,
        totalAmount: 0,
      });
    }
    const group = roomGroupsMap.get(rKey)!;
    group.items.push(item);
    group.subtotal += item.subtotal;
    group.serviceFeeAmount += item.serviceFeeAmount;
    group.vatAmount += item.vatAmount;
    group.totalAmount += item.totalAmount;
  });

  const orderedGroups: RoomFolioBreakdownGroup[] = [];
  orderedRooms.forEach((rn) => {
    const g = roomGroupsMap.get(`Phòng ${rn}`);
    if (g) {
      orderedGroups.push(g);
      roomGroupsMap.delete(`Phòng ${rn}`);
    }
  });
  roomGroupsMap.forEach((g) => orderedGroups.push(g));

  const baseSubtotal = hasValidInvoice
    ? Number(invoice!.subTotal)
    : items.reduce((sum, it) => sum + it.subtotal, 0);
  const serviceFeeAmount = hasValidInvoice
    ? Number(invoice!.serviceFeeAmount)
    : items.reduce((sum, it) => sum + it.serviceFeeAmount, 0);
  const totalVatAmount = hasValidInvoice
    ? Number(invoice!.vatAmount)
    : items.reduce((sum, it) => sum + it.vatAmount, 0);
  const grandTotalBeforeVoucher = hasValidInvoice
    ? Number(invoice!.grandTotal)
    : baseSubtotal + serviceFeeAmount + totalVatAmount;

  return {
    items,
    roomGroups: orderedGroups,
    hasValidInvoice,
    baseSubtotal,
    serviceFeeAmount,
    totalVatAmount,
    grandTotalBeforeVoucher,
  };
}

