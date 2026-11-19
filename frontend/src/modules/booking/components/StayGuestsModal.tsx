import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Users,
  Plus,
  Trash2,
  ShieldCheck,
  Calendar,
  CreditCard,
  User,
  MapPin,
  RefreshCw,
  AlertCircle,
  Upload,
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { extractIdentityImage, identityOcrErrorMessage, ocrFields, type IdentityOcrDraft } from '../../operation/services/identityOcr.service';
import stayGuestService from '../../operation/services/stayGuest.service';
import { hasAnyRole } from '../../auth/utils/token.utils';
import { LODGING_ROLES } from '../../operation/types/lodging.types';
import StayGuestEditForm from './StayGuestEditForm';
import type {
  StayGuestResponse,
  StayGuestCreateRequest,
  DocumentType,
  Gender,
} from '../../operation/types/stayGuest.types';

export interface StayGuestsModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingRoomId: number | null;
  initialEditGuestId?: number;
  hotelId: number;
  bookingId?: number;
  roomId?: number;
  roomNumber?: string;
  leadGuestName?: string;
  leadGuestPhone?: string;
  defaultCheckInTime?: string;
  defaultExpectedCheckOutTime?: string;
}

// A committed context change mounts fresh state, so old room data is never rendered under a new title.
export const StayGuestsModal: React.FC<StayGuestsModalProps> = (props) => props.isOpen
  ? <StayGuestsModalContent key={`${props.hotelId}:${props.bookingRoomId}:${props.bookingId}:${props.roomId}`} {...props} />
  : null;

