import React, { useState, useEffect, useRef } from 'react';
import {
  Upload,
  Star,
  Trash2,
  Image as ImageIcon,
  Loader2,
  AlertCircle,
  Check,
} from 'lucide-react';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import { hotelRoomTypeService } from '../services/hotelRoomType.service';
import type { HotelImageDto } from '../../organization/types/organization.types';

interface RoomTypeMediaGalleryModalProps {
  isOpen: boolean;
  onClose: () => void;
  hotelRoomTypeId: number;
  roomTypeName: string;
  hotelName?: string;
  onCoverImageChanged?: () => void;
}

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export const RoomTypeMediaGalleryModal: React.FC<RoomTypeMediaGalleryModalProps> = ({
  isOpen,
  onClose,
  hotelRoomTypeId,
  roomTypeName,
  hotelName,
  onCoverImageChanged,
}) => {
  const [images, setImages] = useState<HotelImageDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Set primary state
  const [settingPrimaryId, setSettingPrimaryId] = useState<number | null>(null);

  // Delete state
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!isOpen || !hotelRoomTypeId) return;

    let isMounted = true;
    const timer = setTimeout(() => {
      setIsLoading(true);
      setErrorMsg(null);
      setSuccessMsg(null);

      hotelRoomTypeService
        .getImages(hotelRoomTypeId)
        .then((res) => {
          if (isMounted && res && res.result) {
            setImages(res.result);
          }
        })
        .catch((err) => {
          console.error('Lỗi khi tải danh sách ảnh hạng phòng:', err);
          if (isMounted) setErrorMsg('Không thể tải danh sách ảnh hạng phòng.');
        })
        .finally(() => {
          if (isMounted) setIsLoading(false);
        });
    }, 0);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [isOpen, hotelRoomTypeId]);

  const reloadImages = async () => {
    if (!hotelRoomTypeId) return;
    setIsLoading(true);
    try {
      const res = await hotelRoomTypeService.getImages(hotelRoomTypeId);
      if (res && res.result) {
        setImages(res.result);
      }
    } catch (err) {
      console.error('Lỗi khi tải lại ảnh hạng phòng:', err);
      setErrorMsg('Không thể tải lại danh sách ảnh.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFilesSelected = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;
    setErrorMsg(null);
    setSuccessMsg(null);

    const validFiles: File[] = [];
    const invalidNames: string[] = [];

    Array.from(fileList).forEach((file) => {
      if (!ALLOWED_MIME_TYPES.includes(file.type)) {
        invalidNames.push(`${file.name} (định dạng không hỗ trợ)`);
      } else if (file.size > MAX_FILE_SIZE_BYTES) {
        invalidNames.push(`${file.name} (> 10MB)`);
      } else {
        validFiles.push(file);
      }
    });

    if (invalidNames.length > 0) {
      setErrorMsg(`Một số tệp không hợp lệ: ${invalidNames.join(', ')}`);
    }

    if (validFiles.length === 0) return;

    setIsUploading(true);
    try {
      const res = await hotelRoomTypeService.uploadImages(hotelRoomTypeId, validFiles);
      if (res && res.result) {
        setImages(res.result);
        setSuccessMsg(`Tải lên thành công ${validFiles.length} hình ảnh!`);
        onCoverImageChanged?.();
      }
    } catch (err: unknown) {
      console.error('Lỗi tải ảnh lên:', err);
      const message =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Lỗi khi tải ảnh lên máy chủ.';
      setErrorMsg(message || 'Không thể tải ảnh lên.');
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    handleFilesSelected(e.dataTransfer.files);
  };

  const handleSetPrimary = async (imageId: number) => {
    setSettingPrimaryId(imageId);
    setErrorMsg(null);
    try {
      await hotelRoomTypeService.setPrimaryImage(hotelRoomTypeId, imageId);
      setImages((prev) =>
        prev.map((img) => ({
          ...img,
          isPrimary: img.id === imageId,
        }))
      );
      setSuccessMsg('Đã cập nhật ảnh đại diện chính của hạng phòng.');
      onCoverImageChanged?.();
    } catch (err) {
      console.error('Lỗi khi đặt ảnh đại diện:', err);
      setErrorMsg('Không thể đặt ảnh này làm ảnh đại diện.');
    } finally {
      setSettingPrimaryId(null);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deleteConfirmId) return;
    setIsDeleting(true);
    setErrorMsg(null);
    try {
      await hotelRoomTypeService.deleteImage(hotelRoomTypeId, deleteConfirmId);
      await reloadImages();
      setSuccessMsg('Xóa ảnh thành công.');
      setDeleteConfirmId(null);
      onCoverImageChanged?.();
    } catch (err) {
      console.error('Lỗi khi xóa ảnh:', err);
      setErrorMsg('Không thể xóa ảnh này.');
    } finally {
      setIsDeleting(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (!bytes) return '0 B';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title={`Thư viện ảnh hạng phòng: ${roomTypeName}`}
        description={hotelName ? `Khách sạn: ${hotelName}` : 'Quản lý hình ảnh thực tế của hạng phòng cơ sở'}
        maxWidth="4xl"
      >
        <div className="space-y-6">
          {/* Notifications */}
          {errorMsg && (
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{errorMsg}</span>
            </div>
          )}

          {successMsg && (
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs">
              <Check className="w-4 h-4 shrink-0 text-emerald-600" />
              <span>{successMsg}</span>
            </div>
          )}

          {/* Upload Dropzone */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
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
              {isUploading ? (
                <Loader2 className="w-6 h-6 animate-spin" />
              ) : (
                <Upload className="w-6 h-6" />
              )}
            </div>
            <p className="text-xs font-semibold text-neutral-900">
              {isUploading
                ? 'Đang tải ảnh lên máy chủ...'
                : 'Kéo thả ảnh vào đây, hoặc nhấn để chọn tệp'}
            </p>
            <p className="text-[11px] text-neutral-400 mt-1">
              Định dạng hỗ trợ: JPG, PNG, WebP (Tối đa 10MB mỗi ảnh)
            </p>
          </div>

          {/* Gallery Grid */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-xs font-bold text-neutral-900 flex items-center gap-1.5 uppercase tracking-wider">
                <ImageIcon className="w-4 h-4 text-neutral-500" />
                Danh sách ảnh đã lưu ({images.length})
              </h3>
            </div>

            {isLoading ? (
              <div className="py-12 text-center text-xs text-neutral-400 flex items-center justify-center gap-2">
                <Loader2 className="w-5 h-5 animate-spin text-red-600" />
                Đang tải danh sách ảnh...
              </div>
            ) : images.length === 0 ? (
              <div className="py-12 text-center rounded-2xl border border-dashed border-neutral-200 bg-neutral-50/50">
                <p className="text-xs font-medium text-neutral-500">
                  Hạng phòng này chưa có hình ảnh nào.
                </p>
                <p className="text-[11px] text-neutral-400 mt-1">
                  Hãy tải ảnh lên để hiển thị trên Cổng tìm kiếm phòng trực tuyến.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                {images.map((img) => {
                  const isSettingThisPrimary = settingPrimaryId === img.id;
                  return (
                    <div
                      key={img.id}
                      className={`group relative rounded-xl border overflow-hidden bg-neutral-900 aspect-4/3 flex items-center justify-center transition-all ${
                        img.isPrimary
                          ? 'border-amber-400 ring-2 ring-amber-400/30'
                          : 'border-neutral-200 hover:border-neutral-400'
                      }`}
                    >
                      <img
                        src={img.url}
                        alt={img.originalFilename}
                        className="w-full h-full object-cover"
                      />

                      {/* Primary Badge */}
                      {img.isPrimary && (
                        <div className="absolute top-2 left-2 px-2 py-0.5 rounded-full bg-amber-500 text-white text-[10px] font-bold flex items-center gap-1 shadow-sm z-10">
                          <Star className="w-3 h-3 fill-current" />
                          Ảnh đại diện
                        </div>
                      )}

                      {/* Hover Overlay Controls */}
                      <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-between p-2.5 z-10">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => setDeleteConfirmId(img.id)}
                            className="p-1.5 rounded-lg bg-black/40 text-neutral-200 hover:text-red-400 hover:bg-black/60 transition-colors"
                            title="Xóa ảnh này"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>

                        <div>
                          <p className="text-[10px] text-white/90 truncate font-medium">
                            {img.originalFilename}
                          </p>
                          <p className="text-[9px] text-neutral-400 mb-2 font-mono">
                            {formatFileSize(img.sizeBytes)}
                          </p>

                          {!img.isPrimary && (
                            <button
                              type="button"
                              onClick={() => handleSetPrimary(img.id)}
                              disabled={isSettingThisPrimary}
                              className="w-full py-1 px-2 rounded-lg bg-white/20 hover:bg-white/30 text-white text-[11px] font-semibold flex items-center justify-center gap-1 backdrop-blur-xs transition-colors"
                            >
                              {isSettingThisPrimary ? (
                                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                              ) : (
                                <>
                                  <Star className="w-3 h-3" />
                                  Đặt làm ảnh chính
                                </>
                              )}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="flex justify-end pt-3 border-t border-neutral-100">
            <Button variant="secondary" onClick={onClose} className="rounded-xl text-xs">
              Đóng
            </Button>
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      {deleteConfirmId && (
        <ConfirmModal
          isOpen={Boolean(deleteConfirmId)}
          onClose={() => setDeleteConfirmId(null)}
          onConfirm={handleDeleteConfirm}
          isLoading={isDeleting}
          variant="danger"
          title="Xác nhận xóa ảnh hạng phòng"
          description="Bạn có chắc chắn muốn xóa ảnh này khỏi hạng phòng? Tệp sẽ bị xóa vĩnh viễn khỏi hệ thống lưu trữ."
          confirmText="Xác nhận Xóa"
          cancelText="Hủy bỏ"
        />
      )}
    </>
  );
};

export default RoomTypeMediaGalleryModal;

