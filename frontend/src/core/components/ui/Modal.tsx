import React, { useEffect } from 'react'
import { cn } from '../../utils/cn'
import { X } from 'lucide-react'

export interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  description?: string
  children?: React.ReactNode
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl' | '5xl'
  headerActions?: React.ReactNode
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  description,
  children,
  maxWidth = 'md',
  headerActions,
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (isOpen) {
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', handleKeyDown)
    }
    return () => {
      document.body.style.overflow = 'unset'
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  const maxWidthStyles = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '4xl': 'max-w-4xl',
    '5xl': 'max-w-5xl',
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto scrollbar-none">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/40 backdrop-blur-xs transition-opacity animate-in fade-in"
        onClick={onClose}
      />

      {/* Modal Dialog (16px border-radius) */}
      <div
        className={cn(
          'relative w-full max-h-[92vh] flex flex-col bg-white rounded-2xl shadow-xl border border-neutral-100 z-10 overflow-hidden animate-in zoom-in-95 duration-200 my-auto',
          maxWidthStyles[maxWidth]
        )}
      >
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 md:p-6 border-b border-neutral-100 shrink-0">
          <div>
            <h2 className="text-lg md:text-xl font-bold text-neutral-900">{title}</h2>
            {description && <p className="text-xs md:text-sm text-neutral-500 mt-1">{description}</p>}
          </div>
          <div className="flex items-center gap-2.5 shrink-0 self-end sm:self-auto">
            {headerActions}
            <button
              onClick={onClose}
              aria-label="Đóng hộp thoại"
              className="p-1.5 text-neutral-400 hover:text-neutral-700 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="p-5 md:p-6 overflow-y-auto scrollbar-none flex-1 overscroll-contain">
          {children}
        </div>
      </div>
    </div>
  )
}

export { ConfirmModal, type ConfirmModalProps } from './ConfirmModal'
export default Modal
