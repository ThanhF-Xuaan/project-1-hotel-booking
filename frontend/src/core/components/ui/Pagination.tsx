import React from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import Button from './Button'

export interface PaginationProps {
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  onPageChange: (newPage: number) => void
}

export const Pagination: React.FC<PaginationProps> = ({
  pageNumber,
  pageSize,
  totalElements,
  totalPages,
  onPageChange,
}) => {
  if (totalElements === 0) return null

  const start = pageNumber * pageSize + 1
  const end = Math.min((pageNumber + 1) * pageSize, totalElements)

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 py-3 px-4 border-t border-neutral-200 bg-white">
      <div className="text-xs md:text-sm text-neutral-600">
        Hiển thị <span className="font-semibold text-neutral-900">{start}</span> -{' '}
        <span className="font-semibold text-neutral-900">{end}</span> trên tổng số{' '}
        <span className="font-semibold text-neutral-900">{totalElements}</span> bản ghi
      </div>

      <div className="flex items-center gap-1.5">
        <Button
          variant="outline"
          size="sm"
          disabled={pageNumber === 0}
          onClick={() => onPageChange(pageNumber - 1)}
          aria-label="Trang trước"
        >
          <ChevronLeft className="w-4 h-4" />
        </Button>

        <span className="text-xs md:text-sm font-medium px-2.5 text-neutral-700">
          Trang <span className="font-bold text-red-600">{pageNumber + 1}</span> / {Math.max(1, totalPages)}
        </span>

        <Button
          variant="outline"
          size="sm"
          disabled={pageNumber >= totalPages - 1}
          onClick={() => onPageChange(pageNumber + 1)}
          aria-label="Trang tiếp theo"
        >
          <ChevronRight className="w-4 h-4" />
        </Button>
      </div>
    </div>
  )
}

export default Pagination
