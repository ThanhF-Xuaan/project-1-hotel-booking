import React, { useState, useEffect, useId } from 'react';
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
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import hotelService from '../../organization/services/hotel.service';
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import bookingService from '../services/booking.service';
import { apiClient } from '../../../core/api/client';
import type { HotelDto } from '../../organization/types/organization.types';
import type { HotelRoomTypeDto, RoomInstanceDto } from '../../inventory/types/inventory.types';
import type {
  WalkInQuoteResponse,
  WalkInBookingRequest,
  BookingType,
  BookingChargeType,
  WalkInStayGuestRequest,
} from '../types/booking.types';

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

export const WalkInBookingModal: React.FC<WalkInBookingModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [step, setStep] = useState<1 | 2 | 3 | 4 | 5>(1);

  // Khách sạn & Danh mục phòng
  const [hotels, setHotels] = useState<HotelDto[]>([]);
  const [selectedHotelId, setSelectedHotelId] = useState<number>(1);
  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([]);
  const [readyRooms, setReadyRooms] = useState<RoomInstanceDto[]>([]);

  // Ngày lưu trú
  const today = new Date().toISOString().split('T')[0];
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
  const [checkInDate, setCheckInDate] = useState(today);
  const [checkOutDate, setCheckOutDate] = useState(tomorrow);
  const [actualCheckInTime, setActualCheckInTime] = useState(
    new Date().toTimeString().slice(0, 5)
  );

  // Cấu hình phòng (Tối đa 5 phòng)
  const [rooms, setRooms] = useState<RoomConfig[]>([
    {
      id: 'room_1',
      hotelRoomTypeId: '',
      roomInstanceId: '',
      adultCount: 2,
      childCount: 0,
      infantCount: 0,
      extraBeds: 0,
      stayGuests: [
        {
          fullName: '',
          birthDate: '',
          guestType: 'ADULT',
          gender: 'MALE',
          identityType: 'CCCD',
          identityNumber: '',
          nationality: 'Việt Nam',
          isPrimaryGuest: true,
        },
      ],
    },
  ]);

  // Bước 2: Thông tin Doanh nghiệp & Khách đặt
  const [bookingType, setBookingType] = useState<BookingType>('FIT');
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | undefined>(undefined);
  const [companyName, setCompanyName] = useState('');
  const [taxCode, setTaxCode] = useState('');
  const [companyAddress, setCompanyAddress] = useState('');
  const [companyContactName, setCompanyContactName] = useState('');
  const [companyContactPhone, setCompanyContactPhone] = useState('');
  const [companySuggestions, setCompanySuggestions] = useState<any[]>([]);
  const [isSearchingCompany, setIsSearchingCompany] = useState(false);

  // Người đặt / Đại diện thanh toán (BookingGuest)
  const [selectedGuestId, setSelectedGuestId] = useState<number | undefined>(undefined);
  const [guestPhone, setGuestPhone] = useState('');
  const [guestFullName, setGuestFullName] = useState('');
  const [guestEmail, setGuestEmail] = useState('');
  const [guestIdentityType, setGuestIdentityType] = useState('CCCD');
  const [guestIdentityNumber, setGuestIdentityNumber] = useState('');
  const [guestNationality, setGuestNationality] = useState('Việt Nam');
  const [isSearchingGuest, setIsSearchingGuest] = useState(false);
  const [guestFoundMessage, setGuestFoundMessage] = useState<string | null>(null);

  // Bước 3: Báo giá & Phụ thu
  const [quoteResponse, setQuoteResponse] = useState<WalkInQuoteResponse | null>(null);
  const [isLoadingQuote, setIsLoadingQuote] = useState(false);
  const [confirmedCharges, setConfirmedCharges] = useState<ConfirmedChargeItem[]>([]);

  // Bước 4: Dịch vụ thêm (F&B / Minibar)
  const [availableServices] = useState<AddonServiceItem[]>([
    { id: 1, name: 'Ăn sáng Buffet Cao Cấp', price: 150000, quantity: 0, roomIndex: 0 },
    { id: 2, name: 'Gói Chào Mừng Minibar', price: 120000, quantity: 0, roomIndex: 0 },
    { id: 3, name: 'Đưa Đón Sân Bay (Xe 4 Chỗ)', price: 350000, quantity: 0, roomIndex: 0 },
    { id: 4, name: 'Voucher Spa Thư Giãn (60p)', price: 400000, quantity: 0, roomIndex: 0 },
  ]);
  const [selectedServices, setSelectedServices] = useState<AddonServiceItem[]>([]);

  // Bước 5: Thanh toán tại quầy
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'VNPAY'>('CASH');
  const [paymentPurpose, setPaymentPurpose] = useState<'FULL_PAYMENT' | 'DEPOSIT'>('FULL_PAYMENT');
  const [customPaidAmount, setCustomPaidAmount] = useState<number>(0);
  const [transactionRef, setTransactionRef] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Unique IDs for HTML form elements
  const hotelSelectId = useId();
  const checkInDateId = useId();
  const checkOutDateId = useId();
  const checkInTimeId = useId();

  // Load Hotels
  useEffect(() => {
    hotelService
      .filter({ pageSize: 50, status: 'ACTIVE' })
      .then((res) => {
        if (res.result?.content && res.result.content.length > 0) {
          setHotels(res.result.content);
          setSelectedHotelId(res.result.content[0].id);
        }
      })
      .catch((err) => console.error('Lỗi tải danh sách khách sạn:', err));
  }, []);

  // Load Hotel Room Types
  useEffect(() => {
    if (selectedHotelId) {
      hotelRoomTypeService
        .filter({ hotelId: selectedHotelId, pageSize: 50, status: 'ACTIVE' })
        .then((res) => {
          if (res.result?.content) {
            setRoomTypes(res.result.content);
          }
        })
        .catch((err) => console.error('Lỗi tải hạng phòng:', err));
    }
  }, [selectedHotelId]);

  // Load Physical Rooms with status 'READY'
  useEffect(() => {
    if (selectedHotelId) {
      roomInstanceService
        .filter({ hotelId: selectedHotelId, currentStatus: 'READY', pageSize: 100 })
        .then((res) => {
          if (res.result?.content) {
            setReadyRooms(res.result.content);
          }
        })
        .catch((err) => console.error('Lỗi tải phòng sẵn sàng:', err));
    }
  }, [selectedHotelId]);

  // Quản lý danh sách phòng (Thêm / Xóa / Cập nhật)
  const handleAddRoom = () => {
    if (rooms.length >= 5) return;
    setRooms([
      ...rooms,
      {
        id: `room_${Date.now()}`,
        hotelRoomTypeId: '',
        roomInstanceId: '',
        adultCount: 2,
        childCount: 0,
        infantCount: 0,
        extraBeds: 0,
        stayGuests: [
          {
            fullName: '',
            birthDate: '',
            guestType: 'ADULT',
            gender: 'MALE',
            identityType: 'CCCD',
            identityNumber: '',
            nationality: 'Việt Nam',
            isPrimaryGuest: true,
          },
        ],
      },
    ]);
  };

  const handleRemoveRoom = (index: number) => {
    if (rooms.length <= 1) return;
    setRooms(rooms.filter((_, idx) => idx !== index));
  };

  const updateRoomField = (index: number, field: keyof RoomConfig, value: any) => {
    const updated = [...rooms];
    updated[index] = { ...updated[index], [field]: value };
    // Nếu đổi hạng phòng thì reset roomInstanceId đã chọn
    if (field === 'hotelRoomTypeId') {
      updated[index].roomInstanceId = '';
    }
    setRooms(updated);
  };

  // Tra cứu nhanh khách cũ qua SĐT
  const handleSearchGuestByPhone = async () => {
    if (!guestPhone.trim()) return;
    setIsSearchingGuest(true);
    setGuestFoundMessage(null);
    try {
      const res = await apiClient.get<any>(`/api/v1/guests/phone/${guestPhone.trim()}`);
      if (res.result) {
        setSelectedGuestId(res.result.id);
        setGuestFullName(res.result.fullName || `${res.result.lastName || ''} ${res.result.firstName || ''}`.trim());
        if (res.result.email) setGuestEmail(res.result.email);
        if (res.result.identityNumber) setGuestIdentityNumber(res.result.identityNumber);
        if (res.result.identityType) setGuestIdentityType(res.result.identityType);
        if (res.result.nationality) setGuestNationality(res.result.nationality);
        setGuestFoundMessage(`Đã tìm thấy hồ sơ khách: ${res.result.fullName || 'Khách quen'} (ID #${res.result.id})`);
      }
    } catch {
      setSelectedGuestId(undefined);
      setGuestFoundMessage('Khách mới - Hệ thống sẽ tự động lưu hồ sơ khách.');
    } finally {
      setIsSearchingGuest(false);
    }
  };

  // Tra cứu nhanh Doanh nghiệp (Autocomplete)
  const handleSearchCompany = async (query: string) => {
    setCompanyName(query);
    setSelectedCompanyId(undefined);
    if (!query.trim() || query.trim().length < 2) {
      setCompanySuggestions([]);
      return;
    }
    setIsSearchingCompany(true);
    try {
      const res = await apiClient.post<any>('/api/v1/companies/filter', {
        keyword: query.trim(),
        pageSize: 5,
      });
      if (res.result?.content) {
        setCompanySuggestions(res.result.content);
      }
    } catch {
      setCompanySuggestions([]);
    } finally {
      setIsSearchingCompany(false);
    }
  };

  const handleSelectCompany = (comp: any) => {
    setSelectedCompanyId(comp.id);
    setCompanyName(comp.name || '');
    setTaxCode(comp.taxCode || '');
    setCompanyAddress(comp.address || '');
    setCompanyContactName(comp.contactName || '');
    setCompanyContactPhone(comp.contactPhone || '');
    setCompanySuggestions([]);
  };

  // Nút 1-click: Gán người đặt vào phòng #1
  const copyLeadGuestToRoom1 = () => {
    if (!guestFullName) {
      alert('Vui lòng nhập họ tên người đặt trước.');
      return;
    }
    const updated = [...rooms];
    if (updated[0]) {
      const guests = [...updated[0].stayGuests];
      guests[0] = {
        ...guests[0],
        fullName: guestFullName,
        identityType: guestIdentityType,
        identityNumber: guestIdentityNumber,
        nationality: guestNationality,
        isPrimaryGuest: true,
      };
      updated[0].stayGuests = guests;
      setRooms(updated);
    }
  };

  // Thêm người ở cho từng phòng
  const handleAddStayGuest = (roomIndex: number) => {
    const updated = [...rooms];
    updated[roomIndex].stayGuests.push({
      fullName: '',
      birthDate: '',
      guestType: 'ADULT',
      gender: 'MALE',
      identityType: 'CCCD',
      identityNumber: '',
      nationality: 'Việt Nam',
      isPrimaryGuest: false,
    });
    setRooms(updated);
  };

  const handleRemoveStayGuest = (roomIndex: number, guestIndex: number) => {
    const updated = [...rooms];
    if (updated[roomIndex].stayGuests.length > 1) {
      updated[roomIndex].stayGuests = updated[roomIndex].stayGuests.filter((_, gIdx) => gIdx !== guestIndex);
      setRooms(updated);
    }
  };

  const updateStayGuestField = (
    roomIndex: number,
    guestIndex: number,
    field: keyof WalkInStayGuestRequest,
    value: any
  ) => {
    const updated = [...rooms];
    const guests = [...updated[roomIndex].stayGuests];
    guests[guestIndex] = { ...guests[guestIndex], [field]: value };
    updated[roomIndex].stayGuests = guests;
    setRooms(updated);
  };

  // Chuyển sang Bước 3: Tính báo giá và phụ thu
  const fetchQuoteAndProceedToStep3 = async () => {
    // Validate Bước 1
    for (let i = 0; i < rooms.length; i++) {
      if (!rooms[i].hotelRoomTypeId) {
        setFormError(`Phòng #${i + 1} chưa chọn hạng phòng.`);
        return;
      }
      if (!rooms[i].roomInstanceId) {
        setFormError(`Phòng #${i + 1} chưa chọn số phòng vật lý (READY).`);
        return;
      }
    }
    // Validate Bước 2
    if (!guestPhone.trim() || !guestFullName.trim()) {
      setFormError('Vui lòng nhập số điện thoại và họ tên người đặt/đại diện.');
      return;
    }
    if (bookingType === 'CORPORATE' && !companyName.trim()) {
      setFormError('Đơn Doanh nghiệp yêu cầu nhập Tên công ty.');
      return;
    }

    setFormError(null);
    setIsLoadingQuote(true);
    try {
      const quotePayload = {
        hotelId: selectedHotelId,
        checkInDate,
        checkOutDate,
        actualCheckInTime,
        rooms: rooms.map((r) => ({
          hotelRoomTypeId: Number(r.hotelRoomTypeId),
          adultCount: r.adultCount,
          childCount: r.childCount,
          infantCount: r.infantCount,
          extraBeds: r.extraBeds,
        })),
      };

      const res = await bookingService.calculateWalkInQuote(quotePayload);
      if (res.result) {
        setQuoteResponse(res.result);

        // Khởi tạo danh sách phụ thu phát hiện được để lễ tân phê duyệt
        const detectedCharges: ConfirmedChargeItem[] = [];
        res.result.rooms.forEach((r, idx) => {
          const roomInstanceId = Number(rooms[idx]?.roomInstanceId);
          // Early check-in
          if (r.earlyCheckinProposal?.applicable) {
            detectedCharges.push({
              roomInstanceId,
              chargeType: 'EARLY_CHECKIN',
              title: `P.${readyRooms.find((rm) => rm.id === roomInstanceId)?.roomNumber || idx + 1}: ${r.earlyCheckinProposal.suggestedItemName}`,
              amount: r.earlyCheckinProposal.suggestedAmount,
              suggestedAmount: r.earlyCheckinProposal.suggestedAmount,
              action: 'APPLY',
              waivedReason: '',
              ruleDescription: r.earlyCheckinProposal.policyRule,
            });
          }
          // Extra capacity / extra beds
          if (r.capacitySurcharge && r.capacitySurcharge.totalSurchargeAmount > 0) {
            detectedCharges.push({
              roomInstanceId,
              chargeType: 'OTHER',
              title: `P.${readyRooms.find((rm) => rm.id === roomInstanceId)?.roomNumber || idx + 1}: Phụ thu vượt sức chứa / Giường phụ`,
              amount: r.capacitySurcharge.totalSurchargeAmount,
              suggestedAmount: r.capacitySurcharge.totalSurchargeAmount,
              action: 'APPLY',
              waivedReason: '',
              ruleDescription: r.capacitySurcharge.surchargeNotes?.join(', ') || 'Theo biểu phí phụ thu hạng phòng',
            });
          }
        });

        setConfirmedCharges(detectedCharges);
        setStep(3);
      }
    } catch (err: any) {
      setFormError(err?.response?.data?.message || 'Lỗi tính báo giá phòng và phụ thu.');
    } finally {
      setIsLoadingQuote(false);
    }
  };

  // Tính tổng tiền đơn hàng
  const totalRoomGross = quoteResponse?.totalAmount || 0;
  const totalApprovedCharges = confirmedCharges.reduce((acc, c) => {
    if (c.action === 'WAIVE') return acc;
    return acc + (c.action === 'CUSTOM' ? c.amount : c.suggestedAmount);
  }, 0);
  const totalServices = selectedServices.reduce((acc, s) => acc + s.price * s.quantity, 0);
  const grandTotal = totalRoomGross + totalApprovedCharges + totalServices;

  // Ràng buộc thanh toán: Đặt ít phòng (≤ 2 phòng) hoặc FIT -> Bắt buộc 100%
  const isSmallOrFit = rooms.length <= 2 && bookingType === 'FIT';

  // Khi sang Bước 5, tự động tính số tiền thanh toán
  useEffect(() => {
    if (step === 5) {
      if (isSmallOrFit) {
        setPaymentPurpose('FULL_PAYMENT');
        setCustomPaidAmount(grandTotal);
      } else {
        if (paymentPurpose === 'FULL_PAYMENT') {
          setCustomPaidAmount(grandTotal);
        } else {
          setCustomPaidAmount(Math.round(grandTotal * 0.5));
        }
      }
    }
  }, [step, grandTotal, isSmallOrFit, paymentPurpose]);

  // Xử lý tạo đơn Walk-In và Check-in ngay
  const handleCompleteWalkIn = async () => {
    setFormError(null);
    setIsSubmitting(true);

    try {
      const finalAmount = customPaidAmount > 0 ? customPaidAmount : grandTotal;

      if (isSmallOrFit && (paymentPurpose !== 'FULL_PAYMENT' || finalAmount < grandTotal)) {
        setFormError('Khách vãng lai đặt ít phòng (≤ 2 phòng) bắt buộc thanh toán 100% khi nhận phòng.');
        setIsSubmitting(false);
        return;
      }

      const payload: WalkInBookingRequest = {
        hotelId: selectedHotelId,
        bookingType,
        companyId: selectedCompanyId,
        newCompany:
          bookingType === 'CORPORATE' && !selectedCompanyId && companyName.trim()
            ? {
                name: companyName,
                taxCode,
                address: companyAddress,
                contactName: companyContactName,
                contactPhone: companyContactPhone,
              }
            : undefined,
        guestId: selectedGuestId,
        newGuest: !selectedGuestId
          ? {
              fullName: guestFullName,
              phone: guestPhone,
              email: guestEmail,
              identityType: guestIdentityType,
              identityNumber: guestIdentityNumber,
              nationality: guestNationality,
            }
          : undefined,
        checkInDate,
        checkOutDate,
        notes: `Khách vãng lai Check-in ngay tại quầy lúc ${actualCheckInTime}.`,
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
          paymentMethod,
          paymentPurpose,
          amount: finalAmount,
          transactionReference: transactionRef,
        },
      };

      const res = await bookingService.createWalkInBooking(payload);
      if (res.result) {
        onSuccess(res.result.bookingNumber);
        onClose();
      }
    } catch (err: any) {
      setFormError(err?.response?.data?.message || 'Có lỗi xảy ra khi hoàn tất nhận khách vãng lai.');
    } finally {
      setIsSubmitting(false);
    }
  };

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
              className={`flex items-center gap-2 cursor-pointer ${
                step === item.num
                  ? 'text-red-600 font-bold'
                  : step > item.num
                  ? 'text-neutral-900 font-medium'
                  : 'text-neutral-400'
              }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-all ${
                  step === item.num
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

        {/* ======================================================== */}
        {/* BƯỚC 1: NHU CẦU LƯU TRÚ & CẤU HÌNH PHÒNG TỐI ĐA 5 PHÒNG */}
        {/* ======================================================== */}
        {step === 1 && (
          <div className="space-y-6 animate-in fade-in duration-150">
            {/* Thông tin cơ sở & Mốc thời gian */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 p-4 bg-neutral-50 rounded-2xl border border-neutral-200">
              <div>
                <label htmlFor={hotelSelectId} className="block text-xs font-semibold text-neutral-600 mb-1">Khách sạn cơ sở</label>
                <select
                  id={hotelSelectId}
                  value={selectedHotelId}
                  onChange={(e) => setSelectedHotelId(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                >
                  {hotels.map((h) => (
                    <option key={h.id} value={h.id}>
                      {h.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor={checkInDateId} className="block text-xs font-semibold text-neutral-600 mb-1">Ngày nhận phòng</label>
                <input
                  id={checkInDateId}
                  type="date"
                  value={checkInDate}
                  min={today}
                  onChange={(e) => setCheckInDate(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                />
              </div>
              <div>
                <label htmlFor={checkOutDateId} className="block text-xs font-semibold text-neutral-600 mb-1">Ngày trả phòng</label>
                <input
                  id={checkOutDateId}
                  type="date"
                  value={checkOutDate}
                  min={checkInDate || today}
                  onChange={(e) => setCheckOutDate(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                />
              </div>
              <div>
                <label htmlFor={checkInTimeId} className="block text-xs font-semibold text-neutral-600 mb-1">Giờ đến thực tế</label>
                <div className="relative">
                  <input
                    id={checkInTimeId}
                    type="time"
                    value={actualCheckInTime}
                    onChange={(e) => setActualCheckInTime(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                  />
                  <Clock className="w-4 h-4 text-neutral-400 absolute right-3 top-2.5 pointer-events-none" />
                </div>
              </div>
            </div>

            {/* Danh sách phòng đã chọn (Tối đa 5 phòng) */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                    <BedDouble className="w-5 h-5 text-red-600" />
                    Cấu hình từng phòng đón khách
                  </h3>
                  <p className="text-xs text-neutral-500">
                    Mỗi phòng được cấu hình khách riêng biệt và gán trực tiếp phòng vật lý sẵn sàng (READY).
                  </p>
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
                // Lọc danh sách phòng READY của loại phòng này mà chưa bị các phòng khác trong form chọn
                const otherSelectedRoomInstanceIds = rooms
                  .filter((_, rIdx) => rIdx !== idx)
                  .map((r) => r.roomInstanceId);
                const availablePhysicalRooms = readyRooms.filter(
                  (r) =>
                    r.hotelRoomTypeId === Number(room.hotelRoomTypeId) &&
                    !otherSelectedRoomInstanceIds.includes(r.id)
                );

                return (
                  <div
                    key={room.id}
                    className="p-5 bg-white rounded-2xl border border-neutral-200 shadow-xs space-y-4 relative"
                  >
                    <div className="flex items-center justify-between border-b border-neutral-100 pb-3">
                      <span className="text-sm font-bold text-red-600 flex items-center gap-2">
                        Phòng #{idx + 1}
                      </span>
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
                      {/* Chọn Hạng phòng */}
                      <div>
                        <label className="block text-xs font-semibold text-neutral-600 mb-1">
                          Hạng phòng <span className="text-red-500">*</span>
                        </label>
                        <select
                          value={room.hotelRoomTypeId}
                          onChange={(e) =>
                            updateRoomField(idx, 'hotelRoomTypeId', e.target.value ? Number(e.target.value) : '')
                          }
                          className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600"
                        >
                          <option value="">-- Chọn loại phòng --</option>
                          {roomTypes.map((rt) => (
                            <option key={rt.id} value={rt.id}>
                              {rt.roomTypeName} (Chuẩn: {rt.standardAdults} lớn, Tối đa: {rt.maxTotalGuests || 4} khách)
                            </option>
                          ))}
                        </select>
                      </div>

                      {/* Chọn Số phòng vật lý (Chỉ hiển thị phòng READY) */}
                      <div>
                        <label className="block text-xs font-semibold text-neutral-600 mb-1">
                          Số phòng vật lý sẵn sàng (READY) <span className="text-red-500">*</span>
                        </label>
                        <select
                          value={room.roomInstanceId}
                          disabled={!room.hotelRoomTypeId}
                          onChange={(e) =>
                            updateRoomField(idx, 'roomInstanceId', e.target.value ? Number(e.target.value) : '')
                          }
                          className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-red-600 disabled:opacity-50"
                        >
                          <option value="">
                            {room.hotelRoomTypeId
                              ? availablePhysicalRooms.length > 0
                                ? `-- Chọn phòng (${availablePhysicalRooms.length} phòng sẵn sàng) --`
                                : '-- Không có phòng READY nào --'
                              : '-- Vui lòng chọn loại phòng trước --'}
                          </option>
                          {availablePhysicalRooms.map((pr) => (
                            <option key={pr.id} value={pr.id}>
                              Phòng {pr.roomNumber} (Sạch sẽ, sẵn sàng)
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>

                    {/* Số lượng khách chi tiết theo từng phòng */}
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

        {/* ======================================================== */}
        {/* BƯỚC 2: KHAI BÁO KHÁCH HÀNG & DOANH NGHIỆP & NGƯỜI Ở     */}
        {/* ======================================================== */}
        {step === 2 && (
          <div className="space-y-6 animate-in fade-in duration-150">
            {/* Toggle Loại hình đặt phòng */}
            <div className="flex items-center gap-4 p-3 bg-neutral-50 rounded-2xl border border-neutral-200">
              <span className="text-xs font-bold text-neutral-600">Loại khách:</span>
              <button
                type="button"
                onClick={() => setBookingType('FIT')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                  bookingType === 'FIT'
                    ? 'bg-red-600 text-white shadow-xs'
                    : 'bg-white text-neutral-700 hover:bg-neutral-100 border border-neutral-200'
                }`}
              >
                Khách Cá Nhân (FIT)
              </button>
              <button
                type="button"
                onClick={() => setBookingType('CORPORATE')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                  bookingType === 'CORPORATE'
                    ? 'bg-red-600 text-white shadow-xs'
                    : 'bg-white text-neutral-700 hover:bg-neutral-100 border border-neutral-200'
                }`}
              >
                Doanh Nghiệp / Công Ty (CORPORATE)
              </button>
            </div>

            {/* Thông tin Doanh nghiệp nếu chọn CORPORATE */}
            {bookingType === 'CORPORATE' && (
              <div className="p-4 bg-blue-50/50 rounded-2xl border border-blue-200 space-y-4">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-bold text-blue-900 flex items-center gap-2">
                    <Briefcase className="w-4 h-4 text-blue-600" />
                    Thông tin Doanh nghiệp bảo trợ / Đối tác công tác
                  </h4>
                  {selectedCompanyId && (
                    <span className="text-xs bg-blue-100 text-blue-800 font-bold px-2.5 py-1 rounded-full border border-blue-300">
                      Đã chọn đối tác #{selectedCompanyId}
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
                      value={companyName}
                      onChange={(e) => handleSearchCompany(e.target.value)}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
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
                            <div className="text-[11px] text-neutral-500">
                              MST: {c.taxCode || 'N/A'} {c.contactPhone ? `| SĐT: ${c.contactPhone}` : ''}
                            </div>
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
                      value={taxCode}
                      onChange={(e) => setTaxCode(e.target.value)}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">Địa chỉ xuất hóa đơn</label>
                    <input
                      type="text"
                      placeholder="Hà Nội, Việt Nam"
                      value={companyAddress}
                      onChange={(e) => setCompanyAddress(e.target.value)}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">Người đại diện liên hệ</label>
                    <input
                      type="text"
                      placeholder="Nguyễn Văn Đại diện"
                      value={companyContactName}
                      onChange={(e) => setCompanyContactName(e.target.value)}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-neutral-700 mb-1">SĐT người liên hệ</label>
                    <input
                      type="text"
                      placeholder="0912345678"
                      value={companyContactPhone}
                      onChange={(e) => setCompanyContactPhone(e.target.value)}
                      className="w-full px-3 py-2 bg-white border border-neutral-300 rounded-xl text-sm"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Người đặt / Đại diện thanh toán (BookingGuest) */}
            <div className="p-5 bg-white rounded-2xl border border-neutral-200 shadow-xs space-y-4">
              <div className="flex items-center justify-between border-b border-neutral-100 pb-2">
                <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                  <UserCheck className="w-5 h-5 text-red-600" />
                  Người đặt phòng / Đại diện thanh toán (Lead Guest)
                </h4>
                {guestFoundMessage && (
                  <span className="text-xs text-green-700 font-medium bg-green-50 px-2.5 py-1 rounded-full border border-green-200">
                    {guestFoundMessage}
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                {/* Số điện thoại + Tìm kiếm nhanh */}
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Số điện thoại <span className="text-red-500">*</span>
                  </label>
                  <div className="flex gap-1.5">
                    <input
                      type="text"
                      placeholder="0912345678"
                      value={guestPhone}
                      onChange={(e) => setGuestPhone(e.target.value)}
                      className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm font-medium"
                    />
                    <button
                      type="button"
                      onClick={handleSearchGuestByPhone}
                      disabled={isSearchingGuest}
                      className="px-3 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-xl text-xs font-semibold transition-colors shrink-0"
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
                    placeholder="NGUYỄN VĂN A"
                    value={guestFullName}
                    onChange={(e) => setGuestFullName(e.target.value.toUpperCase())}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm font-bold"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Số CCCD / Hộ chiếu</label>
                  <input
                    type="text"
                    placeholder="001200001234"
                    value={guestIdentityNumber}
                    onChange={(e) => setGuestIdentityNumber(e.target.value)}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Email</label>
                  <input
                    type="email"
                    placeholder="khach@gmail.com"
                    value={guestEmail}
                    onChange={(e) => setGuestEmail(e.target.value)}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm"
                  />
                </div>
              </div>
            </div>

            {/* Danh sách người lưu trú thực tế theo từng phòng (StayGuest) */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-neutral-700" />
                  Danh sách người thực tế lưu trú (Đồng bộ khai báo tạm trú BCA)
                </h4>
                <button
                  type="button"
                  onClick={copyLeadGuestToRoom1}
                  className="px-3 py-1.5 bg-red-50 text-red-600 hover:bg-red-100 rounded-xl text-xs font-bold transition-colors"
                >
                  ⚡ Gán người đặt trực tiếp ở Phòng #1
                </button>
              </div>

              {rooms.map((room, rIdx) => {
                const assignedRoomNumber = readyRooms.find((r) => r.id === room.roomInstanceId)?.roomNumber;
                return (
                  <div key={room.id} className="p-4 bg-neutral-50 rounded-2xl border border-neutral-200 space-y-3">
                    <div className="flex items-center justify-between border-b border-neutral-200 pb-2">
                      <span className="text-xs font-bold text-neutral-900">
                        Phòng #{rIdx + 1} {assignedRoomNumber && `(P.${assignedRoomNumber})`} — {room.adultCount} lớn, {room.childCount} trẻ
                      </span>
                      <button
                        type="button"
                        onClick={() => handleAddStayGuest(rIdx)}
                        className="text-xs text-red-600 hover:text-red-700 font-semibold flex items-center gap-1"
                      >
                        <Plus className="w-3.5 h-3.5" /> Thêm khách ở phòng này
                      </button>
                    </div>

                    {room.stayGuests.map((guest, gIdx) => (
                      <div key={gIdx} className="grid grid-cols-1 sm:grid-cols-6 gap-2 items-center bg-white p-2.5 rounded-xl border border-neutral-200">
                        <div className="sm:col-span-2">
                          <input
                            type="text"
                            placeholder="Họ và tên khách"
                            value={guest.fullName}
                            onChange={(e) => updateStayGuestField(rIdx, gIdx, 'fullName', e.target.value.toUpperCase())}
                            className="w-full px-2.5 py-1.5 border border-neutral-200 rounded-lg text-xs font-semibold"
                          />
                        </div>
                        <div>
                          <input
                            type="text"
                            placeholder="Số CCCD / Hộ chiếu"
                            value={guest.identityNumber || ''}
                            onChange={(e) => updateStayGuestField(rIdx, gIdx, 'identityNumber', e.target.value)}
                            className="w-full px-2.5 py-1.5 border border-neutral-200 rounded-lg text-xs"
                          />
                        </div>
                        <div>
                          <select
                            value={guest.gender || 'MALE'}
                            onChange={(e) => updateStayGuestField(rIdx, gIdx, 'gender', e.target.value)}
                            className="w-full px-2.5 py-1.5 border border-neutral-200 rounded-lg text-xs font-medium"
                          >
                            <option value="MALE">Nam</option>
                            <option value="FEMALE">Nữ</option>
                            <option value="OTHER">Khác</option>
                          </select>
                        </div>
                        <div>
                          <input
                            type="date"
                            value={guest.birthDate || ''}
                            onChange={(e) => updateStayGuestField(rIdx, gIdx, 'birthDate', e.target.value)}
                            className="w-full px-2.5 py-1.5 border border-neutral-200 rounded-lg text-xs font-medium"
                          />
                        </div>
                        <div className="flex items-center justify-end">
                          {room.stayGuests.length > 1 && (
                            <button
                              type="button"
                              onClick={() => handleRemoveStayGuest(rIdx, gIdx)}
                              className="text-neutral-400 hover:text-red-600 p-1"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          )}
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

        {/* ======================================================== */}
        {/* BƯỚC 3: PHỤ THU TỰ ĐỘNG & LỄ TÂN PHÊ DUYỆT (SURCHARGES)  */}
        {/* ======================================================== */}
        {step === 3 && (
          <div className="space-y-6 animate-in fade-in duration-150">
            {/* Tóm tắt tiền phòng ban đầu từ PriceEngine */}
            <div className="p-4 bg-neutral-50 rounded-2xl border border-neutral-200 flex items-center justify-between">
              <div>
                <span className="text-xs font-semibold text-neutral-500 uppercase">Tiền phòng tạm tính (Đã gồm VAT & Phí DV):</span>
                <div className="text-xl font-extrabold text-neutral-900 mt-0.5">
                  {totalRoomGross.toLocaleString('vi-VN')} đ
                </div>
              </div>
              <div className="text-right">
                <span className="text-xs font-medium text-neutral-500">Giờ vào thực tế:</span>
                <div className="text-sm font-bold text-red-600 flex items-center gap-1">
                  <Clock className="w-4 h-4" /> {actualCheckInTime} (Chuẩn: 14:00)
                </div>
              </div>
            </div>

            {/* Danh sách phụ thu tự động phát hiện được */}
            <div className="space-y-3">
              <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-amber-500" />
                Phụ thu tự động phát hiện theo mốc giờ và sức chứa từng phòng
              </h4>

              {confirmedCharges.length === 0 ? (
                <div className="p-6 bg-white rounded-2xl border border-dashed border-neutral-300 text-center text-neutral-500 text-sm">
                  Không phát sinh phụ thu tự động nào (Nhận phòng đúng giờ và đúng số người chuẩn).
                </div>
              ) : (
                confirmedCharges.map((charge, cIdx) => (
                  <div
                    key={cIdx}
                    className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs space-y-3"
                  >
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

                    {/* Lựa chọn xử lý của Lễ tân */}
                    <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-neutral-100">
                      <button
                        type="button"
                        onClick={() => {
                          const updated = [...confirmedCharges];
                          updated[cIdx].action = 'APPLY';
                          setConfirmedCharges(updated);
                        }}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                          charge.action === 'APPLY'
                            ? 'bg-blue-600 text-white'
                            : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                        }`}
                      >
                        ✓ Áp dụng đề xuất ({charge.suggestedAmount.toLocaleString('vi-VN')} đ)
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          const updated = [...confirmedCharges];
                          updated[cIdx].action = 'WAIVE';
                          setConfirmedCharges(updated);
                        }}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                          charge.action === 'WAIVE'
                            ? 'bg-green-600 text-white'
                            : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                        }`}
                      >
                        Miễn phí (0đ)
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          const updated = [...confirmedCharges];
                          updated[cIdx].action = 'CUSTOM';
                          setConfirmedCharges(updated);
                        }}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                          charge.action === 'CUSTOM'
                            ? 'bg-purple-600 text-white'
                            : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                        }`}
                      >
                        Điều chỉnh số tiền
                      </button>

                      {/* Input nếu miễn phí */}
                      {charge.action === 'WAIVE' && (
                        <div className="w-full mt-2">
                          <input
                            type="text"
                            placeholder="Nhập lý do miễn phụ thu (VD: Khách VIP, Duyệt ban giám đốc...)"
                            value={charge.waivedReason}
                            onChange={(e) => {
                              const updated = [...confirmedCharges];
                              updated[cIdx].waivedReason = e.target.value;
                              setConfirmedCharges(updated);
                            }}
                            className="w-full px-3 py-1.5 border border-green-300 rounded-xl text-xs bg-green-50/50"
                          />
                        </div>
                      )}

                      {/* Input nếu điều chỉnh */}
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

        {/* ======================================================== */}
        {/* BƯỚC 4: DỊCH VỤ ĐI KÈM (ADD-ON SERVICES / POS)            */}
        {/* ======================================================== */}
        {step === 4 && (
          <div className="space-y-6 animate-in fade-in duration-150">
            <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
              <Coffee className="w-4 h-4 text-red-600" />
              Dịch vụ đón tiếp và tiện ích yêu cầu thêm (Tùy chọn)
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {availableServices.map((svc) => {
                const selected = selectedServices.find((s) => s.id === svc.id);
                const qty = selected?.quantity || 0;

                return (
                  <div
                    key={svc.id}
                    className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs flex items-center justify-between"
                  >
                    <div>
                      <div className="text-sm font-bold text-neutral-900">{svc.name}</div>
                      <div className="text-xs font-semibold text-red-600 mt-0.5">
                        {svc.price.toLocaleString('vi-VN')} đ / gói
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          if (qty > 0) {
                            setSelectedServices(
                              selectedServices
                                .map((s) => (s.id === svc.id ? { ...s, quantity: s.quantity - 1 } : s))
                                .filter((s) => s.quantity > 0)
                            );
                          }
                        }}
                        className="w-8 h-8 rounded-lg bg-neutral-100 hover:bg-neutral-200 flex items-center justify-center font-bold text-neutral-700"
                      >
                        -
                      </button>
                      <span className="w-8 text-center text-sm font-bold">{qty}</span>
                      <button
                        type="button"
                        onClick={() => {
                          if (selected) {
                            setSelectedServices(
                              selectedServices.map((s) => (s.id === svc.id ? { ...s, quantity: s.quantity + 1 } : s))
                            );
                          } else {
                            setSelectedServices([...selectedServices, { ...svc, quantity: 1 }]);
                          }
                        }}
                        className="w-8 h-8 rounded-lg bg-red-50 hover:bg-red-100 flex items-center justify-center font-bold text-red-600"
                      >
                        +
                      </button>
                    </div>
                  </div>
                );
              })}
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
                Tiếp tục: Quyết toán & Thanh toán <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}

        {/* ======================================================== */}
        {/* BƯỚC 5: QUYẾT TOÁN, THANH TOÁN TẠI QUẦY & CHECK-IN NGAY */}
        {/* ======================================================== */}
        {step === 5 && (
          <div className="space-y-6 animate-in fade-in duration-150">
            {/* Bảng Kê Chi Phí Toàn Đơn */}
            <div className="p-5 bg-neutral-50 rounded-2xl border border-neutral-200 space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-neutral-600 font-medium">Tiền phòng ({rooms.length} phòng):</span>
                <span className="font-bold text-neutral-900">{totalRoomGross.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-neutral-600 font-medium">Phụ thu đã phê duyệt:</span>
                <span className="font-bold text-neutral-900">{totalApprovedCharges.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-neutral-600 font-medium">Dịch vụ đi kèm:</span>
                <span className="font-bold text-neutral-900">{totalServices.toLocaleString('vi-VN')} đ</span>
              </div>
              <div className="border-t border-neutral-200 pt-3 flex justify-between items-center">
                <span className="text-base font-extrabold text-neutral-900">TỔNG CẦN THANH TOÁN:</span>
                <span className="text-2xl font-black text-red-600">{grandTotal.toLocaleString('vi-VN')} đ</span>
              </div>
            </div>

            {/* Quy Tắc Thanh Toán Walk-In */}
            <div className="p-4 bg-white rounded-2xl border border-neutral-200 space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-2">
                  <CreditCard className="w-5 h-5 text-red-600" />
                  Phương thức thanh toán tại quầy
                </h4>
                {isSmallOrFit && (
                  <span className="text-xs text-red-700 bg-red-50 border border-red-200 px-3 py-1 rounded-full font-bold">
                    ⚠️ Đặt ít phòng (≤ 2 phòng): Bắt buộc thu 100%
                  </span>
                )}
              </div>

              {/* Loại thanh toán: 100% vs Đặt cọc */}
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setPaymentPurpose('FULL_PAYMENT');
                    setCustomPaidAmount(grandTotal);
                  }}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    paymentPurpose === 'FULL_PAYMENT'
                      ? 'border-red-600 bg-red-50/50 shadow-xs'
                      : 'border-neutral-200 hover:bg-neutral-50'
                  }`}
                >
                  <div className="text-xs font-bold text-neutral-900">Thanh toán 100% (Khuyến nghị)</div>
                  <div className="text-sm font-extrabold text-red-600 mt-1">
                    {grandTotal.toLocaleString('vi-VN')} đ
                  </div>
                </button>

                <button
                  type="button"
                  disabled={isSmallOrFit}
                  onClick={() => {
                    if (!isSmallOrFit) {
                      setPaymentPurpose('DEPOSIT');
                      setCustomPaidAmount(Math.round(grandTotal * 0.5));
                    }
                  }}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    isSmallOrFit
                      ? 'opacity-40 cursor-not-allowed bg-neutral-100 border-neutral-200'
                      : paymentPurpose === 'DEPOSIT'
                      ? 'border-red-600 bg-red-50/50 shadow-xs'
                      : 'border-neutral-200 hover:bg-neutral-50'
                  }`}
                >
                  <div className="text-xs font-bold text-neutral-900">Đặt cọc (Tối thiểu 50%)</div>
                  <div className="text-sm font-extrabold text-neutral-700 mt-1">
                    {(grandTotal * 0.5).toLocaleString('vi-VN')} đ
                  </div>
                </button>
              </div>

              {/* Số tiền thực thu */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Số tiền thực thu tại quầy (VNĐ) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="0"
                    step="10000"
                    disabled={isSmallOrFit}
                    value={customPaidAmount}
                    onChange={(e) => setCustomPaidAmount(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-base font-black text-red-600 disabled:bg-neutral-100"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">Hình thức thanh toán</label>
                  <select
                    value={paymentMethod}
                    onChange={(e: any) => setPaymentMethod(e.target.value)}
                    className="w-full px-3 py-2 border border-neutral-300 rounded-xl text-sm font-semibold"
                  >
                    <option value="CASH">Tiền mặt tại quầy (CASH)</option>
                    <option value="CREDIT_CARD">Quẹt thẻ POS (CREDIT_CARD)</option>
                    <option value="BANK_TRANSFER">Chuyển khoản ngân hàng (BANK_TRANSFER)</option>
                    <option value="VNPAY">Quét mã VNPAY-QR</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Mã tham chiếu / Số biên lai giao dịch (Tùy chọn)
                </label>
                <input
                  type="text"
                  placeholder="VD: POS-998822 hoặc STK MBBank..."
                  value={transactionRef}
                  onChange={(e) => setTransactionRef(e.target.value)}
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
                <CheckCircle2 className="w-5 h-5" /> Hoàn tất Check-in & Đón khách (IN-HOUSE)
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default WalkInBookingModal;
