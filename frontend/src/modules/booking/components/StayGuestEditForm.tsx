import { useRef, useState, type FormEvent } from 'react'
import { getErrorStatus } from '../../../core/api/client'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import stayGuestService from '../../operation/services/stayGuest.service'
import type { DocumentType, Gender, StayGuestResponse, StayGuestUpdateRequest } from '../../operation/types/stayGuest.types'

const vietnamDateTime = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Ho_Chi_Minh', dateStyle: 'short', timeStyle: 'short',
})
const toLocalInput = (value?: string) => value && Number.isFinite(Date.parse(value))
  ? vietnamDateTime.format(new Date(value)).replace(' ', 'T') : ''

function valuesOf(guest: StayGuestResponse) {
  return {
    fullName: guest.fullName ?? '',
    dateOfBirth: guest.dateOfBirth ?? '',
    gender: guest.gender ?? ('' as const),
    nationality: guest.nationality ?? '',
    documentType: guest.documentType ?? ('' as const),
    documentNumber: guest.documentNumber ?? '',
    permanentAddress: guest.permanentAddress ?? '',
    reasonForStay: guest.reasonForStay ?? '',
    expectedCheckOutTime: toLocalInput(guest.expectedCheckOutTime),
  }
}

export default function StayGuestEditForm({ guest, onSaved, onCancel, onBusyChange }: {
  guest: StayGuestResponse
  onSaved: () => void
  onCancel: () => void
  onBusyChange: (busy: boolean) => void
}) {
  const [base, setBase] = useState(guest)
  const [draft, setDraft] = useState(() => valuesOf(guest))
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [needsReload, setNeedsReload] = useState(false)
  const lock = useRef(false)

  function change<K extends keyof typeof draft>(field: K, value: typeof draft[K]) {
    setDraft(previous => ({ ...previous, [field]: value }))
  }

  async function reload() {
    if (lock.current) return
    lock.current = true
    setBusy(true)
    onBusyChange(true)
    try {
      const { result } = await stayGuestService.getById(guest.id)
      if (result.bookingRoomId !== guest.bookingRoomId || result.hotelId !== guest.hotelId) {
        setError('Khách không còn thuộc phòng này. Hãy đóng và mở lại hồ sơ từ danh sách.')
        return
      }
      const before = valuesOf(base)
      const latest = valuesOf(result)
      setDraft(previous => ({
        fullName: previous.fullName === before.fullName ? latest.fullName : previous.fullName,
        dateOfBirth: previous.dateOfBirth === before.dateOfBirth ? latest.dateOfBirth : previous.dateOfBirth,
        gender: previous.gender === before.gender ? latest.gender : previous.gender,
        nationality: previous.nationality === before.nationality ? latest.nationality : previous.nationality,
        documentType: previous.documentType === before.documentType ? latest.documentType : previous.documentType,
        documentNumber: previous.documentNumber === before.documentNumber ? latest.documentNumber : previous.documentNumber,
        permanentAddress: previous.permanentAddress === before.permanentAddress ? latest.permanentAddress : previous.permanentAddress,
        reasonForStay: previous.reasonForStay === before.reasonForStay ? latest.reasonForStay : previous.reasonForStay,
        expectedCheckOutTime: previous.expectedCheckOutTime === before.expectedCheckOutTime
          ? latest.expectedCheckOutTime : previous.expectedCheckOutTime,
      }))
      setBase(result)
      setError('')
      setNeedsReload(false)
      setNotice('Đã tải lại hồ sơ và giữ các trường bạn đã sửa. Kiểm tra trước khi lưu.')
    } catch {
      setError('Chưa tải lại được hồ sơ. Các trường bạn nhập vẫn được giữ.')
    } finally {
      lock.current = false
      setBusy(false)
      onBusyChange(false)
    }
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    if (lock.current || needsReload) return
    const original = valuesOf(base)
    const updates: StayGuestUpdateRequest = {}
    const textFields = ['fullName', 'nationality', 'documentNumber', 'permanentAddress', 'reasonForStay'] as const
    for (const field of textFields) {
      if (draft[field].trim() !== original[field].trim()) updates[field] = draft[field].trim()
    }
    if (!draft.fullName.trim() || !draft.nationality.trim() || (original.documentNumber && !draft.documentNumber.trim())) {
      setError('Kiểm tra họ tên, quốc tịch và số giấy tờ trước khi lưu.')
      return
    }
    if ((original.dateOfBirth && !draft.dateOfBirth) || (original.gender && !draft.gender) ||
        (original.documentType && !draft.documentType) || (original.expectedCheckOutTime && !draft.expectedCheckOutTime)) {
      setError('Các trường ngày sinh, giới tính, loại giấy tờ và giờ trả phòng đã có giá trị cần được thay bằng giá trị hợp lệ.')
      return
    }
    if (draft.dateOfBirth && draft.dateOfBirth !== original.dateOfBirth) updates.dateOfBirth = draft.dateOfBirth
    if (draft.gender && draft.gender !== original.gender) updates.gender = draft.gender
    if (draft.documentType && draft.documentType !== original.documentType) updates.documentType = draft.documentType
    if (draft.expectedCheckOutTime && draft.expectedCheckOutTime !== original.expectedCheckOutTime) {
      const checkout = draft.expectedCheckOutTime + ':00+07:00'
      if (!Number.isFinite(Date.parse(checkout)) || (base.checkInTime && Date.parse(checkout) <= Date.parse(base.checkInTime))) {
        setError('Giờ trả phòng dự kiến phải sau giờ đến.')
        return
      }
      updates.expectedCheckOutTime = checkout
    }
    if (!Object.keys(updates).length) {
      setError('Hồ sơ chưa có thay đổi để lưu.')
      return
    }
    lock.current = true
    setBusy(true)
    onBusyChange(true)
    setError('')
    setNotice('')
    try {
      await stayGuestService.update(guest.id, updates)
      onSaved()
    } catch (reason) {
      const status = getErrorStatus(reason)
      if (status === 409 || status == null || status >= 500) {
        setNeedsReload(true)
        setError(status === 409
          ? 'Hồ sơ đang xung đột hoặc đã được xuất. Tải lại hồ sơ để kiểm tra trước khi tiếp tục.'
          : 'Chưa xác định được kết quả lưu. Tải lại hồ sơ để đối chiếu.')
      } else {
        setError(status === 403 ? 'Bạn không có quyền sửa hồ sơ này.' : 'Không thể lưu hồ sơ. Kiểm tra thông tin vừa nhập.')
      }
    } finally {
      lock.current = false
      setBusy(false)
      onBusyChange(false)
    }
  }

  const selectClass = 'mt-1 block w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm'
  return <form onSubmit={save} className="space-y-4 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
    <h4 className="font-semibold">Sửa hồ sơ khách lưu trú</h4>
    <p className="text-sm text-neutral-600">Phòng {base.roomNumber || '—'} · Giờ đến (UTC+7): {toLocalInput(base.checkInTime).replace('T', ' ') || 'Chưa ghi nhận'}</p>
    <fieldset disabled={busy} className="grid gap-3 sm:grid-cols-2">
      <Input id="edit-stay-name" label="Họ và tên" value={draft.fullName} maxLength={250} required onChange={e => change('fullName', e.target.value)} />
      <Input id="edit-stay-dob" label="Ngày sinh" type="date" value={draft.dateOfBirth} onChange={e => change('dateOfBirth', e.target.value)} />
      <label className="text-sm font-medium">Giới tính
        <select className={selectClass} value={draft.gender} onChange={e => change('gender', e.target.value as Gender | '')}>
          <option value="">Chưa cung cấp</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option>
        </select>
      </label>
      <Input id="edit-stay-nationality" label="Quốc tịch" value={draft.nationality} maxLength={100} required onChange={e => change('nationality', e.target.value)} />
      <label className="text-sm font-medium">Loại giấy tờ
        <select className={selectClass} value={draft.documentType} onChange={e => change('documentType', e.target.value as DocumentType | '')}>
          <option value="">Chưa cung cấp</option><option value="CCCD">CCCD</option><option value="PASSPORT">Hộ chiếu</option>
          <option value="CMND">CMND</option><option value="DRIVER_LICENSE">Giấy phép lái xe</option><option value="OTHER">Khác</option>
        </select>
      </label>
      <Input id="edit-stay-document" label="Số giấy tờ" value={draft.documentNumber} maxLength={50} autoComplete="off" onChange={e => change('documentNumber', e.target.value)} />
      <Input id="edit-stay-address" label="Địa chỉ thường trú / nơi cấp" value={draft.permanentAddress} onChange={e => change('permanentAddress', e.target.value)} />
      <Input id="edit-stay-checkout" label="Giờ trả phòng dự kiến (UTC+7)" type="datetime-local" value={draft.expectedCheckOutTime} onChange={e => change('expectedCheckOutTime', e.target.value)} />
      <Input id="edit-stay-reason" label="Lý do lưu trú" value={draft.reasonForStay} maxLength={150} onChange={e => change('reasonForStay', e.target.value)} />
    </fieldset>
    {error && <p className="text-sm text-red-700" role="alert">{error}</p>}
    {notice && <p className="text-sm text-neutral-700" role="status">{notice}</p>}
    <div className="flex flex-wrap justify-end gap-2">
      <Button type="button" variant="outline" disabled={busy} onClick={onCancel}>Hủy sửa</Button>
      {needsReload ? <Button type="button" isLoading={busy} onClick={() => void reload()}>Tải lại hồ sơ</Button>
        : <Button type="submit" isLoading={busy}>Lưu thay đổi</Button>}
    </div>
  </form>
}
