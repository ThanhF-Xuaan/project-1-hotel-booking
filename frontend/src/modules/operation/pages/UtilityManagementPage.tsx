import { useCallback, useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { getApiErrorMessage } from '../../../core/api/error'
import { getErrorStatus } from '../../../core/api/client'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import { hasAnyRole } from '../../auth/utils/token.utils'
import { MeterForm, ReadingForm, selectClass } from '../components/UtilityForms'
import { utilityService } from '../services/utility.service'
import { UTILITY_WRITE_ROLES, type MeterType, type UtilityMeter, type UtilityReading } from '../types/utility.types'

type Load<T> = (signal: AbortSignal) => Promise<T>
type LoadResult<T> = { data: T; error?: never } | { error: string; data?: never }

function useLoad<T>(load: Load<T> | null) {
  const [revision, setRevision] = useState(0)
  const [state, setState] = useState<{ load: Load<T>; revision: number; result: LoadResult<T> }>()
  useEffect(() => {
    if (!load) return
    const request = new AbortController()
    const settle = (result: LoadResult<T>) => {
      if (!request.signal.aborted) setState({ load, revision, result })
    }
    void load(request.signal).then(
      data => settle({ data }),
      error => settle({ error: getApiErrorMessage(error, error instanceof Error ? error.message : 'Không thể tải dữ liệu').message }),
    )
    return () => request.abort()
  }, [load, revision])

  const result = state?.load === load && state?.revision === revision ? state.result : undefined
  return { data: result?.data, error: result?.error, loading: !!load && !result, retry: () => setRevision(value => value + 1) }
}

function LoadStatus({ loading, error, retry, empty, children }: {
  loading: boolean
  error?: string
  retry: () => void
  empty?: string
  children?: ReactNode
}) {
  if (loading) return <p role="status" className="text-sm">Đang tải dữ liệu…</p>
  if (error) return <div className="space-y-2">
    <p role="alert" className="text-sm text-red-700">{error}</p>
    <Button size="sm" variant="outline" onClick={retry}>Thử lại</Button>
  </div>
  return empty ? <p role="status" className="text-sm text-neutral-600">{empty}</p> : children
}

const pageSize = 10
const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 3 })
const dateFormat = (date: string) => date.split('-').reverse().join('/')
const cell = 'px-3 py-2 text-left'
const panel = 'space-y-4 rounded-xl border border-neutral-200 bg-white p-4'

type Action =
  | { kind: 'meter'; meter?: UtilityMeter }
  | { kind: 'reading'; meter: UtilityMeter; reading?: UtilityReading }
  | { kind: 'deleteMeter'; meter: UtilityMeter }
  | { kind: 'deleteReading'; reading: UtilityReading }

