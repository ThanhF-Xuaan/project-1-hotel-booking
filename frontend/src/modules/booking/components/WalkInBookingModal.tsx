import React, { useState, useEffect, useId, useMemo } from 'react';
import {
  BedDouble,
  CreditCard,
  Plus,
  Trash2,
  CheckCircle2,
  AlertTriangle,
  Search,
  Sparkles,
  Coffee,
  Check,
  ChevronRight,
  ChevronLeft,
  Briefcase,
  UserCheck,
  ShieldCheck,
  Clock,
  Camera,
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import OcrIdentityScannerModal, {
  type ExtractedOcrResult,
} from '../../operation/components/OcrIdentityScannerModal';
import hotelService from '../../organization/services/hotel.service';
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import bookingService from '../services/booking.service';
import { apiClient } from '../../../core/api/client';
import { isValidVietnamesePhone, cleanPhoneNumber } from '../../../core/utils/phone';
import type { HotelDto } from '../../organization/types/organization.types';
import type { HotelRoomTypeDto, RoomInstanceDto } from '../../inventory/types/inventory.types';
import type {
  WalkInQuoteResponse,
  WalkInBookingRequest,
  BookingType,
  BookingChargeType,
  WalkInStayGuestRequest,
} from '../types/booking.types';
import Select, { type SelectOption } from '@/core/components/ui/Select';

// ============================================================================
// TYPES & CONSTANTS
// ============================================================================
const DEFAULT_TODAY = new Date().toISOString().split('T')[0];
const DEFAULT_TOMORROW = new Date(Date.now() + 86400000).toISOString().split('T')[0];
const getCurrentLocalTime = () => {
  const now = new Date();
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
};

export interface WalkInBookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (bookingNumber: string) => void;
}

interface RoomConfig {
  id: string;
  hotelRoomTypeId: number | '';
  roomInstanceId: number | '';
  adultCount: number;
  childCount: number;
  infantCount: number;
  extraBeds: number;
  stayGuests: WalkInStayGuestRequest[];
}

interface ConfirmedChargeItem {
  roomInstanceId?: number;
  chargeType: BookingChargeType;
  title: string;
  amount: number;
  suggestedAmount: number;
  action: 'APPLY' | 'WAIVE' | 'CUSTOM';
  waivedReason: string;
  ruleDescription?: string;
}

interface AddonServiceItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  roomIndex: number;
}

interface CompanySuggestionItem {
  id: number;
  name: string;
  taxCode?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
}

const INITIAL_GUEST: WalkInStayGuestRequest = {
  fullName: '',
  birthDate: '',
  guestType: 'ADULT',
  gender: 'MALE',
  identityType: 'CCCD',
  identityNumber: '',
  nationality: 'Việt Nam',
  isPrimaryGuest: true,
};

const DEFAULT_SERVICES: AddonServiceItem[] = [
  { id: 1, name: 'Ăn sáng Buffet Cao Cấp', price: 150000, quantity: 0, roomIndex: 0 },
  { id: 2, name: 'Gói Chào Mừng Minibar', price: 120000, quantity: 0, roomIndex: 0 },
  { id: 3, name: 'Đưa Đón Sân Bay (Xe 4 Chỗ)', price: 350000, quantity: 0, roomIndex: 0 },
  { id: 4, name: 'Voucher Spa Thư Giãn (60p)', price: 400000, quantity: 0, roomIndex: 0 },
];

