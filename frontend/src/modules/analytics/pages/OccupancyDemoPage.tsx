import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, FlaskConical, RefreshCw } from 'lucide-react'
import { getErrorStatus } from '../../../core/api/client'
import Button from '../../../core/components/ui/Button'
import OccupancyChart from '../components/OccupancyChart'
import { displayDate, occupancyDemoService, occupancyPercent, type DemoScenario, type OccupancyDemo } from '../services/occupancyDemoService'

const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 })
const formatPercent = (value: number) => `${number.format(value)}%`
const STATUS: Record<OccupancyDemo['forecast']['status'], string> = {
  FORECAST_READY: 'Dự báo sẵn sàng', BASELINE_FALLBACK: 'Dùng mô hình nền',
  INSUFFICIENT_COVERAGE: 'Chưa đủ dữ liệu để dự báo', INVALID_REQUEST: 'Yêu cầu dự báo không hợp lệ',
  ACCESS_DENIED: 'Không được phép đọc dữ liệu dự báo', DEPENDENCY_UNAVAILABLE: 'Dịch vụ dự báo chưa sẵn sàng',
}
function errorMessage(error: unknown): string {
  switch (getErrorStatus(error)) {
    case 401: return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
    case 403: return 'Quyền truy cập demo đã bị từ chối. Hãy kiểm tra tài khoản admin.'
    case 404: return 'Demo chưa được bật trên máy chủ. Hãy bật cấu hình demo rồi tải lại.'
    case 503: return 'Dịch vụ demo tạm thời chưa sẵn sàng. Hãy thử lại sau.'
    default: return 'Không tải được dữ liệu demo hợp lệ. Hãy kiểm tra kết nối hoặc cấu hình rồi thử lại.'
  }
}

