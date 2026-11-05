import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Camera,
  Upload,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  ShieldCheck,
  CreditCard,
  User,
  Calendar,
  MapPin,
  X,
  FileText,
  Sparkles,
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import {
  extractIdentityImage,
  ocrFields,
  type IdentityOcrDraft,
} from '../services/identityOcr.service';
import type { DocumentType, Gender } from '../types/stayGuest.types';

export interface ExtractedOcrResult {
  fullName: string;
  documentType: DocumentType;
  documentNumber: string;
  dateOfBirth: string;
  gender: Gender;
  nationality: string;
  permanentAddress: string;
}

export interface OcrIdentityScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApply: (data: ExtractedOcrResult) => void;
  title?: string;
  targetDescription?: string;
}

export const OcrIdentityScannerModal: React.FC<OcrIdentityScannerModalProps> = ({
  isOpen,
  onClose,
  onApply,
  title = 'Quét & Nhận diện CCCD / Hộ chiếu (OCR)',
  targetDescription = 'Tải ảnh CCCD / Hộ chiếu để trích xuất tự động thông tin khách.',
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isReadingImage, setIsReadingImage] = useState(false);
  const [ocrDraft, setOcrDraft] = useState<IdentityOcrDraft | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [humanVerified, setHumanVerified] = useState(false);

  // Form states (editable overrides)
  const [fullName, setFullName] = useState('');
  const [documentType, setDocumentType] = useState<DocumentType>('CCCD');
  const [documentNumber, setDocumentNumber] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState<Gender>('MALE');
  const [nationality, setNationality] = useState('Việt Nam');
  const [permanentAddress, setPermanentAddress] = useState('');

  const ocrRequest = useRef<AbortController | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Clean up object URL when component unmounts or previewUrl changes
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const resetState = useCallback(() => {
    ocrRequest.current?.abort();
    setIsReadingImage(false);
    setOcrDraft(null);
    setErrorMessage(null);
    setHumanVerified(false);
    setFile(null);
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
    }
    setFullName('');
    setDocumentType('CCCD');
    setDocumentNumber('');
    setDateOfBirth('');
    setGender('MALE');
    setNationality('Việt Nam');
    setPermanentAddress('');
  }, [previewUrl]);

  useEffect(() => {
    if (!isOpen) {
      resetState();
    }
  }, [isOpen, resetState]);

  const handleProcessImage = async (selectedFile: File) => {
    if (!selectedFile) return;

    // Validate mime-type
    const validTypes = ['image/png', 'image/jpeg', 'image/webp'];
    if (!validTypes.includes(selectedFile.type)) {
      setErrorMessage('Định dạng ảnh không được hỗ trợ. Vui lòng chọn ảnh PNG, JPEG hoặc WebP.');
      return;
    }

    // Validate size <= 5MB
    if (selectedFile.size > 5 * 1024 * 1024) {
      setErrorMessage('Kích thước ảnh vượt quá 5 MiB. Vui lòng chọn file nhẹ hơn.');
      return;
    }

    // Revoke previous URL if any
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    const objectUrl = URL.createObjectURL(selectedFile);
    setFile(selectedFile);
    setPreviewUrl(objectUrl);
    setErrorMessage(null);
    setHumanVerified(false);
    setIsReadingImage(true);

    ocrRequest.current?.abort();
    const request = new AbortController();
    ocrRequest.current = request;

    try {
      const draft = await extractIdentityImage(selectedFile, request.signal);
      if (request.signal.aborted) return;

      if (draft.status === 'UNREADABLE_DOCUMENT' || draft.status === 'INVALID_FILE') {
        setErrorMessage('Không nhận diện được nội dung giấy tờ. Vui lòng thử ảnh rõ nét hơn hoặc điền thủ công.');
        return;
      }

      setOcrDraft(draft);

      // Auto-populate form fields from OCR extraction
      if (draft.fullName?.value) setFullName(draft.fullName.value.toUpperCase());
      if (draft.documentNumber?.value) setDocumentNumber(draft.documentNumber.value);
      if (draft.dateOfBirth?.value) setDateOfBirth(draft.dateOfBirth.value);
      if (draft.gender?.value) setGender(draft.gender.value);
      if (draft.nationality?.value) setNationality(draft.nationality.value);
      if (draft.documentType?.value) setDocumentType(draft.documentType.value);
      if (draft.permanentAddress?.value) setPermanentAddress(draft.permanentAddress.value);
    } catch {
      if (!request.signal.aborted) {
        setErrorMessage('Không thể đọc ảnh qua dịch vụ OCR. Vui lòng kiểm tra ảnh hoặc nhập thủ công.');
      }
    } finally {
      if (!request.signal.aborted) {
        setIsReadingImage(false);
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0];
    if (selected) {
      void handleProcessImage(selected);
    }
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    const droppedFile = e.dataTransfer.files?.[0];
    if (droppedFile) {
      void handleProcessImage(droppedFile);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleApply = (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      setErrorMessage('Họ và tên khách không được để trống.');
      return;
    }

    if (ocrDraft && !humanVerified) {
      setErrorMessage('Vui lòng đối chiếu với giấy tờ gốc và tích xác nhận trước khi áp dụng.');
      return;
    }

    onApply({
      fullName: fullName.trim(),
      documentType,
      documentNumber: documentNumber.trim(),
      dateOfBirth,
      gender,
      nationality: nationality.trim() || 'Việt Nam',
      permanentAddress: permanentAddress.trim(),
    });

    onClose();
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      description={targetDescription}
      maxWidth="4xl"
    >
      <div className="space-y-4 max-h-[80vh] overflow-y-auto pr-1">
        {/* Compliance and Instruction Banner */}
        <div className="p-3 bg-red-50 border border-red-200 rounded-xl flex items-start gap-2.5 text-xs text-red-900">
          <ShieldCheck className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            <span className="font-bold">Quy chuẩn Trích xuất OCR:</span> Ảnh giấy tờ được xử lý tức thời trong bộ nhớ RAM và không lưu ảnh vào hệ thống. Lễ tân cần đối chiếu bản nháp với thẻ căn cước gốc trước khi áp dụng để đảm bảo độ chính xác cho hồ sơ BCA.
          </div>
        </div>

        {errorMessage && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
              <span>{errorMessage}</span>
            </div>
            <button
              type="button"
              onClick={() => setErrorMessage(null)}
              className="font-bold text-red-800 hover:underline cursor-pointer"
            >
              ✕
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
          {/* Left Column: Image Upload & Preview Pane (5 cols) */}
          <div className="md:col-span-5 space-y-3">
            <div className="text-xs font-bold text-neutral-800 flex items-center justify-between">
              <span>Ảnh giấy tờ (CCCD / Passport)</span>
              {previewUrl && (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="text-red-600 hover:text-red-700 underline text-xs font-medium cursor-pointer"
                >
                  Chọn ảnh khác
                </button>
              )}
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={handleFileChange}
              className="hidden"
            />

            {!previewUrl ? (
              <div
                onDrop={handleDrop}
                onDragOver={handleDragOver}
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-neutral-300 hover:border-red-500 rounded-2xl p-6 text-center cursor-pointer transition-colors bg-neutral-50 hover:bg-neutral-100/70 flex flex-col items-center justify-center min-h-[220px]"
              >
                <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mb-3">
                  <Camera className="w-6 h-6" />
                </div>
                <p className="text-sm font-bold text-neutral-800">
                  Nhấn để tải ảnh hoặc kéo thả vào đây
                </p>
                <p className="text-xs text-neutral-500 mt-1">
                  Hỗ trợ PNG, JPG, WebP (Tối đa 5 MiB)
                </p>
                <div className="mt-4 inline-flex items-center gap-1.5 px-3 py-1.5 bg-white border border-neutral-200 rounded-lg text-xs font-semibold text-neutral-700 shadow-xs">
                  <Upload className="w-3.5 h-3.5" />
                  Duyệt file từ máy
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                <div className="relative border border-neutral-200 rounded-2xl overflow-hidden bg-black/5 flex items-center justify-center max-h-[260px]">
                  <img
                    src={previewUrl}
                    alt="Giấy tờ tùy thân"
                    className="object-contain w-full max-h-[260px] rounded-2xl"
                  />
                  {isReadingImage && (
                    <div className="absolute inset-0 bg-neutral-900/60 backdrop-blur-xs flex flex-col items-center justify-center text-white p-4">
                      <RefreshCw className="w-8 h-8 animate-spin text-red-500 mb-2" />
                      <p className="text-sm font-bold">Đang nhận diện ký tự OCR...</p>
                      <p className="text-xs text-neutral-300 mt-1">Vui lòng chờ giây lát</p>
                    </div>
                  )}
                </div>
                <div className="flex items-center justify-between text-xs text-neutral-500">
                  <span className="truncate max-w-[200px] font-medium">{file?.name}</span>
                  <span>{file ? (file.size / 1024).toFixed(0) + ' KB' : ''}</span>
                </div>
              </div>
            )}

            {/* OCR Confidence Summary Badge list */}
            {ocrDraft && (
              <div className="p-3 bg-neutral-50 border border-neutral-200 rounded-xl space-y-2 text-xs">
                <div className="flex items-center justify-between font-bold text-neutral-800">
                  <span className="flex items-center gap-1">
                    <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                    Độ tin cậy OCR
                  </span>
                  <span
                    className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                      ocrDraft.status === 'SUCCESS'
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {ocrDraft.status === 'SUCCESS' ? 'Độ chính xác cao' : 'Cần rà soát'}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-1.5 pt-1 text-[11px]">
                  {Object.entries(ocrFields).map(([key, label]) => {
                    const fieldObj = ocrDraft[key as keyof typeof ocrFields];
                    if (!fieldObj) return null;
                    const pct = Math.round(fieldObj.confidence * 100);
                    const isGood = fieldObj.valid && pct >= 70;
                    return (
                      <div
                        key={key}
                        className="flex items-center justify-between bg-white px-2 py-1 rounded-md border border-neutral-100"
                      >
                        <span className="text-neutral-600 truncate">{label}:</span>
                        <span
                          className={`font-mono font-bold ${
                            isGood ? 'text-emerald-600' : 'text-amber-600'
                          }`}
                        >
                          {pct}%
                        </span>
                      </div>
                    );
                  })}
                </div>

                {ocrDraft.warnings.length > 0 && (
                  <div className="pt-1 border-t border-neutral-200 space-y-1 text-neutral-500 text-[11px]">
                    {ocrDraft.warnings.map((w, idx) => (
                      <div key={idx} className="flex items-center gap-1 text-amber-700">
                        <AlertCircle className="w-3 h-3 shrink-0" />
                        <span>{w}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Right Column: Editable Extracted Fields Form (7 cols) */}
          <div className="md:col-span-7">
            <form onSubmit={handleApply} className="space-y-3">
              <div className="flex items-center justify-between border-b border-neutral-200 pb-2">
                <h4 className="text-sm font-bold text-neutral-900 flex items-center gap-1.5">
                  <FileText className="w-4 h-4 text-red-600" />
                  Dữ liệu trích xuất (Có thể chỉnh sửa)
                </h4>
                {ocrDraft && (
                  <span className="text-xs text-neutral-500">
                    Đối chiếu với giấy tờ gốc
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Họ và tên */}
                <div className="sm:col-span-2">
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Họ và tên khách <span className="text-red-500">*</span>
                  </label>
                  <Input
                    placeholder="NGUYỄN VĂN A"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value.toUpperCase())}
                    required
                    leftIcon={<User className="w-4 h-4" />}
                  />
                </div>

                {/* Loại giấy tờ */}
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Loại giấy tờ
                  </label>
                  <select
                    className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
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

                {/* Số giấy tờ */}
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Số CCCD / Hộ chiếu
                  </label>
                  <Input
                    placeholder="001200001234"
                    value={documentNumber}
                    onChange={(e) => setDocumentNumber(e.target.value)}
                    leftIcon={<CreditCard className="w-4 h-4" />}
                  />
                </div>

                {/* Ngày sinh */}
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Ngày sinh (YYYY-MM-DD)
                  </label>
                  <Input
                    type="date"
                    value={dateOfBirth}
                    onChange={(e) => setDateOfBirth(e.target.value)}
                    leftIcon={<Calendar className="w-4 h-4" />}
                  />
                </div>

                {/* Giới tính */}
                <div>
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Giới tính
                  </label>
                  <select
                    className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                    value={gender}
                    onChange={(e) => setGender(e.target.value as Gender)}
                  >
                    <option value="MALE">Nam</option>
                    <option value="FEMALE">Nữ</option>
                    <option value="OTHER">Khác</option>
                  </select>
                </div>

                {/* Quốc tịch */}
                <div className="sm:col-span-2">
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Quốc tịch
                  </label>
                  <Input
                    placeholder="Việt Nam"
                    value={nationality}
                    onChange={(e) => setNationality(e.target.value)}
                  />
                </div>

                {/* Địa chỉ thường trú */}
                <div className="sm:col-span-2">
                  <label className="block text-xs font-semibold text-neutral-700 mb-1">
                    Địa chỉ thường trú (theo CCCD)
                  </label>
                  <Input
                    placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
                    value={permanentAddress}
                    onChange={(e) => setPermanentAddress(e.target.value)}
                    leftIcon={<MapPin className="w-4 h-4" />}
                  />
                </div>
              </div>

              {/* Human Verification Checkbox */}
              {ocrDraft && (
                <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 mt-2">
                  <label className="flex items-start gap-2.5 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={humanVerified}
                      onChange={(e) => setHumanVerified(e.target.checked)}
                      className="mt-0.5 rounded text-red-600 focus:ring-red-500"
                    />
                    <span className="text-xs text-neutral-800 leading-snug">
                      Tôi xác nhận đã <strong>đối chiếu kỹ với giấy tờ gốc</strong> của khách hàng và đảm bảo các thông tin trên là chính xác.
                    </span>
                  </label>
                </div>
              )}

              {/* Action buttons */}
              <div className="flex justify-end gap-2 pt-3 border-t border-neutral-200">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={onClose}
                  disabled={isReadingImage}
                >
                  Hủy bỏ
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  disabled={isReadingImage || !fullName.trim() || (!!ocrDraft && !humanVerified)}
                  className="bg-red-600 hover:bg-red-700 text-white font-bold"
                >
                  <CheckCircle2 className="w-4 h-4 mr-1.5" />
                  Áp dụng & Điền tự động
                </Button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Modal>
  );
};

export default OcrIdentityScannerModal;

