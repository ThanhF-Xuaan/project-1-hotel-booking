import React, { useState } from 'react';
import {
  Copy,
  Check,
  Star,
  Trash2,
  ExternalLink,
} from 'lucide-react';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import type { MediaItemDto } from '../types/media.types';

interface MediaDetailModalProps {
  media: MediaItemDto | null;
  onClose: () => void;
  onSetPrimary: (id: number) => Promise<void>;
  onDelete: (id: number) => Promise<void>;
}

export const MediaDetailModal: React.FC<MediaDetailModalProps> = ({
  media,
  onClose,
  onSetPrimary,
  onDelete,
}) => {
  const [copied, setCopied] = useState<boolean>(false);
  const [isSettingPrimary, setIsSettingPrimary] = useState<boolean>(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<boolean>(false);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  if (!media) return null;

  const handleCopyUrl = async () => {
    try {
      await navigator.clipboard.writeText(media.url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
      setCopied(false);
    }
  };

  const handleSetPrimary = async () => {
    setIsSettingPrimary(true);
    try {
      await onSetPrimary(media.id);
      onClose();
    } finally {
      setIsSettingPrimary(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await onDelete(media.id);
      setShowDeleteConfirm(false);
      onClose();
    } finally {
      setIsDeleting(false);
    }
  };

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  return (
    <>
      <Modal
        isOpen={Boolean(media)}
        onClose={onClose}
        title="Chi Tiết Tệp Đa Phương Tiện"
        description={media.originalFilename}
        maxWidth="2xl"
      >
        <div className="space-y-4">
          {/* Large Image Preview */}
          <div className="relative w-full h-72 sm:h-80 bg-neutral-900 rounded-2xl overflow-hidden flex items-center justify-center">
            <img
              src={media.url}
              alt={media.originalFilename}
              className="max-h-full max-w-full object-contain"
            />
            {media.isPrimary && (
              <span className="absolute top-3 left-3 px-2.5 py-1 bg-amber-500 text-white rounded-full text-xs font-bold flex items-center gap-1 shadow-md">
                <Star className="w-3.5 h-3.5 fill-current" />
                Ảnh chính
              </span>
            )}
          </div>

          {/* Quick Actions */}
          <div className="flex flex-wrap items-center justify-between gap-2 p-3 bg-neutral-50 rounded-xl border border-neutral-200">
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="outline"
                onClick={handleCopyUrl}
                className="rounded-xl text-xs h-9"
              >
                {copied ? (
                  <>
                    <Check className="w-3.5 h-3.5 mr-1 text-emerald-600" />
                    Đã sao chép liên kết
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5 mr-1 text-neutral-500" />
                    Sao chép URL
                  </>
                )}
              </Button>
              <a
                href={media.url}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-neutral-300 bg-white text-xs font-medium text-neutral-700 hover:bg-neutral-50 h-9 transition-colors"
              >
                <ExternalLink className="w-3.5 h-3.5" />
                Mở ảnh gốc
              </a>
            </div>

            <div className="flex items-center gap-2">
              {!media.isPrimary && (
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={isSettingPrimary}
                  onClick={handleSetPrimary}
                  className="rounded-xl text-xs h-9 font-semibold text-amber-700 hover:bg-amber-50 border border-amber-200"
                >
                  <Star className="w-3.5 h-3.5 mr-1" />
                  Đặt làm ảnh chính
                </Button>
              )}
              <Button
                size="sm"
                variant="danger"
                onClick={() => setShowDeleteConfirm(true)}
                className="rounded-xl text-xs h-9 bg-red-600 hover:bg-red-700 text-white"
              >
                <Trash2 className="w-3.5 h-3.5 mr-1" />
                Xóa ảnh
              </Button>
            </div>
          </div>

          {/* Metadata Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div className="p-3 bg-white rounded-xl border border-neutral-200 space-y-1">
              <span className="text-neutral-400 font-medium">Danh mục & Khách sạn</span>
              <div className="font-semibold text-neutral-900">{media.categoryLabel}</div>
              {media.hotelName && (
                <div className="text-neutral-500">{media.hotelName} (ID: {media.hotelId})</div>
              )}
              {media.referenceId && (
                <div className="text-neutral-500">Mã tham chiếu: {media.referenceId}</div>
              )}
            </div>

            <div className="p-3 bg-white rounded-xl border border-neutral-200 space-y-1">
              <span className="text-neutral-400 font-medium">Định dạng & Dung lượng</span>
              <div className="font-semibold text-neutral-900">{media.contentType}</div>
              <div className="text-neutral-500">{formatBytes(media.sizeBytes)}</div>
            </div>

            <div className="p-3 bg-white rounded-xl border border-neutral-200 space-y-1">
              <span className="text-neutral-400 font-medium">Bảo mật & Người tải</span>
              <div className="font-semibold text-neutral-900">{media.accessLevel}</div>
              <div className="text-neutral-500">Tải lên bởi: {media.uploadedBy || 'system'}</div>
            </div>

            <div className="p-3 bg-white rounded-xl border border-neutral-200 space-y-1">
              <span className="text-neutral-400 font-medium">Thời gian tạo</span>
              <div className="font-semibold text-neutral-900">
                {new Date(media.createdAt).toLocaleString('vi-VN')}
              </div>
            </div>
          </div>

          {/* Storage Key & Checksum */}
          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 text-xs space-y-1.5 font-mono">
            <div>
              <span className="text-neutral-500">Storage Key:</span>{' '}
              <span className="text-neutral-800 break-all">{media.storageKey}</span>
            </div>
            {media.checksumSha256 && (
              <div>
                <span className="text-neutral-500">SHA-256:</span>{' '}
                <span className="text-neutral-600 break-all">{media.checksumSha256}</span>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <ConfirmModal
          isOpen={showDeleteConfirm}
          onClose={() => setShowDeleteConfirm(false)}
          onConfirm={handleDelete}
          isLoading={isDeleting}
          variant="danger"
          title="Xác nhận xóa tệp hình ảnh"
          description={`Bạn có chắc chắn muốn xóa tệp "${media.originalFilename}" khỏi hệ thống lưu trữ? Hành động này không thể hoàn tác.`}
          confirmText="Xác nhận Xóa"
          cancelText="Hủy bỏ"
        />
      )}
    </>
  );
};

export default MediaDetailModal;

