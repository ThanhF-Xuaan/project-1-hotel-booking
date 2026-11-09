import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Download, Pencil, RotateCw, Trash2 } from 'lucide-react'
import { getErrorStatus } from '../../../core/api/client'
import type { PageResponse } from '../../../core/api/types'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import { downloadFile } from '../../../core/utils/downloadFile'
import { hasAnyRole } from '../../auth/utils/token.utils'
import LodgingGuestEditDialog from '../components/LodgingGuestEditDialog'
import { isEmptyLodgingExport, loadLodgingHotels, lodgingExportCause, lodgingService } from '../services/lodging.service'
import stayGuestService from '../services/stayGuest.service'
import { LODGING_STATUS_LABELS, type LodgingCategory, type LodgingExportRequest, type LodgingQueueItem, type LodgingStatus } from '../types/lodging.types'

const panel = 'space-y-4 rounded-xl border border-neutral-200 bg-white p-4'
const selectStyle = 'mt-1 block w-full rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm'
const cell = 'px-3 py-2 text-left align-top'
const emptyFilters = { status: '', from: '', to: '' } as const
const reconcileFilters = { ...emptyFilters, status: 'EXPORTED' as const }
const reconciliationNotice = 'Chưa xác định được kết quả xuất. Đối chiếu thời gian và mã đợt trong danh sách Đã xuất file trước khi xuất lại.'
type Filters = { status: LodgingStatus | ''; from: string; to: string }
type Hotel = { id: number; name: string }
type LoadedPage = { key: string; data?: PageResponse<LodgingQueueItem>; error?: string }
type GuestAction = { type: 'edit' | 'delete'; row: LodgingQueueItem }

function isExportLocked(row: LodgingQueueItem) {
  return row.status === 'EXPORTED' || row.status === 'PROCESSING' || Boolean(row.exportedAt)
}

function canDeleteGuest(row: LodgingQueueItem) {
  return !isExportLocked(row) && ['PENDING', 'ERROR', 'FAILED'].includes(row.status)
}

const vietnamDate = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh', dateStyle: 'short' })
const vietnamTime = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh', dateStyle: 'short', timeStyle: 'short',
})

function displayTime(value?: string) {
  return value && Number.isFinite(Date.parse(value)) ? vietnamTime.format(new Date(value)) : '—'
}

