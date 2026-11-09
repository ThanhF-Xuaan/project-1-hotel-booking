import React, { useState, useRef } from 'react';
import { Upload, X, AlertCircle, Image as ImageIcon, Loader2 } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import { mediaService } from '../services/media.service';
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
  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>(initialHotelId ?? '');
  const [referenceId, setReferenceId] = useState<string>(initialReferenceId ? String(initialReferenceId) : '');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState<boolean>(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

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

    setIsUploading(true);
    setErrorMsg(null);

    try {
      const hId = selectedHotelId ? Number(selectedHotelId) : undefined;
      const refId = referenceId.trim() ? Number(referenceId.trim()) : undefined;

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

  const isHotelRequired = selectedCategory === 'HOTEL_PROPERTY_PHOTO' || selectedCategory === 'HOTEL_ROOM_PHOTO';

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
              onChange={(e) => setSelectedCategory(e.target.value as StorageCategory)}
              className="w-full h-10 rounded-xl border border-neutral-300 bg-white px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600"
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
            <select
              value={selectedHotelId}
              onChange={(e) => setSelectedHotelId(e.target.value ? Number(e.target.value) : '')}
              required={isHotelRequired}
              className="w-full h-10 rounded-xl border border-neutral-300 bg-white px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">-- Chọn khách sạn cơ sở --</option>
              {hotels.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {selectedCategory === 'HOTEL_ROOM_PHOTO' && (
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              ID Hạng Phòng Cơ Sở (Reference ID)
            </label>
            <input
              type="number"
              value={referenceId}
              onChange={(e) => setReferenceId(e.target.value)}
              placeholder="Nhập ID cấu hình hạng phòng (hotelRoomTypeId)..."
              className="w-full h-10 rounded-xl border border-neutral-300 px-3 text-xs text-neutral-800 focus:outline-none focus:ring-2 focus:ring-red-600"
            />
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