// ============================================================================
// MAIN COMPONENT
// ============================================================================
export const WalkInBookingModal: React.FC<WalkInBookingModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [step, setStep] = useState<1 | 2 | 3 | 4 | 5>(1);

  // Master Data
  const [hotels, setHotels] = useState<HotelDto[]>([]);
  const [selectedHotelId, setSelectedHotelId] = useState<number>(1);
  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([]);
  const [readyRooms, setReadyRooms] = useState<RoomInstanceDto[]>([]);

  // Thời gian lưu trú
  const [stayPeriod, setStayPeriod] = useState({
    checkInDate: DEFAULT_TODAY,
    checkOutDate: DEFAULT_TOMORROW,
    actualCheckInTime: getCurrentLocalTime(),
  });

  // Cấu hình phòng
  const [rooms, setRooms] = useState<RoomConfig[]>([
    {
      id: 'room_1',
      hotelRoomTypeId: '',
      roomInstanceId: '',
      adultCount: 2,
      childCount: 0,
      infantCount: 0,
      extraBeds: 0,
      stayGuests: [{ ...INITIAL_GUEST }],
    },
  ]);

  // Bước 2: Doanh nghiệp & Khách chính
  const [bookingType, setBookingType] = useState<BookingType>('FIT');
  const [company, setCompany] = useState({
    id: undefined as number | undefined,
    name: '',
    taxCode: '',
    address: '',
    contactName: '',
    contactPhone: '',
  });
  const [companySuggestions, setCompanySuggestions] = useState<CompanySuggestionItem[]>([]);
  const [isSearchingCompany, setIsSearchingCompany] = useState(false);

  const [leadGuest, setLeadGuest] = useState({
    id: undefined as number | undefined,
    phone: '',
    fullName: '',
    email: '',
    identityType: 'CCCD',
    identityNumber: '',
    nationality: 'Việt Nam',
    dateOfBirth: '',
    gender: 'MALE' as 'MALE' | 'FEMALE' | 'OTHER',
    address: '',
  });
  const [isSearchingGuest, setIsSearchingGuest] = useState(false);
  const [guestFoundMessage, setGuestFoundMessage] = useState<string | null>(null);

  // OCR Modal
  const [isOcrModalOpen, setIsOcrModalOpen] = useState(false);
  const [ocrTarget, setOcrTarget] = useState<
    | { type: 'LEAD_GUEST' }
    | { type: 'STAY_GUEST'; roomIndex: number; guestIndex: number }
    | null
  >(null);

  // Bước 3: Báo giá & Phụ thu
  const [quoteResponse, setQuoteResponse] = useState<WalkInQuoteResponse | null>(null);
  const [isLoadingQuote, setIsLoadingQuote] = useState(false);
  const [confirmedCharges, setConfirmedCharges] = useState<ConfirmedChargeItem[]>([]);

  // Bước 4: Tiện ích / Dịch vụ
  const [selectedServices, setSelectedServices] = useState<AddonServiceItem[]>(DEFAULT_SERVICES);

  // Bước 5: Quyết toán
  const [payment, setPayment] = useState({
    method: 'CASH' as 'CASH' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'VNPAY',
    purpose: 'FULL_PAYMENT' as 'FULL_PAYMENT' | 'DEPOSIT',
    customAmount: 0,
    transactionRef: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // HTML IDs
  const checkInDateId = useId();
  const checkOutDateId = useId();
  const checkInTimeId = useId();

  // --------------------------------------------------------------------------
  // EFFECTS: Fetch Master Data
  // --------------------------------------------------------------------------
  useEffect(() => {
    if (isOpen) {
      Promise.resolve().then(() => {
        setStayPeriod((prev) => ({ ...prev, actualCheckInTime: getCurrentLocalTime() }));
      });
    }
  }, [isOpen]);

  useEffect(() => {
    hotelService
      .filter({ pageSize: 50, status: 'ACTIVE' })
      .then((res) => {
        if (res.result?.content?.length) {
          setHotels(res.result.content);
          setSelectedHotelId(res.result.content[0].id);
        }
      })
      .catch((err) => console.error('Lỗi tải danh sách khách sạn:', err));
  }, []);

  useEffect(() => {
    if (!selectedHotelId) return;

    Promise.all([
      hotelRoomTypeService.filter({ hotelId: selectedHotelId, pageSize: 50, status: 'ACTIVE' }),
      roomInstanceService.filter({ hotelId: selectedHotelId, currentStatus: 'READY', pageSize: 100 }),
    ])
      .then(([roomTypesRes, roomsRes]) => {
        if (roomTypesRes.result?.content) setRoomTypes(roomTypesRes.result.content);
        if (roomsRes.result?.content) setReadyRooms(roomsRes.result.content);
      })
      .catch((err) => console.error('Lỗi tải dữ liệu phòng:', err));
  }, [selectedHotelId]);

  // --------------------------------------------------------------------------
  // ROOM HANDLERS
  // --------------------------------------------------------------------------
  const handleAddRoom = () => {
    if (rooms.length >= 5) return;
    setRooms((prev) => [
      ...prev,
      {
        id: `room_${Date.now()}`,
        hotelRoomTypeId: '',
        roomInstanceId: '',
        adultCount: 2,
        childCount: 0,
        infantCount: 0,
        extraBeds: 0,
        stayGuests: [{ ...INITIAL_GUEST, isPrimaryGuest: false }],
      },
    ]);
  };

  const handleRemoveRoom = (index: number) => {
    if (rooms.length <= 1) return;
    setRooms((prev) => prev.filter((_, idx) => idx !== index));
  };

  const updateRoomField = <K extends keyof RoomConfig>(
    index: number,
    field: K,
    value: RoomConfig[K]
  ) => {
    setRooms((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      if (field === 'hotelRoomTypeId') {
        updated[index].roomInstanceId = '';
      }
      return updated;
    });
  };

  const updateStayGuest = (
    roomIndex: number,
    guestIndex: number,
    field: keyof WalkInStayGuestRequest,
    value: unknown
  ) => {
    setRooms((prev) => {
      const updated = [...prev];
      const guests = [...updated[roomIndex].stayGuests];
      guests[guestIndex] = { ...guests[guestIndex], [field]: value };
      updated[roomIndex] = { ...updated[roomIndex], stayGuests: guests };
      return updated;
    });
  };

  // --------------------------------------------------------------------------
  // SEARCH & AUTOCOMPLETE
  // --------------------------------------------------------------------------
  const handleSearchGuestByPhone = async () => {
    if (!leadGuest.phone.trim()) return;
    setIsSearchingGuest(true);
    setGuestFoundMessage(null);
    try {
      const res = await apiClient.get<Record<string, unknown>>(`/api/v1/guests/phone/${leadGuest.phone.trim()}`);
      if (res.result) {
        const result = res.result;
        const name = (result.fullName as string) || `${(result.lastName as string) || ''} ${(result.firstName as string) || ''}`.trim();
        setLeadGuest((prev) => ({
          ...prev,
          id: result.id as number | undefined,
          fullName: name || prev.fullName,
          email: (result.email as string) || prev.email,
          identityNumber: (result.identityNumber as string) || prev.identityNumber,
          identityType: (result.identityType as 'CCCD' | 'PASSPORT') || prev.identityType,
          nationality: (result.nationality as string) || prev.nationality,
        }));
        setGuestFoundMessage(`Đã tìm thấy khách: ${name || 'Khách quen'} (ID #${result.id})`);
      }
    } catch {
      setLeadGuest((prev) => ({ ...prev, id: undefined }));
      setGuestFoundMessage('Khách mới - Hệ thống sẽ tự động lưu hồ sơ.');
    } finally {
      setIsSearchingGuest(false);
    }
  };

  const handleSearchCompany = async (query: string) => {
    setCompany((prev) => ({ ...prev, name: query, id: undefined }));
    if (!query.trim() || query.trim().length < 2) {
      setCompanySuggestions([]);
      return;
    }
    setIsSearchingCompany(true);
    try {
      const res = await apiClient.post<{ content?: CompanySuggestionItem[] }>('/api/v1/companies/filter', {
        keyword: query.trim(),
        pageSize: 5,
      });
      if (res.result?.content) {
        setCompanySuggestions(
          res.result.content.map((item: CompanySuggestionItem) => ({
            id: item.id,
            name: item.name || '',
            taxCode: item.taxCode,
            address: item.address,
            contactName: item.contactName,
            contactPhone: item.contactPhone,
          }))
        );
      }
    } catch {
      setCompanySuggestions([]);
    } finally {
      setIsSearchingCompany(false);
    }
  };

  const handleSelectCompany = (comp: CompanySuggestionItem) => {
    setCompany({
      id: comp.id,
      name: comp.name,
      taxCode: comp.taxCode || '',
      address: comp.address || '',
      contactName: comp.contactName || '',
      contactPhone: comp.contactPhone || '',
    });
    setCompanySuggestions([]);
  };

  const copyLeadGuestToRoom1 = () => {
    if (!leadGuest.fullName) {
      alert('Vui lòng nhập họ tên người đặt trước.');
      return;
    }
    setRooms((prev) => {
      const next = [...prev];
      if (next[0]) {
        const guests = [...next[0].stayGuests];
        guests[0] = {
          ...guests[0],
          fullName: leadGuest.fullName,
          identityType: leadGuest.identityType,
          identityNumber: leadGuest.identityNumber,
          nationality: leadGuest.nationality,
          birthDate: leadGuest.dateOfBirth || guests[0].birthDate,
          gender: leadGuest.gender || guests[0].gender,
          isPrimaryGuest: true,
        };
        next[0].stayGuests = guests;
      }
      return next;
    });
  };

  // --------------------------------------------------------------------------
  // OCR HANDLERS
  // --------------------------------------------------------------------------
  const handleApplyOcrResult = (result: ExtractedOcrResult) => {
    if (!ocrTarget) return;

    if (ocrTarget.type === 'LEAD_GUEST') {
      setLeadGuest((prev) => ({
        ...prev,
        fullName: result.fullName,
        identityType: result.documentType,
        identityNumber: result.documentNumber,
        dateOfBirth: result.dateOfBirth,
        gender: result.gender,
        nationality: result.nationality || 'Việt Nam',
        address: result.permanentAddress,
      }));

      // Tự động đồng bộ khách chính sang phòng #1
      setRooms((prev) => {
        const next = [...prev];
        if (next[0]?.stayGuests?.length > 0) {
          const guests = [...next[0].stayGuests];
          guests[0] = {
            ...guests[0],
            fullName: result.fullName,
            identityType: result.documentType,
            identityNumber: result.documentNumber,
            birthDate: result.dateOfBirth,
            gender: result.gender,
            nationality: result.nationality || 'Việt Nam',
            isPrimaryGuest: true,
          };
          next[0] = { ...next[0], stayGuests: guests };
        }
        return next;
      });
    } else {
      const { roomIndex, guestIndex } = ocrTarget;
      setRooms((prev) => {
        const next = [...prev];
        if (next[roomIndex]?.stayGuests[guestIndex]) {
          const guests = [...next[roomIndex].stayGuests];
          guests[guestIndex] = {
            ...guests[guestIndex],
            fullName: result.fullName,
            identityType: result.documentType,
            identityNumber: result.documentNumber,
            birthDate: result.dateOfBirth,
            gender: result.gender,
            nationality: result.nationality || 'Việt Nam',
          };
          next[roomIndex] = { ...next[roomIndex], stayGuests: guests };
        }
        return next;
      });
    }
  };

  // --------------------------------------------------------------------------
  // CALCULATION & PRICING
  // --------------------------------------------------------------------------
  const fetchQuoteAndProceedToStep3 = async () => {
    for (let i = 0; i < rooms.length; i++) {
      if (!rooms[i].hotelRoomTypeId) return setFormError(`Phòng #${i + 1} chưa chọn hạng phòng.`);
      if (!rooms[i].roomInstanceId) return setFormError(`Phòng #${i + 1} chưa chọn số phòng vật lý.`);
    }
    if (!leadGuest.phone.trim() || !leadGuest.fullName.trim()) {
      return setFormError('Vui lòng nhập số điện thoại và họ tên người đặt.');
    }
    if (!isValidVietnamesePhone(leadGuest.phone.trim())) {
      return setFormError('Số điện thoại người đặt không đúng định dạng (VD: 0912345678 hoặc 02466668888).');
    }
    if (bookingType === 'CORPORATE') {
      if (!company.name.trim()) {
        return setFormError('Đơn Doanh nghiệp yêu cầu nhập Tên công ty.');
      }
      if (company.contactPhone.trim() && !isValidVietnamesePhone(company.contactPhone.trim())) {
        return setFormError('Số điện thoại người liên hệ doanh nghiệp không hợp lệ (VD: 02462556789 hoặc 0912345678).');
      }
    }

    setFormError(null);
    setIsLoadingQuote(true);
    try {
      const res = await bookingService.calculateWalkInQuote({
        hotelId: selectedHotelId,
        checkInDate: stayPeriod.checkInDate,
        checkOutDate: stayPeriod.checkOutDate,
        actualCheckInTime: stayPeriod.actualCheckInTime,
        rooms: rooms.map((r) => ({
          hotelRoomTypeId: Number(r.hotelRoomTypeId),
          adultCount: r.adultCount,
          childCount: r.childCount,
          infantCount: r.infantCount,
          extraBeds: r.extraBeds,
        })),
      });

      if (res.result) {
        setQuoteResponse(res.result);
        const detectedCharges: ConfirmedChargeItem[] = [];

        res.result.rooms.forEach((r, idx) => {
          const roomInstanceId = Number(rooms[idx]?.roomInstanceId);
          const roomNumber = readyRooms.find((rm) => rm.id === roomInstanceId)?.roomNumber || idx + 1;

          if (r.earlyCheckinProposal?.applicable) {
            detectedCharges.push({
              roomInstanceId,
              chargeType: 'EARLY_CHECKIN',
              title: `P.${roomNumber}: ${r.earlyCheckinProposal.suggestedItemName}`,
              amount: r.earlyCheckinProposal.suggestedAmount,
              suggestedAmount: r.earlyCheckinProposal.suggestedAmount,
              action: 'APPLY',
              waivedReason: '',
              ruleDescription: r.earlyCheckinProposal.policyRule,
            });
          }

          if (r.capacitySurcharge && r.capacitySurcharge.totalSurchargeAmount > 0) {
            detectedCharges.push({
              roomInstanceId,
              chargeType: 'OTHER',
              title: `P.${roomNumber}: Phụ thu vượt sức chứa / Giường phụ`,
              amount: r.capacitySurcharge.totalSurchargeAmount,
              suggestedAmount: r.capacitySurcharge.totalSurchargeAmount,
              action: 'APPLY',
              waivedReason: '',
              ruleDescription: r.capacitySurcharge.surchargeNotes?.join(', ') || 'Biểu phí hạng phòng',
            });
          }
        });

        setConfirmedCharges(detectedCharges);
        setStep(3);
      }
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Lỗi tính báo giá phòng và phụ thu.';
      setFormError(msg);
    } finally {
      setIsLoadingQuote(false);
    }
  };

  const totalRoomGross = quoteResponse?.totalAmount || 0;
  const totalApprovedCharges = useMemo(
    () =>
      confirmedCharges.reduce((acc, c) => {
        if (c.action === 'WAIVE') return acc;
        return acc + (c.action === 'CUSTOM' ? c.amount : c.suggestedAmount);
      }, 0),
    [confirmedCharges]
  );
  const totalServices = useMemo(
    () => selectedServices.reduce((acc, s) => acc + s.price * s.quantity, 0),
    [selectedServices]
  );
  const grandTotal = totalRoomGross + totalApprovedCharges + totalServices;
  const isSmallOrFit = rooms.length <= 2 && bookingType === 'FIT';

  useEffect(() => {
    if (step === 5) {
      Promise.resolve().then(() => {
        if (isSmallOrFit || payment.purpose === 'FULL_PAYMENT') {
          setPayment((prev) => ({ ...prev, purpose: 'FULL_PAYMENT', customAmount: grandTotal }));
        } else {
          setPayment((prev) => ({ ...prev, customAmount: Math.round(grandTotal * 0.5) }));
        }
      });
    }
  }, [step, grandTotal, isSmallOrFit, payment.purpose]);

  // --------------------------------------------------------------------------
  // SUBMISSION
  // --------------------------------------------------------------------------
  const handleCompleteWalkIn = async () => {
    setFormError(null);
    setIsSubmitting(true);
    try {
      const finalAmount = payment.customAmount > 0 ? payment.customAmount : grandTotal;
      if (isSmallOrFit && (payment.purpose !== 'FULL_PAYMENT' || finalAmount < grandTotal)) {
        setFormError('Khách vãng lai đặt ≤ 2 phòng bắt buộc thanh toán 100% khi nhận phòng.');
        setIsSubmitting(false);
        return;
      }

      const payload: WalkInBookingRequest = {
        hotelId: selectedHotelId,
        bookingType,
        companyId: company.id,
        newCompany:
          bookingType === 'CORPORATE' && !company.id && company.name.trim()
            ? {
              name: company.name,
              taxCode: company.taxCode,
              address: company.address,
              contactName: company.contactName,
              contactPhone: company.contactPhone ? cleanPhoneNumber(company.contactPhone) : undefined,
            }
            : undefined,
        guestId: leadGuest.id,
        newGuest: !leadGuest.id
          ? {
            fullName: leadGuest.fullName,
            phone: cleanPhoneNumber(leadGuest.phone),
            email: leadGuest.email,
            identityType: leadGuest.identityType,
            identityNumber: leadGuest.identityNumber,
            birthDate: leadGuest.dateOfBirth || undefined,
            nationality: leadGuest.nationality,
          }
          : undefined,
        checkInDate: stayPeriod.checkInDate,
        checkOutDate: stayPeriod.checkOutDate,
        notes: `Khách vãng lai Check-in quầy lúc ${stayPeriod.actualCheckInTime}.`,
        rooms: rooms.map((r) => ({
          hotelRoomTypeId: Number(r.hotelRoomTypeId),
          roomInstanceId: Number(r.roomInstanceId),
          adultCount: r.adultCount,
          childCount: r.childCount,
          infantCount: r.infantCount,
          extraBeds: r.extraBeds,
          stayGuests: r.stayGuests,
        })),
        confirmedCharges: confirmedCharges.map((c) => ({
          roomInstanceId: c.roomInstanceId,
          chargeType: c.chargeType,
          amount: c.action === 'WAIVE' ? 0 : c.action === 'CUSTOM' ? c.amount : c.suggestedAmount,
          description: c.title,
          isWaived: c.action === 'WAIVE',
          waivedReason: c.waivedReason,
        })),
        serviceItems: selectedServices
          .filter((s) => s.quantity > 0)
          .map((s) => ({
            roomInstanceId: Number(rooms[s.roomIndex]?.roomInstanceId || rooms[0]?.roomInstanceId),
            menuItemId: s.id,
            quantity: s.quantity,
          })),
        payment: {
          paymentMethod: payment.method,
          paymentPurpose: payment.purpose,
          amount: finalAmount,
          transactionReference: payment.transactionRef,
        },
      };

      const res = await bookingService.createWalkInBooking(payload);
      if (res.result) {
        onSuccess(res.result.bookingNumber);
        onClose();
      }
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Có lỗi xảy ra khi hoàn tất nhận khách vãng lai.';
      setFormError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const hotelSelectOptions: SelectOption<number>[] = useMemo(
    () => hotels.map((h) => ({ value: h.id, label: h.name })),
    [hotels]
  );

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Tiếp Nhận Khách Vãng Lai & Check-In Ngay (Walk-In)"
      description="Quy trình đón tiếp, xếp phòng vật lý READY, duyệt phụ thu tự động và chuyển thẳng sang IN-HOUSE"
      maxWidth="5xl"
    >
      <div className="space-y-6">
        {/* Stepper Progress Bar */}
        <div className="flex items-center justify-between border-b border-neutral-200 pb-4">
          {[
            { num: 1, label: 'Nhu Cầu & Phòng' },
            { num: 2, label: 'Khách & Doanh Nghiệp' },
            { num: 3, label: 'Phụ Thu Tự Động' },
            { num: 4, label: 'Dịch Vụ Đi Kèm' },
            { num: 5, label: 'Quyết Toán & Check-In' },
          ].map((item) => (
            <div
              key={item.num}
              className={`flex items-center gap-2 ${step === item.num
                ? 'text-red-600 font-bold'
                : step > item.num
                  ? 'text-neutral-900 font-medium'
                  : 'text-neutral-400'
                }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-all ${step === item.num
                  ? 'bg-red-600 text-white shadow-md'
                  : step > item.num
                    ? 'bg-green-600 text-white'
                    : 'bg-neutral-100 text-neutral-500'
                  }`}
              >
                {step > item.num ? <Check className="w-4 h-4" /> : item.num}
              </div>
              <span className="hidden md:inline text-xs">{item.label}</span>
              {item.num < 5 && <ChevronRight className="w-4 h-4 text-neutral-300 hidden md:inline ml-2" />}
            </div>
          ))}
        </div>

        {formError && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 shrink-0 text-red-600" />
            <span>{formError}</span>
          </div>
        )}

        {/* STEP 1: NHU CẦU & CẤU HÌNH PHÒNG */}
        {step === 1 && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 p-4 bg-neutral-50 rounded-2xl border border-neutral-200">
              <div>
                <label className="block text-xs font-semibold text-neutral-600 mb-1">Khách sạn cơ sở</label>
                <Select<number>
                  value={selectedHotelId}
                  onChange={(val) => setSelectedHotelId(val)}
                  options={hotelSelectOptions}
                  placeholder="Chọn khách sạn..."
                />
              </div>
              <div>
                <label htmlFor={checkInDateId} className="block text-xs font-semibold text-neutral-600 mb-1">
                  Ngày nhận phòng
                </label>
                <input
                  id={checkInDateId}
                  type="date"
                  value={stayPeriod.checkInDate}
                  min={DEFAULT_TODAY}
                  onChange={(e) => setStayPeriod((p) => ({ ...p, checkInDate: e.target.value }))}
                  className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                />
              </div>
              <div>
                <label htmlFor={checkOutDateId} className="block text-xs font-semibold text-neutral-600 mb-1">
                  Ngày trả phòng
                </label>
                <input
                  id={checkOutDateId}
                  type="date"
                  value={stayPeriod.checkOutDate}
                  min={stayPeriod.checkInDate || DEFAULT_TODAY}
                  onChange={(e) => setStayPeriod((p) => ({ ...p, checkOutDate: e.target.value }))}
                  className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                />
              </div>
              <div>
                <label htmlFor={checkInTimeId} className="block text-xs font-semibold text-neutral-600 mb-1">
                  Giờ đến thực tế
                </label>
                <div className="relative">
                  <input
                    id={checkInTimeId}
                    type="time"
                    value={stayPeriod.actualCheckInTime}
                    onChange={(e) => setStayPeriod((p) => ({ ...p, actualCheckInTime: e.target.value }))}
                    className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                  />
                  <Clock className="w-4 h-4 text-neutral-400 absolute right-3 top-2.5 pointer-events-none" />
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                    <BedDouble className="w-5 h-5 text-red-600" /> Cấu hình từng phòng đón khách
                  </h3>
                  <p className="text-xs text-neutral-500">Mỗi phòng gán trực tiếp số phòng vật lý sẵn sàng (READY).</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="px-3 py-1 bg-neutral-100 text-neutral-700 font-semibold text-xs rounded-full border border-neutral-200">
                    Đã chọn: {rooms.length} / 5 phòng
                  </span>
                  <Button
                    variant="secondary"
                    onClick={handleAddRoom}
                    disabled={rooms.length >= 5}
                    className="text-xs flex items-center gap-1.5"
                  >
                    <Plus className="w-4 h-4" /> Thêm phòng
                  </Button>
                </div>
              </div>

              {rooms.map((room, idx) => {
                const otherSelectedRoomInstanceIds = rooms
                  .filter((_, rIdx) => rIdx !== idx)
                  .map((r) => r.roomInstanceId);
                const availablePhysicalRooms = readyRooms.filter(
                  (r) =>
                    r.hotelRoomTypeId === Number(room.hotelRoomTypeId) &&
                    !otherSelectedRoomInstanceIds.includes(r.id)
                );

                return (
                  <div key={room.id} className="p-5 bg-white rounded-2xl border border-neutral-200 shadow-xs space-y-4">
                    <div className="flex items-center justify-between border-b border-neutral-100 pb-3">
                      <span className="text-sm font-bold text-red-600">Phòng #{idx + 1}</span>
                      {rooms.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveRoom(idx)}
                          className="text-neutral-400 hover:text-red-600 transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-semibold text-neutral-600 mb-1">
                          Hạng phòng <span className="text-red-500">*</span>
                        </label>
                        <Select<number>
                          value={room.hotelRoomTypeId ? Number(room.hotelRoomTypeId) : 0}
                          onChange={(val) =>
                            updateRoomField(idx, 'hotelRoomTypeId', val ? Number(val) : '')
                          }
                          options={[
                            { value: 0, label: 'Chọn loại phòng', disabled: true },
                            ...roomTypes.map((rt) => ({
                              value: rt.id,
                              label: `${rt.roomTypeName} (Chuẩn: ${rt.standardAdults} lớn, Tối đa: ${rt.maxTotalGuests || 4} khách)`,
                            })),
                          ]}
                          size="sm"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-semibold text-neutral-600 mb-1">
                          Số phòng vật lý sẵn sàng (READY) <span className="text-red-500">*</span>
                        </label>
                        <Select<number>
                          value={room.roomInstanceId ? Number(room.roomInstanceId) : 0}
                          disabled={!room.hotelRoomTypeId}
                          onChange={(val) =>
                            updateRoomField(idx, 'roomInstanceId', val ? Number(val) : '')
                          }
                          placeholder={
                            room.hotelRoomTypeId
                              ? availablePhysicalRooms.length > 0
                                ? `Chọn phòng (${availablePhysicalRooms.length} phòng sẵn sàng)`
                                : 'Không có phòng khả dụng'
                              : 'Vui lòng chọn loại phòng trước'
                          }
                          options={[
                            {
                              value: 0,
                              label: room.hotelRoomTypeId
                                ? availablePhysicalRooms.length > 0
                                  ? `Chọn phòng (${availablePhysicalRooms.length}`
                                  : 'Không có phòng khả dụng'
                                : 'Vui lòng chọn loại phòng trước',
                              disabled: true,
                            },
                            ...availablePhysicalRooms.map((pr) => ({
                              value: pr.id,
                              label: `Phòng ${pr.roomNumber} (Sạch sẽ, sẵn sàng)`,
                            })),
                          ]}
                          size="sm"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
                      <div>
                        <label className="block text-xs font-medium text-neutral-600 mb-1">Người lớn</label>
                        <input
                          type="number"
                          min="1"
                          max="10"
                          value={room.adultCount}
                          onChange={(e) => updateRoomField(idx, 'adultCount', parseInt(e.target.value) || 1)}
                          className="w-full px-3 py-1.5 border border-neutral-200 rounded-lg text-sm text-center font-semibold"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-medium text-neutral-600 mb-1">Trẻ em</label>
                        <input
                          type="number"
                          min="0"
                          max="5"
                          value={room.childCount}
                          onChange={(e) => updateRoomField(idx, 'childCount', parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-1.5 border border-neutral-200 rounded-lg text-sm text-center font-semibold"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-medium text-neutral-600 mb-1">Em bé (0-5t)</label>
                        <input
                          type="number"
                          min="0"
                          max="5"
                          value={room.infantCount}
                          onChange={(e) => updateRoomField(idx, 'infantCount', parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-1.5 border border-neutral-200 rounded-lg text-sm text-center font-semibold"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-medium text-neutral-600 mb-1">Giường phụ</label>
                        <input
                          type="number"
                          min="0"
                          max="2"
                          value={room.extraBeds}
                          onChange={(e) => updateRoomField(idx, 'extraBeds', parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-1.5 border border-neutral-200 rounded-lg text-sm text-center font-semibold"
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="flex justify-end pt-4 border-t border-neutral-200">
              <Button
                variant="primary"
                onClick={() => setStep(2)}
                className="bg-red-600 hover:bg-red-700 text-white font-semibold flex items-center gap-2"
              >
                Tiếp tục: Khai báo khách <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}

        {/* STEP 2: KHAI BÁO KHÁCH & DOANH NGHIỆP */}
        {step === 2 && (
          <div className="space-y-6">
            <div className="flex items-center gap-4 p-3 bg-neutral-50 rounded-2xl border border-neutral-200">
              <span className="text-xs font-bold text-neutral-600">Loại khách:</span>
              <button
                type="button"
                onClick={() => setBookingType('FIT')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${bookingType === 'FIT'
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'bg-white text-neutral-700 hover:bg-neutral-100 border border-neutral-200'
                  }`}
              >
                Khách Cá Nhân (FIT)
              </button>
              <button
                type="button"
                onClick={() => setBookingType('CORPORATE')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${bookingType === 'CORPORATE'
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'bg-white text-neutral-700 hover:bg-neutral-100 border border-neutral-200'
                  }`}
              >
                Doanh Nghiệp (CORPORATE)
              </button>
            </div>

            {bookingType === 'CORPORATE' && (
              <div className="p-4 bg-blue-50/50 rounded-2xl border border-blue-200 space-y-4">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-bold text-blue-900 flex items-center gap-2">
                    <Briefcase className="w-4 h-4 text-blue-600" /> Doanh nghiệp bảo trợ
                  </h4>
                  {company.id && (
                    <span className="text-xs bg-blue-100 text-blue-800 font-bold px-2.5 py-1 rounded-full border border-blue-300">
                      Đã chọn đối tác #{company.id}
                    </span>
                  )}
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  <div className="relative">
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">
                      Tên công ty <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      placeholder="Gõ tìm hoặc nhập mới..."
                      value={company.name}
                      onChange={(e) => handleSearchCompany(e.target.value)}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-blue-500"
                    />
                    {isSearchingCompany && (
                      <span className="absolute right-3 top-8 text-xs text-neutral-400">Đang tìm...</span>
                    )}
                    {companySuggestions.length > 0 && (
                      <div className="absolute z-20 left-0 right-0 mt-1 bg-white border border-neutral-200 rounded-xl shadow-lg max-h-48 overflow-y-auto">
                        {companySuggestions.map((c) => (
                          <div
                            key={c.id}
                            onClick={() => handleSelectCompany(c)}
                            className="p-2.5 hover:bg-blue-50 cursor-pointer border-b border-neutral-100 last:border-b-0 text-left"
                          >
                            <div className="font-semibold text-xs text-neutral-900">{c.name}</div>
                            <div className="text-[11px] text-neutral-500">MST: {c.taxCode || 'N/A'}</div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">Mã số thuế</label>
                    <input
                      type="text"
                      placeholder="0101234567"
                      value={company.taxCode}
                      onChange={(e) => setCompany((p) => ({ ...p, taxCode: e.target.value }))}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">Địa chỉ xuất hóa đơn</label>
                    <input
                      type="text"
                      placeholder="Hà Nội, Việt Nam"
                      value={company.address}
                      onChange={(e) => setCompany((p) => ({ ...p, address: e.target.value }))}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Thông tin Lead Guest */}
            <div className="p-5 bg-white rounded-2xl border border-neutral-200 shadow-xs space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-neutral-100 pb-2 gap-2">
                <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                  <UserCheck className="w-5 h-5 text-red-600" /> Thông tin khách chính (Lead Guest)
                </h4>
                <div className="flex items-center gap-2">
                  {guestFoundMessage && (
                    <span className="text-xs text-green-700 font-medium bg-green-50 px-2.5 py-1 rounded-full border border-green-200">
                      {guestFoundMessage}
                    </span>
                  )}
                  <button
                    type="button"
                    onClick={() => {
                      setOcrTarget({ type: 'LEAD_GUEST' });
                      setIsOcrModalOpen(true);
                    }}
                    className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5"
                  >
                    <Camera className="w-4 h-4" /> Quét CCCD / Passport
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Số điện thoại <span className="text-red-500">*</span>
                  </label>
                  <div className="flex gap-1.5">
                    <input
                      type="text"
                      placeholder="0912345678"
                      value={leadGuest.phone}
                      onChange={(e) => setLeadGuest((p) => ({ ...p, phone: e.target.value }))}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm font-medium"
                    />
                    <button
                      type="button"
                      onClick={handleSearchGuestByPhone}
                      disabled={isSearchingGuest}
                      className="px-3 bg-neutral-100 hover:bg-neutral-200 rounded-xl text-xs font-semibold"
                    >
                      <Search className="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Họ và tên <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={leadGuest.fullName}
                    onChange={(e) => setLeadGuest((p) => ({ ...p, fullName: e.target.value.toUpperCase() }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm font-bold"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Số CCCD / Hộ chiếu</label>
                  <input
                    type="text"
                    value={leadGuest.identityNumber}
                    onChange={(e) => setLeadGuest((p) => ({ ...p, identityNumber: e.target.value }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Email</label>
                  <input
                    type="email"
                    value={leadGuest.email}
                    onChange={(e) => setLeadGuest((p) => ({ ...p, email: e.target.value }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-3 pt-1">
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Ngày sinh</label>
                  <input
                    type="date"
                    value={leadGuest.dateOfBirth}
                    onChange={(e) => setLeadGuest((p) => ({ ...p, dateOfBirth: e.target.value }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Giới tính</label>
                  <Select<'MALE' | 'FEMALE' | 'OTHER'>
                    value={leadGuest.gender}
                    onChange={(val) => setLeadGuest((p) => ({ ...p, gender: val }))}
                    options={[
                      { value: 'MALE', label: 'Nam' },
                      { value: 'FEMALE', label: 'Nữ' },
                      { value: 'OTHER', label: 'Khác' },
                    ]}
                    size="sm"
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Địa chỉ thường trú</label>
                  <input
                    type="text"
                    value={leadGuest.address}
                    onChange={(e) => setLeadGuest((p) => ({ ...p, address: e.target.value }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm"
                  />
                </div>
              </div>
            </div>

            {/* Stay Guests */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-neutral-700" /> Khách thực tế lưu trú (Khai báo tạm trú BCA)
                </h4>
                <button
                  type="button"
                  onClick={copyLeadGuestToRoom1}
                  className="px-3 py-1.5 bg-red-50 text-red-600 hover:bg-red-100 rounded-xl text-xs font-bold"
                >
                  ⚡ Gán người đặt vào Phòng #1
                </button>
              </div>

              {rooms.map((room, rIdx) => {
                const roomNum = readyRooms.find((r) => r.id === room.roomInstanceId)?.roomNumber;
                return (
                  <div key={room.id} className="p-4 bg-neutral-50 rounded-2xl border border-neutral-200 space-y-3">
                    <div className="flex items-center justify-between border-b border-neutral-200 pb-2">
                      <span className="text-xs font-bold text-neutral-900">
                        Phòng #{rIdx + 1} {roomNum && `(P.${roomNum})`} — {room.adultCount} lớn, {room.childCount} trẻ
                      </span>
                    </div>

                    {room.stayGuests.map((guest, gIdx) => (
                      <div key={gIdx} className="grid grid-cols-1 sm:grid-cols-6 gap-2 items-center bg-white p-2.5 rounded-xl border border-neutral-200">
                        <div className="sm:col-span-2">
                          <input
                            type="text"
                            placeholder="Họ và tên khách"
                            value={guest.fullName}
                            onChange={(e) => updateStayGuest(rIdx, gIdx, 'fullName', e.target.value.toUpperCase())}
                            className="w-full px-2.5 py-1.5 border border-neutral-200 rounded-lg text-xs font-semibold"
                          />
                        </div>
                        <div>
                          <input
                            type="text"
                            placeholder="CCCD / Hộ chiếu"
                            value={guest.identityNumber || ''}
                            onChange={(e) => updateStayGuest(rIdx, gIdx, 'identityNumber', e.target.value)}
                            className="w-full px-2.5 py-1.5 border border-neutral-200 rounded-lg text-xs"
                          />
                        </div>
                        <div>
                          <Select<'MALE' | 'FEMALE' | 'OTHER'>
                            value={(guest.gender as 'MALE' | 'FEMALE' | 'OTHER') || 'MALE'}
                            onChange={(val) => updateStayGuest(rIdx, gIdx, 'gender', val)}
                            options={[
                              { value: 'MALE', label: 'Nam' },
                              { value: 'FEMALE', label: 'Nữ' },
                              { value: 'OTHER', label: 'Khác' },
                            ]}
                            size="sm"
                          />
                        </div>
                        <div>
                          <input
                            type="date"
                            value={guest.birthDate || ''}
                            onChange={(e) => updateStayGuest(rIdx, gIdx, 'birthDate', e.target.value)}
                            className="w-full px-2.5 py-1.5 border border-neutral-200 rounded-lg text-xs"
                          />
                        </div>
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => {
                              setOcrTarget({ type: 'STAY_GUEST', roomIndex: rIdx, guestIndex: gIdx });
                              setIsOcrModalOpen(true);
                            }}
                            className="text-neutral-500 hover:text-red-600 p-1.5"
                          >
                            <Camera className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>

            <div className="flex justify-between pt-4 border-t border-neutral-200">
              <Button variant="secondary" onClick={() => setStep(1)} className="flex items-center gap-1.5">
                <ChevronLeft className="w-4 h-4" /> Quay lại
              </Button>
              <Button
                variant="primary"
                onClick={fetchQuoteAndProceedToStep3}
                isLoading={isLoadingQuote}
                className="bg-red-600 hover:bg-red-700 text-white font-semibold flex items-center gap-2"
              >
                Tiếp tục: Duyệt phụ thu <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}

        {/* STEP 3: PHỤ THU TỰ ĐỘNG */}
        {step === 3 && (
          <div className="space-y-6">
            <div className="p-4 bg-neutral-50 rounded-2xl border border-neutral-200 flex items-center justify-between">
              <div>
                <span className="text-xs font-semibold text-neutral-500 uppercase">Tiền phòng tạm tính:</span>
                <div className="text-xl font-extrabold text-neutral-900 mt-0.5">
                  {totalRoomGross.toLocaleString('vi-VN')} đ
                </div>
              </div>
              <div className="text-right">
                <span className="text-xs font-medium text-neutral-500">Giờ đến:</span>
                <div className="text-sm font-bold text-red-600 flex items-center gap-1">
                  <Clock className="w-4 h-4" /> {stayPeriod.actualCheckInTime}
                </div>
              </div>
            </div>

            <div className="space-y-3">
              <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-amber-500" /> Phụ thu tự động phát hiện
              </h4>

              {confirmedCharges.length === 0 ? (
                <div className="p-6 bg-white rounded-2xl border border-dashed border-neutral-300 text-center text-neutral-500 text-sm">
                  Không phát sinh phụ thu.
                </div>
              ) : (
                confirmedCharges.map((charge, cIdx) => (
                  <div key={cIdx} className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs space-y-3">
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="text-sm font-bold text-neutral-900">{charge.title}</div>
                        {charge.ruleDescription && (
                          <div className="text-xs text-neutral-500 mt-0.5">{charge.ruleDescription}</div>
                        )}
                      </div>
                      <div className="text-right">
                        <span className="text-xs text-neutral-400">Đề xuất:</span>
                        <div className="text-sm font-bold text-neutral-700">
                          {charge.suggestedAmount.toLocaleString('vi-VN')} đ
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-neutral-100">
                      {(['APPLY', 'WAIVE', 'CUSTOM'] as const).map((action) => (
                        <button
                          key={action}
                          type="button"
                          onClick={() => {
                            const updated = [...confirmedCharges];
                            updated[cIdx].action = action;
                            setConfirmedCharges(updated);
                          }}
                          className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${charge.action === action
                            ? action === 'APPLY'
                              ? 'bg-blue-600 text-white'
                              : action === 'WAIVE'
                                ? 'bg-green-600 text-white'
                                : 'bg-purple-600 text-white'
                            : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                            }`}
                        >
                          {action === 'APPLY' && `✓ Áp dụng (${charge.suggestedAmount.toLocaleString('vi-VN')} đ)`}
                          {action === 'WAIVE' && 'Miễn phí (0đ)'}
                          {action === 'CUSTOM' && 'Điều chỉnh giá'}
                        </button>
                      ))}

                      {charge.action === 'WAIVE' && (
                        <input
                          type="text"
                          placeholder="Lý do miễn phụ thu..."
                          value={charge.waivedReason}
                          onChange={(e) => {
                            const updated = [...confirmedCharges];
                            updated[cIdx].waivedReason = e.target.value;
                            setConfirmedCharges(updated);
                          }}
                          className="w-full mt-2 px-3 py-1.5 border border-green-300 rounded-xl text-xs bg-green-50/50"
                        />
                      )}

                      {charge.action === 'CUSTOM' && (
                        <div className="w-full mt-2 flex items-center gap-2">
                          <input
                            type="number"
                            min="0"
                            step="10000"
                            value={charge.amount}
                            onChange={(e) => {
                              const updated = [...confirmedCharges];
                              updated[cIdx].amount = parseFloat(e.target.value) || 0;
                              setConfirmedCharges(updated);
                            }}
                            className="px-3 py-1.5 border border-purple-300 rounded-xl text-xs font-bold bg-purple-50/50 w-44"
                          />
                          <span className="text-xs text-neutral-500">VNĐ thực thu</span>
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>

            <div className="flex justify-between pt-4 border-t border-neutral-200">
              <Button variant="secondary" onClick={() => setStep(2)} className="flex items-center gap-1.5">
                <ChevronLeft className="w-4 h-4" /> Quay lại
              </Button>
              <Button
                variant="primary"
                onClick={() => setStep(4)}
                className="bg-red-600 hover:bg-red-700 text-white font-semibold flex items-center gap-2"
              >
                Tiếp tục: Dịch vụ thêm <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}

        {/* STEP 4: DỊCH VỤ ĐI KÈM */}
        {step === 4 && (
          <div className="space-y-6">
            <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
              <Coffee className="w-4 h-4 text-red-600" /> Dịch vụ đón tiếp và tiện ích
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {selectedServices.map((svc) => (
                <div key={svc.id} className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs flex items-center justify-between">
                  <div>
                    <div className="text-sm font-bold text-neutral-900">{svc.name}</div>
                    <div className="text-xs font-semibold text-red-600 mt-0.5">
                      {svc.price.toLocaleString('vi-VN')} đ / gói
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() =>
                        setSelectedServices((prev) =>
                          prev.map((s) => (s.id === svc.id ? { ...s, quantity: Math.max(0, s.quantity - 1) } : s))
                        )
                      }
                      className="w-8 h-8 rounded-lg bg-neutral-100 hover:bg-neutral-200 flex items-center justify-center font-bold"
                    >
                      -
                    </button>
                    <span className="w-8 text-center text-sm font-bold">{svc.quantity}</span>
                    <button
                      type="button"
                      onClick={() =>
                        setSelectedServices((prev) =>
                          prev.map((s) => (s.id === svc.id ? { ...s, quantity: s.quantity + 1 } : s))
                        )
                      }
                      className="w-8 h-8 rounded-lg bg-red-50 hover:bg-red-100 flex items-center justify-center font-bold text-red-600"
                    >
                      +
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex justify-between pt-4 border-t border-neutral-200">
              <Button variant="secondary" onClick={() => setStep(3)} className="flex items-center gap-1.5">
                <ChevronLeft className="w-4 h-4" /> Quay lại
              </Button>
              <Button
                variant="primary"
                onClick={() => setStep(5)}
                className="bg-red-600 hover:bg-red-700 text-white font-semibold flex items-center gap-2"
              >
                Tiếp tục: Thanh toán <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}

        {/* STEP 5: QUYẾT TOÁN & THANH TOÁN */}
        {step === 5 && (
          <div className="space-y-6">
            <div className="p-5 bg-neutral-50 rounded-2xl border border-neutral-200 space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-neutral-600">Tiền phòng ({rooms.length} phòng):</span>
                <span className="font-bold">{totalRoomGross.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-neutral-600">Phụ thu:</span>
                <span className="font-bold">{totalApprovedCharges.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-neutral-600">Dịch vụ đi kèm:</span>
                <span className="font-bold">{totalServices.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="border-t border-neutral-200 pt-3 flex justify-between items-center">
                <span className="text-base font-extrabold text-neutral-900">TỔNG CẦN THANH TOÁN:</span>
                <span className="text-2xl font-black text-red-600">{grandTotal.toLocaleString('vi-VN')} đ</span>
              </div>
            </div>

            <div className="p-4 bg-white rounded-2xl border border-neutral-200 space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                  <CreditCard className="w-5 h-5 text-red-600" /> Phương thức thanh toán
                </h4>
                {isSmallOrFit && (
                  <span className="text-xs text-red-700 bg-red-50 border border-red-200 px-3 py-1 rounded-full font-bold">
                    ⚠️ Bắt buộc thu 100%
                  </span>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setPayment((p) => ({ ...p, purpose: 'FULL_PAYMENT', customAmount: grandTotal }))}
                  className={`p-3 rounded-xl border text-left ${payment.purpose === 'FULL_PAYMENT' ? 'border-red-600 bg-red-50/50 shadow-xs' : 'border-neutral-200'
                    }`}
                >
                  <div className="text-xs font-bold text-neutral-900">Thanh toán 100%</div>
                  <div className="text-sm font-extrabold text-red-600 mt-1">{grandTotal.toLocaleString('vi-VN')} đ</div>
                </button>

                <button
                  type="button"
                  disabled={isSmallOrFit}
                  onClick={() =>
                    setPayment((p) => ({ ...p, purpose: 'DEPOSIT', customAmount: Math.round(grandTotal * 0.5) }))
                  }
                  className={`p-3 rounded-xl border text-left ${isSmallOrFit
                    ? 'opacity-40 cursor-not-allowed bg-neutral-100'
                    : payment.purpose === 'DEPOSIT'
                      ? 'border-red-600 bg-red-50/50'
                      : 'border-neutral-200'
                    }`}
                >
                  <div className="text-xs font-bold text-neutral-900">Đặt cọc (50%)</div>
                  <div className="text-sm font-extrabold text-neutral-700 mt-1">
                    {(grandTotal * 0.5).toLocaleString('vi-VN')} đ
                  </div>
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Số tiền thực thu (VNĐ)</label>
                  <input
                    type="number"
                    min="0"
                    disabled={isSmallOrFit}
                    value={payment.customAmount}
                    onChange={(e) => setPayment((p) => ({ ...p, customAmount: parseFloat(e.target.value) || 0 }))}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-base font-black text-red-600 disabled:bg-neutral-100"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Hình thức thanh toán</label>
                  <Select<'CASH' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'VNPAY'>
                    value={payment.method}
                    onChange={(val) => setPayment((p) => ({ ...p, method: val }))}
                    options={[
                      { value: 'CASH', label: 'Tiền mặt tại quầy (CASH)' },
                      { value: 'CREDIT_CARD', label: 'Quẹt thẻ POS (CREDIT_CARD)' },
                      { value: 'BANK_TRANSFER', label: 'Chuyển khoản (BANK_TRANSFER)' },
                      { value: 'VNPAY', label: 'VNPAY-QR' },
                    ]}
                    size="sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">Mã tham chiếu / Số biên lai</label>
                <input
                  type="text"
                  placeholder="VD: POS-998822..."
                  value={payment.transactionRef}
                  onChange={(e) => setPayment((p) => ({ ...p, transactionRef: e.target.value }))}
                  className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm"
                />
              </div>
            </div>

            <div className="flex justify-between pt-4 border-t border-neutral-200">
              <Button variant="secondary" onClick={() => setStep(4)} className="flex items-center gap-1.5">
                <ChevronLeft className="w-4 h-4" /> Quay lại
              </Button>
              <Button
                variant="primary"
                onClick={handleCompleteWalkIn}
                isLoading={isSubmitting}
                className="bg-red-600 hover:bg-red-700 text-white font-extrabold text-sm px-6 py-2.5 rounded-xl shadow-md flex items-center gap-2"
              >
                <CheckCircle2 className="w-5 h-5" /> Hoàn tất Check-in (IN-HOUSE)
              </Button>
            </div>
          </div>
        )}
      </div>

      <OcrIdentityScannerModal
        isOpen={isOcrModalOpen}
        onClose={() => {
          setIsOcrModalOpen(false);
          setOcrTarget(null);
        }}
        onApply={handleApplyOcrResult}
        title={
          ocrTarget?.type === 'LEAD_GUEST'
            ? 'Quét CCCD / Hộ chiếu - Khách chính'
            : ocrTarget?.type === 'STAY_GUEST'
              ? `Quét CCCD / Hộ chiếu - Phòng #${ocrTarget.roomIndex + 1}`
              : 'Quét & Nhận diện CCCD / Hộ chiếu (OCR)'
        }
        targetDescription={
          ocrTarget?.type === 'LEAD_GUEST'
            ? 'Trích xuất tự động thông tin khách chính đại diện nhận phòng.'
            : 'Trích xuất thông tin người lưu trú để đồng bộ khai báo tạm trú BCA.'
        }
      />
    </Modal>
  );
};

export default WalkInBookingModal;