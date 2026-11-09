import React, { useState, useRef, useEffect } from 'react';
import { Upload, X, AlertCircle, Image as ImageIcon, Loader2 } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import { mediaService } from '../services/media.service';
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service';
import type { HotelRoomTypeDto } from '../../inventory/types/inventory.types';
import { formatCurrency } from '../../../core/utils/currency';
import type { CategoryMetaDto, StorageCategory } from '../types/media.types';

interface MediaUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  categories: CategoryMetaDto[];
  hotels: { id: number; name: string }[];
  initialCategory?: StorageCategory;
  initialHotelId?: number;
  initialReferenceId?: number;
}

const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

export const MediaUploadModal: React.FC<MediaUploadModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  categories,
  hotels,
  initialCategory = 'HOTEL_PROPERTY_PHOTO',
  initialHotelId,
  initialReferenceId,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<StorageCategory>(initialCategory);
  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>(
    initialCategory === 'BRAND_ASSET' ? '' : (initialHotelId ?? '')
  );
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState<number | ''>(initialReferenceId ?? '');
  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([]);
  const [isLoadingRoomTypes, setIsLoadingRoomTypes] = useState<boolean>(false);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState<boolean>(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const isBrandAsset = selectedCategory === 'BRAND_ASSET';
  const isRoomPhoto = selectedCategory === 'HOTEL_ROOM_PHOTO';
  const isPropertyPhoto = selectedCategory === 'HOTEL_PROPERTY_PHOTO';
  const isHotelRequired = isPropertyPhoto || isRoomPhoto;

  // Lắng nghe tải danh sách hạng phòng cơ sở khi chọn HOTEL_ROOM_PHOTO
  useEffect(() => {
    let isMounted = true;
    const timer = setTimeout(() => {
      if (selectedCategory !== 'HOTEL_ROOM_PHOTO' || !selectedHotelId) {
        setRoomTypes([]);
        return;
      }

      setIsLoadingRoomTypes(true);
      hotelRoomTypeService
        .filter({ hotelId: Number(selectedHotelId), pageSize: 100, status: 'ACTIVE' })
        .then((res) => {
          if (isMounted && res && res.result) {
            setRoomTypes(res.result.content || []);
          } else if (isMounted) {
            setRoomTypes([]);
          }
        })
        .catch((err) => {
          console.error('Lỗi khi tải danh sách hạng phòng:', err);
          if (isMounted) setRoomTypes([]);
        })
        .finally(() => {
          if (isMounted) setIsLoadingRoomTypes(false);
        });
    }, 0);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [selectedCategory, selectedHotelId]);

  if (!isOpen) return null;

  const handleCategoryChange = (newCat: StorageCategory) => {
    setSelectedCategory(newCat);
    setErrorMsg(null);
    if (newCat === 'BRAND_ASSET') {
      setSelectedHotelId('');
      setSelectedRoomTypeId('');
    } else if (newCat !== 'HOTEL_ROOM_PHOTO') {
      setSelectedRoomTypeId('');
    }
  };

  const handleHotelChange = (newHotelId: number | '') => {
    setSelectedHotelId(newHotelId);
    setSelectedRoomTypeId('');
    setErrorMsg(null);
  };

  const handleFilesSelected = (files: FileList | null) => {
    if (!files || files.length === 0) return;
    setErrorMsg(null);

    const validFiles: File[] = [];
    const invalidErrors: string[] = [];

    Array.from(files).forEach((file) => {
      if (!ALLOWED_MIME_TYPES.includes(file.type)) {
        invalidErrors.push(`${file.name}: định dạng không hỗ trợ (chỉ nhận JPG, PNG, WEBP)`);
      } else if (file.size > MAX_FILE_SIZE_BYTES) {
        invalidErrors.push(`${file.name}: vượt quá dung lượng tối đa 10MB`);
      } else {
        validFiles.push(file);
      }
    });

    if (invalidErrors.length > 0) {
      setErrorMsg(invalidErrors.join('; '));
    }

    if (validFiles.length > 0) {
      setSelectedFiles((prev) => [...prev, ...validFiles]);
    }
  };

  const handleRemoveFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedFiles.length === 0) {
      setErrorMsg('Vui lòng chọn ít nhất một tệp hình ảnh để tải lên');
      return;
    }

    if (isHotelRequired && !selectedHotelId) {
      setErrorMsg('Vui lòng chọn khách sạn cơ sở!');
      return;
    }

    if (isRoomPhoto && !selectedRoomTypeId) {
      setErrorMsg('Vui lòng chọn hạng phòng cụ thể cho ảnh này!');
      return;
    }

    setIsUploading(true);
    setErrorMsg(null);

    try {
      const hId = isBrandAsset || !selectedHotelId ? undefined : Number(selectedHotelId);
      const refId = isRoomPhoto && selectedRoomTypeId ? Number(selectedRoomTypeId) : undefined;

      await mediaService.upload(selectedCategory, selectedFiles, hId, refId);
      setSelectedFiles([]);
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMsg(axiosErr?.response?.data?.message || (err instanceof Error ? err.message : 'Tải lên hình ảnh thất bại'));
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={() => !isUploading && onClose()}
      title="Tải Lên Hình Ảnh Vào Kho Đa Phương Tiện"
      description="Hỗ trợ tải lên nhiều tệp ảnh định dạng JPG, PNG, WebP (Tối đa 10MB/ảnh)"
      maxWidth="xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {errorMsg && (
          <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-700 flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 mt-0.5 shrink-0" />
            <div className="flex-1">{errorMsg}</div>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Phân Loại Danh Mục <span className="text-red-500">*</span>
            </label>
            <select
              value={selectedCategory}
              onChange={(e) => handleCategoryChange(e.target.value as StorageCategory)}
              className="w-full h-10 rounded-xl border border-neutral-300 bg-white px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600 cursor-pointer"
            >
              {categories.map((cat) => (
                <option key={cat.category} value={cat.category}>
                  {cat.label} ({cat.folderPrefix})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Khách Sạn Cơ Sở {isHotelRequired && <span className="text-red-500">*</span>}
            </label>
            {isBrandAsset ? (
              <div className="h-10 rounded-xl border border-neutral-200 bg-neutral-100 px-3 flex items-center text-xs text-neutral-500 italic">
                Tài nguyên toàn chuỗi (Không gắn cơ sở)
              </div>
            ) : (
              <select
                value={selectedHotelId}
                onChange={(e) => handleHotelChange(e.target.value ? Number(e.target.value) : '')}
                required={isHotelRequired}
                className="w-full h-10 rounded-xl border border-neutral-300 bg-white px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600 cursor-pointer"
              >
                {isHotelRequired ? (
                  <option value="">-- Chọn khách sạn cơ sở * --</option>
                ) : (
                  <option value="">-- Toàn chuỗi / Dành cho trang chủ --</option>
                )}
                {hotels.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        </div>

        {isRoomPhoto && (
          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200/80 space-y-1">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Hạng Phòng Của Cơ Sở <span className="text-red-500">*</span>
            </label>
            {!selectedHotelId ? (
              <select
                disabled
                className="w-full h-10 rounded-xl border border-neutral-200 bg-neutral-100 px-3 text-xs text-neutral-400 cursor-not-allowed"
              >
                <option>-- Vui lòng chọn khách sạn cơ sở trước --</option>
              </select>
            ) : isLoadingRoomTypes ? (
              <div className="h-10 rounded-xl border border-neutral-200 bg-white px-3 flex items-center text-xs text-neutral-500 gap-2">
                <Loader2 className="w-4 h-4 animate-spin text-red-600" />
                Đang tải danh sách hạng phòng...
              </div>
            ) : (
              <select
                value={selectedRoomTypeId}
                onChange={(e) => setSelectedRoomTypeId(e.target.value ? Number(e.target.value) : '')}
                required
                className="w-full h-10 rounded-xl border border-neutral-300 bg-white px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600 cursor-pointer"
              >
                <option value="">-- Chọn hạng phòng cụ thể * --</option>
                {roomTypes.map((rt) => (
                  <option key={rt.id} value={rt.id}>
                    {rt.roomTypeName} ({rt.roomTypeCode}) — {formatCurrency(rt.basePrice)}/đêm
                  </option>
                ))}
              </select>
            )}
            <p className="text-[11px] text-neutral-500">
              {!selectedHotelId
                ? 'Khóa lựa chọn: Bạn cần chọn khách sạn cơ sở ở trên để hiển thị các hạng phòng tương ứng.'
                : roomTypes.length === 0 && !isLoadingRoomTypes
                ? 'Cơ sở này hiện chưa có cấu hình hạng phòng nào.'
                : `Đã tìm thấy ${roomTypes.length} hạng phòng thuộc cơ sở đã chọn.`}
            </p>
          </div>
        )}

        {/* Drag & Drop Upload Zone */}
        <div
          onDragOver={(e) => {
            e.preventDefault();
            setIsDragging(true);
          }}
          onDragLeave={() => setIsDragging(false)}
          onDrop={(e) => {
            e.preventDefault();
            setIsDragging(false);
            handleFilesSelected(e.dataTransfer.files);
          }}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer transition-colors ${
            isDragging
              ? 'border-red-500 bg-red-50/50'
              : 'border-neutral-200 hover:border-red-400 hover:bg-neutral-50/50'
          }`}
        >
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept="image/jpeg,image/png,image/webp"
            onChange={(e) => handleFilesSelected(e.target.files)}
            className="hidden"
          />
          <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-2.5">
            <Upload className="w-6 h-6" />
          </div>
          <p className="text-xs font-semibold text-neutral-900">
            Kéo thả tệp ảnh vào đây hoặc nhấp để chọn tệp từ máy
          </p>
          <p className="text-[11px] text-neutral-400 mt-1">
            Hỗ trợ JPG, PNG, WebP (Tối đa 10MB/tệp)
          </p>
        </div>

        {/* File Preview List */}
        {selectedFiles.length > 0 && (
          <div className="space-y-2 max-h-48 overflow-y-auto">
            <div className="text-xs font-semibold text-neutral-700">
              Đã chọn {selectedFiles.length} tệp:
            </div>
            {selectedFiles.map((file, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-2 rounded-xl bg-neutral-50 border border-neutral-200 text-xs"
              >
                <div className="flex items-center gap-2 truncate max-w-sm">
                  <ImageIcon className="w-4 h-4 text-neutral-400 shrink-0" />
                  <span className="font-medium text-neutral-800 truncate">{file.name}</span>
                  <span className="text-[11px] text-neutral-400 shrink-0">
                    ({(file.size / (1024 * 1024)).toFixed(2)} MB)
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => handleRemoveFile(idx)}
                  className="text-neutral-400 hover:text-red-600 p-1 rounded-md"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-neutral-100">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={isUploading}
            className="rounded-xl text-xs"
          >
            Hủy bỏ
          </Button>
          <Button
            type="submit"
            disabled={isUploading || selectedFiles.length === 0}
            className="rounded-xl text-xs bg-red-600 hover:bg-red-700 text-white font-semibold"
          >
            {isUploading ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                Đang tải lên...
              </>
            ) : (
              `Tải lên (${selectedFiles.length})`
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default MediaUploadModal;