function UtilityTables({ hotelId, canWrite }: { hotelId: number; canWrite: boolean }) {
  const [keyword, setKeyword] = useState('')
  const [meterType, setMeterType] = useState<MeterType | ''>('')
  const [meterPage, setMeterPage] = useState(0)
  const [meter, setMeter] = useState<UtilityMeter>()
  const [range, setRange] = useState({ from: '', to: '' })
  const [rangeError, setRangeError] = useState('')
  const [readingPage, setReadingPage] = useState(0)
  const [action, setAction] = useState<Action>()
  const [notice, setNotice] = useState('')
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)
  const [uncertain, setUncertain] = useState(false)
  const writeLock = useRef(false)

  const meterId = meter?.id
  const loadMeters = useCallback((signal: AbortSignal) => utilityService.filterMeters({
    hotelId, keyword: keyword.trim() || undefined, meterType: meterType || undefined,
    page: meterPage, pageSize, sortBy: 'meterCode', sortDirection: 'ASC',
  }, signal), [hotelId, keyword, meterType, meterPage])
  const loadReadings = useCallback((signal: AbortSignal) => {
    if (meterId === undefined) throw new Error('Chưa chọn đồng hồ')
    return utilityService.filterReadings({
      hotelId, meterId, fromDate: range.from || undefined, toDate: range.to || undefined,
      page: readingPage, pageSize, sortBy: 'readingDate', sortDirection: 'DESC',
    }, signal)
  }, [hotelId, meterId, range, readingPage])
  const meters = useLoad(loadMeters)
  const readings = useLoad(meterId === undefined ? null : loadReadings)

  const open = (next: Action) => {
    setError(undefined)
    setNotice('')
    setUncertain(false)
    setAction(next)
  }
  const close = () => {
    if (!writeLock.current) setAction(undefined)
  }
  const chooseMeter = (next?: UtilityMeter) => {
    setMeter(next)
    setRange({ from: '', to: '' })
    setRangeError('')
    setReadingPage(0)
  }
  const changeRange = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const from = String(data.get('from') ?? '')
    const to = String(data.get('to') ?? '')
    if (from && to && from > to) {
      setRangeError('Từ ngày phải trước hoặc bằng Đến ngày.')
      return
    }
    setRangeError('')
    setRange({ from, to })
    setReadingPage(0)
  }

  const save = async (request: () => Promise<unknown>, onSuccess: () => void) => {
    if (writeLock.current || uncertain || !canWrite) return
    writeLock.current = true
    setBusy(true)
    setError(undefined)
    try {
      await request()
      onSuccess()
      setAction(undefined)
      setNotice('Đã cập nhật dữ liệu.')
    } catch (cause) {
      const status = getErrorStatus(cause)
      if (status === undefined || status === 408 || status === 504) {
        setUncertain(true)
        meters.retry()
        readings.retry()
        setError('Chưa xác nhận được kết quả. Đóng hộp thoại và đối chiếu danh sách đã tải lại trước khi thao tác lại.')
      } else {
        const info = getApiErrorMessage(cause, 'Không thể lưu thay đổi')
        setError(info.code === 3021 ? 'Tài khoản chưa được liên kết với hồ sơ nhân viên.' : info.message)
      }
    } finally {
      writeLock.current = false
      setBusy(false)
    }
  }
  const formActions = { busy, uncertain, error, close }

  return <div className="space-y-5">
    {notice && <p role="status" className="text-sm text-green-700">{notice}</p>}
    <section aria-labelledby="utility-meters" className={panel}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="utility-meters" className="text-lg font-semibold">Đồng hồ</h2>
        {canWrite && <Button onClick={() => open({ kind: 'meter' })}>Thêm đồng hồ</Button>}
      </div>
      <div className="grid items-end gap-3 sm:grid-cols-2">
        <Input id="utility-meter-keyword" label="Tìm theo mã" value={keyword}
          onChange={event => { setKeyword(event.target.value); setMeterPage(0) }} />
        <label className="text-sm font-medium">Loại đồng hồ
          <select className={selectClass} value={meterType}
            onChange={event => { setMeterType(event.target.value as MeterType | ''); setMeterPage(0) }}>
            <option value="">Tất cả</option>
            <option value="ELECTRICITY">Điện</option>
            <option value="WATER">Nước</option>
          </select>
        </label>
      </div>
      <LoadStatus {...meters} empty={meters.data?.empty ? 'Không có đồng hồ phù hợp.' : undefined}>
        {meters.data && <>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-neutral-50"><tr>
                {['Mã đồng hồ', 'Loại', 'Vị trí', 'Thao tác'].map(name => <th key={name} scope="col" className={cell}>{name}</th>)}
              </tr></thead>
              <tbody className="divide-y divide-neutral-100">
                {meters.data.content.map(item => <tr key={item.id} className={meterId === item.id ? 'bg-red-50' : ''}>
                  <th scope="row" className={cell}>{item.meterCode}</th>
                  <td className={cell}>{item.meterType === 'ELECTRICITY' ? 'Điện' : 'Nước'}</td>
                  <td className={cell}>{item.locationLabel || '—'}</td>
                  <td className={cell}><div className="flex flex-wrap gap-1">
                    <Button size="sm" variant="outline" aria-pressed={meterId === item.id}
                      onClick={() => { if (meterId !== item.id) chooseMeter(item) }}>Xem chỉ số</Button>
                    {canWrite && <>
                      <Button size="sm" variant="ghost" onClick={() => open({ kind: 'meter', meter: item })}>Sửa</Button>
                      <Button size="sm" variant="ghost" onClick={() => open({ kind: 'deleteMeter', meter: item })}>Xóa</Button>
                    </>}
                  </div></td>
                </tr>)}
              </tbody>
            </table>
          </div>
          <Pagination {...meters.data} onPageChange={setMeterPage} />
        </>}
      </LoadStatus>
    </section>
    <section aria-labelledby="utility-readings" className={panel}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="utility-readings" className="text-lg font-semibold">Lịch sử chỉ số{meter && ' · ' + meter.meterCode}</h2>
        {meter && canWrite && <Button onClick={() => open({ kind: 'reading', meter })}>Thêm chỉ số</Button>}
      </div>
      {!meter ? <p className="text-sm text-neutral-600">Chọn một đồng hồ để xem lịch sử.</p> : <>
        <form key={meter.id} onSubmit={changeRange} className="grid items-end gap-3 sm:grid-cols-[1fr_1fr_auto]">
          <Input id="utility-from" label="Từ ngày" name="from" type="date" />
          <Input id="utility-to" label="Đến ngày" name="to" type="date" />
          <Button type="submit" variant="outline">Lọc ngày</Button>
          {rangeError && <p role="alert" className="text-sm text-red-700 sm:col-span-3">{rangeError}</p>}
        </form>
        <LoadStatus {...readings} empty={readings.data?.empty ? 'Không có chỉ số trong khoảng ngày đã chọn.' : undefined}>
          {readings.data && <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-neutral-50"><tr>
                  {['Ngày ghi', 'Chỉ số', 'Mức sử dụng', 'Đặt lại', ...(canWrite ? ['Thao tác'] : [])]
                    .map(name => <th key={name} scope="col" className={cell}>{name}</th>)}
                </tr></thead>
                <tbody className="divide-y divide-neutral-100">
                  {readings.data.content.map(item => <tr key={item.id}>
                    <th scope="row" className={cell}>{dateFormat(item.readingDate)}</th>
                    <td className={cell}>{numberFormat.format(item.readingValue)}</td>
                    <td className={cell}>{item.usage == null ? '—' : numberFormat.format(item.usage)}</td>
                    <td className={cell}>{item.isMeterReset ? 'Có' : 'Không'}</td>
                    {canWrite && <td className={cell}><div className="flex flex-wrap gap-1">
                      <Button size="sm" variant="ghost" onClick={() => open({ kind: 'reading', meter, reading: item })}>Sửa</Button>
                      <Button size="sm" variant="ghost" onClick={() => open({ kind: 'deleteReading', reading: item })}>Xóa</Button>
                    </div></td>}
                  </tr>)}
                </tbody>
              </table>
            </div>
            <Pagination {...readings.data} onPageChange={setReadingPage} />
          </>}
        </LoadStatus>
      </>}
    </section>
    {action?.kind === 'meter' && <Modal isOpen onClose={close} title={action.meter ? 'Sửa đồng hồ' : 'Thêm đồng hồ'}>
      <MeterForm {...formActions} meter={action.meter} hotelId={hotelId}
        save={data => save(() => action.meter
          ? utilityService.updateMeter(action.meter.id, { meterCode: data.meterCode, locationLabel: data.locationLabel })
          : utilityService.createMeter(data), () => {
          if (meter && action.meter?.id === meter.id) setMeter({ ...meter, meterCode: data.meterCode, locationLabel: data.locationLabel })
          meters.retry()
        })} />
    </Modal>}
    {action?.kind === 'reading' && <Modal isOpen onClose={close} title={action.reading ? 'Sửa chỉ số' : 'Thêm chỉ số'}>
      <ReadingForm {...formActions} meter={action.meter} reading={action.reading}
        save={data => save(() => action.reading
          ? utilityService.updateReading(action.reading.id, { readingValue: data.readingValue, isMeterReset: data.isMeterReset })
          : utilityService.createReading(data), readings.retry)} />
    </Modal>}
    {(action?.kind === 'deleteMeter' || action?.kind === 'deleteReading') && <ConfirmModal
      isOpen onClose={close} isLoading={busy} title={action.kind === 'deleteMeter' ? 'Xóa đồng hồ' : 'Xóa chỉ số'}
      description={action.kind === 'deleteMeter'
        ? 'Đồng hồ ' + action.meter.meterCode + ' sẽ biến mất khỏi danh sách. Lịch sử chỉ số vẫn được lưu.'
        : 'Chỉ số ngày ' + dateFormat(action.reading.readingDate) + ' sẽ bị ẩn khỏi lịch sử.'}
      confirmText={uncertain ? 'Đóng và kiểm tra' : undefined}
      onConfirm={() => uncertain ? close() : save(() => action.kind === 'deleteMeter'
        ? utilityService.deleteMeter(action.meter.id) : utilityService.deleteReading(action.reading.id), () => {
        if (action.kind === 'deleteMeter') {
          if (meterId === action.meter.id) chooseMeter()
          if (meters.data?.content.length === 1) setMeterPage(page => Math.max(0, page - 1))
          meters.retry()
        } else {
          if (readings.data?.content.length === 1) setReadingPage(page => Math.max(0, page - 1))
          readings.retry()
        }
      })}>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    </ConfirmModal>}
  </div>
}

