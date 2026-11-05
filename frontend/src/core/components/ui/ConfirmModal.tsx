import React, { useEffect } from 'react';
import {
  AlertTriangle,
  AlertCircle,
  Info,
  CheckCircle2,
  Trash2,
  X,
  Loader2,
} from 'lucide-react';
import { cn } from '../../utils/cn';

export type ConfirmVariant = 'danger' | 'warning' | 'info' | 'success' | 'primary';

export interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  isLoading?: boolean;
  variant?: ConfirmVariant;
  icon?: React.ReactNode;
  children?: React.ReactNode;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmText,
  cancelText = 'Hủy bỏ',
  isLoading = false,
  variant = 'danger',
  icon,
  children,
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isLoading) {
        onClose();
      }
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.style.overflow = 'unset';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, isLoading, onClose]);

  if (!isOpen) return null;

  const variantConfig = {
    danger: {
      bgIcon: 'bg-red-50 text-red-600 border border-red-200',
      defaultIcon: <Trash2 className="w-6 h-6 text-red-600" />,
      confirmBtn: 'bg-red-600 hover:bg-red-700 text-white shadow-xs focus:ring-red-500',
      defaultConfirmText: 'Xác nhận xóa',
    },
    warning: {
      bgIcon: 'bg-amber-50 text-amber-600 border border-amber-200',
      defaultIcon: <AlertTriangle className="w-6 h-6 text-amber-600" />,
      confirmBtn: 'bg-amber-600 hover:bg-amber-700 text-white shadow-xs focus:ring-amber-500',
      defaultConfirmText: 'Xác nhận thực hiện',
    },
    info: {
      bgIcon: 'bg-blue-50 text-blue-600 border border-blue-200',
      defaultIcon: <Info className="w-6 h-6 text-blue-600" />,
      confirmBtn: 'bg-blue-600 hover:bg-blue-700 text-white shadow-xs focus:ring-blue-500',
      defaultConfirmText: 'Đồng ý',
    },
    success: {
      bgIcon: 'bg-green-50 text-green-600 border border-green-200',
      defaultIcon: <CheckCircle2 className="w-6 h-6 text-green-600" />,
      confirmBtn: 'bg-green-600 hover:bg-green-700 text-white shadow-xs focus:ring-green-500',
      defaultConfirmText: 'Xác nhận',
    },
    primary: {
      bgIcon: 'bg-red-50 text-red-600 border border-red-200',
      defaultIcon: <AlertCircle className="w-6 h-6 text-red-600" />,
      confirmBtn: 'bg-red-600 hover:bg-red-700 text-white shadow-xs focus:ring-red-500',
      defaultConfirmText: 'Xác nhận',
    },
  };

  const config = variantConfig[variant] || variantConfig.danger;
  const finalConfirmText = confirmText || config.defaultConfirmText;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-xs transition-opacity animate-in fade-in duration-200"
        onClick={() => {
          if (!isLoading) onClose();
        }}
      />

      {/* Dialog Body (16px border-radius) */}
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-neutral-200 z-10 overflow-hidden animate-in zoom-in-95 duration-200">
        {/* Header with close button */}
        <div className="flex items-center justify-between p-5 border-b border-neutral-100">
          <div className="flex items-center gap-3">
            <div
              className={cn(
                'w-10 h-10 rounded-xl flex items-center justify-center shrink-0 shadow-2xs',
                config.bgIcon
              )}
            >
              {icon || config.defaultIcon}
            </div>
            <div>
              <h3 className="text-sm md:text-base font-black text-neutral-900 tracking-tight">
                {title}
              </h3>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="p-1.5 text-neutral-400 hover:text-neutral-700 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
            aria-label="Đóng"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 space-y-4">
          {description && (
            <p className="text-xs md:text-sm text-neutral-600 leading-relaxed font-medium">
              {description}
            </p>
          )}

          {children && <div className="mt-2">{children}</div>}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 px-5 py-4 bg-neutral-50 border-t border-neutral-100">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="h-10 px-4 rounded-xl border border-neutral-300 bg-white hover:bg-neutral-100 text-neutral-700 font-bold text-xs transition-colors cursor-pointer disabled:opacity-50"
          >
            {cancelText}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isLoading}
            className={cn(
              'h-10 px-5 rounded-xl font-bold text-xs uppercase tracking-wider transition-all duration-150 cursor-pointer flex items-center gap-2 focus:ring-2 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed',
              config.confirmBtn
            )}
          >
            {isLoading && <Loader2 className="w-4 h-4 animate-spin shrink-0" />}
            <span>{finalConfirmText}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;