export default function OccupancyDemoPage() {
  const [selection, setSelection] = useState({ scenario: 'HOTEL_A' as DemoScenario, revision: 0 })
  const [result, setResult] = useState<{ key: typeof selection; data?: OccupancyDemo; error?: string } | null>(null)
  const current = result?.key === selection ? result : null
  const loading = current === null
  const data = current?.data
  useEffect(() => {
    const controller = new AbortController()
    occupancyDemoService.get(selection.scenario, controller.signal).then(value => {
      if (!controller.signal.aborted) setResult({ key: selection, data: value })
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) setResult({ key: selection, error: errorMessage(error) })
    })
    return () => controller.abort()
  }, [selection])
  const reload = () => setSelection(previous => ({ ...previous, revision: previous.revision + 1 }))

  return (
    <main className="min-h-screen bg-neutral-50 px-4 py-6 text-neutral-900 sm:px-8 sm:py-10">
      <div className="mx-auto max-w-6xl space-y-6">
        <Link to="/dashboard" className="inline-flex min-h-11 items-center gap-2 rounded-lg text-sm font-medium text-neutral-700 hover:text-red-700 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-700">
          <ArrowLeft aria-hidden="true" className="h-4 w-4" />Về bàn làm việc
        </Link>
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-amber-300 bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-900"><FlaskConical aria-hidden="true" className="h-4 w-4" />DEMO · DỮ LIỆU GIẢ LẬP</span>
            <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">Dự báo tỷ lệ lấp đầy</h1>
            <p className="mt-2 max-w-2xl text-sm leading-relaxed text-neutral-600">Xem lại lịch sử và dự báo theo mốc cố định. Dữ liệu này không phản ánh hoạt động của khách sạn thật.</p>
          </div>
        </header>
        <section aria-label="Chọn dữ liệu demo" className="flex flex-wrap items-end gap-4 rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
          <div className="min-w-0 flex-1 sm:flex-none">
            <label htmlFor="occupancy-scenario" className="mb-2 block text-sm font-semibold">Kịch bản mô phỏng</label>
            <select id="occupancy-scenario" value={selection.scenario} onChange={event => setSelection({ scenario: event.target.value as DemoScenario, revision: 0 })}
              className="min-h-12 w-full rounded-xl border border-neutral-300 bg-white px-3 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-700 sm:w-64">
              <option value="HOTEL_A">Kịch bản A</option><option value="HOTEL_B">Kịch bản B</option>
            </select>
          </div>
          <Button type="button" variant="outline" className="min-h-11" disabled={loading} onClick={reload} leftIcon={<RefreshCw aria-hidden="true" className="h-4 w-4" />}>Tải lại</Button>
          <p className="w-full text-xs leading-relaxed text-neutral-600">Mốc mô phỏng: 30/10/2026 lúc 00:00 (giờ Việt Nam). Các kịch bản độc lập với danh mục khách sạn thật.</p>
        </section>
        {loading && <div role="status" className="rounded-2xl border border-neutral-200 bg-white p-8 text-center text-neutral-600">Đang tải dữ liệu mô phỏng…</div>}
        {current?.error && <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-900"><p>{current.error}</p><Button type="button" variant="outline" className="mt-4 min-h-11 bg-white" onClick={reload}>Thử lại</Button></div>}
        {data && <>
          <section aria-label="Tóm tắt kịch bản" className="grid gap-4 sm:grid-cols-3">
            <Summary label="Lịch sử mô phỏng" value={`${data.history.samples.length} ngày`} detail={`${displayDate(data.history.fromInclusive)} – ${displayDate(data.history.toInclusive)}`} />
            <Summary label="Chất lượng lịch sử" value={`${data.history.samples.filter(row => occupancyPercent(row) === null).length} ngày không có tỷ lệ`} detail="Thiếu dữ liệu hoặc sức chứa bằng 0; không thay bằng 0%." />
            <Summary label="Trạng thái dự báo" value={STATUS[data.forecast.status]} detail={data.forecast.model === 'RIDGE_TREND_WEEKDAY' ? 'Mô hình xu hướng và thứ trong tuần' : data.forecast.model === 'SEASONAL_NAIVE_7' ? 'Mô hình nền lặp theo tuần' : 'Không tạo số liệu dự báo thay thế'} />
          </section>
          <OccupancyChart data={data} />
          <section className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
            <h2 className="text-lg font-semibold">Bảng dự báo mô phỏng</h2>
            {data.forecast.points.length === 0 ? <p role="status" className="mt-4 text-sm text-neutral-600">{STATUS[data.forecast.status]}. Lịch sử vẫn có thể xem bên dưới.</p> : <>
              <p className="mt-1 text-sm text-neutral-600">{displayDate(data.forecast.points[0].date)} – {displayDate(data.forecast.points.at(-1)!.date)} · Biên sai số: ±{number.format(data.forecast.residualRadiusPp!)} điểm phần trăm.</p>
              <div role="region" aria-label="Số liệu dự báo" tabIndex={0} className="mt-4 overflow-x-auto rounded-lg focus-visible:outline-2 focus-visible:outline-red-700">
                <table className="w-full min-w-100 text-left text-sm"><caption className="sr-only">30 ngày dự báo, tỷ lệ và biên sai số tham chiếu từ máy chủ</caption><thead><tr>{['Ngày', 'Dự báo', 'Biên dưới', 'Biên trên'].map(label => <th key={label} scope="col" className="border-b border-neutral-200 px-3 py-3 font-semibold">{label}</th>)}</tr></thead><tbody>{data.forecast.points.map(row => <tr key={row.date} className="border-b border-neutral-100 tabular-nums last:border-0"><th scope="row" className="px-3 py-3 font-medium">{displayDate(row.date)}</th><td className="px-3 py-3">{formatPercent(row.occupancyPercent)}</td><td className="px-3 py-3">{formatPercent(row.lowerPercent)}</td><td className="px-3 py-3">{formatPercent(row.upperPercent)}</td></tr>)}</tbody></table>
              </div>
            </>}
          </section>
          <details className="rounded-2xl border border-neutral-200 bg-white p-4 sm:p-6">
            <summary className="cursor-pointer rounded-lg py-2 font-semibold focus-visible:outline-2 focus-visible:outline-red-700">Toàn bộ lịch sử mô phỏng ({data.history.samples.length} ngày)</summary>
            <div role="region" aria-label="Số liệu lịch sử" tabIndex={0} className="mt-4 max-h-96 overflow-auto rounded-lg focus-visible:outline-2 focus-visible:outline-red-700">
              <table className="w-full min-w-120 text-left text-sm"><caption className="sr-only">Lịch sử gồm ngày thiếu dữ liệu và ngày không có sức chứa</caption><thead className="sticky top-0 bg-white"><tr>{['Ngày', 'Phòng sử dụng', 'Sức chứa (phòng)', 'Tỷ lệ'].map(label => <th key={label} scope="col" className="border-b border-neutral-200 px-3 py-3 font-semibold">{label}</th>)}</tr></thead><tbody>{data.history.samples.map(row => {
                const percent = occupancyPercent(row)
                return <tr key={row.date} className="border-b border-neutral-100 tabular-nums last:border-0"><th scope="row" className="px-3 py-3 font-medium">{displayDate(row.date)}</th><td className="px-3 py-3">{row.occupiedRooms ?? 'Thiếu dữ liệu'}</td><td className="px-3 py-3">{row.capacityRooms ?? 'Thiếu dữ liệu'}</td><td className="px-3 py-3">{percent === null ? (row.coverage === 'MISSING' ? 'Thiếu dữ liệu' : 'Không có sức chứa') : formatPercent(percent)}</td></tr>
              })}</tbody></table>
            </div>
          </details>
        </>}
      </div>
    </main>
  )
}

function Summary({ label, value, detail }: { label: string; value: string; detail: string }) {
  return <div className="rounded-2xl border border-neutral-200 bg-white p-5"><h2 className="text-sm font-medium text-neutral-600">{label}</h2><p className="mt-2 text-xl font-bold">{value}</p><p className="mt-2 text-xs leading-relaxed text-neutral-600">{detail}</p></div>
}
