import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { hasAnyRole } from '../../auth/utils/token.utils'
import { LODGING_ROLES } from '../../operation/types/lodging.types'
import bookingService from '../services/booking.service'
import type { BookingResponse, BookingRoomResponse } from '../types/booking.types'
import StayGuestsModal from './StayGuestsModal'

const lodgingPath = '/operation/lodging-queue'

function BookingGuestTarget({ bookingId, roomId, guestId, close }: {
  bookingId: number
  roomId: number
  guestId: number
  close: () => void
}) {
  const [target, setTarget] = useState<{ booking: BookingResponse; room: BookingRoomResponse } | null>()
  useEffect(() => {
    const request = new AbortController()
    void bookingService.getById(bookingId, request.signal).then(({ result: booking }) => {
      if (request.signal.aborted) return
      const room = booking?.bookingDetails?.flatMap(detail => detail.bookingRooms ?? []).find(item => item.id === roomId)
      setTarget(room?.roomInstanceId && room.roomNumber ? { booking, room } : null)
    }).catch(() => { if (!request.signal.aborted) setTarget(null) })
    return () => request.abort()
  }, [bookingId, roomId])

  if (target === undefined) return <p role="status" className="text-sm">Đang mở hồ sơ khách lưu trú…</p>
  if (!target) return <p role="alert" className="text-sm text-red-700">
    Không mở được booking/phòng đã gán. <Link className="underline" to={lodgingPath}>Về hàng đợi lưu trú</Link>
  </p>

  return <StayGuestsModal isOpen onClose={close} hotelId={target.booking.hotelId} bookingId={target.booking.id}
    bookingRoomId={target.room.id} roomId={target.room.roomInstanceId} roomNumber={target.room.roomNumber}
    initialEditGuestId={guestId} />
}

export default function StayGuestBookingLink() {
  const [params, setParams] = useSearchParams()
  if (!params.has('stayGuestId') || !hasAnyRole(LODGING_ROLES)) return null
  const bookingId = Number(params.get('bookingId'))
  const roomId = Number(params.get('bookingRoomId'))
  const guestId = Number(params.get('stayGuestId'))
  if (![bookingId, roomId, guestId].every(id => Number.isSafeInteger(id) && id > 0)) {
    return <p role="alert" className="text-sm text-red-700">Liên kết hồ sơ khách lưu trú không hợp lệ.</p>
  }
  return <BookingGuestTarget key={bookingId + ':' + roomId + ':' + guestId}
    bookingId={bookingId} roomId={roomId} guestId={guestId} close={() => {
      const next = new URLSearchParams(params)
      for (const key of ['bookingId', 'bookingRoomId', 'stayGuestId']) next.delete(key)
      setParams(next, { replace: true })
    }} />
}