export default function UtilityManagementPage() {
  const isAdmin = hasAnyRole(['CHAIN_ADMIN'])
  const canWrite = hasAnyRole(UTILITY_WRITE_ROLES)
  const [selectedHotel, setSelectedHotel] = useState<number>()
  const load = useCallback(async (signal: AbortSignal) => {
    if (isAdmin) return { hotels: await utilityService.hotels(undefined, signal) }
    const scope = await utilityService.currentScope(canWrite ? 'PROPERTY' : 'REGION', signal).catch(error => {
      if (getApiErrorMessage(error).code === 3021) return null
      throw error
    })
    if (!scope) throw new Error('Tài khoản chưa có hồ sơ nhân viên hoặc phạm vi được phân công không hợp lệ.')
    if (canWrite) return { hotelId: scope }
    return { hotels: await utilityService.hotels(scope, signal) }
  }, [isAdmin, canWrite])
  const scope = useLoad(load)
  const hotels = scope.data?.hotels
  const hotelId = scope.data?.hotelId ?? hotels?.find(hotel => hotel.id === selectedHotel)?.id

  return <div className="space-y-5">
    <header>
      <h1 className="text-2xl font-bold text-neutral-900">Điện/Nước</h1>
      <p className="text-sm text-neutral-600">Quản lý đồng hồ và lịch sử ghi chỉ số.</p>
    </header>
    <LoadStatus {...scope} empty={hotels?.length === 0 ? 'Không có khách sạn trong phạm vi được phân công.' : undefined}>
      {hotels && <label className="block max-w-md text-sm font-medium">Khách sạn
        <select className={selectClass} value={hotelId ?? ''} onChange={event => setSelectedHotel(Number(event.target.value) || undefined)}>
          <option value="">Chọn khách sạn</option>
          {hotels.map(hotel => <option key={hotel.id} value={hotel.id}>{hotel.name}</option>)}
        </select>
      </label>}
      {scope.data?.hotelId && <p className="text-sm">Khách sạn được phân công #{scope.data.hotelId}</p>}
      {hotelId ? <UtilityTables key={hotelId} hotelId={hotelId} canWrite={canWrite} />
        : <p className="text-sm text-neutral-600">Chọn khách sạn để xem đồng hồ.</p>}
    </LoadStatus>
  </div>
}
