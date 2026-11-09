import { useEffect, useRef, useState } from 'react'
import { getErrorStatus } from '../../../core/api/client'
import Button from '../../../core/components/ui/Button'
import Modal from '../../../core/components/ui/Modal'
import StayGuestEditForm from '../../booking/components/StayGuestEditForm'
import stayGuestService from '../services/stayGuest.service'
import type { LodgingQueueItem } from '../types/lodging.types'
import type { StayGuestResponse } from '../types/stayGuest.types'

export default function LodgingGuestEditDialog({ row, onClose, onSaved }: {
  row: LodgingQueueItem
  onClose: () => void
  onSaved: () => void
}) {
  const [attempt, setAttempt] = useState(0)
  const [loaded, setLoaded] = useState<{ attempt: number; guest?: StayGuestResponse; error?: string }>()
  const busy = useRef(false)
  const result = loaded?.attempt === attempt ? loaded : undefined
  const close = () => { if (!busy.current) onClose() }

  useEffect(() => {
    const request = new AbortController()
    void stayGuestService.getById(row.stayGuestId, request.signal).then(({ result: guest }) => {
      if (request.signal.aborted) return
      if (guest?.id !== row.stayGuestId || guest.hotelId !== row.hotelId ||
          (guest.bookingRoomId ?? null) !== (row.stayGuest?.bookingRoomId ?? null)) {
        setLoaded({ attempt, error: 'Liên kết hồ sơ đã thay đổi. Hãy đóng hộp thoại và tải lại trang.' })
        return
      }
      setLoaded({ attempt, guest })
    }).catch(error => {
      if (request.signal.aborted) return
      const status = getErrorStatus(error)
      setLoaded({ attempt, error: status === 403 ? 'Bạn không có quyền xem hồ sơ này.'
        : status === 404 ? 'Hồ sơ không còn trong danh sách lưu trú.' : 'Không thể tải hồ sơ. Vui lòng thử lại.' })
    })
    return () => request.abort()
  }, [attempt, row.hotelId, row.stayGuest?.bookingRoomId, row.stayGuestId])

  return <Modal isOpen title="Sửa hồ sơ lưu trú" onClose={close} maxWidth="2xl">
    {!result && <p role="status" className="text-sm">Đang tải hồ sơ…</p>}
    {result?.error && <div className="space-y-3">
      <p role="alert" className="text-sm text-red-700">{result.error}</p>
      <Button variant="outline" onClick={() => setAttempt(value => value + 1)}>Tải lại hồ sơ</Button>
    </div>}
    {result?.guest && <StayGuestEditForm guest={result.guest} onCancel={close} onSaved={onSaved}
      onBusyChange={value => { busy.current = value }} />}
  </Modal>
}
