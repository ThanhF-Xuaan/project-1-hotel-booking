import React, { useState, useEffect, useRef } from 'react'
import {
  Upload,
  Star,
  Trash2,
  Image as ImageIcon,
  Loader2,
  AlertCircle,
  Check,
} from 'lucide-react'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Button from '../../../core/components/ui/Button'
import hotelService from '../services/hotel.service'
import type { HotelImageDto } from '../types/organization.types'

interface HotelMediaGalleryModalProps {
  isOpen: boolean
  onClose: () => void
  hotelId: number
  hotelName: string
  onCoverImageChanged?: () => void
}

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export const HotelMediaGalleryModal: React.FC<HotelMediaGalleryModalProps> = ({
  isOpen,
  onClose,
  hotelId,
  hotelName,
  onCoverImageChanged,
}) => {
  const [images, setImages] = useState<HotelImageDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Set primary state
  const [settingPrimaryId, setSettingPrimaryId] = useState<number | null>(null)

  // Delete state
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!isOpen || !hotelId) return

    let isMounted = true
    hotelService
      .getHotelImages(hotelId)
      .then((res) => {
        if (isMounted && res && res.result) {
          setImages(res.result)
        }
      })
      .catch((err) => {
        console.error('Lỗi khi tải danh sách ảnh khách sạn:', err)
        if (isMounted) setErrorMsg('Không thể tải danh sách ảnh khách sạn.')
      })
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [isOpen, hotelId])

  const reloadImages = async () => {
    if (!hotelId) return
    setIsLoading(true)
    try {
      const res = await hotelService.getHotelImages(hotelId)
      if (res && res.result) {
        setImages(res.result)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách ảnh khách sạn:', err)
      setErrorMsg('Không thể tải danh sách ảnh khách sạn.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleFilesSelected = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return
    setErrorMsg(null)
    setSuccessMsg(null)

    const validFiles: File[] = []
    const invalidNames: string[] = []

    Array.from(fileList).forEach((file) => {
      if (!ALLOWED_MIME_TYPES.includes(file.type)) {
        invalidNames.push(`${file.name} (định dạng không hỗ trợ)`)
      } else if (file.size > MAX_FILE_SIZE_BYTES) {
        invalidNames.push(`${file.name} (> 10MB)`)
      } else {
        validFiles.push(file)
      }
    })

    if (invalidNames.length > 0) {
      setErrorMsg(`Một số tệp không hợp lệ: ${invalidNames.join(', ')}`)
    }

    if (validFiles.length === 0) {
      if (fileInputRef.current) fileInputRef.current.value = ''
      return
    }

    setIsUploading(true)
    try {
      const res = await hotelService.uploadHotelImages(hotelId, validFiles)
      if (res && res.result) {
        setImages(res.result)
        setSuccessMsg(`Tải lên thành công ${validFiles.length} ảnh!`)
        onCoverImageChanged?.()
      }
    } catch (err: unknown) {
      console.error('Lỗi tải ảnh lên:', err)
      const message =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : 'Lỗi khi tải ảnh lên máy chủ.'
      setErrorMsg(message || 'Không thể tải ảnh lên.')
    } finally {
      setIsUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    handleFilesSelected(e.dataTransfer.files)
  }

  const handleSetPrimary = async (imageId: number) => {
    setSettingPrimaryId(imageId)
    setErrorMsg(null)
    try {
      await hotelService.setPrimaryImage(hotelId, imageId)
      setImages((prev) =>
        prev.map((img) => ({
          ...img,
          isPrimary: img.id === imageId,
        }))
      )
      setSuccessMsg('Đã cập nhật ảnh đại diện chính.')
      onCoverImageChanged?.()
    } catch (err) {
      console.error('Lỗi khi đặt ảnh đại diện:', err)
      setErrorMsg('Không thể đặt ảnh này làm ảnh đại diện.')
    } finally {
      setSettingPrimaryId(null)
    }
  }

  const handleDeleteConfirm = async () => {
    if (!deleteConfirmId) return
    setIsDeleting(true)
    setErrorMsg(null)
    try {
      await hotelService.deleteHotelImage(hotelId, deleteConfirmId)
      await reloadImages()
      setSuccessMsg('Xóa ảnh thành công.')
      setDeleteConfirmId(null)
      onCoverImageChanged?.()
    } catch (err) {
      console.error('Lỗi khi xóa ảnh:', err)
      setErrorMsg('Không thể xóa ảnh này.')
    } finally {
      setIsDeleting(false)
    }
  }

  const formatFileSize = (bytes: number): string => {
    if (!bytes) return '0 B'
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title={`Thư viện ảnh: ${hotelName}`}
        description="Quản lý và tải lên các ảnh cơ sở, khuôn viên, phòng ốc và tiện ích khách sạn"
        maxWidth="4xl"
      >
        <div className="space-y-6">
          {/* Notifications */}
          {errorMsg && (
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm">
              <AlertCircle className="w-5 h-5 shrink-0 text-red-600" />
              <span>{errorMsg}</span>
            </div>
          )}

          {successMsg && (
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm">
              <Check className="w-5 h-5 shrink-0 text-emerald-600" />
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
                ? 'border-red-600 bg-red-50/50'
                : 'border-neutral-200 hover:border-neutral-300 bg-neutral-50/50'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/png,image/jpeg,image/webp"
              onChange={(e) => handleFilesSelected(e.target.files)}
              className="hidden"
            />
            <div className="flex flex-col items-center justify-center">
              {isUploading ? (
                <div className="flex flex-col items-center gap-2 py-3">
                  <Loader2 className="w-8 h-8 text-red-600 animate-spin" />
                  <p className="text-sm font-medium text-neutral-700">Đang tải và quét virus hình ảnh...</p>
                </div>
              ) : (
                <>
                  <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center mb-3">
                    <Upload className="w-6 h-6" />
                  </div>
                  <p className="text-sm font-semibold text-neutral-900 mb-1">
                    Nhấp để chọn ảnh hoặc kéo thả ảnh vào đây
                  </p>
                  <p className="text-xs text-neutral-500">
                    Hỗ trợ JPG, PNG, WEBP. Tối đa 10MB/tệp.
                  </p>
                </>
              )}
            </div>
          </div>

          {/* Images Grid */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-neutral-900 uppercase tracking-wider">
                Danh sách hình ảnh ({images.length})
              </h3>
            </div>

            {isLoading ? (
              <div className="flex flex-col items-center justify-center py-12 text-neutral-400">
                <Loader2 className="w-8 h-8 animate-spin mb-2" />
                <p className="text-sm">Đang tải danh sách ảnh...</p>
              </div>
            ) : images.length === 0 ? (
              <div className="text-center py-12 bg-neutral-50/80 rounded-2xl border border-neutral-100">
                <ImageIcon className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
                <p className="text-sm font-medium text-neutral-700 mb-1">
                  Chưa có hình ảnh nào cho khách sạn này
                </p>
                <p className="text-xs text-neutral-400">
                  Hãy tải ảnh lên để hiển thị trên cổng đặt phòng trực tuyến cho khách hàng
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                {images.map((img) => (
                  <div
                    key={img.id}
                    className={`group relative rounded-xl overflow-hidden border bg-white shadow-xs transition-all ${
                      img.isPrimary
                        ? 'border-red-600 ring-2 ring-red-600/20'
                        : 'border-neutral-200 hover:border-neutral-300 hover:shadow-md'
                    }`}
                  >
                    {/* Thumbnail */}
                    <div className="aspect-4/3 w-full bg-neutral-100 overflow-hidden relative">
                      <img
                        src={img.url}
                        alt={img.originalFilename}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        loading="lazy"
                      />

                      {/* Primary Badge */}
                      {img.isPrimary && (
                        <div className="absolute top-2 left-2 z-10 flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold bg-red-600 text-white shadow-sm">
                          <Star className="w-3 h-3 fill-current" />
                          <span>Ảnh đại diện</span>
                        </div>
                      )}

                      {/* Top Right Action (Delete) */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          setDeleteConfirmId(img.id)
                        }}
                        className="absolute top-2 right-2 p-1.5 rounded-lg bg-black/60 hover:bg-red-600 text-white opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer shadow-sm"
                        title="Xóa ảnh"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    {/* Meta info & Action Footer */}
                    <div className="p-2.5 space-y-2">
                      <div className="flex items-center justify-between text-xs text-neutral-500">
                        <span className="truncate max-w-[120px]" title={img.originalFilename}>
                          {img.originalFilename}
                        </span>
                        <span className="shrink-0">{formatFileSize(img.sizeBytes)}</span>
                      </div>

                      {!img.isPrimary && (
                        <Button
                          variant="secondary"
                          size="sm"
                          className="w-full text-xs py-1 h-8"
                          isLoading={settingPrimaryId === img.id}
                          onClick={() => handleSetPrimary(img.id)}
                        >
                          <Star className="w-3.5 h-3.5 mr-1" />
                          Đặt làm ảnh chính
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Confirmation Modal when deleting an image */}
      <ConfirmModal
        isOpen={deleteConfirmId !== null}
        onClose={() => setDeleteConfirmId(null)}
        onConfirm={handleDeleteConfirm}
        isLoading={isDeleting}
        title="Xác nhận xóa ảnh khách sạn?"
        description="Ảnh này sẽ bị gỡ khỏi hệ thống và không còn hiển thị với khách hàng. Nếu đây là ảnh đại diện, hệ thống sẽ tự động chọn một ảnh khác thay thế."
        confirmText="Xóa ảnh"
        variant="danger"
      />
    </>
  )
}

export default HotelMediaGalleryModal

