import React, { useState, useEffect, useCallback } from 'react'
import { X, ChevronLeft, ChevronRight, Image as ImageIcon, Sparkles } from 'lucide-react'
import type { HotelImageDto } from '../../organization/types/organization.types'

interface HotelGalleryViewerModalProps {
  isOpen: boolean
  onClose: () => void
  hotelName: string
  images: HotelImageDto[]
  initialIndex?: number
}

export const HotelGalleryViewerModal: React.FC<HotelGalleryViewerModalProps> = ({
  isOpen,
  onClose,
  hotelName,
  images,
  initialIndex = 0,
}) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex)
  const [prevInitial, setPrevInitial] = useState(initialIndex)

  if (initialIndex !== prevInitial) {
    setPrevInitial(initialIndex)
    setCurrentIndex(initialIndex)
  }

  const handleNext = useCallback(() => {
    if (images.length === 0) return
    setCurrentIndex((prev) => (prev + 1) % images.length)
  }, [images.length])

  const handlePrev = useCallback(() => {
    if (images.length === 0) return
    setCurrentIndex((prev) => (prev - 1 + images.length) % images.length)
  }, [images.length])

  useEffect(() => {
    if (!isOpen) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
      if (e.key === 'ArrowRight') handleNext()
      if (e.key === 'ArrowLeft') handlePrev()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, handleNext, handlePrev, onClose])

  if (!isOpen) return null

  const currentImage = images[currentIndex]

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 overflow-hidden">
      {/* Dark Backdrop */}
      <div
        className="fixed inset-0 bg-black/85 backdrop-blur-md transition-opacity animate-in fade-in"
        onClick={onClose}
      />

      {/* Lightbox Container */}
      <div className="relative w-full max-w-5xl max-h-[92vh] flex flex-col bg-neutral-900 rounded-3xl border border-neutral-800 z-10 overflow-hidden shadow-2xl animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 text-white shrink-0">
          <div>
            <h2 className="text-lg font-bold flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-red-500 fill-red-500" />
              {hotelName}
            </h2>
            <p className="text-xs text-neutral-400 mt-0.5">
              Hình ảnh thực tế khuôn viên, kiến trúc và tiện ích khách sạn
            </p>
          </div>

          <div className="flex items-center gap-3">
            {images.length > 0 && (
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-neutral-800 text-neutral-300 border border-neutral-700">
                {currentIndex + 1} / {images.length}
              </span>
            )}
            <button
              onClick={onClose}
              className="p-1.5 text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-xl transition-colors cursor-pointer"
              aria-label="Đóng thư viện ảnh"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Main Content */}
        <div className="relative flex-1 min-h-[380px] max-h-[60vh] flex items-center justify-center p-4 bg-black/40 overflow-hidden">
          {images.length === 0 ? (
            <div className="text-center text-neutral-400 py-16">
              <ImageIcon className="w-16 h-16 mx-auto mb-3 text-neutral-600" />
              <p className="text-sm font-semibold">Khách sạn chưa có hình ảnh công khai nào</p>
            </div>
          ) : (
            <>
              {/* Prev Button */}
              {images.length > 1 && (
                <button
                  type="button"
                  onClick={handlePrev}
                  className="absolute left-4 top-1/2 -translate-y-1/2 z-20 p-2.5 rounded-full bg-black/60 hover:bg-red-600 text-white transition-all cursor-pointer shadow-lg hover:scale-110"
                  aria-label="Ảnh trước"
                >
                  <ChevronLeft className="w-6 h-6" />
                </button>
              )}

              {/* Active Image */}
              <div className="w-full h-full flex items-center justify-center">
                <img
                  src={currentImage.url}
                  alt={currentImage.originalFilename || hotelName}
                  className="max-h-[56vh] max-w-full object-contain rounded-xl select-none shadow-2xl transition-all duration-200"
                />
              </div>

              {/* Next Button */}
              {images.length > 1 && (
                <button
                  type="button"
                  onClick={handleNext}
                  className="absolute right-4 top-1/2 -translate-y-1/2 z-20 p-2.5 rounded-full bg-black/60 hover:bg-red-600 text-white transition-all cursor-pointer shadow-lg hover:scale-110"
                  aria-label="Ảnh kế tiếp"
                >
                  <ChevronRight className="w-6 h-6" />
                </button>
              )}

              {/* Primary Star Indicator */}
              {currentImage.isPrimary && (
                <div className="absolute bottom-6 left-6 z-20 px-3 py-1 bg-red-600/90 text-white text-xs font-bold rounded-full shadow-md backdrop-blur-xs flex items-center gap-1.5">
                  ★ Ảnh đại diện cơ sở
                </div>
              )}
            </>
          )}
        </div>

        {/* Thumbnail Strip */}
        {images.length > 1 && (
          <div className="p-4 bg-neutral-950 border-t border-neutral-800 shrink-0 overflow-x-auto scrollbar-thin">
            <div className="flex items-center gap-2.5 justify-center">
              {images.map((img, idx) => (
                <button
                  key={img.id}
                  type="button"
                  onClick={() => setCurrentIndex(idx)}
                  className={`relative w-16 h-12 rounded-lg overflow-hidden border-2 shrink-0 transition-all cursor-pointer ${
                    currentIndex === idx
                      ? 'border-red-600 ring-2 ring-red-600/40 scale-105 opacity-100'
                      : 'border-neutral-700 opacity-60 hover:opacity-100'
                  }`}
                >
                  <img
                    src={img.url}
                    alt={img.originalFilename}
                    className="w-full h-full object-cover"
                    loading="lazy"
                  />
                  {img.isPrimary && (
                    <div className="absolute top-0.5 right-0.5 w-2 h-2 rounded-full bg-red-600 ring-1 ring-white" />
                  )}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default HotelGalleryViewerModal

