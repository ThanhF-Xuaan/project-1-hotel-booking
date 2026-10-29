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
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { extractIdentityImage, ocrFields, type IdentityOcrDraft } from '../../operation/services/identityOcr.service';
import stayGuestService from '../../operation/services/stayGuest.service';
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
  hotelId: number;
  bookingId?: number;
  roomId?: number;
  roomNumber?: string;
}

export const StayGuestsModal: React.FC<StayGuestsModalProps> = ({
  isOpen,
  onClose,
  bookingRoomId,
  hotelId,
  bookingId,
  roomId,
  roomNumber,
}) => {
  const [guests, setGuests] = useState<StayGuestResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);

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
  const [checkInTime, setCheckInTime] = useState('');
  const [expectedCheckOutTime, setExpectedCheckOutTime] = useState('');
  const ocrRequest = useRef<AbortController | null>(null);

  useEffect(() => () => { ocrRequest.current?.abort(); }, [isOpen, bookingRoomId]);

  const readIdentityImage = async (file?: File) => {
    if (!file || isSubmitting) return;
    ocrRequest.current?.abort();
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
      setFullName(draft.fullName.value ?? '');
      setDateOfBirth(draft.dateOfBirth.value ?? '');
      setGender(draft.gender.value ?? 'OTHER');
      setNationality(draft.nationality.value ?? '');
      setDocumentType(draft.documentType.value ?? 'OTHER');
      setDocumentNumber(draft.documentNumber.value ?? '');
      setPermanentAddress(draft.permanentAddress.value ?? '');
    } catch {
      if (!request.signal.aborted) setErrorMessage('Không thể đọc ảnh. Kiểm tra định dạng, giới hạn 5 MiB hoặc nhập thủ công.');
    } finally {
      if (!request.signal.aborted) setIsReadingImage(false);
    }
  };

  const resetForm = useCallback(() => {
    ocrRequest.current?.abort();
    setIsReadingImage(false);
    setOcrDraft(null);
    setHumanVerified(false);
    setCheckInTime('');
    setExpectedCheckOutTime('');
    setFullName('');
    setDocumentType('CCCD');
    setDocumentNumber('');
    setDateOfBirth('');
    setGender('MALE');
    setNationality('Việt Nam');
    setPermanentAddress('');
    setReasonForStay('Du lịch / Nghỉ dưỡng');
    setShowAddForm(false);
  }, []);

  const loadGuests = useCallback(async () => {
    if (!bookingRoomId) return;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const res = await stayGuestService.getByBookingRoomId(bookingRoomId);
      if (res.result) {
        setGuests(res.result);
      }
    } catch (err: unknown) {
      console.error('Lỗi nạp danh sách khách lưu trú:', err);
      setErrorMessage('Không thể tải danh sách khách lưu trú.');
    } finally {
      setIsLoading(false);
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
      setSuccessMessage('Khai báo khách lưu trú thành công!');
      resetForm();
      loadGuests();
    } catch (err: unknown) {
      void err; // Không ghi payload giấy tờ hoặc lỗi Axios vào console.
      setErrorMessage('Không thể thêm thông tin khách lưu trú. Vui lòng kiểm tra lại.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteGuest = async (id: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa thông tin khách lưu trú này?')) return;
    try {
      await stayGuestService.delete([id]);
      setSuccessMessage('Đã xóa thông tin khách lưu trú.');
      loadGuests();
    } catch (err) {
      console.error('Lỗi xóa khách lưu trú:', err);
      setErrorMessage('Không thể xóa khách lưu trú.');
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={() => { resetForm(); onClose(); }}
      title={`Khai báo Khách lưu trú - Phòng ${roomNumber || 'Chưa xếp'}`}
      maxWidth="2xl"
    >
      <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
        {/* Banner thông tin lưu trú BCA */}
        <div className="p-3.5 bg-blue-50 border border-blue-200 rounded-xl flex items-start gap-3">
          <ShieldCheck className="w-5 h-5 text-blue-600 shrink-0 mt-0.5" />
          <div className="text-xs text-blue-900 leading-relaxed">
            <span className="font-bold">Đồng bộ lưu trú Bộ Công An (BCA):</span> Khai báo đầy đủ số CCCD/Hộ chiếu,
            ngày sinh và địa chỉ thường trú của từng khách lưu trú. Khi Check-in hoàn tất, hệ thống sẽ tự động đồng bộ
            dữ liệu vào hàng chờ khai báo lưu trú điện tử.
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

        {/* Danh sách khách lưu trú hiện tại */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Users className="w-4 h-4 text-neutral-600" />
              <h4 className="text-sm font-bold text-neutral-900">
                Khách lưu trú tại phòng ({guests.length})
              </h4>
            </div>
            {!showAddForm && (
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
                          {g.documentType}: {g.documentNumber}
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

                  <button
                    onClick={() => handleDeleteGuest(g.id)}
                    className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg shrink-0 transition-colors"
                    title="Xóa thông tin khách"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
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

            <fieldset disabled={isReadingImage || isSubmitting} className="space-y-3">
              <label className="block text-sm font-semibold">
                Đọc ảnh CCCD / Hộ chiếu (PNG, JPEG, WebP; tối đa 5 MiB)
                <input aria-label="Ảnh giấy tờ" type="file" accept="image/png,image/jpeg,image/webp"
                  onChange={(event) => { const file = event.target.files?.[0]; event.target.value = ''; void readIdentityImage(file); }} />
              </label>
              {ocrDraft && <div role="status" className="text-sm space-y-1">
                <p>Bản nháp OCR — đối chiếu giấy tờ gốc và sửa các trường trước khi lưu.</p>
                {Object.entries(ocrFields).map(([key, label]) => {
                  const field = ocrDraft[key as keyof typeof ocrFields];
                  return <p key={key}>{label}: {Math.round(field.confidence * 100)}%{!field.valid ? ' — Cần kiểm tra' : ''}</p>;
                })}
                {ocrDraft.warnings.map((warning, index) => <p key={index}>{warning}</p>)}
              </div>}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Họ và tên khách <span className="text-red-500">*</span>
                </label>
                <Input
                  placeholder="NGUYỄN VĂN A"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value.toUpperCase())}
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Giới tính
                </label>
                <select
                  className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                  value={gender}
                  onChange={(e) => setGender(e.target.value as Gender)}
                >
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                  <option value="OTHER">Khác</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Loại giấy tờ
                </label>
                <select
                  className="w-full h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                  value={documentType}
                  onChange={(e) => setDocumentType(e.target.value as DocumentType)}
                >
                  <option value="CCCD">Căn cước công dân (CCCD)</option>
                  <option value="PASSPORT">Hộ chiếu (Passport)</option>
                  <option value="CMND">Chứng minh nhân dân (CMND)</option>
                  <option value="DRIVER_LICENSE">Giấy phép lái xe</option>
                  <option value="OTHER">Khác</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Số giấy tờ (CCCD/Passport)
                </label>
                <Input
                  required
                  placeholder="001200001234"
                  value={documentNumber}
                  onChange={(e) => setDocumentNumber(e.target.value)}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Ngày sinh
                </label>
                <Input
                  type="date"
                  value={dateOfBirth}
                  onChange={(e) => setDateOfBirth(e.target.value)}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Quốc tịch
                </label>
                <Input
                  placeholder="Việt Nam"
                  value={nationality}
                  onChange={(e) => setNationality(e.target.value)}
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Địa chỉ thường trú (theo CCCD)
                </label>
                <Input
                  placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
                  value={permanentAddress}
                  onChange={(e) => setPermanentAddress(e.target.value)}
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Lý do lưu trú
                </label>
                <Input
                  placeholder="Du lịch / Nghỉ dưỡng"
                  value={reasonForStay}
                  onChange={(e) => setReasonForStay(e.target.value)}
                />
              </div>
            </div>

              <label className="block text-sm">Giờ nhận phòng
                <input aria-label="Giờ nhận phòng" type="datetime-local" required value={checkInTime} onChange={e => setCheckInTime(e.target.value)} />
              </label>
              <label className="block text-sm">Giờ dự kiến trả phòng
                <input aria-label="Giờ dự kiến trả phòng" type="datetime-local" required value={expectedCheckOutTime} onChange={e => setExpectedCheckOutTime(e.target.value)} />
              </label>
              {ocrDraft && <label className="block text-sm">
                <input type="checkbox" checked={humanVerified} onChange={e => setHumanVerified(e.target.checked)} />
                Tôi đã đối chiếu giấy tờ gốc và kiểm tra, sửa thông tin OCR.
              </label>}
            </fieldset>
            {isReadingImage && <p role="status">Đang đọc ảnh...</p>}
            <div className="flex justify-end gap-2 pt-2 border-t border-neutral-200">
              <Button type="button" variant="outline" size="sm" onClick={resetForm}>
                Hủy
              </Button>
              <Button type="submit" variant="primary" size="sm" disabled={isSubmitting || isReadingImage || (!!ocrDraft && !humanVerified)}>
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