const StayGuestsModalContent: React.FC<StayGuestsModalProps> = ({
  isOpen,
  onClose,
  bookingRoomId,
  hotelId,
  bookingId,
  roomId,
  roomNumber,
  leadGuestName,
  initialEditGuestId,
  defaultCheckInTime,
  defaultExpectedCheckOutTime,
}) => {
  const [guests, setGuests] = useState<StayGuestResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editGuestId, setEditGuestId] = useState(initialEditGuestId);
  const [editBusy, setEditBusy] = useState(false);
  const [guestsLoaded, setGuestsLoaded] = useState(false);
  const canEdit = hasAnyRole(LODGING_ROLES);
  const editing = canEdit ? guests.find(guest => guest.id === editGuestId) : undefined;

  // Form states
  const [fullName, setFullName] = useState('');
  const [documentType, setDocumentType] = useState<DocumentType>('CCCD');
  const [documentNumber, setDocumentNumber] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState<Gender>('MALE');
  const [nationality, setNationality] = useState('Việt Nam');
  const [permanentAddress, setPermanentAddress] = useState('');
  const [reasonForStay, setReasonForStay] = useState('Du lịch / Nghỉ dưỡng');

  const [ocrDraft, setOcrDraft] = useState<IdentityOcrDraft | null>(null);
  const [isReadingImage, setIsReadingImage] = useState(false);
  const [humanVerified, setHumanVerified] = useState(false);
  const [checkInTime, setCheckInTime] = useState(defaultCheckInTime || '');
  const [expectedCheckOutTime, setExpectedCheckOutTime] = useState(defaultExpectedCheckOutTime || '');
  const ocrRequest = useRef<AbortController | null>(null);
  const listRequest = useRef<AbortController | null>(null);
  const alive = useRef(true);
  const manualOcrFields = useRef(new Set<string>());

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
      listRequest.current?.abort();
      ocrRequest.current?.abort();
    };
  }, []);

  useEffect(() => () => { ocrRequest.current?.abort(); }, [isOpen, bookingRoomId]);

  const readIdentityImage = async (file?: File) => {
    if (!file || isSubmitting) return;
    ocrRequest.current?.abort();
    setOcrDraft(null);
    setHumanVerified(false);
    if (!manualOcrFields.current.has('fullName')) setFullName('');
    if (!manualOcrFields.current.has('documentType')) setDocumentType('OTHER');
    if (!manualOcrFields.current.has('documentNumber')) setDocumentNumber('');
    if (!manualOcrFields.current.has('dateOfBirth')) setDateOfBirth('');
    if (!manualOcrFields.current.has('gender')) setGender('OTHER');
    if (!manualOcrFields.current.has('nationality')) setNationality('');
    if (!manualOcrFields.current.has('permanentAddress')) setPermanentAddress('');
    const request = new AbortController();
    ocrRequest.current = request;
    setIsReadingImage(true);
    setHumanVerified(false);
    setErrorMessage(null);
    try {
      const draft = await extractIdentityImage(file, request.signal);
      if (request.signal.aborted) return;
      if (draft.status === 'UNREADABLE_DOCUMENT' || draft.status === 'INVALID_FILE') {
        setErrorMessage('Không đọc được giấy tờ. Vui lòng thử ảnh khác hoặc nhập thủ công.');
        return;
      }
      setOcrDraft(draft);
      if (!manualOcrFields.current.has('fullName')) setFullName(draft.fullName.value ?? '');
      if (!manualOcrFields.current.has('dateOfBirth')) setDateOfBirth(draft.dateOfBirth.value ?? '');
      if (!manualOcrFields.current.has('gender')) setGender(draft.gender.value ?? 'OTHER');
      if (!manualOcrFields.current.has('nationality')) setNationality(draft.nationality.value ?? '');
      if (!manualOcrFields.current.has('documentType')) setDocumentType(draft.documentType.value ?? 'OTHER');
      if (!manualOcrFields.current.has('documentNumber')) setDocumentNumber(draft.documentNumber.value ?? '');
      if (!manualOcrFields.current.has('permanentAddress')) setPermanentAddress(draft.permanentAddress.value ?? '');
    } catch (error) {
      if (!request.signal.aborted) setErrorMessage(identityOcrErrorMessage(error));
    } finally {
      if (!request.signal.aborted) setIsReadingImage(false);
    }
  };

  const resetForm = useCallback(() => {
    ocrRequest.current?.abort();
    setIsReadingImage(false);
    setOcrDraft(null);
    setHumanVerified(false);
    manualOcrFields.current.clear();
    setCheckInTime(defaultCheckInTime || '');
    setExpectedCheckOutTime(defaultExpectedCheckOutTime || '');
    setFullName('');
    setDocumentType('CCCD');
    setDocumentNumber('');
    setDateOfBirth('');
    setGender('MALE');
    setNationality('Việt Nam');
    setPermanentAddress('');
    setReasonForStay('Du lịch / Nghỉ dưỡng');
    setShowAddForm(false);
  }, [defaultCheckInTime, defaultExpectedCheckOutTime]);

  const loadGuests = useCallback(async () => {
    if (!bookingRoomId || !alive.current) return;
    listRequest.current?.abort();
    const request = new AbortController();
    listRequest.current = request;
    const current = () => alive.current && !request.signal.aborted && listRequest.current === request;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const res = await stayGuestService.getByBookingRoomId(bookingRoomId, request.signal);
      if (current()) {
        setGuests(res.result ?? []);
        setGuestsLoaded(true);
      }
    } catch {
      if (current()) setErrorMessage('Không thể tải danh sách khách lưu trú.');
    } finally {
      if (current()) setIsLoading(false);
    }
  }, [bookingRoomId]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isOpen && bookingRoomId) {
        void loadGuests();
        resetForm();
        setSuccessMessage(null);
        setErrorMessage(null);
      }
    }, 0);
    return () => clearTimeout(timer);
  }, [isOpen, bookingRoomId, loadGuests, resetForm]);



  const handleCreateGuest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookingRoomId || !roomId || !roomNumber) {
      setErrorMessage('Thông tin phòng không đầy đủ hoặc phòng chưa được gán.');
      return;
    }

    if (isReadingImage || isSubmitting || (ocrDraft && !humanVerified)) {
      setErrorMessage('Vui lòng kiểm tra bản nháp và xác nhận trước khi lưu.');
      return;
    }
    if (!checkInTime || !expectedCheckOutTime || !Number.isFinite(Date.parse(checkInTime))
        || !Number.isFinite(Date.parse(expectedCheckOutTime)) || Date.parse(expectedCheckOutTime) <= Date.parse(checkInTime)) {
      setErrorMessage('Vui lòng nhập giờ lưu trú hợp lệ; giờ trả phải sau giờ nhận phòng.');
      return;
    }
    if (!fullName.trim() || !documentNumber.trim()) {
      setErrorMessage('Vui lòng nhập họ tên và số giấy tờ.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    const payload: StayGuestCreateRequest = {
      hotelId,
      bookingId,
      bookingRoomId,
      roomId,
      roomNumber,
      fullName: fullName.trim(),
      documentType,
      documentNumber: documentNumber.trim() || undefined,
      dateOfBirth: dateOfBirth || undefined,
      gender,
      nationality: nationality.trim() || undefined,
      permanentAddress: permanentAddress.trim() || undefined,
      reasonForStay: reasonForStay.trim() || undefined,
      checkInTime: new Date(checkInTime).toISOString(),
      expectedCheckOutTime: new Date(expectedCheckOutTime).toISOString(),
    };

    try {
      await stayGuestService.create(payload);
      if (!alive.current) return;
      setSuccessMessage('Khai báo khách lưu trú thành công!');
      resetForm();
      loadGuests();
    } catch (err: unknown) {
      if (!alive.current) return;
      void err; // Không ghi payload giấy tờ hoặc lỗi Axios vào console.
      setErrorMessage('Không thể thêm thông tin khách lưu trú. Vui lòng kiểm tra lại.');
    } finally {
      if (alive.current) setIsSubmitting(false);
    }
  };

  const handleDeleteGuest = async (id: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa thông tin khách lưu trú này?')) return;
    try {
      await stayGuestService.delete([id]);
      if (!alive.current) return;
      setSuccessMessage('Đã xóa thông tin khách lưu trú.');
      loadGuests();
    } catch {
      if (!alive.current) return;
      setErrorMessage('Không thể xóa khách lưu trú.');
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={() => { if (!editBusy) { resetForm(); onClose(); } }}
      title={`Khai báo Khách lưu trú - Phòng ${roomNumber || 'Chưa xếp'}`}
      maxWidth="2xl"
    >
      <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
        {/* Banner thông tin lưu trú BCA */}
        <div className="p-3.5 bg-blue-50 border border-blue-200 rounded-xl flex items-start gap-3">
          <ShieldCheck className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
          <div className="text-xs text-blue-900 leading-relaxed">
            <span className="font-bold">Hàng đợi lưu trú:</span> Bổ sung thông tin khách để khách sạn đối soát.
            Hệ thống cập nhật hàng đợi nội bộ từ hồ sơ lưu trú.
          </div>
        </div>

        {errorMessage && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
              <span>{errorMessage}</span>
            </div>
            <button onClick={() => setErrorMessage(null)} className="font-bold text-red-800 hover:underline">
              ✕
            </button>
          </div>
        )}

        {successMessage && (
          <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center justify-between">
            <span>{successMessage}</span>
            <button onClick={() => setSuccessMessage(null)} className="font-bold text-emerald-900 hover:underline">
              ✕
            </button>
          </div>
        )}

        {editing && <StayGuestEditForm
          key={editing.id}
          guest={editing}
          onBusyChange={setEditBusy}
          onCancel={() => setEditGuestId(undefined)}
          onSaved={() => {
            setEditGuestId(undefined);
            setSuccessMessage('Đã lưu hồ sơ khách lưu trú.');
            void loadGuests();
          }}
        />}
        {canEdit && editGuestId !== undefined && guestsLoaded && !editing && !isLoading &&
          <p role="alert" className="text-sm text-red-700">Hồ sơ cần sửa không còn trong phòng này.</p>}

        {/* Danh sách khách lưu trú hiện tại */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Users className="w-4 h-4 text-neutral-600" />
              <h4 className="text-sm font-bold text-neutral-900">
                Khách lưu trú tại phòng ({guests.length})
              </h4>
            </div>
            {!showAddForm && !editing && (
              <Button
                variant="primary"
                size="sm"
                onClick={() => {
                  setShowAddForm(true);
                  setSuccessMessage(null);
                  setErrorMessage(null);
                }}
              >
                <Plus className="w-4 h-4 mr-1" />
                Thêm khách lưu trú
              </Button>
            )}
          </div>

          {isLoading ? (
            <div className="py-8 text-center text-neutral-400">
              <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
              Đang tải danh sách khách...
            </div>
          ) : guests.length === 0 ? (
            <div className="p-6 text-center border-2 border-dashed border-neutral-200 rounded-xl">
              <User className="w-8 h-8 mx-auto mb-2 text-neutral-400" />
              <p className="text-sm font-medium text-neutral-700">Chưa có khách nào được khai báo cho phòng này</p>
              <p className="text-xs text-neutral-500 mt-1">
                Nhấn &quot;Thêm khách lưu trú&quot; để bổ sung CCCD/Hộ chiếu trước khi Check-in.
              </p>
            </div>
          ) : (
            <div className="space-y-2.5">
              {guests.map((g) => (
                <div
                  key={g.id}
                  className="p-3.5 bg-neutral-50 border border-neutral-200 rounded-xl flex items-center justify-between gap-3 hover:bg-neutral-100/60 transition-colors"
                >
                  <div className="space-y-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-bold text-neutral-900 text-sm">{g.fullName}</span>
                      <span className="text-xs px-2 py-0.5 rounded-md font-mono font-medium bg-neutral-200 text-neutral-800">
                        {g.gender === 'MALE' ? 'Nam' : g.gender === 'FEMALE' ? 'Nữ' : 'Khác'}
                      </span>
                      {g.documentNumber && (
                        <span className="text-xs px-2 py-0.5 rounded-md font-mono bg-blue-100 text-blue-800 flex items-center gap-1">
                          <CreditCard className="w-3 h-3" />
                          {g.documentType}: ••••{g.documentNumber.length > 4 ? g.documentNumber.slice(-4) : ''}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-3 text-xs text-neutral-500 flex-wrap">
                      {g.dateOfBirth && (
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3 h-3" /> NS: {g.dateOfBirth}
                        </span>
                      )}
                      {g.nationality && <span>Quốc tịch: {g.nationality}</span>}
                      {g.permanentAddress && (
                        <span className="flex items-center gap-1 truncate max-w-[280px]">
                          <MapPin className="w-3 h-3 shrink-0" /> {g.permanentAddress}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex shrink-0 items-center gap-2">
                    {canEdit && !showAddForm && !editing && <Button variant="outline" size="sm" onClick={() => {
                      setSuccessMessage(null);
                      setErrorMessage(null);
                      setEditGuestId(g.id);
                    }}>Sửa hồ sơ</Button>}
                    <button
                      type="button"
                      disabled={!!editing}
                      onClick={() => handleDeleteGuest(g.id)}
                      className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                      title="Xóa thông tin khách"
                      aria-label="Xóa thông tin khách"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Form thêm mới khách lưu trú */}
        {showAddForm && (
          <form onSubmit={handleCreateGuest} className="p-4 bg-neutral-50 border border-neutral-200 rounded-xl space-y-4">
            <div className="flex items-center justify-between border-b border-neutral-200 pb-2">
              <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-1.5">
                <Plus className="w-4 h-4 text-red-600" />
                Khai báo thông tin khách lưu trú mới
              </h4>
              <button
                type="button"
                onClick={resetForm}
                className="text-xs text-neutral-500 hover:text-neutral-800 underline"
              >
                Hủy bỏ
              </button>
            </div>

            {leadGuestName && (
              <div className="flex items-center justify-between p-2.5 bg-blue-50/70 border border-blue-200 rounded-lg text-xs">
                <span className="text-blue-900 font-medium">
                  Người đặt phòng: <strong>{leadGuestName}</strong>
                </span>
                <button
                  type="button"
                  onClick={() => {
                    manualOcrFields.current.add('fullName');
                    setHumanVerified(false);
                    setFullName(leadGuestName.toUpperCase());
                  }}
                  className="px-2.5 py-1 text-xs font-semibold bg-white text-blue-700 border border-blue-300 rounded hover:bg-blue-50 cursor-pointer shadow-2xs transition"
                  title="Điền nhanh họ tên người đặt phòng làm khách ở phòng này"
                >
                  👤 Điền nhanh người đặt
                </button>
              </div>
            )}

            <fieldset disabled={isReadingImage || isSubmitting} className="space-y-4">
              {/* VÙNG TẢI ẢNH CCCD / HỘ CHIẾU HIỆN ĐẠI (DROPZONE) */}
              <div className="relative border-2 border-dashed border-neutral-300 hover:border-red-500 rounded-xl p-4 bg-white hover:bg-red-50/20 transition-all text-center group cursor-pointer shadow-2xs">
                <input
                  id="guest-document-upload"
                  aria-label="Ảnh giấy tờ"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    event.target.value = '';
                    void readIdentityImage(file);
                  }}
                />
                <div className="flex flex-col items-center justify-center gap-1.5 pointer-events-none">
                  <div className="w-10 h-10 rounded-full bg-red-50 text-red-600 flex items-center justify-center group-hover:scale-105 transition-transform">
                    <Upload className="w-5 h-5" />
                  </div>
                  <div className="text-xs font-bold text-neutral-800">
                    Đọc ảnh CCCD / Hộ chiếu (Tự động điền OCR)
                  </div>
                  <div className="text-[11px] text-neutral-500">
                    Kéo thả ảnh hoặc <span className="text-red-600 font-semibold underline">Chọn tệp từ máy</span> (PNG, JPEG, WebP; tối đa 5 MiB)
                  </div>
                </div>
              </div>

              {/* TRẠNG THÁI ĐANG ĐỌC ẢNH */}
              {isReadingImage && (
                <div role="status" className="p-3 bg-red-50 border border-red-200 rounded-xl flex items-center justify-center gap-2 text-xs font-semibold text-red-700 animate-pulse">
                  <RefreshCw className="w-4 h-4 animate-spin text-red-600" />
                  <span>Đang đọc ảnh...</span>
                </div>
              )}

              {/* KHỐI XÉT DUYỆT BẢN NHÁP OCR */}
              {ocrDraft && (
                <div role="status" className="p-3.5 bg-amber-50/90 border border-amber-200 rounded-xl space-y-2.5 text-xs">
                  <div className="flex items-center gap-2 text-amber-900 font-bold">
                    <ShieldCheck className="w-4 h-4 text-amber-600 shrink-0" />
                    <span>Bản nháp OCR — đối chiếu giấy tờ gốc và sửa các trường trước khi lưu.</span>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                    {Object.entries(ocrFields).map(([key, label]) => {
                      const field = ocrDraft[key as keyof typeof ocrFields];
                      const conf = Math.round(field.confidence * 100);
                      return (
                        <div key={key} className="p-1.5 px-2 bg-white rounded-lg border border-amber-200/80 flex items-center justify-between">
                          <span className="text-neutral-700 font-medium">{label}: {conf}%</span>
                          {!field.valid && <span className="text-red-600 font-bold ml-1 shrink-0">— Cần kiểm tra</span>}
                        </div>
                      );
                    })}
                  </div>
                  {ocrDraft.warnings.length > 0 && (
                    <div className="border-t border-amber-200 pt-1.5 space-y-0.5 text-amber-800 font-medium">
                      {ocrDraft.warnings.map((warning, index) => (
                        <p key={index} className="flex items-center gap-1">⚠️ {warning}</p>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* LƯỚI THÔNG TIN CÁ NHÂN */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label htmlFor="guest-full-name" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Họ và tên khách <span className="text-red-500">*</span>
                  </label>
                  <Input
                    id="guest-full-name"
                    placeholder="NGUYỄN VĂN A"
                    value={fullName}
                    onChange={(e) => {
                      manualOcrFields.current.add('fullName');
                      setHumanVerified(false);
                      setFullName(e.target.value.toUpperCase());
                    }}
                    required
                  />
                </div>

                <div>
                  <label htmlFor="guest-gender" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Giới tính
                  </label>
                  <select
                    id="guest-gender"
                    className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:border-red-600 focus:outline-none transition-all duration-200"
                    value={gender}
                    onChange={(e) => {
                      manualOcrFields.current.add('gender');
                      setHumanVerified(false);
                      setGender(e.target.value as Gender);
                    }}
                  >
                    <option value="MALE">Nam</option>
                    <option value="FEMALE">Nữ</option>
                    <option value="OTHER">Khác</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="guest-doc-type" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Loại giấy tờ
                  </label>
                  <select
                    id="guest-doc-type"
                    className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:border-red-600 focus:outline-none transition-all duration-200"
                    value={documentType}
                    onChange={(e) => {
                      manualOcrFields.current.add('documentType');
                      setHumanVerified(false);
                      setDocumentType(e.target.value as DocumentType);
                    }}
                  >
                    <option value="CCCD">Căn cước công dân (CCCD)</option>
                    <option value="PASSPORT">Hộ chiếu (Passport)</option>
                    <option value="CMND">Chứng minh nhân dân (CMND)</option>
                    <option value="DRIVER_LICENSE">Giấy phép lái xe</option>
                    <option value="OTHER">Khác</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="guest-doc-number" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Số giấy tờ (CCCD/Passport) <span className="text-red-500">*</span>
                  </label>
                  <Input
                    id="guest-doc-number"
                    required
                    placeholder="001200001234"
                    value={documentNumber}
                    onChange={(e) => {
                      manualOcrFields.current.add('documentNumber');
                      setHumanVerified(false);
                      setDocumentNumber(e.target.value);
                    }}
                  />
                </div>

                <div>
                  <label htmlFor="guest-dob" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Ngày sinh
                  </label>
                  <Input
                    id="guest-dob"
                    type="date"
                    value={dateOfBirth}
                    onChange={(e) => {
                      manualOcrFields.current.add('dateOfBirth');
                      setHumanVerified(false);
                      setDateOfBirth(e.target.value);
                    }}
                  />
                </div>

                <div>
                  <label htmlFor="guest-nationality" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Quốc tịch
                  </label>
                  <Input
                    id="guest-nationality"
                    placeholder="Việt Nam"
                    value={nationality}
                    onChange={(e) => {
                      manualOcrFields.current.add('nationality');
                      setHumanVerified(false);
                      setNationality(e.target.value);
                    }}
                  />
                </div>

                <div className="sm:col-span-2">
                  <label htmlFor="guest-address" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Địa chỉ thường trú (theo CCCD)
                  </label>
                  <Input
                    id="guest-address"
                    placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
                    value={permanentAddress}
                    onChange={(e) => {
                      manualOcrFields.current.add('permanentAddress');
                      setHumanVerified(false);
                      setPermanentAddress(e.target.value);
                    }}
                  />
                </div>

                <div className="sm:col-span-2">
                  <label htmlFor="guest-reason" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Lý do lưu trú
                  </label>
                  <Input
                    id="guest-reason"
                    placeholder="Du lịch / Nghỉ dưỡng"
                    value={reasonForStay}
                    onChange={(e) => setReasonForStay(e.target.value)}
                  />
                </div>

                {/* ĐƯA THỜI GIAN NHẬN/TRẢ PHÒNG VÀO LƯỚI 2 CỘT CHUẨN FIGMA DESIGN TOKENS */}
                <div>
                  <label htmlFor="guest-check-in-time" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Giờ nhận phòng <span className="text-red-500">*</span>
                  </label>
                  <Input
                    id="guest-check-in-time"
                    aria-label="Giờ nhận phòng"
                    type="datetime-local"
                    required
                    value={checkInTime}
                    onChange={(e) => setCheckInTime(e.target.value)}
                  />
                </div>

                <div>
                  <label htmlFor="guest-expected-check-out-time" className="block text-xs font-semibold text-neutral-700 mb-1">
                    Giờ dự kiến trả phòng <span className="text-red-500">*</span>
                  </label>
                  <Input
                    id="guest-expected-check-out-time"
                    aria-label="Giờ dự kiến trả phòng"
                    type="datetime-local"
                    required
                    value={expectedCheckOutTime}
                    onChange={(e) => setExpectedCheckOutTime(e.target.value)}
                  />
                </div>
              </div>

              {/* HỘP CHECKBOX XÁC NHẬN OCR */}
              {ocrDraft && (
                <label className="flex items-start gap-2.5 p-3 bg-white border border-amber-300 rounded-xl cursor-pointer hover:bg-amber-50/50 transition-colors shadow-2xs">
                  <input
                    type="checkbox"
                    className="w-4 h-4 mt-0.5 rounded border-neutral-300 text-red-600 focus:ring-red-600 cursor-pointer"
                    checked={humanVerified}
                    onChange={(e) => setHumanVerified(e.target.checked)}
                  />
                  <span className="text-xs font-semibold text-neutral-900 leading-snug">
                    Tôi đã đối chiếu giấy tờ gốc và kiểm tra, sửa thông tin OCR.
                  </span>
                </label>
              )}
            </fieldset>

            {/* THANH ĐIỀU KHIỂN HÀNH ĐỘNG FORM */}
            <div className="flex justify-end gap-2 pt-3 border-t border-neutral-200">
              <Button type="button" variant="outline" size="sm" onClick={resetForm} disabled={isSubmitting}>
                Hủy
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="sm"
                disabled={isSubmitting || isReadingImage || (!!ocrDraft && !humanVerified)}
                className="bg-red-600 hover:bg-red-700 text-white font-semibold shadow-xs"
              >
                {isSubmitting ? 'Đang lưu...' : 'Lưu khách lưu trú'}
              </Button>
            </div>
          </form>
        )}

        <div className="pt-2 flex justify-end">
          <Button variant="outline" onClick={() => { resetForm(); onClose(); }}>
            Đóng
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default StayGuestsModal;