function ExportDialog({ hotel, hotels, selectHotel, busy, close, confirm }: {
  hotel: Hotel
  hotels?: Hotel[]
  selectHotel: (id: number) => void
  busy: boolean
  close: () => void
  confirm: (request: LodgingExportRequest) => void
}) {
  const [date, setDate] = useState(() => vietnamDate.format(new Date()))
  const [hour, setHour] = useState('23')
  const [category, setCategory] = useState<LodgingCategory>('VIETNAMESE')
  return <Modal isOpen title="Xuất Excel đối soát nội bộ" onClose={close}>
    <form className="space-y-4" onSubmit={event => {
      event.preventDefault()
      if (!busy && date && hour !== '' && Number.isInteger(Number(hour)) && +hour >= 0 && +hour <= 23) {
        confirm({ hotelId: hotel.id, date, cutOffHour: Number(hour), category })
      }
    }}>
      <fieldset disabled={busy} className="grid gap-3 sm:grid-cols-2">
        {hotels ? <label className="text-sm font-medium sm:col-span-2">Khách sạn xuất báo cáo
          <select className={selectStyle} value={hotel.id} onChange={e => selectHotel(Number(e.target.value))}>
            {hotels.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
        </label> : <p className="text-sm sm:col-span-2">Khách sạn: <strong>{hotel.name}</strong></p>}
        <Input label="Ngày báo cáo" type="date" value={date} onChange={e => setDate(e.target.value)} required />
        <Input label="Giờ chốt (UTC+7)" type="number" min={0} max={23} step={1} value={hour}
          onChange={e => setHour(e.target.value)} required />
        <label className="text-sm font-medium sm:col-span-2">Nhóm khách
          <select className={selectStyle} value={category} onChange={e => setCategory(e.target.value as LodgingCategory)}>
            <option value="VIETNAMESE">Khách Việt Nam</option>
            <option value="FOREIGNER">Khách nước ngoài</option>
          </select>
        </label>
      </fieldset>
      <p className="text-sm text-neutral-600">File gồm các hồ sơ đủ điều kiện đến hết giờ chốt, kể cả ngày trước đó. Sau khi tạo file, hệ thống đánh dấu các hồ sơ trong đợt là Đã xuất file.</p>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" disabled={busy} onClick={close}>Hủy</Button>
        <Button type="submit" isLoading={busy}>Xác nhận xuất</Button>
      </div>
    </form>
  </Modal>
}

function Queue({ hotel, hotels, exportOpen, setExportOpen, selectHotel }: {
  hotel: Hotel
  hotels?: Hotel[]
  exportOpen: boolean
  setExportOpen: (open: boolean) => void
  selectHotel: (id: number) => void
}) {
  const pendingKey = 'lodging-export-pending:' + hotel.id
  const [uncertainExport, setUncertainExport] = useState(() => sessionStorage.getItem(pendingKey) === '1')
  const [draft, setDraft] = useState<Filters>(() => uncertainExport ? reconcileFilters : emptyFilters)
  const [filters, setFilters] = useState<Filters>(() => uncertainExport ? reconcileFilters : emptyFilters)
  const [page, setPage] = useState(0)
  const [version, setVersion] = useState(0)
  const [loaded, setLoaded] = useState<LoadedPage>()
  const [dateError, setDateError] = useState('')
  const [message, setMessage] = useState<{ text: string; error?: boolean }>()
  const [action, setAction] = useState<'export' | 'delete' | number>()
  const [guestAction, setGuestAction] = useState<GuestAction>()
  const lock = useRef(false)
  const canManageDeletion = hasAnyRole(['CHAIN_ADMIN', 'PROPERTY_MANAGER'])
  const actionsDisabled = action !== undefined || guestAction !== undefined || exportOpen

  const { status, from, to } = filters
  const key = JSON.stringify([status, from, to, page, version])
  const result = loaded?.key === key ? loaded : undefined
  const data = result?.data
  const canClearFilters = Boolean(draft.status || draft.from || draft.to || status || from || to)

  useEffect(() => {
    const request = new AbortController()
    void lodgingService.filter({
      hotelId: hotel.id, page, pageSize: 10, status: status || undefined,
      fromDate: from ? from + 'T00:00:00+07:00' : undefined,
      toDate: to ? to + 'T23:59:59.999999+07:00' : undefined,
    }, request.signal).then(
      response => { if (!request.signal.aborted) setLoaded({ key, data: response }) },
      error => {
        if (request.signal.aborted) return
        setLoaded({ key, error: getErrorStatus(error) === 403
          ? 'Bạn không có quyền xem hàng đợi của khách sạn này.' : 'Không thể tải hàng đợi lưu trú.' })
      },
    )
    return () => request.abort()
  }, [hotel.id, page, status, from, to, key])

  const refresh = () => setVersion(value => value + 1)

  function updateDraftAndFilter(next: Partial<Filters>) {
    const updated = { ...draft, ...next }
    setDraft(updated)
    if (updated.from && updated.to && updated.from > updated.to) {
      setDateError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.')
      return
    }
    setDateError('')
    setPage(0)
    setFilters(updated)
  }

  function applyFilters(event: FormEvent) {
    event.preventDefault()
    if (draft.from && draft.to && draft.from > draft.to) {
      setDateError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.')
      return
    }
    setDateError('')
    setPage(0)
    setFilters(draft)
    refresh()
  }

  function clearFilters() {
    setDraft(emptyFilters)
    setFilters(emptyFilters)
    setDateError('')
    setPage(0)
    refresh()
  }

  async function retry(id: number) {
    if (lock.current) return
    lock.current = true
    setAction(id)
    setMessage(undefined)
    try {
      const row = await lodgingService.retry(id)
      setMessage({ text: row.status === 'PENDING' ? 'Hồ sơ đã chuyển sang Chờ xuất.' : 'Hồ sơ vẫn cần bổ sung thông tin.' })
    } catch {
      setMessage({ text: 'Không thể kiểm tra lại hồ sơ. Hãy đối chiếu trạng thái vừa tải.', error: true })
    } finally {
      lock.current = false
      setAction(undefined)
      setPage(0)
      refresh()
    }
  }

  async function deleteGuest() {
    if (lock.current || guestAction?.type !== 'delete' || !canManageDeletion) return
    const { row } = guestAction
    if (row.hotelId !== hotel.id || !canDeleteGuest(row)) return
    lock.current = true
    setAction('delete')
    setMessage(undefined)
    try {
      await stayGuestService.delete([row.stayGuestId])
      setMessage({ text: 'Đã xóa mềm hồ sơ và hủy mục tương ứng trong hàng đợi.' })
    } catch (error) {
      const status = getErrorStatus(error)
      setMessage({ error: true, text: status === 403 ? 'Bạn không có quyền xóa hồ sơ này.'
        : status === 409 ? 'Hồ sơ đang xử lý, đã xuất hoặc vừa thay đổi. Hãy kiểm tra trạng thái mới.'
          : 'Chưa xác nhận được kết quả xóa. Hãy đối chiếu hàng đợi trước khi thử lại.' })
    } finally {
      lock.current = false
      setAction(undefined)
      setGuestAction(undefined)
      setPage(0)
      refresh()
    }
  }

  async function exportWorkbook(request: LodgingExportRequest) {
    if (lock.current || uncertainExport) return
    lock.current = true
    setAction('export')
    setMessage(undefined)
    sessionStorage.setItem(pendingKey, '1')
    try {
      const file = await lodgingService.exportWorkbook(request)
      downloadFile(file, 'luu-tru-doi-soat-' + request.hotelId + '-' + request.date + '-' + request.category.toLowerCase() + '.xlsx')
      sessionStorage.removeItem(pendingKey)
      setMessage({ text: 'Đã tạo file Excel đối soát. Danh sách đã được yêu cầu làm mới.' })
    } catch (error) {
      const cause = lodgingExportCause(error)
      const http = getErrorStatus(cause)
      if (http == null || http >= 500) {
        setDraft(reconcileFilters)
        setFilters(reconcileFilters)
        setDateError('')
        setUncertainExport(true)
      } else {
        sessionStorage.removeItem(pendingKey)
        setMessage({ text: await isEmptyLodgingExport(cause)
          ? 'Không có hồ sơ phù hợp với ngày chốt và nhóm khách đã chọn.'
          : 'Không thể xuất file. Kiểm tra quyền và tham số xuất.', error: true })
      }
    } finally {
      lock.current = false
      setAction(undefined)
      setExportOpen(false)
      setPage(0)
      refresh()
    }
  }

  return <section className={panel} aria-label="Hàng đợi lưu trú">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className="text-lg font-semibold">Hàng đợi lưu trú</h2>
      <Button disabled={actionsDisabled || uncertainExport} onClick={() => setExportOpen(true)}>
        <Download className="h-4 w-4" aria-hidden="true" /> Xuất Excel đối soát
      </Button>
    </div>
    <form onSubmit={applyFilters} className="grid items-end gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <label className="text-sm font-medium">Trạng thái
        <select className={selectStyle} value={draft.status} disabled={uncertainExport}
          onChange={e => updateDraftAndFilter({ status: e.target.value as Filters['status'] })}>
          <option value="">Tất cả</option>
          {Object.entries(LODGING_STATUS_LABELS).filter(([value]) => value !== 'FAILED')
            .map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <Input label="Ngày vào hàng đợi từ" type="date" value={draft.from} disabled={uncertainExport}
        aria-describedby="lodging-date-filter-help"
        onChange={e => updateDraftAndFilter({ from: e.target.value })} />
      <Input label="Ngày vào hàng đợi đến" type="date" value={draft.to} disabled={uncertainExport}
        aria-describedby="lodging-date-filter-help"
        onChange={e => updateDraftAndFilter({ to: e.target.value })} />
      <div className="flex flex-wrap gap-2">
        {canClearFilters && <Button type="button" variant="outline" disabled={actionsDisabled || uncertainExport}
          onClick={clearFilters}>Xóa bộ lọc</Button>}
      </div>
      <p id="lodging-date-filter-help" className="text-sm text-neutral-600 sm:col-span-2 lg:col-span-4">
        Lọc theo ngày hồ sơ được đưa vào hàng đợi, theo giờ Việt Nam.
      </p>
    </form>
    {dateError && <p role="alert" className="text-sm text-red-700">{dateError}</p>}
    {message && <p role={message.error ? 'alert' : 'status'} className={message.error ? 'text-sm text-red-700' : 'text-sm text-green-700'}>{message.text}</p>}
    {uncertainExport && <p role="alert" className="text-sm text-red-700">{reconciliationNotice}</p>}
    {uncertainExport && data && <Button variant="outline" onClick={() => {
      sessionStorage.removeItem(pendingKey)
      setUncertainExport(false)
    }}>Đã đối chiếu trạng thái xuất</Button>}
    {!result && <p role="status" className="text-sm">Đang tải hàng đợi…</p>}
    {result?.error && <div className="space-y-2">
      <p role="alert" className="text-sm text-red-700">{result.error}</p>
      <Button variant="outline" onClick={refresh}>Tải lại hàng đợi</Button>
    </div>}
    {data?.empty && <p role="status" className="text-sm text-neutral-600">Không có hồ sơ phù hợp với bộ lọc.</p>}
    {data && !data.empty && <>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[60rem] text-sm">
          <thead className="bg-neutral-50"><tr>
            {['Khách lưu trú', 'Số phòng', 'Thời điểm nhận phòng', 'Ngày vào hàng đợi',
              'Trạng thái khai báo', 'Thông tin cần bổ sung', 'Lần xuất Excel', 'Thao tác'].map(label =>
              <th key={label} scope="col" className={cell}>{label}</th>)}
          </tr></thead>
          <tbody className="divide-y divide-neutral-100">
            {data.content.map(row => {
              const guest = row.stayGuest
              const needsCorrection = row.status === 'ERROR' || row.status === 'FAILED'
              const hasGuest = Boolean(guest && row.stayGuestId > 0 && row.hotelId === hotel.id)
              const locked = isExportLocked(row)
              return <tr key={row.id}>
                <th scope="row" className={cell}>{guest?.fullName || '—'}</th>
                <td className={cell}>{guest?.roomNumber || '—'}</td>
                <td className={cell}>{displayTime(guest?.checkInTime)}</td>
                <td className={cell}>{displayTime(row.createdAt)}</td>
                <td className={cell}><span className={needsCorrection
                  ? 'rounded-full bg-amber-100 px-2 py-1 font-medium text-amber-900'
                  : 'rounded-full bg-neutral-100 px-2 py-1 font-medium text-neutral-800'}>
                  {LODGING_STATUS_LABELS[row.status] || 'Chưa xác định'}
                </span></td>
                <td className={cell}>{row.errorMessage?.trim() || (needsCorrection ? 'Chưa ghi lý do lỗi.' : '—')}
                </td>
                <td className={cell}>{row.batchReference || '—'}{row.exportedAt && <div className="text-xs text-neutral-500">{displayTime(row.exportedAt)}</div>}</td>
                <td className={cell}>
                  <div className="flex flex-wrap gap-2">
                    {needsCorrection && <Button type="button" size="sm" variant="outline"
                      disabled={actionsDisabled} isLoading={action === row.id}
                      className="!w-9 !px-0" aria-label="Kiểm tra lại hồ sơ"
                      title="Kiểm tra lại hồ sơ lưu trú"
                      onClick={() => void retry(row.id)}>
                      <RotateCw className="size-4" aria-hidden="true" />
                    </Button>}
                    <Button type="button" size="sm" variant="outline" disabled={actionsDisabled || !hasGuest || locked}
                      className="!w-9 !px-0" aria-label="Sửa hồ sơ lưu trú"
                      title={!hasGuest ? 'Thiếu liên kết hồ sơ' : locked ? 'Hồ sơ đã xuất hoặc đang xử lý' : 'Sửa hồ sơ lưu trú'}
                      onClick={() => setGuestAction({ type: 'edit', row })}>
                      <Pencil className="size-4" aria-hidden="true" />
                    </Button>
                    {canManageDeletion && <Button type="button" size="sm" variant="outline"
                      disabled={actionsDisabled || !hasGuest || !canDeleteGuest(row)}
                      className="!w-9 !px-0 text-red-700" aria-label="Xóa mềm hồ sơ lưu trú"
                      title={!hasGuest ? 'Thiếu liên kết hồ sơ' : !canDeleteGuest(row)
                        ? 'Chỉ xóa hồ sơ chưa xuất và không đang xử lý' : 'Xóa mềm hồ sơ lưu trú'}
                      onClick={() => setGuestAction({ type: 'delete', row })}>
                      <Trash2 className="size-4" aria-hidden="true" />
                    </Button>}
                  </div>
                </td>
              </tr>
            })}
          </tbody>
        </table>
      </div>
      <Pagination {...data} onPageChange={setPage} />
    </>}
    {guestAction?.type === 'edit' && <LodgingGuestEditDialog key={guestAction.row.stayGuestId} row={guestAction.row}
      onClose={() => setGuestAction(undefined)} onSaved={() => {
        setGuestAction(undefined)
        setMessage({ text: 'Đã lưu hồ sơ lưu trú.' })
        setPage(0)
        refresh()
      }} />}
    {guestAction?.type === 'delete' && <ConfirmModal isOpen title="Xóa mềm hồ sơ lưu trú"
      description={`Hồ sơ ${guestAction.row.stayGuest?.fullName || 'đã chọn'} sẽ biến mất khỏi danh sách khách lưu trú và bị hủy trong hàng đợi. Dữ liệu gốc vẫn được lưu trong hệ thống.`}
      isLoading={action === 'delete'} onClose={() => { if (!lock.current) setGuestAction(undefined) }}
      onConfirm={deleteGuest} />}
    {exportOpen && !uncertainExport && <ExportDialog hotel={hotel} hotels={hotels} selectHotel={selectHotel} busy={action === 'export'}
      close={() => { if (!lock.current) setExportOpen(false) }} confirm={value => void exportWorkbook(value)} />}
  </section>
}

export default function LodgingQueuePage() {
  const isChainAdmin = hasAnyRole(['CHAIN_ADMIN'])
  const [version, setVersion] = useState(0)
  const [loaded, setLoaded] = useState<{ version: number; hotels?: Hotel[]; error?: string }>()
  const [selectedId, setSelectedId] = useState<number>()
  const [exportOpen, setExportOpen] = useState(false)
  const result = loaded?.version === version ? loaded : undefined
  const hotel = isChainAdmin ? result?.hotels?.find(item => item.id === selectedId) : result?.hotels?.[0]

  useEffect(() => {
    const request = new AbortController()
    void loadLodgingHotels(isChainAdmin, request.signal).then(
      hotels => {
        if (!request.signal.aborted) setLoaded({ version, hotels, error: hotels.length ? undefined
          : isChainAdmin ? 'Chưa có khách sạn trong danh mục.' : 'Tài khoản chưa được gán khách sạn hợp lệ.' })
      },
      error => {
        if (!request.signal.aborted) setLoaded({ version, error: [403, 404].includes(getErrorStatus(error) ?? 0)
          ? 'Không xác định được khách sạn được cấp quyền. Liên hệ quản trị viên.'
          : 'Không thể tải danh sách khách sạn.' })
      },
    )
    return () => request.abort()
  }, [isChainAdmin, version])

  return <div className="space-y-5">
    <header><h1 className="text-2xl font-bold text-neutral-900">Khai báo lưu trú</h1>
      <p className="mt-1 text-sm text-neutral-600">Theo dõi hồ sơ, bổ sung thông tin và xuất Excel đối soát nội bộ.</p>
    </header>
    {!result && <p role="status" className="text-sm">Đang tải khách sạn…</p>}
    {result?.error && <div className={panel}><p role="alert" className="text-sm text-red-700">{result.error}</p>
      <Button variant="outline" onClick={() => setVersion(value => value + 1)}>Thử lại</Button></div>}
    {!!result?.hotels?.length && (isChainAdmin ? <label className="block max-w-md text-sm font-medium">Khách sạn
      <select className={selectStyle} value={selectedId ?? ''} onChange={e => setSelectedId(Number(e.target.value) || undefined)}>
        <option value="">Chọn khách sạn</option>
        {result.hotels.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
      </select>
    </label> : <p className="text-sm">Khách sạn: <strong>{hotel?.name}</strong></p>)}
    {hotel && <Queue key={hotel.id} hotel={hotel} hotels={isChainAdmin ? result?.hotels : undefined}
      exportOpen={exportOpen} setExportOpen={setExportOpen} selectHotel={id => {
        setSelectedId(id)
        if (sessionStorage.getItem('lodging-export-pending:' + id) === '1') setExportOpen(false)
      }} />}
  </div>
}
